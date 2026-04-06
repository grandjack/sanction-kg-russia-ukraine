package com.sanction.kg.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI sanctionKgOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sanction Knowledge Graph API")
                        .description("API for the Sanction-KG Risk System - a knowledge graph for economic sanctions compliance risk assessment (Russia-Ukraine conflict)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sanction-KG Team")));
    }
}
