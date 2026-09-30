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

Completed on 2026-09-30. This validates the privileged transport and platform operations only. Host
`installer-api`, the public install engine, user dialogs and script entry points are still pending.

### Implementation and compatibility decisions

- The private `IPrivilegedInstaller` implements session creation, write, commit, abandon, uninstall,
  default selection, users and UID. `attachClient` links the owner's death token; Shizuku's reserved
  transaction `16777115` closes the service and exits its process.
- Both Shizuku UserService and libsu RootService execute as privileged processes and use system Binders
  directly. `ShizukuBinderWrapper` is exercised separately in the main-process preference diagnostic.
  Wrapping again inside UserService would require an unnecessary app-side Shizuku connection there.
- `priv/hidden` invokes the device's framework Stub via reflection. It covers the pre/post API 26
  uninstall signatures, API 31 installer attribution argument, API 33 long query flags, API 30 user-list
  arguments, and both preferred-activity signatures. No generated partial AIDL with incorrect system
  transaction numbers, compile-only Android replacement classes, or GPL implementation is used.
- `HiddenApiBypass` initializes once per process on API 28+. Reflective calls unwrap
  `InvocationTargetException`, preserving system `SecurityException` and platform error messages.
- AOSP API 24 `IPackageInstallerSession.openWrite` returns a FileBridge endpoint. A raw APK byte stream
  cannot be written to it. The service uses the framework `PackageInstaller.Session` wrapper, drains a
  private ordinary pipe into its OutputStream, verifies the declared byte length and fsyncs before commit.
- `createReliablePipe` failed on the Sony API 28 Magisk path: Binder FD transfer produced
  `DeadObjectException`, while logcat showed an SELinux denial of the accompanying Unix stream socket
  from the Magisk domain to the app. Ordinary pipe descriptors work; writer Futures propagate errors.
  No SELinux policy, permissive mode or root framework setting was changed.
- At most four sessions and 64 split names per session are accepted. Stream reads poll with cancellation
  and a 30-minute ceiling. Commit has a bounded 30-second drain wait. Abandon closes input and output
  streams, including writes still queued behind the four workers, before canceling their Futures.
- The Binder verifies the plugin UID before clearing the incoming calling identity for system calls.
  Context-aware service construction retains the owning app user. RootService is not exported. The
  plugin does not pass this privileged Binder to the host.
- Unbind is asynchronous. Tests wait for Binder death before rebinding; stale disconnect callbacks
  while a fresh binding is still pending are ignored. No sleep is used as a substitute for process exit.
- Default selection uses four independent APK MIME filters: VIEW / INSTALL_PACKAGE x content / file.
  The six-argument `addPreferredActivity` replaces matching filters. On API 24 / 28,
  `replacePreferredActivity` rejects MIME filters with `IllegalArgumentException`; the five-argument
  `addPreferredActivity` is retained. Legacy competing defaults return `DEFAULT_REQUIRES_CLEAR=-1`
  before mutation, for the P5 UI to direct the user to system settings. Only the plugin's own preferences
  are cleared. Other applications' unrelated defaults are never cleared.

