package com.example.docgen;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.FieldHyperlink;
import com.aspose.words.FootnoteType;
import com.aspose.words.ParagraphAlignment;
import com.aspose.words.PdfCompliance;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.SaveFormat;
import com.aspose.words.Shape;
import com.aspose.words.StyleIdentifier;
import com.aspose.words.Table;
import com.aspose.words.Underline;

import java.awt.Color;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Accessibility showcase: one document that exercises every tag type on the ADA checklist,
 * rendered by Aspose.Words to PDF/UA-1. Check the output with the separate a11y-check tool
 * (veraPDF + PDFBox), which doesn't depend on Aspose.
 *
 * Checklist covered:
 *   1  Title in metadata (+ DisplayDocTitle)      7  Footnote (bottom of page) and endnote
 *   2  Document language + a foreign-language span 8  Bulleted list (with a nested level)
 *   3  Headings H1-H3 + PDF bookmarks (outline)     9  Numbered list
 *   4  Paragraphs                                  10  Table: header row, alt text/summary, caption
 *   5  Figure with alt text + caption; decorative  11  Internal cross-reference link
 *      image marked as artifact
 *   6  External hyperlink with screen tip
 *
 *   mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.AccessibilityShowcase -Dexec.args="out/a11y"
 */
public final class AccessibilityShowcase {

