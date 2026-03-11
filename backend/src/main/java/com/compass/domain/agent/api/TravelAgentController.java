package com.compass.domain.agent.api;

import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.plan.application.PlanCommandService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class TravelAgentController {

    private final TravelAgentService travelAgentService;
    private final PlanCommandService planCommandService;
    private final UserRepository userRepository;

    @PostMapping("/plan")
    public ApiResponse<Long> createPlanByAi(
            @RequestBody PlanRequest planRequest,
            @AuthenticationPrincipal OAuth2User oAuth2User) { // 로그인한 유저 정보 주입

        // 1. 로그인한 유저 찾기
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. AI에게 일정 생성 요청 (유저의 취향 반영)
        String aiResponse = travelAgentService.generateTravelPlan(user, planRequest.getRequest());

        // 3. 받은 JSON을 파싱해서 DB에 저장
        Long savedPlanId = planCommandService.savePlanFromAi(user, aiResponse);

        return ApiResponse.success(savedPlanId);
    }
}