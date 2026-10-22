import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { KcPage } from "./kc.gen";

/**
 * In Keycloak, the page arrives with `window.kcContext`. With `npm run dev` there is none, so a
 * sample one stands in: http://localhost:5175/?page=register.ftl previews the sign-up page, and
 * `&error=1` adds a field error where the page has one.
 */
async function main() {
    if (import.meta.env.DEV && window.kcContext === undefined) {
        const { previewContext } = await import("./preview");
        window.kcContext = previewContext(new URLSearchParams(window.location.search));
    }

    createRoot(document.getElementById("root")!).render(
        <StrictMode>{window.kcContext ? <KcPage kcContext={window.kcContext} /> : <h1>No Keycloak context</h1>}</StrictMode>
    );
}

void main();
