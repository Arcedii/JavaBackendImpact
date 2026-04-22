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

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    @Autowired
    private UserService userService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {
        AuthenticationResponse response = userService.loginUser(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        return ResponseEntity.ok(userService.refreshToken(refreshRequest));
    }
}
