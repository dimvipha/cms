package com.vipha.cms.feature.staff;

import com.vipha.cms.feature.staff.dto.CreateStaffRequest;
import com.vipha.cms.feature.staff.dto.StaffResponse;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private static final String PASSWORD_POOL =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 16;

    private final KeycloakUserService keycloakUserService;
    private final UserProfileRepository userProfileRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    public StaffResponse create(CreateStaffRequest request) {

        String password = StringUtils.hasText(request.initialPassword())
                ? request.initialPassword()
                : generateTemporaryPassword();

        String userId = keycloakUserService.createUser(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                password,
                true,
                false
        );

        try {
            keycloakUserService.assignRealmRoles(userId, request.role().name());
            saveUserProfile(userId);
        } catch (RuntimeException ex) {
            keycloakUserService.deleteUser(userId);
            throw ex;
        }

        log.info("Staff user {} created with role {}", request.username(), request.role());

        return new StaffResponse(
                userId,
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.role(),
                password
        );
    }

    private void saveUserProfile(String userId) {
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        userProfileRepository.save(profile);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_POOL.charAt(random.nextInt(PASSWORD_POOL.length())));
        }
        return sb.toString();
    }
}
