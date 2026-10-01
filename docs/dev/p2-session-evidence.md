# P2 session, users and uninstall evidence

Date: 2026-10-01 (Asia/Shanghai).

Integration update: the formerly uncommitted work is included in the logical P2 commits.
`p2-core-evidence.md` adds the three-distinct-package batch, managed-profile installation,
Android 7 user-state repair, and the final cross-device regression results.

Follow-up: `p2-binder-evidence.md` records the subsequent Binder integration. The placeholder-service
and next-step statements below describe the earlier session increment, not the current service.

This increment completes the worker-side batch coordinator, target-user validation and uninstall
engines on top of the previously reviewed single-package engine. It closes P2.4 and the core parts
of P2.5, plus P2.3 result normalization. It does not close the whole P2 milestone or expose a new
host entry point: `ThreeSetupInstallerPluginService` still has its placeholder Binder.

## Implementation

- `InstallSession` runs request items serially and combines the descriptors belonging to each item
  into one prepared split set. Its absolute elapsed-realtime deadline starts when the session is
  created, includes time in the executor queue and preparation, and is passed into every engine call.
- With `continueOnError=true`, failed items appear in their original order and subsequent items run.
  Cancellation or the session deadline stops further source preparation. Already confirmed successes
  stay successful, remaining items receive their own errors without inheriting another package's name.
  With `continueOnError=false`, the first failure goes to `onFailed` and later items never start.
- Success documents use `InstallEngine.Result.interaction`, preserving an auto-to-dialog fallback,
  and versions read for the selected user. The private AIDL adds `getInstalledVersion = 10`; existing
  IDs, including release 9 and Shizuku destroy, remain unchanged. Post-success metadata lookup or
  cleanup failure cannot turn a confirmed install into failure. Host-owned sources are never deleted:
  `sourceDeleted=false` tells their owner to handle deletion under D25.
- Source descriptors are duplicated before scheduling. Each session and each source have separate
  staging directories, including duplicate host IDs or display names. Staging duplicates its own
  input stream descriptor instead of accidentally closing the caller's descriptor. Pipe reads poll
  cancellation/deadline every 100 ms, without changing the host's shared descriptor flags.
- Owned resources are closed before terminal callbacks. Session cancellation and worker retirement
  synchronize on the same lock, and the worker clears its interrupt flag before returning to the
  pool. Close, cancellation before start, worker rejection and late callbacks are covered.
- `DeviceUsers` lists all users through the privileged service or only the plugin's current user
  without privileges. It rejects nonexistent numeric IDs. `current` and `all` resolve to the plugin's
  user; engines additionally map `all` to the corresponding platform flag. User running state is now
  queried instead of guessed. Explicit user selection with `none` requires privileges.
- `PrivilegedUninstallEngine` maps keep-data and all-user flags, bounds the wait by the request
  deadline and preserves platform errors. Explicit silent interaction rejects an unexpected prompt.
  `NoneUninstallEngine` uses a non-exported, token-bound `UninstallDialogActivity` to receive the system
  uninstall result. The bridge maps success, refusal and failure, expires pending tickets and attempts
  to dismiss the child confirmation on cancellation. Cancellation cannot promise rollback after
  Android has already performed a removal.
- Request JSON is parsed strictly. Incorrect optional types, fractional/overflowing integer values,
  trailing documents and documents exceeding the UTF-8 byte ceiling now fail as `INVALID_ARGUMENT`
  instead of being treated as missing/default options.

The parser still requires local `File` access and this environment stages each source. The seekable
source optimization of P2.1 is not claimed here. Preparation checks cancellation between parser calls;
interrupting a long shared-parser extraction internally remains a P6 concern.

