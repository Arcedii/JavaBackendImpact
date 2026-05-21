package com.impact.lessons.controller;

import com.impact.lessons.dto.RefreshRequest;
import com.impact.lessons.model.AuthenticationRequest;
import com.impact.lessons.model.AuthenticationResponse;
import com.impact.lessons.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Autentificare", description = "Login JWT și refresh token")
public class AuthenticationController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Login", description = "Returnează access token și refresh token pentru username/password.")
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {
        AuthenticationResponse response = userService.loginUser(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh token", description = "Generează token-uri noi pe baza refresh token-ului valid.")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        return ResponseEntity.ok(userService.refreshToken(refreshRequest));
    }
}
