package com.impact.lessons.controller;

import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.dto.UserDto;
import com.impact.lessons.dto.UserPersonalDataDto;
import com.impact.lessons.entity.UserPersonalData;
import com.impact.lessons.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.impact.lessons.config.OpenApiConfig;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilizatori", description = "Înregistrare, listă utilizatori și date personale")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Înregistrare utilizator", description = "Public — nu necesită JWT.")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @Operation(summary = "Listă utilizatori", description = "Doar ADMIN.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Actualizare date personale", description = "Utilizatorul autentificat își actualizează profilul.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/me/personal-data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserPersonalData> updatePersonalData(Principal principal, @RequestBody UserPersonalDataDto personalDataDto) {
        String username = principal.getName();
        UserPersonalData updatedData = userService.updatePersonalData(username, personalDataDto);
        return ResponseEntity.ok(updatedData);
    }
}
