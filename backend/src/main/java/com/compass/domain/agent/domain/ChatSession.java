package com.compass.domain.agent.domain;

import com.compass.domain.user.domain.User;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private TravelPlan travelPlan; // 계획 생성 후 연결될 수 있도록 null 허용

    private String sessionTitle;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public ChatSession(User user, TravelPlan travelPlan, String sessionTitle) {
        this.user = user;
        this.travelPlan = travelPlan;
        this.sessionTitle = sessionTitle;
    }
}