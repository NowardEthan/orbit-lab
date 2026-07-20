#!/usr/bin/env bash
# Build APK do Orbit (lab Compose) — sideload, sem Play Store.
# Uso:
#   ./scripts/build-apk.sh            → lab release
#   ./scripts/build-apk.sh --beta     → beta (com.luna.orbitmobile.beta)
#   ./scripts/build-apk.sh --stable   → stable (com.luna.orbitmobile)
#   ./scripts/build-apk.sh --debug    → variante debug
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VARIANT="Release"
FLAVOR="lab"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --beta) FLAVOR="beta"; shift ;;
    --stable) FLAVOR="stable"; shift ;;
    --lab) FLAVOR="lab"; shift ;;
    --debug) VARIANT="Debug"; shift ;;
    *) echo "Flag desconhecida: $1" >&2; exit 1 ;;
  esac
done

FLAVOR_CAP="$(tr '[:lower:]' '[:upper:]' <<< "${FLAVOR:0:1}")${FLAVOR:1}"
TASK="assemble${FLAVOR_CAP}${VARIANT}"
APK_DIR="app/build/outputs/apk/${FLAVOR}/$(echo "$VARIANT" | tr '[:upper:]' '[:lower:]')"
APK_NAME="app-${FLAVOR}-$(echo "$VARIANT" | tr '[:upper:]' '[:lower:]').apk"

echo "=== Orbit (Compose) — build APK ($FLAVOR $VARIANT) ==="
./gradlew --stop >/dev/null 2>&1 || true
./gradlew "$TASK" --no-daemon

BUILT="$ROOT/$APK_DIR/$APK_NAME"
if [[ ! -f "$BUILT" ]]; then
  echo "APK não encontrado em $BUILT" >&2
  exit 1
fi

VERSION="$(grep -E 'versionName\s*=' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
SUFFIX=""
[[ "$FLAVOR" == "beta" ]] && SUFFIX+="-beta"
[[ "$FLAVOR" == "lab" ]] && SUFFIX+="-lab"
[[ "$VARIANT" == "Debug" ]] && SUFFIX+="-debug"

mkdir -p "$ROOT/dist"
# Nome canónico para o fluxo de release (orbit-releases espera orbit.apk)
OUT_NAMED="$ROOT/dist/orbit-${VERSION}${SUFFIX}.apk"
OUT_CANON="$ROOT/dist/orbit.apk"
cp -f "$BUILT" "$OUT_NAMED"
cp -f "$BUILT" "$OUT_CANON"

SIZE_MB="$(du -m "$OUT_NAMED" | cut -f1)"
echo ""
echo "APK pronto: $OUT_NAMED (${SIZE_MB} MB)"
echo "Cópia canónica: $OUT_CANON"
echo "Próximo passo (quando for publicar): gh release upload … ./dist/orbit.apk"
