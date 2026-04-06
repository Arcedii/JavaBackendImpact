package com.impact.lessons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) pentru a transfera datele utilizatorului fără parolă.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id; // ID-ul utilizatorului
    private String username; // Numele de utilizator
}
