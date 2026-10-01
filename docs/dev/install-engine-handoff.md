# InstallEngine handoff

Integration note (2026-10-01): this historical review is now included in the logical P2 commits.
Its uncommitted-work notes describe the earlier handoff. Current source, authorizer, device-matrix
and build results are in `p2-source-evidence.md`, `p2-authorizer-evidence.md` and `p2-core-evidence.md`.

Implemented on 2026-09-30 against the existing, uncommitted P2 sources. This is the single-package
installation engine requested by the maintainer, not completion of the whole P2 milestone.
Updated on 2026-10-01 after review; the maintainer approved the auto-confirmation fallback and
the downgrade-error classification below.

## Calling the engine

`engine/InstallEngine.kt` provides `InstallEngine`, `NoneInstallEngine` and `PrivilegedInstallEngine`.
Select the authorizer with the existing resolver before constructing an engine. Explicit authorizers
are checked against the selected engine; they never fall back to another transport.

```kotlin
val engine: InstallEngine = if (authorizer == Authorizer.NONE) {
    NoneInstallEngine(context)
} else {
    PrivilegedInstallEngine(context, authorizer)
}
val result = engine.install(
    InstallEngine.Request(
        prepared = preparedPackage,
        options = request.options,
        userId = resolvedUserId,
        interaction = request.interaction,
        deadlineMillis = batchDeadlineElapsedRealtime,
    ),
    object : InstallEngine.Listener {
        override fun onStage(stage: String) { /* Forward nonterminal stages. */ }
        override fun onProgress(bytesWritten: Long, totalBytes: Long) { /* Forward item progress. */ }
        override fun onUserAction(intent: Intent) {
            UserActionLauncher.launch(context, intent)
        }
    },
    checkCancelled = {
        if (cancelled.get()) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Installation cancelled")
    },
)
```

Run this call on a worker thread. Callbacks execute on the same worker; dispatch UI work as needed.
`onUserAction` must launch/delegate the provided system confirmation or throw. The existing
`UserActionLauncher` is the current delegate; P3 replaces it with its own Activity and notification
fallback. Explicit `silent` rejects an unexpected system confirmation. `auto` prefers silent behavior
for a privileged engine, but permits a platform-required confirmation and adds a note to the successful
result. The result then reports `interaction = "dialog"`. For `none`, auto uses confirmation behavior.
Pass the original `auto` value into the engine; do not normalize it to `silent` in the session layer.
When interaction is `dialog`, the future session/UI layer must show the plugin's own confirmation
before calling the engine, including when a privileged transport is selected.

`Request.userId` is already resolved by the caller. `current` and `all` use the current app user as
that value; `all` additionally maps to `INSTALL_ALL_USERS`. Numeric IDs must match `options.user`.
The future DeviceUsers layer must check that the target user exists and is permitted.

The engine installs exactly one `PreparedPackage`, with all of its APKs in a single platform session.
The optional absolute deadline is in `SystemClock.elapsedRealtime()` units; the earlier of that value
and the option timeout applies. The engine emits writing, committing and, when necessary, confirming
stages. It returns `Result(packageName, notes, interaction)` on confirmed success and throws `InstallFailure` on
failure. It does not emit terminal stages itself, allowing the session layer to aggregate batches.
Use `result.interaction` when building the public result document, so an auto fallback is not reported
as silent. The field is always `dialog` or `silent`, never `auto`.

The session layer still owns:

- Source acquisition, staging ownership and cleanup, package inspection and split selection.
- Batch ordering, continuation after item failure, terminal callbacks and session registry lifetime.
- Installed-version reads and `InstallDocuments.installResult` construction for the correct target user.
- Source deletion by its owner after success. The engine never deletes sources, including on failure.
- Plugin confirmation UI, foreground notifications and host-facing Binder routing.

## Implementation details

The common session loop validates APK count, names, lengths, authorizer/interaction and target-user
consistency before allocating a session. It writes using a 1 MiB buffer, reports aggregate byte
progress across splits, detects files that grow or shrink during transfer, closes writes before
commit, maps system status through the existing mapper, and abandons operations that stop without
a terminal system result. Both successful and failed terminal results only release local ownership.
The buffer reduction lowers per-call allocation and gives progress updates at most 1 MiB apart;
no throughput improvement is claimed. Critical cancellation checks around callbacks remain.

