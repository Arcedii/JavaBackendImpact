package com.impact.lessons.services;

import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.dto.UserDto;
import com.impact.lessons.entity.User;
import com.impact.lessons.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Injectează repozitoriul pentru utilizatori.

    @Autowired
    private PasswordEncoder passwordEncoder; // Injectează codificatorul de parole.

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username); // Caută utilizatorul în baza de date după username.
        if (user == null) {
            throw new UsernameNotFoundException("Utilizator nu a fost găsit cu acest nume de utilizator: " + username); // Aruncă o excepție dacă utilizatorul nu este găsit.
        }
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), new ArrayList<>()); // Returnează un obiect UserDetails.
    }

    public void createUser(CreateUserRequest request) {
        // Verificăm dacă numele de utilizator este deja ocupat
        if (userRepository.findByUsername(request.getFirstName()) != null) {
            throw new RuntimeException("Eroare: Numele de utilizator este deja ocupat!");
        }
        User newUser = new User(); // Creează o nouă instanță de utilizator.
        newUser.setUsername(request.getFirstName()); // Setează username-ul.
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Codifică și setează parola.
        userRepository.save(newUser); // Salvează noul utilizator în baza de date.
    }

    /**
     * Obține toți utilizatorii din baza de date și îi convertește într-un DTO sigur.
     * @return Lista de utilizatori fără parole.
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream() // Obținem toți utilizatorii din repozitoriu
                .map(user -> new UserDto(user.getId(), user.getUsername())) // Convertim fiecare User în UserDto
                .collect(Collectors.toList()); // Îi colectăm într-o listă
    }
}
