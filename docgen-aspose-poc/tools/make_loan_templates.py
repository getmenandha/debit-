"""Builds loan-agreement-template.docx and disclosures.docx (Java LINQ syntax: count(), sum()).
    python tools/make_loan_templates.py src/main/resources/templates/loan-agreement-template.docx
"""
import sys
from docx import Document
from docx.shared import Pt, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

def header_row(row):
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement('w:tblHeader'); el.set(qn('w:val'), 'true'); trPr.append(el)

def alt_text(table, title, desc):
    tblPr = table._tbl.tblPr
    c = OxmlElement('w:tblCaption'); c.set(qn('w:val'), title); tblPr.append(c)
    d = OxmlElement('w:tblDescription'); d.set(qn('w:val'), desc); tblPr.append(d)

def set_lang(doc):
    rpr = doc.styles['Normal'].element.get_or_add_rPr()
    lang = OxmlElement('w:lang'); lang.set(qn('w:val'), 'en-US'); rpr.append(lang)

out = sys.argv[1]
d = Document(); set_lang(d)
d.styles['Normal'].font.name = 'Arial'; d.styles['Normal'].font.size = Pt(10.5)
d.core_properties.title = 'Term Loan Agreement'
d.add_heading('Term Loan Agreement', 0)
p = d.add_paragraph(); p.add_run('Agreement No: ').bold = True; p.add_run('<<[agreement.number]>>')
p = d.add_paragraph(); p.add_run('Agreement Date: ').bold = True; p.add_run('<<[agreement.date]:"dd MMMM yyyy">>')

d.add_heading('1. Parties', 1)
d.add_paragraph('This agreement is made between <<[lender.name]>> (the "Lender") and '
                '<<[borrower.legalName]>>, <<[borrower.address]>> (the "Borrower"), '
                'customer reference <<[borrower.customerId]>>.')

d.add_heading('2. Facilities', 1)
d.add_paragraph('The Lender makes available the following facilities to the Borrower:')
t = d.add_table(rows=2, cols=5); t.style = 'Table Grid'
for i, h in enumerate(['Facility', 'Type', 'Currency', 'Amount', 'Maturity']):
    t.rows[0].cells[i].text = h
    t.rows[0].cells[i].paragraphs[0].runs[0].bold = True
header_row(t.rows[0]); alt_text(t, 'Facilities', 'Facilities made available: id, type, currency, amount and maturity date')
vals = ['<<foreach [f in facilities]>><<[f.id]>>', '<<[f.type]>>', '<<[f.currency]>>',
        '<<[f.amount]:"#,##0.00">>', '<<[f.maturityDate]:"dd-MMM-yyyy">><</foreach>>']
for i, v in enumerate(vals): t.rows[1].cells[i].text = v
p = d.add_paragraph(); p.add_run('Total committed amount: ').bold = True
p.add_run('<<[facilities.sum(f => f.amount)]:"#,##0.00">>')

d.add_heading('3. Collateral', 1)
d.add_paragraph('<<if [collateral.count() > 0]>>The facilities are secured by:')
d.add_paragraph('<<foreach [c in collateral]>><<[c.description]>> valued at <<[c.currency]>> '
                '<<[c.value]:"#,##0.00">> (LTV limit <<[c.ltvLimitPct]>>%)', style='List Bullet')
d.add_paragraph('<</foreach>><<else>>The facilities are unsecured.<</if>>')

d.add_heading('4. Covenants', 1)
d.add_paragraph('<<foreach [cv in covenants]>><<[cv.name]>>: <<[cv.threshold]>>, tested <<[cv.frequency]>>',
                style='List Number')
d.add_paragraph('<</foreach>>')

d.add_heading('5. Signatures', 1)
d.add_paragraph('Each party signs below to accept the terms of this agreement.').paragraph_format.keep_with_next = True
t = d.add_table(rows=2, cols=4); t.style = 'Table Grid'
for i, h in enumerate(['Name', 'Role', 'Signature', 'Date']):
    t.rows[0].cells[i].text = h; t.rows[0].cells[i].paragraphs[0].runs[0].bold = True
header_row(t.rows[0]); alt_text(t, 'Signatures', 'Signer name, role, signature and date for each party')
vals = ['<<foreach [s in signers]>><<[s.name]>>', '<<[s.role]>>',
        '<<bookmark ["SIG_" + s.id]>> <</bookmark>>', '<<bookmark ["DATE_" + s.id]>> <</bookmark>><</foreach>>']
for i, v in enumerate(vals): t.rows[1].cells[i].text = v
for r in t.rows:
    r.height = Pt(36)
    trPr = r._tr.get_or_add_trPr(); cs = OxmlElement('w:cantSplit'); cs.set(qn('w:val'), 'true'); trPr.insert(0, cs)
    for c in r.cells:
        for p in c.paragraphs: p.paragraph_format.keep_with_next = True
d.save(out)

# disclosures (static), appended to make a document packet
d2 = Document(); set_lang(d2); d2.styles['Normal'].font.name = 'Arial'
d2.add_heading('Important Disclosures', 1)
for s in ['Interest is calculated on the outstanding principal on an actual/360 basis.',
          'Late payments may incur a fee as described in the fee schedule.',
          'You may contact us at any time to request this document in an alternative format.']:
    d2.add_paragraph(s, style='List Bullet')
d2.save(out.replace('loan-agreement-template', 'disclosures'))
