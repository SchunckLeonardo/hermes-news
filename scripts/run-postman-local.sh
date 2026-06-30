#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COLLECTION="$ROOT_DIR/postman/hermes-news.postman_collection.json"
ENVIRONMENT="$ROOT_DIR/postman/hermes-news.local.postman_environment.json"

if ! command -v newman >/dev/null 2>&1; then
  echo "Newman is not installed. This script will not install dependencies automatically."
  echo "Import these files into Postman and run the collection manually:"
  echo "  $COLLECTION"
  echo "  $ENVIRONMENT"
  exit 127
fi

newman run "$COLLECTION" -e "$ENVIRONMENT"
