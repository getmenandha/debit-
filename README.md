# Document generation POC: Aspose.Words + Aspose.PDF (Java)

This repository is the proof of concept for a PDF-as-a-service for loan documents. Templates are authored in Microsoft Word and filled from a JSON payload. The output is a tagged, accessible PDF/UA document.

| Folder | What it is |
|---|---|
| [`docgen-aspose-poc/`](docgen-aspose-poc/README.md) | Java/Maven POC: payload validation, Word template rendering (Aspose.Words), PDF/UA output, signature positions, signature fields and validation (Aspose.PDF), audit record. It also includes the accessibility showcase and the layout tests (dynamic rows, text overflow, two-column layouts, Spanish) |
| [`a11y-check/`](a11y-check/README.md) | Vendor-neutral PDF accessibility checker (veraPDF PDF/UA-1 and WCAG 2.2, plus a PDFBox tag inventory). It works on any PDF, whatever produced it |

## Quick start

Prerequisites: JDK 17+ and Maven. You also need access to the Aspose Maven repo (`https://releases.aspose.com/java/repo/`) or an internal mirror of it, and to Maven Central.

```bash
cd docgen-aspose-poc
mvn test                                                                  # 4 tests
mvn -q compile exec:java -Dexec.args="samples/loan-payload.json out"      # generate a loan agreement
mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.LayoutTests -Dexec.args="out/layout"
mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.AccessibilityShowcase -Dexec.args="out/a11y"

cd ../a11y-check
mvn -q compile exec:java -Dexec.args="../docgen-aspose-poc/out/a11y/accessibility-showcase.pdf report.md"
```

Without `ASPOSE_LICENSE_PATH`, Aspose runs in evaluation mode. That adds a watermark and cuts long documents off. Test results and sample outputs from evaluation mode are in `docgen-aspose-poc/samples/`.
