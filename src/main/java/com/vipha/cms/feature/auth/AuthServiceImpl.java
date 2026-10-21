package com.vipha.cms.feature.auth;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.auth.dto.ChangePasswordRequest;
import com.vipha.cms.feature.auth.dto.ForgotPasswordRequest;
import com.vipha.cms.feature.auth.dto.LoginRequest;
import com.vipha.cms.feature.auth.dto.LogoutRequest;
import com.vipha.cms.feature.auth.dto.RefreshRequest;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.KeycloakAuthClient;
import com.vipha.cms.security.KeycloakUserService;
import com.vipha.cms.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String PATIENT_GROUP = "patient-management";
    private static final List<String> RESET_ACTIONS = List.of("UPDATE_PASSWORD");
    private static final int RESET_LINK_LIFESPAN_SECONDS = 3_600;

    private final KeycloakUserService keycloakUserService;
    private final KeycloakAuthClient keycloakAuthClient;
    private final UserProfileRepository userProfileRepository;

    @Override
    public void register(RegisterRequest request) {

        if (!request.password().equals(request.confirmedPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }

        String userId = keycloakUserService.createUser(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                false,
                true
        );

        try {
            keycloakUserService.assignRealmRoles(userId, Roles.PATIENT);
            keycloakUserService.joinGroup(userId, PATIENT_GROUP);
            saveUserProfile(userId);
        } catch (RuntimeException ex) {
            keycloakUserService.deleteUser(userId);
            throw ex;
        }

        log.info("Patient {} registered", request.username());
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        return keycloakAuthClient.login(request.username(), request.password());
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        return keycloakAuthClient.refresh(request.refreshToken());
    }

    @Override
    public void logout(LogoutRequest request) {
        keycloakAuthClient.logout(request.refreshToken());
    }

    /**
     * Emails a reset link when the address belongs to an active account. The
     * caller always gets the same answer, so the endpoint can't be used to find
     * out which emails have accounts.
     */
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        keycloakUserService.findEnabledUserIdByEmail(request.email()).ifPresent(userId -> {
            try {
                keycloakUserService.sendExecuteActionsEmail(userId, RESET_ACTIONS, RESET_LINK_LIFESPAN_SECONDS);
                log.info("Password reset email sent for user {}", userId);
            } catch (RuntimeException ex) {
                log.error("Password reset email for user {} failed: {}", userId, ex.getMessage());
            }
        });
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmedPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessRuleException("New password must be different from the current one");
        }

        String userId = AuthUtils.extractUserId();
        String username = AuthUtils.extractUsername();
        if (username == null) {
            username = keycloakUserService.getUsername(userId);
        }

        // proving the current password opens a session; close it again right away
        TokenResponse check;
        try {
            check = keycloakAuthClient.login(username, request.currentPassword());
        } catch (BadCredentialsException ex) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        keycloakAuthClient.logout(check.refreshToken());

        keycloakUserService.resetPassword(userId, request.newPassword());
        log.info("User {} changed their password", userId);
    }

    private void saveUserProfile(String userId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setId(userId);
        userProfileRepository.save(userProfile);
    }
}
