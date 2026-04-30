package com.impact.lessons.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expired");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
