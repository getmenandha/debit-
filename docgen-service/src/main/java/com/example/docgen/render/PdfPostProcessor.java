package com.example.docgen.render;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.PdfFormat;
import com.aspose.pdf.Rectangle;
import com.aspose.pdf.SignatureField;
import com.aspose.pdf.tagged.logicalstructure.elements.Element;
import com.aspose.pdf.tagged.logicalstructure.elements.StructureElement;
import com.aspose.pdf.tagged.logicalstructure.elements.bls.TableElement;
import com.aspose.pdf.tagged.logicalstructure.elements.bls.TableTHElement;
import org.springframework.stereotype.Component;

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

/** Aspose.PDF stage: optional post-processing and the PDF/UA check. Stateless and thread-safe. */
@Component
public class PdfPostProcessor {

    public record PdfUaReport(boolean compliant, int errors, int warnings, List<String> errorSummary) {}

    private static final Pattern PROBLEM = Pattern.compile(
            "<Problem Severity=\"([^\"]+)\"[^>]*Code=\"([^\"]+)\">([^<]*)</Problem>");

    /** Aspose.PDF's PDF/UA-1 validation, summarised. */
    public PdfUaReport validatePdfUa(byte[] pdfBytes) throws Exception {
        Path log = Files.createTempFile("pdfua-", ".xml");
        try {
            Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
            boolean ok = pdf.validate(log.toString(), PdfFormat.PDF_UA_1);
            Map<String, Integer> grouped = new LinkedHashMap<>();
            int errors = 0, warnings = 0;
            Matcher m = PROBLEM.matcher(Files.readString(log));
            while (m.find()) {
                if ("Error".equals(m.group(1))) {
                    errors++;
                    grouped.merge(m.group(2) + " " + m.group(3), 1, Integer::sum);
                } else if ("Warning".equals(m.group(1))) {
                    warnings++;
                }
            }
            List<String> summary = new ArrayList<>();
            grouped.forEach((k, v) -> summary.add(v + "x " + k));
            return new PdfUaReport(ok, errors, warnings, summary);
        } finally {
            Files.deleteIfExists(log);
        }
    }

    /**
     * Signature-integrated path (internal e-auth): empty signature fields at the measured SIG
     * positions, each inside a /Form structure element so the PDF stays PDF/UA-valid.
     */
    public byte[] addSignatureFields(byte[] pdfBytes, List<SignatureAnchor> anchors,
                                     Map<String, String> signerNames) throws Exception {
        Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
        var tagged = pdf.getTaggedContent();
        var root = tagged.getRootElement();
        for (SignatureAnchor a : anchors) {
            if (!"SIG".equals(a.type())) continue;
            Page page = pdf.getPages().get_Item(a.page());
            SignatureField field = new SignatureField(page,
                    new Rectangle(a.llx() + 3, a.lly() + 3, a.urx() - 3, a.ury() - 3));
            field.setPartialName("sig_" + a.signerId());
            field.setAlternateName("Signature of " + signerNames.getOrDefault(a.signerId(), a.signerId()));
            pdf.getForm().add(field, a.page());
            var formElement = tagged.createFormElement();
            formElement.setAlternativeText(field.getAlternateName());
            root.appendChild(formElement);
            tagWidget(formElement, field);
        }
        return save(pdf);
    }

    /**
     * Aspose.Words tags a table's first column as row headers (TH). For form/layout tables whose
     * Word alt-text description starts with "[layout]", re-tag TH cells as TD and drop the marker.
     */
    public byte[] retagLayoutTables(byte[] pdfBytes) throws Exception {
        Document pdf = new Document(new ByteArrayInputStream(pdfBytes));
        walk(pdf.getTaggedContent().getRootElement(), false);
        return save(pdf);
    }

    private static void walk(Element e, boolean inLayout) {
        boolean layout = inLayout;
        if (e instanceof StructureElement se) {
            String alt = se.getAlternativeText();
            if (e instanceof TableElement && alt != null && alt.startsWith("[layout]")) {
                layout = true;
                se.setAlternativeText(alt.substring("[layout]".length()).trim());
            }
            if (layout && e instanceof TableTHElement) se.setTag("TD");
        }
        for (Object child : e.getChildElements()) {
            if (child instanceof Element c) walk(c, layout);
        }
    }

    private static byte[] save(Document pdf) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        return out.toByteArray();
    }

    /** FormElement.tag(widget) - called reflectively so older Aspose.PDF builds still compile. */
    private static void tagWidget(Object formElement, Object widget) {
        for (Method m : formElement.getClass().getMethods()) {
            if (m.getName().equals("tag") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isInstance(widget)) {
                try {
                    m.invoke(formElement, widget);
                } catch (ReflectiveOperationException ignored) {
                    // leave untagged; the PDF/UA check will report it
                }
                return;
            }
        }
    }
}
