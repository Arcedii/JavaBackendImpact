package com.impact.lessons.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleID.class)
@Getter
@Setter
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Short roleId;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

}
