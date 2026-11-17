package com.example.a11y;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vendor-neutral PDF accessibility check. It doesn't depend on Aspose, so you can use it on
 * output from Aspose, Jasper, iText or anything else.
 *
 *  Part A: feature inventory from the PDF tag tree (Apache PDFBox): title, language, headings,
 *          paragraphs, figures and alt text, links, notes and references, lists, tables.
 *  Part B: rule validation with veraPDF against PDF/UA-1 and WCAG 2.2 (machine-checkable rules).
 *
 *   mvn -q compile exec:java -Dexec.args="path/to/file.pdf [report.md]"
 */
public final class A11yCheck {

    private final StringBuilder md = new StringBuilder();
    private final Map<String, List<PDStructureElement>> byType = new TreeMap<>();
    private final Map<PDPage, Map<Integer, String>> mcidText = new HashMap<>();
    private PDDocument doc;

    public static void main(String[] args) throws Exception {
        java.util.logging.LogManager.getLogManager().reset();   // silence veraPDF/PDFBox parser chatter
        if (args.length < 1) {
            System.err.println("usage: A11yCheck <file.pdf> [report.md]");
            System.exit(1);
        }
        File pdf = new File(args[0]);
        A11yCheck check = new A11yCheck();
        check.inventory(pdf);
        boolean ua = check.vera(pdf, PDFAFlavour.PDFUA_1, "PDF/UA-1");
        try {
            check.vera(pdf, PDFAFlavour.WCAG_2_2_MACHINE, "WCAG 2.2 (machine-checkable rules)");
        } catch (Exception e) {
            check.md.append("## B. veraPDF WCAG 2.2: could not run (").append(e.getMessage()).append(")\n\n");
        }
        String report = check.md.toString();
        System.out.println(report);
        if (args.length > 1) Files.writeString(Path.of(args[1]), report);
        System.exit(ua ? 0 : 3);
    }

    // ---------------------------------------------------------------- Part A: inventory

