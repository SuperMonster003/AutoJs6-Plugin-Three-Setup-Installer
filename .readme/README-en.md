<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Installs, updates and uninstalls Android apps with system confirmation, Shizuku or Root</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Setup Installer installs, updates, inspects and uninstalls Android apps through AutoJs6 installation entries and external package-opening or sharing requests. It supports ordinary Android confirmation and privileged installation through Shizuku or Root. The script API and standalone home and settings pages are still planned.

AutoJs6 discovers the plugin through its Binder service and hands over package files as read-only file descriptors; the plugin parses the package, picks the authorizer, shows its own confirmation and progress dialog when needed, and reports stages, progress and results back. Privileged operations run in a Shizuku user service or a libsu root service that talks to the system package installer directly.

******

### Status

******

1.0.0: P3 development preview. Confirmation, progress, results and batch dialogs, external opening and sharing, optional source deletion, system confirmation and foreground notifications are implemented. The script API, standalone home and settings, installation history and default-installer configuration remain planned. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for progress and device coverage. AutoJs6 >= 6.8.0 (5299).

******

### Features

******

Available in this development preview, with later features explicitly marked:

- Package formats: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` and ZIP archives that contain APKs; split packages are selected for the device; `.aab` files are recognized and described but not installed.
- Authorizers: `none` uses Android confirmation; `shizuku` and `root` provide privileged operations. `auto` chooses available Shizuku, then Root, then system confirmation. The installation dialog lets you choose an authorizer.
- Optional source deletion after successful installation is best effort. Downgrades, test packages, low target SDK bypass (Android 14+), installer attribution and other target users require Shizuku or Root and remain subject to Android restrictions.
- Silent uninstallation with an optional keep-data flag through Shizuku or Root; the regular system dialog otherwise.
- Confirmation shows app details, old and new versions, signatures and selectable APK components. Progress supports cancellation; results show success actions or error details with copying. Batch installation shows each item separately.
- Open package files or share one or multiple packages with the plugin. Failed external sources can be retried while their URI and access remain available.
- Foreground installation progress, cancellation and result notifications. Denying notification permission does not prevent installation.
- Dialogs follow AutoJs6 language, night mode and theme color by default, with system language/night and a default color when the host is unavailable.
- Planned: default-installer configuration, including privileged selection and guidance for system defaults where needed.
- Planned for P4: script API `installer` (alias `$installer`) with synchronous, `...Async` and session forms, and `InstallerError` failures with stable `code` values.
- Planned for P5: standalone home and settings pages, installation history and installed-app management.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) on a device with AutoJs6 build 5299 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Setup Installer` is recognized, and enable it.
3. Use an installation action in AutoJs6, or choose 3-Setup Installer when opening or sharing package files. When a confirmation dialog appears, review the app and options before installing. Prepare Shizuku or Root authorization when selecting a privileged method.

******

### Authorizers

******

What each authorizer can do and what it needs:

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: needs Shizuku running (started through wireless debugging, ADB or Root) and permission granted to the plugin. Its shell privileges support silent installation and uninstallation and operations for other users.
- `root`: needs a Root manager that grants `su` to the plugin; provides the same operations as Shizuku through a libsu root service. Downgrades on regular (user) firmware still succeed only for debuggable apps, which is a framework rule, not a plugin limit.
- **Note:** With privileges available, host requests using `interaction: 'auto'` install silently without proactively opening confirmation. If Android still requires confirmation, `auto` permits it and records this in `notes`. Use `interaction: 'dialog'` to request confirmation before installation, or `interaction: 'silent'` to fail when system confirmation is required. The planned script API follows the same default.

******

### Quick Start

******

A script that installs silently, updates with a downgrade allowance, watches a session and uninstalls (available from roadmap P4 on):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### Compatibility

******

Platform facts that shape what the plugin can do:

- Android 7.0 (API 24) and later. Device validation and remaining coverage are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- The bypass of the low target SDK block exists from Android 14 (API 34); on older systems the option is ignored and noted in the result.
- Some OEM systems restrict which app may be the default installer or require an installer package name that they trust (HyperOS accepts `com.android.shell`); the plugin reports the system's answer as it is.

******

### FAQ

******

