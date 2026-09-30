# Third-party notices

3-Setup Installer (`AutoJs6-Plugin-Three-Setup-Installer`) is licensed under the Mozilla Public License 2.0
(see `LICENSE`). The components below are distributed with the plugin or used to build it; each keeps its own
license, reproduced in full in the distribution of the respective project.

## Host contract artifacts (staged in `libs/`, hash-locked in `locks/host-api-aars.lock`)

| Artifact | Origin | Version | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `common-plugin-api.aar` | AutoJs6 module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | host build 6.8.0 / 5298, commit `86d9bfa26b` | MPL 2.0 | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |
| `package-archive-parser.aar` | AutoJs6 module `plugin-api/package-archive-parser` (the shared package archive parser, roadmap D10 / D28) | host build 6.8.0 / 5299 (P1 line), 2026-09-30 | MPL 2.0 | `d979659e6ea8c03a248ba841bf2b78b833697149e36fe1416ee52fc046ff2ab6` |
| `installer-api.aar` | AutoJs6 module `plugin-api/installer-api` (installer contract V1, roadmap P1.2) | host build 6.8.0 / 5299 (P1 line), 2026-09-30 | MPL 2.0 | `0e9996f86736e7ba312c625b5f7f5db78967be5c46f1c1399d75e60470ecb72f` |

## Runtime dependencies (Gradle)

| Component | Coordinates | Version | License | Purpose |
| --- | --- | --- | --- | --- |
| Shizuku API | `dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider` (https://github.com/RikkaApps/Shizuku-API) | 13.1.5 | Apache License 2.0 | Shizuku permission request, binder wrapper and the privileged `UserService` (roadmap D11) |
| libsu | `com.github.topjohnwu.libsu:core`, `com.github.topjohnwu.libsu:service` (https://github.com/topjohnwu/libsu, JitPack) | 6.0.0 | Apache License 2.0 | Root shell and the `RootService` hosting the privileged installer (roadmap D11) |
| AndroidHiddenApiBypass | `org.lsposed.hiddenapibypass:hiddenapibypass` (https://github.com/LSPosed/AndroidHiddenApiBypass) | 6.1 | Apache License 2.0 | Lifts the hidden API restriction for the package installer interfaces (roadmap D15) |
| AndroidX AppCompat | `androidx.appcompat:appcompat` | 1.7.1 | Apache License 2.0 | Activity and theme support |
| Material Components for Android | `com.google.android.material:material` | 1.13.0 | Apache License 2.0 | Material 3 widgets and themes |
| Gson | `com.google.code.gson:gson` | 2.13.2 | Apache License 2.0 | JSON documents of the Binder contract |
| Kotlin standard library | `org.jetbrains.kotlin:kotlin-stdlib` | managed by the platform versions plugin | Apache License 2.0 | Language runtime |

Test-only dependencies (JUnit 4, AndroidX Test runner / rules / ext-junit, Apache License 2.0 or EPL 1.0) are
not shipped in the APK.

## Android Open Source Project interface signatures

The hand-written adapters in `priv/hidden` use signatures of hidden Android framework interfaces
(`android.content.pm.IPackageInstaller`, `IPackageInstallerSession`, `IPackageManager`, `ParceledListSlice`,
`android.os.IUserManager`, `UserInfo` and related constants). Signature references are from the Android Open
Source Project (https://android.googlesource.com/platform/frameworks/base, Apache License 2.0), Android 7 through
15 release tags. No AOSP implementation is copied: reflection invokes the device's own Binder Stub and framework
Session, preserving its transaction numbering and FileBridge protocol. No replacement `android.*` classes are
packaged. The package installer status constants come from the public Android SDK.

## Reference projects (no code reused)

InstallerX (https://github.com/iamr0s/InstallerX) and InstallerX Revived (https://github.com/wxxsfxyzm/InstallerX-Revived)
are licensed under the GNU General Public License v3.0. They served only as an architecture, behavior and option
catalog reference for the roadmap (decision D29); no source code, resource, string or asset of either project is
included in this repository.

## Build tooling (not distributed)

Gradle, the Android Gradle Plugin, the Kotlin Gradle Plugin, `io.github.supermonster003.autojs6-platform-versions`,
`io.github.supermonster003.autojs6-native-alignment`, Apache Commons Compress and XZ for Java are used by
`build-logic/` and the Gradle build only.
