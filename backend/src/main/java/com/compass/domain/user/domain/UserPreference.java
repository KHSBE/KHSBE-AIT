package com.compass.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;

// 사용자 여행 취향
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String preferredCategory;
    private String dietRestriction;
    private String travelPace;

    // 정보 업데이트
    public void update(String category, String diet, String pace) {
        this.preferredCategory = category;
        this.dietRestriction = diet;
        this.travelPace = pace;
    }
}