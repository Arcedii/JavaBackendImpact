package com.impact.lessons.repository;

import com.impact.lessons.entity.UserRole;
import com.impact.lessons.entity.UserRoleID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleID> {
    Optional<UserRole> findByUserId(Long userId);
}
