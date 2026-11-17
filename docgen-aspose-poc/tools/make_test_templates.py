"""
Builds the Word templates for the layout tests (T1-T4) with python-docx.
In real life business users author these in Word; this script just makes the tests reproducible.

    pip install python-docx
    python tools/make_test_templates.py src/main/resources/templates
"""
import sys
from xml.sax.saxutils import escape
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls
from docx.shared import Pt, Inches, RGBColor

OUT = sys.argv[1] if len(sys.argv) > 1 else "."


# ---------------------------------------------------------------- helpers
def base(lang="en-US", title=""):
    d = Document()
    st = d.styles["Normal"]
    st.font.name = "Arial"
    st.font.size = Pt(10)
    rpr = st.element.get_or_add_rPr()
    l = OxmlElement("w:lang"); l.set(qn("w:val"), lang); rpr.append(l)
    # make heading fonts explicit (avoid theme Calibri substitution)
    for s in ("Title", "Heading 1", "Heading 2", "Heading 3"):
        d.styles[s].font.name = "Arial"
    d.core_properties.title = title
    return d


def header_row(row):
    trPr = row._tr.get_or_add_trPr()
    e = OxmlElement("w:tblHeader"); e.set(qn("w:val"), "true"); trPr.append(e)


def cant_split(row):
    trPr = row._tr.get_or_add_trPr()
    e = OxmlElement("w:cantSplit"); e.set(qn("w:val"), "true"); trPr.insert(0, e)


def row_height(row, twips, rule):
    trPr = row._tr.get_or_add_trPr()
    e = OxmlElement("w:trHeight"); e.set(qn("w:val"), str(twips)); e.set(qn("w:hRule"), rule); trPr.append(e)


