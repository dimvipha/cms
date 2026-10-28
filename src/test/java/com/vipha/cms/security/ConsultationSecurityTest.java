package com.vipha.cms.security;

import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.feature.pharmacy.MedicineController;
import com.vipha.cms.feature.pharmacy.MedicineService;
import com.vipha.cms.feature.prescription.PrescriptionController;
import com.vipha.cms.feature.prescription.PrescriptionService;
import com.vipha.cms.feature.visit.VisitController;
import com.vipha.cms.feature.visit.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** URL authorization rules for the consultation endpoints, run against the real SecurityConfig. */
@WebMvcTest(controllers = {VisitController.class, PrescriptionController.class, MedicineController.class})
@Import(SecurityConfig.class)
class ConsultationSecurityTest {

    private static final String DOCTOR = "visit:read visit:write vitals:write prescription:read prescription:write medicine:read";
    private static final String NURSE = "visit:read visit:open vitals:write prescription:read medicine:read";
    private static final String PHARMACIST = "prescription:read medicine:read";
    private static final String RECEPTIONIST = "patient:read-all appointment:read appointment:write visit:open";
    private static final String LAB_TECHNICIAN = "patient:read-all visit:read";

    private static final String VITALS = "{\"systolicBp\":120,\"diastolicBp\":80}";
    private static final String PRESCRIPTION = """
            {"visitId":"%s","items":[{"medicineId":"%s","dosage":"500mg","frequency":"TID",
             "duration":"5 days","quantity":15}]}""".formatted(UUID.randomUUID(), UUID.randomUUID());

    @Autowired MockMvc mvc;

    @MockitoBean VisitService visitService;
    @MockitoBean PrescriptionService prescriptionService;
    @MockitoBean MedicineService medicineService;
    @MockitoBean JwtDecoder jwtDecoder;

    private final UUID id = UUID.randomUUID();

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/visits/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void nurseCanOpenAVisitAndRecordVitalsButNotRunTheConsultation() throws Exception {
        mvc.perform(post("/api/v1/visits").with(as(NURSE)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/visits/{id}/vitals", id).with(as(NURSE)).contentType(MediaType.APPLICATION_JSON).content(VITALS))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/visits/{id}", id).with(as(NURSE)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/visits/{id}/start", id).with(as(NURSE)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/visits/{id}/cancel", id).with(as(NURSE)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/visits/{id}", id).with(as(NURSE)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/visits/{id}/complete", id).with(as(NURSE)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/prescriptions").with(as(NURSE)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCanRunTheWholeConsultation() throws Exception {
        mvc.perform(post("/api/v1/visits").with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/visits/{id}/start", id).with(as(DOCTOR)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/visits/{id}", id).with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisCode\":\"J06.9\",\"diagnosis\":\"URTI\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/prescriptions").with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/prescriptions/{id}", id).with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/medicines").param("search", "para").with(as(DOCTOR)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/visits/{id}/complete", id).with(as(DOCTOR)))
                .andExpect(status().isOk());
    }

    @Test
    void pharmacistCanReadAndPrintButNotPrescribe() throws Exception {
        when(prescriptionService.renderPdf(id)).thenReturn(new PdfFile("prescription-MRN-1-2026-10-07.pdf", new byte[]{'%', 'P'}));

        mvc.perform(get("/api/v1/prescriptions/{id}/pdf", id).with(as(PHARMACIST)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"prescription-MRN-1-2026-10-07.pdf\""));

        mvc.perform(post("/api/v1/prescriptions").with(as(PHARMACIST)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/visits/{id}", id).with(as(PHARMACIST)))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionistRegistersWalkInsButSeesNoClinicalData() throws Exception {
        mvc.perform(post("/api/v1/visits").with(as(RECEPTIONIST)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/visits/{id}/start", id).with(as(RECEPTIONIST))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/visits/{id}/vitals", id).with(as(RECEPTIONIST)).contentType(MediaType.APPLICATION_JSON).content(VITALS))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/visits").with(as(RECEPTIONIST))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/prescriptions/{id}", id).with(as(RECEPTIONIST))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/medicines").with(as(RECEPTIONIST))).andExpect(status().isForbidden());
    }

    @Test
    void labTechnicianCannotOpenVisits() throws Exception {
        mvc.perform(post("/api/v1/visits").with(as(LAB_TECHNICIAN)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownershipDenialFromTheServiceIsA403() throws Exception {
        when(visitService.update(any(), any())).thenThrow(new AccessDeniedException("Only the visit's doctor can change this visit"));

        mvc.perform(patch("/api/v1/visits/{id}", id).with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidIcdCodeIsRejected() throws Exception {
        mvc.perform(patch("/api/v1/visits/{id}", id).with(as(DOCTOR)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisCode\":\"not-a-code\"}"))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor as(String authorities) {
        return jwt().authorities(Arrays.stream(authorities.split(" "))
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new));
    }
}
