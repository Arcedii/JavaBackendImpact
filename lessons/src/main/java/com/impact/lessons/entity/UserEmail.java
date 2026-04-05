package com.impact.lessons.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_emails")
@Getter
@Setter

public class UserEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private boolean isPrimary;
    private LocalDateTime verifiedAt;
     @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
}
