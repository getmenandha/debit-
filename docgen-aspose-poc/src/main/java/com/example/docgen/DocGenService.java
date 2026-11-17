package com.example.docgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates one "generate" call: validate -> render (Words) -> PDF/UA check (PDF)
 * -> optional signature fields (PDF) -> write outputs + audit record.
 *
 * In the real service this is the body of POST /documents; outputs would go to S3/DocHub
 * instead of a local folder.
 */
public final class DocGenService {

    /** documentType -> template + static parts appended to the packet. */
    private static final Map<String, List<String>> TEMPLATES = Map.of(
            "TERM_LOAN_AGREEMENT", List.of("loan-agreement-template.docx", "disclosures.docx"));

    private final ObjectMapper json = new ObjectMapper();
    private final PayloadValidator validator = new PayloadValidator();
    private final WordRenderer renderer = new WordRenderer();
    private final PdfPostProcessor pdfProcessor = new PdfPostProcessor();

    public record Outcome(boolean success, Path outDir, Map<String, Object> audit) {}

    public Outcome generate(byte[] payload, Path outRoot, boolean addSignatureFields) throws Exception {
        AsposeLicensing.init();
        long t0 = System.nanoTime();
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("receivedAt", Instant.now().toString());
        audit.put("payloadSha256", sha256(payload));
        audit.put("asposeLicensed", AsposeLicensing.isLicensed());
        audit.put("asposeWordsVersion", com.aspose.words.BuildVersionInfo.getVersion());

        JsonNode root = json.readTree(payload);
        String requestId = root.path("requestId").asText("unknown-request");
        audit.put("requestId", requestId);
        Path outDir = Files.createDirectories(outRoot.resolve(safe(requestId)));

        // 1. Validate - fail fast with every error, no enrichment.
        List<PayloadValidator.ValidationError> errors = validator.validate(root);
        String docType = root.path("documentType").asText();
        if (errors.isEmpty() && !TEMPLATES.containsKey(docType)) {
            errors = List.of(new PayloadValidator.ValidationError("/documentType", "unknown document type " + docType));
        }
        if (!errors.isEmpty()) {
            audit.put("status", "REJECTED");
            audit.put("errors", errors.stream().map(Object::toString).toList());
            write(outDir.resolve("audit.json"), audit);
            return new Outcome(false, outDir, audit);
        }

        // 2. Render with Aspose.Words.
        List<String> parts = TEMPLATES.get(docType);
        String title = "Term Loan Agreement " + root.at("/agreement/number").asText();
        long tRender = System.nanoTime();
        WordRenderer.RenderResult result = renderer.render(parts.get(0), payload, title, parts.subList(1, parts.size()));
        audit.put("renderMs", ms(tRender));
        Files.write(outDir.resolve("document.docx"), result.docx());
        Files.write(outDir.resolve("document.pdf"), result.pdf());
        write(outDir.resolve("signature-anchors.json"), result.anchors());
        audit.put("pages", result.pageCount());
        audit.put("renderWarnings", result.warnings());

        // 3. PDF/UA check with Aspose.PDF.
        long tUa = System.nanoTime();
        PdfPostProcessor.PdfUaReport ua = pdfProcessor.validatePdfUa(result.pdf(), outDir.resolve("pdfua-validation.xml"));
        audit.put("pdfUaCheckMs", ms(tUa));
        audit.put("pdfUa", ua);

        // 4. Optional: signature-integrated path (internal e-auth) - add tagged signature fields.
        byte[] finalPdf = result.pdf();
        if (addSignatureFields) {
            Map<String, String> names = new LinkedHashMap<>();
            root.path("signers").forEach(s -> names.put(s.path("id").asText(), s.path("name").asText()));
            finalPdf = pdfProcessor.addSignatureFields(result.pdf(), result.anchors(), names);
            Files.write(outDir.resolve("document-signable.pdf"), finalPdf);
            audit.put("pdfUaSignable", pdfProcessor.validatePdfUa(finalPdf, outDir.resolve("pdfua-validation-signable.xml")));
        }

        audit.put("documentSha256", sha256(finalPdf));
        audit.put("totalMs", ms(t0));
        audit.put("status", "GENERATED");
        write(outDir.resolve("audit.json"), audit);
        return new Outcome(true, outDir, audit);
    }

    private void write(Path file, Object value) throws Exception {
        json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
    }

    private static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
