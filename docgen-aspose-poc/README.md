# DocGen POC: Aspose.Words + Aspose.PDF (Java)

This POC takes a loan-document JSON payload and returns:

- a validated, filled **DOCX**
- a **tagged PDF/UA-1** document
- **signature locations** as JSON, for DocuSign or internal e-auth
- an optional **signable PDF** with tagged signature fields
- an **audit record**: payload and document hashes, timings, and PDF/UA results

```
payload.json ──► PayloadValidator ──► WordRenderer (Aspose.Words) ──► PdfPostProcessor (Aspose.PDF)
                 strict, all errors     LINQ template → DOCX + PDF/UA     PDF/UA check, signature fields
                 no enrichment          signature XY from Word layout     (signature-integrated mode only)
```

## Run it

Prerequisites: JDK 17+, Maven, and access to the Aspose Maven repo (`https://releases.aspose.com/java/repo/`).
In the enterprise, proxy that repo through Artifactory or Nexus and change the `<repository>` in `pom.xml`.

```bash
# Optional but recommended: a temporary license removes watermarks (get a free 30-day one from Aspose)
export ASPOSE_LICENSE_PATH=/path/to/Aspose.Total.Java.lic
# Optional: folder with the approved fonts (Arial etc.) - essential in Linux containers
export DOCGEN_FONTS_DIR=/usr/share/fonts

mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out"                      # DocGen-only (DocuSign path)
mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out --signature-fields"   # signature-integrated path
mvn -q compile exec:java -Dexec.args="samples/loan-payload-unsecured.json out"            # if/else branch, 1 signer
mvn -q compile exec:java -Dexec.args="samples/loan-payload-invalid.json out"              # rejected, exit code 2
mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out --bench 20"           # single-thread timings
mvn test
```

Output goes to `out/<requestId>/`:

| File | What it is |
|---|---|
| `document.docx` | Filled Word document, for the "Word output later" requirement |
| `document.pdf` | Tagged PDF/UA-1 with a title, language, heading, list and table structure, and table header cells |
| `signature-anchors.json` | One box per signer and type (SIG/DATE): page, `x/yFromTop` (top-left origin, points, as DocuSign tabs expect) and `llx/lly/urx/ury` (PDF space) |
| `pdfua-validation.xml` | Aspose.PDF PDF/UA-1 validation log, kept with the audit record |
| `document-signable.pdf` | Only with `--signature-fields`: empty signature fields, each with a tooltip and placed inside a `/Form` tag |
| `audit.json` | requestId, payload SHA-256, document SHA-256, render and check times, page count, font warnings, PDF/UA summary |

## Template authoring (Word + LINQ Reporting Engine)

Business users edit `src/main/resources/templates/loan-agreement-template.docx` in ordinary Word:

| Need | Tag |
|---|---|
| Field | `<<[borrower.legalName]>>` |
| Date / number format | `<<[agreement.date]:"dd MMMM yyyy">>`, `<<[f.amount]:"#,##0.00">>` |
| Repeating table rows (facilities, signers) | first cell `<<foreach [f in facilities]>><<[f.id]>>` … last cell `…<</foreach>>` |
| Repeating paragraphs / bullets | `<<foreach [c in collateral]>>…` in the list paragraph, `<</foreach>>` at the start of the next paragraph |
| Conditional section | `<<if [collateral.count() > 0]>> … <<else>> … <</if>>` |
| Aggregate | `<<[facilities.sum(f => f.amount)]:"#,##0.00">>` |
| Row number | `<<[f.indexOf() + 1]>>` |
| String length / substring | `<<[name.length()]>>`, `<<[name.substring(0, 37)]>>` |
| Signature location | `<<bookmark ["SIG_" + s.id]>> <</bookmark>>` inside the signature cell |

**Java syntax differs from Aspose's .NET documentation.** In Java, methods are lower-case and strings use Java methods: `count()`, `sum()`, `any()`, `first()`, `indexOf()`, `length()`, `substring()`. The .NET versions (`Count()`, `Sum()`, `.Length`) fail. With `ALLOW_MISSING_MEMBERS` they fail *silently*: the value renders empty, or the wrong if/else branch is chosen. An earlier version of this POC rendered an empty total and "unsecured" this way. `TemplateLintTest` renders every template in strict mode, and `DocGenServiceTest` checks the rendered text, so this now fails the build.

One Word template handles both the fixed-layout parts and the variable rows. Today those are split across Tungsten tagging and Jasper.

For accessibility in the template: use real Heading styles, mark table header rows "Repeat as header row", add table alt text (Table Properties > Alt Text), and add alt text to images.

