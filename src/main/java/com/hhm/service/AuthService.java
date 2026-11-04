package com.hhm.service;

import com.hhm.dto.AssignRoleRequest;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.User;
import com.hhm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserFactory userFactory,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(RegisterRequest request) {
        User user = userFactory.create(request);
        return userRepository.save(user);
    }

    /**
     * Promotes a pending OAuth2 user to a real role.
     * Deletes the base User row and re-creates it as the correct subclass,
     * using the existing display_name as the name in the new record.
     */
    @Transactional
    public User promoteOAuthUser(long userId, AssignRoleRequest req) {
        User pending = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User " + userId + " not found"));

        if (pending.getUserType() != null) {
            throw new IllegalStateException("User " + userId + " already has role " + pending.getUserType());
        }

        validateRoleFields(req);

        String email = pending.getEmail();
        String name = pending.getDisplayName() != null ? pending.getDisplayName() : email;

        // Delete the base row; the subclass insert below will own the new ID
        userRepository.delete(pending);
        userRepository.flush();

        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setName(name);
        // OAuth2 users never log in with a password; random value keeps UserDetails contract
        register.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        register.setType(req.getRole());
        register.setPhone(req.getPhone());
        register.setLicenseNumber(req.getLicenseNumber());
        register.setDepartmentId(req.getDepartmentId());

        return createUser(register);
    }

    private void validateRoleFields(AssignRoleRequest req) {
        if (req.getRole() == Type.PATIENT && (req.getPhone() == null || req.getPhone().isBlank())) {
            throw new IllegalArgumentException("phone is required for PATIENT role");
        }
        if (req.getRole() == Type.DOCTOR && (req.getLicenseNumber() == null || req.getLicenseNumber().isBlank())) {
            throw new IllegalArgumentException("licenseNumber is required for DOCTOR role");
        }
    }
}
