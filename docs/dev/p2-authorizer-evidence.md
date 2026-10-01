# P2.2 authorizer implementation and verification

Date: 2026-10-01.

This document records source inspection and the focused verification entry points. Device results
must be recorded below only after the corresponding instrumentation run has completed.

## Source implementation

- `auth/Authorizer.kt` contains `NONE`, `SHIZUKU`, and `ROOT`; `auto` is a request value, not an
  authorizer. `AuthorizerResolver` defaults to Shizuku, Root, then none, respects configured order
  and enabled entries, and gives an explicit script selection priority over those settings. An
  explicit unavailable or denied selection returns its own error without falling back.
- `AuthorizerStates` preserves the Binder-facing facade while delegating to testable
  `ShizukuAuthorizer` and `RootAuthorizer`. `none` always has all three state booleans set to true.
  State reads do not create a root shell or request a permission dialog.
- Shizuku `available` means installed and compatible with the required API. An active pre-v11
  server has `available=false`, `running=true`, `granted=false`, and an API 11 requirement in
  `reason`. This maps to `AUTHORIZER_UNAVAILABLE`, including for an explicit request.
- Root `available` means an executable `su` was found or libsu has already confirmed root access;
  `running` is always true because Root has no separately managed server. `granted` reflects the
  current cached root shell or libsu's latest permission result. Missing `su` maps to
  `AUTHORIZER_UNAVAILABLE`; an available but refused root request maps to `AUTHORIZER_DENIED`.
- The Shizuku adapter registers Binder-received and Binder-dead listeners. Death/replacement of a
  previously observed server releases the pending permission request and retires only the Shizuku
  UserService cache. Initial and duplicate received callbacks are ignored by Binder identity.
  State is checked directly when queried, so delayed callbacks cannot make a dead Binder usable.
- Concurrent Shizuku permission requests share one dialog. Each generation has its own request
  code, ignores unrelated/late results, and removes its result listener when the last waiter
  finishes. Timeout and interruption cancel a posted prompt that has not started. A single short
  waiter does not cancel another waiter's request. Listener cleanup occurs outside the authorizer
  lock to avoid inversion with the Shizuku API's listener lock.
- Root authorization calls `Shell.getShell()` through a serial `RootShellQueue` and configures
  `Shell.Builder` with a 10-second timeout and no mount-master flag. The caller's smaller budget
  is respected; expiration produces `TIMEOUT`, not a fabricated permission refusal. Concurrent
  callers share the same pending authorization. Only the last departing waiter cancels it.
- `PrivilegedServiceBinding` obtains the RootService launch task using `RootService.bindOrTask`
  and runs it through the same shell queue. Authorization probes and service startup therefore
  cannot close each other's shell. The queue closes the shell after each operation, including
  failure or interruption; installation itself continues through the cached service Binder.
- `PrivilegedServiceBinding` links the service Binder's death recipient and handles null/dead
  bindings. Cleanup tolerates a dead authorization server. `SharedBindingCache` preserves one
  connection per authorizer, independent waiter cancellation, stale-callback rejection and
  replacement of a dead Binder; it also supports authorizer-specific invalidation.
- Each Shizuku binding generation uses a unique UserService tag. All waiters in that generation
  still share one service. A retiring service's delayed death callback cannot notify the next
  generation, and separate client processes do not compete for the same single-owner service.

