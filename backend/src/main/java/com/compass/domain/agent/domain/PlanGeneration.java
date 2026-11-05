package com.compass.domain.agent.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "plan_generations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanGeneration {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "request_id", nullable = false, length = 36)
  private String requestId;

  @Column(nullable = false, length = 64)
  private String fingerprint;

  @Column(nullable = false)
  private LocalDate quotaDate;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private Instant expiresAt;

  private Long planId;

  @Column(nullable = false)
  private int attempt;
}
