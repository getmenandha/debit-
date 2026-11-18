package com.example.docgen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI docgenOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Document Generation Service")
                .version("v1")
                .description("""
                        Word templates + JSON payload -> accessible PDF (PDF/UA-1) or DOCX, using Aspose.Words \
                        and Aspose.PDF. Start with GET /api/v1/templates, fetch a sample payload, then POST it \
                        to /api/v1/documents/{documentType}."""));
    }
}
