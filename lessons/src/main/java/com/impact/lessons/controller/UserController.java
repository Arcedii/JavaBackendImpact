package com.impact.lessons.controller;

import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.dto.UserDto;
import com.impact.lessons.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users") // Toate cererile către acest controler vor începe cu /api/users
public class UserController {

    @Autowired
    private UserService userService; // Injectăm serviciul nostru pentru lucrul cu utilizatorii

    /**
     * Această metodă va procesa cererile POST către /api/users/register.
     * Este accesibilă public.
     * @param createUserRequest Datele pentru înregistrare (JSON)
     * @return Răspuns despre înregistrarea cu succes
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody CreateUserRequest createUserRequest) {
        // Apelăm metoda serviciului pe care am scris-o anterior pentru a crea utilizatorul
        userService.createUser(createUserRequest);
        // Returnăm un răspuns "200 OK" cu un mesaj
        return ResponseEntity.ok("User registered successfully!");
    }

    /**
     * Această metodă va procesa cererile GET către /api/users.
     * Este PROTEJATĂ și necesită un token JWT valid.
     * @return Lista tuturor utilizatorilor.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        // Apelăm metoda serviciului pentru a obține toți utilizatorii
        List<UserDto> users = userService.getAllUsers();
        // Returnăm lista de utilizatori și statusul 200 OK
        return ResponseEntity.ok(users);
    }
}
