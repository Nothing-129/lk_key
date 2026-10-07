#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/35.0.0"
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
"$BT/aapt2" compile --dir app/res -o build/resources.zip
"$BT/aapt2" link -I "$SDK/platforms/android-35/android.jar" --manifest app/AndroidManifest.xml --java build/generated -o build/unsigned.apk build/resources.zip
javac --release 8 -classpath "$SDK/platforms/android-35/android.jar" -d build/classes app/src/com/zcy/lkkey/*.java build/generated/com/zcy/lkkey/R.java
"$BT/d8" --lib "$SDK/platforms/android-35/android.jar" --min-api 31 --output build/dex build/classes/com/zcy/lkkey/*.class
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/debug.keystore ]; then
 keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug' -keyalg RSA -validity 10000 >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out build/lk-key.apk build/aligned.apk
"$BT/apksigner" verify build/lk-key.apk
