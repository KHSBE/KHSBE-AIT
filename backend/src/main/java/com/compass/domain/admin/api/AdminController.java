package com.compass.domain.admin.api;

import com.compass.domain.admin.application.AdminAuthorizationService;
import com.compass.domain.admin.dto.AdminMeResponse;
import com.compass.domain.admin.dto.AdminQuotaUpdateRequest;
import com.compass.domain.admin.dto.AdminUserQuotaResponse;
import com.compass.domain.admin.dto.AdminUserSummaryResponse;
import com.compass.domain.agent.application.RequestQuotaService;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthorizationService adminAuthorizationService;
    private final UserRepository userRepository;
    private final RequestQuotaService requestQuotaService;

    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> getAdminMe(Authentication authentication) {
        String email = adminAuthorizationService.assertAdmin(authentication);
        return ApiResponse.success(new AdminMeResponse(email, true));
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserSummaryResponse>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "30") int limit,
            Authentication authentication
    ) {
        adminAuthorizationService.assertAdmin(authentication);

        int safeLimit = Math.max(1, Math.min(limit, 100));
        var pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "id"));
        var normalizedKeyword = keyword == null ? "" : keyword.trim();

        var page = normalizedKeyword.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                normalizedKeyword,
                normalizedKeyword,
                pageable
        );

        List<AdminUserSummaryResponse> users = page.getContent().stream()
                .map(user -> new AdminUserSummaryResponse(user.getId(), user.getEmail(), user.getNickname()))
                .toList();

        return ApiResponse.success(users);
    }

    @GetMapping("/users/{userId}/quota")
    public ApiResponse<AdminUserQuotaResponse> getUserQuota(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        adminAuthorizationService.assertAdmin(authentication);
        User user = getUser(userId);
        var detail = requestQuotaService.getQuotaAdminDetail(userId);

        AdminUserQuotaResponse response = new AdminUserQuotaResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                detail.defaultPlanPerDayLimit(),
                detail.overridePlanPerDayLimit(),
                detail.effectivePlanPerDayLimit(),
                detail.planPerDayUsed(),
                detail.planPerDayRemaining(),
                detail.defaultChatPerDayLimit(),
                detail.overrideChatPerDayLimit(),
                detail.effectiveChatPerDayLimit(),
                detail.chatPerDayUsed(),
                detail.chatPerDayRemaining()
        );
        return ApiResponse.success(response);
    }

    @PutMapping("/users/{userId}/quota")
    public ApiResponse<AdminUserQuotaResponse> updateUserQuota(
            @PathVariable Long userId,
            @Valid @RequestBody AdminQuotaUpdateRequest request,
            Authentication authentication
    ) {
        adminAuthorizationService.assertAdmin(authentication);
        getUser(userId);

        requestQuotaService.upsertUserQuotaLimit(userId, request.planPerDayLimit(), request.chatPerDayLimit());
        return getUserQuota(userId, authentication);
    }

    @DeleteMapping("/users/{userId}/quota")
    public ApiResponse<AdminUserQuotaResponse> resetUserQuota(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        adminAuthorizationService.assertAdmin(authentication);
        getUser(userId);

        requestQuotaService.clearUserQuotaLimit(userId);
        return getUserQuota(userId, authentication);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
