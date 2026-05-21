package com.impact.lessons.controller;

import com.impact.lessons.dto.AssignRoleRequest;
import com.impact.lessons.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.impact.lessons.config.OpenApiConfig;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Operații rezervate rolului ADMIN")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Health check admin")
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Admin access granted");
    }

    @Operation(summary = "Atribuire rol utilizator")
    @PostMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignRole(@PathVariable Long userId, @RequestBody AssignRoleRequest request) {
        userService.assignRoleToUser(userId, request);
        return ResponseEntity.ok("Rolul a fost actualizat cu succes");
    }
}
