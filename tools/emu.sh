#!/usr/bin/env bash
# Build, install and look at HullBeat on the attached emulator.
#
# Every UI change this project makes is checked on the emulator, and every
# check used to retype the same dozen commands - with the same traps each
# time: Gradle's loopback socket in the short-name TEMP, Git Bash rewriting
# /data/... paths, a "\r" on every line adb prints, and the theme switched
# for a screenshot and never switched back.
#
#   tools/emu.sh build                 assembleDebug
#   tools/emu.sh install               install -r (keeps data) and restart
#   tools/emu.sh run                   build + install
#   tools/emu.sh theme                 print the stored scheme
#   tools/emu.sh theme NIGHT           store a scheme and restart the app
#   tools/emu.sh shot NAME             screenshot to build/emu/NAME.png
#   tools/emu.sh tap X Y               tap at device pixels
#   tools/emu.sh tapt "Text"           tap the centre of the first view with that text
#   tools/emu.sh find "Text"           print the bounds of views containing that text
#   tools/emu.sh back                  press Back
#   tools/emu.sh db DIR                copy .db, -wal and -shm into DIR
#   tools/emu.sh crash                 last lines of the crash buffer
#
# The emulator must already be running: starting it from here shuts down
# within seconds.
set -euo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ADB:-C:/Users/LaktionovVolodymyr/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
PKG=app.hullbeat
PREFS=shared_prefs/hullbeat_prefs.xml
SHOTS="$ROOT/build/emu"

adb() { "$ADB" "$@"; }
need_device() {
    adb get-state >/dev/null 2>&1 || { echo "no emulator attached - start it from Android Studio" >&2; exit 1; }
}
gradle() {
    export JAVA_HOME="${JAVA_HOME_HULLBEAT:-C:/Users/LaktionovVolodymyr/.jdks/jbr-21.0.11}"
    export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/Users/LaktionovVolodymyr/.gradle/udtmp"
    (cd "$ROOT" && ./gradlew "$@")
}
restart() {
    adb shell am start -S -n "$PKG/.MainActivity" >/dev/null
    sleep 4
}
current_theme() {
    adb shell run-as "$PKG" cat "$PREFS" | tr -d '\r' \
        | sed -n 's|.*<string name="theme_scheme">\([A-Z]*\)</string>.*|\1|p'
}
# Prints "x y" for the centre of the first node whose text contains $1.
centre_of() {
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null
    adb shell cat /sdcard/ui.xml | tr -d '\r' | grep -o "<node [^>]*>" \
        | grep -F "text=\"$1" | head -1 \
        | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' \
        | tr -c '0-9\n' ' ' | awk '{ print int(($1+$3)/2), int(($2+$4)/2) }'
}

cmd="${1:-}"; shift || true
case "$cmd" in
    build)
        gradle :app:assembleDebug -q
        ls -l "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    install)
        need_device
        adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" >/dev/null
        restart
        echo "installed, theme $(current_theme)"
        ;;
    run)
        "$0" build && "$0" install
        ;;
    theme)
        need_device
        if [ $# -eq 0 ]; then current_theme; exit 0; fi
        want="$1"
        case "$want" in SYSTEM|LIGHT|DARK|SUNLIGHT|NIGHT|COLORFUL) ;; *)
            echo "unknown scheme $want" >&2; exit 1 ;; esac
        was="$(current_theme)"
        adb shell am force-stop "$PKG"
        adb shell "run-as $PKG sed -i 's|<string name=\"theme_scheme\">$was</string>|<string name=\"theme_scheme\">$want</string>|' $PREFS"
        restart
        echo "theme $was -> $(current_theme)"
        ;;
    shot)
        need_device
        mkdir -p "$SHOTS"
        out="$SHOTS/${1:-shot}.png"
        adb exec-out screencap -p > "$out"
        echo "$out"
        ;;
    tap)
        adb shell input tap "$1" "$2"; sleep 1
        ;;
    tapt)
        xy="$(centre_of "$1")"
        [ -n "$xy" ] || { echo "no view with text \"$1\" on screen" >&2; exit 1; }
        adb shell input tap $xy; sleep 1
        echo "tapped \"$1\" at $xy"
        ;;
    find)
        adb shell uiautomator dump /sdcard/ui.xml >/dev/null
        adb shell cat /sdcard/ui.xml | tr -d '\r' | grep -o "<node [^>]*>" \
            | grep -F "$1" | grep -o 'text="[^"]*"\|bounds="[^"]*"' | paste - -
        ;;
    back)
        adb shell input keyevent BACK; sleep 1
        ;;
    db)
        need_device
        dir="${1:?db DIR}"; mkdir -p "$dir"
        for f in hullbeat.db hullbeat.db-wal hullbeat.db-shm; do
            adb exec-out run-as "$PKG" cat "databases/$f" > "$dir/$f"
        done
        ls -l "$dir"
        ;;
    crash)
        adb logcat -d -b crash | tail -40
        ;;
    *)
        sed -n '2,25p' "$0"; exit 1
        ;;
esac
