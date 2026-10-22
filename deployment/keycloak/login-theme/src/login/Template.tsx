import { useEffect } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { CircleAlert, CircleCheck, Globe, Info, RotateCcw, Stethoscope, TriangleAlert } from "lucide-react";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";

type MessageKey = Parameters<I18n["msgStr"]>[0];

/** The side panel's line for each kind of page. */
function panelTextKey(pageId: KcContext["pageId"]): MessageKey {
    switch (pageId) {
        case "login.ftl":
        case "login-username.ftl":
        case "login-password.ftl":
            return "panelSignIn";
        case "register.ftl":
            return "panelRegister";
        case "login-reset-password.ftl":
        case "login-update-password.ftl":
            return "panelPassword";
        case "login-config-totp.ftl":
        case "login-otp.ftl":
        case "login-reset-otp.ftl":
            return "panelTwoFactor";
        case "login-verify-email.ftl":
        case "update-email.ftl":
            return "panelEmail";
        default:
            return "panelDefault";
    }
}

/** The line under the page title, where Keycloak's page has none of its own. */
function subtitleKey(pageId: KcContext["pageId"]): MessageKey | undefined {
    switch (pageId) {
        case "login.ftl":
        case "login-password.ftl":
            return "subtitleSignIn";
        case "register.ftl":
            return "subtitleRegister";
        case "login-update-password.ftl":
            return "subtitleUpdatePassword";
        case "login-otp.ftl":
            return "subtitleOtp";
        case "login-config-totp.ftl":
            return "subtitleConfigTotp";
        default:
            return undefined;
    }
}

const ALERT = {
    success: { style: "border-emerald-200 bg-emerald-50 text-emerald-800", Icon: CircleCheck },
    warning: { style: "border-amber-200 bg-amber-50 text-amber-800", Icon: TriangleAlert },
    error: { style: "border-red-200 bg-red-50 text-red-700", Icon: CircleAlert },
    info: { style: "border-sky-200 bg-sky-50 text-sky-800", Icon: Info }
};

/**
 * Every Keycloak page in the clinic's two-panel card: the teal welcome panel on the left (hidden
 * on phones), the page's own form on the right.
 */
