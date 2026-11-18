package com.example.docgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/** Bound from the {@code docgen.*} section of application.yml. */
@ConfigurationProperties(prefix = "docgen")
public record DocGenProperties(Aspose aspose, boolean pdfuaCheck, boolean warmUp, Templates templates) {

    public record Aspose(String licensePath, String licenseBase64, String fontsDir) {}

    public record Templates(String location, Map<String, DocumentType> types) {}

    /**
     * One document type = one Word template (+ static parts appended to it), its JSON Schema,
     * a sample payload and rendering options.
     */
    public record DocumentType(
            String description,
            String template,
            List<String> append,
            String schema,
            String sample,
            String title,
            String locale,
            boolean allowMissingMembers,
            boolean retagLayoutTables) {

        public List<String> appendOrEmpty() {
            return append == null ? List.of() : append;
        }
    }
}
