3-Setup Installer installs, updates, inspects and uninstalls Android apps from its standalone home screen, AutoJs6 entries and scripts, or external package-opening and sharing requests. It supports Android confirmation and privileged operations through Shizuku or Root.

1.0.0: Development preview with standalone home, settings, installed-app management, serial queues and installation history. Installation confirmation, progress, results and foreground notifications are available. A process restart retains saved confirmed results and marks unfinished tasks cancelled; it never automatically resumes or retries installation. The `installer` script API requires AutoJs6 >= 6.8.0 (5300); base host integration requires build 5299. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for device coverage and remaining acceptance work.

### Usage

1. Install the plugin APK from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) page on Android 7.0 or later. The standalone launcher opens its home screen.
2. On Home, check authorization and the default installer, then use the add button to choose one or multiple packages. Review the installation dialog; follow active progress and recent history on Home.
3. For AutoJs6 integration, use build 5299 (6.8.0) or later and enable `3-Setup Installer` in the plugin center. The script API requires build 5300 or later.
4. Use an installation action in AutoJs6, or choose 3-Setup Installer when opening or sharing package files. When a confirmation dialog appears, review the app and options before installing. Prepare Shizuku or Root authorization when selecting a privileged method.
5. Use the Home menu for installed apps and settings. Review local installation defaults, appearance, launcher icon and notifications; About, release history and manual update checks are in settings.

### Authorizers

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: needs Shizuku running (started through wireless debugging, ADB or Root) and permission granted to the plugin. Its shell privileges support silent installation and uninstallation and operations for other users.
- `root`: needs a Root manager that grants `su` to the plugin; provides the same operations as Shizuku through a libsu root service. Downgrades on regular (user) firmware still succeed only for debuggable apps, which is a framework rule, not a plugin limit.
- **Note:** With privileges available, host requests using `interaction: 'auto'` install silently without proactively opening confirmation. If Android still requires confirmation, `auto` permits it and records this in `notes`. Use `interaction: 'dialog'` to request confirmation before installation, or `interaction: 'silent'` to fail when system confirmation is required. The script API follows the same default.
- Settings save authorizer order/enabled methods, installation options and progress-notification preferences. Local home/external installation defaults to `dialog`; explicitly saved `auto` or `silent` choices take effect. Host/script requests retain their explicit options, and the script API still defaults to `auto`. Picker changes are saved only after confirmation.

### Permissions and Security

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the external "Open with" entry only accepts package files and never runs a script.
- REQUEST_INSTALL_PACKAGES and REQUEST_DELETE_PACKAGES support Android confirmation. QUERY_ALL_PACKAGES supports installed-app management, installed-version and signature comparisons, and default-installer detection.
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC support background installation work; POST_NOTIFICATIONS allows progress and result notifications. Missing notification permission does not block installation.
- Shizuku and Root are used only for the operation you start; the privileged service holds no state, keeps no shell open between operations and is never reached from outside the plugin.
- Installation, inspection, history and app management work offline. INTERNET is used only when you manually check releases at the plugin's fixed GitHub Releases API, with a 12-hour interval. No background update checks or package uploads are performed.
- Package sources are opened read-only. History stores bounded app metadata and outcomes, not package contents or source URIs; error paths are redacted. Private plugin storage is excluded from backups. Deleting a history record does not uninstall its app or remove its source.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for the installation guide and the current progress.
