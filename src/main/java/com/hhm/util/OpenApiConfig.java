package com.hhm.util;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hospital Healthcare Management API",
                version = "1.0.0",
                description = "REST API for managing patient records",
                contact = @Contact(
                        name = "Manzi",
                        email = "audace.manzi@amalitechtraining.org"
                ),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Dev")
        },
        // Applies Bearer auth to every endpoint by default.
        // Endpoints under /api/v1/auth/** are permitAll in SecurityConfig, so the token is optional there.
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "Paste the JWT token returned by POST /api/v1/auth/login"
)
public class OpenApiConfig { }