    private void inventory(File file) throws Exception {
        doc = Loader.loadPDF(file);
        PDDocumentCatalog cat = doc.getDocumentCatalog();
        md.append("# Accessibility report: ").append(file.getName()).append("\n\n");
        md.append("## A. Tag inventory (PDFBox)\n\n| # | Check | Result | Details |\n|---|---|---|---|\n");

        // 1. Title
        String infoTitle = doc.getDocumentInformation().getTitle();
        String xmpTitle = xmpTitle(cat);
        PDViewerPreferences vp = cat.getViewerPreferences();
        boolean display = vp != null && vp.displayDocTitle();
        row("1", "Title in metadata, shown in title bar",
                ok(notBlank(infoTitle) && notBlank(xmpTitle) && display),
                "Info: \"" + infoTitle + "\"; XMP dc:title: \"" + xmpTitle + "\"; DisplayDocTitle=" + display);

        // 2. Tagged + language
        boolean marked = cat.getMarkInfo() != null && cat.getMarkInfo().isMarked();
        PDStructureTreeRoot root = cat.getStructureTreeRoot();
        row("2", "Tagged PDF (MarkInfo + structure tree)", ok(marked && root != null),
                "Marked=" + marked + ", StructTreeRoot=" + (root != null));

        if (root == null) { md.append("\nNo structure tree, so nothing else can be checked.\n"); return; }
        for (PDPage p : doc.getPages()) mcidText.put(p, extractMcidText(p));
        for (Object k : root.getKids()) if (k instanceof PDStructureElement e) walk(e);

        List<String> spanLangs = new ArrayList<>();
        for (List<PDStructureElement> l : byType.values())
            for (PDStructureElement e : l)
                if (e.getLanguage() != null && !e.getLanguage().equalsIgnoreCase(cat.getLanguage()))
                    spanLangs.add(e.getStandardStructureType() + " lang=" + e.getLanguage() + " \"" + trim(text(e), 40) + "\"");
        row("3", "Document language (+ foreign-language parts)", ok(notBlank(cat.getLanguage())),
                "/Lang=" + cat.getLanguage() + (spanLangs.isEmpty() ? "; no other-language parts" : "; " + String.join("; ", spanLangs)));

        // 4. Headings
        List<String> heads = new ArrayList<>();
        int last = 0; boolean skipped = false; int hCount = 0;
        for (PDStructureElement e : inOrder()) {
            String t = e.getStandardStructureType();
            if (t != null && t.matches("H[1-6]")) {
                int lvl = t.charAt(1) - '0';
                if (last > 0 && lvl > last + 1) skipped = true;
                if (last == 0 && lvl != 1) skipped = true;
                last = lvl; hCount++;
                heads.add(t + " \"" + trim(text(e), 30) + "\"");
            }
        }
        int outline = countOutline(cat);
        row("4", "Headings: tagged H1-H6, no skipped levels", ok(hCount > 0 && !skipped),
                hCount + " headings" + (skipped ? " (LEVEL SKIPPED)" : "") + ": " + String.join(", ", heads)
                        + "; PDF bookmarks (outline): " + outline);

        // 5. Paragraphs
        row("5", "Paragraphs tagged as P", ok(count("P") > 0), count("P") + " P elements");

        // 6. Figures
        List<String> figs = new ArrayList<>(); boolean allAlt = true;
        for (PDStructureElement f : list("Figure")) {
            boolean has = notBlank(f.getAlternateDescription());
            allAlt &= has;
            figs.add((has ? "alt=\"" + trim(f.getAlternateDescription(), 60) + "\"" : "NO ALT TEXT"));
        }
        List<String> decorative = artifactImages();
        row("6", "Figures have alt text; decorative images are artifacts",
                count("Figure") == 0 ? "n/a" : ok(allAlt),
                count("Figure") + " Figure(s): " + String.join("; ", figs) + "; images marked as artifacts (decorative, incl. any evaluation watermark): " + decorative.size() + " " + decorative);

        // 7. Links
        List<String> links = new ArrayList<>(); boolean linksOk = true; int evalLinks = 0;
        for (PDPage p : doc.getPages()) {
            for (PDAnnotation a : p.getAnnotations()) {
                if (!(a instanceof PDAnnotationLink l)) continue;
                String target = linkTarget(l);
                boolean eval = target.contains("aspose.com");
                if (eval) { evalLinks++; continue; }
                boolean tagged = isInLinkTag(l);
                boolean hasAlt = notBlank(l.getContents());
                linksOk &= tagged && hasAlt;
                links.add(target + " [Link tag=" + tagged + ", Contents=" + (hasAlt ? "\"" + trim(l.getContents(), 45) + "\"" : "MISSING") + "]");
            }
        }
        row("7", "Links: inside a Link tag with an alternate description (/Contents)", links.isEmpty() ? "n/a" : ok(linksOk),
                count("Link") + " Link tags; " + String.join("; ", links)
                        + (evalLinks > 0 ? "; ignored " + evalLinks + " Aspose evaluation-banner link(s)" : ""));

        // 8. Notes and references (footnotes / endnotes)
        List<String> notes = new ArrayList<>(); boolean idsOk = true;
        for (PDStructureElement n : list("Note")) {
            String id = n.getElementIdentifier();
            idsOk &= notBlank(id);
            notes.add("Note id=" + id + " page " + pageNo(n) + " \"" + trim(text(n), 45) + "\"");
        }
        row("8", "Footnotes/endnotes: Note tags with unique IDs (+ Reference)", count("Note") == 0 ? "n/a" : ok(idsOk),
                count("Note") + " Note(s), " + count("Reference") + " Reference(s): " + String.join("; ", notes));

        // 9. Lists
        List<String> lists = new ArrayList<>();
        for (PDStructureElement l : list("L")) {
            String numbering = null;
            for (int i = 0; i < l.getAttributes().size(); i++) {
                PDAttributeObject ao = l.getAttributes().getObject(i);
                if (ao instanceof PDListAttributeObject la) numbering = la.getListNumbering();
            }
            int items = 0;
            for (Object k : l.getKids()) if (k instanceof PDStructureElement c && "LI".equals(c.getStandardStructureType())) items++;
            lists.add(items + " items, ListNumbering=" + numbering);
        }
        boolean listStruct = count("L") > 0 && count("LI") > 0 && count("LBody") > 0;
        row("9", "Bulleted and numbered lists: L > LI > Lbl + LBody", count("L") == 0 ? "n/a" : ok(listStruct),
                count("L") + " L (" + String.join("; ", lists) + "), " + count("LI") + " LI, "
                        + count("Lbl") + " Lbl, " + count("LBody") + " LBody");

        // 10. Tables
        int thScoped = 0;
        for (PDStructureElement th : list("TH"))
            for (int i = 0; i < th.getAttributes().size(); i++)
                if (th.getAttributes().getObject(i) instanceof PDTableAttributeObject ta && ta.getScope() != null) thScoped++;
        List<String> summaries = new ArrayList<>();
        for (PDStructureElement t : list("Table")) {
            String s = null;
            for (int i = 0; i < t.getAttributes().size(); i++)
                if (t.getAttributes().getObject(i) instanceof PDTableAttributeObject ta && ta.getSummary() != null) s = ta.getSummary();
            summaries.add("summary=" + (s == null ? "none" : "\"" + trim(s, 40) + "\"")
                    + ", alt=" + (t.getAlternateDescription() == null ? "none" : "\"" + trim(t.getAlternateDescription(), 40) + "\""));
        }
        row("10", "Tables: Table > TR > TH (with Scope) / TD", count("Table") == 0 ? "n/a"
                        : count("TH") > 0 ? "PASS" : "CHECK (no header cells: fine for layout tables, not for data tables)",
                count("Table") + " Table, " + count("TR") + " TR, " + count("TH") + " TH (" + thScoped + " with Scope), "
                        + count("TD") + " TD, THead=" + count("THead") + ", Caption=" + count("Caption")
                        + "; " + String.join("; ", summaries));

        // Reading order: block-level elements in tag order = the order a screen reader reads
        md.append("\n<details><summary>Reading order (tag order, as a screen reader reads it)</summary>\n\n");
        int n = 0;
        for (PDStructureElement e : inOrder()) {
            String t = e.getStandardStructureType();
            if (t == null || !t.matches("H[1-6]|P|TH|TD|LBody|Caption|Figure")) continue;
            String txt = "Figure".equals(t) ? "[alt] " + e.getAlternateDescription() : text(e);
            if (txt.isBlank()) continue;
            md.append(++n).append(". ").append(t).append(": ").append(trim(txt, 90)).append("  \n");
        }
        md.append("\n</details>\n");
        md.append("\nAll structure types found: ");
        List<String> all = new ArrayList<>();
        byType.forEach((k, v) -> all.add(k + "=" + v.size()));
        md.append(String.join(", ", all)).append("\n\n");
    }

