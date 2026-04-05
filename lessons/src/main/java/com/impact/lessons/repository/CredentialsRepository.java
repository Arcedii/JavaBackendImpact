package com.impact.lessons.repository;

import com.impact.lessons.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface CredentialsRepository extends JpaRepository<UserCredentials, Long> {
}
