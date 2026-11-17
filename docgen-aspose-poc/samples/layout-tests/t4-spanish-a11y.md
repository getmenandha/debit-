# Accessibility report: t4-spanish.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | PASS | Info: "Resumen del préstamo a plazo"; XMP dc:title: "Resumen del préstamo a plazo"; DisplayDocTitle=true |
| 2 | Tagged PDF (MarkInfo + structure tree) | PASS | Marked=true, StructTreeRoot=true |
| 3 | Document language (+ foreign-language parts) | PASS | /Lang=es-ES; no other-language parts |
| 4 | Headings: tagged H1-H6, no skipped levels | PASS | 4 headings: H1 "1. Las partes", H1 "2. Líneas de crédito", H1 "3. Condiciones", H1 "4. Firmas"; PDF bookmarks (outline): 4 |
| 5 | Paragraphs tagged as P | PASS | 19 P elements |
| 6 | Figures have alt text; decorative images are artifacts | n/a | 0 Figure(s): ; images marked as artifacts (decorative, incl. any evaluation watermark): 1 [p1 920x500px] |
| 7 | Links: inside a Link tag with an alternate description (/Contents) | n/a | 1 Link tags; ; ignored 1 Aspose evaluation-banner link(s) |
| 8 | Footnotes/endnotes: Note tags with unique IDs (+ Reference) | n/a | 0 Note(s), 0 Reference(s):  |
| 9 | Bulleted and numbered lists: L > LI > Lbl + LBody | PASS | 1 L (3 items, ListNumbering=null), 3 LI, 3 Lbl, 3 LBody |
| 10 | Tables: Table > TR > TH (with Scope) / TD | PASS | 1 Table, 3 TR, 6 TH (6 with Scope), 6 TD, THead=0, Caption=0; summary=none, alt="Cada línea con su tipo, importe y fecha ..." |

<details><summary>Reading order (tag order, as a screen reader reads it)</summary>

1. P: Created with an evaluation copy of Aspose.Words. To remove all limitations, you can use Fr...  
2. P: Resumen del préstamo a plazo  
3. P: Contrato n.º TLA-2026-004512, firmado el 16 de noviembre de 2026.  
4. H1: 1. Las partes  
5. P: Este contrato se celebra entre Example Bank N.A. (el «Prestamista») y Logística Peñalara S...  
6. H1: 2. Líneas de crédito  
7. TH: Línea  
8. P: Línea  
9. TH: Tipo  
10. P: Tipo  
11. TH: Importe  
12. P: Importe  
13. TH: Vencimiento  
14. P: Vencimiento  
15. TH: F-001  
16. P: F-001  
17. TD: Préstamo a plazo A  
18. P: Préstamo a plazo A  
19. TD: USD 2.500.000,00  
20. P: USD 2.500.000,00  
21. TD: 16 de noviembre de 2031  
22. P: 16 de noviembre de 2031  
23. TH: F-002  
24. P: F-002  
25. TD: Línea de crédito renovable  
26. P: Línea de crédito renovable  
27. TD: USD 750.000,00  
28. P: USD 750.000,00  
29. TD: 16 de noviembre de 2029  
30. P: 16 de noviembre de 2029  
31. P: Importe total comprometido: 3.250.000,00 USD  
32. H1: 3. Condiciones  
33. LBody: El interés se calcula sobre el saldo pendiente, con base en un año de 360 días.  
34. LBody: ¿Tiene preguntas? ¡Llámenos! Atención al cliente en español: lunes a viernes.  
35. LBody: Pequeña muestra de caracteres: á é í ó ú ü ñ Á É Í Ó Ú Ü Ñ ¿ ¡ « » €.  
36. H1: 4. Firmas  
37. P: José Muñoz Ibáñez, Prestatario, director financiero  
38. P: Priya Raman, Prestamista, gestora de relaciones  

</details>

All structure types found: Document=1, H1=4, L=1, LBody=3, LI=3, Lbl=3, Link=1, P=19, Span=3, TD=6, TH=6, TR=3, Table=1

## B. veraPDF PDF/UA-1: **FAIL** (2 rule(s)) - 3261 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.18.5-2 | 1 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |
| 7.18.1-2 | 1 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](6 0 obj PDPage)/annots[0](19 0 obj PDLinkAnnot) |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **PASS** - 3157 checks run, profile has 108 rules

