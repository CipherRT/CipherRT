#!/usr/bin/env bash
# Creates the signing key for release builds and the keystore.properties file that
# points to it. Run once, from the GymPlanner folder:   ./scripts/make-release-key.sh
#
# KEEP THE .jks FILE AND PASSWORDS SAFE (password manager + a cloud backup).
# If you lose them, friends can't install updates — they'd have to uninstall first.
set -euo pipefail

KEYSTORE="${1:-$HOME/gymplanner-release.jks}"
ALIAS="gymplanner"

if [ -e "$KEYSTORE" ]; then
  echo "A keystore already exists at $KEYSTORE — not overwriting it." >&2
  exit 1
fi

read -rsp "Choose a keystore password (6+ characters): " PASS; echo
read -rsp "Repeat it: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "Passwords don't match." >&2; exit 1; }

keytool -genkeypair -v \
  -keystore "$KEYSTORE" -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=Gym Planner"

cat > keystore.properties <<PROPS
storeFile=$KEYSTORE
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
PROPS

echo
echo "Keystore created:   $KEYSTORE"
echo "Settings written:   $(pwd)/keystore.properties   (git-ignored, never commit it)"
echo "Now build with:     ./gradlew assembleRelease"
echo "For GitHub Actions: base64 -w0 \"$KEYSTORE\"   → paste into the KEYSTORE_BASE64 secret"
