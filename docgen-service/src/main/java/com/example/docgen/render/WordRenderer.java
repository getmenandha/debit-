package com.example.docgen.render;

import com.aspose.words.Bookmark;
import com.aspose.words.CurrentThreadSettings;
import com.aspose.words.Document;
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
import com.aspose.words.Shape;
import com.aspose.words.ShapeType;
import com.aspose.words.WarningType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aspose.Words stage: Word template (LINQ Reporting Engine tags) + JSON payload -> filled document,
 * saved as tagged PDF/UA-1 or DOCX. Stateless and thread-safe: one Aspose Document per call.
 *
 * Template syntax (Java engine - note lower-case functions): {@code <<[borrower.legalName]>>},
 * {@code <<foreach [f in facilities]>>...<</foreach>>}, {@code <<if [collateral.count() > 0]>>},
 * {@code <<[facilities.sum(f => f.amount)]:"#,##0.00">>}, {@code <<bookmark ["SIG_" + s.id]>>}.
 */
@Component
public class WordRenderer {

    /** Result of filling a template: the live document plus what we measured from its layout. */
    public record Rendered(Document document, List<SignatureAnchor> anchors, int pageCount, List<String> warnings) {}

    private static final Map<String, Integer> LCID = Map.of(
            "en-US", 1033, "en-GB", 2057, "es-ES", 3082, "es-US", 21514, "es-MX", 2058, "fr-FR", 1036);

    public Rendered render(byte[] template, List<byte[]> appended, byte[] payloadJson, String title,
                           String localeTag, boolean allowMissingMembers) throws Exception {
        List<String> warnings = new ArrayList<>();
        Locale previous = CurrentThreadSettings.getLocale();
        CurrentThreadSettings.setLocale(Locale.forLanguageTag(localeTag));   // dates, number formats
        try {
            Document doc = new Document(new ByteArrayInputStream(template));
            doc.setWarningCallback(info -> {
                if (info.getWarningType() == WarningType.FONT_SUBSTITUTION) {
                    warnings.add("Font substitution: " + info.getDescription());
                }
            });

            ReportingEngine engine = new ReportingEngine();
            int options = ReportBuildOptions.REMOVE_EMPTY_PARAGRAPHS;
            if (allowMissingMembers) options |= ReportBuildOptions.ALLOW_MISSING_MEMBERS;
            engine.setOptions(options);
            JsonDataLoadOptions json = new JsonDataLoadOptions();
            json.setAlwaysGenerateRootObject(true);
            engine.buildReport(doc, new JsonDataSource(new ByteArrayInputStream(payloadJson), json), "data");

            for (byte[] part : appended) {
                Document p = new Document(new ByteArrayInputStream(part));
                p.getFirstSection().getPageSetup().setSectionStart(SectionStart.NEW_PAGE);
                doc.appendDocument(p, ImportFormatMode.KEEP_SOURCE_FORMATTING);
            }

            normaliseBullets(doc);
            markTextBoxFramesDecorative(doc);
            doc.getBuiltInDocumentProperties().setTitle(title);
            doc.getStyles().getDefaultFont().setLocaleId(LCID.getOrDefault(localeTag, 1033));

            if (doc.getText().contains("truncated here because it was created in the Evaluation Mode")) {
                warnings.add("Output truncated by Aspose evaluation mode - configure a license");
            }

            List<SignatureAnchor> anchors = findSignatureAnchors(doc);
            return new Rendered(doc, anchors, doc.getPageCount(), warnings);
        } finally {
            CurrentThreadSettings.setLocale(previous);
        }
    }

    public byte[] toPdf(Document doc) throws Exception {
        PdfSaveOptions o = new PdfSaveOptions();
        o.setCompliance(PdfCompliance.PDF_UA_1);          // tagged PDF: structure tree, alt text, language
        o.setExportDocumentStructure(true);
        o.setDisplayDocTitle(true);
        o.setExportLanguageToSpanTag(true);
        o.getOutlineOptions().setHeadingsOutlineLevels(3); // PDF bookmarks from headings
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out, o);
        return out.toByteArray();
    }

    public byte[] toDocx(Document doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out, SaveFormat.DOCX);
        return out.toByteArray();
    }

    /** Unicode bullet in a standard font, instead of Word's Symbol-font glyph (a PDF/UA failure). */
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

    /** Text-box outlines become artifacts; the text inside stays tagged (else PDF/UA 7.3 fails). */
    static void markTextBoxFramesDecorative(Document doc) {
        for (Object o : doc.getChildNodes(NodeType.SHAPE, true)) {
            Shape shape = (Shape) o;
            boolean noAlt = shape.getAlternativeText() == null || shape.getAlternativeText().isBlank();
            if (shape.getShapeType() == ShapeType.TEXT_BOX && noAlt) {
                shape.isDecorative(true);
            }
        }
    }

    /** Rectangles of the table cells holding SIG_<id> / DATE_<id> bookmarks. */
    static List<SignatureAnchor> findSignatureAnchors(Document doc) throws Exception {
        List<SignatureAnchor> anchors = new ArrayList<>();
        boolean any = false;
        for (Bookmark bm : doc.getRange().getBookmarks()) {
            if (bm.getName().startsWith("SIG_") || bm.getName().startsWith("DATE_")) { any = true; break; }
        }
        if (!any) return anchors;

        LayoutCollector collector = new LayoutCollector(doc);
        doc.updatePageLayout();
        LayoutEnumerator layout = new LayoutEnumerator(doc);
        for (Bookmark bm : doc.getRange().getBookmarks()) {
            String name = bm.getName();
            String type = name.startsWith("SIG_") ? "SIG" : name.startsWith("DATE_") ? "DATE" : null;
            if (type == null) continue;
            Object entity = collector.getEntity(bm.getBookmarkStart());
            if (entity == null) entity = collector.getEntity(bm.getBookmarkStart().getParentNode());
            if (entity == null) continue;
            layout.setCurrent(entity);
            while (layout.getType() != LayoutEntityType.CELL && layout.moveParent()) { /* climb to the cell */ }
            java.awt.geom.Rectangle2D r = layout.getRectangle();
            Section section = (Section) bm.getBookmarkStart().getAncestor(NodeType.SECTION);
            double pageHeight = section.getPageSetup().getPageHeight();
            anchors.add(new SignatureAnchor(type, name.substring(type.length() + 1), layout.getPageIndex(),
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
