# P6.2 security review

Review date: 2026-10-01. Baseline: plugin `4785087` (1.0.0, build 35), plus the P6 changes described below. This is a source and regression review of the local implementation, not a claim that the remaining P6 device, failure and performance matrices are complete.

## Scope and trust boundaries

- The public installer Binder accepts the installed official AutoJs6 host, with its actual package UID, matching current signing certificates and the required host version. Possession of a Binder or the plugin permission alone does not authorize an installation request.
- The external Activity accepts package sources from other apps. Those callers are not trusted to provide installation options, package identity, an executable Intent, or a truthful MIME type, filename, size or file-descriptor access mode.
- The privileged Binder is private to the plugin UID. Root and Shizuku are already privileged principals; their service-shutdown operations are explicitly allowed and are not a route to install or uninstall packages as an arbitrary app.
- The local user can choose privileged authorizers, silent local installation and source deletion. An external caller cannot change these preferences through Intent extras. When the user has explicitly saved silent/auto behavior, an external package request follows that saved choice; this review does not assume every external request must display a confirmation.
- A compromised OS, root administrator, official host signing key or the plugin's own process is outside the caller-isolation guarantee. No key, certificate, package name, installed-app inventory or installation payload is uploaded by the installation path.

## Reviewed controls

| Area | Implemented control and source | Evidence |
| --- | --- | --- |
| Host identity | `binder/HostCallerGuard` in `CallerGuard.kt` reads `Binder.getCallingUid`, `PackageManager.getPackagesForUid`, installed host UID, actual version code and SHA-256 of current signers. `CallerPolicy` requires the official host package, exact UID, minimum build 5299 and equal nonempty signer sets. Caller-supplied version fields cannot replace installed-package facts. | Existing `CallerPolicyTest` covers UID, package, missing host, old host, signer mismatch and empty signer sets. |
| Every public operation | `InstallerBinder` checks the guard before authorizer status/request, default status/change, inspect, users, uninstall and openSession. Only `getInfo` and `getCapabilities` expose non-sensitive plugin metadata without the runtime host check. Session `getId`, `getStatus`, `cancel` and `close` additionally require the original owner UID. | Existing `InstallerBinderDeviceTest.productionRouterExposesCapabilitiesButRejectsPluginUidAsHost`; new `SecurityBoundaryDeviceTest.productionInstallerRejectsNonHostUidBeforeEveryOperationAndCallback` exercises all eight guarded operations against the production service. |
| Request limits | Envelopes, strict JSON types and enums, 64 KiB request documents, at most 32 batch items, 64 APKs per item, four active sessions and bounded queues are validated before installation work. A delegated session handle does not transfer its owner's identity. | Existing request, queue, session-registry and hostile Binder tests; [P2 Binder evidence](p2-binder-evidence.md). |
| External sources | `ExternalSources.fromIntent` reads only the supported action and its URI/stream list. Only `content` and nonempty `file` paths are accepted. The new forwarding Intent contains only package URI ClipData and read/write grant flags; caller extras, nested Intents, component names and activity flags are not forwarded. The write grant can support an explicitly selected delete-source operation, but source reading always requests `r`. | Existing `ExternalInstallDeviceTest.externalDocumentsAcceptViewAndSendMultipleButRejectUnsafeSchemesAndOversizedBatches`; source review of `ExternalSources` and `ExternalInstallActivity`. |
| Actual PFD access mode | Host `inspect` and `openSession` use `SourceDescriptors.validate` to require a regular file or pipe and `O_RDONLY`. P6 applies this same check to the descriptor actually returned by an external provider, before retaining or reading it. The existing failure handler closes the rejected handle. Merely asking a provider for `r` is not treated as proof of read-only access. | Existing hostile Binder descriptor tests; new adversarial-provider and normal file/content/pipe tests in `SecurityBoundaryDeviceTest`. |
| Descriptor ownership | Host PFDs are duplicated for owned work. Cancellation/close and callback death release owned descriptors and staging; remote unmarshalled descriptors are closed after duplication. Seekable reads keep a held inode and do not seek the caller's shared offset. Pipe reads use bounded buffers and cancellation. | Existing `SourceFormatsDeviceTest`, `InstallerBinderDeviceTest`, [P2 core evidence](p2-core-evidence.md). The new provider regression identifies open handles by device/inode, not global FD-count heuristics. |
| Filename, MIME and archive contents | The external chooser's MIME/extension filters only route requests. `ArchiveOpener` and the locked shared parser inspect archive contents and APK manifests. Provider names cannot become unchecked extraction paths: staging sanitizes display names, uses independent random session directories, and the parser rejects unsafe archive entries. XAPK metadata cannot override the actual base APK package/version. AAB remains inspect-only. | Existing `ArchiveOpenerTest` covers metadata mismatch, invalid archives, traversal names, counts, required splits, mixed packages and split dependencies; `SourceFormatsDeviceTest` covers real binary manifest containers. |
| Source changes | Preparation compares source digests around inspection and the install writer checks the approved bytes before commit. Holding a descriptor alone is not considered a byte-stability guarantee. A source change fails the request instead of committing an unapproved package. | Existing `ArchiveOpenerTest` source-mutation case and engine write-integrity tests. |
| Private privileged Binder | `PrivilegedInstallerImpl.checkCaller` requires the plugin UID before `attachClient`, UID discovery and every operation routed through `privileged`. `privileged` clears the incoming Binder identity only after the guard. The service holds one attached live client; death/close abandons sessions and closes queued/running streams. A record must belong to this service instance to be written, committed or released. | Source review of `PrivilegedInstallerImpl`, `PrivilegedServiceBinding`, `RootInstallerService`, `ShizukuUserService`; existing private-service and lifecycle device tests. |
| Privileged parameters | `PrivilegedOptions` validates package names, split basenames, allowed install flags, split/session limits, nonnegative users and declared stream lengths. `createSession` accepts only its two known option keys. Default selection accepts this plugin's component only. The private P6 write-status query uses the same UID and session-record checks as the existing write operations. | Existing `PrivilegedOptionsTest`; source review including P6 write-status changes. |
| Command injection | Installation, uninstall, package state, users and default selection use framework Binder calls. libsu is used to authorize and start its own generated RootService task. No package source, filename, label, package name or request field is concatenated into a shell install command. | Source search and review of `auth/RootShellAccess`, `priv/PrivilegedServiceBinding` and `priv/hidden`. The optional `pm install-*` fallback was not enabled at P0.2. |
| Backup and transfer | Main Manifest declares `allowBackup=false`. `data_extraction_rules.xml` excludes all nine supported app-data domains for both cloud backup and device transfer, including device-protected domains. Neither rule set includes data. | Existing `ManifestContractTest`, new `SecurityConfigurationTest`, installed `ApplicationInfo.FLAG_ALLOW_BACKUP` assertion in `SecurityBoundaryDeviceTest`. |
| Persistence and recovery | History contains bounded display facts, package/version/result/authorizer/origin/timestamps and diagnostics. It does not persist a source URI, PFD, Intent or executable request. Diagnostic paths/URIs are redacted, and incomplete records become interrupted/cancelled after restart. Recovery does not replay package installation. | `history/InstallHistoryEntry`, store/codec tests, display-recovery tests and [P5 evidence](p5-standalone-evidence.md). |
| Network | Only a user-triggered release check calls the fixed HTTPS GitHub API endpoint. It has 10-second connect/read timeouts, no redirect following, a 256 KiB response cap, strict UTF-8/JSON, cancellation and a 12-hour attempt policy. Release links must be the exact HTTPS repository/tag path. It does not upload package bytes or metadata, and no background update worker is registered. | Source review of `settings/AppUpdateRepository` and `AppUpdateController`; existing update-policy/repository tests. |
| Dependencies and release separation | Host AARs are local release artifacts checked against exact SHA-256 locks at Gradle configuration. No framework implementation or `android.*` stub is packaged. Fixture providers, permissive debug-only installer probes and test services exist only in `src/debug`. | AAR lock checks, native-alignment check, Manifest source review, installed merged-component regression below. |

