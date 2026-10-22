package com.vipha.cms.security;

import com.vipha.cms.feature.auth.AuthController;
import com.vipha.cms.feature.auth.AuthService;
import com.vipha.cms.feature.directory.ClinicDirectoryController;
import com.vipha.cms.feature.directory.ClinicDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What visitors who are not signed in may call: the landing page's directory and the sign-in options. */
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
    void visitorsCanAskWhetherGoogleIsOffered() throws Exception {
        mvc.perform(get("/api/v1/auth/google")).andExpect(status().isOk());
    }

    @Test
    void welcomingANewcomerNeedsTheirLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/welcome")).andExpect(status().isUnauthorized());
        // a newcomer's token holds no clinic role yet; being signed in is enough
        mvc.perform(post("/api/v1/auth/welcome").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void theRestOfTheApiStillNeedsALogin() throws Exception {
        mvc.perform(get("/api/v1/departments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/doctors")).andExpect(status().isUnauthorized());
    }
}
