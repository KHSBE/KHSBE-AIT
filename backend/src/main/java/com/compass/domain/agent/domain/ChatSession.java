package com.compass.domain.agent.domain;

import com.compass.domain.user.domain.User;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.global.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore; // ★ 추가됨
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
    @JsonIgnore // ★ JSON 변환 시 User 제외 (에러 방지)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnore // ★ JSON 변환 시 TravelPlan 제외
    private TravelPlan travelPlan;

    private String sessionTitle;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @JsonIgnore // ★ JSON 변환 시 Message 리스트 제외 (무한루프 방지)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public ChatSession(User user, TravelPlan travelPlan, String sessionTitle) {
        this.user = user;
        this.travelPlan = travelPlan;
        this.sessionTitle = sessionTitle;
    }
}