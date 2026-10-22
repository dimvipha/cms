package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.auth.dto.GoogleSignInOptions;
import com.vipha.cms.feature.auth.dto.WelcomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the web app needs around sign-in, which itself happens on Keycloak's page (authorization
 * code flow with PKCE through the frontend client).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Whether sign-in with Google is set up, so the frontend can offer it. */
    @GetMapping("/google")
    public GoogleSignInOptions googleOptions() {
        return authService.googleOptions();
    }

    /** After signing in on Keycloak's page: makes someone new to the clinic a patient. */
    @PostMapping("/welcome")
    public WelcomeResponse welcome() {
        return authService.welcome();
    }
}
