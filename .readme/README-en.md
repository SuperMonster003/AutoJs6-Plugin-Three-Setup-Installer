<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Installs, updates and uninstalls Android apps with Android confirmation, Shizuku, Root or Dhizuku</p>

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

3-Setup Installer installs, updates, inspects and uninstalls Android apps from its standalone home screen, AutoJs6 entries and scripts, or external package-opening and sharing requests. It supports Android confirmation and privileged operations through Shizuku, Root or Dhizuku.

The plugin handles package inspection, installation and results independently of the host. Choose dialog, silent or notification interaction. Notification mode keeps confirmation, cancellation and results in notifications; Android confirmation, when required, opens only after you tap its notification.

******

### Status

******

1.2.1 documents the implemented installation, app-management and script features below. This version is available from [GitHub Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/tag/v1.2.1) and the AutoJs6 plugin center. Host integration requires AutoJs6 >= 6.8.0 (5299), and the `installer` script API requires build 5300 or later. Device coverage and remaining acceptance work are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Dhizuku, notification installation and persistent-default script options require AutoJs6 6.8.0 build 5307 or later with installer contract V2. Base host integration remains available from build 5299, and V1 script methods from build 5300.

******

### Features

******

Implemented features in 1.2.1:

- Package formats: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` and ZIP archives that contain APKs; split packages are selected for the device; `.aab` files are recognized and described but not installed.
- `none` uses Android confirmation. Fresh settings try usable `shizuku -> root -> dhizuku -> none` for `auto`; each method can be reordered or disabled. Existing saved three-method orders keep their relative order and enabled choices, with Dhizuku inserted before `none` but disabled. An explicit authorizer never falls back.
- Optional source deletion after successful installation is best effort. Downgrades, test packages, low target SDK bypass (Android 14+), installer attribution and other target users require Shizuku or Root and remain subject to Android restrictions.
- Search installed apps by label or package name, sort by name, installation or update time, and optionally show system apps. Open an app or its system details, or confirm uninstallation. Shizuku and Root support silent removal and optional data retention; Dhizuku supports privileged removal only in its current owner user and does not support `keepData`.
- Confirmation shows app details, old and new versions, signatures and selectable APK components. Progress supports cancellation; results show success actions or error details with copying. Batch installation shows each item separately.
- Open or share one or multiple installation packages, including APKS files shared by MT Manager. Multiple packages enter a serial queue. Failed external items can be retried while their URI and access remain available.
- `interaction: 'notification'` is an installation mode: notifications carry the initial confirmation, cancellation, progress and results without plugin installation dialogs. Android confirmation still requires a notification tap. Notification permission, the app's notifications and its installation channel must be enabled; otherwise the request fails with `NOTIFICATION_UNAVAILABLE`. Notification denial does not block the other interaction modes. Uninstallation does not accept `notification`.
- Appearance settings cover language, night mode, theme color and launcher icon. The first three follow AutoJs6 by default and support local overrides; host unavailability falls back to system language/night and the default color. Launcher icons offer light, dark, automatic and transparent modes; automatic follows the system, subject to launcher caching and masks.
- The default-installer page distinguishes ordinary preferences and persistent policy. Ordinary preferences use Shizuku or Root and remain subject to ROM restrictions. Persistent policy is available through Dhizuku on API 26-33; API 34+ is rejected before mutation because the owner callback cannot be verified. Root uses a system-UID helper only in user 0 on supported devices. Competing persistent policies are not overwritten. `persistentConfigured` is a receipt of a previous successful configuration, not proof of the current system policy; passive observation reports only `preferred` or `none`.
- The `installer` script API (alias `$installer`) provides synchronous, `...Async` and session forms for single, batch and split installation, uninstallation, inspection, authorizer and user queries, and default-installer settings. Failures are `InstallerError` objects with stable `code` values (requires AutoJs6 >= 6.8.0 (5300)).
- The standalone home shows Shizuku/Root/Dhizuku availability and authorization, the current default installer, active tasks and recent installations. Choose multiple packages with the system document picker to install serially, continue after individual failures or cancel remaining items.
- Private installation history retains up to 200 items, including package, label, old/new versions, outcome, time, origin (host/script/external/home), authorizer and failure details. Delete individual records or clear the history without uninstalling apps or deleting source files. After process death, unfinished items become cancelled and never resume automatically.
- Settings save authorizer order/enabled methods, installation options and notification preferences. Home/external installation defaults to `dialog`, with explicit `auto`, `silent` or `notification` choices available. Host UI installation actions use `dialog`; scripts retain explicit options and still default to `auto`. Changes are saved after confirmation.
- About and the built-in release history are available from settings in ten languages. Manual update checks use the plugin's GitHub Releases API with a 12-hour interval, cached results and ignored-version management. Release pages open in the browser; updates are not downloaded or installed automatically.
- `dhizuku`: requires Android 8.0 (API 26)+, an active Dhizuku device/profile owner and permission granted to this plugin. It operates in the current owner user, and installation attribution uses the real owner package. It does not grant shell/root downgrade, test-package, low-targetSdk bypass, other-user, arbitrary installer attribution or keep-data uninstall options. The plugin does not provision an owner.
- Advanced installation options: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` and `packageSource`. Unsupported platforms or authorizers reject requests instead of silently ignoring them. `none` adds no manual compilation and does not disable Android compilation.
- Successful results may report observed `updateOwner` and `dexopt`. Null means Android returned no owner to the current calling identity, possibly due to visibility filtering; it does not prove global absence. A failed read omits the field and adds notes. DexOpt states are accepted/failed/cancelled/timeout/unavailable/unknown; accepted includes a system skip and does not prove compilation ran. Failure of this extra step preserves confirmed installation success.
- Signature checks and local exact package-name/SharedUID blacklists apply to every entry point, using `BLOCKED_BY_POLICY`. Only a real dialog can allow a mismatch/unknown signature once for that item; Android still validates it. Silent/notification modes cannot grant this exception, and a blacklist cannot be overridden. Permission preview describes declarations in the selected APK splits, not granted permissions.
- Source profiles can be named, enabled, edited and reordered in settings. The first enabled profile matching both origin and actual package-name prefix applies; profiles never stack, and any origin includes Home. A profile supplies partial defaults for 12 per-app options. Explicit requests take precedence, and the real confirmation page can make final changes.

