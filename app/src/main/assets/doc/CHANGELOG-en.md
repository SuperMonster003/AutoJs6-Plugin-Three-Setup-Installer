******

### Release History

******

# v1.0.0

###### 2026/10/01

* `Hint` Development preview with standalone home, settings, installed-app management, serial queues and installation history. Installation confirmation, progress, results and foreground notifications are available. A process restart retains saved confirmed results and marks unfinished tasks cancelled; it never automatically resumes or retries installation. The `installer` script API requires AutoJs6 >= 6.8.0 (5300); base host integration requires build 5299. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for device coverage and remaining acceptance work
* `Feature` 3-Setup Installer installs, updates, inspects and uninstalls Android apps from its standalone home screen, AutoJs6 entries and scripts, or external package-opening and sharing requests. It supports Android confirmation and privileged operations through Shizuku or Root
* `Feature` README, plugin-center instructions and changelog in 10 languages
* `Feature` The `installer` script API (alias `$installer`) provides synchronous, `...Async` and session forms for single, batch and split installation, uninstallation, inspection, authorizer and user queries, and default-installer settings. Failures are `InstallerError` objects with stable `code` values (requires AutoJs6 >= 6.8.0 (5300))
* `Feature` The standalone home shows Shizuku/Root availability and authorization, the current default installer, active tasks and recent installations. Choose multiple packages with the system document picker to install serially, continue after individual failures or cancel remaining items
* `Feature` Private installation history retains up to 200 items, including package, label, old/new versions, outcome, time, origin (host/script/external/home), authorizer and failure details. Delete individual records or clear the history without uninstalling apps or deleting source files. After process death, unfinished items become cancelled and never resume automatically
* `Feature` Installed-app management searches by label or package name, sorts by name, installation time or update time, and can include system apps. Open an app or its system details, or review and confirm uninstallation. Shizuku or Root can uninstall without a further system prompt and optionally retain data; other cases use Android confirmation
* `Feature` Settings save authorizer order/enabled methods, installation options and progress-notification preferences. Local home/external installation defaults to `dialog`; explicitly saved `auto` or `silent` choices take effect. Host/script requests retain their explicit options, and the script API still defaults to `auto`. Picker changes are saved only after confirmation
* `Feature` The home status card and settings open the same default-installer page, with privileged set/clear actions and system-settings guidance when privileges are unavailable. OEM policies may prevent a change or require clearing the previous handler. Scripts retain `installer.isDefault`, `installer.setDefault` and `setDefaultAsync`; results reflect the device response
* `Feature` About and the built-in release history are available from settings in ten languages. Manual update checks use the plugin's GitHub Releases API with a 12-hour interval, cached results and ignored-version management. Release pages open in the browser; updates are not downloaded or installed automatically
* `Fix` Cancel actions did not follow the plugin language on devices missing the corresponding system translation
* `Fix` Required split APKs such as base.apk now retain a visible checkmark when disabled in both light and dark themes
* `Fix` Package containers opened from Files by Google and other content providers with opaque URIs now appear in the installer chooser even when the provider uses a generic ZIP or binary MIME type
* `Improvement` The plugin id, engine, service action / category, Binder descriptor and minimum host version now come from the host installer-api contract constants; the capabilities declare installer contract version 1 and the minimum host build is back-filled to 5299
* `Improvement` Seekable sources avoid a full cache copy, while streams are staged as needed. ZIP split packages are supported, AAB files support inspection only, and changed sources are rejected.
* `Improvement` Explicit authorization choices never fall back. Refusal, timeouts and incompatibility are distinguished, and concurrent requests share authorization and privileged connections.
* `Improvement` Core installation and updates use system confirmation, Shizuku or Root, with cancellation and results that reflect the actual confirmation and system response.
* `Improvement` Core uninstallation supports system confirmation, Shizuku and Root, with optional data retention when using a privileged authorizer.
* `Improvement` Serial batch installation can continue after failures or cancel remaining items, and supports validating and selecting target users with privileges.
* `Improvement` Host service requests support inspection, installation, uninstallation and user queries, with explicit confirmation, cancellation when callers exit, up to four concurrent sessions and automatic cleanup.
* `Improvement` Appearance settings cover language, night mode, theme color and launcher icon. The first three follow AutoJs6 by default and support local overrides; host unavailability falls back to system language/night and the default color. Launcher icons offer light, dark, automatic and transparent modes; automatic follows the system, subject to launcher caching and masks
* `Improvement` Background installation now has foreground progress, cancellation and result notifications. Denying notification permission does not block installation.
* `Improvement` Added installation confirmation, progress and result dialogs with app details, APK component selection, options, error copying and per-item batch status. After a process restart, the restored view shows saved confirmed results and marks unfinished items as interrupted. It is read-only and never automatically installs or retries.
* `Improvement` System installation confirmation now handles unknown-source permission guidance and interruption. Privileged uninstallation shows app details and a keep-data choice before confirmation.
* `Improvement` Open or share one or multiple packages, retry failed external sources while access remains available, and optionally attempt source deletion after success. Deletion refusal preserves the successful installation result.
* `Improvement` Opening APKS packages shared by MT Manager supports the application/vnd.android.package-archives MIME type
* `Dependency` Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) for the Shizuku authorizer
* `Dependency` libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) for the Root authorizer
* `Dependency` AndroidHiddenApiBypass 6.1 for the hidden package installer APIs used by the privileged service
* `Dependency` `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5298, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` `package-archive-parser.aar` and `installer-api.aar` (AutoJs6 modules `plugin-api/package-archive-parser` and `plugin-api/installer-api`, MPL 2.0), hash-locked in `locks/host-api-aars.lock` together with `common-plugin-api.aar`
* `Dependency` Refresh the bundled package archive parser to recognize ordinary ZIP split containers
