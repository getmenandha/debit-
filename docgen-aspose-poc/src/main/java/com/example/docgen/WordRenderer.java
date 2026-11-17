package com.example.docgen;

import com.aspose.words.Bookmark;
import com.aspose.words.Document;
import com.aspose.words.FontSettings;
import com.aspose.words.ImportFormatMode;
import com.aspose.words.JsonDataLoadOptions;
import com.aspose.words.JsonDataSource;
import com.aspose.words.LayoutCollector;
import com.aspose.words.LayoutEntityType;
import com.aspose.words.LayoutEnumerator;
import com.aspose.words.ListLevel;
import com.aspose.words.NodeType;
import com.aspose.words.NumberStyle;
import com.aspose.words.PdfCompliance;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.ReportBuildOptions;
import com.aspose.words.ReportingEngine;
import com.aspose.words.SaveFormat;
import com.aspose.words.Section;
import com.aspose.words.SectionStart;
import com.aspose.words.WarningType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1 - Aspose.Words.
 *
 * Word template (LINQ Reporting Engine tags) + JSON payload  ->  DOCX + tagged PDF/UA-1,
 * plus signature/date coordinates measured from the Word layout.
 *
 * Template syntax used (see src/main/resources/templates/loan-agreement-template.docx):
 *   <<[borrower.legalName]>>                         simple field
 *   <<[agreement.date]:"dd MMMM yyyy">>              date format
 *   <<[f.amount]:"#,##0.00">>                        number format
 *   <<foreach [f in facilities]>> ... <</foreach>>   repeating table rows / list paragraphs
 *   <<if [collateral.Count() > 0]>> ... <<else>> ... <</if>>
 *   <<[facilities.Sum(f => f.amount)]>>              aggregates
 *   <<bookmark ["SIG_" + s.id]>> <</bookmark>>       named, per-signer signature location
 */
public final class WordRenderer {

    public record RenderResult(byte[] docx, byte[] pdf, List<SignatureAnchor> anchors,
                               int pageCount, List<String> warnings) {}

    private static final String TEMPLATE_DIR = "/templates/";

    public WordRenderer() {
        // Server fonts matter: missing fonts get substituted, which changes layout and can break PDF/UA
        // (e.g. bullet glyphs falling back to a symbol font). Point this at the approved corporate font set.
        String fontsDir = System.getenv("DOCGEN_FONTS_DIR");
        if (fontsDir != null && !fontsDir.isBlank()) {
            FontSettings.getDefaultInstance().setFontsFolders(new String[]{fontsDir}, true);
        }
    }

    public RenderResult render(String templateName, byte[] payloadJson, String title,
                               List<String> appendTemplates) throws Exception {
        List<String> warnings = new ArrayList<>();
        Document doc = loadTemplate(templateName);
        doc.setWarningCallback(info -> {
            if (info.getWarningType() == WarningType.FONT_SUBSTITUTION) {
                warnings.add("Font substitution: " + info.getDescription());
            }
        });

        // 1. Merge data. ALLOW_MISSING_MEMBERS lets empty arrays (e.g. unsecured -> collateral: [])
        //    render; correctness of the payload is enforced up front by PayloadValidator.
        ReportingEngine engine = new ReportingEngine();
        engine.setOptions(ReportBuildOptions.REMOVE_EMPTY_PARAGRAPHS | ReportBuildOptions.ALLOW_MISSING_MEMBERS);
        // Always keep the JSON root as an object, so a payload with a single top-level member isn't collapsed.
        JsonDataLoadOptions jsonOptions = new JsonDataLoadOptions();
        jsonOptions.setAlwaysGenerateRootObject(true);
        engine.buildReport(doc, new JsonDataSource(new ByteArrayInputStream(payloadJson), jsonOptions), "loan");

        // 2. Assemble the packet (static disclosures etc.) at Word level, so tagging stays intact.
        for (String extra : appendTemplates) {
            Document part = loadTemplate(extra);
            part.getFirstSection().getPageSetup().setSectionStart(SectionStart.NEW_PAGE);
            doc.appendDocument(part, ImportFormatMode.KEEP_SOURCE_FORMATTING);
        }

        // 3. Accessibility hygiene.
        normaliseBullets(doc);
        markTextBoxFramesDecorative(doc);
        doc.getBuiltInDocumentProperties().setTitle(title);   // PDF/UA requires a title
        doc.getStyles().getDefaultFont().setLocaleId(1033);   // en-US -> /Lang in the PDF

        // 4. Measure signature locations from the final layout.
        List<SignatureAnchor> anchors = findSignatureAnchors(doc);

        // 5. Save outputs.
        ByteArrayOutputStream docx = new ByteArrayOutputStream();
        doc.save(docx, SaveFormat.DOCX);

        PdfSaveOptions pdfOptions = new PdfSaveOptions();
        pdfOptions.setCompliance(PdfCompliance.PDF_UA_1);  // tagged PDF, structure tree, alt text
        pdfOptions.setExportDocumentStructure(true);
        pdfOptions.setDisplayDocTitle(true);
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        doc.save(pdf, pdfOptions);

        return new RenderResult(docx.toByteArray(), pdf.toByteArray(), anchors, doc.getPageCount(), warnings);
    }

