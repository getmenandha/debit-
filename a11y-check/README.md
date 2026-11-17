# a11y-check: vendor-neutral PDF accessibility checker

This tool checks any PDF, whichever product made it (Aspose, Jasper, iText, Tungsten), with open-source tools. It doesn't depend on Aspose, so a vendor's output is never graded by that vendor's own validator.

```bash
mvn -q compile exec:java -Dexec.args="path/to/file.pdf report.md"
```

Exit code is 0 for PDF/UA-1 pass and 3 for fail, so you can use it as a CI gate.

## What it reports

**A. Tag inventory (Apache PDFBox 3.0.8):** reads the PDF tag tree directly.

| # | Check |
|---|---|
| 1 | Title in Info and XMP metadata, and DisplayDocTitle (title bar shows the title, not the file name) |
| 2 | Tagged PDF: MarkInfo/Marked and a structure tree |
| 3 | Document language (/Lang) and any other-language parts (for example a Spanish Span) |
| 4 | Headings H1-H6 in order, no skipped levels, and the PDF bookmarks (outline) count |
| 5 | Paragraphs (P) |
| 6 | Figures with alt text; decorative images marked as artifacts |
| 7 | Links: each link annotation sits inside a Link tag and has /Contents (alternate description) |
| 8 | Footnotes/endnotes: Note tags with IDs, plus Reference tags |
| 9 | Lists: L > LI > Lbl + LBody, and the ListNumbering attribute |
| 10 | Tables: Table > TR > TH (with Scope) / TD, Summary/Alt, THead, Caption |

**B. Rule validation (veraPDF 1.30.4):**
- **PDF/UA-1:** veraPDF's implementation of the Matterhorn Protocol machine checks, the same basis as PAC.
- **WCAG 2.2, machine-checkable part:** uses the profile from veraPDF-validation-profiles, bundled in `src/main/resources/profiles`.

Links from the Aspose evaluation banner are listed separately and ignored in check 7, because they disappear once a license is applied. veraPDF still reports them, at `annots[...]` with a URI on `products.aspose.com`.

## What it can't check

These still need a person, for example with PAC 2024 and NVDA or JAWS:
- whether the alt text is meaningful
- whether the reading order makes sense
- colour contrast in images
- whether a heading actually describes its section

## Licences

veraPDF is MPL 2.0 / GPL 3+ (dual), and PDFBox is Apache 2.0. Both are fine for an internal test tool.
