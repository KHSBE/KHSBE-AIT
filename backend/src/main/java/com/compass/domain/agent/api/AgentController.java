package com.compass.domain.agent.api;

import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dto.ChatResponse;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/agent")
@Validated
public class AgentController {

    private final TravelAgentService travelAgentService;
    private final UserRepository userRepository;

    public AgentController(TravelAgentService travelAgentService, UserRepository userRepository) {
        this.travelAgentService = travelAgentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/chat")
    public ChatResponse chat(
            @RequestParam @NotBlank(message = "message 파라미터는 필수입니다.") String message,
            @RequestParam(required = false) Long sessionId,
            Principal principal) {

        if (principal == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return travelAgentService.chatWithAgent(user, sessionId, message);
    }
}
