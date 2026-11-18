package com.example.docgen.config;

import com.example.docgen.service.DocumentGenerationService;
import com.example.docgen.service.OutputFormat;
import com.example.docgen.service.TemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Renders every document type's sample once at startup. The first Aspose render loads fonts and layout
 * code (several seconds); doing it before the readiness probe goes green keeps that off real traffic.
 * ApplicationRunners finish before Spring Boot marks the app ACCEPTING_TRAFFIC.
 */
@Component
@ConditionalOnProperty(prefix = "docgen", name = "warm-up", havingValue = "true", matchIfMissing = true)
public class WarmUp implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WarmUp.class);
    private final TemplateRegistry registry;
    private final DocumentGenerationService service;

    public WarmUp(TemplateRegistry registry, DocumentGenerationService service) {
        this.registry = registry;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        long t0 = System.currentTimeMillis();
        for (String type : registry.documentTypes()) {
            try {
                service.generate(type, registry.sample(type), OutputFormat.PDF, false, "warm-up-" + type);
            } catch (Exception e) {
                log.warn("Warm-up render failed for {}: {}", type, e.toString());
            }
        }
        log.info("Warm-up rendered {} document types in {} ms", registry.documentTypes().size(),
                System.currentTimeMillis() - t0);
    }
}
