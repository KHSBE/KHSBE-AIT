package com.compass.global.common;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  // Liveness only: never call paid AI APIs or wake the database from a health probe.
  @GetMapping({"/", "/healthz"})
  public ResponseEntity<ApiResponse<Map<String, String>>> status() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.success(Map.of("service", "compass-backend", "status", "UP")));
  }
}
