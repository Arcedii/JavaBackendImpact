package com.impact.lessons.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String accessToken;
    private String refreshToken;
}
