package com.impact.lessons.config;

import com.impact.lessons.config.jwt.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Returnează un bean pentru codificarea parolelor folosind BCrypt.
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager(); // Expune AuthenticationManager-ul ca un bean.
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Dezactivează protecția CSRF, deoarece folosim token-uri JWT.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/authenticate", "/api/users/register").permitAll() // Permite accesul neautentificat la endpoint-urile de autentificare și înregistrare.
                        .anyRequest().authenticated() // Toate celelalte cereri necesită autentificare.
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Configurează managementul sesiunii pentru a fi stateless, deoarece folosim JWT.
                );
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class); // Adaugă filtrul JWT înaintea filtrului de autentificare standard.
        return http.build(); // Construiește și returnează lanțul de filtre de securitate.
    }
}
