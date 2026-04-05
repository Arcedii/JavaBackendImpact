package com.impact.lessons.repository;

import com.impact.lessons.entity.UserCredentials;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository {
    Optional<UserCredentials> findUsername(String username);
}
