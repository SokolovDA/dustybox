package com.dustymotors.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI dustyboxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dustybox API")
                        .description("DustyBox backend App")
                        .version("1.0.0"))
    }
}