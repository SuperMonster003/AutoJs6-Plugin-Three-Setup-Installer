******

### Release History

******

# v1.0.0

###### 2026/09/30

* `Hint` P0 development preview: the repository skeleton, the plugin identity recognized by the AutoJs6 plugin center, and the privileged installation spike. The Binder contract, the installer engine, the dialogs, the script API and the settings page follow the phases of ROADMAP.md.
* `Feature` Plugin identity `three-setup-installer` (engine `installer`) with the INFO service, the Wake Activity and the `org.autojs.plugin.INSTALLER` service skeleton for host discovery
* `Feature` README, plugin-center instructions and changelog in 10 languages
* `Dependency` Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) for the Shizuku authorizer
* `Dependency` libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) for the Root authorizer
* `Dependency` AndroidHiddenApiBypass 6.1 for the hidden package installer APIs used by the privileged service
* `Dependency` `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5298, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
