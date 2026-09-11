package com.vipha.cms.feature.auth;

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

    private UsersResource getUsersResource(){
        return keycloak.realm(realm).users();
    }
    private GroupsResource getGroupsResource(){
        return keycloak.realm(realm).groups();
    }

    private UserRepresentation getUser(String username){
        return getUsersResource().search(username).getFirst();
    }

    private void saveUserProfile(String username){
        UserRepresentation createUser =getUser(username);
        UserProfile userProfile = new UserProfile();
        userProfile.setId(createUser.getId());

        userProfileRepository.save(userProfile);

    }

    @Override
    public void register(RegisterRequest registerRequest) {
        // Create UserRepresentation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setFirstName(registerRequest.firstName());
        user.setLastName(registerRequest.lastName());

        // Validate password and confirmed password
        if (!registerRequest.password().equals(registerRequest.confirmedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Passwords do not match. Please try again");
        }

        // Prepare password credential
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registerRequest.password());
        user.setCredentials(List.of(credential));

        // Default value
        user.setEmailVerified(true);
        user.setEnabled(true);

        // Save into keycloak
        UsersResource usersResource = keycloak.realm(realm).users();

        try (Response response = usersResource.create(user)) {
            // TODO: failed (409, 401, 403, ...), succeed (201, 200, ...)
            log.info("Response status code: {}", response.getStatus());
            if (response.getStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Username or email already exists");
            }
            if (response.getStatus() == HttpStatus.CREATED.value()) {
                log.info("User {} created successfully", user.getUsername());
                saveUserProfile(user.getUsername());
                assignRoles(user.getUsername());
//                assignGroup(user.getUsername());
            }
        }
    }



//    public void assignGroup(String username){
//        UserRepresentation createUser =getUser(username);
//        UserResource keycloakUser = getUsersResource().get(createUser.getId());
//        GroupsResource groupsResource = keycloak.realm(realm).groups();
//        GroupRepresentation patientGroup=groupsResource
//                .groups("patient-management",0,1).getFirst();
//        log.info("Group Id: {}", patientGroup.getId());
//        keycloakUser.joinGroup(patientGroup.getId());
//    }

    private void assignRoles(String username) {
        // Start assigning role (USER, CUSTOMER)
        // Load created user by username from Keycloak
        UserRepresentation createUser =getUser(username);
        UserResource keycloakUser = getUsersResource().get(createUser.getId());

        // Create RoleRepresentation
        RolesResource rolesResource = keycloak.realm(realm).roles();
        RoleRepresentation roleUser = rolesResource
                .get(KeycloakRoleEnum.USER.toString())
                .toRepresentation();
        RoleRepresentation rolePatient = rolesResource
                .get(KeycloakRoleEnum.PATIENT.toString())
                .toRepresentation();
        List<RoleRepresentation> roles = List.of(roleUser, rolePatient);

        keycloakUser.roles()
                .realmLevel()
                .add(roles);
    }
}