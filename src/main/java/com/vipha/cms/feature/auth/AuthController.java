package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.auth.dto.ChangePasswordRequest;
import com.vipha.cms.feature.auth.dto.ForgotPasswordRequest;
import com.vipha.cms.feature.auth.dto.GoogleSignInOptions;
import com.vipha.cms.feature.auth.dto.GoogleSignInRequest;
import com.vipha.cms.feature.auth.dto.LoginRequest;
import com.vipha.cms.feature.auth.dto.LogoutRequest;
import com.vipha.cms.feature.auth.dto.RefreshRequest;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

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

    /** Whether sign-in with Google is set up, and where the browser starts it. */
    @GetMapping("/google")
    public GoogleSignInOptions googleOptions() {
        return authService.googleOptions();
    }

    /** Finishes sign-in with Google: the code Keycloak sent back to the frontend, for the app's tokens. */
    @PostMapping("/google")
    public TokenResponse signInWithGoogle(@Valid @RequestBody GoogleSignInRequest request) {
        return authService.signInWithGoogle(request);
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