export default function Template(props: TemplateProps<KcContext, I18n>) {
    const {
        displayInfo = false,
        displayMessage = true,
        displayRequiredFields = false,
        headerNode,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        bodyClassName,
        kcContext,
        i18n,
        doUseDefaultCss,
        classes,
        children
    } = props;

    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });
    const { msg, msgStr, currentLanguage, enabledLanguages } = i18n;
    const { auth, url, message, isAppInitiatedAction, pageId } = kcContext;

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", msgStr("clinicName"));
    }, [documentTitle, msgStr]);

    useSetClassName({ qualifiedName: "html", className: kcClsx("kcHtmlClass") });
    useSetClassName({ qualifiedName: "body", className: bodyClassName ?? kcClsx("kcBodyClass") });

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });
    if (!isReadyToRender) return null;

    const subtitle = subtitleKey(pageId);
    // app-initiated actions should not see warnings about the action they are already doing
    const alert = displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction) ? message : undefined;
    const showUsername = auth !== undefined && auth.showUsername && !auth.showResetCredentials;

    return (
        <div className="flex min-h-screen w-full items-center justify-center bg-gradient-to-r from-teal-500/40 via-white to-teal-500/50 p-4 font-sans sm:p-8">
            <div className="grid min-h-[520px] w-full max-w-4xl grid-cols-1 overflow-hidden rounded-2xl bg-white shadow-2xl md:grid-cols-2">
                <aside className="relative hidden flex-col justify-center overflow-hidden bg-gradient-to-br from-teal-400 to-teal-700 p-14 text-white md:flex">
                    <span aria-hidden className="absolute -bottom-20 -left-16 h-64 w-64 rounded-full bg-white/10" />
                    <span aria-hidden className="absolute top-[40%] -right-10 h-40 w-40 rounded-full bg-white/15" />
                    <span aria-hidden className="absolute top-8 right-16 h-24 w-24 rounded-full bg-white/10" />
                    <div className="relative z-10">
                        <p className="mb-3 text-4xl leading-none font-extrabold tracking-[4px]">{msg("panelTitle")}</p>
                        <p className="mb-6 font-semibold tracking-[2px] uppercase opacity-90">{msg("clinicName")}</p>
                        <p className="max-w-xs leading-relaxed opacity-85">{msg(panelTextKey(pageId))}</p>
                    </div>
                </aside>

                <main className="kc-content relative flex flex-col justify-center p-8 sm:p-14">
                    {/* the panel is hidden on phones, so the clinic's name goes here */}
                    <p className="mb-8 flex items-center gap-2 font-semibold text-teal-700 md:hidden">
                        <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-teal-600 text-white">
                            <Stethoscope className="h-4 w-4" />
                        </span>
                        {msg("clinicName")}
                    </p>
                    {enabledLanguages.length > 1 && (
                        <div className="absolute top-6 right-6 sm:top-5 sm:right-5">
                            <details className="relative">
                                <summary
                                    className="inline-flex cursor-pointer list-none items-center gap-1.5 rounded-md px-2 py-1 text-sm text-slate-600 hover:bg-slate-100"
                                    aria-label={msgStr("languages")}
                                >
                                    <Globe className="h-4 w-4" />
                                    {currentLanguage.label}
                                </summary>
                                <ul className="absolute right-0 z-20 mt-1 max-h-64 min-w-40 overflow-auto rounded-md border border-slate-200 bg-white py-1 shadow-lg">
                                    {enabledLanguages.map(({ languageTag, label, href }) => (
                                        <li key={languageTag}>
                                            <a
                                                href={href}
                                                className="block px-3 py-1.5 text-sm font-normal! text-slate-700! no-underline! hover:bg-slate-100"
                                            >
                                                {label}
                                            </a>
                                        </li>
                                    ))}
                                </ul>
                            </details>
                        </div>
                    )}

                    <h1 id="kc-page-title" className="mb-2 text-3xl font-bold text-slate-900">
                        {headerNode}
                    </h1>
                    {subtitle && <p className="mb-7 text-slate-600">{msg(subtitle)}</p>}
                    {!subtitle && <div className="mb-5" />}

                    {showUsername && (
                        <div id="kc-username" className="mb-5 flex items-center justify-between gap-2 rounded-md bg-slate-50 px-3 py-2 text-sm">
                            <span id="kc-attempted-username" className="truncate font-medium text-slate-900">
                                {auth.attemptedUsername}
                            </span>
                            <a
                                id="reset-login"
                                href={url.loginRestartFlowUrl}
                                className="inline-flex items-center gap-1 text-xs"
                                title={msgStr("restartLoginTooltip")}
                            >
                                <RotateCcw className="h-3.5 w-3.5" />
                                {msg("restartLoginTooltip")}
                            </a>
                        </div>
                    )}

                    {displayRequiredFields && (
                        <p className="-mt-3 mb-4 text-xs text-slate-500">
                            <span className="text-red-500">*</span> {msg("requiredFields")}
                        </p>
                    )}

                    {alert && (
                        <div role="alert" className={`mb-4 flex items-start gap-2 rounded-md border px-3 py-2 text-sm ${ALERT[alert.type].style}`}>
                            {(() => {
                                const { Icon } = ALERT[alert.type];
                                return <Icon className="mt-0.5 h-4 w-4 shrink-0" />;
                            })()}
                            <span dangerouslySetInnerHTML={{ __html: kcSanitize(alert.summary) }} />
                        </div>
                    )}

                    {children}

                    {auth !== undefined && auth.showTryAnotherWayLink && (
                        <form id="kc-select-try-another-way-form" action={url.loginAction} method="post" className="mt-4 text-center text-sm">
                            <input type="hidden" name="tryAnotherWay" value="on" />
                            <button type="submit" className="cursor-pointer font-medium text-teal-600 hover:text-teal-700 hover:underline">
                                {msg("doTryAnotherWay")}
                            </button>
                        </form>
                    )}

                    {socialProvidersNode}

                    {displayInfo && (
                        <div id="kc-info" className={kcClsx("kcSignUpClass")}>
                            <div id="kc-info-wrapper" className={kcClsx("kcInfoAreaWrapperClass")}>
                                {infoNode}
                            </div>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
}
