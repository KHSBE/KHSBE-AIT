package com.compass.domain.plan.domain;

import com.compass.domain.user.domain.User;
import com.compass.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private String region;
    private LocalDate startDate;
    private LocalDate endDate;

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL)
    private List<Itinerary> itineraries = new ArrayList<>();

    @Builder
    public TravelPlan(User user, String title, String region, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.title = title;
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}