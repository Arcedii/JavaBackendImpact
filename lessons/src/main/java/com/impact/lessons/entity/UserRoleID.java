package com.impact.lessons.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserRoleID implements Serializable {

    private Long userId;
    private Short roleId;

    public UserRoleID() {}

    public UserRoleID(Long userId, Short roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleID that)) return false;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}