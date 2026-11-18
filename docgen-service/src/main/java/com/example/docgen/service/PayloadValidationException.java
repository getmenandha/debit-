package com.example.docgen.service;

import java.util.List;

/** The payload failed JSON Schema validation. Carries every error, not just the first. */
public class PayloadValidationException extends RuntimeException {

    public record FieldError(String path, String message) {}

    private final List<FieldError> errors;

    public PayloadValidationException(String message, List<FieldError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> errors() {
        return errors;
    }
}
