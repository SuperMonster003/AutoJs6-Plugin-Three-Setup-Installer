******

### Release History

******

# v1.0.0

###### 2026/10/01

* `Hint` P2 development preview: core installation, inspection, user queries and uninstallation are connected to the host service, with explicit confirmation and automatic session cleanup. Full host-entry validation, the complete interface, external opening, default-installer activation, the script API and settings remain in progress.
* `Feature` Plugin identity `three-setup-installer` (engine `installer`) with the INFO service, the Wake Activity and the `org.autojs.plugin.INSTALLER` service skeleton for host discovery
* `Feature` README, plugin-center instructions and changelog in 10 languages
* `Improvement` P0 validation completed for silent installation, updates, uninstallation and ordinary default-installer selection with Shizuku and Root. Host and script installation entry points are not available yet; persistent defaults remain outside this release.
* `Improvement` The plugin id, engine, service action / category, Binder descriptor and minimum host version now come from the host installer-api contract constants; the capabilities declare installer contract version 1 and the minimum host build is back-filled to 5299
* `Improvement` Seekable sources avoid a full cache copy, while streams are staged as needed. ZIP split packages are supported, AAB files support inspection only, and changed sources are rejected.
* `Improvement` Explicit authorization choices never fall back. Refusal, timeouts and incompatibility are distinguished, and concurrent requests share authorization and privileged connections.
* `Improvement` Core installation and updates use system confirmation, Shizuku or Root, with cancellation and results that reflect the actual confirmation and system response.
* `Improvement` Core uninstallation supports system confirmation, Shizuku and Root, with optional data retention when using a privileged authorizer.
* `Improvement` Serial batch installation can continue after failures or cancel remaining items, and supports validating and selecting target users with privileges.
* `Improvement` Host service requests support inspection, installation, uninstallation and user queries, with explicit confirmation, cancellation when callers exit, up to four concurrent sessions and automatic cleanup.
* `Improvement` Installation dialogs follow AutoJs6 language, night mode and theme color, with a fallback when the host is unavailable and layouts that support large text and RTL.
* `Dependency` Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) for the Shizuku authorizer
* `Dependency` libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) for the Root authorizer
* `Dependency` AndroidHiddenApiBypass 6.1 for the hidden package installer APIs used by the privileged service
* `Dependency` `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5298, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` `package-archive-parser.aar` and `installer-api.aar` (AutoJs6 modules `plugin-api/package-archive-parser` and `plugin-api/installer-api`, host P1 build 6.8.0 / 5299, MPL 2.0), hash-locked in `locks/host-api-aars.lock` together with `common-plugin-api.aar`
* `Dependency` Refresh the bundled package archive parser to recognize ordinary ZIP split containers
