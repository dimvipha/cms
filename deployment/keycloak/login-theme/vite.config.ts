import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";
import { defineConfig } from "vite";

export default defineConfig({
    plugins: [
        react(),
        tailwindcss(),
        keycloakify({
            themeName: "spring-clinic",
            accountThemeImplementation: "none",
            // Keycloak 26: one jar, copied into ../providers by `npm run build-keycloak-theme`
            keycloakVersionTargets: { "22-to-25": false, "all-other-versions": "spring-clinic-theme.jar" }
        })
    ],
    server: { port: 5175 }
});
