package com.vipha.cms.feature.auth;

import com.vipha.cms.common.FrontendProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.auth.dto.GoogleSignInRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakAuthClient;
import com.vipha.cms.security.KeycloakUserService;
import com.vipha.cms.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleSignInTest {

    private static final String USER_ID = "google-user-1";
    private static final GoogleSignInRequest REQUEST =
            new GoogleSignInRequest("code-1", "verifier-1", "http://localhost:5173/auth/callback");

    @Mock KeycloakUserService keycloakUserService;
    @Mock KeycloakAuthClient keycloakAuthClient;
    @Mock UserProfileRepository userProfileRepository;
    @Mock JwtDecoder jwtDecoder;

    AuthServiceImpl service;
    final TokenResponse first = tokens("first");

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(keycloakUserService, keycloakAuthClient, userProfileRepository,
                new FrontendProperties("http://localhost:5173/login", "cms-app", List.of("http://localhost:5173")),
                jwtDecoder);
    }

    @Test
    void someoneNewBecomesAPatient_andGetsTokensCarryingTheRole() {
        signsIn(Set.of("default-roles-clinic", "offline_access"));
        TokenResponse refreshed = tokens("with-roles");
        when(keycloakAuthClient.refresh(first.refreshToken())).thenReturn(refreshed);

        assertThat(service.signInWithGoogle(REQUEST)).isEqualTo(refreshed);
        verify(keycloakUserService).assignRealmRoles(USER_ID, Roles.PATIENT);
        verify(keycloakUserService).joinGroup(USER_ID, "patient-management");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void someoneWithAnAccountKeepsIt_whetherPatientOrStaff() {
        for (String role : List.of(Roles.PATIENT, Roles.DOCTOR, Roles.ADMIN)) {
            signsIn(Set.of(role, "default-roles-clinic"));

            assertThat(service.signInWithGoogle(REQUEST)).as(role).isEqualTo(first);
        }
        verify(keycloakUserService, never()).assignRealmRoles(anyString(), any(String[].class));
        verify(keycloakAuthClient, never()).refresh(anyString());
    }

    @Test
    void theBrowserMustComeBackToTheClinicsOwnCallback() {
        GoogleSignInRequest elsewhere = new GoogleSignInRequest("code-1", "verifier-1", "https://evil.example/auth/callback");

        assertThatThrownBy(() -> service.signInWithGoogle(elsewhere)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(keycloakAuthClient);
    }

    @Test
    void optionsSayWhetherTheRealmOffersGoogle() {
        when(keycloakUserService.hasIdentityProvider("google")).thenReturn(false);
        when(keycloakAuthClient.authorizationEndpoint()).thenReturn("http://localhost:1201/realms/clinic/protocol/openid-connect/auth");
        when(keycloakAuthClient.clientId()).thenReturn("private-app");

        var options = service.googleOptions();

        assertThat(options.enabled()).isFalse();
        assertThat(options.clientId()).isEqualTo("private-app");
    }

    private void signsIn(Set<String> roles) {
        when(keycloakAuthClient.exchangeCode("code-1", "verifier-1", REQUEST.redirectUri())).thenReturn(first);
        when(jwtDecoder.decode(first.accessToken())).thenReturn(
                Jwt.withTokenValue(first.accessToken()).header("alg", "none").subject(USER_ID).build());
        when(keycloakUserService.realmRoleNames(USER_ID)).thenReturn(roles);
    }

    private static TokenResponse tokens(String name) {
        return new TokenResponse("access-" + name, 300L, 1800L, "refresh-" + name, "Bearer", "openid");
    }
}
