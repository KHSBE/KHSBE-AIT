package com.compass.domain.agent.api;

import com.compass.domain.agent.application.RequestQuotaService;
import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dto.ChatRequest;
import com.compass.domain.agent.dto.ChatResponse;
import com.compass.domain.agent.dto.QuotaStatusResponse;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final TravelAgentService travelAgentService;
    private final RequestQuotaService requestQuotaService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new BusinessException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found.", HttpStatus.NOT_FOUND));

        ChatResponse response = travelAgentService.chatWithAgent(user, request.sessionId(), request.message());
        return ApiResponse.success(response);
    }

    @GetMapping("/quota")
    public ApiResponse<QuotaStatusResponse> getQuota(
            Principal principal
    ) {
        if (principal == null) {
            throw new BusinessException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found.", HttpStatus.NOT_FOUND));

        QuotaStatusResponse response = requestQuotaService.getQuotaStatus(user.getId());
        return ApiResponse.success(response);
    }
}
