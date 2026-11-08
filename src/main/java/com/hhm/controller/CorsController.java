package com.hhm.controller;

import com.hhm.dto.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "CORS Demo", description = "Demonstrates Cross-Origin Resource Sharing enforcement")
public class CorsController {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:4200",
            "http://localhost:5173"
    );

    @Operation(
        summary     = "CORS demonstration endpoint",
        description = "Public endpoint that shows how CORS is configured. "
                    + "Call it from a browser at an allowed origin to see the "
                    + "Access-Control-Allow-Origin header in the response. "
                    + "A browser request from a different origin will be blocked."
    )
    @ApiResponse(responseCode = "200", description = "CORS configuration details returned")
    @GetMapping("/cors-demo")
    public ResponseEntity<ApiResult<Map<String, Object>>> corsDemo(HttpServletRequest request) {
        String incomingOrigin = request.getHeader("Origin");

        String corsResult;
        if (incomingOrigin == null) {
            corsResult = "N/A — same-origin or non-browser request (Postman/curl). CORS is a browser-only mechanism.";
        } else if (ALLOWED_ORIGINS.contains(incomingOrigin)) {
            corsResult = "ALLOWED — the server set Access-Control-Allow-Origin: " + incomingOrigin;
        } else {
            corsResult = "BLOCKED — " + incomingOrigin + " is not in the allowed list. The browser will suppress this response.";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("incomingOrigin",  incomingOrigin != null ? incomingOrigin : "(none)");
        body.put("allowedOrigins",  ALLOWED_ORIGINS);
        body.put("corsResult",      corsResult);
        body.put("note", "CORS does not protect Postman or curl — only browsers enforce it. "
                       + "CSRF is separately disabled because this API is stateless (JWT Bearer tokens).");

        return ResponseEntity.ok(ApiResult.success("CORS demo", body));
    }
}