    private void walk(PDStructureElement e) {
        String t = e.getStandardStructureType();
        byType.computeIfAbsent(t == null ? "?" : t, x -> new ArrayList<>()).add(e);
        for (Object k : e.getKids()) if (k instanceof PDStructureElement c) walk(c);
    }

    private List<PDStructureElement> inOrder() {
        List<PDStructureElement> out = new ArrayList<>();
        for (Object k : doc.getDocumentCatalog().getStructureTreeRoot().getKids())
            if (k instanceof PDStructureElement e) collect(e, out);
        return out;
    }

    private void collect(PDStructureElement e, List<PDStructureElement> out) {
        out.add(e);
        for (Object k : e.getKids()) if (k instanceof PDStructureElement c) collect(c, out);
    }

    private int count(String type) { return list(type).size(); }

    private List<PDStructureElement> list(String type) { return byType.getOrDefault(type, List.of()); }

    // ---- text of a structure element via its marked-content ids
    private Map<Integer, String> extractMcidText(PDPage page) throws Exception {
        PDFMarkedContentExtractor ex = new PDFMarkedContentExtractor();
        ex.processPage(page);
        Map<Integer, String> map = new HashMap<>();
        for (PDMarkedContent mc : ex.getMarkedContents()) collectMc(mc, map);
        return map;
    }

