package com.impact.lessons.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    private String username; // Numele de utilizator pentru autentificare.
    private String password; // Parola pentru autentificare.
}
