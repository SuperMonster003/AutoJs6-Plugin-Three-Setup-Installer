******

### Release History

******

# v1.1.0

###### 2026/10/02

* `Feature` `dhizuku`: requires Android 8.0 (API 26)+, an active Dhizuku device/profile owner and permission granted to this plugin. It operates in the current owner user, and installation attribution uses the real owner package. It does not grant shell/root downgrade, test-package, low-targetSdk bypass, other-user, arbitrary installer attribution or keep-data uninstall options. The plugin does not provision an owner.
* `Feature` The default-installer page distinguishes ordinary preferences and persistent policy. Ordinary preferences use Shizuku or Root and remain subject to ROM restrictions. Persistent policy is available through Dhizuku on API 26-33; API 34+ is rejected before mutation because the owner callback cannot be verified. Root uses a system-UID helper only in user 0 on supported devices. Competing persistent policies are not overwritten. `persistentConfigured` is a receipt of a previous successful configuration, not proof of the current system policy; passive observation reports only `preferred` or `none`.
* `Improvement` `none` uses Android confirmation. Fresh settings try usable `shizuku -> root -> dhizuku -> none` for `auto`; each method can be reordered or disabled. Existing saved three-method orders keep their relative order and enabled choices, with Dhizuku inserted before `none` but disabled. An explicit authorizer never falls back.
* `Dependency` Add Dhizuku API 2.6.0 (MIT) for the device/profile-owner authorizer
* `Dependency` Upgrade `installer-api.aar` to contract V2 (MPL 2.0), retaining V1 negotiation and appending the persistent-default method; provenance and SHA-256 are in Third-Party Notices; AutoJs6 >= 6.8.0 (5307).

# v1.0.0

###### 2026/10/02

* `Hint` 1.0.0 documents the implemented installation, app-management and script features below. The official GitHub Release and plugin-center listing are still pending. Host integration requires AutoJs6 >= 6.8.0 (5299), and the `installer` script API requires build 5300 or later. Device coverage and remaining acceptance work are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
* `Feature` 3-Setup Installer installs, updates, inspects and uninstalls Android apps from its standalone home screen, AutoJs6 entries and scripts, or external package-opening and sharing requests. It supports Android confirmation and privileged operations through Shizuku or Root
* `Feature` Package formats: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` and ZIP archives that contain APKs; split packages are selected for the device; `.aab` files are recognized and described but not installed
* `Feature` Optional source deletion after successful installation is best effort. Downgrades, test packages, low target SDK bypass (Android 14+), installer attribution and other target users require Shizuku or Root and remain subject to Android restrictions
* `Feature` The `installer` script API (alias `$installer`) provides synchronous, `...Async` and session forms for single, batch and split installation, uninstallation, inspection, authorizer and user queries, and default-installer settings. Failures are `InstallerError` objects with stable `code` values (requires AutoJs6 >= 6.8.0 (5300)). Scripts default to `interaction: 'auto'` and install silently when privileges are available. Host UI installation actions use `dialog`. If Android requires confirmation, `auto` permits it and records this in `notes`. Choose `interaction: 'dialog'` for confirmation before installation. Explicit `silent` fails with `AUTHORIZER_REQUIRED` if privileges are unavailable or system confirmation is required
* `Feature` The standalone home shows Shizuku/Root availability and authorization, the current default installer, active tasks and recent installations. Choose multiple packages with the system document picker to install serially, continue after individual failures or cancel remaining items
* `Feature` Confirmation shows app details, old and new versions, signatures and selectable APK components. Progress supports cancellation; results show success actions or error details with copying. Batch installation shows each item separately
* `Feature` Foreground installation progress, cancellation and result notifications. Denying notification permission does not prevent installation
* `Feature` Open or share one or multiple installation packages, including APKS files shared by MT Manager. Multiple packages enter a serial queue. Failed external items can be retried while their URI and access remain available
* `Feature` Installed-app management searches by label or package name, sorts by name, installation time or update time, and can include system apps. Open an app or its system details, or review and confirm uninstallation. Shizuku or Root can uninstall without a further system prompt and optionally retain data; other cases use Android confirmation
* `Feature` The home status card and settings open the same default-installer page, with privileged set/clear actions and system-settings guidance when privileges are unavailable. OEM policies may prevent a change or require clearing the previous handler. Scripts retain `installer.isDefault`, `installer.setDefault` and `setDefaultAsync`; results reflect the device response
* `Feature` Settings save authorizer order/enabled methods, installation options and progress-notification preferences. Local home/external installation defaults to `dialog`; explicitly saved `auto` or `silent` choices take effect. Host/script requests retain their explicit options, and the script API still defaults to `auto`. Picker changes are saved only after confirmation
* `Feature` Appearance settings cover language, night mode, theme color and launcher icon. The first three follow AutoJs6 by default and support local overrides; host unavailability falls back to system language/night and the default color. Launcher icons offer light, dark, automatic and transparent modes; automatic follows the system, subject to launcher caching and masks
* `Feature` Private installation history retains up to 200 items, including package, label, old/new versions, outcome, time, origin (host/script/external/home), authorizer and failure details. Delete individual records or clear the history without uninstalling apps or deleting source files. After process death, unfinished items become cancelled and never resume automatically
* `Feature` About and the built-in release history are available from settings in ten languages. Manual update checks use the plugin's GitHub Releases API with a 12-hour interval, cached results and ignored-version management. Release pages open in the browser; updates are not downloaded or installed automatically
* `Feature` README, plugin-center instructions and changelog in 10 languages
* `Improvement` Seekable sources avoid a full cache copy, while streams are staged as needed. ZIP split packages are supported, AAB files support inspection only, and changed sources are rejected
* `Improvement` Deletion is attempted only after a successful installation. Sources are always retained if installation fails, is cancelled or times out. A deletion failure does not change a successful installation, and an external source provider may refuse deletion. For scripts, the host handles `deleteSource` for paths and `file://` sources, retaining `content://` sources. Check `sourceDeleted` and `notes`. In a batch, confirmed successful items still follow `deleteSource` even if another item fails or the remaining queue is cancelled
* `Improvement` Serialize concurrent installations of the same package across users and authorizers, retain cancellation and timeouts while waiting, and safely reclaim inactive staging directories after 24 hours
* `Improvement` Retry an interrupted privileged connection once while establishing it; never automatically repeat installation or uninstallation that has already started
* `Dependency` Add Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) for the Shizuku authorizer
* `Dependency` Add libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) for the Root authorizer
* `Dependency` Add AndroidHiddenApiBypass 6.1 for the hidden package installer APIs used by the privileged service
* `Dependency` Add `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5298, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Add `installer-api.aar` (AutoJs6, MPL 2.0) for the installation contract; artifact provenance and SHA-256 are listed in Third-Party Notices
* `Dependency` Add `package-archive-parser.aar` (AutoJs6, MPL 2.0) for APK and container inspection and split selection; artifact provenance and SHA-256 are listed in Third-Party Notices
