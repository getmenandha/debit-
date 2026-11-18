package com.example.docgen.service;

import com.example.docgen.config.AsposeSetup;
import com.example.docgen.config.DocGenProperties;
import com.example.docgen.render.PdfPostProcessor;
import com.example.docgen.render.WordRenderer;
import com.example.docgen.service.TemplateRegistry.TemplateSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One generate call: validate -> fill template (Aspose.Words) -> PDF/UA or DOCX -> optional
 * post-processing (Aspose.PDF) -> audit log line. No data enrichment: the payload is the only input.
 */
@Service
public class DocumentGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerationService.class);
    private static final Logger audit = LoggerFactory.getLogger("docgen.audit");
    private static final Pattern TITLE_POINTER = Pattern.compile("\\{(/[^}]*)}");

    private final TemplateRegistry registry;
    private final PayloadValidator validator;
    private final WordRenderer renderer;
    private final PdfPostProcessor pdfProcessor;
    private final AsposeSetup aspose;
    private final ObjectMapper json;
    private final boolean pdfUaCheck;

    public DocumentGenerationService(TemplateRegistry registry, PayloadValidator validator, WordRenderer renderer,
                                     PdfPostProcessor pdfProcessor, AsposeSetup aspose, ObjectMapper json,
                                     DocGenProperties properties) {
        this.registry = registry;
        this.validator = validator;
        this.renderer = renderer;
        this.pdfProcessor = pdfProcessor;
        this.aspose = aspose;
        this.json = json;
        this.pdfUaCheck = properties.pdfuaCheck();
    }

    public GeneratedDocument generate(String documentType, byte[] payload, OutputFormat format,
                                      boolean signatureFields, String requestIdHeader) throws Exception {
        long t0 = System.nanoTime();
        TemplateSet templates = registry.get(documentType);           // 404 if unknown

        JsonNode root;
        try {
            root = json.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new PayloadValidationException("Payload is not valid JSON",
                    List.of(new PayloadValidationException.FieldError("$", e.getOriginalMessage())));
        }
        if (root == null || !root.isObject()) {
            throw new PayloadValidationException("Payload must be a JSON object",
                    List.of(new PayloadValidationException.FieldError("$", "expected a JSON object")));
        }
        validator.validate(documentType, root);                          // 400 with every error

        String requestId = firstNonBlank(requestIdHeader, root.path("requestId").asText(null),
                UUID.randomUUID().toString());
        var config = templates.config();
        String title = title(config.title(), root, documentType);

        WordRenderer.Rendered rendered = renderer.render(templates.main(), templates.appended(), payload, title,
                config.locale() == null ? "en-US" : config.locale(), config.allowMissingMembers());

        byte[] content;
        PdfPostProcessor.PdfUaReport ua = null;
        if (format == OutputFormat.DOCX) {
            content = renderer.toDocx(rendered.document());
        } else {
            content = renderer.toPdf(rendered.document());
            if (config.retagLayoutTables()) content = pdfProcessor.retagLayoutTables(content);
            if (signatureFields && !rendered.anchors().isEmpty()) {
                content = pdfProcessor.addSignatureFields(content, rendered.anchors(), signerNames(root));
            }
            if (pdfUaCheck) ua = pdfProcessor.validatePdfUa(content);
        }

        long ms = (System.nanoTime() - t0) / 1_000_000;
        GeneratedDocument doc = new GeneratedDocument(requestId, documentType, templates.version(), format, content,
                sha256(content), rendered.pageCount(), rendered.anchors(), rendered.warnings(), ua,
                !aspose.isLicensed(), ms);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("event", "document.generated");
        line.put("requestId", requestId);
        line.put("documentType", documentType);
        line.put("templateVersion", templates.version());
        line.put("format", format.name());
        line.put("payloadSha256", sha256(payload));
        line.put("documentSha256", doc.sha256());
        line.put("pages", doc.pageCount());
        line.put("renderMs", ms);
        line.put("warnings", doc.warnings().size());
        line.put("evaluationMode", doc.evaluationMode());
        audit.info(json.writeValueAsString(line));
        return doc;
    }

    private static String title(String pattern, JsonNode root, String fallback) {
        if (pattern == null) return fallback;
        Matcher m = TITLE_POINTER.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(root.at(m.group(1)).asText("")));
        m.appendTail(sb);
        return sb.toString().trim();
    }

    private static Map<String, String> signerNames(JsonNode root) {
        Map<String, String> names = new LinkedHashMap<>();
        root.path("signers").forEach(s -> names.put(s.path("id").asText(), s.path("name").asText()));
        return names;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
