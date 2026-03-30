package com.compass.domain.user.api;

import com.compass.global.config.auth.JwtTokenProvider;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    // ★ 프론트엔드가 Refresh Token을 보내면 새 Access Token을 발급해주는 API
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            String email = jwtTokenProvider.getUserEmail(refreshToken);
            String newAccessToken = jwtTokenProvider.createToken(email);
            return ApiResponse.success(Map.of("accessToken", newAccessToken));
        }
        return ApiResponse.error("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
    }
}