## Exported component review

All names below are relative to `io.github.supermonster003.autojs6.plugin.three.setup.installer` unless fully qualified. This table includes dependency-merged components, not just entries written in the main Manifest.

| Component(s) | Exported in release | Boundary / reason |
| --- | --- | --- |
| `.ThreeSetupInstallerPluginService` | Yes | `org.autojs.permission.PLUGIN` plus per-operation installed-host UID/package/version/signature checks. |
| `.ThreeSetupInstallerPluginInfoService` | Yes | `org.autojs.permission.PLUGIN`; read-only plugin discovery metadata. |
| `.WakeActivity` | Yes | `org.autojs.permission.PLUGIN`; activation trampoline that finishes immediately. |
| `.ui.InstallerSettingsActivity` | Yes | `org.autojs.permission.PLUGIN`; opens the same local settings page, with no option-setting extras. |
| `.ui.ExternalInstallActivity` | Yes | Deliberately unpermissioned package URI entry. Narrow accepted action/scheme/count and actual content/descriptor checks; no script execution. |
| `.launcher.AdaptiveLightIconAlias`, `.launcher.AdaptiveDarkIconAlias`, `.launcher.AdaptiveAutoIconAlias`, `.launcher.TransparentIconAlias` | Yes | Launcher access to `.ui.HomeActivity`. Only one is enabled for the user's selected mode. No install/options payload is consumed by the launcher entry. |
| `rikka.shizuku.ShizukuProvider` | Yes | Shizuku's fixed registration authority, protected for reads and writes by `android.permission.INTERACT_ACROSS_USERS_FULL`. |
| `androidx.profileinstaller.ProfileInstallReceiver` | Yes | Library-merged profile maintenance receiver, protected by `android.permission.DUMP`. It is not an installer operation entry. |
| `.priv.RootInstallerService` | No | libsu instantiates it in its privileged process; its returned Binder still enforces the plugin UID. |
| `.priv.ShizukuUserService` | Not an Android component | No service or Activity is exported for it. Shizuku binding returns the private guarded Binder. |
| Other standalone pages, install/confirmation/user-action/uninstall Activities | No | Internal navigation or token-bound UI only. |
| `.ui.InstallForegroundService` | No | Internal dataSync installation lifetime. |
| `.engine.InstallStatusReceiver`, `.ui.InstallNotifications$CancelReceiver`, `.ui.LauncherIconUpdateReceiver` | No | Explicit internal status/cancellation target or package-replaced maintenance. No exported arbitrary status injection. |
| `androidx.startup.InitializationProvider` | No | Library-merged startup initialization. |