- **Why does installation still require confirmation?** `none` always uses system confirmation. Choose Shizuku or Root in the installation dialog after preparing its authorization. Android or device policy may still require a system prompt.
- **Can an `.aab` be installed?** No. An Android App Bundle is a publishing format; convert it with bundletool into an `.apks` set first. The plugin recognizes `.aab` files and shows their package and module information.
- **Why was the source not deleted?** Deletion runs only after installation succeeds and may be refused by the source provider. Installation remains successful. When AutoJs6 or another sending app owns the source, that app is responsible for deletion.
- **Can I retry or resume?** A failed external URI can be retried while the source and access are available. Once the source or its access is released, reopen the package. If the process is lost, the restored interface reports interruption and never automatically reinstalls. Check the installed app before starting again.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the external "Open with" entry only accepts package files and never runs a script.
- REQUEST_INSTALL_PACKAGES and REQUEST_DELETE_PACKAGES back the regular installation and uninstallation dialogs; QUERY_ALL_PACKAGES lets the plugin show the installed version and compare signatures before an update.
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC support background installation work; POST_NOTIFICATIONS allows progress and result notifications. Missing notification permission does not block installation.
- Shizuku and Root are used only for the operation you start; the privileged service holds no state, keeps no shell open between operations and is never reached from outside the plugin.
- Package files are opened read-only; the plugin makes no network request, collects no data and excludes its private storage from backups.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5299 (6.8.0)
```

`ThreeSetupInstallerPluginService` answers `org.autojs.plugin.INSTALLER` (category `installer`) and implements the host installer-api contract `org.autojs.plugin.installer.api.IInstallerPlugin` from roadmap P1 on. `ThreeSetupInstallerPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/10/01_

- `Hint` P3 development preview. Confirmation, progress, results and batch dialogs, external opening and sharing, optional source deletion, system confirmation and foreground notifications are implemented. The script API, standalone home and settings, installation history and default-installer configuration remain planned. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for progress and device coverage. AutoJs6 >= 6.8.0 (5299).
- `Feature` Plugin identity `three-setup-installer` (engine `installer`) with the INFO service, the Wake Activity and the `org.autojs.plugin.INSTALLER` service skeleton for host discovery
- `Feature` README, plugin-center instructions and changelog in 10 languages
- `Improvement` The plugin id, engine, service action / category, Binder descriptor and minimum host version now come from the host installer-api contract constants; the capabilities declare installer contract version 1 and the minimum host build is back-filled to 5299
- `Improvement` Seekable sources avoid a full cache copy, while streams are staged as needed. ZIP split packages are supported, AAB files support inspection only, and changed sources are rejected.
- `Improvement` Explicit authorization choices never fall back. Refusal, timeouts and incompatibility are distinguished, and concurrent requests share authorization and privileged connections.
- `Improvement` Core installation and updates use system confirmation, Shizuku or Root, with cancellation and results that reflect the actual confirmation and system response.
- `Improvement` Core uninstallation supports system confirmation, Shizuku and Root, with optional data retention when using a privileged authorizer.
- `Improvement` Serial batch installation can continue after failures or cancel remaining items, and supports validating and selecting target users with privileges.
- `Improvement` Host service requests support inspection, installation, uninstallation and user queries, with explicit confirmation, cancellation when callers exit, up to four concurrent sessions and automatic cleanup.
- `Improvement` Installation dialogs follow AutoJs6 language, night mode and theme color, with a fallback when the host is unavailable and layouts that support large text and RTL.
- `Improvement` Background installation now has foreground progress, cancellation and result notifications. Denying notification permission does not block installation.
- `Improvement` Added installation confirmation, progress and result dialogs with app details, APK component selection, options, error copying and per-item batch status. A lost process is reported as interrupted without automatic reinstallation.
- `Improvement` System installation confirmation now handles unknown-source permission guidance and interruption. Privileged uninstallation shows app details and a keep-data choice before confirmation.
- `Improvement` Open or share one or multiple packages, retry failed external sources while access remains available, and optionally attempt source deletion after success. Deletion refusal preserves the successful installation result.
- `Dependency` Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) for the Shizuku authorizer
- `Dependency` libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) for the Root authorizer
- `Dependency` AndroidHiddenApiBypass 6.1 for the hidden package installer APIs used by the privileged service
- `Dependency` `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5298, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
- `Dependency` `package-archive-parser.aar` and `installer-api.aar` (AutoJs6 modules `plugin-api/package-archive-parser` and `plugin-api/installer-api`, host P1 build 6.8.0 / 5299, MPL 2.0), hash-locked in `locks/host-api-aars.lock` together with `common-plugin-api.aar`
- `Dependency` Refresh the bundled package archive parser to recognize ordinary ZIP split containers

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- Installer module documentation: https://docs.autojs6.com/#/installer
- InstallerX and InstallerX Revived (architecture reference, GPL-3.0, no code reused): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
