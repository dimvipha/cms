package com.vipha.cms.feature.auth;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.auth.dto.LoginRequest;
import com.vipha.cms.feature.auth.dto.LogoutRequest;
import com.vipha.cms.feature.auth.dto.RefreshRequest;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakAuthClient;
import com.vipha.cms.security.KeycloakUserService;
import com.vipha.cms.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String PATIENT_GROUP = "patient-management";

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

    private void saveUserProfile(String userId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setId(userId);
        userProfileRepository.save(userProfile);
    }
}
