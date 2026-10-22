import type { PageProps } from "keycloakify/login/pages/PageProps";
import { button, outlineButton, primaryButton } from "../classes";
import type { I18n } from "../i18n";
import type { KcContext } from "../KcContext";

/** A sign-in left open too long: start again, or carry on where it stopped. */
export default function LoginPageExpired(props: PageProps<Extract<KcContext, { pageId: "login-page-expired.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { url } = kcContext;
    const { msg } = i18n;

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} headerNode={msg("pageExpiredTitle")}>
            <div className="grid gap-2">
                <a id="loginContinueLink" href={url.loginAction} className={`${button} ${primaryButton} w-full text-white! no-underline!`}>
                    {msg("pageExpiredContinue")}
                </a>
                <a id="loginRestartLink" href={url.loginRestartFlowUrl} className={`${button} ${outlineButton} w-full text-slate-700! no-underline!`}>
                    {msg("pageExpiredRestart")}
                </a>
            </div>
        </Template>
    );
}