    public static void main(String[] args) throws Exception {
        Path out = Files.createDirectories(Path.of(args.length > 0 ? args[0] : "out/a11y"));
        AsposeLicensing.init();

        Document doc = new Document();
        DocumentBuilder b = new DocumentBuilder(doc);
        doc.getStyles().getDefaultFont().setName("Arial");
        doc.getStyles().getDefaultFont().setSize(11);
        doc.getStyles().getDefaultFont().setLocaleId(1033);                      // (2) en-US
        doc.getBuiltInDocumentProperties().setTitle("Accessibility Test - Term Loan Summary");   // (1)
        doc.getBuiltInDocumentProperties().setSubject("PDF/UA tag coverage test");
        doc.getBuiltInDocumentProperties().setAuthor("DocGen POC");

        // (3)(4) headings and paragraphs
        heading(b, StyleIdentifier.TITLE, "Term Loan Summary");
        heading(b, StyleIdentifier.HEADING_1, "1. Overview");
        para(b, "This document tests every accessibility feature the document generation service needs. "
                + "Each section below uses one feature, so a screen reader and a PDF/UA validator can confirm it.");
        b.write("Borrowers can request this document in large print, braille or audio. ");
        b.write("Accessible formats are also available in Spanish: ");
        b.getFont().setLocaleId(3082);                                             // (2) es-ES span
        b.write("Este documento está disponible en formatos accesibles.");
        b.getFont().setLocaleId(1033);
        b.writeln();

        // (5) decorative image -> artifact (screen readers skip it)
        Shape divider = image(b, "/images/divider.png", 450, 5);
        divider.isDecorative(true);                                               // Java setter name in Aspose.Words
        b.writeln();
        b.getParagraphFormat().clearFormatting();

        // (6) external hyperlink with screen tip
        heading(b, StyleIdentifier.HEADING_1, "2. Links and references");
        b.write("Read our ");
        FieldHyperlink ext = link(b, "accessibility statement", "https://www.example.com/accessibility", false);
        ext.setScreenTip("Example Bank accessibility statement (opens website)");
        b.write(" for more information.");
        b.writeln();

        // (11) internal cross-reference to a bookmark later in the document
        b.write("Fees are described in ");
        FieldHyperlink internal = link(b, "section 5, Fees", "section_fees", true);
        internal.setScreenTip("Go to section 5, Fees");
        b.write(".");
        b.writeln();

        // (7) footnote at the bottom of the page + endnote at the end of the document
        b.write("Interest is calculated on an actual/360 basis");
        b.insertFootnote(FootnoteType.FOOTNOTE,
                "Actual/360 means interest accrues daily on the outstanding balance, using a 360-day year.");
        b.write(" and is payable monthly in arrears");
        b.insertFootnote(FootnoteType.ENDNOTE,
                "Payment dates that fall on a non-business day move to the next business day.");
        b.writeln(".");

        // (8) bulleted list with a nested level
        heading(b, StyleIdentifier.HEADING_1, "3. Lists");
        heading(b, StyleIdentifier.HEADING_2, "3.1 Conditions precedent (bulleted)");
        b.getListFormat().applyBulletDefault();
        b.writeln("Signed facility agreement");
        b.writeln("Evidence of insurance");
        b.getListFormat().listIndent();
        b.writeln("Property insurance");
        b.writeln("Business interruption insurance");
        b.getListFormat().listOutdent();
        b.writeln("Board resolution");
        b.getListFormat().removeNumbers();

        // (9) numbered list
        heading(b, StyleIdentifier.HEADING_2, "3.2 Drawdown steps (numbered)");
        b.getListFormat().applyNumberDefault();
        b.writeln("Submit a drawdown request five business days in advance.");
        b.writeln("The lender confirms the conditions are met.");
        b.writeln("Funds are credited to the operating account.");
        b.getListFormat().removeNumbers();

        // (5) informative figure with alt text and caption
        heading(b, StyleIdentifier.HEADING_1, "4. Facilities");
        heading(b, StyleIdentifier.HEADING_2, "4.1 Chart");
        Shape chart = image(b, "/images/facility-chart.png", 400, 200);
        chart.setAlternativeText("Bar chart of committed amount by facility: Term Loan A USD 2.50 million, "
                + "Revolving Credit USD 0.75 million, Equipment Line USD 0.40 million.");
        chart.setTitle("Committed amount by facility");
        b.writeln();
        b.getParagraphFormat().clearFormatting();
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.CAPTION);
        b.writeln("Figure 1. Committed amount by facility");
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.NORMAL);

        // (10) data table with a header row, alt text (summary) and caption
        heading(b, StyleIdentifier.HEADING_2, "4.2 Facility table");
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.CAPTION);
        b.writeln("Table 1. Facilities");
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.NORMAL);
        Table table = b.startTable();
        b.getRowFormat().setHeadingFormat(true);                                   // header row -> TH
        b.getFont().setBold(true);
        for (String h : new String[]{"Facility", "Type", "Amount (USD)", "Maturity"}) { b.insertCell(); b.write(h); }
        b.endRow();
        b.getRowFormat().setHeadingFormat(false);
        b.getFont().setBold(false);
        String[][] rows = {
                {"F-001", "Term Loan A", "2,500,000.00", "16 Nov 2031"},
                {"F-002", "Revolving Credit", "750,000.00", "16 Nov 2029"},
                {"F-003", "Equipment Line", "400,000.00", "31 May 2030"}};
        for (String[] r : rows) {
            for (String c : r) { b.insertCell(); b.write(c); }
            b.endRow();
        }
        b.endTable();
        table.setTitle("Facilities");
        table.setDescription("Each facility with its type, committed amount in US dollars and maturity date.");
        b.writeln();
        heading(b, StyleIdentifier.HEADING_3, "4.2.1 Maturity notes");
        para(b, "Maturity dates are fixed at signing and can only be extended by written amendment.");

        // (11) bookmark target for the internal link
        b.startBookmark("section_fees");
        heading(b, StyleIdentifier.HEADING_1, "5. Fees");
        b.endBookmark("section_fees");
        para(b, "An arrangement fee of 0.50% of the total commitment is payable on signing. "
                + "A commitment fee of 0.25% a year is payable on undrawn revolving credit amounts.");

        // Outputs
        doc.save(out.resolve("accessibility-showcase.docx").toString(), SaveFormat.DOCX);
        PdfSaveOptions o = new PdfSaveOptions();
        o.setCompliance(PdfCompliance.PDF_UA_1);
        o.setExportDocumentStructure(true);
        o.setDisplayDocTitle(true);
        o.getOutlineOptions().setHeadingsOutlineLevels(3);                         // (3) PDF bookmarks
        o.setExportLanguageToSpanTag(true);                                        // (2) Lang on the Spanish span
        doc.save(out.resolve("accessibility-showcase.pdf").toString(), o);

        PdfPostProcessor.PdfUaReport ua = new PdfPostProcessor().validatePdfUa(
                Files.readAllBytes(out.resolve("accessibility-showcase.pdf")), out.resolve("aspose-pdfua-validation.xml"));
        System.out.println("Saved " + out.resolve("accessibility-showcase.pdf").toAbsolutePath());
        System.out.println("Aspose.PDF PDF/UA-1 check: " + ua);
    }

    private static void heading(DocumentBuilder b, int style, String text) {
        b.getParagraphFormat().clearFormatting();
        b.getFont().clearFormatting();
        b.getParagraphFormat().setStyleIdentifier(style);
        b.writeln(text);
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.NORMAL);
    }

    private static void para(DocumentBuilder b, String text) {
        b.getParagraphFormat().clearFormatting();
        b.getFont().clearFormatting();
        b.getParagraphFormat().setStyleIdentifier(StyleIdentifier.NORMAL);
        b.writeln(text);
    }

    private static Shape image(DocumentBuilder b, String resource, double widthPt, double heightPt) throws Exception {
        try (InputStream in = AccessibilityShowcase.class.getResourceAsStream(resource)) {
            Shape s = b.insertImage(in);
            s.setWidth(widthPt);
            s.setHeight(heightPt);
            b.getCurrentParagraph().getParagraphFormat().setAlignment(ParagraphAlignment.CENTER);
            return s;
        }
    }

    private static FieldHyperlink link(DocumentBuilder b, String text, String target, boolean isBookmark) throws Exception {
        b.getFont().setColor(new Color(0x1F, 0x4E, 0x79));
        b.getFont().setUnderline(Underline.SINGLE);
        FieldHyperlink f = (FieldHyperlink) b.insertHyperlink(text, target, isBookmark);
        b.getFont().clearFormatting();
        return f;
    }
}
