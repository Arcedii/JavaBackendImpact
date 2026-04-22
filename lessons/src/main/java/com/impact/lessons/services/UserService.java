package com.impact.lessons.services;

import com.impact.lessons.config.jwt.JwtUtil;
import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.dto.RefreshRequest;
import com.impact.lessons.dto.UserDto;
import com.impact.lessons.dto.UserPersonalDataDto;
import com.impact.lessons.entity.*;
import com.impact.lessons.model.AuthenticationResponse;
import com.impact.lessons.repository.RoleRepository;
import com.impact.lessons.repository.UserPersonalDataRepository;
import com.impact.lessons.repository.UserRepository;
import com.impact.lessons.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserPersonalDataRepository userPersonalDataRepository;

    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    @Transactional
    public void createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ROLE_USER");
                    return roleRepository.save(newRole);
                });

        newUser.setRoles(Collections.singleton(userRole));

        userRepository.save(newUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserPersonalData updatePersonalData(String username, UserPersonalDataDto personalDataDto) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        UserPersonalData personalData = userPersonalDataRepository.findById(user.getId())
                .orElse(new UserPersonalData());

        personalData.setUser(user);
        personalData.setFirstName(personalDataDto.getFirstName());
        personalData.setLastName(personalDataDto.getLastName());
        personalData.setBirthDate(personalDataDto.getBirthDate());

        return userPersonalDataRepository.save(personalData);
    }

    public AuthenticationResponse loginUser(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        User user = userRepository.findByUsername(username);
        UserPersonalData personalData = userPersonalDataRepository.findById(user.getId()).orElse(null);

        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse(null);

        String accessToken = jwtUtil.generateToken(user, personalData, role);
        RefreshToken refreshToken = refreshTokenService.createOrUpdateRefreshToken(user.getId());

        return new AuthenticationResponse(accessToken, refreshToken.getToken());
    }

    public AuthenticationResponse refreshToken(RefreshRequest refreshRequest) {
        return refreshTokenService.findByToken(refreshRequest.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserPersonalData personalData = userPersonalDataRepository.findById(user.getId()).orElse(null);
                    String role = user.getRoles().stream()
                            .map(Role::getName)
                            .findFirst()
                            .orElse(null);
                    String accessToken = jwtUtil.generateToken(user, personalData, role);
                    return new AuthenticationResponse(accessToken, refreshRequest.getRefreshToken());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
