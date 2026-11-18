# docgen-service

Spring Boot service that fills Word templates with a JSON payload and returns the document bytes —
accessible PDF (PDF/UA-1 tagged) or DOCX — using Aspose.Words and Aspose.PDF. Built for container
deployment (Docker / AWS ECS Fargate). Any consumer that can POST JSON over HTTP can use it.

```
consumer ──POST JSON──▶ /api/v1/documents/{type}
                          1. look up template set (Word .docx + appended parts)
                          2. validate payload against the type's JSON Schema → 400 with every error
                          3. fill template (Aspose.Words LINQ Reporting Engine)
                          4. save PDF/UA-1 (or DOCX); optional Aspose.PDF steps: re-tag layout tables,
                             add signature fields, PDF/UA check
                          5. audit log line (request id, template version, payload + document SHA-256)
          ◀── PDF bytes + headers (X-Request-Id, X-Template-Version, X-Document-Sha256, X-Page-Count)
```

## API

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/templates` | Document types, template version, links |
| GET | `/api/v1/templates/{type}` | One document type |
| GET | `/api/v1/templates/{type}/schema` | JSON Schema (draft 2020-12) the payload must satisfy |
| GET | `/api/v1/templates/{type}/sample` | A sample payload that passes validation |
| POST | `/api/v1/documents/{type}` | The document |
| GET | `/swagger-ui.html`, `/v3/api-docs` | OpenAPI UI and spec |
| GET | `/actuator/health/liveness`, `/actuator/health/readiness` | ECS / ALB health checks |

**POST `/api/v1/documents/{type}`** — body: the JSON payload (`Content-Type: application/json`).

| Option | Values | Effect |
|---|---|---|
| `?format=` | `pdf` (default), `docx`, `json` | `json` returns an envelope: `contentBase64`, `pageCount`, `sha256`, `signatureAnchors`, `warnings`, `templateVersion` |
| `Accept` header | `application/pdf`, `…wordprocessingml.document`, `application/json` | Used when `format` is not given |
| `?signatureFields=true` | | Adds empty, tagged PDF signature fields at each signer's anchor |
| `X-Request-Id` header | any string | Correlation id; echoed back and written to the audit log (falls back to payload `requestId`, then a UUID) |

`signatureAnchors` gives page, x, y (points from the top-left, and PDF lower-left coordinates) and
size for each `SIG_`/`DATE_` bookmark in the template — what DocuSign needs for fixed-position tabs.

Errors are RFC 9457 `application/problem+json`, also when the client asked for a PDF:

```json
{ "title": "Payload validation failed", "status": 400,
  "errors": [ { "path": "/agreement/date", "message": "does not match the regex pattern ^\\d{4}-\\d{2}-\\d{2}$" },
              { "path": "/borrower", "message": "required property 'borrower' not found" } ] }
```
404 = unknown document type; 500 = render failure (body has an `errorId` to find in the logs).

### Document types shipped

| Type | Template | Shows |
|---|---|---|
| `TERM_LOAN_AGREEMENT` | loan-agreement-template.docx + disclosures.docx | dynamic facility rows, totals, conditional collateral, numbered covenants, signature anchors |
| `FACILITY_SCHEDULE` | facility-schedule-template.docx | table that grows with the payload; header row repeats on each page |
| `LOAN_SUMMARY_ES` | spanish-loan-summary-template.docx | Spanish dates, `2.500.000,00` number format, `lang="es-ES"` in the PDF |
| `TAXPAYER_FORM` | two-column-template.docx | W-9 style two-column layout; layout tables re-tagged so screen readers don't read them as data |

## Sample clients (`clients/`)

Each one lists types, generates a PDF, a DOCX and a JSON envelope, and shows a 400 response. Payloads
are in `clients/payloads/` (including `invalid-term-loan-agreement.json`).

```bash
cd clients/java   && java DocGenClient.java http://localhost:8080     # JDK 11+, no dependencies
cd clients/python && python3 docgen_client.py http://localhost:8080   # stdlib only
cd clients/curl   && BASE=http://localhost:8080 ./examples.sh
```
Files land in `out/` next to each client.

## Run locally

```bash
mvn test                                   # 9 tests: API, validation, Spanish, strict template lint
mvn package -DskipTests
java -jar target/docgen-service.jar        # http://localhost:8080/swagger-ui.html
```
Needs Maven access to Maven Central and `https://releases.aspose.com/java/repo/` (or your
Artifactory/Nexus mirror of both).

## Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `ASPOSE_LICENSE_BASE64` | — | Aspose license XML, base64-encoded (preferred for ECS: from Secrets Manager) |
| `ASPOSE_LICENSE_PATH` | — | Or a path to the `.lic` file |
| `DOCGEN_TEMPLATES_LOCATION` | `classpath:/templates/` | Or `file:/opt/docgen/templates/` to mount/sync templates without rebuilding the image |
| `DOCGEN_FONTS_DIR` | — | Extra font folder (licensed corporate fonts) |
| `DOCGEN_PDFUA_CHECK` | `false` | Run the Aspose.PDF PDF/UA check on every request and return the result (adds latency) |
| `DOCGEN_WARM_UP` | `true` | Render every sample at startup, before readiness goes green |

**No license = evaluation mode**: watermark on every page, an evaluation banner that itself fails
PDF/UA, and long documents truncated (about 30 table rows). Responses then carry
`X-Aspose-Evaluation: true` and the startup log warns. A license is required before any real use.

Adding a document type: drop the `.docx` in `templates/`, a schema in `schemas/`, a sample in
`samples/`, and an entry under `docgen.templates.types` in `application.yml`. `TemplateLintTest`
then renders it in strict mode in CI, which catches template typos and .NET-style functions
(`Count()` instead of Java's `count()`).

## Deploy to ECS

```bash
docker build -t docgen-service .                 # multi-stage: Maven build → JRE 17 + fonts, non-root
docker run -p 8080:8080 -e ASPOSE_LICENSE_BASE64="$(base64 -w0 Aspose.Total.Java.lic)" docgen-service

ACCOUNT_ID=123456789012 REGION=us-east-1 TAG=1.0.0 ./ecs/deploy.sh   # ECR push + register task definition
```

`ecs/task-definition.json` (Fargate, 2 vCPU / 4 GB): license from Secrets Manager secret
`docgen/aspose-license` (store the base64 string), CloudWatch logs, container health check on
`/actuator/health/liveness`, 90 s start period for warm-up. One-time setup in your IaC:

- ECS service behind an internal ALB; target group health check path `/actuator/health/readiness`.
- Execution role: `secretsmanager:GetSecretValue` on the license secret, ECR pull, logs.
- Scale on CPU (rendering is CPU-bound); ~0.7 s per document at 8 concurrent requests on 2 vCPU in testing.
- Request size: ALB/Tomcat defaults are fine for payloads; responses of a few hundred KB.

Fonts: the image installs Liberation/Carlito (metric-compatible with Arial/Calibri). For output
identical to Word on a desktop, add your licensed corporate fonts to the image.

## Audit log

Every document produces one JSON line on logger `docgen.audit`:

```json
{"event":"document.generated","requestId":"curl-demo-001","documentType":"TERM_LOAN_AGREEMENT",
 "templateVersion":"132c9217504e","format":"PDF","payloadSha256":"4f2d…","documentSha256":"a4ff…",
 "pages":3,"renderMs":768,"warnings":0,"evaluationMode":false}
```
`templateVersion` is a hash of the template files, so any document can be traced to the exact
template it came from. The payload itself is not logged (it holds customer data); store it in your
own system of record if you need to regenerate.
