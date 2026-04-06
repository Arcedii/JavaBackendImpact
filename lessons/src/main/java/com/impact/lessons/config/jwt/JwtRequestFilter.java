package com.impact.lessons.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService; // Serviciu pentru încărcarea detaliilor utilizatorului.

    @Autowired
    private JwtUtil jwtUtil; // Utilitar pentru lucrul cu token-uri JWT.

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization"); // Extrage header-ul "Authorization".

        String username = null; // Inițializează username-ul ca nul.
        String jwt = null; // Inițializează token-ul JWT ca nul.

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) { // Verifică dacă header-ul există și începe cu "Bearer ".
            jwt = authorizationHeader.substring(7); // Extrage token-ul JWT (ștergând "Bearer ").
            username = jwtUtil.extractUsername(jwt); // Extrage username-ul din token.
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) { // Verifică dacă username-ul a fost extras și nu există deja o autentificare în contextul de securitate.

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username); // Încarcă detaliile utilizatorului folosind username-ul extras.

            if (jwtUtil.validateToken(jwt, userDetails)) { // Validează token-ul.

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()); // Creează un token de autentificare.
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // Setează detaliile de autentificare web.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken); // Setează autentificarea în contextul de securitate.
            }
        }
        chain.doFilter(request, response); // Pasează cererea și răspunsul următorului filtru din lanț.
    }
}
