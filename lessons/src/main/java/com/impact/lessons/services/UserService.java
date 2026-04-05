package com.impact.lessons.services;

import com.impact.lessons.dto.CreateUserRequest;
import com.impact.lessons.entity.User;
import com.impact.lessons.repository.CredentialsRepository;
import com.impact.lessons.repository.UserPersonalDataRepository;
import com.impact.lessons.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final UserPersonalDataRepository personalDataRepository;

    public UserService(
            UserRepository userRepository,
            CredentialsRepository credentialsRepository,
            UserPersonalDataRepository personalDataRepository) {
        this.userRepository = userRepository;
        this.credentialsRepository = credentialsRepository;
        this.personalDataRepository = personalDataRepository;
    }

    public void createUser(CreateUserRequest request) {
        // 1. Создаем пустую коробку для нового пользователя (Entity)
        User newUser = new User();

        // 2. Перекладываем данные из запроса (Postman) в эту коробку
        // (названия методов set... зависят от того, как они называются в твоем классе User)
        newUser.setUsername(request.getFirstName()); // Например, используем имя как логин
        newUser.setPassword(request.getPassword());
        // newUser.setRole(1); // Если нужно задать роль по умолчанию

        // 3. Отдаем приказ кладовщику: "СОХРАНИ ЭТО В БАЗУ!"
        userRepository.save(newUser);

        // P.S. Если нужно, точно так же создаешь и сохраняешь
        // Credentials и UserPersonalData через их репозитории.
    }
}