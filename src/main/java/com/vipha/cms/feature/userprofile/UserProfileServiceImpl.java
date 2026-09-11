package com.vipha.cms.feature.userprofile;


import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;
import com.vipha.cms.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public UserProfileResponse getUserProfile() {
        String userId= AuthUtils.extractUserId();
        UserProfile userProfile = userProfileRepository.findById(userId).orElseThrow(
                ()-> new RuntimeException("User profile not found")
        );

        // return user profile from keycloak
        UsersResource usersResource = keycloak.realm(realm).users();
        UserResource userResource = usersResource.get(userId);
        UserRepresentation userRepresentation=userResource.toRepresentation();

        String phoneNumber=userRepresentation.getAttributes().get("phoneNumber").getFirst();

        return userProfileMapper.toUserProfileResponse(userProfile, userRepresentation.getFirstName(), userRepresentation.getLastName(),phoneNumber);
    }

}
