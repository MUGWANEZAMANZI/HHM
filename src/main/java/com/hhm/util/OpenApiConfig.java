package com.hhm.util;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
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
                @Server(url = "http://localhost:8080", description = "Local Dev"),
        }
)



public class OpenApiConfig { }
