package com.hhm.controller;

import com.hhm.dto.LoginRequest;
import com.hhm.dto.LoginResponse;
import com.hhm.dto.RegisterRequest;
import com.hhm.model.User;
import com.hhm.service.AuthService;
import com.hhm.service.JwtService;
import com.hhm.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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


    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UserService userService,
            AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.authService = authService;
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

    @ApiResponses(
            {@ApiResponse(responseCode = "201", description = "User registered successfully"),
                    @ApiResponse(responseCode = "409", description = "Username or email already exists")
            })

    @PostMapping("/register")
    public User register(
            @Parameter(description = "User registration request containing username, email, and password")
            @Valid @RequestBody RegisterRequest request
    ) {
       return authService.createUser(request);
    }
}
