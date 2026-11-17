# Accessibility report: t2-overflow-long-name.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | PASS | Info: "Text overflow test (long)"; XMP dc:title: "Text overflow test (long)"; DisplayDocTitle=true |
| 2 | Tagged PDF (MarkInfo + structure tree) | PASS | Marked=true, StructTreeRoot=true |
| 3 | Document language (+ foreign-language parts) | PASS | /Lang=en-US; no other-language parts |
| 4 | Headings: tagged H1-H6, no skipped levels | PASS | 9 headings: H1 "A. Flowing paragraph", H1 "B. Table cell, automatic row h...", H1 "C. Table cell, exact row heigh...", H1 "D. Table cell with Word 'Fit t...", H1 "E. Fixed-size text box (200 x ...", H1 "F. Text box set to 'Resize sha...", H1 "G. Template rule: smaller font...", H1 "H. Template rule: truncate wit...", H1 "I. Long free-text field (notes..."; PDF bookmarks (outline): 9 |
| 5 | Paragraphs tagged as P | PASS | 31 P elements |
| 6 | Figures have alt text; decorative images are artifacts | n/a | 0 Figure(s): ; images marked as artifacts (decorative, incl. any evaluation watermark): 2 [p1 920x500px, p2 920x500px] |
| 7 | Links: inside a Link tag with an alternate description (/Contents) | n/a | 1 Link tags; ; ignored 1 Aspose evaluation-banner link(s) |
| 8 | Footnotes/endnotes: Note tags with unique IDs (+ Reference) | n/a | 0 Note(s), 0 Reference(s):  |
| 9 | Bulleted and numbered lists: L > LI > Lbl + LBody | n/a | 0 L (), 0 LI, 0 Lbl, 0 LBody |
| 10 | Tables: Table > TR > TH (with Scope) / TD | PASS | 4 Table, 6 TR, 10 TH (10 with Scope), 2 TD, THead=0, Caption=0; summary=none, alt=none; summary=none, alt=none; summary=none, alt=none; summary=none, alt=none |

<details><summary>Reading order (tag order, as a screen reader reads it)</summary>

1. P: Created with an evaluation copy of Aspose.Words. To remove all limitations, you can use Fr...  
2. P: Text overflow test  
3. P: Each block shows what happens when a value is longer than the space designed for it. Borro...  
4. H1: A. Flowing paragraph  
5. P: Expected: the text wraps and everything after it moves down.  
6. P: The borrower, Northwind Logistics and Cold Chain Distribution Services of the Greater Dela...  
7. H1: B. Table cell, automatic row height  
8. P: Expected: the cell wraps onto more lines and the row gets taller.  
9. TH: Borrower name  
10. P: Borrower name  
11. TH: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
12. P: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
13. TH: Next row  
14. P: Next row  
15. TD: This row is pushed down.  
16. P: This row is pushed down.  
17. H1: C. Table cell, exact row height (0.25 in)  
18. P: Expected: the row height is fixed, so text beyond the first line is cut off.  
19. TH: Borrower name  
20. P: Borrower name  
21. TH: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
22. P: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
23. TH: Next row  
24. P: Next row  
25. TD: This row does not move.  
26. P: This row does not move.  
27. H1: D. Table cell with Word 'Fit text'  
28. P: Expected: the text is squeezed horizontally to stay on one line.  
29. TH: Borrower name  
30. P: Borrower name  
31. TH: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
32. P: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
33. H1: E. Fixed-size text box (200 x 22 pt)  
34. P: Expected: the box keeps its size, so overflowing text is hidden.  
35. P: Paragraph after the fixed text box.  
36. H1: F. Text box set to 'Resize shape to fit text'  
37. P: Expected: the box grows taller to fit the text.  
38. P: Paragraph after the auto-grow text box.  
39. H1: G. Template rule: smaller font when the value is long  
40. P: Rule in the template: if the name is longer than 40 characters, print it at 7 pt, otherwis...  
41. TH: Borrower name  
42. P: Borrower name  
43. TH: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
44. P: Northwind Logistics and Cold Chain Distribution Services of the Greater Delaware Valley, L...  
45. H1: H. Template rule: truncate with an ellipsis  
46. P: Rule in the template: cut at 37 characters and add '...'. Not suitable for legal names.  
47. P: Northwind Logistics and Cold Chain Di...  
48. H1: I. Long free-text field (notes)  
49. P: Expected: a long note flows across lines and pages like normal text.  
50. P: The borrower shall deliver quarterly management accounts within 45 days of each quarter en...  

</details>

All structure types found: Document=1, H1=9, Link=1, P=31, TD=2, TH=10, TR=6, Table=4

## B. veraPDF PDF/UA-1: **FAIL** (2 rule(s)) - 5882 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.18.5-2 | 1 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |
| 7.18.1-2 | 1 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **PASS** - 5743 checks run, profile has 108 rules

