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
    private final String planFallbackModelName;

    public TravelAgentService(
            @Qualifier("generalChatClient") ChatClient generalChatClient,
            @Qualifier("planChatClient") ChatClient planChatClient,
            ToolCallback currentWeatherFunction,
            RequestQuotaService requestQuotaService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            @Value("${app.ai.model.general:gemini-3.1-flash-lite-preview}") String generalModelName,
            @Value("${app.ai.model.plan:gemini-3.1-pro-preview}") String planModelName,
            @Value("${app.ai.model.plan-fallback:gemini-3.1-flash-lite-preview}") String planFallbackModelName
    ) {
        this.generalChatClient = generalChatClient;
        this.planChatClient = planChatClient;
        this.currentWeatherFunction = currentWeatherFunction;
        this.requestQuotaService = requestQuotaService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.generalModelName = generalModelName;
        this.planModelName = planModelName;
        this.planFallbackModelName = planFallbackModelName;

        log.info("[AI-MODEL] initialized general={}, plan={}, fallback={}",
                this.generalModelName, this.planModelName, this.planFallbackModelName);
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

        try {
            return createPlanWithClient(planChatClient, converter, request, historySummary);
        } catch (Exception primary) {
            log.warn("[AI-MODEL] plan model failed. model={}, message={}", planModelName, rootCauseMessage(primary));
            log.info("[AI-MODEL] fallback attempt with general model={}", planFallbackModelName);

            try {
                return createPlanWithClient(generalChatClient, converter, request, historySummary);
            } catch (Exception fallback) {
                log.error("[AI-MODEL] both primary and fallback model failed. primaryModel={}, fallbackModel={}, root={}",
                        planModelName, planFallbackModelName, rootCauseMessage(fallback), fallback);
                throw toModelBusinessException(fallback);
            }
        }
    }

    private AiPlanResponse createPlanWithClient(
            ChatClient client,
            BeanOutputConverter<AiPlanResponse> converter,
            String request,
            String historySummary
    ) {
        return client.prompt()
                .system(s -> s.text(
                        "You are a strict travel planner. Return JSON only.\n" +
                                "Rules:\n" +
                                "1) Use realistic place names.\n" +
                                "2) Keep route flow practical.\n" +
                                "3) Keep memo concise and useful.\n" +
                                "4) Return valid JSON only.\n{formatInstructions}")
                        .param("formatInstructions", converter.getFormat()))
                .user("Final user request: " + request + "\n\nConversation history:\n" + historySummary)
                .call()
                .entity(converter);
    }

    private BusinessException toModelBusinessException(Exception e) {
        String message = rootCauseMessage(e).toLowerCase();

        if (message.contains("resource_exhausted") || message.contains("429") || message.contains("quota")) {
            return new BusinessException("AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (message.contains("credentials") || message.contains("permission") || message.contains("unauthenticated")
                || message.contains("401") || message.contains("403")) {
            return new BusinessException("AI 인증/권한 설정 오류가 발생했습니다. 관리자에게 문의해 주세요.", HttpStatus.BAD_GATEWAY);
        }
        if (message.contains("model") && message.contains("not found")) {
            return new BusinessException("AI 모델 설정이 유효하지 않습니다. 관리자에게 문의해 주세요.", HttpStatus.BAD_GATEWAY);
        }
        return new BusinessException("AI 일정 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
