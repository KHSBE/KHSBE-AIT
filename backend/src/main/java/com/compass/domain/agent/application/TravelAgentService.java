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
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TravelAgentService {

    private static final String INTENT_PLAN_CREATION = "PLAN_CREATION";
    private static final String INTENT_GENERAL_CHAT = "GENERAL_CHAT";
    private static final String PLAN_FORM_GUIDE_MESSAGE = "좋습니다. 여행 일정을 만들기 위해 입력 폼 정보를 알려주세요.";

    private final ChatClient generalChatClient;
    private final ChatClient planChatClient;
    private final ToolCallback currentWeatherFunction;
    private final RequestQuotaService requestQuotaService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final String generalModelName;
    private final String planModelName;
    private final String planFallbackModelName;
    private final int historyWindowMessages;

    public TravelAgentService(
            @Qualifier("generalChatClient") ChatClient generalChatClient,
            @Qualifier("planChatClient") ChatClient planChatClient,
            ToolCallback currentWeatherFunction,
            RequestQuotaService requestQuotaService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            @Value("${app.ai.model.general:gemini-3.1-flash-lite-preview}") String generalModelName,
            @Value("${app.ai.model.plan:gemini-3.1-pro-preview}") String planModelName,
            @Value("${app.ai.model.plan-fallback:gemini-3.1-flash-lite-preview}") String planFallbackModelName,
            @Value("${app.ai.chat.history-window-messages:12}") int historyWindowMessages
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
        this.historyWindowMessages = Math.max(4, historyWindowMessages);

        log.debug("[AI-MODEL] initialized general={}, plan={}, fallback={}, historyWindowMessages={}",
                this.generalModelName, this.planModelName, this.planFallbackModelName, this.historyWindowMessages);
    }

    record ChatAssistantResult(String intent, String message) {
    }

    @Transactional
    public ChatResponse chatWithAgent(User user, Long sessionId, String userMessage) {
        log.debug("[AI-MODEL] chatWithAgent -> general model={}", generalModelName);

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
        String recentHistory = buildRecentHistory(history);

        BeanOutputConverter<ChatAssistantResult> converter = new BeanOutputConverter<>(ChatAssistantResult.class);
        ChatAssistantResult result;

        try {
            result = generalChatClient.prompt()
                    .system(s -> s.text(
                            "당신은 Compass 여행 도우미입니다. 아래 대화 문맥을 보고 반드시 JSON으로만 응답하세요.\n"
                                    + "intent는 'PLAN_CREATION' 또는 'GENERAL_CHAT' 중 하나여야 합니다.\n"
                                    + "- 사용자가 여행 일정 생성을 명확히 요청하면 PLAN_CREATION\n"
                                    + "- 그 외 질문/대화는 GENERAL_CHAT\n"
                                    + "message는 사용자에게 보여줄 자연스러운 문장입니다.\n"
                                    + "PLAN_CREATION일 때는 message를 짧게 작성하세요.\n"
                                    + "{formatInstructions}")
                            .param("formatInstructions", converter.getFormat()))
                    .user("최근 대화\n" + recentHistory)
                    .toolCallbacks(currentWeatherFunction)
                    .call()
                    .entity(converter);
        } catch (Exception e) {
            log.warn("[AI-MODEL] chat single-call parse failed. model={}, message={}", generalModelName, rootCauseMessage(e));
            String fallbackReply = generalChatClient.prompt()
                    .system("당신은 Compass 여행 도우미입니다. 정확하고 간결하게 한국어로 답변하세요.")
                    .user("최근 대화\n" + recentHistory)
                    .toolCallbacks(currentWeatherFunction)
                    .call()
                    .content();
            result = new ChatAssistantResult(INTENT_GENERAL_CHAT, fallbackReply);
        }

        String intent = normalizeIntent(result.intent(), trimmedMessage);
        String aiReplyText = normalizeReply(intent, result.message());

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content(aiReplyText)
                .build());

        return new ChatResponse(intent, aiReplyText, session.getId(), null);
    }

    public AiPlanResponse generateTravelPlan(User user, String request, List<ChatMessage> history) {
        log.debug("[AI-MODEL] generateTravelPlan -> plan model={}", planModelName);

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
            log.debug("[AI-MODEL] fallback attempt with general model={}", planFallbackModelName);

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
                        "You are a strict travel planner. Return JSON only.\n"
                                + "Rules:\n"
                                + "1) Use realistic place names.\n"
                                + "2) Keep route flow practical.\n"
                                + "3) Keep memo concise and useful.\n"
                                + "4) Return valid JSON only.\n{formatInstructions}")
                        .param("formatInstructions", converter.getFormat()))
                .user("Final user request: " + request + "\n\nConversation history:\n" + historySummary)
                .call()
                .entity(converter);
    }

    private String buildRecentHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "(대화 이력 없음)";
        }

        int fromIndex = Math.max(0, history.size() - historyWindowMessages);
        return history.subList(fromIndex, history.size()).stream()
                .map(msg -> {
                    String role = "USER".equalsIgnoreCase(msg.getSenderRole()) ? "사용자" : "어시스턴트";
                    return role + ": " + msg.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    private String normalizeIntent(String intent, String latestUserMessage) {
        if (intent != null && INTENT_PLAN_CREATION.equalsIgnoreCase(intent.trim())) {
            return INTENT_PLAN_CREATION;
        }

        String normalizedUserText = latestUserMessage == null ? "" : latestUserMessage.toLowerCase(Locale.ROOT);
        if (normalizedUserText.contains("여행") && normalizedUserText.contains("일정")) {
            return INTENT_PLAN_CREATION;
        }
        return INTENT_GENERAL_CHAT;
    }

    private String normalizeReply(String intent, String message) {
        if (INTENT_PLAN_CREATION.equals(intent)) {
            return PLAN_FORM_GUIDE_MESSAGE;
        }
        if (message == null || message.isBlank()) {
            return "무엇을 도와드릴까요?";
        }
        return message.trim();
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
