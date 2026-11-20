3-Setup Installer installs, updates, inspects and uninstalls Android apps. Installations can start from its home screen, from AutoJs6 install actions and scripts, or from packages that other apps open or share; Android confirms each one by default, and with Shizuku, Root or Dhizuku authorized it can also install silently. It also works on its own without AutoJs6.

### Getting started

1. Open 3-Setup Installer from the launcher. For silent installation, tap "Authorize" for Shizuku, Root or Dhizuku under "Authorization" on the home screen.
2. Tap "Choose installation packages" and pick one or more APK, APKS, XAPK or similar packages.
3. Check the version, signature and permissions in the installation dialog, then tap "Install".
4. Once this plugin is enabled in AutoJs6 "Plugin Center", install actions in AutoJs6 show the same dialog; 3-Setup Installer can also be chosen when another app opens or shares a package.

### Main features

- **Installation dialog**: shows the version change, size, SDK and signature comparison, and marks a new install, update, reinstall or downgrade; APK components, permissions and "More options" can be expanded and adjusted.
- **Authorization**: without authorization Android confirms each installation; with Shizuku, Root or Dhizuku it can install silently, allow downgrades, grant permissions and more. "Authorization order and availability" in the settings sets the order of automatic selection.
- **Batches and records**: several apps can be installed at once, and the home screen shows running tasks and "Recent installations"; records keep only app information and results, never the packages.
- **Installed apps**: opened from the "More" menu on the home screen, to search, sort, view app information or uninstall apps.
- **Default installer**: makes this plugin the default app for opening APK files; whether this is possible depends on the authorization and system restrictions.
- **Installation profiles and blacklists**: preset installation options in the settings by installation source and package name prefix, or block specific apps with "Package blacklist" and "Shared-UID blacklist".
- **Signature check**: a package whose signature differs from the installed app is blocked by default and can only be allowed once, for that item, in the installation dialog.
- **Settings**: adjust "Default interaction", appearance, the launcher icon and notifications, or view "Release history" and run "Check for updates" manually.

### Using it in scripts

AutoJs6 scripts install, inspect and uninstall apps through the `installer` module. The example below shows the installation dialog and installs after the user confirms; synchronous methods must be called on a script thread, not on the UI thread:

```js
console.log(installer.status);
try {
    let result = installer.install('./app.apk', { interaction: 'dialog' });
    console.log(result.packageName, result.versionName);
} catch (e) {
    console.error(e.code, e.message);
}
```

- Without `interaction` the mode is `auto`: it installs silently when a privileged authorization is available, otherwise Android asks for confirmation.
- Passing an array installs in a batch and returns results in the order of the sources; check `ok` and `error` for each item. `installAsync` returns a Promise and can be used on the UI thread.
- `installer.inspect(source)` only reads the package name, version and other information, without installing.
- See [AutoJs6 documentation > installer](https://docs.autojs6.com/#/installer) for all options and events.

### Requirements

- Android 7.0 or later. Working with AutoJs6 requires AutoJs6 6.8.0 or later; standalone use does not need AutoJs6.
- Shizuku, Root and Dhizuku are all optional and only needed for silent installation or privileged options; Dhizuku requires Android 8.0 or later.
- The install and uninstall permissions request Android confirmation, the app list permission serves installed apps, versions and signature comparison, and the notification permission serves notification-based installation and progress notifications.
- The network is used only when checking for plugin updates manually; packages are never uploaded.

### FAQ

**Why confirmation is still required after authorization**

The policy of Android or the device vendor may still require user confirmation. In that case "Automatic" falls back to Android confirmation, while a silent mode fails and explains why.

**Why accessibility services cannot be turned on after installation**

Since Android 13, apps installed as local files cannot use accessibility and other restricted settings; installations through Shizuku or Root are not marked as local files by default. For a trusted app, allow restricted settings from the top-right menu of its app info page in the system settings, then turn on its accessibility service; the entry varies by system.

**Why .aab files cannot be installed**

An Android App Bundle is a publishing format and cannot be installed directly. Convert it to an .apks package with bundletool first; the plugin can show the package name and modules of an .aab file.

**Why a downgrade fails even with downgrades allowed**

"Allow version downgrade" only asks the system; whether it is accepted depends on the system version, the authorization and whether the app is debuggable. When it fails, the result includes the message returned by the system.

See the [project homepage](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) for details on authorization, installation options and permissions.
