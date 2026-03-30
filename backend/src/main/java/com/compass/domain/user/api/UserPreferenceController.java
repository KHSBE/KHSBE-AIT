package com.compass.domain.user.api;

import com.compass.domain.user.application.UserPreferenceService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.domain.user.dto.UserPreferenceRequest;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/preference")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final UserRepository userRepository;

    @PostMapping
    public ApiResponse<Void> updatePreference(
            @RequestBody UserPreferenceRequest request,
            Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        userPreferenceService.saveOrUpdate(user.getId(), request);
        return ApiResponse.success(null);
    }
}