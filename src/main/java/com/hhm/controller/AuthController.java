package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.dto.LoginRequest;
import com.hhm.dto.LoginResponse;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.User;
import com.hhm.service.AuthService;
import com.hhm.service.BlackListService;
import com.hhm.service.JwtService;
import com.hhm.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication Controller", description = "Handles user authentication and registration")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuthService authService;
    private final BlackListService blackListService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UserService userService,
            AuthService authService,
            BlackListService blackListService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.authService = authService;
        this.blackListService = blackListService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Parameter(description = "User login request containing username and password")
            @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        // 3. Generate JWT token
        String jwtToken = jwtService.generateToken(userDetails);

        // 4. Return token in response
        return ResponseEntity.ok(new LoginResponse(jwtToken, 36000L)).getBody();

    }

    @PostMapping("/logout")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            blackListService.addToBlackList(jwt, jwtService.extractExpiration(jwt));
        }
        return ResponseEntity.ok(ApiResult.success("Logged out successfully", null));
    }

    @ApiResponses(
            {@ApiResponse(responseCode = "201", description = "User registered successfully"),
                    @ApiResponse(responseCode = "409", description = "Username or email already exists")
            })

    @PostMapping("/register")
    public ResponseEntity<ApiResult<User>> register(
            @Parameter(description = "Self-registration — only PATIENT accounts. Use admin endpoints to create other roles.")
            @Valid @RequestBody RegisterRequest request
    ) {
        if (request.getType() != Type.PATIENT) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Public registration is restricted to PATIENT accounts. Contact an admin to create other roles."));
        }
        User user = authService.createUser(request);
        return ResponseEntity.status(201).body(ApiResult.created("Patient registered successfully", user));
    }
}
