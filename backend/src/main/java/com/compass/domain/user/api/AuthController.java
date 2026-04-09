package com.compass.domain.user.api;

import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import com.compass.global.config.auth.JwtTokenProvider;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        if (request == null || !request.containsKey("refreshToken")) {
            throw new BusinessException("refreshToken이 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            String email = jwtTokenProvider.getUserEmail(refreshToken);
            String role = userRepository.findByEmail(email)
                    .map(user -> user.getRoleOrDefault().name())
                    .orElseGet(() -> jwtTokenProvider.getUserRole(refreshToken));

            String newAccessToken = jwtTokenProvider.createToken(email, role);
            return ApiResponse.success(Map.of("accessToken", newAccessToken));
        }

        throw new BusinessException("유효한 refresh token이 아닙니다. 다시 로그인해 주세요.", HttpStatus.UNAUTHORIZED);
    }
}
