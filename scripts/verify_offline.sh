#!/usr/bin/env bash
set -euo pipefail

echo "=========================================================="
echo " CITY7Z7 Strict Offline & Security Verification Script"
echo "=========================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

echo "1. Verifying AndroidManifest.xml permissions whitelist..."
MANIFEST="app/src/main/AndroidManifest.xml"

FORBIDDEN_PERMS=(
    "android.permission.INTERNET"
    "android.permission.ACCESS_NETWORK_STATE"
    "android.permission.ACCESS_WIFI_STATE"
    "android.permission.CAMERA"
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.CALL_PHONE"
)

for perm in "${FORBIDDEN_PERMS[@]}"; do
    if grep -q "$perm" "$MANIFEST"; then
        echo "❌ VIOLATION: Forbidden permission '$perm' found in $MANIFEST!"
        exit 1
    fi
done
echo "✅ Manifest contains NO forbidden network or privacy permissions."

echo "2. Checking app/build.gradle.kts for forbidden network / telemetry dependencies..."
GRADLE_FILE="app/build.gradle.kts"

FORBIDDEN_LIBS=(
    "retrofit"
    "okhttp"
    "firebase"
    "crashlytics"
    "play-services-analytics"
    "play-services-ads"
)

for lib in "${FORBIDDEN_LIBS[@]}"; do
    if grep -E "^[[:space:]]*implementation\(.*${lib}" "$GRADLE_FILE"; then
        echo "❌ VIOLATION: Active dependency containing '$lib' found in $GRADLE_FILE!"
        exit 1
    fi
done
echo "✅ No active network/telemetry dependencies in app/build.gradle.kts."

echo "3. Verifying backup security rules..."
if ! grep -q 'android:allowBackup="false"' "$MANIFEST"; then
    echo "❌ VIOLATION: android:allowBackup must be false in $MANIFEST!"
    exit 1
fi

if ! grep -q '<exclude domain="root" path="."' "app/src/main/res/xml/data_extraction_rules.xml"; then
    echo "❌ VIOLATION: data_extraction_rules.xml must exclude root!"
    exit 1
fi
echo "✅ Complete backup exclusion verified."

echo "=========================================================="
echo "🎉 ALL OFFLINE & SECURITY CHECKS PASSED SUCCESSFULLY!"
echo "=========================================================="
