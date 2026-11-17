package com.example.docgen;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.PdfFormat;
import com.aspose.pdf.Rectangle;
import com.aspose.pdf.SignatureField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 2 - Aspose.PDF.
 *
 *  - validatePdfUa: machine check against PDF/UA-1, XML log kept with the audit trail.
 *  - addSignatureFields: "signature-integrated" mode for the internal e-auth path - adds empty
 *    AcroForm signature fields at the measured locations, tagged inside /Form structure elements
 *    so the document stays PDF/UA-valid. In the DocGen-only (DocuSign) path you skip this and
 *    send the anchors JSON with the PDF instead.
 */
public final class PdfPostProcessor {

    public record PdfUaReport(boolean compliant, int errors, int warnings, List<String> errorSummary) {}

    private static final Pattern PROBLEM = Pattern.compile(
            "<Problem Severity=\"([^\"]+)\"[^>]*Page=\"([^\"]*)\"[^>]*Code=\"([^\"]+)\">([^<]*)</Problem>");

    public PdfUaReport validatePdfUa(byte[] pdfBytes, Path logFile) throws Exception {
        Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
        boolean ok = pdf.validate(logFile.toString(), PdfFormat.PDF_UA_1);

        String log = Files.readString(logFile);
        Map<String, Integer> grouped = new LinkedHashMap<>();
        int errors = 0, warnings = 0;
        Matcher m = PROBLEM.matcher(log);
        while (m.find()) {
            String severity = m.group(1);
            if ("Error".equals(severity)) {
                errors++;
                grouped.merge(m.group(3) + " " + m.group(4), 1, Integer::sum);
            } else if ("Warning".equals(severity)) {
                warnings++;
            }
        }
        List<String> summary = new ArrayList<>();
        grouped.forEach((k, v) -> summary.add(v + "x " + k));
        return new PdfUaReport(ok, errors, warnings, summary);
    }

    public byte[] addSignatureFields(byte[] pdfBytes, List<SignatureAnchor> anchors,
                                     Map<String, String> signerNames) throws Exception {
        Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
        var tagged = pdf.getTaggedContent();
        var root = tagged.getRootElement();

        for (SignatureAnchor a : anchors) {
            if (!"SIG".equals(a.type())) continue;   // DocuSign/e-auth supply "date signed" themselves
            Page page = pdf.getPages().get_Item(a.page());
            // Inset 3pt so the field sits inside the cell borders.
            SignatureField field = new SignatureField(page,
                    new Rectangle(a.llx() + 3, a.lly() + 3, a.urx() - 3, a.ury() - 3));
            field.setPartialName("sig_" + a.signerId());
            // AlternateName = /TU tooltip; screen readers announce it for the field.
            field.setAlternateName("Signature of " + signerNames.getOrDefault(a.signerId(), a.signerId()));
            pdf.getForm().add(field, a.page());

            // PDF/UA 7.18.4: widget annotations must sit inside a /Form structure element.
            var formElement = tagged.createFormElement();
            formElement.setAlternativeText(field.getAlternateName());
            root.appendChild(formElement);
            if (!tagWidget(formElement, field)) {
                System.err.println("[pdf] FormElement.tag(...) not available in this Aspose.PDF version - "
                        + "signature field will be reported as untagged by the PDF/UA check");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        return out.toByteArray();
    }

    /**
     * Aspose.Words tags the first column of every table as row headers (TH). That's right for data
     * tables, but wrong for forms laid out with a table. Tables whose Word alt-text description starts
     * with "[layout]" are fixed here: TH cells are re-tagged as TD and the marker is removed from the alt text.
     */
    public byte[] retagLayoutTables(byte[] pdfBytes) throws Exception {
        Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
        int[] changed = {0};
        walk(pdf.getTaggedContent().getRootElement(), false, changed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        return out.toByteArray();
    }

    private static void walk(com.aspose.pdf.tagged.logicalstructure.elements.Element e, boolean inLayout, int[] changed) {
        boolean layout = inLayout;
        if (e instanceof com.aspose.pdf.tagged.logicalstructure.elements.StructureElement se) {
            String alt = se.getAlternativeText();
            if (e instanceof com.aspose.pdf.tagged.logicalstructure.elements.bls.TableElement && alt != null
                    && alt.startsWith("[layout]")) {
                layout = true;
                se.setAlternativeText(alt.substring("[layout]".length()).trim());
            }
            if (layout && e instanceof com.aspose.pdf.tagged.logicalstructure.elements.bls.TableTHElement) {
                se.setTag("TD");
                changed[0]++;
            }
        }
        for (Object child : e.getChildElements()) {
            if (child instanceof com.aspose.pdf.tagged.logicalstructure.elements.Element c) walk(c, layout, changed);
        }
    }

    /**
     * FormElement.tag(widget) works in Aspose.PDF for Java 26.10 (verified: no PDF/UA 7.18.4 errors).
     * Called reflectively so the POC still compiles if your pinned version predates it.
     */
    private static boolean tagWidget(Object formElement, Object widget) {
        for (Method m : formElement.getClass().getMethods()) {
            if (m.getName().equals("tag") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isInstance(widget)) {
                try {
                    m.invoke(formElement, widget);
                    return true;
                } catch (ReflectiveOperationException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
