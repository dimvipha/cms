package com.vipha.cms.feature.staff;

import com.vipha.cms.feature.doctor.DoctorService;
import com.vipha.cms.feature.staff.dto.CreateStaffRequest;
import com.vipha.cms.feature.staff.dto.StaffResponse;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    /** Staff see medical records, so they also set up a second sign-in step (an authenticator app). */
    private static final String TWO_FACTOR = "CONFIGURE_TOTP";
    private static final List<String> SETUP_ACTIONS = List.of("UPDATE_PASSWORD", "VERIFY_EMAIL", TWO_FACTOR);
    private static final List<String> PASSWORD_RESET_ACTIONS = List.of("UPDATE_PASSWORD");
    private static final int SETUP_LINK_LIFESPAN_SECONDS = 43_200;
    private static final int PASSWORD_RESET_LIFESPAN_SECONDS = 3_600;

    private final KeycloakUserService keycloakUserService;
    private final UserProfileRepository userProfileRepository;
    private final DoctorService doctorService;

    @Override
    public StaffResponse create(CreateStaffRequest request) {

        String userId = keycloakUserService.createUser(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName()
        );

        String[] roleNames = request.roles().stream()
                .map(Enum::name)
                .toArray(String[]::new);

        try {
            keycloakUserService.assignRealmRoles(userId, roleNames);
            // asked at sign-in too, should they set their password without finishing the email's steps
            keycloakUserService.addRequiredActions(userId, TWO_FACTOR);
            keycloakUserService.sendExecuteActionsEmail(userId, SETUP_ACTIONS, SETUP_LINK_LIFESPAN_SECONDS);
            saveUserProfile(userId);
            if (request.roles().contains(StaffType.DOCTOR)) {
                doctorService.createDoctorProfile(userId);
            }
        } catch (RuntimeException ex) {
            keycloakUserService.deleteUser(userId);
            throw ex;
        }

        log.info("Staff user {} created with roles {}; account setup email sent",
                request.username(), request.roles());

        return new StaffResponse(
                userId,
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.roles()
        );
    }

    @Override
    public void triggerPasswordReset(String userId) {
        keycloakUserService.sendExecuteActionsEmail(
                userId, PASSWORD_RESET_ACTIONS, PASSWORD_RESET_LIFESPAN_SECONDS);
        log.info("Password reset email sent for user {}", userId);
    }

    private void saveUserProfile(String userId) {
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        userProfileRepository.save(profile);
    }
}