The hidden signatures follow [AOSP IPackageManager](https://android.googlesource.com/platform/frameworks/base/+/android-8.0.0_r4/core/java/android/content/pm/IPackageManager.aidl)
and [AOSP UserManager](https://android.googlesource.com/platform/frameworks/base/+/61f01fe56bd8464acf3141212371a9176f3d6c9b/core/java/android/os/UserManager.java).
The system activity result semantics follow the [Android Intent reference](https://developer.android.com/reference/android/content/Intent#ACTION_UNINSTALL_PACKAGE).
No framework implementation or new dependency was copied into the project.

## JVM and build evidence

66 JVM cases pass, including 26 added cases:

| Class | Added cases | Covers |
| --- | --- | --- |
| `InstallSessionTest` | 14 | Item/split ordering, partial failures, fail-fast, shared deadline, cancellation, confirmed success, actual interaction, metadata/cleanup failure, confirmation delegation, lifecycle, interrupt cleanup, callback loss and error attribution |
| `RequestDocumentsTest` | 4 | Strict JSON and types, UTF-8 bytes, 32-item / 64-split ceilings and defaults |
| `DeviceUsersTest` | 2 | Current/all/numeric users, nonexistent IDs and privilege restrictions |
| `UninstallEngineTest` | 6 | Flags, authorizer/user validation, platform and confirmation-launch error mapping, cancellation/deadline before submission and confirmed success |

Debug, androidTest and minified release APK assembly pass. Debug lint has zero errors and 13 warnings;
release lint has zero errors and 16 warnings, matching the preceding engine handoff. No new runtime
dependency or version override is introduced. Ten changelog locales and their generated artifacts are
updated; Markdown generation/check and all 15 icon resource checks pass.

## Device evidence

SDK/ABI and users were read again in this session. The test installation refuses to proceed if the
fixture package or retained fixture data already exists. Cleanup only targets that fixture.

| Device | Serial | API / ABI | Authorizer | Verified coverage |
| --- | --- | --- | --- | --- |
| Xiaomi 23046RP50C | `968e9f18` | 35 / arm64-v8a | Shizuku ADB | 12 distinct cases pass; the Root-only data-retention case is deliberately skipped |
| Sony G8441 | `BH900ASK9E` | 28 / arm64-v8a | libsu Root | All 13 distinct cases pass, including data retention |
| AVD_API_24 | `emulator-5554` | 24 / x86 | none | 12 pass and the Root-only case is deliberately skipped; runner reports OK (13 tests) |

The 13 cases comprise six `InstallSessionDeviceTest`, two `UninstallDialogDeviceTest` and five existing
`ThreeSetupInstallerPluginContractTest` cases. On both real devices, one bridge test initially tried to
construct an Activity on the instrumentation worker, which has no Looper. That test setup was moved to
the main thread, and both bridge cases were rerun successfully on each device. The final AVD run uses
the corrected test directly. This was a test construction failure, not a failed package operation.

The real batch contains v1 APK, a malformed package, and v2 APK. Both valid items succeed despite the
middle failure, the update reports previousVersionCode 1 and versionCode 2, and the source files remain.
All three transports then successfully uninstall the fixture. API 24 confirmations were accepted only
after verifying the displayed label `3-Setup Spike Fixture`, for new install, update and uninstall.
These operations exercise the engines directly, not the still-pending public Binder router.

The Root data-retention test installs the fixture, writes a marker under its data directory, uninstalls
with keepData, reinstalls, verifies the exact marker content, then performs a full uninstall. Other
cases check running-user state, nonexistent target rejection, closed PFD refusal, isolated directories,
stalled-source timeout and session cancellation during a real pipe read. Cancellation asserts staging
is already removed in the callback, the host descriptor is still valid and the pooled worker can run
another task without an interrupt flag.

Bridge result tests inject activity answers without opening UI or deleting packages. They do not prove
an OEM's protected/system-app refusal behavior. Both real devices currently expose only user 0, so no
secondary-user installation or data mutation was attempted. No user's installed application or default
installer preference was modified.

Reproduce after installing the debug and androidTest APKs on an explicitly chosen device:

```powershell
adb -s DEVICE_SERIAL shell am instrument -w -r -e engineAuthorizer shizuku -e class io.github.supermonster003.autojs6.plugin.three.setup.installer.InstallSessionDeviceTest,io.github.supermonster003.autojs6.plugin.three.setup.installer.UninstallDialogDeviceTest,io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPluginContractTest io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner
```

Select `root` or `none` for the other transports. The latter requires system confirmations. Without
the explicit authorizer argument the real package-operation cases are skipped. Build outputs and
device logs remain local under `build/` and are not versioned.

## Next integration steps

1. Add the default-installer coordinator around the existing privileged P0 implementation; preserve
   `DEFAULT_REQUIRES_CLEAR` and leave competitors' unrelated defaults intact. The release APK still
   has no external APK entry Activity, so default-installer availability must reflect that fact.
2. P2.6: replace the placeholder with `IInstallerPlugin.Stub`; add caller/version/envelope/PFD checks,
   bounded workers, `SessionRegistry`, owner-UID checks, callback Binder death and ten-minute terminal
   reclamation. Declare capabilities only for routes that are actually ready. The session does not
   itself register a Binder death recipient or enforce the process-wide four-session limit.
3. Construct `DescriptorInstallEnvironment.acquire(...)` before scheduling; parse descriptor metadata
   first. Construct `InstallSession(request, environment, listener, SystemClock::elapsedRealtime)` and
   call `start(executor)` once. Forward `status()`, `cancel()` and `close()` from the guarded session
   Binder. `close()` cancels running work; let its worker own resource cleanup to avoid racing reads.
4. P3 owns plugin confirmation for explicit `interaction=dialog`, including privileged operations.
   Supply the environment's `confirmation` callback before making that route public. Its default
   refuses the operation, so a missing plugin confirmation cannot silently perform an install.
   For privileged uninstall, the caller likewise confirms explicit dialog before invoking the engine.
   Add background-launch notification fallback and complete activity lifecycle/rotation UI tests.
5. Audit the existing `PrivilegedClient` for concurrent first acquisition before exposing parallel
   host sessions. Finish inspect enrichment and source/format fixtures, real split containers,
   secondary/all-user installations, low-target/test/downgrade flags and protected-app OEM failures.
   The session coordinator tests do not substitute for those pending matrices.

This workspace already contained uncommitted P2 sources and review changes when the session began.
They remain preserved and uncommitted. Under AGENTS.md section 3.3, do not package unaudited earlier
work into an automatic commit merely to make the tree clean. VERSION_BUILD stays 10, equal to the
existing HEAD commit count; no repository was pushed and no host source or public contract AAR changed.
