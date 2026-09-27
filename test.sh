#!/usr/bin/env bash
# Repeatable JVM checks. Android's stubs compile platform references; these tests
# exercise pure logic, fake Flickr replies and the loopback-only pairing server.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to Android SDK (platform 35)}"
: "${JSON_JAR:?Set JSON_JAR to org.json 20240303 jar}"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
android_jar="$ANDROID_HOME/platforms/android-35/android.jar"
out="$PWD/build/tests"
mkdir -p "$out"
source_dir=app/src/main/java/dk/femto/albumframe
sources=()
for name in OAuth NetworkFailure SmallQr LocalTls PairingServer CredentialStore FlickrApi PhotoOrder SlideshowClock UiText; do
    sources+=("$source_dir/$name.java")
done
javac -encoding UTF-8 -source 17 -target 17 -cp "$JSON_JAR:$android_jar" -d "$out" "${sources[@]}" tests/*.java
# Keep generated QR fixtures in the ignored build directory.
cd "$out"
for test in CoreTest FlickrTest PhotoOrderTest SlideshowClockTest LanguageTest RecoveryTest; do
    java -Djava.awt.headless=true -cp "$out:$JSON_JAR:$android_jar" "dk.femto.albumframe.$test"
done
