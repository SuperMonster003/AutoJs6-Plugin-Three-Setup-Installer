3-Setup Installer installs, updates, inspects and uninstalls Android apps through AutoJs6 installation entries and external package-opening or sharing requests. It supports ordinary Android confirmation and privileged installation through Shizuku or Root. The script API and standalone home and settings pages are still planned.

1.0.0: P3 development preview. Confirmation, progress, results and batch dialogs, external opening and sharing, optional source deletion, system confirmation and foreground notifications are implemented. After a process restart, the restored view shows saved confirmed results and marks unfinished items as interrupted. It is read-only and never automatically installs or retries. The script API, standalone home and settings, installation history and default-installer configuration remain planned. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for progress and device coverage. AutoJs6 >= 6.8.0 (5299).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) on a device with AutoJs6 build 5299 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Setup Installer` is recognized, and enable it.
3. Use an installation action in AutoJs6, or choose 3-Setup Installer when opening or sharing package files. When a confirmation dialog appears, review the app and options before installing. Prepare Shizuku or Root authorization when selecting a privileged method.

### Authorizers

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: needs Shizuku running (started through wireless debugging, ADB or Root) and permission granted to the plugin. Its shell privileges support silent installation and uninstallation and operations for other users.
- `root`: needs a Root manager that grants `su` to the plugin; provides the same operations as Shizuku through a libsu root service. Downgrades on regular (user) firmware still succeed only for debuggable apps, which is a framework rule, not a plugin limit.
- **Note:** With privileges available, host requests using `interaction: 'auto'` install silently without proactively opening confirmation. If Android still requires confirmation, `auto` permits it and records this in `notes`. Use `interaction: 'dialog'` to request confirmation before installation, or `interaction: 'silent'` to fail when system confirmation is required. The planned script API follows the same default.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for the installation guide and the current progress.