The host declares `org.autojs.permission.PLUGIN` with protection level `signature`. The public installer also independently validates the caller, so an accidental permission-policy change alone cannot authorize package operations. Settings/discovery continue to use the established permission contract.

Debug builds intentionally add `.spike.SpikeInstallerActivity` and its alias as unpermissioned package fixture entries and `.spike.InstallRecoveryProbeActivity` protected by `DUMP`. They must not be used as a production security boundary or distributed as release UI. The installed-manifest regression has an exact separate debug allowlist; the main Manifest static test admits no spike components.

No storage, all-files access, accessibility, microphone or package-install privilege is added by P6. The newly reused descriptor check is a read-only `fstat`/`F_GETFL` check and does not alter the provider's stream position or access flags.

## Logging review

The production source review found logging at these locations only:

| Source | Logged data |
| --- | --- |
| `engine/InstallStatusReceiver` | Unknown status action, or a fixed failure message with the exception when a system confirmation/notification cannot be launched. |
| `priv/PrivilegedInstallerImpl` | A fixed APK-write failure message, declared byte count and the write exception; the P6 write-status handling does not require dumping the input. |
| `ui/InstallPresentation` | Fixed messages when saving history or a display snapshot fails, without serialization of the failed record. |

There is no production log call that dumps an input byte buffer, APK/ZIP entry contents, source stream, request JSON, entire history record, installed-app list or signing material. Framework/IO exception messages may contain a file path or diagnostic metadata; this review does not claim that Logcat is a path-free channel. The private history has its own stricter URI/path redaction. Debug instrumentation logs and ignored local `build/` evidence are not production telemetry.

