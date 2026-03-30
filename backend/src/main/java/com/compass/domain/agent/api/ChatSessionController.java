package com.compass.domain.agent.api;

import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<ChatSession>> getMySessions(Principal principal) {
        // ★ OAuth2User 대신 Principal을 사용하여 JWT 환경에서도 안전하게 이메일을 가져옵니다.
        User user = findUser(principal.getName());
        return ApiResponse.success(chatSessionRepository.findAllByUserIdWithPlan(user.getId()));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return ApiResponse.success(chatMessageRepository.findAllBySessionIdOrderBySentAtAsc(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        chatSessionRepository.deleteById(sessionId);
        return ApiResponse.success(null);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}