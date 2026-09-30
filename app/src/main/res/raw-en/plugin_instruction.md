3-Setup Installer takes over the package installer of AutoJs6: the install buttons of the file manager, the plugin center and the packaged-script builder, the external "Open with" entry for `.apk`, `.apks`, `.xapk`, `.apkm` and `.apkz` files, and the script-side global object `installer` for installing, updating, inspecting and uninstalling apps. Besides the regular system confirmation, it can install and uninstall silently through Shizuku or Root.

Version 1.0.0 is the P0 development preview: the repository skeleton, the plugin identity recognized by the AutoJs6 plugin center, and the privileged-installation spike. The Binder contract, the installer engine, the dialogs, the script API and the settings page follow the phases of [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Requires AutoJs6 6.8.0 (build 5298) or later. P0 validation completed for silent installation, updates, uninstallation and ordinary default-installer selection with Shizuku and Root. Host and script installation entry points are not available yet; persistent defaults remain outside this release.

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) on a device with AutoJs6 build 5298 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Setup Installer` is recognized, and enable it.
3. Tap a package file in the AutoJs6 file manager, open a package from any file manager with 3-Setup Installer, or call `installer.install(...)` from a script. For silent installation, start Shizuku or grant Root when the plugin asks, or choose the authorizer in the plugin settings.

### Authorizers

- `none`: the standard PackageInstaller session; Android asks the user to confirm every installation, split packages are supported, and privileged options are not available.
- `shizuku`: needs the Shizuku app running (started through wireless debugging, ADB or Root) and the permission granted to the plugin; runs with shell rights, which allow silent installation, silent uninstallation, other users and the default-installer lock.
- `root`: needs a Root manager that grants `su` to the plugin; provides the same operations as Shizuku through a libsu root service. Downgrades on regular (user) firmware still succeed only for debuggable apps, which is a framework rule, not a plugin limit.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) for the installation guide and the current progress.
