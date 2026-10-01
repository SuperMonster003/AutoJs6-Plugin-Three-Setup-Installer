3-Setup Installer installs, updates, inspects and uninstalls Android apps from its standalone home screen, AutoJs6 entries and scripts, or external package-opening and sharing requests. It supports Android confirmation and privileged operations through Shizuku or Root.

1.0.0 documents the implemented installation, app-management and script features below. The official GitHub Release and plugin-center listing are still pending. Host integration requires AutoJs6 >= 6.8.0 (5299), and the `installer` script API requires build 5300 or later. Device coverage and remaining acceptance work are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

### Usage

1. On Android 7.0 or later, install the official APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) once published, or use the AutoJs6 plugin-center installation wizard once the official index lists it. Before publication, use a maintainer-provided build or build from source for testing. Open the launcher icon to reach the standalone Home screen.
2. On Home, check authorization and the default installer, then use the add button to choose one or multiple packages. Review the installation dialog; follow active progress and recent history on Home.
3. For AutoJs6 integration, use build 5299 (6.8.0) or later and enable `3-Setup Installer` in the plugin center. The script API requires build 5300 or later.
4. Use an installation action in AutoJs6, or choose 3-Setup Installer when opening or sharing package files. When a confirmation dialog appears, review the app and options before installing. Prepare Shizuku or Root authorization when selecting a privileged method.
5. Use the Home menu for installed apps and settings. Review local installation defaults, appearance, launcher icon and notifications; About, release history and manual update checks are in settings.

### Authorizers

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: requires Shizuku running (started through wireless debugging, ADB or Root) and permission granted specifically to 3-Setup Installer. Permission granted to AutoJs6 does not authorize this plugin. Installation, uninstallation and other-user operations use the privileges of the running Shizuku service.
- `root`: requires a rooted device and a Root manager that grants `su` to 3-Setup Installer. It provides privileged installation, uninstallation and user/default-installer operations through libsu. Android and ROM policies still decide whether each requested operation is allowed.
- **Note:** Scripts default to `interaction: 'auto'` and install silently when privileges are available. Host UI installation actions use `dialog`. If Android requires confirmation, `auto` permits it and records this in `notes`. Choose `interaction: 'dialog'` for confirmation before installation. Explicit `silent` fails with `AUTHORIZER_REQUIRED` if privileges are unavailable or system confirmation is required.
- Settings save authorizer order/enabled methods, installation options and progress-notification preferences. Local home/external installation defaults to `dialog`; explicitly saved `auto` or `silent` choices take effect. Host/script requests retain their explicit options, and the script API still defaults to `auto`. Picker changes are saved only after confirmation.

### Compatibility

- Android 7.0 (API 24) and later. Device validation and remaining coverage are recorded in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- The bypass of the low target SDK block exists from Android 14 (API 34); on older systems the option is ignored and noted in the result.
- ROM policies and existing defaults may restrict changes to the default installer. Version 1.0.0 does not promise a persistent lock; see the FAQ for installer package names and plugin activation.

### FAQ

- **Why does installation still require confirmation?** `none` always uses system confirmation. Choose Shizuku or Root in the installation dialog after preparing its authorization. Android or device policy may still require a system prompt.
- **Can an `.aab` be installed?** No. An Android App Bundle is a publishing format; convert it with bundletool into an `.apks` set first. The plugin recognizes `.aab` files and shows their package and module information.
- **Why can a downgrade still fail with `allowDowngrade: true`?** This flag requests a downgrade; Android decides according to the firmware, authorization identity and whether the app is debuggable. Tested user firmware rejected non-debuggable downgrades on Sony G8441 / API 28 and Xiaomi 23046RP50C / API 35, while Sony XQ-DQ72 / API 33 with Root accepted one. These results are device-specific. Check the returned error and `systemMessage`; Root does not guarantee a downgrade on every ROM.
- **What installer package name works on HyperOS?** With Shizuku started through ADB or wireless debugging, leaving the installer package name unset uses `com.android.shell`. On the tested Xiaomi 23046RP50C / HyperOS / API 35, silent new installations and updates recorded this value. Explicit `com.android.shell` and the plugin's own package name were also accepted and recorded as requested. Other package names or ROM versions still depend on the system's response.
- **ColorOS or another ROM says the plugin needs activation. What should I do?** After installation or a force stop, Android can keep an app stopped until user interaction. In AutoJs6's plugin center, use Activate when offered, or open 3-Setup Installer from its launcher icon, then retry. This follows [Android's rules for stopped apps](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS-specific behavior has not yet been verified on a device.
- **Why can setting the default installer fail?** A ROM can refuse the change. On older Android versions, an existing APK default can require clearing the previous handler in system settings first; follow the page's guidance. If the system offers no clear-default action, the plugin cannot guarantee replacement. Version 1.0.0 does not promise a persistent lock, even with Shizuku or Root.
- **Why was the source not deleted?** Deletion is attempted only after a successful installation. Sources are always retained if installation fails, is cancelled or times out. A deletion failure does not change a successful installation, and an external source provider may refuse deletion. For scripts, the host handles `deleteSource` for paths and `file://` sources, retaining `content://` sources. Check `sourceDeleted` and `notes`. In a batch, confirmed successful items still follow `deleteSource` even if another item fails or the remaining queue is cancelled.
- **Can I retry or resume?** A failed external URI can be retried while the source and access are available. Once the source or its access is released, reopen the package. After a process restart, the restored view shows saved confirmed results and marks unfinished items as interrupted. It is read-only and never automatically installs or retries. Check the installed app before starting again.

### Permissions and Security

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the external "Open with" entry only accepts package files and never runs a script.
- REQUEST_INSTALL_PACKAGES and REQUEST_DELETE_PACKAGES support Android confirmation. QUERY_ALL_PACKAGES supports installed-app management, installed-version and signature comparisons, and default-installer detection.
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC support background installation work; POST_NOTIFICATIONS allows progress and result notifications. Missing notification permission does not block installation.
- Shizuku and Root are used only for the operation you start; the privileged service holds no state, keeps no shell open between operations and is never reached from outside the plugin.
- Installation, inspection, history and app management work offline. INTERNET is used only when you manually check releases at the plugin's fixed GitHub Releases API, with a 12-hour interval. No background update checks or package uploads are performed.
- Package sources are opened read-only. History stores bounded app metadata and outcomes, not package contents or source URIs; error paths are redacted. Private plugin storage is excluded from backups. Deleting a history record does not uninstall its app or remove its source.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for the installation guide and the current progress.
