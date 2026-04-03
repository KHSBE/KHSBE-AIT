package com.compass.domain.agent.domain;

import com.compass.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_quota_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_quota_settings_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserQuotaSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "plan_per_day_limit")
    private Integer planPerDayLimit;

    @Column(name = "chat_per_day_limit")
    private Integer chatPerDayLimit;

    public void update(Integer planPerDayLimit, Integer chatPerDayLimit) {
        this.planPerDayLimit = planPerDayLimit;
        this.chatPerDayLimit = chatPerDayLimit;
    }
}
