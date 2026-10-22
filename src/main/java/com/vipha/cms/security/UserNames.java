package com.vipha.cms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names of staff and patients for responses that only store a Keycloak user id
 * (doctors, vitals recorders, payment takers). Names live in Keycloak, so they
 * are cached for a few minutes to keep list endpoints to one lookup per person.
 */
@Component
@RequiredArgsConstructor
public class UserNames {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final KeycloakUserService keycloakUserService;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(PersonName name, Instant expiresAt) {}

    /** Empty when the user is gone or Keycloak can't be reached; such misses are not cached. */
    public Optional<PersonName> find(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        Cached cached = cache.get(userId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Optional.of(cached.name());
        }
        Optional<PersonName> name = keycloakUserService.findName(userId);
        name.ifPresent(n -> cache.put(userId, new Cached(n, Instant.now().plus(TTL))));
        return name;
    }

    /** "First Last", or null when unknown. */
    public String fullName(String userId) {
        return find(userId).map(PersonName::fullName).orElse(null);
    }

    /** Call after changing a user's name so the next lookup sees it. */
    public void evict(String userId) {
        cache.remove(userId);
    }
}
