package com.vipha.cms.security;

import com.vipha.cms.common.FrontendProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(FrontendProperties.class)
public class SecurityConfig {

    private static final Set<String> IGNORED_ROLES = Set.of("offline_access", "uma_authorization");

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/google",
            // what the landing page shows visitors: departments and the doctors taking bookings
            "/api/v1/public/**"
    };

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(request -> request

                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Staff (user management)
                        .requestMatchers("/api/v1/staff/**").hasAuthority(Roles.ADMIN)

                        // Departments
                        .requestMatchers(HttpMethod.GET,    "/api/v1/departments/**").hasAuthority("department:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/departments/**").hasAuthority("department:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/departments/**").hasAuthority("department:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasAuthority("department:delete")

                        // Doctors — doctor:update covers your own schedule; department, specialties
                        // and activation are managed with doctor:write
                        .requestMatchers(HttpMethod.GET,    "/api/v1/doctors/**").hasAuthority("doctor:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/doctors/**").hasAuthority("doctor:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/doctors/*/schedule")
                        .hasAnyAuthority("doctor:update", "doctor:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/doctors/**").hasAuthority("doctor:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/doctors/**").hasAuthority("doctor:delete")

                        // Patients — permissions without -all reach only your own record (checked in the service)
                        .requestMatchers(HttpMethod.GET,    "/api/v1/patients").hasAuthority("patient:read-all")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/patients/**")
                        .hasAnyAuthority("patient:read", "patient:read-all")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/patients/**")
                        .hasAnyAuthority("patient:write", "patient:write-all")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/patients/**")
                        .hasAnyAuthority("patient:update", "patient:update-all")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/patients/**")
                        .hasAuthority(Roles.ADMIN)

                        // Own profile
                        .requestMatchers(HttpMethod.GET, "/api/v1/user-profiles/all").hasAuthority(Roles.ADMIN)
                        .requestMatchers("/api/v1/user-profiles/**").authenticated()

                        // Appointments — without -all, patients and doctors reach only their own;
                        // the front-desk rules must precede the general POST rule
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/**")
                        .hasAnyAuthority("appointment:read", "appointment:read-all")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/check-in", "/api/v1/appointments/*/no-show")
                        .hasAuthority("appointment:write-all")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/**")
                        .hasAnyAuthority("appointment:write", "appointment:write-all")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/**")
                        .hasAnyAuthority("appointment:write", "appointment:write-all")

                        // Visits (consultations) — vitals rule must precede the general POST rule
                        .requestMatchers(HttpMethod.GET,   "/api/v1/visits/**").hasAuthority("visit:read")
                        .requestMatchers(HttpMethod.POST,  "/api/v1/visits/*/vitals").hasAuthority("vitals:write")
                        .requestMatchers(HttpMethod.POST,  "/api/v1/visits/**").hasAuthority("visit:write")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/visits/**").hasAuthority("visit:write")

                        // Prescriptions
                        .requestMatchers(HttpMethod.GET,    "/api/v1/prescriptions/**").hasAuthority("prescription:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/prescriptions/**").hasAuthority("prescription:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/prescriptions/**").hasAuthority("prescription:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/prescriptions/**").hasAuthority("prescription:write")

                        // Medicine catalogue and stock
                        .requestMatchers(HttpMethod.GET,  "/api/v1/medicines/**").hasAuthority("medicine:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/medicines/**").hasAuthority("medicine:write")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/medicines/**").hasAuthority("medicine:write")

                        // Billing — payment rule must precede the general POST rule
                        .requestMatchers(HttpMethod.GET,  "/api/v1/invoices/**").hasAuthority("invoice:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/*/payments").hasAuthority("payment:record")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/**").hasAuthority("invoice:write")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/invoices/**").hasAuthority("invoice:write")

                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JsonSecurityErrorHandler jsonSecurityErrorHandler() {
        return new JsonSecurityErrorHandler();
    }

    /** Lets the browser frontend call the API from its own origin. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(FrontendProperties frontend) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(frontend.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> converter = jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) {
                return List.of();
            }
            Object roles = realmAccess.get("roles");
            if (!(roles instanceof Collection<?> collection)) {
                return List.of();
            }
            return collection.stream()
                    .map(String::valueOf)
                    .filter(role -> !IGNORED_ROLES.contains(role) && !role.startsWith("default-roles-"))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        };

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }
}
