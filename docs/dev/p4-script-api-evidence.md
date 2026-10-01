# P4 script API evidence

Date: 2026-10-01. Scope: P4.1, P4.2 and P4.3, with the existing P7.1 API documentation and declaration synchronization required by the host repository rules. No roadmap section was added, split or removed. All changes and generated artifacts are local; no Git push, tag, npm publication or GitHub release was performed.

## Implementation

- AutoJs6 6.8.0 / build 5300 registers `installer` and `$installer` next to `mail`. Each ScriptRuntime owns an InstallerService and closes it on exit.
- All Appendix A operations are implemented: synchronous / Async installation, session installation, uninstall, inspect, authorizer queries and requests, default-installer queries and setters, users, availability and status.
- Arguments preserve File / Uri wrappers, arrays and explicit `{ splits }`. A one-element array remains a batch; a split set remains one application. Unknown options, explicit null, cycles, sparse arrays, invalid enums, fractional IDs / timeouts and contract limits are rejected before opening sources.
- A script owns at most 36 admitted operations, with up to four running. Provider cancellation, late descriptor / binding handoff, queued operations and script exit have explicit cleanup paths.
- Binder and worker callbacks carry plain values. Rhino conversion, Promise settlement and session listeners execute on the creating script thread. Sync waits use a Condition and drain only that thread's installer dispatchers. Sync APIs, including session creation and metadata getters, reject Android's UI thread.
- `session.wait(timeout)` times out that wait; `options.timeout` ends the whole operation. Script stop suppresses queued callbacks and closes unfinished sessions. A confirmed install is never rolled back by cleanup or a late cancel. V1 one-shot uninstall, authorization and default-setting calls cannot retract an operation already accepted by Android.
- Path deletion is attempted only after confirmed success and before the result is delivered. Open-descriptor and path identities are compared; known replacement, modification, symlink, inaccessible or closed sources are retained. Failed or unprocessed items protect shared paths. Content URIs are retained. Added deletion notes cannot turn a successful result near the 64 KiB wire limit into a failure.
- APK / AAB inspection retains metadata; valid bounded PNG icons become Bitmap objects. `InstallerError` is an actual JavaScript Error, including inside batch results.

## Device-discovered protocol correction

The first complete API 35 script run exposed an existing V1 ambiguity: `InstallRequest.isBatch` was inferred solely from the number of grouped applications. `install([badSource])` therefore threw a single-item error instead of returning one failed batch item.

The request now carries an optional boolean `batch`, defined by `InstallerContract.FIELD_BATCH`. The script service sends the original argument shape. Missing `batch` retains legacy inference; `false` with multiple applications and non-boolean values are rejected. The AIDL order, contract version 1 and minimum base host build 5299 are unchanged. This is an additive correction to the unreleased V1 implementation; script validation uses the paired host build 5300 and plugin build 30.

The release `installer-api.aar` is 30,940 bytes, SHA-256 `ba633d8dfc5a08ed2659654780e2de2dfe3468af080e8c847f8e370c7bfb93f7`. The plugin consumes its own hash-locked copy. Common and parser AARs retain their previous bytes.

## Verification

### JVM and build

| Check | Result |
| --- | --- |
| Host `:app:testAppDebugUnitTest` | 3,272 tests, zero failures / errors, six existing unrelated skips; all 82 installer tests passed |
| Host Debug and androidTest APKs | Passed; host build 5300, including 16 KiB native alignment verification |
| Installer contract release AAR and JVM tests | Passed; AIDL order unchanged |
| Plugin `:app:testDebugUnitTest` | All 201 tests passed, no skips |
| Plugin Debug / androidTest / Release APKs | Passed, plugin build 30 |
| Plugin `:app:lintDebug` | Passed: zero errors, 21 warnings |
| Plugin Markdown / icon checks | Passed: ten languages / 36 generated documents / 15 icons |

Logs: host `build/p4-batch-host-build.log`, `build/p4-contract-build.log`; plugin `build/p4-batch-plugin-build.log`.

The additional host-wide `:app:lintAppDebug` did not produce a final report after about 18 minutes. Its thread dump showed `JoinEffectDetector` / `com.android.tools.lint.checks.fx.analysis.Analysis` still performing interprocedural analysis. The task's own Gradle client was stopped to release the build; no checks were disabled, no baseline was changed, and this invocation is recorded as incomplete. Logs: host `build/p4-host-lint.log` and `build/p4-lint-threads.txt`. The completed plugin lint, host JVM / APK and device gates above are separate results.

The test suite covers source / option normalization, typed and JavaScript errors, result shape, callback ownership, operation deadlines, cancellation, late open cleanup, shared-file deletion and exact 65,536-byte result boundaries. Nine additional host / plugin tests cover the explicit batch marker, legacy requests, invalid marker types, failed one-item batches and fail-fast behavior.

The test-APK-only providers use Java because they can run in a separate test process without the target APK's Kotlin runtime. An initial device run diagnosed this fixture problem through `NoClassDefFoundError: kotlin.jvm.internal.Intrinsics`; neither that run nor its assumptions is treated as acceptance evidence.

### Actual Android descriptors and completion races

`InstallerSourceDeviceTest` has five tests for real file deletion / retention, provider pipes with unknown size, cancellation and permission error mapping. `InstallerSessionHandleDeviceTest` has three tests for completion / deadline ordering and keeping the host descriptor open until deletion and explicit close. Race ordering uses a paused dispatcher rather than sleep-based probability.

