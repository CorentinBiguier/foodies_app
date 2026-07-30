#!/usr/bin/env bash
# Scaffold a new microservice from services/_template.
#
# Usage: scaffold-service.sh <service-name>
#   <service-name> : kebab-case, e.g. user-service, recipe-service, inventory-service
#
# What it does (mechanical, deterministic part of new-microservice skill):
#   - copies services/_template -> services/<service-name>
#   - renames the Java package com.foodies.template -> com.foodies.<domain>
#   - renames TemplateApplication -> <Domain>Application
#   - renames the Example* demo classes -> <Domain>* (Entity, Repository, Dto,
#     Service, ServiceImpl, Controller, and their tests)
#   - updates pom.xml (artifactId/name) and application.yml (spring.application.name)
#   - appends a new service block to the root docker-compose.yml on the shared
#     foodies-network, with the next free host port
#   - generates a starter README.md for the service
#
# What it deliberately does NOT do (left to judgment, see SKILL.md):
#   - fix irregular plurals in the REST path (e.g. inventory -> inventories)
#   - add domain-specific Maven dependencies (security, etc.)
#   - write real business logic / entity fields beyond the generic Example shape
#   - run/verify the build (SKILL.md covers that as a follow-up step)

set -euo pipefail

SERVICE_NAME="${1:-}"

if [[ -z "$SERVICE_NAME" ]]; then
  echo "Usage: scaffold-service.sh <service-name>  (kebab-case, e.g. user-service)" >&2
  exit 1
fi

if ! [[ "$SERVICE_NAME" =~ ^[a-z][a-z0-9]*(-[a-z0-9]+)*$ ]]; then
  echo "Error: '$SERVICE_NAME' must be kebab-case (lowercase letters, digits, dashes)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
TEMPLATE_DIR="$REPO_ROOT/services/_template"
TARGET_DIR="$REPO_ROOT/services/$SERVICE_NAME"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"

if [[ ! -d "$TEMPLATE_DIR" ]]; then
  echo "Error: template not found at $TEMPLATE_DIR" >&2
  exit 1
fi

if [[ -e "$TARGET_DIR" ]]; then
  echo "Error: $TARGET_DIR already exists." >&2
  exit 1
fi

# Derive naming variants from the service name, stripping a trailing "-service".
DOMAIN_RAW="${SERVICE_NAME%-service}"

DOMAIN_PASCAL=""
IFS='-' read -ra PARTS <<< "$DOMAIN_RAW"
for part in "${PARTS[@]}"; do
  first_upper="$(printf '%s' "${part:0:1}" | tr '[:lower:]' '[:upper:]')"
  DOMAIN_PASCAL="${DOMAIN_PASCAL}${first_upper}${part:1}"
done
PACKAGE_SEGMENT="$(printf '%s' "$DOMAIN_RAW" | tr -d '-' | tr '[:upper:]' '[:lower:]')"
DOMAIN_CAMEL="$(printf '%s' "${DOMAIN_PASCAL:0:1}" | tr '[:upper:]' '[:lower:]')${DOMAIN_PASCAL:1}"

echo "==> Scaffolding '$SERVICE_NAME' (domain: $DOMAIN_PASCAL, package: com.foodies.$PACKAGE_SEGMENT)"

# 1. Copy the template, excluding any local build artifacts.
cp -r "$TEMPLATE_DIR" "$TARGET_DIR"
find "$TARGET_DIR" -type d -name target -prune -exec rm -rf {} +

# 2. Move the Java package directories.
for src_set in main test; do
  OLD_PKG_DIR="$TARGET_DIR/src/$src_set/java/com/foodies/template"
  NEW_PKG_DIR="$TARGET_DIR/src/$src_set/java/com/foodies/$PACKAGE_SEGMENT"
  if [[ -d "$OLD_PKG_DIR" ]]; then
    mv "$OLD_PKG_DIR" "$NEW_PKG_DIR"
  fi
done

# 3. Rename Template*/Example* files to the new domain name.
find "$TARGET_DIR" -type f -name '*.java' | while read -r f; do
  base="$(basename "$f")"
  newbase="$(printf '%s' "$base" | sed -e "s/Template/${DOMAIN_PASCAL}/g" -e "s/Example/${DOMAIN_PASCAL}/g")"
  if [[ "$base" != "$newbase" ]]; then
    mv "$f" "$(dirname "$f")/$newbase"
  fi
done

# 4. Rewrite file contents: literal artifact name first, then class/package words.
find "$TARGET_DIR" -type f \( -name '*.java' -o -name '*.xml' -o -name '*.yml' -o -name '*.yaml' \) -print0 |
  while IFS= read -r -d '' f; do
    sed -i \
      -e "s/template-service/${SERVICE_NAME}/g" \
      -e "s/Template/${DOMAIN_PASCAL}/g" \
      -e "s/Example/${DOMAIN_PASCAL}/g" \
      -e "s/template/${PACKAGE_SEGMENT}/g" \
      -e "s/example/${DOMAIN_CAMEL}/g" \
      "$f"
  done

# 5. Pick the next free host port from docker-compose.yml (default 8080 base).
EXISTING_PORTS="$(grep -oE '"[0-9]+:[0-9]+"' "$COMPOSE_FILE" | grep -oE '^"[0-9]+' | tr -d '"' || true)"
MAX_PORT=8080
for p in $EXISTING_PORTS; do
  if (( p > MAX_PORT )); then
    MAX_PORT=$p
  fi
done
NEW_PORT=$((MAX_PORT + 1))

# 6. Append the new service to docker-compose.yml, on the shared network.
cat >> "$COMPOSE_FILE" <<EOF

  ${SERVICE_NAME}:
    build:
      context: ./services/${SERVICE_NAME}
    container_name: ${SERVICE_NAME}
    ports:
      - "${NEW_PORT}:8080"
    networks:
      - foodies-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 20s
EOF

# 7. Starter README for the service.
PLURAL_GUESS="${DOMAIN_CAMEL}s"
cat > "$TARGET_DIR/README.md" <<EOF
# ${SERVICE_NAME}

Port hôte (docker-compose) : **${NEW_PORT}** (conteneur écoute en interne sur 8080).

## Endpoints

> Générés depuis le template, à vérifier/compléter au fur et à mesure de
> l'implémentation réelle du domaine ${DOMAIN_PASCAL}.

- \`GET /api/${PLURAL_GUESS}\`
- \`GET /api/${PLURAL_GUESS}/{id}\`
- \`POST /api/${PLURAL_GUESS}\`
- \`GET /actuator/health\`

## Lancer localement

\`\`\`bash
docker compose build ${SERVICE_NAME}
docker compose up -d ${SERVICE_NAME}
\`\`\`
EOF

echo "==> Done. Created services/${SERVICE_NAME} on port ${NEW_PORT}."
echo "==> Manual follow-up needed (see SKILL.md):"
echo "    - check REST path plural '${PLURAL_GUESS}' is correct French/English pluralization"
echo "    - add any service-specific Maven dependencies to pom.xml"
echo "    - flesh out the ${DOMAIN_PASCAL} entity/DTO fields and real business logic"
echo "    - rebuild and run tests before committing"
