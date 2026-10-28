package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.auth.dto.ChangePasswordRequest;
import com.vipha.cms.feature.auth.dto.ForgotPasswordRequest;
import com.vipha.cms.feature.auth.dto.LoginRequest;
import com.vipha.cms.feature.auth.dto.LogoutRequest;
import com.vipha.cms.feature.auth.dto.RefreshRequest;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing in and up with a password through the API (Keycloak's password grant), and the account
 * calls around it. The web app's own sign-in, sign-up, forgot-password and change-password forms
 * use these, as do API clients such as the Postman collection. Two-factor accounts send their
 * authenticator code with the password. OAuth's security advice (RFC 9700) prefers signing in on
 * Keycloak's page; cms.auth.password-endpoints=false turns these off for such a deployment.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cms.auth.password-endpoints", havingValue = "true", matchIfMissing = true)
public class PasswordAuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    /** Always 202, whether or not the email has an account. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }
}
