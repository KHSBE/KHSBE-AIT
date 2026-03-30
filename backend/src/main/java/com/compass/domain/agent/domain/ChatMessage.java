package com.compass.domain.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // ★ 추가됨
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @JsonIgnore // ★ JSON 변환 시 Session 제외 (무한루프 방지)
    private ChatSession session;

    @Column(nullable = false)
    private String senderRole; // USER, ASSISTANT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime sentAt;

    @Builder
    public ChatMessage(ChatSession session, String senderRole, String content) {
        this.session = session;
        this.senderRole = senderRole;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}