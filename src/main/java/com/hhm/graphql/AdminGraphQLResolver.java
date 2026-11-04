package com.hhm.graphql;

import com.hhm.dto.AssignRoleInput;
import com.hhm.dto.AssignRoleRequest;
import com.hhm.dto.RegisterInput;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.Admin;
import com.hhm.model.User;
import com.hhm.repository.AdminRepository;
import com.hhm.repository.UserRepository;
import com.hhm.service.AuthService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class AdminGraphQLResolver {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final AuthService authService;

    public AdminGraphQLResolver(UserRepository userRepository,
                                AdminRepository adminRepository,
                                AuthService authService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.authService = authService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @QueryMapping
    public List<User> users() {
        return userRepository.findAll();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @QueryMapping
    public List<Admin> admins() {
        return adminRepository.findAll();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @QueryMapping
    public List<User> pendingUsers() {
        return userRepository.findByUserTypeIsNull();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public String createAdmin(@Argument RegisterInput input) {
        RegisterRequest req = new RegisterRequest();
        req.setName(input.name());
        req.setEmail(input.email());
        req.setPassword(input.password());
        req.setType(Type.ADMIN);
        authService.createUser(req);
        return "Admin account created for " + input.email();
    }

    @PreAuthorize("hasAuthority('ADMIN') and authentication.principal.id == #id")
    @MutationMapping
    public boolean updateAdmin(@Argument long id, @Argument String name) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin " + id + " not found"));
        admin.setName(name);
        adminRepository.save(admin);
        return true;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public User assignRole(@Argument long id, @Argument AssignRoleInput input) {
        AssignRoleRequest req = new AssignRoleRequest();
        req.setRole(Type.valueOf(input.role().toUpperCase()));
        req.setPhone(input.phone());
        req.setLicenseNumber(input.licenseNumber());
        req.setDepartmentId(input.departmentId());
        return authService.promoteOAuthUser(id, req);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public boolean deleteUser(@Argument long id, Authentication auth) {
        User principal = (User) auth.getPrincipal();
        if (principal.getId() == id) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        userRepository.deleteById(id);
        return true;
    }
}
