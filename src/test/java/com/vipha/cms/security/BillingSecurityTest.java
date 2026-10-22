package com.vipha.cms.security;

import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.feature.billing.InvoiceController;
import com.vipha.cms.feature.billing.InvoiceService;
import com.vipha.cms.feature.billing.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** URL authorization rules for billing, run against the real SecurityConfig. */
@WebMvcTest(controllers = InvoiceController.class)
@Import(SecurityConfig.class)
class BillingSecurityTest {

    private static final String RECEPTIONIST = "invoice:read invoice:write payment:record patient:read-all";
    private static final String PHARMACIST = "invoice:read payment:record prescription:read medicine:read";
    private static final String DOCTOR = "visit:read visit:write prescription:read prescription:write";

    private static final String PAYMENT = "{\"amount\":250.00,\"method\":\"CASH\"}";
    private static final String DRAFT = """
            {"tax":0,"discount":0,"items":[{"itemType":"CONSULTATION","description":"Consultation fee",
             "quantity":1,"unitPrice":250.00}]}""";

    @Autowired MockMvc mvc;

    @MockitoBean InvoiceService invoiceService;
    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtDecoder jwtDecoder;

    private final UUID id = UUID.randomUUID();

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/invoices/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void receptionistRunsTheWholeBillingFlow() throws Exception {
        when(invoiceService.renderPdf(id)).thenReturn(new PdfFile("receipt-INV-1.pdf", new byte[]{'%', 'P'}));

        mvc.perform(get("/api/v1/invoices/billable-visits").with(as(RECEPTIONIST))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/invoices/from-visit/{visitId}", id).with(as(RECEPTIONIST))).andExpect(status().isCreated());
        mvc.perform(put("/api/v1/invoices/{id}", id).with(as(RECEPTIONIST)).contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/invoices/{id}/issue", id).with(as(RECEPTIONIST))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/invoices/{id}/payments", id).with(as(RECEPTIONIST)).contentType(MediaType.APPLICATION_JSON).content(PAYMENT))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/invoices/{id}/pdf", id).with(as(RECEPTIONIST)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(post("/api/v1/invoices/{id}/void", id).with(as(RECEPTIONIST))).andExpect(status().isOk());
    }

    @Test
    void pharmacistCanTakePaymentButNotChangeInvoices() throws Exception {
        mvc.perform(get("/api/v1/invoices").with(as(PHARMACIST))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/invoices/{id}/payments", id).with(as(PHARMACIST)).contentType(MediaType.APPLICATION_JSON).content(PAYMENT))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/invoices/from-visit/{visitId}", id).with(as(PHARMACIST))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/invoices/{id}", id).with(as(PHARMACIST)).contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invoices/{id}/issue", id).with(as(PHARMACIST))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invoices/{id}/void", id).with(as(PHARMACIST))).andExpect(status().isForbidden());
    }

    @Test
    void doctorsHaveNoBillingAccess() throws Exception {
        mvc.perform(get("/api/v1/invoices").with(as(DOCTOR))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invoices/from-visit/{visitId}", id).with(as(DOCTOR))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invoices/{id}/payments", id).with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON).content(PAYMENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPaymentAmountsAreRejected() throws Exception {
        for (String amount : new String[]{"0", "-5", "10.005"}) {
            mvc.perform(post("/api/v1/invoices/{id}/payments", id).with(as(RECEPTIONIST)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":" + amount + ",\"method\":\"CASH\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    private static RequestPostProcessor as(String authorities) {
        return jwt().authorities(Arrays.stream(authorities.split(" "))
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new));
    }
}
