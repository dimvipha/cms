package com.vipha.cms.security;

import com.vipha.cms.feature.auth.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class KeycloakAuthClient {

    private final RestClient restClient;
    private final String tokenEndpoint;
    private final String logoutEndpoint;
    private final String authorizationEndpoint;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAuthClient(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            // the address browsers reach Keycloak on, when it differs from the one the API uses
            @Value("${keycloak.public-url:${keycloak.server-url}}") String publicUrl
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        String base = serverUrl + "/realms/" + realm + "/protocol/openid-connect";
        this.tokenEndpoint = base + "/token";
        this.logoutEndpoint = base + "/logout";
        this.authorizationEndpoint = publicUrl + "/realms/" + realm + "/protocol/openid-connect/auth";
        this.restClient = RestClient.create();
    }

    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);
        return exchangeToken(form, "Invalid username or password");
    }

    /**
     * Finishes a browser sign-in (authorization code flow with PKCE): the code Keycloak sent to
     * the frontend's redirect URI, exchanged with this client's secret.
     */
    public TokenResponse exchangeCode(String code, String codeVerifier, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        return exchangeToken(form, "Sign-in did not finish. Please try again.");
    }

    /** Where a browser starts a sign-in with this client. */
    public String authorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String clientId() {
        return clientId;
    }

    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        return exchangeToken(form, "Invalid or expired refresh token");
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        try {
            restClient.post()
                    .uri(logoutEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak logout returned {}", ex.getStatusCode());
        }
    }

    private TokenResponse exchangeToken(MultiValueMap<String, String> form, String failureMessage) {
        try {
            return restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.BAD_REQUEST.value()) {
                throw new BadCredentialsException(failureMessage);
            }
            log.error("Keycloak token exchange failed: {} - {}", status, ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Authentication service unavailable");
        }
    }
}
