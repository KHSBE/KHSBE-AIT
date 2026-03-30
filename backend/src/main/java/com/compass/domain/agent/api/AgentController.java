package com.compass.domain.agent.api;

import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dto.ChatResponse;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final TravelAgentService travelAgentService;
    private final UserRepository userRepository;

    public AgentController(TravelAgentService travelAgentService, UserRepository userRepository) {
        this.travelAgentService = travelAgentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/chat")
    public ChatResponse chat(
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId,
            Principal principal) { // ★ 여기도 Principal로 변경!

        // 1. 현재 로그인한 유저 찾기 (principal.getName()은 보통 JWT 토큰 안의 이메일/ID를 반환합니다)
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. 서비스로 위임
        return travelAgentService.chatWithAgent(user, sessionId, message);
    }
}