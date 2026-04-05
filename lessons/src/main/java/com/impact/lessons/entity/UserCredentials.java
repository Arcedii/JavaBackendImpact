package com.impact.lessons.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter

public class UserCredentials {
  @Id
  private Long userId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(unique = true,nullable = false)
    private String passwordHash;
}
