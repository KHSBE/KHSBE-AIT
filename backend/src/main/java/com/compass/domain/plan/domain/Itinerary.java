package com.compass.domain.plan.domain;

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
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private TravelPlan travelPlan;

    private Integer dayNumber; // 1일차, 2일차...
    private LocalDate travelDate;
    private String theme;      // 해당 일차의 테마

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL)
    private List<Place> places = new ArrayList<>();

    @Builder
    public Itinerary(TravelPlan travelPlan, Integer dayNumber, LocalDate travelDate, String theme) {
        this.travelPlan = travelPlan;
        this.dayNumber = dayNumber;
        this.travelDate = travelDate;
        this.theme = theme;
    }
}