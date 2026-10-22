import { useLayoutEffect, useState } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { UserProfileFormFieldsProps } from "keycloakify/login/UserProfileFormFieldsProps";
import type { JSX } from "keycloakify/tools/JSX";
import type { LazyOrNot } from "keycloakify/tools/LazyOrNot";
import { button, primaryButton } from "../classes";
import type { I18n } from "../i18n";
import type { KcContext } from "../KcContext";

type RegisterProps = PageProps<Extract<KcContext, { pageId: "register.ftl" }>, I18n> & {
    UserProfileFormFields: LazyOrNot<(props: UserProfileFormFieldsProps) => JSX.Element>;
    doMakeUserConfirmPassword: boolean;
};

/**
 * Signing up: the realm's user profile fields (email, password, names, phone, ...), then the
 * button, and the way back to signing in below, as on the sign-in page.
 */
export default function Register(props: RegisterProps) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes, UserProfileFormFields, doMakeUserConfirmPassword } = props;
    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });
    const { messageHeader, url, messagesPerField, recaptchaRequired, recaptchaVisible, recaptchaSiteKey, recaptchaAction, termsAcceptanceRequired } =
        kcContext;
    const { msg, msgStr, advancedMsg } = i18n;

    const [isFormSubmittable, setIsFormSubmittable] = useState(false);
    const [areTermsAccepted, setAreTermsAccepted] = useState(false);

    useLayoutEffect(() => {
        (window as unknown as Record<string, unknown>)["onSubmitRecaptcha"] = () => {
            (document.getElementById("kc-register-form") as HTMLFormElement | null)?.requestSubmit();
        };
        return () => {
            delete (window as unknown as Record<string, unknown>)["onSubmitRecaptcha"];
        };
    }, []);

    const invisibleRecaptcha = recaptchaRequired && !recaptchaVisible && recaptchaAction !== undefined;

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            headerNode={messageHeader !== undefined ? advancedMsg(messageHeader) : msg("registerTitle")}
            displayMessage={messagesPerField.exists("global")}
            displayInfo
            infoNode={
                <span>
                    {msg("haveAccount")}
                    <a href={url.loginUrl} className="ml-1">
                        {msg("signIn")}
                    </a>
                </span>
            }
        >
            <form id="kc-register-form" className={kcClsx("kcFormClass")} action={url.registrationAction} method="post">
                <UserProfileFormFields
                    kcContext={kcContext}
                    i18n={i18n}
                    kcClsx={kcClsx}
                    onIsFormSubmittableValueChange={setIsFormSubmittable}
                    doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                />

                {termsAcceptanceRequired && (
                    <div className="grid gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
                        <p className="font-medium text-slate-900">{msg("termsTitle")}</p>
                        <div id="kc-registration-terms-text" className="text-slate-600">
                            {msg("termsText")}
                        </div>
                        <label className="flex items-center gap-2 text-slate-700">
                            <input
                                type="checkbox"
                                id="termsAccepted"
                                name="termsAccepted"
                                className="h-4 w-4 accent-teal-600"
                                checked={areTermsAccepted}
                                onChange={e => setAreTermsAccepted(e.target.checked)}
                                aria-invalid={messagesPerField.existsError("termsAccepted")}
                            />
                            {msg("acceptTerms")}
                        </label>
                        {messagesPerField.existsError("termsAccepted") && (
                            <span
                                id="input-error-terms-accepted"
                                className={kcClsx("kcInputErrorMessageClass")}
                                aria-live="polite"
                                dangerouslySetInnerHTML={{ __html: kcSanitize(messagesPerField.get("termsAccepted")) }}
                            />
                        )}
                    </div>
                )}

                {recaptchaRequired && (recaptchaVisible || recaptchaAction === undefined) && (
                    <div className="g-recaptcha" data-size="compact" data-sitekey={recaptchaSiteKey} data-action={recaptchaAction} />
                )}

                <div id="kc-form-buttons" className="pt-2">
                    {invisibleRecaptcha ? (
                        <button
                            className={`${button} ${primaryButton} g-recaptcha w-full`}
                            data-sitekey={recaptchaSiteKey}
                            data-callback="onSubmitRecaptcha"
                            data-action={recaptchaAction}
                            type="submit"
                        >
                            {msg("doRegister")}
                        </button>
                    ) : (
                        <input
                            disabled={!isFormSubmittable || (termsAcceptanceRequired && !areTermsAccepted)}
                            className={`${button} ${primaryButton} w-full`}
                            type="submit"
                            value={msgStr("doRegister")}
                        />
                    )}
                </div>
            </form>
        </Template>
    );
}
