package com.compass.domain.agent.api;

import com.compass.domain.agent.application.RequestQuotaService;
import com.compass.domain.agent.dto.QuotaStatusResponse;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
  private final RequestQuotaService quota;
  private final UserRepository users;

  @GetMapping("/quota")
  public ApiResponse<QuotaStatusResponse> quota(Authentication auth) {
    if (auth == null) throw new BusinessException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
    var user =
        users
            .findByEmail(auth.getName())
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    return ApiResponse.success(quota.getQuotaStatus(user.getId()));
  }
}
