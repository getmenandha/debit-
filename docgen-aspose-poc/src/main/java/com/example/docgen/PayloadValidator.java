package com.example.docgen;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strict payload validation, run before any rendering. Collects every problem
 * (not just the first) with a JSON-pointer path so callers get one clear error response.
 *
 * In a real service this would be a JSON Schema per document type; hand-written
 * checks keep the POC dependency-free.
 */
public final class PayloadValidator {

    /** Signer ids become Word bookmark names, so keep them short and simple. */
    private static final Pattern SIGNER_ID = Pattern.compile("[A-Za-z0-9_]{1,30}");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    public record ValidationError(String path, String message) {
        @Override public String toString() { return path + ": " + message; }
    }

    public List<ValidationError> validate(JsonNode root) {
        List<ValidationError> errors = new ArrayList<>();
        requireText(root, "/requestId", errors);
        requireText(root, "/documentType", errors);
        requireText(root, "/agreement/number", errors);
        requireDate(root, "/agreement/date", errors);
        requireText(root, "/lender/name", errors);
        requireText(root, "/borrower/customerId", errors);
        requireText(root, "/borrower/legalName", errors);
        requireText(root, "/borrower/address", errors);

        JsonNode facilities = root.at("/facilities");
        if (!facilities.isArray() || facilities.isEmpty()) {
            errors.add(new ValidationError("/facilities", "at least one facility is required"));
        } else {
            for (int i = 0; i < facilities.size(); i++) {
                String p = "/facilities/" + i;
                requireText(root, p + "/id", errors);
                requireText(root, p + "/type", errors);
                requirePattern(root, p + "/currency", CURRENCY, "must be a 3-letter ISO currency code", errors);
                requirePositiveNumber(root, p + "/amount", errors);
                requireDate(root, p + "/maturityDate", errors);
            }
        }

        JsonNode collateral = root.at("/collateral");
        if (!collateral.isMissingNode() && !collateral.isArray()) {
            errors.add(new ValidationError("/collateral", "must be an array (use [] for unsecured)"));
        } else if (collateral.isArray()) {
            for (int i = 0; i < collateral.size(); i++) {
                String p = "/collateral/" + i;
                requireText(root, p + "/description", errors);
                requirePattern(root, p + "/currency", CURRENCY, "must be a 3-letter ISO currency code", errors);
                requirePositiveNumber(root, p + "/value", errors);
            }
        }

        boolean signatureRequired = root.at("/signatureRequired").asBoolean(false);
        JsonNode signers = root.at("/signers");
        if (signatureRequired && (!signers.isArray() || signers.isEmpty())) {
            errors.add(new ValidationError("/signers", "at least one signer is required when signatureRequired=true"));
        } else if (signers.isArray()) {
            for (int i = 0; i < signers.size(); i++) {
                String p = "/signers/" + i;
                requirePattern(root, p + "/id", SIGNER_ID, "must match " + SIGNER_ID.pattern(), errors);
                requireText(root, p + "/name", errors);
                requireText(root, p + "/role", errors);
            }
        }
        return errors;
    }

    private static void requireText(JsonNode root, String ptr, List<ValidationError> errors) {
        JsonNode n = root.at(ptr);
        if (n.isMissingNode() || n.isNull() || !n.isTextual() || n.asText().isBlank()) {
            errors.add(new ValidationError(ptr, "required, non-blank string"));
        }
    }

    private static void requirePattern(JsonNode root, String ptr, Pattern pattern, String msg, List<ValidationError> errors) {
        JsonNode n = root.at(ptr);
        if (!n.isTextual() || !pattern.matcher(n.asText()).matches()) {
            errors.add(new ValidationError(ptr, msg));
        }
    }

    private static void requirePositiveNumber(JsonNode root, String ptr, List<ValidationError> errors) {
        JsonNode n = root.at(ptr);
        if (!n.isNumber() || n.decimalValue().signum() <= 0) {
            errors.add(new ValidationError(ptr, "required, number > 0"));
        }
    }

    private static void requireDate(JsonNode root, String ptr, List<ValidationError> errors) {
        JsonNode n = root.at(ptr);
        try {
            if (!n.isTextual()) throw new DateTimeParseException("not text", "", 0);
            LocalDate.parse(n.asText());
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(ptr, "required, ISO date yyyy-MM-dd"));
        }
    }
}
