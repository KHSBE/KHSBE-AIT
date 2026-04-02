package com.compass.domain.agent.api;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        User user = findUser(principal.getName());
        return ApiResponse.success(chatSessionRepository.findAllByUserIdWithPlan(user.getId()));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable Long sessionId, Principal principal) {
        User user = findUser(principal.getName());
        chatSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new BusinessException("해당 채팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return ApiResponse.success(chatMessageRepository.findAllBySessionIdAndSessionUserIdOrderBySentAtAsc(sessionId, user.getId()));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId, Principal principal) {
        User user = findUser(principal.getName());
        long deleted = chatSessionRepository.deleteByIdAndUserId(sessionId, user.getId());
        if (deleted == 0) {
            throw new BusinessException("해당 채팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        return ApiResponse.success(null);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}

