package com.compass.domain.user.dto;

import lombok.Builder;


@Builder
// 사용자 정보 응답
public record UserResponse(
        Long id,
        String email,
        String nickname
) {}