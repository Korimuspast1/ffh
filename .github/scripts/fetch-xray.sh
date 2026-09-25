#!/usr/bin/env bash
# Downloads the official Xray-core Android builds and packages them as native
# libraries so that the app can execute the core as a child process.
#
# The Xray Android archive ships a PIE executable named `xray`. Android refuses
# to execute binaries that live inside the writable app data directory, but it
# does extract native libraries from the APK into an executable location, so the
# binary is shipped as `libxray.so` inside jniLibs/<abi>/.
set -euo pipefail

XRAY_VERSION="${1:-${XRAY_VERSION:-v26.3.27}}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JNILIBS="$ROOT_DIR/app/src/main/jniLibs"
ASSETS="$ROOT_DIR/app/src/main/assets"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# ABI directory -> suffix used by the Xray release assets
XRAY_ABIS=(
  "arm64-v8a:arm64-v8a"
  "x86_64:amd64"
)

echo "==> Xray-core $XRAY_VERSION"
mkdir -p "$JNILIBS" "$ASSETS"

for pair in "${XRAY_ABIS[@]}"; do
  abi="${pair%%:*}"
  asset_abi="${pair##*:}"
  asset="Xray-android-${asset_abi}.zip"
  url="https://github.com/XTLS/Xray-core/releases/download/${XRAY_VERSION}/${asset}"
  echo "    downloading $asset"
  if ! curl -fsSL --retry 3 --connect-timeout 20 -o "$WORK/$asset" "$url"; then
    echo "    !! $asset is not available for $XRAY_VERSION, skipping"
    continue
  fi
  rm -rf "$WORK/$abi"
  mkdir -p "$WORK/$abi"
  unzip -q -o "$WORK/$asset" -d "$WORK/$abi"

  if [ ! -f "$WORK/$abi/xray" ]; then
    echo "    !! xray binary missing inside $asset"
    continue
  fi

  mkdir -p "$JNILIBS/$abi"
  cp "$WORK/$abi/xray" "$JNILIBS/$abi/libxray.so"
  chmod 755 "$JNILIBS/$abi/libxray.so"
  echo "    $abi <- xray $(du -h "$JNILIBS/$abi/libxray.so" | cut -f1)"

  # Geo databases are shipped once (they are ABI independent)
  for dat in geoip.dat geosite.dat; do
    if [ -f "$WORK/$abi/$dat" ]; then
      cp "$WORK/$abi/$dat" "$ASSETS/$dat"
    fi
  done
done

if [ ! -d "$JNILIBS/arm64-v8a" ]; then
  echo "!! No Xray binary could be fetched, aborting" >&2
  exit 1
fi

echo "==> Xray core prepared:"
find "$JNILIBS" -type f -printf '    %p (%s bytes)\n'
ls -la "$ASSETS" 2>/dev/null | sed 's/^/    /'
