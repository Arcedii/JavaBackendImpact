package com.impact.lessons.controller;

import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.dto.UserDto;
import com.impact.lessons.dto.UserPersonalDataDto;
import com.impact.lessons.entity.UserPersonalData;
import com.impact.lessons.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/me/personal-data")
    public ResponseEntity<UserPersonalData> updatePersonalData(Principal principal, @RequestBody UserPersonalDataDto personalDataDto) {
        String username = principal.getName();
        UserPersonalData updatedData = userService.updatePersonalData(username, personalDataDto);
        return ResponseEntity.ok(updatedData);
    }
}
