package com.impact.lessons.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_status")
@Getter
@Setter

public class UserStatus {
    @Id
    private Long userID;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime deleteadAt;
}
