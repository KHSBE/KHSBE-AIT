package com.compass.domain.admin.application;

import com.compass.global.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAuthorizationService {

    private final Set<String> adminEmails;

    public AdminAuthorizationService(
            @Value("${app.admin.emails:}") String adminEmails
    ) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    public String assertAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        if (adminEmails.isEmpty()) {
            throw new BusinessException("관리자 이메일이 설정되지 않았습니다. ADMIN_EMAILS를 설정해 주세요.", HttpStatus.FORBIDDEN);
        }

        String email = authentication.getName().trim().toLowerCase();
        if (!adminEmails.contains(email)) {
            throw new BusinessException("관리자 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return email;
    }
}
