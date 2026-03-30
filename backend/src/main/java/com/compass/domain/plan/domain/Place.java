package com.compass.domain.plan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    @JsonIgnore
    private Itinerary itinerary;

    private String placeName;
    private String googlePlaceId;
    private Double latitude;  // 위도 (구글 맵 필수)
    private Double longitude; // 경도 (구글 맵 필수)
    private Integer visitOrder;

    @Column(columnDefinition = "TEXT")
    private String memo;
}