import { useState } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { useScript } from "keycloakify/login/pages/Login.useScript";
import { useIsPasswordRevealed } from "keycloakify/tools/useIsPasswordRevealed";
import { Eye, EyeOff, Lock, User } from "lucide-react";
import { button, classes as kc, outlineButton, primaryButton } from "../classes";
import type { I18n } from "../i18n";
import type { KcContext } from "../KcContext";

/** The clinic's sign-in page: email and password, Google, and the way to sign up. */
export default function Login(props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { social, realm, url, usernameHidden, login, auth, registrationDisabled, messagesPerField, enableWebAuthnConditionalUI } = kcContext;
    const { msg, msgStr } = i18n;

    const [isLoginButtonDisabled, setIsLoginButtonDisabled] = useState(false);
    const webAuthnButtonId = "authenticateWebAuthnButton";
    useScript({ webAuthnButtonId, kcContext, i18n });

    const hasError = messagesPerField.existsError("username", "password");
    const error = hasError ? (
        <span
            id="input-error"
            className={kc.kcInputErrorMessageClass}
            aria-live="polite"
            dangerouslySetInnerHTML={{ __html: kcSanitize(messagesPerField.getFirstError("username", "password")) }}
        />
    ) : null;
    const providers = realm.password ? (social?.providers ?? []) : [];

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={!hasError}
            headerNode={msg("loginAccountTitle")}
            displayInfo={realm.password && realm.registrationAllowed && !registrationDisabled}
            infoNode={
                <span id="kc-registration">
                    {msg("noAccount")}
                    <a tabIndex={8} href={url.registrationUrl} className="ml-1">
                        {msg("doRegister")}
                    </a>
                </span>
            }
            socialProvidersNode={
                providers.length !== 0 && (
                    <div id="kc-social-providers">
                        <div className="my-4 flex items-center gap-3 text-xs text-slate-400">
                            <span className="h-px flex-1 bg-slate-200" />
                            {msg("orContinueWith")}
                            <span className="h-px flex-1 bg-slate-200" />
                        </div>
                        <ul className="m-0 flex list-none flex-col gap-2 p-0">
                            {providers.map(p => (
                                <li key={p.alias}>
                                    <a
                                        id={`social-${p.alias}`}
                                        href={p.loginUrl}
                                        className={`${button} ${outlineButton} w-full font-medium! text-slate-700! no-underline!`}
                                    >
                                        {p.providerId === "google" && <GoogleLogo />}
                                        <span dangerouslySetInnerHTML={{ __html: kcSanitize(p.displayName) }} />
                                    </a>
                                </li>
                            ))}
                        </ul>
                    </div>
                )
            }
        >
            {realm.password && (
                <form
                    id="kc-form-login"
                    className="space-y-4"
                    onSubmit={() => {
                        setIsLoginButtonDisabled(true);
                        return true;
                    }}
                    action={url.loginAction}
                    method="post"
                >
                    {!usernameHidden && (
                        <div className={kc.kcFormGroupClass}>
                            <label htmlFor="username" className={kc.kcLabelClass}>
                                {!realm.loginWithEmailAllowed
                                    ? msg("username")
                                    : !realm.registrationEmailAsUsername
                                      ? msg("usernameOrEmail")
                                      : msg("email")}
                            </label>
                            <div className="relative">
                                <User className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-500" />
                                <input
                                    tabIndex={2}
                                    id="username"
                                    name="username"
                                    className={`${kc.kcInputClass} pl-9`}
                                    defaultValue={login.username ?? ""}
                                    type="text"
                                    placeholder={realm.registrationEmailAsUsername ? msgStr("placeholderEmail") : undefined}
                                    autoFocus
                                    autoComplete={enableWebAuthnConditionalUI ? "username webauthn" : "username"}
                                    aria-invalid={hasError}
                                />
                            </div>
                            {error}
                        </div>
                    )}

                    <div className={kc.kcFormGroupClass}>
                        <label htmlFor="password" className={kc.kcLabelClass}>
                            {msg("password")}
                        </label>
                        <PasswordInput i18n={i18n} invalid={hasError} />
                        {usernameHidden && error}
                    </div>

                    <div className="flex items-center justify-between pt-1 text-sm">
                        {realm.rememberMe && !usernameHidden ? (
                            <label className="inline-flex cursor-pointer items-center gap-2 text-slate-600 select-none">
                                <input
                                    tabIndex={5}
                                    id="rememberMe"
                                    name="rememberMe"
                                    type="checkbox"
                                    className="h-4 w-4 accent-teal-600"
                                    defaultChecked={!!login.rememberMe}
                                />
                                {msg("rememberMe")}
                            </label>
                        ) : (
                            <span />
                        )}
                        {realm.resetPasswordAllowed && (
                            <a tabIndex={6} href={url.loginResetCredentialsUrl}>
                                {msg("doForgotPassword")}
                            </a>
                        )}
                    </div>

                    <div id="kc-form-buttons">
                        <input type="hidden" id="id-hidden-input" name="credentialId" value={auth.selectedCredential} />
                        <input
                            tabIndex={7}
                            disabled={isLoginButtonDisabled}
                            className={`${button} ${primaryButton} w-full`}
                            name="login"
                            id="kc-login"
                            type="submit"
                            value={msgStr("doLogIn")}
                        />
                    </div>
                </form>
            )}

            {enableWebAuthnConditionalUI && (
                <>
                    <form id="webauth" action={url.loginAction} method="post">
                        <input type="hidden" id="clientDataJSON" name="clientDataJSON" />
                        <input type="hidden" id="authenticatorData" name="authenticatorData" />
                        <input type="hidden" id="signature" name="signature" />
                        <input type="hidden" id="credentialId" name="credentialId" />
                        <input type="hidden" id="userHandle" name="userHandle" />
                        <input type="hidden" id="error" name="error" />
                    </form>
                    {kcContext.authenticators !== undefined && kcContext.authenticators.authenticators.length !== 0 && (
                        <form id="authn_select">
                            {kcContext.authenticators.authenticators.map((authenticator, i) => (
                                <input key={i} type="hidden" name="authn_use_chk" readOnly value={authenticator.credentialId} />
                            ))}
                        </form>
                    )}
                    <input id={webAuthnButtonId} type="button" className={`${button} ${outlineButton} mt-3 w-full`} value={msgStr("passkey-doAuthenticate")} />
                </>
            )}
        </Template>
    );
}

