package com.compass.domain.agent.api;

import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.plan.application.PlanCommandService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class TravelAgentController {

    private final TravelAgentService agentService;
    private final PlanCommandService planService;
    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @PostMapping("/plan")
    @Transactional
    public ApiResponse<AiPlanResponse> createPlan(@Valid @RequestBody PlanRequest req, Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        var trimmedRequest = req.request().trim();
        var session = (req.sessionId() != null)
                ? sessionRepository.findByIdAndUserId(req.sessionId(), user.getId())
                .orElseThrow(() -> new BusinessException("해당 채팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND))
                : sessionRepository.save(ChatSession.builder()
                .user(user)
                .sessionTitle(trimmedRequest.substring(0, Math.min(trimmedRequest.length(), 10)) + "...")
                .build());

        var history = messageRepository.findAllBySessionIdOrderBySentAtAsc(session.getId());

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("USER")
                .content(trimmedRequest)
                .build());

        AiPlanResponse aiRes = agentService.generateTravelPlan(user, trimmedRequest, history);

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content("요청하신 일정 초안 생성을 완료했습니다.")
                .build());

        planService.savePlanFromAi(user, aiRes);

        return ApiResponse.success(aiRes);
    }
}
