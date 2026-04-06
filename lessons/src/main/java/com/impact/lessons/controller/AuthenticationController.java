package com.impact.lessons.controller;

import com.impact.lessons.config.jwt.JwtUtil;
import com.impact.lessons.model.AuthenticationRequest;
import com.impact.lessons.model.AuthenticationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager; // Injectează managerul de autentificare.

    @Autowired
    private JwtUtil jwtUtil; // Injectează utilitarul pentru JWT.

    @Autowired
    private UserDetailsService userDetailsService; // Injectează serviciul pentru detalii utilizator.

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        // Autentifică utilizatorul folosind username-ul și parola.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
        );

        // Încarcă detaliile utilizatorului.
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());

        // Generează token-ul JWT.
        final String jwt = jwtUtil.generateToken(userDetails);

        // Returnează token-ul JWT în răspuns.
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }
}
