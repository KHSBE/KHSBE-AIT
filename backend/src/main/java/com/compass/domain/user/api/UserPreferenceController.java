package com.compass.domain.user.api;

import com.compass.domain.user.application.UserPreferenceService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.domain.user.dto.UserPreferenceRequest;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody UserPreferenceRequest request,
            Authentication authentication) {

        if (authentication == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        userPreferenceService.saveOrUpdate(user.getId(), request);
        return ApiResponse.success(null);
    }
}
