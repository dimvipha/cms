package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakAuthClient;
import com.vipha.cms.security.KeycloakUserService;
import com.vipha.cms.security.Roles;
import com.vipha.cms.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomeTest {

    private static final String USER_ID = "new-user-1";

    @Mock KeycloakUserService keycloakUserService;
    @Mock KeycloakAuthClient keycloakAuthClient;
    @Mock UserProfileRepository userProfileRepository;

    AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(keycloakUserService, keycloakAuthClient, userProfileRepository);
        TestAuth.loginAs(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestAuth.logout();
    }

    @Test
    void someoneNewBecomesAPatient() {
        when(keycloakUserService.realmRoleNames(USER_ID)).thenReturn(Set.of("default-roles-clinic", "offline_access"));

        assertThat(service.welcome().joined()).isTrue();
        verify(keycloakUserService).assignRealmRoles(USER_ID, Roles.PATIENT);
        verify(keycloakUserService).joinGroup(USER_ID, "patient-management");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void someoneWithAnAccountKeepsIt_whetherPatientOrStaff() {
        for (String role : List.of(Roles.PATIENT, Roles.DOCTOR, Roles.ADMIN)) {
            when(keycloakUserService.realmRoleNames(USER_ID)).thenReturn(Set.of(role, "default-roles-clinic"));

            assertThat(service.welcome().joined()).as(role).isFalse();
        }
        verify(keycloakUserService, never()).assignRealmRoles(anyString(), any(String[].class));
    }

    @Test
    void optionsSayWhetherTheRealmOffersGoogle() {
        when(keycloakUserService.hasIdentityProvider("google")).thenReturn(true);
        assertThat(service.googleOptions().enabled()).isTrue();
    }
}
