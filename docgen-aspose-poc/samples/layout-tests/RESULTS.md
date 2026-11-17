# Layout tests T1-T4: results (Aspose.Words / Aspose.PDF 26.10 for Java, evaluation mode)

Regenerate the outputs with:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.docgen.LayoutTests -Dexec.args="out/layout"
```

Every PDF was also checked with `a11y-check` (veraPDF PDF/UA-1 and WCAG 2.2, plus a PDFBox tag inventory). The reports are the `*-a11y.md` files next to each PDF.

## T1: Dynamic table rows

| Facilities in payload | Data rows rendered | Pages | Table spans pages | Last data row on page | "Next section" heading on page |
|---|---|---|---|---|---|
| 1 | 1 | 1 | no | 1 | 1 |
| 5 | 5 | 1 | no | 1 | 1 |
| 20 | 20 | 2 | yes, header row repeats | 2 | 2 |

- **Rows:** one per array item, from `<<foreach [f in facilities]>>` on a single template row. Row numbers come from `f.indexOf() + 1`, and the total from `facilities.sum(f => f.amount)`.
- **What follows the table:** the next section starts directly after the last row. There is no fixed gap and no overlap.
- **Long tables:** the header row repeats on each new page ("Repeat as header row" in Word). No row splits across pages ("Allow row to break across pages" is off).
- **40+ rows:** not tested. Evaluation mode cuts the document off ("This document was truncated here because it was created in the Evaluation Mode"). Volume tests need a temporary license.

## T2: Text longer than its space

The same template was rendered with a 23-character name and a 92-character name.

| # | Where the value sits | What happens with the long value | Accessibility | Recommendation |
|---|---|---|---|---|
| A | Flowing paragraph | Wraps; everything after it moves down | OK | Default |
| B | Table cell, automatic row height | Wraps to 3 lines; the row gets taller and later rows move down | OK | Default for form-like layouts |
| C | Table cell, **exact** row height | **Cut off** mid-line. The hidden text is still in the PDF text layer, so screen readers and copy-paste pick it up mixed into the next row | Poor | Avoid |
| D | Table cell, Word "Fit text" | Squeezed horizontally onto one line, so long values become very narrow | OK, but hard to read | Only for short values with a known maximum length |
| E | Fixed-size text box | **Cut off**; hidden text still in the text layer | Poor | Avoid |
| F | Text box set to "Resize shape to fit text" | Box grows taller | OK (after the fix below) | Works, but table cells are simpler |
| G | Template rule: smaller font if the value is longer than 40 characters | 7 pt instead of 10 pt | OK, but set a minimum size | Good for bounded boxes |
| H | Template rule: truncate with "..." | Value is cut | Loses data | Never for names, amounts or legal text |
| I | Long free text (notes) | Flows across lines and pages | OK | Default |

**Fixed PDF form fields** (`t2-pdf-form-fields-*.pdf`, Aspose.PDF; same approach as filling a form with iText):

| Field | Long value |
|---|---|
| Single-line, fixed 10 pt | Aspose **shrinks** the text to about 5 pt to fit, even though 10 pt was set |
| Multi-line, fixed 10 pt, room for 2-3 lines | Fits if short. A 256-character note is **cut off mid-line** |
| Single- or multi-line, auto font size | Always fits, but the note dropped to about 6.5 pt |

These results are for Aspose's own rendering and the flattened copy. Acrobat may redraw fields that are left fillable differently, so flatten them before sending.

**Fix applied:** Word text boxes were exported as a Figure with no alt text, which fails PDF/UA 7.3. `WordRenderer.markTextBoxFramesDecorative` now marks the box outline decorative, and the text inside stays tagged. Template authors can do the same in Word with "Mark as decorative".

**Bottom line:** in the Word-based flow nothing is cut off unless the template uses a fixed height (C, E) or a fixed PDF form field. For fixed-layout boxes, set a maximum length per field in the payload validator so the request is rejected with a clear error, rather than shrinking or cutting the value.

## T3: Two-column layouts

- **Part A, newspaper columns** (a Word section set to 2 columns): text fills the left column, then continues in the right. Reading order is correct: the whole left column, then the right one.
- **Part B, side-by-side form** like the W-9 layout (a table with merged cells): renders correctly, and the checkboxes follow the payload (☒/☐ from a template rule). Reading order is correct, field 1 to 7, row by row.
- **Fix applied:** Aspose.Words tags the first column of every table as row headers (TH), and has no setting to stop it. That's right for data tables but wrong for forms. Tables whose Word alt-text description starts with `[layout]` are re-tagged by `PdfPostProcessor.retagLayoutTables` (TH changed to TD, marker removed).
- **Checkbox glyphs:** ☐ and ☒ need a font that contains them (DejaVu Sans here). Choose an approved corporate font with these glyphs.

## T4: Spanish

- **Characters:** á é í ó ú ü ñ, the uppercase versions, ¿ ¡ « » and € all render correctly and are extracted correctly as text.
- **Dates:** `"d 'de' MMMM 'de' yyyy"` gives "16 de noviembre de 2026".
- **Numbers:** `"#,##0.00"` gives "2.500.000,00" with the locale set per request: `CurrentThreadSettings.setLocale(es-ES)`. The setting is per thread, so it's safe with parallel consumers.
- **Language tag:** the document language is es-ES (`/Lang`), so screen readers use Spanish pronunciation. PDF/UA passes apart from the evaluation banner.

## Accessibility results across all outputs

| File | veraPDF PDF/UA-1 | veraPDF WCAG 2.2 | Tag inventory |
|---|---|---|---|
| t1-facilities-1 / 5 / 20 | Only the evaluation-banner link | PASS | all PASS |
| t2-overflow-short / long | Only the evaluation-banner link | PASS | all PASS |
| t3-two-column | Evaluation-banner link, plus 7.1-3 from the Aspose.PDF evaluation watermark added when re-saving (the file before re-saving has no 7.1-3) | 7.1-3 only (same cause) | all PASS; layout tables have no header cells, as intended |
| t4-spanish | Only the evaluation-banner link | PASS | all PASS, /Lang=es-ES |
