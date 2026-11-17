package com.example.docgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocGenServiceTest {

    @TempDir Path out;
    private final DocGenService service = new DocGenService();

    private static String docText(Path docx) throws Exception {
        return new com.aspose.words.Document(docx.toString()).getText();
    }

    private static byte[] sample(String name) throws Exception {
        return Files.readAllBytes(Path.of("samples", name));
    }

    @Test
    void generatesPdfDocxAndSignatureAnchors() throws Exception {
        var outcome = service.generate(sample("loan-payload.json"), out, true);
        assertTrue(outcome.success());
        assertTrue(Files.size(outcome.outDir().resolve("document.pdf")) > 0);
        assertTrue(Files.exists(outcome.outDir().resolve("document.docx")));
        assertTrue(Files.exists(outcome.outDir().resolve("document-signable.pdf")));
        // Content assertions: catch template expressions that silently render empty
        String text = docText(outcome.outDir().resolve("document.docx"));
        assertTrue(text.contains("Total committed amount: 3,650,000.00"), "total missing");
        assertTrue(text.contains("The facilities are secured by:"), "secured branch missing");
        assertTrue(text.contains("Warehouse at 12 Dock Road"), "collateral rows missing");
        assertFalse(text.contains("unsecured"), "wrong branch rendered");
        String anchors = Files.readString(outcome.outDir().resolve("signature-anchors.json"));
        assertTrue(anchors.contains("borrower_1") && anchors.contains("lender_1"));
        if (AsposeLicensing.isLicensed()) {
            // With a license there is no evaluation watermark, so the PDF/UA check should be clean.
            var ua = (PdfPostProcessor.PdfUaReport) outcome.audit().get("pdfUa");
            assertEquals(0, ua.errors(), String.join("\n", ua.errorSummary()));
        }
    }

    @Test
    void unsecuredLoanRendersElseBranch() throws Exception {
        var outcome = service.generate(sample("loan-payload-unsecured.json"), out, false);
        assertTrue(outcome.success());
        assertTrue(docText(outcome.outDir().resolve("document.docx")).contains("The facilities are unsecured."));
    }

    @Test
    void invalidPayloadIsRejectedWithAllErrors() throws Exception {
        var outcome = service.generate(sample("loan-payload-invalid.json"), out, false);
        assertFalse(outcome.success());
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) outcome.audit().get("errors");
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("/borrower/legalName")));
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("/facilities/1/amount")));
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("/signers")));
        assertFalse(Files.exists(outcome.outDir().resolve("document.pdf")));
    }
}
