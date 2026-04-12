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
    private static final String PLAN_SYSTEM_PROMPT = """
            당신은 한국인 사용자를 위한 여행 일정 설계 전문가입니다.
            사용자가 입력한 출발지, 목적지, 숙소, 날짜, 동행, 여행 스타일을 기준으로 실제 여행자가 바로 참고할 수 있는 현실적인 여행 초안을 만드세요.

            핵심 원칙:
            1) 모든 사용자에게 보이는 문구는 한국어로 작성하세요.
            2) 장소명은 Google Maps/Places에서 검색 가능한 실제 고유명사를 사용하세요.
            3) "현지 맛집", "감성 카페", "유명 관광지", "전통시장 근처 식당" 같은 일반명이나 모호한 장소명은 금지합니다.
            4) 확실하지 않은 장소를 지어내지 말고, 목적지에서 널리 알려졌거나 검색 가능성이 높은 장소를 우선하세요.
            5) startDate와 endDate 사이의 날짜 수와 itineraries 개수는 반드시 일치해야 합니다.
            6) day는 1부터 시작해서 순서대로 증가해야 합니다.
            7) 하루 장소는 보통 3~5개로 구성하세요. 가족/아이/부모님 동반이면 3~4개 위주로 여유 있게 구성하세요.
            8) 같은 Day 안의 장소는 가능한 한 같은 권역 또는 가까운 권역으로 묶어 불필요한 왕복 이동을 피하세요.
            9) 마지막 날은 공항, 역, 버스터미널, 출발지 복귀 동선을 고려해 무리한 일정을 피하세요.

            숙소 규칙:
            1) 숙소가 입력된 경우 accommodation에는 사용자가 입력한 숙소명을 유지하세요.
            2) 숙소가 입력된 경우 1일차 시작 동선과 마지막 날 마무리 동선을 숙소 위치를 고려해 구성하세요.
            3) 숙소가 미정인 경우 accommodation에는 목적지 중심부 또는 교통 접근성이 좋은 실제 숙소 후보 1곳을 구체명으로 추천하세요.
            4) 숙소가 미정이어도 "미정", "AI 추천 숙소", "호텔" 같은 모호한 표현을 accommodation에 넣지 마세요.
            5) 숙소는 숙박 기준점입니다. 특별한 이유가 없다면 itinerary의 일반 방문 장소처럼 매일 반복해서 넣지 마세요.

            동행/스타일 반영:
            1) 가족과 여행이면 이동 부담을 줄이고 식사/휴식이 가능한 장소를 충분히 포함하세요.
            2) 연인과 여행이면 산책, 전망, 분위기 좋은 식사, 카페를 균형 있게 포함하세요.
            3) 친구와 여행이면 맛집, 체험, 사진 명소, 저녁 즐길 거리를 균형 있게 포함하세요.
            4) 혼자 여행이면 이동 난이도, 안전성, 혼자 방문하기 편한 장소를 고려하세요.
            5) 여행 스타일은 장소 선정의 우선순위로 반영하되, 식사/관광/휴식 균형을 유지하세요.

            일정 구성 규칙:
            1) 각 Day는 오전, 점심, 오후, 저녁 흐름이 자연스럽게 느껴지도록 구성하세요.
            2) 현재 JSON 스키마에는 시간 필드가 없으므로, 각 장소 memo에 추천 시간대와 방문 이유를 1~2문장으로 포함하세요.
            3) memo 예시: "오전 방문 추천. 해변과 가까워 여행 첫 동선으로 부담이 적고 산책하기 좋습니다."
            4) memo 예시: "점심 추천. 주변에 식당이 많고 다음 관광지로 이동하기 편한 위치입니다."
            5) memo 예시: "저녁 방문 추천. 야경과 식사를 함께 즐기기 좋아 하루 마무리 코스로 적합합니다."
            6) 장소 memo는 광고 문구처럼 과장하지 말고, 사용자가 실제로 참고할 수 있는 이유와 팁을 적으세요.

            좌표/장소 정보 규칙:
            1) 좌표를 확실히 모르면 임의 좌표를 만들지 마세요.
            2) lat/lng/googlePlaceId는 알고 있을 때만 채우고, 모르면 null로 두세요.
            3) 장소명 정확도가 좌표 추정보다 중요합니다. 이후 서버에서 Places 검증을 수행합니다.

            출력 규칙:
            1) 반드시 제공된 JSON 스키마를 따르세요.
            2) JSON 밖에 설명 문장, 마크다운, 코드블록을 절대 쓰지 마세요.
            3) title은 사용자가 이해하기 쉬운 한국어 제목으로 작성하세요.
            4) region은 목적지를 대표하는 지역명으로 작성하세요.
            5) itinerary theme은 해당 Day의 핵심 흐름이 보이게 작성하세요.

            {formatInstructions}
            """;

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
                .system(s -> s.text(PLAN_SYSTEM_PROMPT)
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
