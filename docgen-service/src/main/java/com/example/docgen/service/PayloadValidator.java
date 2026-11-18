package com.example.docgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strict payload validation against the document type's JSON Schema (draft 2020-12),
 * before any rendering. All errors are returned together. The same schema is published at
 * GET /api/v1/templates/{type}/schema so consumers can validate on their side too.
 */
@Component
public class PayloadValidator {

    private final TemplateRegistry registry;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private final Map<String, JsonSchema> schemas = new ConcurrentHashMap<>();

    // Error paths as JSON Pointer ("/agreement/date"), the same form the template titles use.
    private final SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
            .pathType(PathType.JSON_POINTER).build();

    public PayloadValidator(TemplateRegistry registry) {
        this.registry = registry;
    }

    public void validate(String documentType, JsonNode payload) {
        JsonSchema schema = schemas.computeIfAbsent(documentType,
                t -> factory.getSchema(new ByteArrayInputStream(registry.schema(t)), config));
        Set<ValidationMessage> messages = schema.validate(payload);
        if (!messages.isEmpty()) {
            List<PayloadValidationException.FieldError> errors = messages.stream()
                    .map(PayloadValidator::toFieldError)
                    .sorted(Comparator.comparing(PayloadValidationException.FieldError::path))
                    .toList();
            throw new PayloadValidationException(
                    "Payload is not valid for document type " + documentType, errors);
        }
    }

    /** "required" errors point at the missing member itself, and the path prefix is trimmed from the message. */
    private static PayloadValidationException.FieldError toFieldError(ValidationMessage m) {
        String path = m.getInstanceLocation().toString();
        if ("required".equals(m.getType()) && m.getProperty() != null) {
            path = (path.equals("/") ? "" : path) + "/" + m.getProperty();
        }
        if (path.isEmpty()) path = "/";
        String msg = m.getMessage();
        String prefix = m.getInstanceLocation().toString() + ": ";
        if (msg.startsWith(prefix)) msg = msg.substring(prefix.length());
        return new PayloadValidationException.FieldError(path, msg);
    }
}
