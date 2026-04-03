package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.agent.dto.ChatResponse;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TravelAgentService {

    private final ChatClient generalChatClient;
    private final ChatClient planChatClient;
    private final ToolCallback currentWeatherFunction;
    private final RequestQuotaService requestQuotaService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final String generalModelName;
    private final String planModelName;

    public TravelAgentService(
            @Qualifier("generalChatClient") ChatClient generalChatClient,
            @Qualifier("planChatClient") ChatClient planChatClient,
            ToolCallback currentWeatherFunction,
            RequestQuotaService requestQuotaService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            @Value("${app.ai.model.general:gemini-3.1-flash-lite-preview}") String generalModelName,
            @Value("${app.ai.model.plan:gemini-3.1-pro-preview}") String planModelName
    ) {
        this.generalChatClient = generalChatClient;
        this.planChatClient = planChatClient;
        this.currentWeatherFunction = currentWeatherFunction;
        this.requestQuotaService = requestQuotaService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.generalModelName = generalModelName;
        this.planModelName = planModelName;

        log.info("[AI-MODEL] initialized general={}, plan={}", this.generalModelName, this.planModelName);
    }

    record IntentClassification(String intent) {}

    @Transactional
    public ChatResponse chatWithAgent(User user, Long sessionId, String userMessage) {
        log.info("[AI-MODEL] chatWithAgent -> general model={}", generalModelName);

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

        requestQuotaService.assertChatAllowedToday(user.getId());

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
        IntentClassification classification = generalChatClient.prompt()
                .system(s -> s.text("사용자 메시지 의도를 분류하세요. 여행 일정 생성 요청이면 'PLAN_CREATION', 그 외는 'GENERAL_CHAT'.\n{formatInstructions}")
                        .param("formatInstructions", intentConverter.getFormat()))
                .user(trimmedMessage)
                .call()
                .entity(intentConverter);

        String intent = classification.intent();
        String aiReplyText;

        if ("PLAN_CREATION".equals(intent)) {
            aiReplyText = "좋습니다. 여행 일정을 만들기 위해 입력 폼 정보를 알려주세요.";
        } else {
            aiReplyText = generalChatClient.prompt()
                    .system("당신은 Compass 여행 도우미입니다. 정확하고 간결하게 답변하세요.")
                    .user("이전 대화 이력:\n" + conversationHistory + "\n\n현재 사용자 메시지: " + trimmedMessage)
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
        log.info("[AI-MODEL] generateTravelPlan -> plan model={}", planModelName);

        BeanOutputConverter<AiPlanResponse> converter = new BeanOutputConverter<>(AiPlanResponse.class);

        String historySummary = "";
        if (history != null && !history.isEmpty()) {
            historySummary = history.stream()
                    .map(msg -> msg.getSenderRole() + ": " + msg.getContent())
                    .collect(Collectors.joining("\n"));
        }

        return planChatClient.prompt()
                .system(s -> s.text(
                        "당신은 숙련된 여행 플래너입니다. 사용자 요청을 바탕으로 일정 JSON을 생성하세요.\n" +
                        "규칙:\n" +
                        "1) places는 실제 장소명을 사용합니다.\n" +
                        "2) 동선은 현실적으로 구성합니다.\n" +
                        "3) memo는 추천 이유를 간결히 작성합니다.\n" +
                        "4) 반드시 JSON만 반환합니다.\n{formatInstructions}")
                        .param("formatInstructions", converter.getFormat()))
                .user("사용자 최종 요청: " + request + "\n\n참고 대화 이력:\n" + historySummary)
                .call()
                .entity(converter);
    }
}