Primary signature references: [AOSP Android 7 installer AIDL](https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/content/pm/IPackageInstaller.aidl),
[AOSP Android 15 package-manager AIDL](https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/core/java/android/content/pm/IPackageManager.aidl),
[Shizuku UserService lifecycle](https://github.com/RikkaApps/Shizuku-API/blob/master/README.md),
[libsu RootService lifecycle](https://topjohnwu.github.io/libsu/com/topjohnwu/superuser/ipc/RootService.html).

### Fixtures and repeatable execution

`app/src/androidTest/assets/fixture-v1.apk` and `fixture-v2.apk` are signed, code-free APKs with package
`io.github.supermonster003.autojs6.installer.spike.fixture`, version codes 1 / 2, minSdk 24, targetSdk 28,
no permissions and no components. Each file is 8,536 bytes. Both share one test-only signing key.

| Asset | SHA-256 |
| --- | --- |
| `fixture-v1.apk` | `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69` |
| `fixture-v2.apk` | `dc3716a9c54e63219ed621ef06f68c4088f54630e4b4c167aebfdda653a4bb38` |

Manifest sources are under `tools/spike-fixtures/`; `tools/build-spike-fixtures.ps1` rebuilds both using
Android build-tools 37.0.0 and SDK platform 37.0. The locally generated test key stays in ignored
`build/spike-fixtures/`. Rebuilding on another machine produces a new shared signing key and new hashes;
update the pair and this table together. It never uses the project's release key.

The test refuses to proceed if this package already exists. Cleanup uninstalls only the fixture created
by that run. Preference tests refuse to alter pre-existing APK defaults, seed a competing debug alias
within the plugin, verify the switch, then clear only the plugin's temporary preference entries.
The temporary activity, alias and result receiver exist only in the debug source set.

```powershell
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:assembleDebugAndroidTest
.\tools\run-privileged-spike.ps1 -Serial emulator-5554 -Authorizer shizuku
.\tools\run-privileged-spike.ps1 -Serial BH900ASK9E -Authorizer root
```

Start Shizuku and grant the plugin permission before running the Shizuku suite. The root suite also
uses Shizuku for the main-process persistent-preference diagnostic; on the Sony it runs as uid 0.
Root operations themselves use libsu, not Shizuku. The script records output in ignored
`build/spike-evidence/` and fails on either a failed or skipped device check. Ordinary instrumentation
without `privilegedAuthorizer` skips these three opt-in cases and runs the baseline contract suite.

### Device observations

All serials, API levels and ABIs were read again in this session. Shizuku version is 13.5.4; root is
Magisk 26.4 on Sony G8441. Durations below are individual successful observations, not a benchmark.
Installation and uninstall success means `PackageInstaller.STATUS_SUCCESS`, with version codes checked
after installation. Downgrade from 2 to 1 without an override returned `INSTALL_FAILED_VERSION_DOWNGRADE`.

| Device / serial | API / ABI | Authorizer / UID | New / update / rejected downgrade / uninstall (ms) | Default |
| --- | --- | --- | --- | --- |
| AVD / `emulator-5554` | 24 / x86 | Shizuku ADB / 2000 | 108 / 189 / 137 / 42 | 4/4 filters |
| Dedicated AVD / `emulator-5560` | 31 / x86_64 | Shizuku ADB / 2000 | 140 / 109 / 161 / 209 | 4/4 filters |
| Xiaomi 23046RP50C / `968e9f18` | 35 / arm64-v8a | Shizuku ADB / 2000 | 261 / 410 / 32 / 166 | 4/4 filters |
| Sony G8441 / `BH900ASK9E` | 28 / arm64-v8a | libsu Root / 0 | 383 / 632 / 148 / 295 | 4/4 filters |
| Sony XQ-AT72 / `QV710AF65F` | 31 / arm64-v8a | Shizuku ADB / 2000 | 303 / 197 / 110 / 451 | skipped to preserve existing APK defaults |

The dedicated API 31 AVD `Three_Setup_Installer_Spike_31` used the already installed Google Play x86_64
system image. It supplements the API 31 physical device without clearing its defaults. User enumeration
returned `[0, 10]` on that physical device and `[0]` on the other tested targets; no secondary-user
installation was performed. The API 24 and API 31 AVDs and Xiaomi used shell identity for Shizuku.
The first four rows are the final build 6 rerun, including replacement of the seeded debug alias.

For all four default-selection targets, `addPersistentPreferredActivity` threw `SecurityException`
stating that only the system may call it. This includes uid 0 on the Sony (via the Shizuku root wrapper).
Plain `addPreferredActivity` succeeded as both shell and libsu root. Thus D11 remains valid, D23 is
qualified for legacy existing defaults, and the command-based fallback in appendix E.2 is not needed.

### Verification and remaining boundaries

- JVM: 17 tests, zero failures (the original 14 plus three `PrivilegedOptionsTest` cases).
- Device: `PrivilegedInstallerDeviceTest` has three cases covering malformed/truncated input and session
  capacity, queued-write cancellation, real package operations, shutdown/rebind, and default permissions.
  All three pass on API 24 / 31 / 35 Shizuku and API 28 Root. Baseline INFO / activation Binder tests are
  rerun on API 24 and API 35.
- Build: debug APK, instrumentation APK, minified release APK and debug/release lint pass. Lint reports
  no errors; three new PrivateApi warnings are intentional and remain visible, alongside the nine
  existing dependency/resource warnings. Both APK variants pass the no-native-library check.
- Documentation and launcher generation checks pass. The release manifest and dex are checked for
  absence of debug spike components and replacement Android framework classes.
- The legacy competing-package `DEFAULT_REQUIRES_CLEAR` UI flow, actual abrupt host death under load,
  signed release installation on-device, large/split APK performance, all-user installs, explicit
  downgrade flags, low-target bypass and keep-data uninstall remain P2 / P5 / P6 validation work.
  P0's tests do not claim these capabilities have passed.
