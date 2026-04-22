package com.impact.lessons.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationResponse {

    private String accessToken;
    private String refreshToken;

    public AuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