    private void collectMc(PDMarkedContent mc, Map<Integer, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Object o : mc.getContents()) {
            if (o instanceof TextPosition tp) sb.append(tp.getUnicode());
            else if (o instanceof PDMarkedContent child) collectMc(child, map);
        }
        if (mc.getMCID() >= 0) map.merge(mc.getMCID(), sb.toString(), String::concat);
    }

    private String text(PDStructureElement e) {
        StringBuilder sb = new StringBuilder();
        PDPage page = pageDeep(e);
        for (Object k : e.getKids()) {
            if (k instanceof Integer mcid && page != null) sb.append(mcidText.getOrDefault(page, Map.of()).getOrDefault(mcid, ""));
            else if (k instanceof PDMarkedContentReference r) {
                PDPage p = r.getPage() != null ? r.getPage() : page;
                if (p != null) sb.append(mcidText.getOrDefault(p, Map.of()).getOrDefault(r.getMCID(), ""));
            } else if (k instanceof PDStructureElement c) sb.append(text(c));
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private PDPage pageOf(PDStructureElement e) {
        Object cur = e;
        while (cur instanceof PDStructureElement se) {
            if (se.getPage() != null) return se.getPage();
            cur = se.getParent();
        }
        return null;
    }

    private PDPage pageDeep(PDStructureElement e) {
        PDPage p = pageOf(e);
        if (p != null) return p;
        for (Object k : e.getKids()) {
            if (k instanceof PDMarkedContentReference r && r.getPage() != null) return r.getPage();
            if (k instanceof PDStructureElement c && (p = pageDeep(c)) != null) return p;
        }
        return null;
    }

    private int pageNo(PDStructureElement e) {
        PDPage p = pageDeep(e);
        return p == null ? -1 : doc.getPages().indexOf(p) + 1;
    }

    // ---- decorative images: image XObjects drawn inside /Artifact marked content
    private List<String> artifactImages() throws Exception {
        List<String> out = new ArrayList<>();
        int page = 0;
        for (PDPage p : doc.getPages()) {
            page++;
            PDFMarkedContentExtractor ex = new PDFMarkedContentExtractor();
            ex.processPage(p);
            for (PDMarkedContent mc : ex.getMarkedContents()) artifactImages(mc, false, page, out);
        }
        return out;
    }

    private void artifactImages(PDMarkedContent mc, boolean inArtifact, int page, List<String> out) {
        boolean art = inArtifact || "Artifact".equals(mc.getTag());
        for (Object o : mc.getContents()) {
            if (o instanceof PDImageXObject im && art) out.add("p" + page + " " + im.getWidth() + "x" + im.getHeight() + "px");
            else if (o instanceof PDMarkedContent c) artifactImages(c, art, page, out);
        }
    }

    // ---- links
    private String linkTarget(PDAnnotationLink l) throws Exception {
        if (l.getAction() instanceof PDActionURI u) return u.getURI();
        if (l.getAction() instanceof PDActionGoTo) return "internal (GoTo)";
        if (l.getDestination() != null) return "internal destination";
        return "unknown";
    }

    private boolean isInLinkTag(PDAnnotationLink annot) {
        for (PDStructureElement link : list("Link"))
            for (Object k : link.getKids())
                if (k instanceof PDObjectReference ref) {
                    COSObjectable o = ref.getReferencedObject();
                    if (o != null && o.getCOSObject() == annot.getCOSObject()) return true;
                }
        return false;
    }

    private int countOutline(PDDocumentCatalog cat) {
        if (cat.getDocumentOutline() == null) return 0;
        int[] n = {0};
        for (PDOutlineItem i : cat.getDocumentOutline().children()) countItems(i, n);
        return n[0];
    }

    private void countItems(PDOutlineItem i, int[] n) {
        n[0]++;
        for (PDOutlineItem c : i.children()) countItems(c, n);
    }

    private String xmpTitle(PDDocumentCatalog cat) throws Exception {
        if (cat.getMetadata() == null) return null;
        try (InputStream in = cat.getMetadata().exportXMPMetadata()) {
            String xmp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("<dc:title>.*?<rdf:li[^>]*>(.*?)</rdf:li>", Pattern.DOTALL).matcher(xmp);
            return m.find() ? m.group(1) : null;
        }
    }

    // ---------------------------------------------------------------- Part B: veraPDF

    private boolean vera(File file, PDFAFlavour flavour, String label) throws Exception {
        VeraGreenfieldFoundryProvider.initialise();
        ValidationResult result;
        try (InputStream in = new FileInputStream(file);
             PDFAParser parser = Foundries.defaultInstance().createParser(in, flavour)) {
            PDFAValidator validator;
            if (flavour == PDFAFlavour.WCAG_2_2_MACHINE) {
                // WCAG profile is not bundled in the veraPDF jars; ours comes from veraPDF-validation-profiles (master)
                validator = Foundries.defaultInstance().createValidator(wcagProfile(), false);
            } else {
                validator = Foundries.defaultInstance().createValidator(flavour, false);
            }
            result = validator.validate(parser);
        }
        Map<String, List<TestAssertion>> failed = new LinkedHashMap<>();
        for (TestAssertion a : result.getTestAssertions())
            if (a.getStatus() == TestAssertion.Status.FAILED)
                failed.computeIfAbsent(a.getRuleId().getClause() + "-" + a.getRuleId().getTestNumber(), k -> new ArrayList<>()).add(a);

        md.append("## B. veraPDF ").append(label).append(": ")
          .append(result.isCompliant() ? "**PASS**" : "**FAIL** (" + failed.size() + " rule(s))")
          .append(" - ").append(result.getTotalAssertions()).append(" checks run, profile has ")
          .append(result.getValidationProfile().getRules().size()).append(" rules\n\n");
        if (!failed.isEmpty()) {
            md.append("| Rule | Failures | Description | Example location |\n|---|---|---|---|\n");
            failed.forEach((rule, list) -> md.append("| ").append(rule).append(" | ").append(list.size()).append(" | ")
                    .append(list.get(0).getMessage().replace("|", "/")).append(" | ")
                    .append(trim(String.valueOf(list.get(0).getLocation().getContext()), 90).replace("|", "/")).append(" |\n"));
            md.append("\n");
        }
        return result.isCompliant();
    }

    /**
     * veraPDF 1.30's XML loader doesn't map the WCAG flavour name, so load the rules under a known
     * flavour name, then rebuild the profile with the WCAG flavour.
     */
    private static ValidationProfile wcagProfile() throws Exception {
        String xml;
        try (InputStream in = A11yCheck.class.getResourceAsStream("/profiles/WCAG-2-2-Machine.xml")) {
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("flavour=\"WCAG_2_2_MACHINE\"", "flavour=\"PDFUA_1\"");
        }
        ValidationProfile p = Profiles.profileFromXml(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return Profiles.profileFromValues(PDFAFlavour.WCAG_2_2_MACHINE, p.getDetails(), p.getHexSha1Digest(),
                p.getRules(), p.getVariables());
    }

    // ---------------------------------------------------------------- helpers

    private void row(String n, String check, String result, String details) {
        md.append("| ").append(n).append(" | ").append(check).append(" | ").append(result).append(" | ")
          .append(details.replace("|", "/")).append(" |\n");
    }

    private static String ok(boolean b) { return b ? "PASS" : "**FAIL**"; }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String trim(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
