package com.hhm.graphql;

import com.hhm.dto.LoginResponse;
import com.hhm.dto.RegisterInput;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.User;
import com.hhm.service.AuthService;
import com.hhm.service.JwtService;
import com.hhm.service.RedisSecurityService;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class AuthGraphQLResolver {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuthService authService;
    private final RedisSecurityService redisSecurityService;

    public AuthGraphQLResolver(AuthenticationManager authenticationManager,
                               UserDetailsService userDetailsService,
                               JwtService jwtService,
                               AuthService authService,
                               RedisSecurityService redisSecurityService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.authService = authService;
        this.redisSecurityService = redisSecurityService;
    }

    @PreAuthorize("isAuthenticated()")
    @QueryMapping
    public User me(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    @MutationMapping
    public LoginResponse login(@Argument LoginInput input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.email(), input.password()));
        UserDetails details = userDetailsService.loadUserByUsername(input.email());
        String token = jwtService.generateToken(details);
        return new LoginResponse(token, 36000L);
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public boolean logout() {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String jwt = header.substring(7);
            String jti = jwtService.extractJti(jwt);
            long remainingMillis = jwtService.extractExpiration(jwt).getTime() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                redisSecurityService.blacklistToken(jti, Duration.ofMillis(remainingMillis));
            }
        }
        return true;
    }

    @MutationMapping
    public User register(@Argument RegisterInput input) {
        RegisterRequest req = new RegisterRequest();
        req.setName(input.name());
        req.setEmail(input.email());
        req.setPassword(input.password());
        req.setType(Type.PATIENT);
        return authService.createUser(req);
    }

    record LoginInput(String email, String password) {}
}
