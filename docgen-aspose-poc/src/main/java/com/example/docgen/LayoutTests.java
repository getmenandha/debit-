package com.example.docgen;

import com.aspose.words.CurrentThreadSettings;
import com.aspose.words.Document;
import com.aspose.words.JsonDataLoadOptions;
import com.aspose.words.JsonDataSource;
import com.aspose.words.LayoutCollector;
import com.aspose.words.NodeType;
import com.aspose.words.Paragraph;
import com.aspose.words.PdfCompliance;
import com.aspose.words.PdfSaveOptions;
import com.aspose.words.ReportBuildOptions;
import com.aspose.words.ReportingEngine;
import com.aspose.words.Row;
import com.aspose.words.Table;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Layout tests T1-T4:
 *   T1 dynamic table rows (1 / 5 / 40 facilities), content after the table
 *   T2 text longer than its space: Word layout options + fixed PDF form fields
 *   T3 two-column layouts (newspaper columns and a side-by-side form)
 *   T4 Spanish document (locale-aware dates and numbers, /Lang es-ES)
 *
 *   mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.LayoutTests -Dexec.args="out/layout"
 */
public final class LayoutTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StringBuilder LOG = new StringBuilder();
    private static Path out;

    public static void main(String[] args) throws Exception {
        out = Files.createDirectories(Path.of(args.length > 0 ? args[0] : "out/layout"));
        AsposeLicensing.init();
        t1DynamicRows();
        t2Overflow();
        t2PdfFormFields();
        t3TwoColumns();
        t4Spanish();
        Files.writeString(out.resolve("layout-tests-log.md"), LOG.toString());
        System.out.println(LOG);
    }

    // ------------------------------------------------------------------ T1
    static void t1DynamicRows() throws Exception {
        log("## T1 Dynamic table rows\n\n| Facilities in payload | Data rows rendered | Pages | Table spans pages | Last data row on page | 'Next section' heading on page |\n|---|---|---|---|---|---|");
        String[] types = {"Term Loan A", "Term Loan B", "Revolving Credit", "Equipment Line", "Letter of Credit"};
        for (int n : new int[]{1, 5, 20}) {   // 40+ needs a license: evaluation mode truncates long documents
            ObjectNode p = JSON.createObjectNode();
            p.putObject("borrower").put("legalName", "Northwind Logistics LLC");
            ArrayNode fac = p.putArray("facilities");
            for (int i = 1; i <= n; i++) {
                fac.addObject().put("id", String.format("F-%03d", i)).put("type", types[(i - 1) % types.length])
                        .put("purpose", "General corporate purposes, including working capital and capital expenditure for site " + i)
                        .put("currency", "USD").put("amount", 100_000.0 * i)
                        .put("maturityDate", LocalDate.of(2027, 1, 31).plusMonths(i * 3L).toString());
            }
            Document doc = render("facility-schedule-template.docx", p, "Schedule of Facilities", 1033);
            Table table = (Table) doc.getChild(NodeType.TABLE, 0, true);
            int dataRows = table.getRows().getCount() - 2;          // minus header + total row
            LayoutCollector lc = new LayoutCollector(doc);
            doc.updatePageLayout();
            Row lastData = table.getRows().get(table.getRows().getCount() - 2);
            Paragraph next = findParagraph(doc, "2. Next section");
            log(String.format("| %d | %d | %d | %s | %d | %d |", n, dataRows, doc.getPageCount(),
                    lc.getNumPagesSpanned(table) > 0 ? "yes (header row repeats)" : "no",
                    lc.getStartPageIndex(lastData), lc.getStartPageIndex(next)));
            save(doc, "t1-facilities-" + n + ".pdf");
        }
        log("");
    }

    // ------------------------------------------------------------------ T2
    static void t2Overflow() throws Exception {
        log("## T2 Text longer than its space (Word template)\n");
        String notes = ("The borrower shall deliver quarterly management accounts within 45 days of each quarter end, "
                + "together with a compliance certificate signed by the chief financial officer confirming the debt service "
                + "coverage ratio and total leverage for the relevant period. ").repeat(6);
        String[][] cases = {
                {"short", "Northwind Logistics LLC"},
                {"long", "Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, LLC"}};
        for (String[] c : cases) {
            ObjectNode p = JSON.createObjectNode();
            p.putObject("b").put("name", c[1]).put("notes", notes);
            Document doc = render("overflow-test-template.docx", p, "Text overflow test (" + c[0] + ")", 1033);
            save(doc, "t2-overflow-" + c[0] + "-name.pdf");
            log("- `t2-overflow-" + c[0] + "-name.pdf`: name length " + c[1].length() + ", pages " + doc.getPageCount());
        }
        log("");
    }

    /** Fixed-layout PDF form (the iText/AcroForm approach): fields have a fixed rectangle. */
    static void t2PdfFormFields() throws Exception {
        String longName = "Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, LLC";
        com.aspose.pdf.Document pdf = new com.aspose.pdf.Document();
        com.aspose.pdf.Page page = pdf.getPages().add();
        com.aspose.pdf.TextBuilder tb = new com.aspose.pdf.TextBuilder(page);
        text(tb, 50, 800, 14, "Fixed PDF form fields (each box 250 pt wide)");

        String longNote = "The borrower shall deliver quarterly management accounts within 45 days of each quarter end, "
                + "together with a compliance certificate signed by the chief financial officer confirming the debt "
                + "service coverage ratio and total leverage for the relevant period.";
        Object[][] fields = {
                // label, y, height, multiline, fontSize (0 = auto-fit), value
                {"1. Single-line field, fixed 10 pt font (92-char name)", 740, 20, false, 10.0, longName},
                {"2. Multi-line field, fixed 10 pt font (92-char name)", 670, 34, true, 10.0, longName},
                {"3. Single-line field, auto font size (92-char name)", 600, 20, false, 0.0, longName},
                {"4. Multi-line field, auto font size (92-char name)", 530, 34, true, 0.0, longName},
                {"5. Multi-line field, fixed 10 pt, 3-line box (" + longNote.length() + "-char note)", 450, 34, true, 10.0, longNote},
                {"6. Multi-line field, auto font size, same box (" + longNote.length() + "-char note)", 370, 34, true, 0.0, longNote}};
        int i = 0;
        for (Object[] f : fields) {
            double y = ((Number) f[1]).doubleValue(), h = ((Number) f[2]).doubleValue();
            text(tb, 50, y + h + 6, 9, (String) f[0]);
            com.aspose.pdf.TextBoxField field = new com.aspose.pdf.TextBoxField(page,
                    new com.aspose.pdf.Rectangle(50, y, 300, y + h));
            field.setPartialName("field" + (++i));
            field.setAlternateName((String) f[0]);
            field.setMultiline((Boolean) f[3]);
            field.setDefaultAppearance(new com.aspose.pdf.DefaultAppearance("Helvetica", (Double) f[4], java.awt.Color.BLACK));
            com.aspose.pdf.Border border = new com.aspose.pdf.Border(field);
            border.setWidth(1);
            field.setBorder(border);
            field.getCharacteristics().setBorder(java.awt.Color.BLUE);
            pdf.getForm().add(field, 1);
            field.setValue((String) f[5]);
        }
        text(tb, 50, 330, 9, "The flattened copy shows what prints or is archived.");
        pdf.save(out.resolve("t2-pdf-form-fields-fillable.pdf").toString());
        com.aspose.pdf.Document filled = new com.aspose.pdf.Document(out.resolve("t2-pdf-form-fields-fillable.pdf").toString());
        filled.flatten();
        filled.save(out.resolve("t2-pdf-form-fields-flattened.pdf").toString());
        log("- `t2-pdf-form-fields-*.pdf`: fixed PDF form fields (fillable and flattened)\n");
    }

    // ------------------------------------------------------------------ T3
    static void t3TwoColumns() throws Exception {
        ObjectNode p = JSON.createObjectNode();
        ArrayNode terms = p.putArray("terms");
        String[][] t = {
                {"Interest", "Interest accrues daily on the outstanding principal at the agreed rate and is payable monthly in arrears."},
                {"Prepayment", "The borrower may prepay all or part of the loan on any business day with five business days' notice."},
                {"Fees", "An arrangement fee is payable on signing and a commitment fee accrues on undrawn amounts."},
                {"Covenants", "The borrower will maintain a debt service coverage ratio of at least 1.25x, tested quarterly."},
                {"Events of default", "Non-payment, breach of covenant, insolvency and material adverse change are events of default."},
                {"Governing law", "This agreement is governed by the laws of the State of New York."},
                {"Notices", "Notices must be in writing and delivered to the addresses set out in the schedule."},
                {"Assignment", "The borrower may not assign its rights without the lender's prior written consent."}};
        for (String[] x : t) terms.addObject().put("heading", x[0]).put("text", x[1]);
        p.putObject("taxpayer").put("name", "Jane Alvarez").put("businessName", "Northwind Logistics LLC")
                .put("classification", "LLC").put("exemptPayeeCode", "5").put("reportingExemptionCode", "-")
                .put("address", "200 Harbor Way, Suite 400").put("cityStateZip", "Wilmington, DE 19801")
                .put("accounts", "CUST-778812").put("ein", "12-3456789");
        p.putObject("requester").put("name", "Example Bank N.A.").put("address", "1 Example Plaza, New York, NY 10001");
        Document doc = render("two-column-template.docx", p, "Two-column layout test", 1033);
        save(doc, "t3-two-column-as-rendered.pdf");
        // Post-process: tables marked "[layout]" in Word get TD instead of TH cells.
        Files.write(out.resolve("t3-two-column.pdf"), new PdfPostProcessor()
                .retagLayoutTables(Files.readAllBytes(out.resolve("t3-two-column-as-rendered.pdf"))));
        log("## T3 Two-column layouts\n\n- `t3-two-column.pdf`: pages " + doc.getPageCount() + "\n");
    }

    // ------------------------------------------------------------------ T4
    static void t4Spanish() throws Exception {
        ObjectNode p = JSON.createObjectNode();
        p.putObject("agreement").put("number", "TLA-2026-004512").put("date", "2026-11-16");
        p.putObject("lender").put("name", "Example Bank N.A.");
        p.putObject("borrower").put("legalName", "Logística Peñalara S.L.").put("address", "Calle de Alcalá 45, 28014 Madrid");
        ArrayNode f = p.putArray("facilities");
        f.addObject().put("id", "F-001").put("typeEs", "Préstamo a plazo A").put("currency", "USD").put("amount", 2500000.00).put("maturityDate", "2031-11-16");
        f.addObject().put("id", "F-002").put("typeEs", "Línea de crédito renovable").put("currency", "USD").put("amount", 750000.00).put("maturityDate", "2029-11-16");
        ArrayNode s = p.putArray("signers");
        s.addObject().put("name", "José Muñoz Ibáñez").put("roleEs", "Prestatario, director financiero");
        s.addObject().put("name", "Priya Raman").put("roleEs", "Prestamista, gestora de relaciones");

        Locale previous = CurrentThreadSettings.getLocale();
        CurrentThreadSettings.setLocale(Locale.forLanguageTag("es-ES"));   // month names, decimal comma
        try {
            Document doc = render("spanish-loan-summary-template.docx", p, "Resumen del préstamo a plazo", 3082);
            save(doc, "t4-spanish.pdf");
            log("## T4 Spanish\n\n- `t4-spanish.pdf`: pages " + doc.getPageCount() + "\n");
        } finally {
            CurrentThreadSettings.setLocale(previous);
        }
    }

    // ------------------------------------------------------------------ helpers
    private static Document render(String template, ObjectNode payload, String title, int localeId) throws Exception {
        Document doc;
        try (InputStream in = LayoutTests.class.getResourceAsStream("/templates/" + template)) {
            doc = new Document(in);
        }
        // Strict mode (no ALLOW_MISSING_MEMBERS): every payload here is complete, so any template error fails loudly.
        ReportingEngine engine = new ReportingEngine();
        engine.setOptions(ReportBuildOptions.REMOVE_EMPTY_PARAGRAPHS);
        byte[] json = JSON.writeValueAsBytes(payload);
        // Always keep the JSON root as an object, so a payload with a single top-level member isn't collapsed.
        JsonDataLoadOptions jsonOptions = new JsonDataLoadOptions();
        jsonOptions.setAlwaysGenerateRootObject(true);
        engine.buildReport(doc, new JsonDataSource(new ByteArrayInputStream(json), jsonOptions), "data");
        if (doc.getText().contains("truncated here because it was created in the Evaluation Mode"))
            throw new IllegalStateException(template + ": output truncated by Aspose evaluation mode - use a license");
        WordRenderer.normaliseBullets(doc);
        WordRenderer.markTextBoxFramesDecorative(doc);
        doc.getBuiltInDocumentProperties().setTitle(title);
        doc.getStyles().getDefaultFont().setLocaleId(localeId);
        return doc;
    }

    private static void save(Document doc, String name) throws Exception {
        doc.save(out.resolve(name).toString(), pdfOptions());
    }

    private static PdfSaveOptions pdfOptions() {
        PdfSaveOptions o = new PdfSaveOptions();
        o.setCompliance(PdfCompliance.PDF_UA_1);
        o.setExportDocumentStructure(true);
        o.setDisplayDocTitle(true);
        o.setExportLanguageToSpanTag(true);
        o.getOutlineOptions().setHeadingsOutlineLevels(3);
        return o;
    }

    private static Paragraph findParagraph(Document doc, String startsWith) {
        for (Object o : doc.getChildNodes(NodeType.PARAGRAPH, true)) {
            Paragraph p = (Paragraph) o;
            if (p.getText().trim().startsWith(startsWith)) return p;
        }
        throw new IllegalStateException("paragraph not found: " + startsWith);
    }

    private static void text(com.aspose.pdf.TextBuilder tb, double x, double y, double size, String s) {
        com.aspose.pdf.TextFragment tf = new com.aspose.pdf.TextFragment(s);
        tf.setPosition(new com.aspose.pdf.Position(x, y));
        tf.getTextState().setFontSize((float) size);
        tb.appendText(tf);
    }

    private static void log(String s) {
        LOG.append(s).append('\n');
    }
}
