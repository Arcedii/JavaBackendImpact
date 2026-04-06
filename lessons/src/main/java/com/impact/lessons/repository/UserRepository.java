package com.impact.lessons.repository;

import com.impact.lessons.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username); // Găsește un utilizator după numele de utilizator.
}
