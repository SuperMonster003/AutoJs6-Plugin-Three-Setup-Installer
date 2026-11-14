# Privileged installation sources and restricted settings

Date: 2026-11-14. Candidate: 1.2.1/build146.

## Report and cause

The reported device is Sony XQ-DQ72 / Android 13. Installation through the
automatic authorizer resulted in restricted accessibility settings, while the
device's system installer did not. The report does not identify which authorizer
the automatic selection actually used.

`PrivilegedInstallerImpl.createSession` supplied `PACKAGE_SOURCE_LOCAL_FILE`
whenever `packageSource` was omitted. This implementation is shared by Shizuku
UserService and libsu RootService. Android 13's `InstallPackageHelper` applies
`ACCESS_RESTRICTED_SETTINGS = MODE_ERRORED` to local-file and downloaded-file
sources even when installation uses a privileged identity. The shell installer
instead defaults to `PACKAGE_SOURCE_OTHER`.

Primary references:

- [Android 13 shell installation defaults](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-release/services/core/java/com/android/server/pm/PackageManagerShellCommand.java).
- [AOSP restricted-settings installation tests](https://android.googlesource.com/platform/cts/+/86ec9bc4ab5/tests/tests/packageinstaller/install/src/android/packageinstaller/install/cts/RestrictedSettingsTest.kt).
- [Android guidance for existing restricted settings](https://support.google.com/android/answer/12623953).

## Change and scope

- On API 33+, omitted privileged package sources now resolve to `OTHER`.
- All five explicitly supplied source values retain their framework values.
- The ordinary and Dhizuku transports retain their local-file default. The API
  gate, installing package identity, originating UID, installation flags,
  authorization order and public contract are unchanged.
- Installation does not set app-op modes or enable an accessibility service.
  Existing restrictions remain subject to Android's user confirmation.
- README FAQ, plugin instructions and current changelog were updated in all ten
  languages. The privileged spike runner selects the new regression only on its
  Android 13/14 policy range and reads the current APK version from the repository.

## Device regression

A task-owned API 33 Google APIs x86_64 emulator was used. The existing fixed spike
APK has no accessibility service; the regression reads the actual app-op used to
restrict access, rather than claiming an accessibility UI interaction.

`PrivilegedInstallerDeviceTest.defaultSourceAvoidsRestrictedSettingsAndExplicitSourcesArePreserved`
installs six fresh variants and one update through the real private service. It
checks the platform's recorded package source and installing package, and reads
the effective restricted-settings app-op. It refuses pre-existing fixture data
across all enumerated users and removes only its own fixture.

| Requested source | Recorded source | Restricted-settings mode |
| --- | --- | --- |
| Omitted | OTHER (1) | ALLOWED |
| LOCAL_FILE (3) | LOCAL_FILE (3) | ERRORED |
| DOWNLOADED_FILE (4) | DOWNLOADED_FILE (4) | ERRORED |
| UNSPECIFIED (0) | UNSPECIFIED (0) | ALLOWED |
| OTHER (1) | OTHER (1) | ALLOWED |
| STORE (2) | STORE (2) | ALLOWED |
| Update a restricted LOCAL_FILE installation with omitted source | OTHER (1) | ERRORED remains |

Results:

- Shizuku with shell UID 2000: the focused regression passed, 1/1, in 5.899 s.
- The updated privileged spike runner passed all 4/4 tests, no skips, in 6.460 s.
  This also exercises install/update/downgrade rejection/uninstall/rebind, invalid
  writes and abandonment, and the existing ordinary-default tests.
- Shizuku restarted as root UID 0 on the same task-owned emulator: the focused
  regression passed, 1/1, in 7.515 s. Both identities exercise the common production
  implementation. This does not claim a separate libsu RootService binding test.
- The first focused attempt installed and cleaned up its fixture, but the read of
  the restricted app-op failed because the test adopted only GET_APP_OPS_STATS.
  Android also requires MANAGE_APPOPS to read this particular operation. The test
  now adopts both only for the read and always drops that identity in `finally`.
  No production permission or app-op write was added.

## Build checks

- JVM: 394 tests, zero failures/errors/skips.
- Debug and androidTest assembly: passed.
- Debug lint: zero errors, 60 warnings.
- Signed Release assembly and lint: passed, zero lint errors and 60 warnings.
- Markdown generation check: ten languages, 36 artifacts.
- Icon Studio reproduction: 22 resources verified.
- Privileged runner PowerShell parse and actual API 33 execution: passed.

## Reported Sony and delivery

The user supplied the remote ADB connection for Sony XQ-DQ72 after the local
emulator run. The device reports API 33 and arm64-v8a/armeabi-v7a/armeabi. The
installed plugin was 1.2.1/build144, without debug flags, and its Shizuku permission
was granted. The retained Shizuku server ran as root UID 0. There was no saved
authorizer/default-options preference file; installation history included actual
`shizuku` results. These facts support Shizuku as the current automatic choice,
without claiming every historical `auto` entry resolved the same way.

- The original APK and settings/history were backed up locally before replacement.
- The focused regression on a temporary build146 Debug installation passed 1/1,
  no skips, in 7.285 s through the existing Shizuku service. All six fresh-source
  cases and the existing-restriction update case matched the table above.
- A separate libsu RootService attempt stopped before creating a session because
  `Shell.getShell().isRoot` was false (`Root was not granted`). It is recorded as
  a failed authorization check, not a passed Root transport test. No root-manager
  grant was modified. The successful Shizuku path nevertheless exercised the
  shared installation implementation under the device's actual root UID.
- The temporary test APK was removed, the test fixture was absent including
  retained data, and the signed build146 Release replaced the temporary Debug APK.
- All 49 original history entries and the settings/history file hashes were
  unchanged. The original Shizuku server PID and UID were unchanged. Notification
  and Shizuku runtime permission states were retained.
- Instrumentation left an explicit `NO_ISOLATED_STORAGE: deny` audit row where
  none had been displayed before. The Android 13 operation default is also
  `MODE_ERRORED`; effective access is unchanged. Existing recorded app-op modes,
  including the plugin's own restricted-settings denial, were preserved. This
  does not claim byte-identical app-op output or clear existing restrictions.

Delivered APK: `autojs6-plugin-three-setup-installer-v1.2.1-8bb1746f.apk`, 2219971 bytes,
versionCode 146, APK Signature Scheme v2 verified, Release flags. SHA-256:
`7ad46e39b1c8ac8985631c396a1a4dedf056a7d132c90fee86843d47ca313683`.
The digest packaging task passed, with one signed APK in `releases/`. This is a
local signed build; the two pre-existing local release APKs are retained in
`.bak/restricted-settings-previous-releases/` without changing their bytes.

The Sony behavioral regression used Debug; the final signed Release was checked
for signature, version and installed-file identity. No actual accessibility
service was enabled, and the OEM accessibility screen was not exercised. Android
14 and newer ECM/OEM policy variants have not been exercised by this run. The
task-owned emulator was shut down. Automatic approval review rejected deletion
of its verified task-owned directory with only `blocked by policy` as the reason;
the disk image remains in the ignored `.bak/restricted-settings-avd/` directory.
Local test logs and the original Sony APK/settings/history backup are kept in
`.bak/` as well.
