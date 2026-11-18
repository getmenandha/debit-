package com.example.docgen.api;

import com.example.docgen.config.DocGenProperties.DocumentType;
import com.example.docgen.service.TemplateRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Discover document types, their payload schema and a sample payload")
public class TemplateController {

    private final TemplateRegistry registry;

    public TemplateController(TemplateRegistry registry) {
        this.registry = registry;
    }

    public record TemplateInfo(String documentType, String description, String template, List<String> appended,
                               String locale, String templateVersion, Map<String, String> links) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List document types")
    public List<TemplateInfo> list() {
        return registry.documentTypes().stream().map(this::info).toList();
    }

    @GetMapping(value = "/{documentType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Describe one document type")
    public TemplateInfo get(@PathVariable String documentType) {
        return info(documentType.toUpperCase());
    }

    @GetMapping(value = "/{documentType}/schema", produces = "application/schema+json")
    @Operation(summary = "JSON Schema (draft 2020-12) the payload must satisfy")
    public ResponseEntity<byte[]> schema(@PathVariable String documentType) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/schema+json"))
                .body(registry.schema(documentType.toUpperCase()));
    }

    @GetMapping(value = "/{documentType}/sample", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "A sample payload that passes validation")
    public ResponseEntity<byte[]> sample(@PathVariable String documentType) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(registry.sample(documentType.toUpperCase()));
    }

    private TemplateInfo info(String type) {
        DocumentType c = registry.config(type);
        String base = "/api/v1/templates/" + type;
        return new TemplateInfo(type, c.description(), c.template(), c.appendOrEmpty(),
                c.locale() == null ? "en-US" : c.locale(), registry.get(type).version(),
                Map.of("schema", base + "/schema", "sample", base + "/sample",
                        "generate", "/api/v1/documents/" + type));
    }
}
