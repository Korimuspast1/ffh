#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
PROPERTIES_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [[ ! -f "$PROPERTIES_FILE" ]]; then
  echo "Gradle wrapper properties were not found: $PROPERTIES_FILE" >&2
  exit 1
fi

raw_url="$(grep '^distributionUrl=' "$PROPERTIES_FILE" | cut -d'=' -f2-)"
distribution_url="${raw_url//\\:/:}"
archive_name="$(basename "$distribution_url")"
gradle_version="$(printf '%s' "$archive_name" | sed -E 's/^gradle-([0-9][0-9.]*[0-9])-bin\.zip$/\1/')"

if [[ -z "$gradle_version" || "$gradle_version" == "$archive_name" ]]; then
  echo "Unable to parse Gradle version from $archive_name" >&2
  exit 1
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$APP_HOME/.gradle}"
install_root="$GRADLE_USER_HOME/wrapper/dists/gradle-$gradle_version-bin"
gradle_home="$install_root/gradle-$gradle_version"
gradle_bin="$gradle_home/bin/gradle"

if [[ ! -x "$gradle_bin" ]]; then
  mkdir -p "$install_root"
  zip_path="$install_root/$archive_name"
  if [[ ! -f "$zip_path" ]]; then
    echo "Downloading Gradle $gradle_version..." >&2
    if command -v curl >/dev/null 2>&1; then
      curl -fL "$distribution_url" -o "$zip_path"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$zip_path" "$distribution_url"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$zip_path" -d "$install_root"
  else
    echo "unzip is required to extract Gradle." >&2
    exit 1
  fi
fi

exec "$gradle_bin" "$@"
