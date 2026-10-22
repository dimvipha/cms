import { lazy, Suspense } from "react";
import DefaultPage from "keycloakify/login/DefaultPage";
import { classes } from "./classes";
import { useI18n } from "./i18n";
import type { KcContext } from "./KcContext";
import Template from "./Template";
import "../index.css";

const UserProfileFormFields = lazy(() => import("keycloakify/login/UserProfileFormFields"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const LoginConfigTotp = lazy(() => import("./pages/LoginConfigTotp"));
const Info = lazy(() => import("./pages/Info"));
const LoginPageExpired = lazy(() => import("./pages/LoginPageExpired"));

const doMakeUserConfirmPassword = true;

/**
 * Sign-in, sign-up, two-factor setup, notices and the expired page are drawn by hand; every other
 * Keycloak page is Keycloakify's own, inside the clinic's Template and styled through `classes`.
 */
export default function KcPage(props: { kcContext: KcContext }) {
    const { kcContext } = props;
    const { i18n } = useI18n({ kcContext });
    const common = { i18n, classes, Template, doUseDefaultCss: false } as const;

    return (
        <Suspense>
            {(() => {
                switch (kcContext.pageId) {
                    case "login.ftl":
                        return <Login kcContext={kcContext} {...common} />;
                    case "register.ftl":
                        return (
                            <Register
                                kcContext={kcContext}
                                {...common}
                                UserProfileFormFields={UserProfileFormFields}
                                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                            />
                        );
                    case "login-config-totp.ftl":
                        return <LoginConfigTotp kcContext={kcContext} {...common} />;
                    case "info.ftl":
                        return <Info kcContext={kcContext} {...common} />;
                    case "login-page-expired.ftl":
                        return <LoginPageExpired kcContext={kcContext} {...common} />;
                    default:
                        return (
                            <DefaultPage
                                kcContext={kcContext}
                                {...common}
                                UserProfileFormFields={UserProfileFormFields}
                                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                            />
                        );
                }
            })()}
        </Suspense>
    );
}