## P6 finding and regression

Before P6, the host Binder rejected writable source descriptors, while the external provider path only requested `"r"`. A provider can ignore that argument and return an `O_RDWR` descriptor. No source-writing code was found in the read pipeline, but the external boundary did not enforce the stated read-only PFD contract.

`ExternalSources.openItem` now validates the actual returned descriptor with the same `SourceDescriptors.validate` routine used by the host. Validation sits inside the existing owned-descriptor `try/catch`, so a failure closes the handle. Normal read-only files and pipe read ends remain valid. No permission, dependency, UI prompt or new source mutation was added.

The hostile debug provider branch is opt-in via `?writable=1`, remains non-exported, and only reads the existing strictly validated `p2-source-fixtures-<UUID>/fixture.apk` path. The regression first verifies that ContentResolver actually receives `O_RDWR`, verifies that the fixture's inode is visible among this process's FDs, then checks `INVALID_ARGUMENT`, unchanged original bytes and no remaining FD referring to that inode. Ordinary file, ordinary content and pipe content are read completely and compared byte-for-byte in a separate test.

## Verification record and remaining boundaries

Existing successful P2/P3/P5 regressions are documented in their linked evidence files. New P6 tests are intentionally separate from privileged install/uninstall fixtures and do not request Shizuku/Root, install/remove apps, modify preferences, set defaults or contact the network.

| New check | Planned execution / evidence |
| --- | --- |
| `SecurityConfigurationTest` | Passed in the full 261-test JVM run, with no failures or skips (`build/p6-first-build.log`). |
| `SecurityBoundaryDeviceTest` | All four passed without skips on API 24 AVD and API 35 Xiaomi, alongside five storage tests (9/9 per device), and API 33 XQ-DQ72 alongside storage/concurrency (11/11). Logs: `build/p6-security-storage-api24.log`, `build/p6-security-storage-api35.log`, `build/p6-root-api33.log`. |
| Existing `ManifestContractTest`, `CallerPolicyTest`, external-source and hostile Binder tests | Manifest/CallerPolicy JVM tests passed in the full run. Normal read-only file/content/pipe and hostile writable-provider checks passed in SecurityBoundaryDeviceTest on the three devices above. No new unsigned-app attack or positive host-UID script run is claimed. |

The production-service rejection probe runs as the plugin UID, which is not the official host UID. It verifies that a same-signer app with the plugin permission does not automatically become a host. Signer/package/version permutations and session-owner changes additionally have JVM policy coverage. This is not a claim that an unsigned hostile APK was installed, nor a new positive end-to-end host test.

The actual default-installer multi-device matrix, complete process-death/OEM matrix, large-container/performance measurements and publication checks remain separate roadmap gates. A source checklist does not close those gates.

For the original P6.2 checkboxes:

1. The implementation/audit checkbox is supported by the descriptor and compatibility regressions above, plus this component/privacy review.
2. The optional `pm install-*` checkbox is **not applicable**: P0.2 did not enable this path and P6 does not add it. Record N/A with that reason rather than claiming a shell-install whitelist was tested.
3. The non-host rejection and static component/permission checkbox is supported by the actual production-service/merged-manifest device tests and JVM manifest/caller policy checks above.

