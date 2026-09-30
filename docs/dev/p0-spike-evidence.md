# P0 evidence: repository skeleton and privileged installation spike

Companion of `ROADMAP.md` phase P0. Dates are local (GMT+08:00). Device serials follow `adb devices -l` of the day.

## P0.1 Repository skeleton (2026-09-30)

### Toolchain

| Item | Value |
| --- | --- |
| Gradle / AGP / Kotlin / JDK | 9.5.0 / 9.3.2 / 2.3.20 / 21 (platform versions plugin 1.8.3, single version decision block in the build log) |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| Host API AAR | `common-plugin-api.aar`, SHA-256 `ee7eb787...532e15`, byte-identical to the artifact staged by 3-Stove Agent (host 6.8.0 / 5298) |
| Runtime dependencies | Shizuku API 13.1.5 (Maven Central), libsu 6.0.0 (JitPack; Maven Central has no such coordinate), AndroidHiddenApiBypass 6.1 (Maven Central), AppCompat 1.7.1, Material 1.13.0, Gson 2.13.2 |
| Debug APK | `autojs6-plugin-three-setup-installer-v1.0.0.apk`, 6,376,497 bytes, no native libraries (`verifyDebugNativePageAlignment` passed) |

### Verification

| Check | Result |
| --- | --- |
| `:app:testDebugUnitTest` | 14 tests, 0 failures (`ThreeSetupInstallerPluginRuntimeInfoTest` 3, `ManifestContractTest` 6, `StringResourceParityTest` 4, `ApplicationTextPunctuationTest` 1) |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest` | passed |
| `:app:lintDebug` | 0 errors, 9 warnings (newer dependency versions available; launcher icon resources unused until the P5.4 aliases) |
| `py -3 .python/generate_markdown.py --check` | `MARKDOWN_OK languages=10 artifacts=36` |
| `py -3 .python/generate_launcher_icons.py --check` | 15 icon resources verified, byte-identical on the second run |
| `:app:connectedDebugAndroidTest` on AVD API 24 (`emulator-5554`, x86, Android 7.0) | 5 tests, 0 failures |
| `:app:connectedDebugAndroidTest` on Xiaomi 23046RP50C (`968e9f18`, API 35, HyperOS) | 5 tests, 0 failures |
| `cmd package query-services --brief -a org.autojs.plugin.INFO -c installer` (API 35) | exactly `.ThreeSetupInstallerPluginInfoService` |
| `cmd package query-services --brief -a org.autojs.plugin.INSTALLER -c installer` (API 35) | exactly `.ThreeSetupInstallerPluginService` |
| `cmd package query-activities -a org.autojs.plugin.action.WAKE` (API 35) | `.WakeActivity`; no MAIN / LAUNCHER entry for the package |
| AutoJs6 plugin center (host 6.8.0 / 5298 on the Xiaomi) | lists `3-Setup Installer 1.0.0 (1) SuperMonster003` with the zh-Hans description and enables it automatically through the official signature; the row shows the dark adaptive system icon because the host's transparent-icon allowlist does not know the package yet (roadmap P1.3) |

### Facts recorded for later phases

- The plugin center row of the other official plugins carries a release date after the version (`1.2.0 (78) | 2026-09-21`); the new plugin shows `1.0.0 (1)` only. The date appears to come from the online index release metadata rather than from `PluginInfo.versionDate`; to be confirmed in P1.3 / P7.3.
- `FOREGROUND_SERVICE` is declared without a service type; the type permission is decided in P3.4 (appendix D, Q6).
- `REQUIRED_HOST_VERSION` is the temporary value 5298 until P1.5 back-fills the host build that ships `installer-api`.
- Device pool of the day: Xiaomi 23046RP50C (API 35, Shizuku 13.5.4 installed but not running, no `su`), Sony G8441 (API 28, Shizuku 13.5.4 server running as root, Magisk 26.4 grants `su` to shell), Sony XQ-AT72 (API 31, Shizuku installed, not running, no `su`), Redmi 22120RN86C (API 33, Shizuku installed, not running, no `su`), Sony XQ-DQ72 (API 33, `/system/bin/su` present, disconnected during the session), AVD API 24 x86 (`emulator-5554`, `emulator-5558`), AVD API 37 x86_64 16 KB (`emulator-5556`); the AOSP `su` of the emulators refuses app callers, so the Root path can only be exercised on the Sony G8441.

## P0.2 Privileged installation spike

Pending.
