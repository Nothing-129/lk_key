#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
BT="$SDK/build-tools/35.0.0"
SIGNING_KEYSTORE="${LK_KEYSTORE_PATH:-$HOME/.android/lk-key/debug.keystore}"
# Keep this app's signing identity outside disposable build output. Migrate an
# existing project key, but never silently generate a replacement key.
if [ ! -f "$SIGNING_KEYSTORE" ] && [ -f build/debug.keystore ]; then
 mkdir -p "$(dirname "$SIGNING_KEYSTORE")"
 cp build/debug.keystore "$SIGNING_KEYSTORE"
 chmod 600 "$SIGNING_KEYSTORE"
fi
if [ ! -f "$SIGNING_KEYSTORE" ]; then
 echo "找不到应用签名：$SIGNING_KEYSTORE。请复制原签名或设置 LK_KEYSTORE_PATH。" >&2
 exit 1
fi
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
(cd h5-car-key && npm run build:android)
"$BT/aapt2" compile --dir app/res -o build/resources.zip
"$BT/aapt2" link -I "$SDK/platforms/android-35/android.jar" --manifest app/AndroidManifest.xml -A app/assets --java build/generated -o build/unsigned.apk build/resources.zip
javac --release 8 -classpath "$SDK/platforms/android-35/android.jar" -d build/classes app/src/com/zcy/lkkey/*.java build/generated/com/zcy/lkkey/R.java
"$BT/d8" --lib "$SDK/platforms/android-35/android.jar" --min-api 31 --output build/dex build/classes/com/zcy/lkkey/*.class
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-pass pass:android --out build/lk-key.apk build/aligned.apk
"$BT/apksigner" verify build/lk-key.apk
