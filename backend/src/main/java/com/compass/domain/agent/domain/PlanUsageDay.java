package com.compass.domain.agent.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "plan_usage_days",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quota_date"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanUsageDay {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "quota_date", nullable = false)
  private LocalDate quotaDate;

  @Column(nullable = false)
  private int used;
}
