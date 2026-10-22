import type { KcContext } from "./login/KcContext";
import { getKcContextMock } from "./login/KcPageStory";

/** Development only: a sample Keycloak context for the page named in the address. */
export function previewContext(params: URLSearchParams): KcContext {
    const pageId = (params.get("page") ?? "login.ftl") as KcContext["pageId"];
    const error = params.get("error") === "1";
    const notice = params.get("message") as "success" | "info" | "warning" | "error" | null;

    const failing = (...fields: string[]) => error && fields.some(field => ["username", "password", "email", "totp"].includes(field));
    const fieldError = "Invalid username or password.";

    return getKcContextMock({
        pageId: pageId as never,
        overrides: {
            ...(notice ? { message: { type: notice, summary: sampleMessage[notice] } } : {}),
            ...(error
                ? {
                      messagesPerField: {
                          printIfExists: <T extends string>(field: string, text: T) => (failing(field) ? text : undefined),
                          existsError: (...fields: string[]) => failing(...fields),
                          exists: (field: string) => failing(field),
                          get: (field: string) => (failing(field) ? fieldError : ""),
                          getFirstError: (...fields: string[]) => (failing(...fields) ? fieldError : "")
                      }
                  }
                : {}),
            ...(pageId === "register.ftl"
                ? {
                      profile: {
                          attributesByName: {
                              phoneNumber: {
                                  name: "phoneNumber",
                                  displayName: "Phone number",
                                  required: true,
                                  readOnly: false,
                                  validators: {},
                                  annotations: {},
                                  multivalued: false
                              }
                          }
                      }
                  }
                : {})
        } as never
    }) as KcContext;
}

const sampleMessage = {
    success: "Your account has been updated.",
    info: "You need to change your password to activate your account.",
    warning: "You need to verify your email address to activate your account.",
    error: "Invalid username or password."
};
