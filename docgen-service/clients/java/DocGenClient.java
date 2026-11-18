import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sample consumer of the document generation API. Plain JDK 11+ (java.net.http), no dependencies:
 *
 *   java DocGenClient.java                                   # all demos against http://localhost:8080
 *   java DocGenClient.java https://docgen.internal.example   # another base URL
 *
 * Shows: (1) list templates, (2) POST a payload and receive raw PDF bytes, (3) receive DOCX,
 * (4) JSON envelope with base64 + DocuSign signature anchors, (5) handling a 400 validation error.
 * Payloads are read from ../payloads/ - in your application they come from your own domain objects.
 */
public class DocGenClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl;

    public DocGenClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /** Result of a generate call: the document bytes plus the audit headers the service returns. */
    public record Document(byte[] bytes, String contentType, String requestId, String sha256, String templateVersion,
                           int pageCount) {}

    /** Thrown for 4xx/5xx; body is RFC 9457 problem+json (for 400 it has an "errors" array). */
    public static class DocGenException extends RuntimeException {
        public final int status;
        public final String problemJson;
        DocGenException(int status, String problemJson) {
            super("HTTP " + status + ": " + problemJson);
            this.status = status;
            this.problemJson = problemJson;
        }
    }

    /** POST payload -> document bytes. format: "pdf" or "docx". */
    public Document generate(String documentType, String payloadJson, String format, boolean signatureFields)
            throws Exception {
        String requestId = UUID.randomUUID().toString();   // send your own correlation id; it is echoed back
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/documents/" + documentType
                        + "?format=" + format + "&signatureFields=" + signatureFields))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("X-Request-Id", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();
        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) throw new DocGenException(res.statusCode(), new String(res.body()));
        var h = res.headers();
        return new Document(res.body(), h.firstValue("Content-Type").orElse(""),
                h.firstValue("X-Request-Id").orElse(requestId), h.firstValue("X-Document-Sha256").orElse(""),
                h.firstValue("X-Template-Version").orElse(""),
                Integer.parseInt(h.firstValue("X-Page-Count").orElse("0")));
    }

    /** POST payload -> JSON envelope (base64 content + metadata + signature anchors), as a string. */
    public String generateEnvelope(String documentType, String payloadJson) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/documents/" + documentType + "?format=json"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new DocGenException(res.statusCode(), res.body());
        return res.body();
    }

    public String get(String path) throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new DocGenException(res.statusCode(), res.body());
        return res.body();
    }

    // ------------------------------------------------------------------ demo

    public static void main(String[] args) throws Exception {
        DocGenClient client = new DocGenClient(args.length > 0 ? args[0] : "http://localhost:8080");
        Path payloads = Path.of(System.getProperty("payloads", "../payloads"));
        Path out = Files.createDirectories(Path.of("out"));

        System.out.println("1) Available document types:");
        Matcher m = Pattern.compile("\"documentType\":\"([A-Z_]+)\"").matcher(client.get("/api/v1/templates"));
        while (m.find()) System.out.println("   - " + m.group(1));

        System.out.println("2) Term loan agreement -> PDF bytes");
        String loan = Files.readString(payloads.resolve("term-loan-agreement.json"));
        Document pdf = client.generate("TERM_LOAN_AGREEMENT", loan, "pdf", true);
        Path pdfFile = out.resolve("term-loan-agreement.pdf");
        Files.write(pdfFile, pdf.bytes());
        System.out.printf("   %s  %,d bytes, %d pages, template %s, sha256 %s...%n", pdfFile, pdf.bytes().length,
                pdf.pageCount(), pdf.templateVersion(), pdf.sha256().substring(0, 16));

        System.out.println("3) Facility schedule -> DOCX bytes");
        Document docx = client.generate("FACILITY_SCHEDULE",
                Files.readString(payloads.resolve("facility-schedule.json")), "docx", false);
        Files.write(out.resolve("facility-schedule.docx"), docx.bytes());
        System.out.printf("   %s  %,d bytes%n", out.resolve("facility-schedule.docx"), docx.bytes().length);

        System.out.println("4) Spanish loan summary -> JSON envelope (base64 + anchors)");
        String env = client.generateEnvelope("LOAN_SUMMARY_ES", Files.readString(payloads.resolve("loan-summary-es.json")));
        Matcher b64 = Pattern.compile("\"contentBase64\":\"([^\"]+)\"").matcher(env);
        if (b64.find()) {
            Files.write(out.resolve("loan-summary-es.pdf"), Base64.getDecoder().decode(b64.group(1)));
            System.out.println("   " + out.resolve("loan-summary-es.pdf"));
        }
        Matcher anchors = Pattern.compile("\"signatureAnchors\":(\\[[^\\]]*\\])").matcher(
                client.generateEnvelope("TERM_LOAN_AGREEMENT", loan));
        if (anchors.find()) System.out.println("   loan signature anchors for DocuSign: " + anchors.group(1));

        System.out.println("5) Invalid payload -> 400 with every field error");
        try {
            client.generate("TERM_LOAN_AGREEMENT",
                    Files.readString(payloads.resolve("invalid-term-loan-agreement.json")), "pdf", false);
        } catch (DocGenException e) {
            System.out.println("   HTTP " + e.status + " " + e.problemJson);
        }
    }
}