******

### Usage

******

1. On Android 7.0 or later, install the official APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) once published, or use the AutoJs6 plugin-center installation wizard once the official index lists it. Before publication, use a maintainer-provided build or build from source for testing. Open the launcher icon to reach the standalone Home screen.
2. On Home, check authorization and default-installer status, then select one or multiple packages. Confirm through the chosen dialog or notification and follow active tasks and recent history.
3. For AutoJs6 integration, use build 5299 (6.8.0) or later and enable `3-Setup Installer` in the plugin center. The script API requires build 5300 or later. Dhizuku, notification installation and persistent-default script options require AutoJs6 6.8.0 build 5307 or later with installer contract V2. Base host integration remains available from build 5299, and V1 script methods from build 5300.
4. Use an installation action in AutoJs6, or choose 3-Setup Installer when opening or sharing package files. When a confirmation dialog appears, review the app and options before installing. Prepare Shizuku, Root or Dhizuku authorization when selecting a privileged method.
5. Use the Home menu for installed apps and settings. Review local installation defaults, appearance, launcher icon and notifications; About, release history and manual update checks are in settings.

******

### Authorizers

******

What each authorizer can do and what it needs:

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: requires Shizuku running (started through wireless debugging, ADB or Root) and permission granted specifically to 3-Setup Installer. Permission granted to AutoJs6 does not authorize this plugin. Installation, uninstallation and other-user operations use the privileges of the running Shizuku service.
- `root`: requires a rooted device and a Root manager that grants `su` to 3-Setup Installer. It provides privileged installation, uninstallation and user/default-installer operations through libsu. Android and ROM policies still decide whether each requested operation is allowed.
- `dhizuku`: requires Android 8.0 (API 26)+, an active Dhizuku device/profile owner and permission granted to this plugin. It operates in the current owner user, and installation attribution uses the real owner package. It does not grant shell/root downgrade, test-package, low-targetSdk bypass, other-user, arbitrary installer attribution or keep-data uninstall options. The plugin does not provision an owner.
- **Note:** Scripts default to `interaction: 'auto'` and install silently when privileges are available. Host UI installation actions use `dialog`. If Android requires confirmation, `auto` permits it and records this in `notes`. Choose `interaction: 'dialog'` for confirmation before installation. Explicit `silent` fails with `AUTHORIZER_REQUIRED` if privileges are unavailable or system confirmation is required.
- Settings save authorizer order/enabled methods, installation options and notification preferences. Home/external installation defaults to `dialog`, with explicit `auto`, `silent` or `notification` choices available. Host UI installation actions use `dialog`; scripts retain explicit options and still default to `auto`. Changes are saved after confirmation.

