# P3 foreground service and notification evidence

Date: 2026-10-01.

Real 2 GiB XAPK installation has passed in the background on the Xiaomi API 35 device with its existing notification denial preserved. Visible progress updates have separately passed with a short real APK on the API 24 emulator. These two results do not establish visible notification updates during a 2 GiB transfer. The final publication-lock fix has passed the full build, its deterministic JVM tests, and subsequent device replay on both API 24 and API 35.

## Implementation and platform behavior

- `InstallNotifications.update` accepts worker callbacks, retains at most four sessions and coalesces progress publication. Preparation alone does not start a foreground service. After the first writing stage, a batch keeps service coverage until the real terminal result. A bounded set of completed tokens rejects late progress callbacks.
- One `InstallForegroundService` uses channel `installation`, declares and passes `dataSync` on supported APIs, and returns `START_NOT_STICKY`. Process recreation never replays an installation. Completion removes only that session; the last session stops foreground work. Result notifications are separate.
- Android 12+ can reject a background foreground-service start. The implementation catches that failure, publishes an ordinary notification when permitted, and lets `resumeForeground` retry service protection after the user opens the installer. It does not change an authoritative installation result. See [Android foreground-service background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).
- Android 15+ applies a cumulative six-hour background budget to `dataSync` in a 24-hour period. `onTimeout` stops the service immediately and asynchronously requests cancellation of unfinished sessions. The engine's confirmed-result boundary remains authoritative. This timeout path is implemented but was not exercised by the short runs below. See [Android foreground-service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout).
- Notification denial does not block an otherwise allowed foreground-service start. The mandatory foreground notification is still supplied; optional progress/result/confirmation notices decline publication, and no worker opens a permission prompt. See [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- Notification taps use immutable direct Activity `PendingIntent`s for plugin-owned screens. Unique categories distinguish tokens/purposes without changing source data or extras. There is no service/broadcast trampoline that opens UI. An explicit non-exported receiver invokes each session's existing cancellation callback at most once, off the main thread. See [Android activity launch security](https://developer.android.com/guide/components/activities/secure-bal).
- Result publication is synchronous under the same lock as ownership removal and notification cancellation. Action/progress publication uses that lock too. Removal that wins first suppresses publication; removal after an in-flight publication cancels it before returning. The implementation reuses the existing maximum of 128 terminal tokens and retains no queued completion callback per installation.

## Build, JVM and notification-contract results

`build/p3-complete-build.log` records `BUILD SUCCESSFUL in 1m 10s`, including JVM tests, debug, instrumentation APK, release, debug lint and release lint. This build includes the final publication-lock fix. The matching JVM XML reports show five passing `InstallationNoticeRegistryTest` cases and six passing `InstallationNoticePublicationsTest` cases, with zero failures, errors or skips. They cover session limits, finite progress, cancellation idempotence, late callbacks, both publication/removal orderings, synchronous completion, exceptions and bounded terminal retention. Thread barriers control the race tests.

`build/p3-api24-ui-final.log` records all 47 selected device tests passing in 18.856 seconds. Its three `InstallNotificationIntentDeviceTest` cases verify token isolation even for colliding Java hashes, separate progress/result/action identities, preserved source intents, and rejection of another package as a notification Activity target. These initial device results preceded the final publication-lock change. The final API 24 replay below includes those PendingIntent checks again.

## Initial background installation results

| Observation | Xiaomi 23046RP50C / API 35 | Android SDK built for x86 / API 24 |
|---|---|---|
| Log | `build/p3-api35-fgs-large.log` | `build/p3-api24-foreground-entry-final.log` |
| Source | XAPK, 2147501419 bytes | APK, 8536 bytes |
| APK bytes selected and actually written | 2147500874 across two APKs | 8536 across one APK |
| Installation result | Real silent installation succeeded; version and installed APK byte total matched | Real silent installation succeeded; version and installed APK byte total matched |
| Existing notification state | `postPermission=false`, `drawerEnabled=false` | `postPermission=true`, `drawerEnabled=true` |
| Background interval | 12016 ms | 1507 ms |
| Foreground-service observations | 39 | 8 |
| Process | PID 4791 remained unchanged | PID 5833 remained unchanged |
| Visible notification progress | Not verified; recorded progress list was empty | Observed `[0, 100]` |
| Observation hold in the byte callback | 0 ms | 1200 ms, explicitly for short-operation observation |
| Measured operation elapsed time | 30262 ms | 2912 ms |

The API 35 test completed in 31.251 seconds including test overhead and reported `OK (1 test)`. Its two source manifests had no explicit permissions or components and targetSdk 35. The real `dataSync` service remained foreground while the write ran after HOME, with no foreground-service type/start exception. The denied notification state was preserved; this is evidence for the denial branch, not for a visible notification drawer.

The API 24 group contained six selected cases: five passed and the large-XAPK case was skipped because `largeFixturePath` was absent. The runner printed `OK (6 tests)`, but its `-4` assumption status is explicitly counted as a skip, not large-package success. Group duration was 7.712 seconds. Its short-transfer evidence records the actual installation Activity as `STOPPED`, `focus=false`, `visibility=8` after HOME. The 1200 ms callback hold made the real tiny APK's progress observable across notification frames; it is not a sustained large-transfer benchmark.

An earlier API 24 probe timed out while waiting for `UiAutomation.rootInActiveWindow` to change package. The service and initial notification had already been observed. System/lifecycle logs showed that HOME had started the launcher and stopped the installer. The final probe therefore requires the actual Activity to be `STOPPED` and unfocused; accessibility-root state is diagnostic only. The final log above confirms this stronger lifecycle observation.

Both tests verify actual installed version and the exact `sourceDir` plus `splitSourceDirs` byte total. They also wait for foreground-service termination and check that notification permission remains unchanged. The process is instrumented; an unchanged PID during these runs does not demonstrate survival under arbitrary memory pressure.

## Final publication-lock device replay

The final fix was replayed using the same real installation checks on both permission branches:

| Observation | API 24 short APK | Xiaomi 23046RP50C / API 35 large XAPK |
|---|---|---|
| Log | `build/p3-api24-publications-final.log` | `build/p3-api35-publications-final.log` |
| Selected test group | 21 passed, zero failures or skips | 14 passed, zero failures or skips |
| Group duration | 12.721 seconds | 33.468 seconds |
| Actual APK bytes written and installed | 8536 | 2147500874 across two APKs; source remained 2147501419 bytes |
| Existing notification state | `postPermission=true`, `drawerEnabled=true` | `postPermission=false`, `drawerEnabled=false` |
| Background Activity | `STOPPED`, `focus=false`, `visibility=8` | `STOPPED`, `focus=false`, `visibility=8` |
| Background interval | 1535 ms | 11556 ms |
| Foreground-service observations | 8 | 44 |
| Process | PID 6482 remained unchanged | PID 16607 remained unchanged |
| Visible notification progress | `[0, 100]` | Not verified; progress list remained empty |
| Byte-callback observation hold | 1200 ms | 0 ms |
| Measured operation elapsed time | 2176 ms | 28938 ms |

Both installations succeeded with matching installed version and APK byte totals, stopped their foreground service, and preserved the pre-existing notification permission state. Both groups also include the updated plugin capability contract and notification-bridge cases. API 24 reruns the PendingIntent and external-entry tests; API 35 additionally covers actual unknown-source cancellation and task cleanup. Detailed acceptance for those surrounding flows belongs to the main P3 integration record.

The publication-lock device replay is complete. The large-file notification-denial branch and the short-file visible-progress branch remain distinct; this replay still does not demonstrate a visible notification during a 2 GiB transfer.

## Fixture and ownership controls

`tools/build-large-install-fixture.ps1 -SizeMiB 2048` successfully generated the large fixture under ignored `build/large-install-fixture-2048/`. The helper verified two independently signed APKs, each 1073750437 bytes, before packaging them in a stored XAPK. The payload is 2147483648 bytes; the APK total is 2147500874 bytes; the source total is 2147501419 bytes. Neither individual APK crosses 2 GiB. Source SHA-256 is `f7e75b0161e3ad633d2457ad5426ea253c2d793e333e219b9ab5a5d8dc243ec8`.

The helper uses a separate test key and produces no application code, permissions or components. Its metadata sum was corrected to index each PowerShell `OrderedDictionary` entry directly; only the metadata changed, not the XAPK or its digest. The helper removes only explicit intermediate files within its checked build directory and performs no device operations.

`LargeInstallForegroundDeviceTest` accepts the separately transferred XAPK through `largeFixturePath`, inside the plugin cache, and requires an explicit privileged `foregroundAuthorizer`. It checks both source size and actual selected APK total against 1984 MiB, preventing container padding from faking the size requirement. The short mode requires `foregroundShortFixture=true` and uses the existing code-free core fixture.

Before either installation, all device users' package lists, including retained data, must prove the fixed fixture package absent. Every parsed APK manifest must match the dedicated package/version and explicitly request no permissions. PackageManager's no-code/component checks remain in place; platform compatibility permissions are logged separately from source declarations. Cleanup waits for the worker to stop, removes only the freshly installed fixture, releases privileged bindings and deletes only the test-created short source. The test leaves an externally supplied large source for the runner's explicit evidence collection/cleanup.

## Remaining matrix

- A 2 GiB transfer with notification permission already granted on API 34+ and visibly changing notification progress has not been demonstrated. The API 35 denial run and API 24 short visible run cannot be combined to claim that result.
- The cumulative `dataSync` timeout callback, background-start rejection followed by notification-tap recovery, multiple concurrent foreground sessions, and physical notification cancellation/taps need dedicated runtime evidence beyond these transfer and PendingIntent checks.
- This file does not claim the complete P3 device matrix, real host-UID integration, or every install/uninstall dialog path. Detailed results for the surrounding confirmation, refusal and task-lifecycle checks are retained in the main P3 integration evidence.

Notification strings have been merged into the product locale resources; `build/p3-notification-strings.json` was only an ignored staging file. Referenced logs and generated APK/fixture artifacts remain local ignored output; the measured outcomes and remaining gaps are preserved here.
