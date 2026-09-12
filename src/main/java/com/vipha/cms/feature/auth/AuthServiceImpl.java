package com.vipha.cms.feature.auth;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.userprofile.UserProfile;
import com.vipha.cms.feature.userprofile.UserProfileRepository;
import com.vipha.cms.security.KeycloakRoleEnum;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloak;
    private final UserProfileRepository userProfileRepository;

    @Value("${keycloak.realm}")
    private String realm;

    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }
    private GroupsResource getGroupsResource() {
        return keycloak.realm(realm).groups();
    }
    private UserRepresentation getUser(String username) {
        List<UserRepresentation> users = getUsersResource().search(username);

        if (users.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        return users.getFirst();
    }

    @Override
    public void register(RegisterRequest request) {

        if (!request.password().equals(request.confirmedPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmailVerified(true);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());

        user.setCredentials(List.of(credential));

        try (Response response = getUsersResource().create(user)) {

            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new BusinessRuleException(
                        "Username or email already exists"
                );
            }

            if (response.getStatus() != HttpStatus.CREATED.value()) {
                log.error(
                        "Failed to create Keycloak user. Status: {}",
                        response.getStatus()
                );

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to create user"
                );
            }

            log.info("User {} created successfully", user.getUsername());

            UserRepresentation createdUser = getUser(user.getUsername());

            saveUserProfile(createdUser.getId());
            assignRoles(createdUser.getId());
            assignGroup(createdUser.getId());
        }
    }

    private void saveUserProfile(String userId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setId(userId);

        userProfileRepository.save(userProfile);
    }

    private void assignRoles(String userId) {

        UserResource keycloakUser = getUsersResource().get(userId);

        RolesResource rolesResource = keycloak.realm(realm).roles();

        RoleRepresentation userRole = rolesResource
                .get(KeycloakRoleEnum.USER.toString())
                .toRepresentation();

        RoleRepresentation patientRole = rolesResource
                .get(KeycloakRoleEnum.PATIENT.toString())
                .toRepresentation();

        keycloakUser.roles()
                .realmLevel()
                .add(List.of(userRole, patientRole));
    }

    private void assignGroup(String userId) {
        UserResource keycloakUser = getUsersResource().get(userId);

        GroupRepresentation patientGroup = getGroupsResource()
                .groups("patient-management", 0, 1)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient group not found")
                );

        keycloakUser.joinGroup(patientGroup.getId());
    }
}