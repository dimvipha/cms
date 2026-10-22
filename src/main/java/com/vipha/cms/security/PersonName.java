package com.vipha.cms.security;

import org.springframework.util.StringUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A Keycloak user's first and last name. */
public record PersonName(String firstName, String lastName) {

    /** "First Last", or null when neither is set. */
    public String fullName() {
        String full = Stream.of(firstName, lastName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        return full.isEmpty() ? null : full;
    }
}
