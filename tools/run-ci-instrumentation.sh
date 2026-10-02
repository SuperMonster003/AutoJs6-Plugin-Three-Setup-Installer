#!/usr/bin/env bash
# Keep evidence while emulator-runner still owns the live, disposable device.
set -euo pipefail

: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the disposable CI emulator}"
task_repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$task_repo_dir"

collect_failure() {
    task_exit_code=$?
    trap - EXIT
    if [[ "$task_exit_code" -ne 0 ]]; then
        # Diagnostics are best effort; their failures must not replace the test exit code.
        set +e
        task_output_dir=build/ci-device
        task_package=io.github.supermonster003.autojs6.plugin.three.setup.installer
        mkdir -p "$task_output_dir/ui-test-failures"
        adb -s "$ANDROID_SERIAL" logcat -d -v threadtime > "$task_output_dir/logcat.txt" 2>&1
        adb -s "$ANDROID_SERIAL" shell dumpsys window > "$task_output_dir/window.txt" 2>&1
        adb -s "$ANDROID_SERIAL" shell dumpsys activity activities > "$task_output_dir/activities.txt" 2>&1
        adb -s "$ANDROID_SERIAL" shell dumpsys input_method > "$task_output_dir/input-method.txt" 2>&1
        adb -s "$ANDROID_SERIAL" shell dumpsys power > "$task_output_dir/power.txt" 2>&1
        adb -s "$ANDROID_SERIAL" exec-out screencap -p > "$task_output_dir/screen-after-tests.png" 2> "$task_output_dir/screencap-error.txt"
        # The UI fixtures capture their active window before their finally blocks close it.
        # Read only those generated filenames; do not pull the application's private data tree.
        while IFS= read -r task_name; do
            task_name="${task_name%$'\r'}"
            if [[ ! "$task_name" =~ ^[A-Za-z0-9._-]+$ || "$task_name" == . || "$task_name" == .. ]]; then
                continue
            fi
            adb -s "$ANDROID_SERIAL" exec-out run-as "$task_package" cat "cache/ui-test-failures/$task_name" \
                > "$task_output_dir/ui-test-failures/$task_name" 2>> "$task_output_dir/fixture-copy-errors.txt"
        done < <(adb -s "$ANDROID_SERIAL" shell run-as "$task_package" ls cache/ui-test-failures 2> "$task_output_dir/fixture-list-error.txt")
    fi
    exit "$task_exit_code"
}

trap collect_failure EXIT
chmod +x gradlew
./gradlew :app:connectedDebugAndroidTest --console=plain --stacktrace \
    -Pautojs.gradle.build.number.auto.increment.enabled=false \
    -Pautojs.gradle.build.time.update.enabled=false "$@"
