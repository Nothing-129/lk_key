#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/35.0.0"
mkdir -p build/device-test/classes build/device-test/dex
javac --release 8 -classpath "$SDK/platforms/android-35/android.jar" -d build/device-test/classes tests/device/com/zcy/lkkey/test/DeviceTest.java
"$BT/d8" --lib "$SDK/platforms/android-35/android.jar" --min-api 31 --output build/device-test/dex build/device-test/classes/com/zcy/lkkey/test/*.class
"$BT/aapt2" link -I "$SDK/platforms/android-35/android.jar" --manifest tests/device/AndroidManifest.xml -o build/device-test/unsigned.apk
(cd build/device-test/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/device-test/unsigned.apk build/device-test/aligned.apk
"$BT/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out build/device-test.apk build/device-test/aligned.apk
