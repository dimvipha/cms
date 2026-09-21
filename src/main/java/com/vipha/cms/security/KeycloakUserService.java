package com.vipha.cms.security;

import com.vipha.cms.common.exception.BusinessRuleException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(String username, String email, String firstName, String lastName) {
        return createUser(username, email, firstName, lastName, null, false, false);
    }

    public String createUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            boolean temporaryPassword,
            boolean emailVerified
    ) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(emailVerified);

        if (password != null) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporaryPassword);
            user.setCredentials(List.of(credential));
        }

        try (Response response = users().create(user)) {
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new BusinessRuleException("Username or email already exists");
            }
            if (response.getStatus() != HttpStatus.CREATED.value()) {
                log.error("Keycloak create user failed. Status: {}", response.getStatus());
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to create user"
                );
            }
            return extractCreatedUserId(response);
        }
    }

    public void sendExecuteActionsEmail(String userId, List<String> actions, int lifespanSeconds) {
        users().get(userId).executeActionsEmail(null, null, lifespanSeconds, actions);
    }

    public void assignRealmRoles(String userId, String... roleNames) {
        UserResource user = users().get(userId);
        List<RoleRepresentation> roles = Arrays.stream(roleNames)
                .map(name -> realmResource().roles().get(name).toRepresentation())
                .toList();
        user.roles().realmLevel().add(roles);
    }

    public void joinGroup(String userId, String groupName) {
        GroupRepresentation group = realmResource().groups()
                .groups(groupName, 0, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Keycloak group not found: " + groupName
                ));
        users().get(userId).joinGroup(group.getId());
    }

    public void deleteUser(String userId) {
        users().delete(userId);
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }

    private UsersResource users() {
        return realmResource().users();
    }

    private String extractCreatedUserId(Response response) {
        URI location = response.getLocation();
        if (location == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not determine created user ID"
            );
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