- API 24 / x86, user-started AVD: all eight tests passed, no skips, 0.106 s. Log: host `build/p4-device-api24-core-verified.log`.
- API 35 / arm64-v8a, Xiaomi 23046RP50C: all eight tests passed in the second full run. The separate script installation case still exposed the batch ambiguity at that point; the overall run was not a pass.
- After the batch correction, API 35 passed all 13 core + script tests with no skips in 10.360 s. Log: host `build/p4-device-api35-final.log`.

### Real script and Binder tests

Test class: `org.autojs.autojs.runtime.api.augment.installer.InstallerScriptSmokeDeviceTest`.

The class uses the actual host UID and Rhino engine, and only the reviewed code-free `io.github.supermonster003.autojs6.installer.spike.fixture` APKs. Before taking ownership, it checks every device user, including retained package data. It copies inputs to a unique host-private cache directory and removes only its owned fixture package / files afterward. Explicit opt-in is required for already-granted Shizuku and temporary enable-state changes. No new Root / Shizuku authorization or default-installer preference change is made.

```powershell
adb -s 968e9f18 shell am instrument -w -r `
  -e class org.autojs.autojs.runtime.api.augment.installer.InstallerScriptSmokeDeviceTest `
  -e installer.smoke.enablePlugin true `
  -e installer.smoke.disablePlugin true `
  -e installer.smoke.authorizer shizuku `
  org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
```

The five cases cover:

1. Alias / status, APK and generated AAB inspection, File / Uri inputs, available / auto authorizer state, safe requests, users / usersAsync and argument errors.
2. Single-element failed batch, synchronous File installation, Async one-element array update, split-set session installation, progress / complete callbacks on the original thread, one terminal event, deletion before result delivery and Async uninstall.
3. Short wait timeout followed by explicit cancellation, one cancellation event, no late callbacks and EPIPE confirmation that every host / plugin pipe reader closed.
4. Force-stop during a Condition-based `session.wait`, with no script revival or late callbacks and all pipe readers closed.
5. Temporarily disabled plugin: false availability probes and real `InstallerError(PLUGIN_UNAVAILABLE)` for synchronous / Async actions, including default-setting guards; exact preference restoration in finally.

The final API 35 report confirms `installForms=3`, `uninstalled=true`, source deletion before result delivery, retained sources without the option, and one completion event. Its logcat report is saved at host `build/p4-device-api35-script-reports.log`. API 24 additionally passed the four script cases that do not require Shizuku, no skips, 3.533 s, using the updated host build 5300 / plugin build 30; log `build/p4-device-api24-scripts.log`. This is 12 P4 cases on API 24 across the core and script runs, separate from its 15 existing P3 cases.

The original API 35 enable preference was explicitly `false`; tests restore that value. The user-started API 24 and API 31 AVDs are left running. Default-setting happy paths that alter Android preferences and newly granted privileged authorizations remain in the original P2 / P5 / P6 device matrix; their error guards and safe queries are covered here.

## Examples and associated repositories

Three host samples were added under `assets-app/sample/应用`: `静默安装应用.js`, `批量安装应用.js`, `设为默认安装器.js`. Their default configuration is safe to open/run: paths must be supplied, and the default-installer sample is read-only until `applyChange` is changed. They pass JavaScript syntax checks. Existing sample loaders use the asset filename; there is no ten-language title resource consumer, so the existing Chinese filename convention was retained.

The host's ten-language changelog and generated release-history fragments describe the new installer module. Host `assets-app/doc` receives those normal changelog artifacts only; API HTML stays with the canonical documentation and offline plugin.

| Repository | Result |
| --- | --- |
| AutoJs6 | Complete script implementation, protocol / resource ownership fixes, examples and ten-language changelog; local commit `a5c6bc7b4c`, build 5300 / 6.8.0 |
| AutoJs6-Documentation | `api/installer.md`, centralized module types, navigation, progress and app.uninstall cross-links; 144 modules generated and checked; local commit `df05ef1` |
| AutoJs6-Plugin-Offline-Docs | Full API assets / search synchronization, current provenance, ten-language changelog; Debug and Release APK gates passed; local commit `406c29f`, build 65, binary version 6.8.5, content version 6.8.0 |
| AutoJs6-TypeScript-Declarations | installer / $installer and overload / session declarations, positive and negative smoke compilation; `aj6dts.bat -Publish` copied locally only; local commit `e56d9b8`, version 4.25.0 |
| AutoJs6-Plugin-Ace-Editor | Hand-maintained installer / init declarations synchronized, selected index preserved, LSP declarations generated and runtime verifier passed; local commit `fe4c13d`, build 118 / 1.17.0 |

The Ace repository's pre-existing untracked `releases/` directory was preserved and excluded from the commit. The documentation / declaration synchronization advances the existing P7.1 items; it does not mark the P7 release gate complete.

## Remaining device acceptance outside P4

The user manually started the API 24 and API 31 AVDs. API 24's existing 15 P3 UI / recovery / real-IME cases passed without skips. API 31 completed the real unknown-source grant and returned to the same platform session, but declined an additional Play Protect upload / scan prompt; restoring the app op then killed the instrumented UID. That run is neither a successful installation nor a normal skipped run. See the P3 evidence files for the recorded session, permission restoration and cleanup.

The original P3 unknown-source successful-install path and file-manager / browser APK / XAPK matrix, the remaining privileged compatibility matrix and P5 standalone application UI are still open. No additional test hardware or irreversible decision is required for this P4 implementation; completing those P3 items will require an appropriate manual confirmation or external-entry action.
