package com.vipha.cms.security;

import com.vipha.cms.common.FrontendProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloak;
    private final FrontendProperties frontend;

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

    /** Emails the user a link to perform the actions; when done, Keycloak offers a link back to the frontend's login page. */
    public void sendExecuteActionsEmail(String userId, List<String> actions, int lifespanSeconds) {
        users().get(userId).executeActionsEmail(frontend.clientId(), frontend.loginUrl(), lifespanSeconds, actions);
    }

    /** The enabled user with exactly this email, if any. */
    public Optional<String> findEnabledUserIdByEmail(String email) {
        return users().searchByEmail(email, true).stream()
                .filter(user -> Boolean.TRUE.equals(user.isEnabled()))
                .map(UserRepresentation::getId)
                .findFirst();
    }

    public String getUsername(String userId) {
        return users().get(userId).toRepresentation().getUsername();
    }

    /** Sets a new permanent password; the realm's password policy may reject it. */
    public void resetPassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        try {
            users().get(userId).resetPassword(credential);
        } catch (BadRequestException ex) {
            throw new BusinessRuleException("Password rejected: " + keycloakError(ex.getResponse()));
        }
    }

    public void assignRealmRoles(String userId, String... roleNames) {
        UserResource user = users().get(userId);
        List<RoleRepresentation> roles = Arrays.stream(roleNames)
                .map(name -> realmResource().roles().get(name).toRepresentation())
                .toList();
        user.roles().realmLevel().add(roles);
    }

    /** Steps Keycloak makes the user take at their next sign-in, e.g. CONFIGURE_TOTP. */
    public void addRequiredActions(String userId, String... actions) {
        UserResource resource = users().get(userId);
        UserRepresentation user = resource.toRepresentation();
        Set<String> required = new LinkedHashSet<>(
                user.getRequiredActions() == null ? List.of() : user.getRequiredActions());
        required.addAll(Arrays.asList(actions));
        user.setRequiredActions(List.copyOf(required));
        resource.update(user);
    }

    /** The user's realm roles, given directly or through a group. */
    public Set<String> realmRoleNames(String userId) {
        return users().get(userId).roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Whether the realm offers sign-in through this identity provider, e.g. "google". Without the
     * view-identity-providers role (keycloak_setup.sh grants it) the answer is no, not an error.
     */
    public boolean hasIdentityProvider(String alias) {
        try {
            return Boolean.TRUE.equals(realmResource().identityProviders().get(alias).toRepresentation().isEnabled());
        } catch (NotFoundException ex) {
            return false;
        } catch (ForbiddenException ex) {
            log.warn("Cannot see identity provider {}: re-run deployment/keycloak_setup.sh", alias);
            return false;
        }
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

    /**
     * "First Last" for display on printed documents. Empty when the user is gone or
     * Keycloak can't be reached, so a missing name never blocks printing.
     */
    public Optional<String> findFullName(String userId) {
        return findName(userId).map(PersonName::fullName);
    }

    /** Empty when the user is gone or Keycloak can't be reached, so a missing name never blocks a response. */
    public Optional<PersonName> findName(String userId) {
        try {
            UserRepresentation user = users().get(userId).toRepresentation();
            PersonName name = new PersonName(user.getFirstName(), user.getLastName());
            return name.fullName() == null ? Optional.empty() : Optional.of(name);
        } catch (WebApplicationException | ProcessingException ex) {
            log.warn("Could not resolve name for user {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Keycloak's error_description, e.g. "Invalid password: minimum length 8." */
    private String keycloakError(Response response) {
        try {
            Object description = response.readEntity(Map.class).get("error_description");
            return description != null ? description.toString() : "HTTP " + response.getStatus();
        } catch (RuntimeException ex) {
            return "HTTP " + response.getStatus();
        }
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