## Process-death follow-up (2026-10-01)

The new private process-identity and session-recovery methods retain the plugin UID guard. Recovery does not scan or adopt sessions: it can only abandon a previously returned id after matching the platform's real installer UID, originating UID, target user, creation time, installer package, target package and size. Missing framework fields disable this additional recovery path; AOSP exposes the complete identity on API 33+. The existing instance-owned `abandon` method was not broadened. API 35 Shizuku and API 33 Root each reject ten deliberately corrupted recovery identities without removing the live session.

`InstallProcessDeathProbeActivity` is Debug-only, exported under `android.permission.DUMP`, and is included in the device component allowlist. Its self-termination control accepts only its current PID and an already recorded, active fixture case. A restarted process ignores a stale termination request. No Release component or Binder method can request arbitrary process termination. The official host uses a separate DUMP-protected debug caller process and a non-exported read-only source provider; its temporary enablement journal changes and restores only this plugin's existing enable key, never trust or priority.

The lifecycle/security combination passed 11/11 on API 24 and API 35. Actual official-host cancellation and privileged process tests are recorded separately in `p6-process-death-evidence.md`; these now provide positive official-host UID evidence in addition to the earlier non-host rejection checks.

## P8 review (2026-10-02)

The public V2 default operation uses the same installed-host UID/signature/version guard before
decoding its request or dispatching a callback. V1's first ten transactions stay frozen; capabilities
advertise a minimum of 1 and a maximum of 2, and unchanged result envelopes remain readable by V1.
The real production-service rejection test now covers nine guarded operations on API 24 and 36.

Dhizuku initialization first verifies the current owner and provider package, user, UID and signing
identity. It then verifies the API's cached owner before wrapping a fresh framework object. Shared
process managers are not modified. Only current-user operations and the actual owner's installer
identity are accepted; Root/shell flags and keep-data uninstall are rejected. API 24 does not initialize
the API-26 library. The known-ID journal requires owner identity, package, size and a random origin
marker before recovery can abandon a session. It skips a live creator, retains unverifiable records,
and does not replay an installation. Real unfinished-session process death and a non-journal control
session are recorded in [P8 Dhizuku evidence](p8-dhizuku-session-evidence.md).

Persistent defaults distinguish passive public resolution from a local completed-write receipt.
Dhizuku's API 34+ final policy callback cannot be verified by this transport, so it rejects changes
before writing. Its partial failures retain uncertainty without clearing an older unknown policy.
Root uses a separate bounded UID/GID 1000 process with fixed operations, user 0, component and filters;
the shared RootService never changes identity. A verified handshake and explicit commit precede writes.
Known duplicate policies are idempotent, unknown XML remains protected, and rollback cannot delete an
unproven concurrent change. Cancellation, hard termination and the limits of compensation are recorded
in [P8 Root evidence](p8-root-system-spike-evidence.md).

Notification approval is an immutable, per-session/per-prompt action. Stale or repeated actions cannot
approve a different item. Android confirmation is opened only by its notification action. Notification
denial rejects a real external request before source opening or session creation on API 36. Cross-UID
testing additionally found that a stopped service could retain its URI permission owner. Android 12's
[start-result bookkeeping](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-12.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java)
removes the delivered start for a non-sticky result, while the
[service record's shutdown cleanup](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-12.0.0_r1/services/core/java/com/android/server/am/ServiceRecord.java)
revokes grants attached to the retained starts. The exact owner's device diagnosis, correction and
re-delivery/no-replay verification belong to [P8 notification evidence](p8-notification-evidence.md);
merely observing the foreground notification disappear is insufficient proof of permission release.

No new exported production component or arbitrary process-kill operation is added. Notification
source providers and relay activities belong only to the test APK, and process-death controls remain
Debug-only, bound to this test's token, fixture, package UID and process identity. No broad URI revoke,
DeviceOwner replacement on a user device, security-policy relaxation or remote publication is used.
