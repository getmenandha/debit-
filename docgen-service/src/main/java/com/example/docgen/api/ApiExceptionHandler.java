package com.example.docgen.api;

import com.example.docgen.service.PayloadValidationException;
import com.example.docgen.service.UnknownDocumentTypeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 9457 problem+json for every error. Bodies are written as bytes so errors still reach clients
 * that sent "Accept: application/pdf" (otherwise Spring would answer 406 instead of the real error).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;
    private final ObjectMapper json;

    public ApiExceptionHandler(ObjectMapper json) {
        this.json = json;
    }

    @ExceptionHandler(PayloadValidationException.class)
    ResponseEntity<byte[]> invalid(PayloadValidationException e, HttpServletRequest req) {
        Map<String, Object> body = problem(HttpStatus.BAD_REQUEST, "Payload validation failed", e.getMessage(), req);
        body.put("errors", e.errors());
        return respond(HttpStatus.BAD_REQUEST, body);
    }

    @ExceptionHandler(UnknownDocumentTypeException.class)
    ResponseEntity<byte[]> unknown(UnknownDocumentTypeException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, problem(HttpStatus.NOT_FOUND, "Unknown document type", e.getMessage(), req));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<byte[]> badRequest(Exception e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, problem(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage(), req));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<byte[]> mediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type", "Send the payload as Content-Type: application/json", req));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<byte[]> unexpected(Exception e, HttpServletRequest req) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled error {} on {} {}", errorId, req.getMethod(), req.getRequestURI(), e);
        Map<String, Object> body = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Document generation failed",
                "See server logs for errorId " + errorId, req);
        body.put("errorId", errorId);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    private static Map<String, Object> problem(HttpStatus status, String title, String detail, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", req.getRequestURI());
        String requestId = req.getHeader("X-Request-Id");
        if (requestId != null) body.put("requestId", requestId);
        return body;
    }

    private ResponseEntity<byte[]> respond(HttpStatus status, Map<String, Object> body) {
        try {
            return ResponseEntity.status(status).contentType(PROBLEM).body(json.writeValueAsBytes(body));
        } catch (Exception e) {
            return ResponseEntity.status(status).contentType(PROBLEM).body(new byte[0]);
        }
    }
}
