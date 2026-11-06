package com.hhm.controller;

import com.hhm.model.User;
import com.hhm.repository.UserRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class GoogleLoginController {

    private final UserRepository userRepository;

    public GoogleLoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = (principal instanceof OAuth2AuthenticationToken token)
                ? token.getPrincipal().getAttribute("email")
                : principal.getName();

        User user = userRepository.findByEmail(email).orElse(null);

        model.addAttribute("displayName",
                user != null && user.getDisplayName() != null ? user.getDisplayName() : email);
        model.addAttribute("role",
                user != null && user.getUserType() != null ? user.getUserType() : "PENDING");

        return "index";
    }
}
