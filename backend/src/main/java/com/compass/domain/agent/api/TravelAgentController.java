package com.compass.domain.agent.api;

import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.plan.application.PlanCommandService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

// 여행 에이전트 컨트롤러
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class TravelAgentController {

    private final TravelAgentService agentService;
    private final PlanCommandService planService;
    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    // ★ 1. 리턴 타입을 Long 에서 AiPlanResponse 로 변경합니다!
    @PostMapping("/plan")
    @Transactional
    public ApiResponse<AiPlanResponse> createPlan(@RequestBody PlanRequest req, Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        var session = (req.sessionId() != null)
                ? sessionRepository.findById(req.sessionId()).orElseThrow()
                : sessionRepository.save(ChatSession.builder()
                .user(user)
                .sessionTitle(req.request().substring(0, Math.min(req.request().length(), 10)) + "...")
                .build());

        var history = messageRepository.findAllBySessionIdOrderBySentAtAsc(session.getId());

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("USER")
                .content(req.request())
                .build());

        // AI 일정 생성 호출 (AiPlanResponse 객체 생성)
        AiPlanResponse aiRes = agentService.generateTravelPlan(user, req.request(), history);

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content("성공적으로 [" + aiRes.title() + "] 일정을 생성했습니다! DB에 저장 완료되었습니다.")
                .build());

        // ★ 2. DB에 저장은 기존처럼 똑같이 진행합니다.
        planService.savePlanFromAi(user, aiRes);

        // ★ 3. 핵심: 프론트엔드에서 화면을 예쁘게 그릴 수 있도록, 일정 데이터(aiRes)를 통째로 반환합니다!
        return ApiResponse.success(aiRes);
    }
}