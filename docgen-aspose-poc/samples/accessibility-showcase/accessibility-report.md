# Accessibility report: accessibility-showcase.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | PASS | Info: "Accessibility Test - Term Loan Summary"; XMP dc:title: "Accessibility Test - Term Loan Summary"; DisplayDocTitle=true |
| 2 | Tagged PDF (MarkInfo + structure tree) | PASS | Marked=true, StructTreeRoot=true |
| 3 | Document language (+ foreign-language parts) | PASS | /Lang=en-US; Span lang=es-ES "Este documento está disponible en format..." |
| 4 | Headings: tagged H1-H6, no skipped levels | PASS | 11 headings: H1 "Term Loan Summary", H1 "1. Overview", H1 "2. Links and references", H1 "3. Lists", H2 "3.1 Conditions precedent (bull...", H2 "3.2 Drawdown steps (numbered)", H1 "4. Facilities", H2 "4.1 Chart", H2 "4.2 Facility table", H3 "4.2.1 Maturity notes", H1 "5. Fees"; PDF bookmarks (outline): 11 |
| 5 | Paragraphs tagged as P | PASS | 32 P elements |
| 6 | Figures have alt text; decorative images are artifacts | PASS | 1 Figure(s): alt="Bar chart of committed amount by facility: Term Loan A USD 2..."; images marked as artifacts (decorative, incl. any evaluation watermark): 3 [p1 920x500px, p1 1200x12px, p2 920x500px] |
| 7 | Links: inside a Link tag with an alternate description (/Contents) | PASS | 3 Link tags; https://www.example.com/accessibility [Link tag=true, Contents="Example Bank accessibility statement (opens w..."]; internal destination [Link tag=true, Contents="Go to section 5, Fees"]; ignored 1 Aspose evaluation-banner link(s) |
| 8 | Footnotes/endnotes: Note tags with unique IDs (+ Reference) | PASS | 2 Note(s), 0 Reference(s): Note id=Note0 page 1 "1 Actual/360 means interest accrues daily on ..."; Note id=Note1 page 2 "i Payment dates that fall on a non-business d..." |
| 9 | Bulleted and numbered lists: L > LI > Lbl + LBody | PASS | 3 L (3 items, ListNumbering=null; 2 items, ListNumbering=null; 3 items, ListNumbering=Decimal), 8 LI, 8 Lbl, 8 LBody |
| 10 | Tables: Table > TR > TH (with Scope) / TD | PASS | 1 Table, 4 TR, 7 TH (7 with Scope), 9 TD, THead=0, Caption=0; summary=none, alt="Each facility with its type, committed a..." |

<details><summary>Reading order (tag order, as a screen reader reads it)</summary>

1. P: Created with an evaluation copy of Aspose.Words. To remove all limitations, you can use Fr...  
2. H1: Term Loan Summary  
3. H1: 1. Overview  
4. P: This document tests every accessibility feature the document generation service needs. Eac...  
5. P: Borrowers can request this document in large print, braille or audio. Accessible formats a...  
6. H1: 2. Links and references  
7. P: Read our accessibility statement for more information.  
8. P: Fees are described in section 5, Fees.  
9. P: Interest is calculated on an actual/360 basis1and is payable monthly in arrearsi. 1 Actual...  
10. P: 1 Actual/360 means interest accrues daily on the outstanding balance, using a 360-day year...  
11. H1: 3. Lists  
12. H2: 3.1 Conditions precedent (bulleted)  
13. LBody: Signed facility agreement  
14. LBody: Evidence of insurance oProperty insuranceoBusiness interruption insurance  
15. LBody: Property insurance  
16. LBody: Business interruption insurance  
17. LBody: Board resolution  
18. H2: 3.2 Drawdown steps (numbered)  
19. LBody: Submit a drawdown request five business days in advance.  
20. LBody: The lender confirms the conditions are met.  
21. LBody: Funds are credited to the operating account.  
22. H1: 4. Facilities  
23. H2: 4.1 Chart  
24. Figure: [alt] Bar chart of committed amount by facility: Term Loan A USD 2.50 million, Revolving C...  
25. P: Figure 1. Committed amount by facility  
26. H2: 4.2 Facility table  
27. P: Table 1. Facilities  
28. TH: Facility  
29. P: Facility  
30. TH: Type  
31. P: Type  
32. TH: Amount (USD)  
33. P: Amount (USD)  
34. TH: Maturity  
35. P: Maturity  
36. TH: F-001  
37. P: F-001  
38. TD: Term Loan A  
39. P: Term Loan A  
40. TD: 2,500,000.00  
41. P: 2,500,000.00  
42. TD: 16 Nov 2031  
43. P: 16 Nov 2031  
44. TH: F-002  
45. P: F-002  
46. TD: Revolving Credit  
47. P: Revolving Credit  
48. TD: 750,000.00  
49. P: 750,000.00  
50. TD: 16 Nov 2029  
51. P: 16 Nov 2029  
52. TH: F-003  
53. P: F-003  
54. TD: Equipment Line  
55. P: Equipment Line  
56. TD: 400,000.00  
57. P: 400,000.00  
58. TD: 31 May 2030  
59. P: 31 May 2030  
60. H3: 4.2.1 Maturity notes  
61. P: Maturity dates are fixed at signing and can only be extended by written amendment.  
62. H1: 5. Fees  
63. P: An arrangement fee of 0.50% of the total commitment is payable on signing. A commitment fe...  
64. P: i Payment dates that fall on a non-business day move to the next business day.  

</details>

All structure types found: Document=1, Figure=1, H1=6, H2=4, H3=1, L=3, LBody=8, LI=8, Lbl=8, Link=3, Note=2, P=32, Span=11, TD=9, TH=7, TR=4, Table=1

## B. veraPDF PDF/UA-1: **FAIL** (2 rule(s)) - 5891 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.18.5-2 | 1 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |
| 7.18.1-2 | 1 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **PASS** - 5691 checks run, profile has 108 rules

