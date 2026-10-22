package com.vipha.cms.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Where the web frontend lives: browsers on {@code allowedOrigins} may call the
 * API, and account emails (invites, password resets) send people back to
 * {@code loginUrl}, which must be a valid redirect URI of Keycloak client {@code clientId}.
 */
@ConfigurationProperties(prefix = "cms.frontend")
public record FrontendProperties(
        @DefaultValue("http://localhost:5173/login") String loginUrl,
        @DefaultValue("cms-app") String clientId,
        @DefaultValue({"http://localhost:5173", "http://localhost:3000"}) List<String> allowedOrigins
) {}
