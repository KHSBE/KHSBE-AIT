package com.compass.domain.agent.domain;

import com.compass.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_quota_daily_resets",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_quota_daily_resets_user_date", columnNames = {"user_id", "quota_date"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserQuotaDailyReset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Column(name = "plan_used_baseline", nullable = false)
    private Integer planUsedBaseline;

    @Column(name = "chat_used_baseline", nullable = false)
    private Integer chatUsedBaseline;

    public void updateBaselines(int planUsedBaseline, int chatUsedBaseline) {
        this.planUsedBaseline = Math.max(0, planUsedBaseline);
        this.chatUsedBaseline = Math.max(0, chatUsedBaseline);
    }
}
