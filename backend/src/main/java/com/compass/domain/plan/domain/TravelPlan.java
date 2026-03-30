package com.compass.domain.plan.domain;

import com.compass.domain.user.domain.User;
import com.compass.global.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 여행 계획 엔티티
@Entity
@Table(name = "travel_plans")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TravelPlan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String region;

    // ★ 새로 추가된 필드들 (프론트엔드 모달과 동기화)
    private String departure; // 출발지 (예: 부산)
    private String companion; // 일행 (예: 친구와)
    private String style;     // 여행 테마 (예: 맛집 탐방)
    private String accommodation; // ★ 새로 추가! (예: 신라호텔, 미정)

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Itinerary> itineraries = new ArrayList<>();
}