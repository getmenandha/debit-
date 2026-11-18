package com.example.docgen.render;

/**
 * Where a signer signs or dates, measured from the Word layout (no anchor text in the PDF).
 *
 *  - x / yFromTop / width / height : top-left origin, points (72 per inch) - the units DocuSign
 *    tabs use (xPosition / yPosition).
 *  - llx / lly / urx / ury          : PDF user space, bottom-left origin, points.
 */
public record SignatureAnchor(
        String type,        // SIG or DATE
        String signerId,
        int page,           // 1-based
        double x, double yFromTop, double width, double height,
        double llx, double lly, double urx, double ury) {
}
