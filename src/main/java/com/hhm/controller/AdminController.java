package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.dto.AssignRoleRequest;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.Admin;
import com.hhm.model.User;
import com.hhm.repository.AdminRepository;
import com.hhm.repository.UserRepository;
import com.hhm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")   // every endpoint in this controller requires ADMIN
@Tag(name = "Admin", description = "Admin-only operations: user management and system overview")
public class AdminController {

    private final AuthService authService;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    public AdminController(AuthService authService,
                           AdminRepository adminRepository,
                           UserRepository userRepository) {
        this.authService = authService;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "List all system users",
               description = "Returns every registered user across all types (Doctor, Patient, Admin, Nurse, Receptionist).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users returned successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResult<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(ApiResult.success(users.size() + " user(s) found", users));
    }

    @GetMapping("/admins")
    @Operation(summary = "List all admin accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admins returned successfully")
    })
    public ResponseEntity<ApiResult<List<Admin>>> getAllAdmins() {
        List<Admin> admins = adminRepository.findAll();
        return ResponseEntity.ok(ApiResult.success(admins.size() + " admin(s) found", admins));
    }

    @PostMapping
    @Operation(summary = "Create a new admin account",
               description = "Only existing admins can create new admin accounts.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin created successfully"),
        @ApiResponse(responseCode = "409", description = "Email already in use"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> createAdmin(@Valid @RequestBody RegisterRequest req) {
        req.setType(Type.ADMIN);
        authService.createUser(req);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Admin account created successfully", null));
    }

    // ADMIN can update their own account only
    @PreAuthorize("hasAuthority('ADMIN') and authentication.principal.id == #id")
    @PutMapping("/{id}")
    @Operation(summary = "Update admin name",
               description = "An admin can only update their own account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin updated"),
        @ApiResponse(responseCode = "403", description = "Cannot update another admin's account"),
        @ApiResponse(responseCode = "404", description = "Admin not found")
    })
    public ResponseEntity<ApiResult<Void>> updateAdmin(
            @PathVariable long id,
            @RequestParam String name) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin with id " + id + " not found"));
        admin.setName(name);
        adminRepository.save(admin);
        return ResponseEntity.ok(ApiResult.success("Admin " + id + " updated", null));
    }

    @GetMapping("/users/pending")
    @Operation(summary = "List pending Google users",
               description = "Returns all users who signed in via Google and have not yet been assigned a role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pending users returned successfully")
    })
    public ResponseEntity<ApiResult<List<User>>> getPendingUsers() {
        List<User> pending = userRepository.findByUserTypeIsNull();
        return ResponseEntity.ok(ApiResult.success(pending.size() + " pending user(s)", pending));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Assign role to a pending Google user",
               description = "Deletes the base user row and re-creates it as the correct subclass. " +
                             "Provide phone for PATIENT, licenseNumber (and optionally departmentId) for DOCTOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role assigned and user promoted successfully"),
        @ApiResponse(responseCode = "400", description = "Missing required fields for the chosen role"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "User already has a role")
    })
    public ResponseEntity<ApiResult<User>> assignRole(
            @PathVariable long id,
            @Valid @RequestBody AssignRoleRequest req) {
        User promoted = authService.promoteOAuthUser(id, req);
        return ResponseEntity.ok(ApiResult.success(
                "User promoted to " + req.getRole(), promoted));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete any user account",
               description = "Permanently removes a user. Admin cannot delete their own account.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete your own account"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable long id,
            org.springframework.security.core.Authentication auth) {
        User principal = (User) auth.getPrincipal();
        if (principal.getId() == id) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
