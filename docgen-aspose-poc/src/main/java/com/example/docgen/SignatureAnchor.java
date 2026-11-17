package com.example.docgen;

/**
 * Where a signer signs / dates, measured from the Word layout (no anchor text in the PDF).
 *
 * Two coordinate systems are returned because consumers need different ones:
 *  - x / yFromTop / width / height : top-left origin, points (72 per inch). This is what
 *    DocuSign tabs (xPosition / yPosition) expect.
 *  - llx / lly / urx / ury          : PDF user space, bottom-left origin, points. This is what
 *    Aspose.PDF / iText / PDFBox expect.
 */
public record SignatureAnchor(
        String type,        // SIG or DATE
        String signerId,
        int page,           // 1-based
        double x, double yFromTop, double width, double height,
        double llx, double lly, double urx, double ury) {
}