def fit_text(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    e = OxmlElement("w:tcFitText"); e.set(qn("w:val"), "true"); tcPr.append(e)


def alt_text(table, title, desc):
    tblPr = table._tbl.tblPr
    c = OxmlElement("w:tblCaption"); c.set(qn("w:val"), title); tblPr.append(c)
    d = OxmlElement("w:tblDescription"); d.set(qn("w:val"), desc); tblPr.append(d)


def col_widths(table, inches):
    table.autofit = False
    for row in table.rows:
        for i, w in enumerate(inches):
            row.cells[i].width = Inches(w)


def bold_cells(row, texts):
    for c, t in zip(row.cells, texts):
        c.text = ""
        r = c.paragraphs[0].add_run(t); r.bold = True


def label(d, text):
    p = d.add_paragraph()
    r = p.add_run(text); r.italic = True; r.font.size = Pt(8.5); r.font.color.rgb = RGBColor(0x55, 0x55, 0x55)
    return p


def vml_textbox(paragraph, text_tag, width_pt, height_pt, grow):
    """Fixed-size Word text box (VML). grow=True -> 'Resize shape to fit text'."""
    fit = ";mso-fit-shape-to-text:t" if grow else ""
    xml = (
        f'<w:r {nsdecls("w")} xmlns:v="urn:schemas-microsoft-com:vml">'
        f'<w:pict><v:shape type="#_x0000_t202" style="width:{width_pt}pt;height:{height_pt}pt" '
        f'strokecolor="#2E75B6" strokeweight="1pt"><v:textbox style="{fit.lstrip(";")}">'
        f'<w:txbxContent><w:p><w:r><w:t xml:space="preserve">{escape(text_tag)}</w:t></w:r></w:p></w:txbxContent>'
        f'</v:textbox></v:shape></w:pict></w:r>'
    )
    paragraph._p.append(parse_xml(xml))


def two_columns(section, n=2, space_twips=432):
    cols = section._sectPr.find(qn("w:cols"))
    if cols is None:
        cols = OxmlElement("w:cols"); section._sectPr.append(cols)
    cols.set(qn("w:num"), str(n)); cols.set(qn("w:space"), str(space_twips))


def layout_table(table):
    """Form/layout table: untick Word's 'Header Row' and 'First Column' table-style options,
    so Aspose doesn't tag cells as TH (row/column headers)."""
    look = table._tbl.tblPr.find(qn("w:tblLook"))
    if look is None:
        look = OxmlElement("w:tblLook"); table._tbl.tblPr.append(look)
    for k, v in {"w:val": "0400", "w:firstRow": "0", "w:lastRow": "0", "w:firstColumn": "0",
                 "w:lastColumn": "0", "w:noHBand": "0", "w:noVBand": "1"}.items():
        look.set(qn(k), v)


def no_borders(table):
    tblPr = table._tbl.tblPr
    b = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        e = OxmlElement(f"w:{edge}"); e.set(qn("w:val"), "nil"); b.append(e)
    tblPr.append(b)


# ---------------------------------------------------------------- T1 dynamic rows
def t1():
    d = base(title="Schedule of Facilities")
    d.add_heading("Schedule of Facilities", 0)
    d.add_paragraph("Borrower: <<[borrower.legalName]>>. This schedule lists <<[facilities.count()]>> "
                    "facilities. The table has one row per facility in the payload.")
    d.add_heading("1. Facilities", 1)
    t = d.add_table(rows=3, cols=6); t.style = "Table Grid"
    bold_cells(t.rows[0], ["#", "Facility", "Type", "Purpose", "Amount (USD)", "Maturity"])
    header_row(t.rows[0])
    vals = ['<<foreach [f in facilities]>><<[f.indexOf() + 1]>>', "<<[f.id]>>", "<<[f.type]>>", "<<[f.purpose]>>",
            '<<[f.amount]:"#,##0.00">>', '<<[f.maturityDate]:"dd-MMM-yyyy">><</foreach>>']
    for i, v in enumerate(vals): t.rows[1].cells[i].text = v
    tot = t.rows[2].cells
    tot[0].merge(tot[3]).text = ""
    tot[0].paragraphs[0].add_run("Total").bold = True
    tot[4].text = ""; tot[4].paragraphs[0].add_run('<<[facilities.sum(f => f.amount)]:"#,##0.00">>').bold = True
    for r in t.rows: cant_split(r)
    col_widths(t, [0.35, 0.75, 1.2, 2.2, 1.1, 0.95])
    alt_text(t, "Facilities", "One row per facility: number, id, type, purpose, amount and maturity, with a total row")
    d.add_heading("2. Next section", 1)
    d.add_paragraph("This section is static text that comes straight after the table. However many rows the "
                    "table has, this heading and paragraph follow it directly, with no fixed gap and no overlap.")
    d.save(f"{OUT}/facility-schedule-template.docx")


# ---------------------------------------------------------------- T2 overflow
def t2():
    d = base(title="Text overflow test")
    d.add_heading("Text overflow test", 0)
    d.add_paragraph("Each block shows what happens when a value is longer than the space designed for it. "
                    "Borrower name length in this payload: <<[b.name.length()]>> characters.")

    d.add_heading("A. Flowing paragraph", 1)
    label(d, "Expected: the text wraps and everything after it moves down.")
    d.add_paragraph("The borrower, <<[b.name]>>, agrees to the terms below. This sentence follows the name.")

    d.add_heading("B. Table cell, automatic row height", 1)
    label(d, "Expected: the cell wraps onto more lines and the row gets taller.")
    t = d.add_table(rows=2, cols=2); t.style = "Table Grid"
    t.rows[0].cells[0].text = "Borrower name"; t.rows[0].cells[1].text = "<<[b.name]>>"
    t.rows[1].cells[0].text = "Next row"; t.rows[1].cells[1].text = "This row is pushed down."
    col_widths(t, [1.5, 2.5])

    d.add_heading("C. Table cell, exact row height (0.25 in)", 1)
    label(d, "Expected: the row height is fixed, so text beyond the first line is cut off.")
    t = d.add_table(rows=2, cols=2); t.style = "Table Grid"
    t.rows[0].cells[0].text = "Borrower name"; t.rows[0].cells[1].text = "<<[b.name]>>"
    row_height(t.rows[0], 360, "exact")
    t.rows[1].cells[0].text = "Next row"; t.rows[1].cells[1].text = "This row does not move."
    col_widths(t, [1.5, 2.5])

    d.add_heading("D. Table cell with Word 'Fit text'", 1)
    label(d, "Expected: the text is squeezed horizontally to stay on one line.")
    t = d.add_table(rows=1, cols=2); t.style = "Table Grid"
    t.rows[0].cells[0].text = "Borrower name"; t.rows[0].cells[1].text = "<<[b.name]>>"
    fit_text(t.rows[0].cells[1])
    col_widths(t, [1.5, 2.5])

    d.add_heading("E. Fixed-size text box (200 x 22 pt)", 1)
    label(d, "Expected: the box keeps its size, so overflowing text is hidden.")
    p = d.add_paragraph(); vml_textbox(p, "<<[b.name]>>", 200, 22, grow=False)
    d.add_paragraph("Paragraph after the fixed text box.")

    d.add_heading("F. Text box set to 'Resize shape to fit text'", 1)
    label(d, "Expected: the box grows taller to fit the text.")
    p = d.add_paragraph(); vml_textbox(p, "<<[b.name]>>", 200, 22, grow=True)
    d.add_paragraph("Paragraph after the auto-grow text box.")

    d.add_heading("G. Template rule: smaller font when the value is long", 1)
    label(d, "Rule in the template: if the name is longer than 40 characters, print it at 7 pt, otherwise 10 pt.")
    t = d.add_table(rows=1, cols=2); t.style = "Table Grid"
    t.rows[0].cells[0].text = "Borrower name"
    c = t.rows[0].cells[1]; c.text = ""; p = c.paragraphs[0]
    p.add_run("<<if [b.name.length() > 40]>>")
    r = p.add_run("<<[b.name]>>"); r.font.size = Pt(7)
    p.add_run("<<else>>")
    r = p.add_run("<<[b.name]>>"); r.font.size = Pt(10)
    p.add_run("<</if>>")
    col_widths(t, [1.5, 2.5])

    d.add_heading("H. Template rule: truncate with an ellipsis", 1)
    label(d, "Rule in the template: cut at 37 characters and add '...'. Not suitable for legal names.")
    d.add_paragraph('<<[b.name.length() > 40 ? b.name.substring(0, 37) + "..." : b.name]>>')

    d.add_heading("I. Long free-text field (notes)", 1)
    label(d, "Expected: a long note flows across lines and pages like normal text.")
    d.add_paragraph("<<[b.notes]>>")
    d.save(f"{OUT}/overflow-test-template.docx")


# ---------------------------------------------------------------- T3 two columns
CHECK = "<<[{expr} ? \"☒\" : \"☐\"]>>"


def checkbox_run(p, expr, text):
    r = p.add_run(CHECK.format(expr=expr)); r.font.name = "DejaVu Sans"
    p.add_run(" " + text + "   ")


def t3():
    d = base(title="Two-column layout test")
    d.add_heading("Two-column layout test", 0)
    d.add_paragraph("Part A uses newspaper columns: text flows down the left column, then continues in the right one. "
                    "Part B is a form laid out side by side, like a tax-form layout.")

    # Part A: section with 2 newspaper columns
    sec = d.add_section(WD_SECTION.CONTINUOUS); two_columns(sec)
    d.add_heading("Part A. Terms and conditions", 1)
    d.add_paragraph("<<foreach [t in terms]>><<[t.heading]>>: <<[t.text]>>")
    d.add_paragraph("<</foreach>>")

    # back to one column
    sec = d.add_section(WD_SECTION.CONTINUOUS); two_columns(sec, 1)
    d.add_heading("Part B. Taxpayer information (side-by-side form)", 1)
    t = d.add_table(rows=6, cols=2); t.style = "Table Grid"
    col_widths(t, [4.2, 2.3])
    a = t.rows[0].cells[0].merge(t.rows[0].cells[1]); a.text = ""
    a.paragraphs[0].add_run("1  Name (as shown on your income tax return)").font.size = Pt(7)
    a.add_paragraph("<<[taxpayer.name]>>")
    b = t.rows[1].cells[0].merge(t.rows[1].cells[1]); b.text = ""
    b.paragraphs[0].add_run("2  Business name, if different from above").font.size = Pt(7)
    b.add_paragraph("<<[taxpayer.businessName]>>")
    c = t.rows[2].cells[0]; c.text = ""
    c.paragraphs[0].add_run("3  Tax classification (check one)").font.size = Pt(7)
    p = c.add_paragraph()
    checkbox_run(p, 'taxpayer.classification == "INDIVIDUAL"', "Individual")
    checkbox_run(p, 'taxpayer.classification == "C_CORP"', "C corporation")
    p = c.add_paragraph()
    checkbox_run(p, 'taxpayer.classification == "S_CORP"', "S corporation")
    checkbox_run(p, 'taxpayer.classification == "LLC"', "Limited liability company")
    e = t.rows[2].cells[1]; e.text = ""
    e.paragraphs[0].add_run("4  Exemptions").font.size = Pt(7)
    e.add_paragraph("Exempt payee code: <<[taxpayer.exemptPayeeCode]>>")
    e.add_paragraph("Reporting exemption code: <<[taxpayer.reportingExemptionCode]>>")
    f = t.rows[3].cells[0]; f.text = ""
    f.paragraphs[0].add_run("5  Address (number, street, and apt. or suite no.)").font.size = Pt(7)
    f.add_paragraph("<<[taxpayer.address]>>")
    g = t.rows[3].cells[1].merge(t.rows[4].cells[1]); g.text = ""
    g.paragraphs[0].add_run("Requester's name and address").font.size = Pt(7)
    g.add_paragraph("<<[requester.name]>>"); g.add_paragraph("<<[requester.address]>>")
    h = t.rows[4].cells[0]; h.text = ""
    h.paragraphs[0].add_run("6  City, state, and ZIP code").font.size = Pt(7)
    h.add_paragraph("<<[taxpayer.cityStateZip]>>")
    k = t.rows[5].cells[0].merge(t.rows[5].cells[1]); k.text = ""
    k.paragraphs[0].add_run("7  Account number(s)").font.size = Pt(7)
    k.add_paragraph("<<[taxpayer.accounts]>>")
    alt_text(t, "Taxpayer information form", "[layout] Taxpayer information form, fields 1 to 7")
    layout_table(t)

    d.add_heading("Part I. Taxpayer Identification Number (TIN)", 2)
    t2 = d.add_table(rows=1, cols=2); t2.style = "Table Grid"; col_widths(t2, [4.2, 2.3])
    t2.rows[0].cells[0].text = ("Enter your TIN in the appropriate box. The TIN must match the name given on line 1. "
                                "For individuals, this is generally your social security number.")
    s = t2.rows[0].cells[1]; s.text = ""
    s.paragraphs[0].add_run("Employer identification number").font.size = Pt(7)
    r = s.add_paragraph().add_run("<<[taxpayer.ein]>>"); r.font.size = Pt(14); r.font.name = "DejaVu Sans Mono"
    alt_text(t2, "TIN", "[layout] Taxpayer identification number")
    layout_table(t2)
    d.add_paragraph("Certification: under penalties of perjury, I certify that the information on this form is correct.")
    d.save(f"{OUT}/two-column-template.docx")


# ---------------------------------------------------------------- T4 Spanish
def t4():
    d = base(lang="es-ES", title="Resumen del préstamo a plazo")
    d.add_heading("Resumen del préstamo a plazo", 0)
    d.add_paragraph("Contrato n.º <<[agreement.number]>>, firmado el <<[agreement.date]:\"dd 'de' MMMM 'de' yyyy\">>.")
    d.add_heading("1. Las partes", 1)
    d.add_paragraph("Este contrato se celebra entre <<[lender.name]>> (el «Prestamista») y <<[borrower.legalName]>>, "
                    "con domicilio en <<[borrower.address]>> (el «Prestatario»).")
    d.add_heading("2. Líneas de crédito", 1)
    t = d.add_table(rows=2, cols=4); t.style = "Table Grid"
    bold_cells(t.rows[0], ["Línea", "Tipo", "Importe", "Vencimiento"]); header_row(t.rows[0])
    for i, v in enumerate(['<<foreach [f in facilities]>><<[f.id]>>', "<<[f.typeEs]>>",
                           '<<[f.currency]>> <<[f.amount]:"#,##0.00">>', '<<[f.maturityDate]:"d \'de\' MMMM \'de\' yyyy">><</foreach>>']):
        t.rows[1].cells[i].text = v
    alt_text(t, "Líneas de crédito", "Cada línea con su tipo, importe y fecha de vencimiento")
    p = d.add_paragraph(); p.add_run("Importe total comprometido: ").bold = True
    p.add_run('<<[facilities.sum(f => f.amount)]:"#,##0.00">> USD')
    d.add_heading("3. Condiciones", 1)
    for s in ["El interés se calcula sobre el saldo pendiente, con base en un año de 360 días.",
              "¿Tiene preguntas? ¡Llámenos! Atención al cliente en español: lunes a viernes.",
              "Pequeña muestra de caracteres: á é í ó ú ü ñ Á É Í Ó Ú Ü Ñ ¿ ¡ « » €."]:
        d.add_paragraph(s, style="List Bullet")
    d.add_heading("4. Firmas", 1)
    d.add_paragraph("<<foreach [s in signers]>><<[s.name]>>, <<[s.roleEs]>>")
    d.add_paragraph("<</foreach>>")
    d.save(f"{OUT}/spanish-loan-summary-template.docx")


t1(); t2(); t3(); t4()
print("templates written to", OUT)
