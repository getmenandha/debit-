# Accessibility report: untagged-control.pdf

## A. Tag inventory (PDFBox)

| # | Check | Result | Details |
|---|---|---|---|
| 1 | Title in metadata, shown in title bar | **FAIL** | Info: "Accessibility Test - Term Loan Summary"; XMP dc:title: "Accessibility Test - Term Loan Summary"; DisplayDocTitle=false |
| 2 | Tagged PDF (MarkInfo + structure tree) | **FAIL** | Marked=false, StructTreeRoot=false |

No structure tree, so nothing else can be checked.
## B. veraPDF PDF/UA-1: **FAIL** (10 rule(s)) - 949 checks run, profile has 106 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.1-10 | 1 | The document catalog dictionary shall include a ViewerPreferences dictionary containing a DisplayDocTitle key, whose value shall be true | root |
| 6.2-1 | 1 | The document catalog dictionary shall include a MarkInfo dictionary containing an entry, Marked, whose value shall be true | root |
| 7.1-11 | 1 | The logical structure of the conforming file shall be described by a structure hierarchy rooted in the StructTreeRoot entry of the document catalog dictionary, as described in ISO 32000-1:2008, 14.7 | root/document[0] |
| 7.18.3-1 | 1 | Every page on which there is an annotation shall contain in its page dictionary the key Tabs, and its value shall be S | root/document[0]/pages[0](5 0 obj PDPage) |
| 7.1-3 | 100 | Content shall be marked as Artifact or tagged as real content | root/document[0]/pages[0](5 0 obj PDPage)/contentStream[0](6 0 obj PDSemanticContentStream... |
| 7.21.8-1 | 3 | A PDF/UA compliant document shall not contain a reference to the .notdef glyph from any of the text showing operators, regardless of text rendering mode, in any content stream | root/document[0]/pages[0](5 0 obj PDPage)/contentStream[0](6 0 obj PDSemanticContentStream... |
| 7.18.5-2 | 2 | Links shall contain an alternate description via their Contents key as described in ISO 32000-1:2008, 14.9.3 | root/document[0]/pages[0](5 0 obj PDPage)/annots[0](13 0 obj PDLinkAnnot) |
| 7.18.5-1 | 4 | Links shall be tagged according to ISO 32000-1:2008, 14.8.4.4.2, Link Element | root/document[0]/pages[0](5 0 obj PDPage)/annots[0](13 0 obj PDLinkAnnot) |
| 7.18.1-2 | 2 | An annotation (except Widget annotations or hidden annotations, or those having rectangle outside the crop-box) shall have either Contents key or an Alt entry in the enclosing structure element | root/document[0]/pages[0](5 0 obj PDPage)/annots[0](13 0 obj PDLinkAnnot) |
| 5-1 | 1 | The PDF/UA version and conformance level of a file shall be specified using the PDF/UA Identification extension schema | root/document[0]/metadata[0](4 0 obj PDMetadata)/XMPPackage[0] |

## B. veraPDF WCAG 2.2 (machine-checkable rules): **FAIL** (7 rule(s)) - 944 checks run, profile has 108 rules

| Rule | Failures | Description | Example location |
|---|---|---|---|
| 7.1-10 | 1 | The document catalog dictionary shall include a ViewerPreferences dictionary containing a DisplayDocTitle key, whose value shall be true | root |
| 6.2-1 | 1 | The document catalog dictionary shall include a MarkInfo dictionary containing an entry, Marked, whose value shall be true | root |
| 7.1-11 | 1 | The logical structure of the conforming file shall be described by a structure hierarchy rooted in the StructTreeRoot entry of the document catalog dictionary, as described in ISO 32000-1:2008, 14.7 | root/document[0] |
| 7.18.3-1 | 1 | Every page on which there is an annotation shall contain in its page dictionary the key Tabs, and its value shall be S | root/document[0]/pages[0](5 0 obj PDPage) |
| 7.1-3 | 100 | Content shall be marked as Artifact or tagged as real content | root/document[0]/pages[0](5 0 obj PDPage)/contentStream[0](6 0 obj PDSemanticContentStream... |
| 7.21.8-1 | 3 | A PDF/UA compliant document shall not contain a reference to the .notdef glyph from any of the text showing operators, regardless of text rendering mode, in any content stream | root/document[0]/pages[0](5 0 obj PDPage)/contentStream[0](6 0 obj PDSemanticContentStream... |
| 7.18.5-1 | 4 | Links shall be tagged according to ISO 32000-1:2008, 14.8.4.4.2, Link Element | root/document[0]/pages[0](5 0 obj PDPage)/annots[0](13 0 obj PDLinkAnnot) |

