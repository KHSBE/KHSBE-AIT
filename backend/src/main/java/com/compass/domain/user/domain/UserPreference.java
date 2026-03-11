package com.compass.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String preferredCategory; // 예: 맛집, 휴양
    private String dietRestriction;    // 예: 다이어트 중(고칼로리 제외)
    private Integer dailyBudget;
    private String travelPace;         // 타이트한, 여유로운
}