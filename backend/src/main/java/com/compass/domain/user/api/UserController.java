package com.compass.domain.user.api;

import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.domain.user.dto.UserResponse;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return ApiResponse.success(UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build());
    }
}