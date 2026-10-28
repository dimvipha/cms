package com.vipha.cms.feature.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Password login through the API: the web app's own sign-in and sign-up forms and API clients
 * such as Postman use it, so it is on, in production too, unless switched off.
 */
class PasswordEndpointsTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AuthController.class, PasswordAuthController.class)
            .withBean(AuthService.class, () -> mock(AuthService.class));

    @Test
    void onUnlessSwitchedOff() {
        runner.run(context -> assertThat(context).hasSingleBean(PasswordAuthController.class));
        runner.withPropertyValues("cms.auth.password-endpoints=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(PasswordAuthController.class)
                        // signing in on Keycloak's page still needs these
                        .hasSingleBean(AuthController.class));
    }

    @Test
    void onInProduction() {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasSingleBean(PasswordAuthController.class));
    }
}
