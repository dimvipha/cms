import { i18nBuilder } from "keycloakify/login";
import type { ThemeName } from "../kc.gen";

/**
 * Keycloak's own messages, with a few reworded to match the clinic's sign-in page, plus the
 * texts this theme adds: the clinic's name, the side panel and the line under each page title.
 */
const { useI18n, ofTypeI18n } = i18nBuilder
    .withThemeName<ThemeName>()
    .withCustomTranslations({
        en: {
            clinicName: "Spring Clinic",
            panelTitle: "WELCOME",
            panelSignIn: "Sign in to book a visit, follow your prescriptions and invoices, or open the staff console.",
            panelRegister: "Create your patient account in a minute, then book a visit with the doctor you choose.",
            panelPassword: "It happens to everyone. Set a new password and you are back in.",
            panelTwoFactor: "Staff accounts open medical records, so they are protected by a second step: a code from your phone.",
            panelEmail: "One more step: confirm your email address so the clinic can reach you about your visits.",
            panelDefault: "Your clinic, one sign-in away.",

            subtitleSignIn: "Enter your credentials to access your account.",
            subtitleRegister: "It takes a minute. You add your patient details after signing in.",
            subtitleUpdatePassword: "Choose a new password for your account.",
            subtitleOtp: "Enter the 6-digit code from your authenticator app.",
            subtitleConfigTotp: "Protect your account with a code from your phone each time you sign in.",
            placeholderEmail: "Enter your email",
            placeholderPassword: "Enter your password",
            orContinueWith: "or continue with",
            haveAccount: "Already have an account?",
            signIn: "Sign in",

            // Keycloak's wording, adjusted
            loginAccountTitle: "Sign In",
            doLogIn: "Sign In",
            registerTitle: "Create your account",
            doRegister: "Register",
            backToLogin: "Back to sign in",
            backToApplication: "Back to the app",
            proceedWithAction: "Continue",
            pageExpiredRestart: "Start again",
            pageExpiredContinue: "Continue where I was",
            emailForgotTitle: "Forgot your password?",
            loginTotpTitle: "Set up two-factor sign-in"
        }
    })
    .build();

type I18n = typeof ofTypeI18n;

export { useI18n, type I18n };
