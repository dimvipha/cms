package com.vipha.cms.feature.staff;

import com.vipha.cms.feature.doctor.DoctorService;
import com.vipha.cms.feature.staff.dto.CreateStaffRequest;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {

    private static final String USER_ID = "staff-1";

    @Mock KeycloakUserService keycloakUserService;
    @Mock UserProfileRepository userProfileRepository;
    @Mock DoctorService doctorService;

    StaffServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StaffServiceImpl(keycloakUserService, userProfileRepository, doctorService);
    }

    @Test
    void newStaffSetUpTwoFactorSignIn_inTheInviteAndAtTheirFirstSignIn() {
        when(keycloakUserService.createUser("sokha", "sokha@clinic.local", "Sokha", "Chan")).thenReturn(USER_ID);

        service.create(new CreateStaffRequest("sokha", "sokha@clinic.local", "Sokha", "Chan", Set.of(StaffType.NURSE)));

        verify(keycloakUserService).assignRealmRoles(USER_ID, "NURSE");
        verify(keycloakUserService).addRequiredActions(USER_ID, "CONFIGURE_TOTP");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> actions = ArgumentCaptor.forClass(List.class);
        verify(keycloakUserService).sendExecuteActionsEmail(eq(USER_ID), actions.capture(), anyInt());
        assertThat(actions.getValue()).containsExactly("UPDATE_PASSWORD", "VERIFY_EMAIL", "CONFIGURE_TOTP");
    }
}
