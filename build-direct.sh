#!/usr/bin/env bash
# Build without Gradle downloads. Requires installed JDK 17 and Android SDK 35.
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?Set ANDROID_HOME to Android SDK}"
export PATH="$JAVA_HOME/bin:$PATH"
bt="$ANDROID_HOME/build-tools/35.0.0"
android_jar="$ANDROID_HOME/platforms/android-35/android.jar"
out="$PWD/build/direct"
mkdir -p "$out/classes" "$out/dex" "$out/generated"
find "$out/classes" "$out/dex" "$out/generated" -mindepth 1 -delete
find "$out" -maxdepth 1 -type f \( -name '*.apk' -o -name '*.jar' -o -name '*.zip' -o -name 'AndroidManifest.xml' -o -name 'sources.txt' \) -delete
python3 - "$out/AndroidManifest.xml" <<'PY'
import sys,xml.etree.ElementTree as E
E.register_namespace('android','http://schemas.android.com/apk/res/android')
a='{http://schemas.android.com/apk/res/android}'
r=E.parse('app/src/main/AndroidManifest.xml').getroot()
r.set('package','dk.femto.albumframe');r.set(a+'versionCode','10');r.set(a+'versionName','0.7.1')
E.SubElement(r,'uses-sdk',{a+'minSdkVersion':'28',a+'targetSdkVersion':'35'})
for e in r.iter():
 n=e.get(a+'name','')
 if n.startswith('.'):e.set(a+'name','dk.femto.albumframe'+n)
E.ElementTree(r).write(sys.argv[1],encoding='utf-8',xml_declaration=True)
PY
"$bt/aapt2" compile --dir app/src/main/res -o "$out/resources.zip"
"$bt/aapt2" link -I "$android_jar" --manifest "$out/AndroidManifest.xml" -A app/src/main/assets --java "$out/generated" -o "$out/resources.apk" "$out/resources.zip"
find app/src/main/java "$out/generated" -name '*.java' > "$out/sources.txt"
javac -encoding UTF-8 -source 17 -target 17 -classpath "$android_jar" -d "$out/classes" @"$out/sources.txt"
jar --create --file "$out/classes.jar" -C "$out/classes" .
"$bt/d8" --release --min-api 28 --lib "$android_jar" --output "$out/dex" "$out/classes.jar"
python3 - "$out" <<'PY'
from pathlib import Path
import sys,shutil,zipfile
p=Path(sys.argv[1]);shutil.copyfile(p/'resources.apk',p/'unaligned.apk')
with zipfile.ZipFile(p/'unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in sorted((p/'dex').glob('*.dex')):z.write(f,f.name)
PY
"$bt/zipalign" -f -p 4 "$out/unaligned.apk" "$out/AlbumFrame-TV-unsigned.apk"
echo "Unsigned APK: $out/AlbumFrame-TV-unsigned.apk"
