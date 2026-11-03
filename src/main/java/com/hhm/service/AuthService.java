package com.hhm.service;

import com.hhm.dto.RegisterRequest;
import com.hhm.model.User;
import com.hhm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;

    public AuthService(UserRepository userRepository, UserFactory userFactory) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
    }

    @Transactional
    public User createUser(RegisterRequest request) {
        User user = userFactory.create(request);
        return userRepository.save(user);
    }
}
