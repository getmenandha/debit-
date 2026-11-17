package com.example.docgen;

import com.aspose.words.Document;
import com.aspose.words.JsonDataLoadOptions;
import com.aspose.words.JsonDataSource;
import com.aspose.words.ReportBuildOptions;
import com.aspose.words.ReportingEngine;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template lint: renders each production template in STRICT mode (no ALLOW_MISSING_MEMBERS)
 * with a complete sample payload. Production uses ALLOW_MISSING_MEMBERS so empty arrays render,
 * but that option also hides template mistakes (wrong method names such as .NET-style Count() instead
 * of Java-style count(), or typos). This test makes those mistakes fail the build.
 */
class TemplateLintTest {

    @Test
    void loanAgreementTemplateRendersStrictly() throws Exception {
        lint("loan-agreement-template.docx", Path.of("samples", "loan-payload.json"), "loan");
    }

    private static void lint(String template, Path payload, String dsName) throws Exception {
        Document doc;
        try (InputStream in = TemplateLintTest.class.getResourceAsStream("/templates/" + template)) {
            doc = new Document(in);
        }
        ReportingEngine engine = new ReportingEngine();
        engine.setOptions(ReportBuildOptions.REMOVE_EMPTY_PARAGRAPHS);   // strict: any error throws
        JsonDataLoadOptions o = new JsonDataLoadOptions();
        o.setAlwaysGenerateRootObject(true);
        assertDoesNotThrow(() -> engine.buildReport(doc,
                new JsonDataSource(new ByteArrayInputStream(Files.readAllBytes(payload)), o), dsName));
        assertFalse(doc.getText().contains("<<"), template + ": unrendered tag left in output");
    }
}
