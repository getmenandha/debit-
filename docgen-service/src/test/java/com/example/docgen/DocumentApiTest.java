package com.example.docgen;

import com.example.docgen.service.TemplateRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "docgen.warm-up=false")
@AutoConfigureMockMvc
class DocumentApiTest {

    @Autowired MockMvc mvc;
    @Autowired TemplateRegistry registry;
    @Autowired ObjectMapper json;

    @Test
    void listsDocumentTypes() throws Exception {
        mvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].documentType").value(org.hamcrest.Matchers.hasItems(
                        "TERM_LOAN_AGREEMENT", "FACILITY_SCHEDULE", "LOAN_SUMMARY_ES", "TAXPAYER_FORM")))
                .andExpect(jsonPath("$[0].templateVersion").isString());
    }

    @Test
    void loanAgreementReturnsPdfBytesWithCorrectContent() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/documents/TERM_LOAN_AGREEMENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "test-1")
                        .content(registry.sample("TERM_LOAN_AGREEMENT")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Request-Id", "test-1"))
                .andExpect(header().exists("X-Document-Sha256"))
                .andReturn();
        byte[] pdf = r.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String text = text(pdf);
        assertThat(text).contains("TLA-2026-004512", "Northwind Logistics", "Total committed amount: 3,650,000.00");
        assertThat(text).doesNotContain("<<");
    }

    @Test
    void jsonEnvelopeCarriesBase64AndSignatureAnchors() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/documents/TERM_LOAN_AGREEMENT?format=json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registry.sample("TERM_LOAN_AGREEMENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.signatureAnchors").isNotEmpty())
                .andExpect(jsonPath("$.signatureAnchors[0].page").isNumber())
                .andReturn();
        JsonNode body = json.readTree(r.getResponse().getContentAsByteArray());
        byte[] pdf = Base64.getDecoder().decode(body.get("contentBase64").asText());
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void docxOutput() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/documents/FACILITY_SCHEDULE?format=docx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registry.sample("FACILITY_SCHEDULE")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getContentType()).contains("wordprocessingml");
        byte[] docx = r.getResponse().getContentAsByteArray();
        assertThat(new String(docx, 0, 2)).isEqualTo("PK");
    }

    @Test
    void spanishSummaryUsesSpanishFormatting() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/documents/LOAN_SUMMARY_ES")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registry.sample("LOAN_SUMMARY_ES")))
                .andExpect(status().isOk()).andReturn();
        assertThat(text(r.getResponse().getContentAsByteArray())).contains(" de ");
    }

    @Test
    void invalidPayloadReturns400WithEveryError() throws Exception {
        ObjectNode payload = (ObjectNode) json.readTree(registry.sample("TERM_LOAN_AGREEMENT"));
        ((ObjectNode) payload.get("agreement")).put("date", "16/11/2026");
        payload.remove("borrower");
        mvc.perform(post("/api/v1/documents/TERM_LOAN_AGREEMENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF)                 // error must still come back as JSON
                        .content(json.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.errors[*].path").value(org.hamcrest.Matchers.hasItems("/agreement/date", "/borrower")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/v1/documents/TERM_LOAN_AGREEMENT")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Payload validation failed"));
    }

    @Test
    void unknownTypeReturns404() throws Exception {
        mvc.perform(post("/api/v1/documents/NO_SUCH_DOC")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument d = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(d);
        }
    }
}