******

### Quick Start

******

Examples of `install`, `installAsync`, `session`, `uninstall` and `setDefault` for AutoJs6 >= 6.8.0 (5300). The functions run only when called with sources, a package name or a default-installer choice you have selected. The initial status query is read-only.

```js
// Read-only availability and compatibility information.
console.log(installer.status);

// Requires Shizuku authorization; silent fails if Android requires confirmation.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// An array means independent packages; every item has its own result.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// All split files belong to one app, including its base APK.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// Starts when called; retain the returned session to cancel or wait.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// Only call with the intended package name; keepData requests data retention.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true sets this plugin as default; false clears its default. ROM limits apply.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// V2 examples require host build 5307. Dhizuku needs an active owner; notification permission is required.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root persistent mode requires supported system-UID access in user 0; competing policies are preserved.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});

// Advanced example: host V3, Root and API 33+ for these metadata fields; calling this function starts installation
let installAdvancedChosen = source => installer.installAsync(source, {
    authorizer: 'root', interaction: 'dialog', deleteSource: false,
    grantAllRequestedPermissions: false, requestUpdateOwnership: false,
    dexopt: 'speed-profile', installReason: 'user', packageSource: 'local-file',
}).then(result => console.log(result.ok, result.updateOwner, result.dexopt, result.notes))
    .catch(error => console.error(error.code, error.systemMessage));

// Source-profile example: host 5312+; retain files and reset three inherited fields; calling this function starts installation
let installWithProfileResets = source => installer.installAsync(source, {
    interaction: 'dialog', deleteSource: false,
    installer: null, installReason: null, packageSource: null,
});
```

