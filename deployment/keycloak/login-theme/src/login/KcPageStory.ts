import { createGetKcContextMock } from "keycloakify/login/KcContext";
import { kcEnvDefaults, themeNames } from "../kc.gen";
import type { KcContextExtension, KcContextExtensionPerPage } from "./KcContext";

const kcContextExtension: KcContextExtension = {
    themeName: themeNames[0],
    properties: { ...kcEnvDefaults }
};

const kcContextExtensionPerPage: KcContextExtensionPerPage = {};

/**
 * Sample Keycloak data for previewing pages with `npm run dev`, set up like the clinic realm:
 * the email is the username, sign-up and password reset are on, and Google is offered.
 */
export const { getKcContextMock } = createGetKcContextMock({
    kcContextExtension,
    kcContextExtensionPerPage,
    overrides: {
        realm: { displayName: "CMS Clinic", registrationEmailAsUsername: true }
    },
    overridesPerPage: {
        "login.ftl": {
            realm: { registrationAllowed: true, loginWithEmailAllowed: true, resetPasswordAllowed: true, rememberMe: false },
            social: {
                providers: [{ alias: "google", displayName: "Google", providerId: "google", loginUrl: "#", iconClasses: "fa fa-google" }]
            }
        }
    }
});