## What was verified, and what to watch

Built and run on **Aspose.Words 26.10 and Aspose.PDF 26.10 for Java** (JDK 21, evaluation mode, Linux). `mvn test` passes 3 of 3 tests, and all the sample commands work.

- **Speed:** after warm-up, generating one 3-page packet takes about **0.5 s** on a single thread (avg 497 ms, p50 471 ms, max 863 ms over 20 runs on a 2-vCPU sandbox). That includes the PDF/UA check, which adds roughly 40%. The first call takes about 5 s while the JVM, fonts and licence checks load, so warm the service up at startup.
- **Rendering:** the fields, date and number formats, repeating rows and bullets, the totals, and the `if/else` branch for unsecured loans all rendered correctly. Coordinates measured from the Word layout matched what the PDF contained.
- **PDF/UA:** the Words output had **no structural errors**. The only errors were the evaluation banner's hyperlink and the Aspose.PDF evaluation watermark, so retest with a temporary license to confirm a clean result. Two fixes were needed and are already in the code:
  - Word's default bullets used a Symbol font that was missing on Linux, which caused a `.notdef glyph` error. `normaliseBullets` swaps them for a Unicode bullet in Arial.
  - Signature fields added by Aspose.PDF fail PDF/UA (7.18.4) unless they are wrapped in a `/Form` structure element. `addSignatureFields` does this, and those errors went away.
- **Fonts are a production concern:** on the test machine, Arial and Calibri were missing and were replaced by Liberation Sans. The audit record's `renderWarnings` caught this, so you can see it happen. Any substituted font changes layout and can break PDF/UA. Bake the approved fonts into the container image, set `DOCGEN_FONTS_DIR`, and keep an eye on `renderWarnings` in the audit record.
- **Empty arrays:** with no collateral, the template failed until `ALLOW_MISSING_MEMBERS` was turned on. That option also hides template typos. The strict `PayloadValidator` makes up for it on the payload side; add a template lint test per document type.
- **No hidden anchor text:** signature positions come from the Word layout (named bookmarks). DocuSign-style white-text anchors would be read aloud by screen readers.
- Remaining PDF/UA warning: "table summary missing" (a warning, not an error).

## Accessibility tag coverage test

`AccessibilityShowcase` builds one document that uses every tag type on the ADA checklist. It renders the document through Aspose.Words to PDF/UA, then checks it with the separate `a11y-check` tool (veraPDF + PDFBox, no Aspose):

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.AccessibilityShowcase -Dexec.args="out/a11y"
cd ../a11y-check && mvn -q compile exec:java -Dexec.args="../poc/out/a11y/accessibility-showcase.pdf ../poc/out/a11y/accessibility-report.md"
```

Results from the evaluation-mode run are in `samples/accessibility-showcase/`.

## Layout tests (T1-T4)

These cover: dynamic table rows, text longer than its space, two-column layouts (newspaper columns and a side-by-side form like W-9), and Spanish. Results and output PDFs are in `samples/layout-tests/` (see `RESULTS.md`).

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.LayoutTests -Dexec.args="out/layout"
```

Templates are generated by `tools/make_test_templates.py` and `tools/make_loan_templates.py`.

**Evaluation mode cuts long documents off.** Volume tests (for example 40+ facility rows) need a temporary license.

## Questions this POC should answer

1. **ADA:** with a license, does `pdfua-validation.xml` come back clean for each document type? Also confirm with PAC 2024 and a screen reader.
2. **Throughput:** run `--bench` for single-thread latency. For Kafka bulk, run N parallel consumers. Aspose `Document` objects aren't thread-safe, so use one per request.
3. **Template ownership:** can business or ops users maintain the LINQ tags in Word, or does it need a developer?
4. **Licensing cost:** compare per-developer/OEM pricing with Tungsten + iText (AGPL/commercial) + Jasper.

## Next steps if it goes ahead

- Wrap `DocGenService.generate` in a REST endpoint and a Kafka consumer.
- Use one template map per line of business, keyed by `documentType`.
- Map `signature-anchors.json` to DocuSign `signHereTabs` and `dateSignedTabs` (the `x`/`yFromTop` values are in the units DocuSign expects).
- For archiving, try `PdfCompliance.PDF_A_4_UA_2` (archival and accessible in one file), or convert a signed PDF to PDF/A with Aspose.PDF.
- Aspose.PDF can also merge third-party PDFs (valuations etc.) into the packet. Re-run the PDF/UA check afterwards, because untagged inputs break compliance.
