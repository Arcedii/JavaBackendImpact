package com.impact.lessons.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationResponse {

    private final String jwt; // Token-ul JWT generat.
}
