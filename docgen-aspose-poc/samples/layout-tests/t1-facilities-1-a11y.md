# Accessibility report: t1-facilities-1.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | PASS | Info: "Schedule of Facilities"; XMP dc:title: "Schedule of Facilities"; DisplayDocTitle=true |
| 2 | Tagged PDF (MarkInfo + structure tree) | PASS | Marked=true, StructTreeRoot=true |
| 3 | Document language (+ foreign-language parts) | PASS | /Lang=en-US; no other-language parts |
| 4 | Headings: tagged H1-H6, no skipped levels | PASS | 2 headings: H1 "1. Facilities", H1 "2. Next section"; PDF bookmarks (outline): 2 |
| 5 | Paragraphs tagged as P | PASS | 19 P elements |
| 6 | Figures have alt text; decorative images are artifacts | n/a | 0 Figure(s): ; images marked as artifacts (decorative, incl. any evaluation watermark): 1 [p1 920x500px] |
| 7 | Links: inside a Link tag with an alternate description (/Contents) | n/a | 1 Link tags; ; ignored 1 Aspose evaluation-banner link(s) |
| 8 | Footnotes/endnotes: Note tags with unique IDs (+ Reference) | n/a | 0 Note(s), 0 Reference(s):  |
| 9 | Bulleted and numbered lists: L > LI > Lbl + LBody | n/a | 0 L (), 0 LI, 0 Lbl, 0 LBody |
| 10 | Tables: Table > TR > TH (with Scope) / TD | PASS | 1 Table, 3 TR, 8 TH (8 with Scope), 7 TD, THead=0, Caption=0; summary=none, alt="One row per facility: number, id, type, ..." |

<details><summary>Reading order (tag order, as a screen reader reads it)</summary>

1. P: Created with an evaluation copy of Aspose.Words. To remove all limitations, you can use Fr...  
2. P: Schedule of Facilities  
3. P: Borrower: Northwind Logistics LLC. This schedule lists 1 facilities. The table has one row...  
4. H1: 1. Facilities  
5. TH: #  
6. P: #  
7. TH: Facility  
8. P: Facility  
9. TH: Type  
10. P: Type  
11. TH: Purpose  
12. P: Purpose  
13. TH: Amount (USD)  
14. P: Amount (USD)  
15. TH: Maturity  
16. P: Maturity  
17. TH: 1  
18. P: 1  
19. TD: F-001  
20. P: F-001  
21. TD: Term Loan A  
22. P: Term Loan A  
23. TD: General corporate purposes, including working capital and capital expenditure for site 1  
24. P: General corporate purposes, including working capital and capital expenditure for site 1  
25. TD: 100,000.00  
26. P: 100,000.00  
27. TD: 30-Apr-2027  
28. P: 30-Apr-2027  
29. TH: Total  
30. P: Total  
31. TD: 100,000.00  
32. P: 100,000.00  
33. H1: 2. Next section  
34. P: This section is static text that comes straight after the table. However many rows the tab...  

</details>

All structure types found: Document=1, H1=2, Link=1, P=19, TD=7, TH=8, TR=3, Table=1

## B. veraPDF PDF/UA-1: **FAIL** (2 rule(s)) - 2080 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.18.5-2 | 1 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |
| 7.18.1-2 | 1 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **PASS** - 2000 checks run, profile has 108 rules

