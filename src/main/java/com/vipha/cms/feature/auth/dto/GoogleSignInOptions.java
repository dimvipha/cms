package com.vipha.cms.feature.auth.dto;

/**
 * Whether the realm offers sign-in with Google, and where the browser starts it: the frontend
 * sends the user to the authorization endpoint with this client id, PKCE and kc_idp_hint=google.
 */
public record GoogleSignInOptions(
        boolean enabled,
        String authorizationEndpoint,
        String clientId
) {}
