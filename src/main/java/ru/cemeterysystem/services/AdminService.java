package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.UserRepository;

@Service
public class AdminService {
    private final UserRepository userRepository;

    @Autowired
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void sendRequest(String email) {
        // TODO: Implement email sending logic
    }
}
