#!/usr/bin/env bash
# Packs the clinic email theme (clinic-theme/) into providers/clinic-theme.jar,
# which docker-compose mounts into Keycloak. Keycloak reads provider jars at
# startup, so restart it after rebuilding:
#
#   ./build_theme.sh
#   docker restart keycloak-clinic-db

set -euo pipefail
cd "$(dirname "$0")"

rm -f providers/clinic-theme.jar
(cd clinic-theme && zip -qr ../providers/clinic-theme.jar META-INF theme)
echo "built providers/clinic-theme.jar — restart Keycloak to load it"
