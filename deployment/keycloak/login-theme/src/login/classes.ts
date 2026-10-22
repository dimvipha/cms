import type { ClassKey } from "keycloakify/login";

/**
 * The look of Keycloak's pages, as Tailwind classes on the hooks Keycloakify gives each element.
 * Pages without a hand-made version (password reset, verify email, info, error, ...) are styled
 * entirely through these, so they match the sign-in page.
 */
const input =
    "block h-9 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-xs outline-none transition " +
    "placeholder:text-slate-400 focus:border-teal-600 focus:ring-3 focus:ring-teal-600/15 " +
    "disabled:cursor-not-allowed disabled:opacity-50 aria-[invalid=true]:border-red-400 aria-[invalid=true]:focus:ring-red-500/15";

export const button =
    "inline-flex h-9 cursor-pointer items-center justify-center gap-2 rounded-md px-4 text-sm font-medium transition-colors " +
    "outline-none focus-visible:ring-3 focus-visible:ring-teal-600/30 disabled:pointer-events-none disabled:opacity-50";

export const primaryButton = "bg-[#0f8a7b] text-white shadow-xs hover:bg-teal-700";

export const outlineButton = "border border-slate-200 bg-white text-slate-700 shadow-xs hover:bg-slate-50 hover:text-slate-900";

export const classes = {
    kcHtmlClass: "",
    kcBodyClass: "bg-white text-slate-900 antialiased",

    kcFormClass: "grid gap-4",
    kcFormGroupClass: "grid gap-1.5",
    kcFormGroupHeader: "pt-2 text-sm font-semibold text-slate-900",
    kcFormSettingClass: "",
    kcContentWrapperClass: "",
    kcLabelWrapperClass: "flex items-center gap-1 text-sm",
    kcLabelClass: "text-sm font-medium text-slate-900",
    kcInputWrapperClass: "grid gap-1.5",
    kcInputClass: input,
    kcTextareaClass: `${input} h-auto min-h-20 py-2`,
    kcInputErrorMessageClass: "block text-xs text-red-600",
    kcInputHelperTextBeforeClass: "text-xs text-slate-500",
    kcInputHelperTextAfterClass: "text-xs text-slate-500",
    kcInputGroup: "relative [&>input]:pr-10",

    kcFormPasswordVisibilityButtonClass:
        "absolute right-1.5 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 cursor-pointer items-center justify-center rounded text-slate-500 hover:bg-slate-100 hover:text-slate-900",
    kcFormPasswordVisibilityIconShow: "kc-icon kc-icon-eye",
    kcFormPasswordVisibilityIconHide: "kc-icon kc-icon-eye-off",

    kcCheckboxInputClass: "h-4 w-4 accent-teal-600",
    kcCheckInputClass: "h-4 w-4 accent-teal-600",
    kcInputClassCheckbox: "flex items-center gap-2",
    kcInputClassCheckboxInput: "h-4 w-4 accent-teal-600",
    kcInputClassCheckboxLabel: "text-sm text-slate-700",
    kcInputClassRadio: "flex items-center gap-2",
    kcInputClassRadioInput: "h-4 w-4 accent-teal-600",
    kcInputClassRadioLabel: "text-sm text-slate-700",
    kcInputClassRadioCheckboxLabelDisabled: "opacity-50",

    kcFormOptionsClass: "text-sm",
    kcFormOptionsWrapperClass: "text-sm text-slate-600",
    kcFormButtonsClass: "grid gap-2 pt-2",
    kcFormButtonsWrapperClass: "grid gap-2",
    kcButtonClass: button,
    kcButtonPrimaryClass: primaryButton,
    kcButtonDefaultClass: outlineButton,
    kcButtonBlockClass: "w-full",
    kcButtonLargeClass: "",

    kcFormSocialAccountSectionClass: "mt-6",
    kcFormSocialAccountListClass: "m-0 flex list-none flex-col gap-2 p-0",
    kcFormSocialAccountListGridClass: "grid grid-cols-2",
    kcFormSocialAccountListButtonClass: `${button} ${outlineButton} w-full no-underline!`,
    kcFormSocialAccountGridItem: "",
    kcFormSocialAccountNameClass: "",
    kcCommonLogoIdP: "hidden",

    kcSignUpClass: "mt-6 text-center text-sm text-slate-500",
    kcInfoAreaWrapperClass: "",

    kcLoginOTPListClass: "grid gap-2",
    kcLoginOTPListInputClass: "peer sr-only",
    kcLoginOTPListItemHeaderClass:
        "flex cursor-pointer items-center gap-3 rounded-md border border-slate-200 px-3 py-2 peer-checked:border-teal-600 peer-checked:bg-teal-50",
    kcLoginOTPListItemIconBodyClass: "text-teal-700",
    kcLoginOTPListItemIconClass: "kc-icon kc-icon-phone",
    kcLoginOTPListItemTitleClass: "text-sm font-medium text-slate-900",

    kcSelectAuthListClass: "grid gap-2",
    kcSelectAuthListItemClass:
        "flex items-center gap-3 rounded-md border border-slate-200 px-3 py-3 text-left hover:border-teal-600 hover:bg-teal-50",
    kcSelectAuthListItemIconClass: "text-teal-700",
    kcSelectAuthListItemIconPropertyClass: "",
    kcSelectAuthListItemBodyClass: "flex-1",
    kcSelectAuthListItemHeadingClass: "text-sm font-medium text-slate-900",
    kcSelectAuthListItemDescriptionClass: "text-xs text-slate-500",
    kcSelectAuthListItemFillClass: "flex-1",
    kcSelectAuthListItemArrowClass: "text-slate-400",
    kcSelectAuthListItemArrowIconClass: "kc-icon kc-icon-chevron",
    kcSelectAuthListItemTitle: "text-sm font-medium",

    kcRecoveryCodesWarning: "rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800",
    kcRecoveryCodesList: "grid grid-cols-2 gap-1 rounded-md bg-slate-50 p-3 font-mono text-sm",
    kcRecoveryCodesActions: "flex gap-2",
    kcRecoveryCodesConfirmation: "flex items-center gap-2 text-sm"
} satisfies { [key in ClassKey]?: string };