function PasswordInput(props: { i18n: I18n; invalid: boolean }) {
    const { i18n, invalid } = props;
    const { isPasswordRevealed, toggleIsPasswordRevealed } = useIsPasswordRevealed({ passwordInputId: "password" });

    return (
        <div className="relative">
            <Lock className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-500" />
            <input
                tabIndex={3}
                id="password"
                name="password"
                className={`${kc.kcInputClass} pr-10 pl-9`}
                type="password"
                placeholder={i18n.msgStr("placeholderPassword")}
                autoComplete="current-password"
                aria-invalid={invalid}
            />
            <button
                type="button"
                tabIndex={4}
                className={kc.kcFormPasswordVisibilityButtonClass}
                aria-label={i18n.msgStr(isPasswordRevealed ? "hidePassword" : "showPassword")}
                aria-controls="password"
                onClick={toggleIsPasswordRevealed}
            >
                {isPasswordRevealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
        </div>
    );
}

function GoogleLogo() {
    return (
        <svg aria-hidden viewBox="0 0 24 24" className="h-4 w-4">
            <path fill="#4285F4" d="M23.5 12.27c0-.82-.07-1.6-.2-2.36H12v4.47h6.45a5.52 5.52 0 0 1-2.39 3.62v3h3.87c2.26-2.09 3.57-5.16 3.57-8.73Z" />
            <path fill="#34A853" d="M12 24c3.24 0 5.95-1.07 7.93-2.9l-3.87-3c-1.07.72-2.45 1.15-4.06 1.15-3.12 0-5.77-2.11-6.71-4.95h-4v3.1A12 12 0 0 0 12 24Z" />
            <path fill="#FBBC05" d="M5.29 14.3a7.2 7.2 0 0 1 0-4.6V6.6h-4a12 12 0 0 0 0 10.8l4-3.1Z" />
            <path fill="#EA4335" d="M12 4.75c1.76 0 3.34.6 4.58 1.8l3.43-3.43A11.97 11.97 0 0 0 12 0 12 12 0 0 0 1.29 6.6l4 3.1C6.23 6.86 8.88 4.75 12 4.75Z" />
        </svg>
    );
}
