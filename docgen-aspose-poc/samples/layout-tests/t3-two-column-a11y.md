# Accessibility report: t3-two-column.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | PASS | Info: "Two-column layout test"; XMP dc:title: "Two-column layout test"; DisplayDocTitle=true |
| 2 | Tagged PDF (MarkInfo + structure tree) | PASS | Marked=true, StructTreeRoot=true |
| 3 | Document language (+ foreign-language parts) | PASS | /Lang=en-US; no other-language parts |
| 4 | Headings: tagged H1-H6, no skipped levels | PASS | 3 headings: H1 "Part A. Terms and conditions", H1 "Part B. Taxpayer information (...", H2 "Part I. Taxpayer Identificatio..."; PDF bookmarks (outline): 3 |
| 5 | Paragraphs tagged as P | PASS | 34 P elements |
| 6 | Figures have alt text; decorative images are artifacts | n/a | 0 Figure(s): ; images marked as artifacts (decorative, incl. any evaluation watermark): 1 [p1 920x500px] |
| 7 | Links: inside a Link tag with an alternate description (/Contents) | n/a | 1 Link tags; ; ignored 1 Aspose evaluation-banner link(s) |
| 8 | Footnotes/endnotes: Note tags with unique IDs (+ Reference) | n/a | 0 Note(s), 0 Reference(s):  |
| 9 | Bulleted and numbered lists: L > LI > Lbl + LBody | n/a | 0 L (), 0 LI, 0 Lbl, 0 LBody |
| 10 | Tables: Table > TR > TH (with Scope) / TD | CHECK (no header cells: fine for layout tables, not for data tables) | 2 Table, 7 TR, 0 TH (0 with Scope), 10 TD, THead=0, Caption=0; summary=none, alt="Taxpayer information form, fields 1 to 7"; summary=none, alt="Taxpayer identification number" |

<details><summary>Reading order (tag order, as a screen reader reads it)</summary>

1. P: Created with an evaluation copy of Aspose.Words. To remove all limitations, you can use Fr...  
2. P: Two-column layout test  
3. P: Part A uses newspaper columns: text flows down the left column, then continues in the righ...  
4. H1: Part A. Terms and conditions  
5. P: Interest: Interest accrues daily on the outstanding principal at the agreed rate and is pa...  
6. P: Prepayment: The borrower may prepay all or part of the loan on any business day with five ...  
7. P: Fees: An arrangement fee is payable on signing and a commitment fee accrues on undrawn amo...  
8. P: Covenants: The borrower will maintain a debt service coverage ratio of at least 1.25x, tes...  
9. P: Events of default: Non-payment, breach of covenant, insolvency and material adverse change...  
10. P: Governing law: This agreement is governed by the laws of the State of New York.  
11. P: Notices: Notices must be in writing and delivered to the addresses set out in the schedule...  
12. P: Assignment: The borrower may not assign its rights without the lender's prior written cons...  
13. H1: Part B. Taxpayer information (side-by-side form)  
14. TD: 1 Name (as shown on your income tax return)Jane Alvarez  
15. P: 1 Name (as shown on your income tax return)  
16. P: Jane Alvarez  
17. TD: 2 Business name, if different from aboveNorthwind Logistics LLC  
18. P: 2 Business name, if different from above  
19. P: Northwind Logistics LLC  
20. TD: 3 Tax classification (check one)☐ Individual ☐ C corporation☐ S corporation ☒ Limited liab...  
21. P: 3 Tax classification (check one)  
22. P: ☐ Individual ☐ C corporation  
23. P: ☐ S corporation ☒ Limited liability company  
24. TD: 4 ExemptionsExempt payee code: 5Reporting exemption code: -  
25. P: 4 Exemptions  
26. P: Exempt payee code: 5  
27. P: Reporting exemption code: -  
28. TD: 5 Address (number, street, and apt. or suite no.)200 Harbor Way, Suite 400  
29. P: 5 Address (number, street, and apt. or suite no.)  
30. P: 200 Harbor Way, Suite 400  
31. TD: Requester's name and addressExample Bank N.A.1 Example Plaza, New York, NY 10001  
32. P: Requester's name and address  
33. P: Example Bank N.A.  
34. P: 1 Example Plaza, New York, NY 10001  
35. TD: 6 City, state, and ZIP codeWilmington, DE 19801  
36. P: 6 City, state, and ZIP code  
37. P: Wilmington, DE 19801  
38. TD: 7 Account number(s)CUST-778812  
39. P: 7 Account number(s)  
40. P: CUST-778812  
41. H2: Part I. Taxpayer Identification Number (TIN)  
42. TD: Enter your TIN in the appropriate box. The TIN must match the name given on line 1. For in...  
43. P: Enter your TIN in the appropriate box. The TIN must match the name given on line 1. For in...  
44. TD: Employer identification number12-3456789  
45. P: Employer identification number  
46. P: 12-3456789  
47. P: Certification: under penalties of perjury, I certify that the information on this form is ...  

</details>

All structure types found: Document=1, H1=2, H2=1, Link=1, P=34, TD=10, TR=7, Table=2

## B. veraPDF PDF/UA-1: **FAIL** (3 rule(s)) - 5034 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.1-3 | 1 | Content shall be marked as Artifact or tagged as real content | root/document[0]/pages[0](6 0 obj PDPage)/contentStream[0](226 0 obj PDSemanticContentStre... |
| 7.18.5-2 | 1 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |
| 7.18.1-2 | 1 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **FAIL** (1 rule(s)) - 4924 checks run, profile has 108 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.1-3 | 1 | Content shall be marked as Artifact or tagged as real content | root/document[0]/pages[0](6 0 obj PDPage)/contentStream[0](226 0 obj PDSemanticContentStre... |

