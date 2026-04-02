package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.agent.dto.ChatResponse;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravelAgentService {

    private final ChatClient chatClient;
    private final ToolCallback currentWeatherFunction;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public TravelAgentService(
            ChatClient chatClient,
            ToolCallback currentWeatherFunction,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository
    ) {
        this.chatClient = chatClient;
        this.currentWeatherFunction = currentWeatherFunction;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    record IntentClassification(String intent) {
    }

    @Transactional
    public ChatResponse chatWithAgent(User user, Long sessionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new BusinessException("메시지가 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String trimmedMessage = userMessage.trim();

        ChatSession session;
        if (sessionId == null) {
            session = sessionRepository.save(ChatSession.builder()
                    .user(user)
                    .sessionTitle(trimmedMessage.substring(0, Math.min(trimmedMessage.length(), 10)) + "...")
                    .build());
        } else {
            session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                    .orElseThrow(() -> new BusinessException("해당 채팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        }

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("USER")
                .content(trimmedMessage)
                .build());

        List<ChatMessage> history = messageRepository.findAllBySessionIdOrderBySentAtAsc(session.getId());
        String conversationHistory = history.stream()
                .map(msg -> msg.getSenderRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        BeanOutputConverter<IntentClassification> intentConverter = new BeanOutputConverter<>(IntentClassification.class);
        IntentClassification classification = chatClient.prompt()
                .system(s -> s.text("사용자 메시지 의도를 분류하세요. 일정 요청은 'PLAN_CREATION', 나머지는 'GENERAL_CHAT'.\n{formatInstructions}")
                        .param("formatInstructions", intentConverter.getFormat()))
                .user(trimmedMessage)
                .call()
                .entity(intentConverter);

        String intent = classification.intent();
        String aiReplyText;

        if ("PLAN_CREATION".equals(intent)) {
            aiReplyText = "좋습니다. 여행 일정을 만들기 위해 입력 폼 정보를 알려주세요.";
        } else {
            aiReplyText = chatClient.prompt()
                    .system("당신은 Compass 여행 도우미입니다. 정확하고 간결하게 답변하세요.")
                    .user("이전 대화 내역:\n" + conversationHistory + "\n\n현재 사용자 메시지: " + trimmedMessage)
                    .toolCallbacks(currentWeatherFunction)
                    .call()
                    .content();
        }

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content(aiReplyText)
                .build());

        return new ChatResponse(intent, aiReplyText, session.getId(), null);
    }

    public AiPlanResponse generateTravelPlan(User user, String request, List<ChatMessage> history) {
        BeanOutputConverter<AiPlanResponse> converter = new BeanOutputConverter<>(AiPlanResponse.class);

        String historySummary = "";
        if (history != null && !history.isEmpty()) {
            historySummary = history.stream()
                    .map(msg -> msg.getSenderRole() + ": " + msg.getContent())
                    .collect(Collectors.joining("\n"));
        }

        return chatClient.prompt()
                .system(s -> s.text(
                                "당신은 숙련된 여행 플래너입니다. 사용자 요청을 바탕으로 일정 JSON을 생성하세요.\n" +
                                        "규칙:\n" +
                                        "1) places는 구체적인 실제 장소명만 사용합니다.\n" +
                                        "2) day별 동선을 자연스럽게 구성합니다.\n" +
                                        "3) memo는 장소 추천 이유를 간결히 작성합니다.\n" +
                                        "4) 응답은 JSON만 반환합니다.\n{formatInstructions}")
                        .param("formatInstructions", converter.getFormat()))
                .user("사용자 최종 요청: " + request + "\n\n참고 대화 내역:\n" + historySummary)
                .call()
                .entity(converter);
    }
}

