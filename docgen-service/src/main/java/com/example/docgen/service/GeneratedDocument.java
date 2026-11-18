package com.example.docgen.service;

import com.example.docgen.render.PdfPostProcessor.PdfUaReport;
import com.example.docgen.render.SignatureAnchor;

import java.util.List;

/** Everything one generate call produced. */
public record GeneratedDocument(
        String requestId,
        String documentType,
        String templateVersion,
        OutputFormat format,
        byte[] content,
        String sha256,
        int pageCount,
        List<SignatureAnchor> signatureAnchors,
        List<String> warnings,
        PdfUaReport pdfUa,          // null unless docgen.pdfua-check=true
        boolean evaluationMode,
        long renderMillis) {

    public String fileName() {
        return documentType.toLowerCase() + "-" + requestId + "." + format.extension;
    }
}
