package iti.jets.java.homenursing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Home Nurse Care API")
                        .version("0.0.1")
                        .description("RESTful API for the Home Nurse Care platform. "
                                + "Provides profile management, nurse scheduling, "
                                + "appointment booking, and authentication services.")
                        .contact(new Contact()
                                .name("ITI Java Team")
                                .email("java-team@jets.iti")
                                .url("https://github.com/anomalyco/home-nurse-care"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Enter your JWT Bearer token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
