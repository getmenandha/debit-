package com.example.docgen.service;

import java.util.Locale;

public enum OutputFormat {
    PDF("application/pdf", "pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    public final String mediaType;
    public final String extension;

    OutputFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public static OutputFormat parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported output format '" + value + "' (use pdf or docx)");
        }
    }
}
