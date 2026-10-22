import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { button, primaryButton } from "../classes";
import type { I18n } from "../i18n";
import type { KcContext } from "../KcContext";

/**
 * Keycloak's notices, e.g. after an email link: "Your account has been updated", or the steps an
 * invite asks for. The way on is a button.
 */
export default function Info(props: PageProps<Extract<KcContext, { pageId: "info.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { advancedMsgStr, msg } = i18n;
    const { messageHeader, message, requiredActions, skipLink, pageRedirectUri, actionUri, client } = kcContext;

    const actions = requiredActions?.map(action => advancedMsgStr(`requiredAction.${action}`));
    const summary = kcSanitize(message.summary?.trim() ?? "");
    // without a header of its own, the message is the title, so it is not repeated below
    const title = (messageHeader && kcSanitize(advancedMsgStr(messageHeader)).trim()) || summary;
    const body = title !== summary || actions ? summary : undefined;
    const next = skipLink
        ? undefined
        : pageRedirectUri
          ? { href: pageRedirectUri, label: msg("backToApplication") }
          : actionUri
            ? { href: actionUri, label: msg("proceedWithAction") }
            : client.baseUrl
              ? { href: client.baseUrl, label: msg("backToApplication") }
              : undefined;

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={false}
            headerNode={<span dangerouslySetInnerHTML={{ __html: title }} />}
        >
            <div id="kc-info-message" className="grid gap-4">
                {body && <p className="instruction" dangerouslySetInnerHTML={{ __html: body }} />}
                {actions && (
                    <ul className="m-0 grid list-none gap-1.5 p-0">
                        {actions.map(action => (
                            <li key={action} className="flex items-center gap-2 text-sm text-slate-700">
                                <span className="h-1.5 w-1.5 rounded-full bg-teal-600" />
                                {action}
                            </li>
                        ))}
                    </ul>
                )}
                {next && (
                    <a href={next.href} className={`${button} ${primaryButton} mt-2 w-full text-white! no-underline!`}>
                        {next.label}
                    </a>
                )}
            </div>
        </Template>
    );
}
