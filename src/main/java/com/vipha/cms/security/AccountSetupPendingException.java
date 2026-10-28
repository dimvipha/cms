package com.vipha.cms.security;

/**
 * The password is right, but the account has steps to finish first, such as choosing a new
 * password or setting up two-factor sign-in. Keycloak walks people through those on its own page.
 * Answered with 403, so a sign-in form can tell it from a wrong password (401).
 */
public class AccountSetupPendingException extends RuntimeException {

    public static final String MESSAGE =
            "Your account has setup steps to finish first, such as a new password or two-factor sign-in.";

    public AccountSetupPendingException() {
        super(MESSAGE);
    }
}
