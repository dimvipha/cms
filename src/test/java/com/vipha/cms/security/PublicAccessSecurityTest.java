package com.vipha.cms.security;

import com.vipha.cms.feature.auth.AuthController;
import com.vipha.cms.feature.auth.AuthService;
import com.vipha.cms.feature.directory.ClinicDirectoryController;
import com.vipha.cms.feature.directory.ClinicDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What visitors who are not signed in may call: the landing page's directory and Google sign-in. */
@WebMvcTest(controllers = {AuthController.class, ClinicDirectoryController.class})
@Import(SecurityConfig.class)
class PublicAccessSecurityTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthService authService;
    @MockitoBean ClinicDirectoryService directoryService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void visitorsCanReadTheClinicDirectory() throws Exception {
        mvc.perform(get("/api/v1/public/clinic")).andExpect(status().isOk());
    }

    @Test
    void visitorsCanStartAndFinishSignInWithGoogle() throws Exception {
        mvc.perform(get("/api/v1/auth/google")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"c\",\"codeVerifier\":\"v\",\"redirectUri\":\"http://localhost:5173/auth/callback\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void theRestOfTheApiStillNeedsALogin() throws Exception {
        mvc.perform(get("/api/v1/departments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/doctors")).andExpect(status().isUnauthorized());
    }
}