Progress counts bytes accepted by the session stream. Reaching total bytes is not installation
completion: the privileged writer still drains and fsyncs, and Android validates/installs at commit.
The P3 UI must keep displaying the committing stage until the terminal result arrives.

The regular backend uses the framework Session API, explicitly requests user action on API 31+ and
marks the source as local on API 33+. The privileged backend uses the P0 service and the existing
PrivilegedClient. It maps downgrade, test-only, all-user and low-target flags, and reports an ignored
low-target option in notes on API < 34. The default installer attribution is `com.android.shell`
under shell identity and the plugin package under root; an explicit installer option is preserved.
Each privileged engine caches the UID by Binder identity. Reusing a Binder, even through a new AIDL
wrapper, avoids another UID transaction; a replacement Binder triggers a fresh lookup. A failed
lookup is not cached, and an explicit installer option does not require a UID lookup.

The privileged pipe is nonblocking and checks cancellation/deadlines while waiting for the reader.
The service remains responsible for draining, validating and fsyncing the pipe before commit. The
`Os.fcntlInt` compatibility call is in `priv/hidden/HiddenApiAccess`: it is public on API 30+, and its
AOSP hidden signature is used on older supported versions. EPIPE is reported directly as
`INSTALL_FAILED` with a destination-reader message and the original errno text in `systemMessage`.
The privileged service logs the first non-cancellation writer exception once per session, including
declared length and the exception, without logging APK contents. Previously the main write loop
already mapped pipe IO to INSTALL_FAILED via platformIo; the new EPIPE branch makes attribution
explicit and the server log preserves the underlying failure.

Platform SecurityException from regular-installer operations maps to nonretryable `BLOCKED_BY_POLICY`
with the original text in `systemMessage`. Privileged security refusals retain `AUTHORIZER_UNAVAILABLE`.
Status 4 containing `INSTALL_FAILED_VERSION_DOWNGRADE` maps to `INSTALL_FAILED`, preserving both status
and systemMessage; other invalid-package statuses still map to `INVALID_PACKAGE`.

Two integration fixes accompany the engine:

- The manifest declares the existing InstallStatusReceiver as non-exported, and its contract test
  now checks that declaration.
- PrivilegedInstallerImpl retains session ownership after commit. Previously it removed the record
  immediately, so a later abandon during pending user action was ignored. Private AIDL now adds
  `release(int sessionId) = 9`, preserving all existing transaction IDs. `release` only removes local
  resources; it does not call the framework's abandonSession. Cancellation, timeout or a local failure
  before a terminal result uses `abandon`, followed by idempotent release during handle close. Any
  terminal system status, including installation failure, uses release only. The session layer calls
  the engine rather than managing these private platform session IDs itself.

Record.closeSession guards the framework Session.close call with a synchronized sessionClosed flag.
The flag is set before making the call, so concurrent cleanup and an ambiguous Binder failure cannot
trigger a second close. Both commit and removal use this helper. The P0 install test now also uses
release after a terminal answer, while its preterminal failures and uncommitted-session tests still
use abandon.

Once the system confirms installation, a late cancellation or cleanup failure does not turn that
success into a reported failure. Cancellation before confirmation attempts to abandon the platform
session; it does not promise rollback of an installation that the system already completed.

