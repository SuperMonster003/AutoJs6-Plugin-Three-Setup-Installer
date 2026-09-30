# P1 evidence: shared parser, installer contract, host client and host installer removal

Companion of `ROADMAP.md` phase P1. Dates are local (GMT+08:00). Host repository `D:/idea-projects/AutoJs6`,
baseline `master@c8047fa1bf` (6.8.0 / 5298); the P1 line is host build 5299, commit `3c31c686e6`.

## P1.1 Shared package archive parser (2026-09-30)

| Item | Value |
| --- | --- |
| Module | `plugin-api/package-archive-parser`, namespace `org.autojs.plugin.packagearchive`, Android library without AIDL / resources / BuildConfig, `implementation(libs.gson)` |
| Sources moved (`git mv`, package line changed, every `internal` made public) | `AndroidPackageArchive.kt`, `BundletoolTocDecoder.kt`, `ApkManifestDisplayDecoder.kt`, `AabManifestDisplayDecoder.kt`, `ApkSignatureDetector.kt`, `PackageInspectionLimits.kt` |
| Tests moved | `AndroidPackageArchiveInspectorTest`, `BundletoolTocDecoderTest`, `ApkManifestDisplayDecoderTest`, `AabManifestDisplayDecoderTest`, `ApkSignatureDetectorTest`, `LargePackageInspectionTest` |
| Host consumers with explicit imports | `ApkInfoDialogManager`, `DisplayManifestActivity`, `AndroidPackageSessionInstaller` (deleted again in P1.4), `PackageInstallerActivity` (deleted again in P1.4), debug `ActivityLaunchCompatibilityProbe`, `AndroidPackageArchiveDeviceTest` |
| `:plugin-api:package-archive-parser:testDebugUnitTest` | 52 tests, 0 failures (8 + 11 + 12 + 3 + 16 + 2) |
| Release AAR | `package-archive-parser-release.aar`, 262,639 bytes, SHA-256 `d979659e6ea8c03a248ba841bf2b78b833697149e36fe1416ee52fc046ff2ab6`, staged as `libs/package-archive-parser.aar` |
| APK Inspector comparison | not folded back in this phase; `docs/dev/package-archive-parser-v1.md` (host) starts the decision table for P8 |

## P1.2 Installer contract module (2026-09-30)

| Item | Value |
| --- | --- |
| Module | `plugin-api/installer-api`, namespace `org.autojs.plugin.installer.api`, `aidl = true`, `api(project(":plugin-api:common-plugin-api"))`, consumer rules keep the package |
| AIDL (declaration order frozen) | `IInstallerPlugin` (10 methods), `IInstallerSession` (4), `IInstallerSessionCallback` (4, oneway), `IInstallerCallback` (2, oneway) |
| Constants | `InstallerActions`, `InstallerIds` (`REQUIRED_HOST_VERSION_CODE` 5299), `InstallerContract` (keys, fields, closed sets, ceilings), `InstallerCapabilityKeys`, `InstallerErrorCodes` (20 codes) |
| `:plugin-api:installer-api:testDebugUnitTest` | 8 tests, 0 failures (`InstallerAidlOrderTest` 1, `InstallerContractTest` 7) |
| Release AAR | `installer-api-release.aar`, 30,918 bytes, SHA-256 `0e9996f86736e7ba312c625b5f7f5db78967be5c46f1c1399d75e60470ecb72f`, staged as `libs/installer-api.aar` |
| Protocol document | host `docs/dev/installer-plugin-protocol-v1.md` |

## P1.3 Host client, router and degradation (2026-09-30)

Host package `org.autojs.autojs.core.plugin.installer`: `InstallerPluginHost`, `InstallerSessionHandle` (+ callback binder),
`InstallerSource`, `InstallerJson`, `InstallerBundles`, `InstallerError`, `InstallerErrorMapper`, `PackageChangeObserver`,
`PackageInstallRouter`, `InstallerPluginUi`, `ThreeSetupInstallerOfficialPlugin`. Registration: `PluginCenterViewModel`
(`installer` -> `org.autojs.plugin.INSTALLER`), `InstalledPluginRepository` (service query + discovery branch), manifest
`<queries>`, `PluginInstallWizardCatalog` (`three.setup.installer`, Tools).

