package com.compass.domain.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "auth_grants")
@Getter
@Setter
@NoArgsConstructor
public class AuthGrant {
  @Id
  @Column(length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, length = 36)
  private String familyId;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean used;
}