The RootService launch adaptation follows the public task API in
[libsu 6.0.0 RootService](https://github.com/topjohnwu/libsu/blob/6.0.0/service/src/main/java/com/topjohnwu/superuser/ipc/RootService.java).
The shell lifetime and builder behavior were checked against
[libsu 6.0.0 Shell](https://github.com/topjohnwu/libsu/blob/6.0.0/core/src/main/java/com/topjohnwu/superuser/Shell.java).
The exact Shizuku 13.1.5 API source artifact in the Gradle dependency cache was inspected for
permission-result registration and Binder listener lifecycle.

## Focused JVM coverage

The tests cover these cases; pass/fail results are recorded in the integration run below.

| Test class | Cases covered |
| --- | --- |
| `AuthorizerResolverTest` | Default order, configured order, all enabled subsets, every meaningful Shizuku/Root state, explicit selection priority, missing state, invalid identifier, no implicit fallback |
| `ShizukuAuthorizerTest` | State reads without prompting, pre-v11 rejection, permanent refusal, shared permission request, unrelated/stale results, timeout, interruption, Binder changes, initial/duplicate received callbacks, synchronous result, listener cleanup |
| `RootAuthorizerTest` | Missing `su` vs unknown/denied/granted state, reads without prompting, shared request, independent deadlines/interruption, last-waiter cancellation and retry, 10-second cap, startup failure |
| `RootShellQueueTest` | Authorization and launch shell ownership, queued cancellation, cleanup on task failure or interruption |
| `SharedBindingCacheTest` | Shared first binding, one/last waiter timeout and interruption, dead Binder replacement, release-all, stale callbacks, per-authorizer invalidation |

## Device verification entry points

`AuthorizerDeviceTest` does not install or uninstall packages and does not change default handlers.
Its first test checks `none` and verifies repeated state queries do not create a root shell. Its
second test requires an explicit privileged authorizer and checks state/request, explicit-selection
priority, simultaneous authorization and binding, Binder reuse and cleanup. The Root branch checks
that no authorization or startup shell remains cached after the queue drains.

Run the usual repository JVM, debug and androidTest build tasks first. Then install the produced
debug app and test APK on the explicitly selected serial and run:

```powershell
adb -s <serial> shell am instrument -w -r -e class io.github.supermonster003.autojs6.plugin.three.setup.installer.AuthorizerDeviceTest -e authorizer shizuku -e expectedShizukuUid 2000 io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner
adb -s <root-serial> shell am instrument -w -r -e class io.github.supermonster003.autojs6.plugin.three.setup.installer.AuthorizerDeviceTest -e authorizer root io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner
```

For the four-client cache and rebind regression, run `PrivilegedClientDeviceTest` with
`-e engineAuthorizer shizuku` or `-e engineAuthorizer root` on the corresponding serial. Read SDK,
ABI and Shizuku UID again before recording the device evidence. A skipped privileged test is not
evidence that the privileged route passed. Existing granted permissions may be used; this suite
does not revoke them or claim to have exercised a new user-grant dialog when already authorized.
The Shizuku suite additionally releases/acquires eight consecutive generations without waiting
for the previous process to die, then verifies all eight retired services exited.

## Integration results

- Source inspection and scoped `git diff --check`: completed by the authorizer subtask.
- JVM: integration-generated `app/build/test-results/testDebugUnitTest/TEST-*.xml`, timestamp
  `2026-10-01T02:18:33Z`, reports all 30 focused authorizer cases passed with zero failures/errors:
  resolver 6, Shizuku 8, Root 6, root shell queue 3, binding cache 7. The authorizer subtask inspected
  these XML results; it did not launch a competing Gradle build.
- Initial device state/request checks: both `AuthorizerDeviceTest` cases report status code 0 in
  `build/device-api24-core.log` (AVD, Shizuku ADB), `build/device-api35-core.log` (Shizuku ADB), and
  `build/device-api28-core.log` (Root). This is evidence for those individual authorization cases;
  API 24/35 initially had separate core-matrix failures and their whole mixed suites are not
  claimed as passing here. Final authorizer/cache reruns follow the immediate-rebind fix below.

### API 24 immediate-rebind regression

The integration run `build/device-api24-regression.log` failed the four-client acquisition test
immediately after the last core installation test released its cached service. The collected
`build/api24-binding-logcat.log` records the preceding test finishing at 10:22:58.112, the old
UserService exiting at .116, the new service record starting at .121, and the new acquisition
failing with `Privileged service disconnected` at .124. The new service later attempted to attach
at .267, but its record had already been removed by cleanup.

Inspection of the exact Shizuku 13.1.5 source establishes the race: `unbindUserService(remove=true)`
destroys the remote record without immediately evicting the local service-connection cache. A new
bind with the same class/tag can join that retiring connection; its delayed `died()` notification
then disconnects the new waiter. Unique per-generation UserService tags prevent this cache alias.
The immediate-reacquisition device case and three Binder-received lifecycle JVM cases were added
for the fix. The final integration JVM reports contain 33 authorizer cases (Shizuku now 11),
all passing; the plugin total is 129 tests with zero failures.

Final device reruns on 2026-10-01:

- AVD API 24 / x86 / Shizuku ADB uid 2000: the core matrix, both cache cases and Binder suite
  pass together, 16 tests with no skips (`device-api24-final.log`). This includes four concurrent
  first acquisitions, shutdown/rebind and eight immediate generations without waiting for death.
- Xiaomi 23046RP50C / API 35 / arm64-v8a / Shizuku ADB uid 2000: authorization, both cache cases,
  Binder and explicit-confirmation suites pass together, 12 tests with no skips
  (`device-api35-final.log`).
- Sony G8441 / API 28 / arm64-v8a / libsu Root: the same selection reports 12 tests, 11 passing
  and the Shizuku-only eight-generation case intentionally skipped (`device-api28-final.log`).
  Root state/request, concurrent authorization/binding, no retained shell and four-client rebind
  pass. No skip is counted as coverage for a privileged route.
