package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.service.JwtService;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/v1/analysis")
@Tag(name = "Analysis Controller", description = "Handles analysis operations, including token decoding")
public class AnalysisController {

    private final JwtService jwtService;


    public AnalysisController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Token decoded successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access - invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Access Denied - insufficient permissions to access this resource")
        }
    )
    @GetMapping
    public ResponseEntity<ApiResult> getAnalysis(HttpServletRequest request, @RequestParam String token) {

        String decodedToken = jwtService.decodeToken(token);
        return ResponseEntity.ok(ApiResult.success("Token decoded successfully", decodedToken));

    }
}
