#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

load_env_file() {
  local env_file="$ROOT_DIR/.env"
  local key
  local value

  if [[ ! -f "$env_file" ]]; then
    return
  fi

  while IFS='=' read -r key value; do
    [[ -z "${key:-}" || "$key" == \#* ]] && continue
    case "$key" in
      EVOLUTION_BASE_URL|EVOLUTION_API_KEY|EVOLUTION_INSTANCE|EVOLUTION_WEBHOOK_URL)
        value="${value%$'\r'}"
        value="${value%\"}"
        value="${value#\"}"
        value="${value%\'}"
        value="${value#\'}"
        if [[ -z "${!key:-}" ]]; then
          export "$key=$value"
        fi
        ;;
    esac
  done < "$env_file"
}

load_env_file

EVOLUTION_BASE_URL="${EVOLUTION_BASE_URL:-http://localhost:8081}"
EVOLUTION_API_KEY="${EVOLUTION_API_KEY:-change-me-local-only}"
EVOLUTION_INSTANCE="${EVOLUTION_INSTANCE:-hermes-local}"
EVOLUTION_WEBHOOK_URL="${EVOLUTION_WEBHOOK_URL:-http://app:8080/api/whatsapp/webhook}"

payload="$(printf '{"webhook":{"enabled":true,"url":"%s","byEvents":false,"base64":false,"events":["MESSAGES_UPSERT"]}}' "$EVOLUTION_WEBHOOK_URL")"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to configure the Evolution webhook."
  exit 127
fi

curl --fail --silent --show-error \
  --request POST \
  --url "$EVOLUTION_BASE_URL/webhook/set/$EVOLUTION_INSTANCE" \
  --header "Content-Type: application/json" \
  --header "apikey: $EVOLUTION_API_KEY" \
  --data "$payload"

echo
echo "Evolution webhook configured for instance '$EVOLUTION_INSTANCE' -> $EVOLUTION_WEBHOOK_URL"
