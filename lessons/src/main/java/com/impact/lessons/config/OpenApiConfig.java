package com.impact.lessons.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI lessonsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Impact Lessons API")
                        .description("Documentație interactivă pentru endpoint-urile REST ale proiectului lessons.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Impact Academy")
                                .email("support@impact.example")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT obținut de la POST /api/authenticate")));
    }
}
