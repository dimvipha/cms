package com.vipha.cms.security;

import com.vipha.cms.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private static final Set<String> IGNORED_ROLES = Set.of(
            "offline_access", "uma_authorization", "default-roles-cms");

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    };

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> request

                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // Staff (user management)
                        .requestMatchers("/api/v1/staff/**").hasAuthority(Roles.ADMIN)

                        // Departments
                        .requestMatchers(HttpMethod.GET,    "/api/v1/departments/**").hasAuthority("department:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/departments/**").hasAuthority("department:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/departments/**").hasAuthority("department:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasAuthority("department:delete")

                        // Doctors
                        .requestMatchers(HttpMethod.GET,    "/api/v1/doctors/**").hasAuthority("doctor:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/doctors/**").hasAuthority("doctor:write")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/doctors/**").hasAuthority("doctor:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/doctors/**").hasAuthority("doctor:delete")

                        // Patients
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


                        // Appointments (patient-owned)
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/**")
                        .hasAuthority("appointment:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/**").hasAuthority("appointment:write")

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

                        // Medicine catalogue (read-only lookup for prescribing)
                        .requestMatchers(HttpMethod.GET, "/api/v1/medicines/**").hasAuthority("medicine:read")

                        // Billing — payment rule must precede the general POST rule
                        .requestMatchers(HttpMethod.GET,  "/api/v1/invoices/**").hasAuthority("invoice:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/*/payments").hasAuthority("payment:record")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/**").hasAuthority("invoice:write")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/invoices/**").hasAuthority("invoice:write")

                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
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
                    .filter(role -> !IGNORED_ROLES.contains(role))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        };

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }
}