package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.service.JwtService;
import com.hhm.service.SecurityEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/v1/analysis")
@Tag(name = "Analysis", description = "Admin-only: token decoding and security reporting")
public class AnalysisController {

    private final JwtService jwtService;
    private final SecurityEventService securityEventService;

    public AnalysisController(JwtService jwtService, SecurityEventService securityEventService) {
        this.jwtService = jwtService;
        this.securityEventService = securityEventService;
    }

    @Operation(summary = "Decode a JWT token",
               description = "Returns the claims (sub, jti, iat, exp) of the supplied token. ADMIN only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token decoded successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "ADMIN access required")
    })
    @GetMapping
    public ResponseEntity<ApiResult> getAnalysis(HttpServletRequest request, @RequestParam String token) {
        String decodedToken = jwtService.decodeToken(token);
        return ResponseEntity.ok(ApiResult.success("Token decoded successfully", decodedToken));
    }

    @Operation(summary = "Security event report",
               description = "Returns login success/failure counts, active blacklisted token count, "
                           + "and per-endpoint access frequency since the server started. ADMIN only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report generated"),
        @ApiResponse(responseCode = "403", description = "ADMIN access required")
    })
    @GetMapping("/report")
    public ResponseEntity<ApiResult<Map<String, Object>>> getSecurityReport() {
        Map<String, Object> report = securityEventService.buildReport();
        return ResponseEntity.ok(ApiResult.success("Security report", report));
    }
}