A source can be a path, `file://` or readable `content://` URI. Arrays are independent batch items, while `{ splits: [...] }` installs one app. `session(...)` starts immediately; its returned object supports `cancel()` and `wait()`. Synchronous calls can throw `InstallerError` and cannot run on the UI thread. This also applies to reading `installer.status`, creating `installer.session(...)` and calling `session.wait()`. Use Async methods on the UI thread, or perform synchronous operations on a script worker thread. A session object must be used only on the script thread that created it. Handle Promise rejection and inspect each batch result's `ok` and `error`. A missing or incompatible plugin reports `PLUGIN_UNAVAILABLE`. `setDefault` returns whether the requested state was reached, including clearing the default. `app.uninstall` remains the host's system-uninstall shortcut; use `installer.uninstall` for privileged options. See the [installer API documentation](https://docs.autojs6.com/#/installer) for all options and events.

Dhizuku, notification installation and persistent-default script options require AutoJs6 6.8.0 build 5307 or later with installer contract V2. Base host integration remains available from build 5299, and V1 script methods from build 5300.

Advanced script options require AutoJs6 build 5308+ and negotiated V3 with `advanced-install-options`. Without profile overrides, omission preserves existing behavior; explicit `false`/`none` still requires support. The roadmap records official release and device acceptance status.

Host UI and script profiles require AutoJs6 5312+ and the V3 `source-profiles` feature. Older or non-negotiated requests retain their behavior. Omitted fields may inherit defaults; explicit `false`/`auto`/`current`/`none` override them. Only `installer`, `installReason` and `packageSource` accept null resets; the latter two still require advanced-option support. `interaction`, `timeout` and `continueOnError` remain session-wide and are excluded from profiles.

Profiles and automatic authorizer ordering are frozen when processing starts; settings edits cannot change that task or later batch items. A confirmed retry retains the complete confirmed options; an unconfirmed retry reads entry defaults and profiles again. Every retry rechecks sources and signatures without reusing a one-time policy exception. Profiles cannot bypass signatures, blacklists, authorization or Android limits.

After profile negotiation, successful items report `sourceDeleteRequested` as the effective request, not proof of deletion. Explicit host `deleteSource: false` vetoes cleanup; missing or mistyped decisions retain files with notes. The host waits for batch results and retains files or proven aliases shared with failed, unprocessed or retained items. Local external entries also retain repeated batch URIs/normalized paths. `sourceDeleted` reports actual cleanup; the host never deletes content URIs.

******

### Compatibility

******

Platform facts that shape what the plugin can do:

- Android 7.0 (API 24) and later. Device validation and remaining coverage are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- The bypass of the low target SDK block exists from Android 14 (API 34); on older systems the option is ignored and noted in the result.
- The default-installer page distinguishes ordinary preferences and persistent policy. Ordinary preferences use Shizuku or Root and remain subject to ROM restrictions. Persistent policy is available through Dhizuku on API 26-33; API 34+ is rejected before mutation because the owner callback cannot be verified. Root uses a system-UID helper only in user 0 on supported devices. Competing persistent policies are not overwritten. `persistentConfigured` is a receipt of a previous successful configuration, not proof of the current system policy; passive observation reports only `preferred` or `none`.
- `dhizuku`: requires Android 8.0 (API 26)+, an active Dhizuku device/profile owner and permission granted to this plugin. It operates in the current owner user, and installation attribution uses the real owner package. It does not grant shell/root downgrade, test-package, low-targetSdk bypass, other-user, arbitrary installer attribution or keep-data uninstall options. The plugin does not provision an owner.
- Permission-grant requests and non-none DexOpt require Shizuku/Root; verify needs API 26+. Install reason needs API 26+, package source API 33+, and requesting update ownership API 34+. Ownership can start only on initial installation; updates or an existing package in another user may be ignored. False does not revoke an existing owner.
- none/Dhizuku can inspect installed signatures only in the current user; Shizuku/Root query globally. With nonempty SharedUID rules, uncertainty about a package in another user is blocked without an exception.

******

### FAQ

******

- **Why does installation still require confirmation?** `none` always requires Android confirmation. Privileged authorizers may also be subject to Android policy. `notification` opens system confirmation only through its notification action; it does not bypass Android's confirmation.
- **Can an `.aab` be installed?** No. An Android App Bundle is a publishing format; convert it with bundletool into an `.apks` set first. The plugin recognizes `.aab` files and shows their package and module information.
- **Why can a downgrade still fail with `allowDowngrade: true`?** This flag requests a downgrade; Android decides according to the firmware, authorization identity and whether the app is debuggable. Tested user firmware rejected non-debuggable downgrades on Sony G8441 / API 28 and Xiaomi 23046RP50C / API 35, while Sony XQ-DQ72 / API 33 with Root accepted one. These results are device-specific. Check the returned error and `systemMessage`; Root does not guarantee a downgrade on every ROM.
- **What installer package name works on HyperOS?** With Shizuku started through ADB or wireless debugging, leaving the installer package name unset uses `com.android.shell`. On the tested Xiaomi 23046RP50C / HyperOS / API 35, silent new installations and updates recorded this value. Explicit `com.android.shell` and the plugin's own package name were also accepted and recorded as requested. Other package names or ROM versions still depend on the system's response.
- **ColorOS or another ROM says the plugin needs activation. What should I do?** After installation or a force stop, Android can keep an app stopped until user interaction. In AutoJs6's plugin center, use Activate when offered, or open 3-Setup Installer from its launcher icon, then retry. This follows [Android's rules for stopped apps](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS-specific behavior has not yet been verified on a device.
- **Why can a default-installer change fail, or a saved persistent marker differ from the current handler?** The default-installer page distinguishes ordinary preferences and persistent policy. Ordinary preferences use Shizuku or Root and remain subject to ROM restrictions. Persistent policy is available through Dhizuku on API 26-33; API 34+ is rejected before mutation because the owner callback cannot be verified. Root uses a system-UID helper only in user 0 on supported devices. Competing persistent policies are not overwritten. `persistentConfigured` is a receipt of a previous successful configuration, not proof of the current system policy; passive observation reports only `preferred` or `none`.
- **Why was the source not deleted?** Deletion is attempted only after a successful installation. Sources are always retained if installation fails, is cancelled or times out. A deletion failure does not change a successful installation, and an external source provider may refuse deletion. For scripts, the host handles `deleteSource` for paths and `file://` sources, retaining `content://` sources. Check `sourceDeleted` and `notes`. In a batch, confirmed successful items still follow `deleteSource` even if another item fails or the remaining queue is cancelled.
- **Can I retry or resume?** A failed external URI can be retried while the source and access are available. Once the source or its access is released, reopen the package. After a process restart, the restored view shows saved confirmed results and marks unfinished items as interrupted. It is read-only and never automatically installs or retries. Check the installed app before starting again.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the external "Open with" entry only accepts package files and never runs a script.
- REQUEST_INSTALL_PACKAGES and REQUEST_DELETE_PACKAGES support Android confirmation. QUERY_ALL_PACKAGES supports installed-app management, installed-version and signature comparisons, and default-installer detection. The normal permission ENFORCE_UPDATE_OWNERSHIP supports an explicit ownership request; it does not guarantee an owner is assigned.
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC support installation work and temporary source access; POST_NOTIFICATIONS permits notifications. `notification` interaction requires notifications and its channel to be enabled. Other interaction modes tolerate missing notification permission.
- Shizuku, Root and Dhizuku are used for requested operations. The plugin does not provision a device/profile owner. Persistent default rules are changed only through the requested set/clear operation; source packages are never uploaded.
- Installation, inspection, history and app management work offline. INTERNET is used only when you manually check releases at the plugin's fixed GitHub Releases API, with a 12-hour interval. No background update checks or package uploads are performed.
- Package sources are opened read-only. History stores bounded app metadata and outcomes, not package contents or source URIs; error paths are redacted. Private plugin storage is excluded from backups. Deleting a history record does not uninstall its app or remove its source.
- The grant option requests permissions the system can grant, including supported installer-changeable app-ops such as USE_FULL_SCREEN_INTENT on Android 14. It does not guarantee every declared permission or grant accessibility, overlays or arbitrary signature permissions. Restricted/system-fixed/policy-fixed constraints remain, without an extra restricted-permission allowlist flag.
- Signature checks and local exact package-name/SharedUID blacklists apply to every entry point, using `BLOCKED_BY_POLICY`. Only a real dialog can allow a mismatch/unknown signature once for that item; Android still validates it. Silent/notification modes cannot grant this exception, and a blacklist cannot be overridden. Permission preview describes declarations in the selected APK splits, not granted permissions.

After publication, only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

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

`ThreeSetupInstallerPluginService` answers `org.autojs.plugin.INSTALLER` (category `installer`) and implements the host installer-api contract `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.2.1

_2026/10/04_

- `Improvement` Plugin Center icons use the sizes, positions, light and dark artwork, and circular backgrounds adjusted in Icon Studio, retaining reproducible sources and parameters

#### v1.2.0

_2026/10/02_

- `Feature` Advanced installation options: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` and `packageSource`. Unsupported platforms or authorizers reject requests instead of silently ignoring them. `none` adds no manual compilation and does not disable Android compilation.
- `Feature` Signature checks and local exact package-name/SharedUID blacklists apply to every entry point, using `BLOCKED_BY_POLICY`. Only a real dialog can allow a mismatch/unknown signature once for that item; Android still validates it. Silent/notification modes cannot grant this exception, and a blacklist cannot be overridden. Permission preview describes declarations in the selected APK splits, not granted permissions.
- `Feature` Source profiles can be named, enabled, edited and reordered in settings. The first enabled profile matching both origin and actual package-name prefix applies; profiles never stack, and any origin includes Home. A profile supplies partial defaults for 12 per-app options. Explicit requests take precedence, and the real confirmation page can make final changes.
- `Improvement` Advanced script options require AutoJs6 build 5308+ and negotiated V3 with `advanced-install-options`. Without profile overrides, omission preserves existing behavior; explicit `false`/`none` still requires support. The roadmap records official release and device acceptance status.
- `Improvement` Successful results may report observed `updateOwner` and `dexopt`. Null means Android returned no owner to the current calling identity, possibly due to visibility filtering; it does not prove global absence. A failed read omits the field and adds notes. DexOpt states are accepted/failed/cancelled/timeout/unavailable/unknown; accepted includes a system skip and does not prove compilation ran. Failure of this extra step preserves confirmed installation success.
- `Improvement` Permission-grant requests and non-none DexOpt require Shizuku/Root; verify needs API 26+. Install reason needs API 26+, package source API 33+, and requesting update ownership API 34+. Ownership can start only on initial installation; updates or an existing package in another user may be ignored. False does not revoke an existing owner.
- `Improvement` The grant option requests permissions the system can grant, including supported installer-changeable app-ops such as USE_FULL_SCREEN_INTENT on Android 14. It does not guarantee every declared permission or grant accessibility, overlays or arbitrary signature permissions. Restricted/system-fixed/policy-fixed constraints remain, without an extra restricted-permission allowlist flag.
- `Improvement` none/Dhizuku can inspect installed signatures only in the current user; Shizuku/Root query globally. With nonempty SharedUID rules, uncertainty about a package in another user is blocked without an exception.
- `Improvement` Host UI and script profiles require AutoJs6 5312+ and the V3 `source-profiles` feature. Older or non-negotiated requests retain their behavior. Omitted fields may inherit defaults; explicit `false`/`auto`/`current`/`none` override them. Only `installer`, `installReason` and `packageSource` accept null resets; the latter two still require advanced-option support. `interaction`, `timeout` and `continueOnError` remain session-wide and are excluded from profiles.
- `Improvement` Profiles and automatic authorizer ordering are frozen when processing starts; settings edits cannot change that task or later batch items. A confirmed retry retains the complete confirmed options; an unconfirmed retry reads entry defaults and profiles again. Every retry rechecks sources and signatures without reusing a one-time policy exception. Profiles cannot bypass signatures, blacklists, authorization or Android limits.
- `Improvement` After profile negotiation, successful items report `sourceDeleteRequested` as the effective request, not proof of deletion. Explicit host `deleteSource: false` vetoes cleanup; missing or mistyped decisions retain files with notes. The host waits for batch results and retains files or proven aliases shared with failed, unprocessed or retained items. Local external entries also retain repeated batch URIs/normalized paths. `sourceDeleted` reports actual cleanup; the host never deletes content URIs.
- `Improvement` Consistent visual sizing for launcher and Plugin Center icons, with transparent backgrounds and neutral black, white or grayscale artwork
- `Dependency` Upgrade installer-api.aar to contract V3 (MPL 2.0), retaining V1/V2 and all eleven AIDL transactions; advanced script options require host build 5308+. The same V3 contract adds the optional source-profiles feature and profile fields from host 5312, with all eleven AIDL transactions unchanged
- `Dependency` Upgrade the shared package archive parser (MPL 2.0) to verify the actual manifest root and sharedUserId; ambiguous input is rejected. Read permission declarations from real Android-namespace elements, excluding comment and extension-namespace decoys and refusing conflicting compiled attributes.

#### v1.1.0

_2026/10/02_

- `Feature` `dhizuku`: requires Android 8.0 (API 26)+, an active Dhizuku device/profile owner and permission granted to this plugin. It operates in the current owner user, and installation attribution uses the real owner package. It does not grant shell/root downgrade, test-package, low-targetSdk bypass, other-user, arbitrary installer attribution or keep-data uninstall options. The plugin does not provision an owner.
- `Feature` The default-installer page distinguishes ordinary preferences and persistent policy. Ordinary preferences use Shizuku or Root and remain subject to ROM restrictions. Persistent policy is available through Dhizuku on API 26-33; API 34+ is rejected before mutation because the owner callback cannot be verified. Root uses a system-UID helper only in user 0 on supported devices. Competing persistent policies are not overwritten. `persistentConfigured` is a receipt of a previous successful configuration, not proof of the current system policy; passive observation reports only `preferred` or `none`.
- `Feature` `interaction: 'notification'` is an installation mode: notifications carry the initial confirmation, cancellation, progress and results without plugin installation dialogs. Android confirmation still requires a notification tap. Notification permission, the app's notifications and its installation channel must be enabled; otherwise the request fails with `NOTIFICATION_UNAVAILABLE`. Notification denial does not block the other interaction modes. Uninstallation does not accept `notification`.
- `Improvement` `none` uses Android confirmation. Fresh settings try usable `shizuku -> root -> dhizuku -> none` for `auto`; each method can be reordered or disabled. Existing saved three-method orders keep their relative order and enabled choices, with Dhizuku inserted before `none` but disabled. An explicit authorizer never falls back.
- `Improvement` Settings save authorizer order/enabled methods, installation options and notification preferences. Home/external installation defaults to `dialog`, with explicit `auto`, `silent` or `notification` choices available. Host UI installation actions use `dialog`; scripts retain explicit options and still default to `auto`. Changes are saved after confirmation.
- `Dependency` Add Dhizuku API 2.6.0 (MIT) for the device/profile-owner authorizer
- `Dependency` Upgrade `installer-api.aar` to contract V2 (MPL 2.0), retaining V1 negotiation and appending the persistent-default method; provenance and SHA-256 are in Third-Party Notices; AutoJs6 >= 6.8.0 (5307).

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

Developers can build and verify the plugin with the commands below. Before the official release, use a maintainer-provided build or a local build for testing; published APKs will be distributed through Releases and the plugin center after indexing.

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
