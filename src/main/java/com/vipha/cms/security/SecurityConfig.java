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
                        .requestMatchers(HttpMethod.GET,    "/api/v1/departments/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/departments/**").hasAuthority(Roles.ADMIN)
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/departments/**").hasAuthority(Roles.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasAuthority(Roles.ADMIN)

                        // Doctors
                        .requestMatchers(HttpMethod.GET,    "/api/v1/doctors/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/v1/doctors/**").hasAuthority(Roles.ADMIN)
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/doctors/**").hasAuthority(Roles.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/doctors/**").hasAuthority(Roles.ADMIN)

                        // Patients
                        .requestMatchers(HttpMethod.GET,    "/api/v1/patients/**")
                        .hasAnyAuthority(Roles.ADMIN, Roles.DOCTOR, Roles.NURSE, Roles.RECEPTIONIST)
                        .requestMatchers(HttpMethod.POST,   "/api/v1/patients/**")
                        .hasAnyAuthority(Roles.ADMIN, Roles.NURSE, Roles.RECEPTIONIST)
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/patients/**")
                        .hasAnyAuthority(Roles.ADMIN, Roles.NURSE, Roles.RECEPTIONIST)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/patients/**")
                        .hasAuthority(Roles.ADMIN)

                        // Own profile
                        .requestMatchers("/api/v1/user-profiles/**").authenticated()

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