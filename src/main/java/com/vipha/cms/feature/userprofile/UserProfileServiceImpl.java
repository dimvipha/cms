package com.vipha.cms.feature.userprofile;

import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.userprofile.dto.PatchUserProfileRequest;
import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.UserNames;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final Keycloak keycloak;
    private final UserNames userNames;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public UserProfileResponse getUserProfile() {

        String userId = AuthUtils.extractUserId();

        UserProfile userProfile = getUserProfile(userId);

        UserRepresentation keycloakUser = getKeycloakUser(userId)
                .toRepresentation();

        return userProfileMapper.toUserProfileResponse(
                userProfile,
                keycloakUser.getFirstName(),
                keycloakUser.getLastName(),
                getPhoneNumber(keycloakUser)
        );
    }

    @Override
    public UserProfileResponse updateUserProfile(
            PatchUserProfileRequest request
    ) {

        String userId = AuthUtils.extractUserId();

        // Update database
        UserProfile userProfile = getUserProfile(userId);
        userProfileMapper.toEntity(request, userProfile);
        userProfileRepository.save(userProfile);

        // Update Keycloak
        UserResource userResource = getKeycloakUser(userId);
        UserRepresentation keycloakUser =
                userResource.toRepresentation();

        updateKeycloakUser(keycloakUser, request);

        userResource.update(keycloakUser);
        userNames.evict(userId);

        return userProfileMapper.toUserProfileResponse(
                userProfile,
                keycloakUser.getFirstName(),
                keycloakUser.getLastName(),
                getPhoneNumber(keycloakUser)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> findAll(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return userProfileRepository.findAll(pageable).map(this::toResponseWithKeycloak);
    }


    private UserProfileResponse toResponseWithKeycloak(UserProfile profile) {
        UserRepresentation kc;
        try {
            kc = getKeycloakUser(profile.getId()).toRepresentation();
        } catch (NotFoundException nf) {
            kc = null;
        }
        return userProfileMapper.toUserProfileResponse(
                profile,
                kc == null ? null : kc.getFirstName(),
                kc == null ? null : kc.getLastName(),
                kc == null ? null : getPhoneNumber(kc)
        );
    }

    private UserProfile getUserProfile(String userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User profile not found")
                );
    }

    private UserResource getKeycloakUser(String userId) {
        return keycloak
                .realm(realm)
                .users()
                .get(userId);
    }

    private void updateKeycloakUser(
            UserRepresentation user,
            PatchUserProfileRequest request
    ) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.phoneNumber() != null) {
            Map<String, List<String>> attributes =
                    user.getAttributes() != null
                            ? new HashMap<>(user.getAttributes())
                            : new HashMap<>();

            attributes.put(
                    "phoneNumber",
                    List.of(request.phoneNumber())
            );

            user.setAttributes(attributes);
        }
    }

    private String getPhoneNumber(UserRepresentation user) {

        if (user.getAttributes() == null) {
            return null;
        }

        List<String> phones =
                user.getAttributes().get("phoneNumber");

        return phones == null || phones.isEmpty()
                ? null
                : phones.getFirst();
    }
}