| Check | Result |
| --- | --- |
| `:app:testAppDebugUnitTest --tests org.autojs.autojs.core.plugin.installer.*` | 14 tests, 0 failures (`InstallerJsonTest` 9, `InstallerErrorMapperTest` 3, `PackageInstallRouterFormatTest` 2) |
| `:app:compileAppDebugKotlin`, `:app:compileInrtDebugKotlin` | passed |
| Not unit-tested | the observer timeout and the system-installer intent construction (both need an Android runtime); covered by the device checks listed under P1.4 |

## P1.4 Host installer removal (2026-09-30)

Deleted: `PackageInstallerActivity`, `PackageInstallerEntryActivity` (with the `${installerEnabled}` placeholder of both
flavors), `AndroidPackageSessionInstaller`, `PackageInstallStatusActivity` / `Coordinator` / `Receiver` / `Store`,
`PackageInstallStatusRecordTest`, `PackageInstallStatusStoreDeviceTest`, `IntentUtils.installApk`,
`com.stardust.util.IntentUtil.installApk` / `installApkOrToast`, fourteen strings in eleven locales. Rewired to
`PackageInstallRouter`: `ExplorerItem.install` (file manager), `UpdateChecker` (self update), `BuildActivity` (finished
build), `PluginInstaller` (Plugin Center file / URL), `PluginInstallWizardInstaller` + rewritten
`PluginInstallWizardInstallAwaiter`, `ApkInfoDialogManager` (install button), the debug probe (`install-ui`; the
session and recovery routes were removed with the host installer). Added strings:
`error_installer_plugin_required_for_package_format`, `hint_installer_plugin_recommended` (eleven locales).

| Check | Result |
| --- | --- |
| `:app:assembleAppDebug` | passed (x86 and x86_64 debug APKs, 39.6 MB each) |
| `:app:lintAppDebug` | 0 errors, 2392 warnings (2403 before P1; none in the new installer code); the first run exhausted the default 4 GB Gradle heap during `lintAnalyzeAppDebug` and was restarted alone with `-Dorg.gradle.jvmargs=-Xmx12g`, 8 min 41 s |
| Device checks (file manager `.apk` -> info dialog -> install -> system installer; `.xapk` -> plugin guide; Plugin Center URL install -> system installer; the same three with the plugin installed) | not executed in this session; recorded as open in the roadmap |

## P1.5 Documents, changelog and version back-fill (2026-09-30)

- Host: `docs/dev/installer-plugin-protocol-v1.md`, `docs/dev/package-archive-parser-v1.md`; ten-language changelog
  (`improvement`: installer moved to the plugin; `dependency`: Package Archive Parser, Installer API modularized);
  `version.properties` `VERSION_BUILD` 5299.
- Plugin: `InstallerIds.REQUIRED_HOST_VERSION_CODE` 5299 referenced by `ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION`,
  the two `requiresHostVersion` meta-data entries, `.readme/common.json`, the runtime info test; capabilities now carry
  `installerContractVersion` 1; `libs/README.md`, `THIRD_PARTY_NOTICES.md`, `locks/host-api-aars.lock`, AGENTS identity
  table and the ten-language changelog updated.
- Plugin verification after staging: `:app:testDebugUnitTest` 17 tests, 0 failures, `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest` and `:app:lintDebug` 0 errors, 12 warnings (PrivateApi 3, UnusedResources 5 for the icon resources the P5.4 aliases will use, newer dependency versions 4); `generate_markdown.py --check`
  `MARKDOWN_OK languages=10 artifacts=36`; `generate_launcher_icons.py --check` 15 resources.
