package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.user.domain.User;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.agent.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
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

    public TravelAgentService(ChatClient chatClient, ToolCallback currentWeatherFunction,
                              ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.chatClient = chatClient;
        this.currentWeatherFunction = currentWeatherFunction;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    record IntentClassification(String intent) {}

    @Transactional
    public ChatResponse chatWithAgent(User user, Long sessionId, String userMessage) {
        // 1. 세션 조회 또는 새 세션 생성
        ChatSession session;
        if (sessionId == null) {
            session = sessionRepository.save(ChatSession.builder()
                    .user(user)
                    .sessionTitle(userMessage.length() > 10 ? userMessage.substring(0, 10) + "..." : userMessage)
                    .build());
        } else {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션입니다."));
        }

        // 2. 유저 메시지 DB 저장
        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("USER")
                .content(userMessage)
                .build());

        // 3. 이전 대화 내역 불러와서 하나의 문자열로 묶기 (AI 문맥용)
        List<ChatMessage> history = messageRepository.findAllBySessionIdOrderBySentAtAsc(session.getId());
        String conversationHistory = history.stream()
                .map(msg -> msg.getSenderRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 4. 의도 분류
        BeanOutputConverter<IntentClassification> intentConverter = new BeanOutputConverter<>(IntentClassification.class);
        IntentClassification classification = chatClient.prompt()
                .system(s -> s.text("사용자 메시지 의도를 분류하세요. 일정 요청은 'PLAN_CREATION', 나머진 'GENERAL_CHAT'.\n{formatInstructions}")
                        .param("formatInstructions", intentConverter.getFormat()))
                .user(userMessage)
                .call()
                .entity(intentConverter);

        String aiReplyText;
        String intent = classification.intent();

        // 5. AI 응답 생성 (과거 대화 내역 주입!)
        if ("PLAN_CREATION".equals(intent)) {
            aiReplyText = "좋습니다! 완벽한 일정을 짜기 위해 화면 안내에 따라 정보를 입력해 주세요!";
        } else {
            aiReplyText = chatClient.prompt()
                    .system("당신은 'Compass'의 수석 여행 에이전트입니다. 친절하게 답변하세요.")
                    .user("이전 대화 내역:\n" + conversationHistory + "\n\n현재 사용자 메시지: " + userMessage)
                    .toolCallbacks(currentWeatherFunction)
                    .call()
                    .content();
        }

        // 6. AI 응답 DB 저장
        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content(aiReplyText)
                .build());

        // 7. 응답 반환 (생성된 세션 ID 포함)
        return new ChatResponse(intent, aiReplyText, session.getId(), null);
    }

    public AiPlanResponse generateTravelPlan(User user, String request, List<ChatMessage> history) {
        BeanOutputConverter<AiPlanResponse> converter = new BeanOutputConverter<>(AiPlanResponse.class);

        return chatClient.prompt()
                .system(s -> s.text(
                                "당신은 10년 이상의 경력을 가진 'Compass'의 수석 여행 플래너입니다. " +
                                        "사용자가 입력한 지역, 날짜, 일행, 취향, 숙소를 완벽하게 분석하여 최고 수준의 여행 일정을 JSON으로 작성하세요.\n\n" +
                                        "★ [일정 작성 절대 원칙 - 반드시 지킬 것]\n" +
                                        "1. 완벽한 숙소 복귀 동선: 사용자가 숙소를 지정했다면 그 숙소를, '미정'이라면 당신이 추천한 최적의 숙소 1곳을 베이스캠프로 삼으세요. **가장 중요: 마지막 날을 제외한 모든 일자(Day)의 마지막 방문 장소(places 배열의 마지막)는 반드시 '해당 숙소'로 지정하여 하루를 마무리하고 숙소로 복귀하는 동선을 시각적으로 완성하세요.** 마지막 날의 마지막 장소는 공항/터미널 등 집으로 돌아가는 출발지로 지정하세요.\n" +
                                        "2. 동선 최적화: 같은 날 방문하는 장소는 지리적으로 가까운 곳들로 묶어 이동 시간을 최소화하세요.\n" +
                                        "3. 구체적 상호명 필수: '근처 식당', '주변 카페', '오사카 맛집' 같은 모호한 표현은 절대 금지합니다! 구글 지도에 실제로 검색되는 정확한 상호명(명소, 식당, 카페 등)만 작성하세요.\n" +
                                        "4. 꽉 찬 일정: 하루에 최소 3~4개의 구체적인 장소(예: 오전 명소 -> 점심 식당 -> 오후 카페/쇼핑 -> 저녁 명소 -> 숙소 복귀)를 배치하세요.\n" +
                                        "5. 디테일한 꿀팁: 'memo' 필드에는 단순히 장소 설명을 넘어서 '여기는 야경이 멋지니 해 질 녘에 가세요', '이 식당에선 타코야끼를 꼭 드세요' 같은 실질적인 팁을 적으세요.\n\n" +
                                        "절대 부가적인 인사말을 쓰지 말고 아래 JSON 구조로만 응답하세요.\n{formatInstructions}")
                        .param("formatInstructions", converter.getFormat()))
                .user("사용자 최종 요청: " + request)
                .call()
                .entity(converter);
    }
}