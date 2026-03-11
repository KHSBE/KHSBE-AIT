package com.compass.domain.plan.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Builder 사용 시 모든 필드를 갖는 생성자가 필요해
@Builder // 이 녀석이 있어야 서비스에서 Place.builder()를 쓸 수 있어
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    private String placeName;
    private String googlePlaceId;
    private Double latitude;
    private Double longitude;
    private Integer visitOrder;

    @Column(columnDefinition = "TEXT")
    private String memo;           // AI 에이전트의 추천 사유
}