    private Document loadTemplate(String name) throws Exception {
        try (InputStream in = WordRenderer.class.getResourceAsStream(TEMPLATE_DIR + name)) {
            if (in == null) throw new IllegalArgumentException("Template not found: " + name);
            return new Document(in);
        }
    }

    /**
     * Word text boxes export as an untagged Figure (the box outline) plus the text inside as normal
     * paragraphs. A Figure with no alt text fails PDF/UA 7.3, so mark text-box outlines decorative
     * (= artifact). The text inside stays tagged and readable. Authors can also tick
     * "Mark as decorative" on the text box in Word.
     */
    static void markTextBoxFramesDecorative(Document doc) {
        for (Object o : doc.getChildNodes(com.aspose.words.NodeType.SHAPE, true)) {
            com.aspose.words.Shape shape = (com.aspose.words.Shape) o;
            boolean noAlt = shape.getAlternativeText() == null || shape.getAlternativeText().isBlank();
            if (shape.getShapeType() == com.aspose.words.ShapeType.TEXT_BOX && noAlt) {
                shape.isDecorative(true);
            }
        }
    }

    /** Use a real Unicode bullet in a standard font instead of the Symbol-font glyph Word uses by default. */
    static void normaliseBullets(Document doc) {
        for (com.aspose.words.List list : doc.getLists()) {
            for (ListLevel level : list.getListLevels()) {
                if (level.getNumberStyle() == NumberStyle.BULLET) {
                    level.getFont().setName("Arial");
                    level.setNumberFormat("•");
                }
            }
        }
    }

    /**
     * Finds every SIG_<signerId> / DATE_<signerId> bookmark and returns the rectangle of the
     * table cell that contains it. Because the location comes from the layout engine, there is
     * no hidden anchor text in the PDF (white anchor text would be read out by screen readers).
     */
    private static List<SignatureAnchor> findSignatureAnchors(Document doc) throws Exception {
        LayoutCollector collector = new LayoutCollector(doc);
        doc.updatePageLayout();   // the collector records node->layout mapping during this pass
        LayoutEnumerator layout = new LayoutEnumerator(doc);
        List<SignatureAnchor> anchors = new ArrayList<>();

        for (Bookmark bm : doc.getRange().getBookmarks()) {
            String name = bm.getName();
            String type;
            if (name.startsWith("SIG_")) type = "SIG";
            else if (name.startsWith("DATE_")) type = "DATE";
            else continue;
            String signerId = name.substring(type.length() + 1);

            Object entity = collector.getEntity(bm.getBookmarkStart());
            if (entity == null) entity = collector.getEntity(bm.getBookmarkStart().getParentNode());
            if (entity == null) continue;
            layout.setCurrent(entity);
            // Climb from the line/span up to the enclosing cell so the whole cell is the signing box.
            while (layout.getType() != LayoutEntityType.CELL && layout.moveParent()) { /* climb */ }

            java.awt.geom.Rectangle2D r = layout.getRectangle();
            int page = layout.getPageIndex();
            Section section = (Section) bm.getBookmarkStart().getAncestor(NodeType.SECTION);
            double pageHeight = section.getPageSetup().getPageHeight();

            anchors.add(new SignatureAnchor(type, signerId, page,
                    round(r.getX()), round(r.getY()), round(r.getWidth()), round(r.getHeight()),
                    round(r.getX()), round(pageHeight - r.getY() - r.getHeight()),
                    round(r.getX() + r.getWidth()), round(pageHeight - r.getY())));
        }
        return anchors;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