Platform references: [PackageInstaller.Session](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session),
[SessionParams](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams),
[AOSP Android 7 Os signatures](https://android.googlesource.com/platform/libcore/+/android-7.0.0_r1/luni/src/main/java/android/system/Os.java).

## Verification

- JVM: 40 tests pass, including 15 InstallEngineTest cases and three InstallFailureTest cases. These cover split writes and progress,
  cancellation, deadlines, changed file lengths, invalid inputs, privileged options, user/authorizer
  mismatch, failure mapping and cleanup, interruption propagation, progress granularity, policy-error
  attribution, terminal failure release and downgrade-error classification.
- Debug, androidTest and minified release APK assembly pass. Both lint variants have zero errors.
  Debug has 13 warnings: the previous 12 plus UsableSpace in the existing PackageStaging file.
  Release has 16 warnings, additionally reporting three unused identity string resources. There are
  no lint warnings in InstallEngine.kt.
- Device: Xiaomi 23046RP50C / API 35 / arm64-v8a / Shizuku ADB and Sony G8441 / API 28 / arm64-v8a /
  libsu Root both pass real new installation, update, downgrade rejection, full-pipe cancellation and
  closed-reader EPIPE attribution. Each run asserts three releases and zero abandons across the three
  terminal install outcomes. Including interaction and baseline Binder tests, each run has 14 passes
  and one deliberate skip of the none-only confirmation-cancellation case (runner reports 15 tests).
- Device: AVD_API_24 / API 24 / x86 passes all four engine tests with authorizer none, including real
  system-confirmed installation/update, downgrade rejection, cancellation while awaiting confirmation,
  stalled-pipe cancellation and EPIPE attribution. Together with interaction and baseline Binder tests,
  all 15 cases pass. Confirmation was accepted only for the bundled test fixture.
- Device: six InstallEngineInteractionDeviceTest cases simulate a platform prompt without installing
  packages or opening UI. They verify auto fallback and its note/actual interaction, explicit silent
  refusal, auto without a prompt, ordinary confirmation, explicit dialog and cancellation. They run on
  all three devices above; they do not claim a specific OEM was made to require an extra confirmation.
- Device: the P0 truncated-write/queued-cancellation case passes on API 28 Root; logcat gained exactly
  one writer-failure record, while expected cancellation cleanup did not produce additional records.
- Device: cancellation while awaiting system confirmation also passes on the API 35 Xiaomi. The
  five baseline discovery/activation Binder cases pass on both API 28 Sony and API 35 Xiaomi.
  The cancellation test now waits up to five seconds for the asynchronous session-list removal;
  an immediate snapshot previously raced an already-destroyed session on API 35.
- Follow-up N1-N4 (2026-10-01): eight focused cases pass without skips on each of API 35 Shizuku and
  API 28 Root. Five PrivilegedLifecycleDeviceTest cases check concurrent/failed close-once behavior,
  UID reuse across AIDL wrappers, Binder replacement, failed lookup retry and explicit attribution.
  The three real-operation cases cover the engine install/update/downgrade flow, the P0 install and
  rebind flow, and preterminal/queued cancellation. The engine's three installs assert one UID read,
  three successful releases and zero abandon calls.
- Fixtures are the two signed P0 APKs. Tests refuse to replace a pre-existing fixture package and
  remove only their own test installation. No user application was removed.

To reproduce, assemble/install both debug APKs on one explicitly selected device, then run:

```powershell
adb -s DEVICE_SERIAL shell am instrument -w -r -e engineAuthorizer shizuku -e class io.github.supermonster003.autojs6.plugin.three.setup.installer.InstallEngineDeviceTest io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner
```

Use `root` for libsu or `none` for the interactive system installer. The none test requires accepting
the fixture's system installation/update confirmations. Without an authorizer argument, only the
non-destructive pipe tests run. Privileged access must already be available or be granted when asked.
Run InstallEngineInteractionDeviceTest without privileges to reproduce the six policy cases.

The full P2 matrix (real split containers, explicit downgrade and test flags, low-target bypass,
secondary/all-user installation, concurrent initial authorizer binding, batch orchestration, full UI
and Binder integration) remains open.
This change does not claim these later integrations are complete.

The working tree already contained Claude's uncommitted P2 dependencies. Per AGENTS.md section 3.3,
this handoff leaves changes uncommitted for integration with that work; VERSION_BUILD remains 10.
All ten README/instruction locales now emphasize the default with a bold Note label: with privileges
available, script APIs do not proactively show confirmation. The system-required auto confirmation
exception and explicit dialog/silent choices remain documented alongside that warning (D32).
The host's `docs/dev/installer-plugin-protocol-v1.md` has the corresponding behavior/error clarifications
only; host code and artifacts are unchanged, and no repository has been pushed by this review pass.
