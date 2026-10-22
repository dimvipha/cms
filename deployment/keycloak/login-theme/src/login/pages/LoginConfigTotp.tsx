import type { ReactNode } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { button, classes as kc, outlineButton, primaryButton } from "../classes";
import type { I18n } from "../i18n";
import type { KcContext } from "../KcContext";

/** Staff setting up two-factor sign-in: three numbered steps, then the code from their phone. */
export default function LoginConfigTotp(props: PageProps<Extract<KcContext, { pageId: "login-config-totp.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { url, isAppInitiatedAction, totp, mode, messagesPerField } = kcContext;
    const { msg, msgStr, advancedMsg } = i18n;

    const fieldError = (field: "totp" | "userLabel", id: string) =>
        messagesPerField.existsError(field) && (
            <span id={id} className={kc.kcInputErrorMessageClass} aria-live="polite" dangerouslySetInnerHTML={{ __html: kcSanitize(messagesPerField.get(field)) }} />
        );

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            headerNode={msg("loginTotpTitle")}
            displayMessage={!messagesPerField.existsError("totp", "userLabel")}
        >
            <ol id="kc-totp-settings" className="mb-6 grid list-none gap-4 p-0 text-sm text-slate-700">
                <Step number={1}>
                    <p>{msg("loginTotpStep1")}</p>
                    <ul id="kc-totp-supported-apps" className="mt-1 flex flex-wrap gap-1.5 p-0">
                        {totp.supportedApplications.map(app => (
                            <li key={app} className="list-none rounded-full bg-teal-50 px-2.5 py-0.5 text-xs font-medium text-teal-800">
                                {advancedMsg(app)}
                            </li>
                        ))}
                    </ul>
                </Step>

                {mode === "manual" ? (
                    <>
                        <Step number={2}>
                            <p>{msg("loginTotpManualStep2")}</p>
                            <p id="kc-totp-secret-key" className="my-2 rounded-md bg-slate-50 px-3 py-2 font-mono text-sm tracking-wider break-all text-slate-900">
                                {totp.totpSecretEncoded}
                            </p>
                            <a href={totp.qrUrl} id="mode-barcode">
                                {msg("loginTotpScanBarcode")}
                            </a>
                        </Step>
                        <Step number={3}>
                            <p>{msg("loginTotpManualStep3")}</p>
                            <ul className="mt-1 grid gap-0.5 p-0 text-xs text-slate-500">
                                <li id="kc-totp-type" className="list-none">
                                    {msg("loginTotpType")}: {msg(`loginTotp.${totp.policy.type}`)}
                                </li>
                                <li id="kc-totp-algorithm" className="list-none">
                                    {msg("loginTotpAlgorithm")}: {totp.policy.getAlgorithmKey()}
                                </li>
                                <li id="kc-totp-digits" className="list-none">
                                    {msg("loginTotpDigits")}: {totp.policy.digits}
                                </li>
                                {totp.policy.type === "totp" ? (
                                    <li id="kc-totp-period" className="list-none">
                                        {msg("loginTotpInterval")}: {totp.policy.period}
                                    </li>
                                ) : (
                                    <li id="kc-totp-counter" className="list-none">
                                        {msg("loginTotpCounter")}: {totp.policy.initialCounter}
                                    </li>
                                )}
                            </ul>
                        </Step>
                    </>
                ) : (
                    <Step number={2}>
                        <p>{msg("loginTotpStep2")}</p>
                        <img
                            id="kc-totp-secret-qr-code"
                            src={`data:image/png;base64, ${totp.totpSecretQrCode}`}
                            alt="QR code"
                            className="my-2 h-40 w-40 rounded-md border border-slate-200 p-1"
                        />
                        <a href={totp.manualUrl} id="mode-manual">
                            {msg("loginTotpUnableToScan")}
                        </a>
                    </Step>
                )}

                <Step number={mode === "manual" ? 4 : 3}>
                    <p>{msg("loginTotpStep3")}</p>
                    <p className="text-xs text-slate-500">{msg("loginTotpStep3DeviceName")}</p>
                </Step>
            </ol>

            <form action={url.loginAction} className={kc.kcFormClass} id="kc-totp-settings-form" method="post">
                <div className={kc.kcFormGroupClass}>
                    <label htmlFor="totp" className={kc.kcLabelClass}>
                        {msg("authenticatorCode")} <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        id="totp"
                        name="totp"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        className={`${kc.kcInputClass} font-mono tracking-[0.3em]`}
                        aria-invalid={messagesPerField.existsError("totp")}
                    />
                    {fieldError("totp", "input-error-otp-code")}
                    <input type="hidden" id="totpSecret" name="totpSecret" value={totp.totpSecret} />
                    {mode && <input type="hidden" id="mode" value={mode} />}
                </div>

                <div className={kc.kcFormGroupClass}>
                    <label htmlFor="userLabel" className={kc.kcLabelClass}>
                        {msg("loginTotpDeviceName")} {totp.otpCredentials.length >= 1 && <span className="text-red-500">*</span>}
                    </label>
                    <input
                        type="text"
                        id="userLabel"
                        name="userLabel"
                        autoComplete="off"
                        className={kc.kcInputClass}
                        aria-invalid={messagesPerField.existsError("userLabel")}
                    />
                    {fieldError("userLabel", "input-error-otp-label")}
                </div>

                <label className="flex items-center gap-2 text-sm text-slate-600">
                    <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" className="h-4 w-4 accent-teal-600" />
                    {msg("logoutOtherSessions")}
                </label>

                <div className="grid gap-2 pt-2">
                    <input type="submit" className={`${button} ${primaryButton} w-full`} id="saveTOTPBtn" value={msgStr("doSubmit")} />
                    {isAppInitiatedAction && (
                        <button type="submit" className={`${button} ${outlineButton} w-full`} id="cancelTOTPBtn" name="cancel-aia" value="true">
                            {msg("doCancel")}
                        </button>
                    )}
                </div>
            </form>
        </Template>
    );
}

function Step(props: { number: number; children: ReactNode }) {
    return (
        <li className="flex gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-teal-600 text-xs font-semibold text-white">
                {props.number}
            </span>
            <div className="min-w-0 flex-1">{props.children}</div>
        </li>
    );
}
