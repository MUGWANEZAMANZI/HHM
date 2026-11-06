package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final JwtService jwtService;

    public AnalysisController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<ApiResult> getAnalysis(HttpServletRequest request, @RequestParam String token) {

        String decodedToken = jwtService.decodeToken(token);
        return ResponseEntity.ok(ApiResult.success("Token decoded successfully", decodedToken));

    }
}
