package com.example.docgen.api;

import com.example.docgen.render.PdfPostProcessor.PdfUaReport;
import com.example.docgen.render.SignatureAnchor;
import com.example.docgen.service.DocumentGenerationService;
import com.example.docgen.service.GeneratedDocument;
import com.example.docgen.service.OutputFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Fill a template with a JSON payload and return the document bytes")
public class DocumentController {

    public static final String JSON_ENVELOPE = "application/vnd.docgen+json";

    private final DocumentGenerationService service;

    public DocumentController(DocumentGenerationService service) {
        this.service = service;
    }

    /** Response body when format=json: the document base64-encoded plus metadata. */
    public record DocumentEnvelope(String requestId, String documentType, String templateVersion, String contentType,
                                   String fileName, int pageCount, String sha256, long renderMillis,
                                   boolean evaluationMode, List<SignatureAnchor> signatureAnchors,
                                   List<String> warnings, PdfUaReport pdfUa, String contentBase64) {}

    @PostMapping(value = "/{documentType}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_PDF_VALUE,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    MediaType.APPLICATION_JSON_VALUE, JSON_ENVELOPE})
    @Operation(summary = "Generate a document",
            description = """
                    Validates the payload against the document type's JSON Schema, fills the Word template and \
                    returns the document. Default response is the raw PDF bytes (PDF/UA-1 tagged). \
                    Use format=docx for Word, or format=json for a JSON envelope with base64 content, \
                    page count, SHA-256 and DocuSign signature anchors (page, x, y in points).""")
    @ApiResponse(responseCode = "200", description = "Document bytes",
            content = {@Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentEnvelope.class))})
    @ApiResponse(responseCode = "400", description = "Payload failed validation (problem+json with errors[])")
    @ApiResponse(responseCode = "404", description = "Unknown document type")
    public ResponseEntity<?> generate(
            @Parameter(description = "e.g. TERM_LOAN_AGREEMENT; see GET /api/v1/templates") @PathVariable String documentType,
            @Parameter(description = "pdf (default), docx or json") @RequestParam(required = false) String format,
            @Parameter(description = "Add empty, tagged PDF signature fields at the signature anchors")
            @RequestParam(defaultValue = "false") boolean signatureFields,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestBody byte[] payload) throws Exception {

        String wanted = format != null ? format : fromAccept(accept);
        boolean envelope = "json".equalsIgnoreCase(wanted);
        OutputFormat out = envelope || wanted == null ? OutputFormat.PDF : OutputFormat.parse(wanted);

        GeneratedDocument doc = service.generate(documentType.toUpperCase(), payload, out, signatureFields, requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", doc.requestId());
        headers.add("X-Template-Version", doc.templateVersion());
        headers.add("X-Document-Sha256", doc.sha256());
        headers.add("X-Page-Count", String.valueOf(doc.pageCount()));
        headers.add("X-Render-Millis", String.valueOf(doc.renderMillis()));
        if (doc.evaluationMode()) headers.add("X-Aspose-Evaluation", "true");

        if (envelope) {
            return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON)
                    .body(new DocumentEnvelope(doc.requestId(), doc.documentType(), doc.templateVersion(),
                            doc.format().mediaType, doc.fileName(), doc.pageCount(), doc.sha256(), doc.renderMillis(),
                            doc.evaluationMode(), doc.signatureAnchors(), doc.warnings(), doc.pdfUa(),
                            Base64.getEncoder().encodeToString(doc.content())));
        }
        headers.setContentDisposition(ContentDisposition.inline().filename(doc.fileName()).build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType(doc.format().mediaType))
                .contentLength(doc.content().length).body(doc.content());
    }

    /** Accept header fallback when no ?format= is given. */
    private static String fromAccept(String accept) {
        if (accept == null) return null;
        if (accept.contains("wordprocessingml")) return "docx";
        if (accept.contains("application/pdf")) return "pdf";
        if (accept.contains("json")) return "json";
        return null;
    }
}
