# CI UI window synchronization

Date: 2026-10-02. Scope: repair the intermittent Android UI test failure in [run 37015190116](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/actions/runs/37015190116), source 8a39ff6/build 65. No production installation behavior, public contract, published v1.2.0 tag or build 63 APK is changed. The host repository remains local-only.

## Failure evidence

The JVM/APK/lint job and API 24 job succeeded. API 35's actual HTML report contains 212 tests, 7 failures and 64 skips, or 141 passing tests. Its console Finished 276 counter is not the real test total and must not be used as a pass count. For the same reason, older records quoting the console's Finished totals are progress observations, not additional test executions.

The first failure was failureShowsCodeSystemMessageAndCopiesThem at primaryClip!!. The installed-app sorting test then failed to find its Cancel action. Both standalone appearance cases timed out on HomeActivity window focus, and the three geometry cases failed on missing focus or a screen-coordinate tap that was not received. In the two IME cases showAccepted was still null: they had not reached showKeyboard, so the trace is not evidence of a keyboard implementation failure.

ActivityScenario launch waits for lifecycle state. It does not make the first onActivity callback a proof of input focus. Android explicitly permits [ClipboardManager.getPrimaryClip](https://developer.android.com/reference/android/content/ClipboardManager#getPrimaryClip()) to return null when the caller has no input focus. The previous test performed Copy and immediately dereferenced the clip in one callback, without a focus wait.

The actual AndroidX Test Core 1.7.0 ActivityInvoker also introduces EmptyActivity while finishing a live scenario and sends asynchronous broadcasts to dismiss its helper activities. Its API 33+ receivers are exported, so there is no source evidence for a cross-UID receiver rejection. The original CI artifact lacked the active-window owner and a screenshot before teardown. It therefore does not prove that an AndroidX helper, system overlay or any particular production window caused every later focus failure.

## Changes

- The Copy test first waits for an attached, laid-out, focused window, clicks once, and polls the nullable clipboard from the test thread until its text matches the existing expected error. It keeps the equality/privacy assertions and shares the original 10-second budget across both waits.
- The installed-app test waits for its real window before opening or reopening the sort dialog and after recreation, and recycles the accessibility nodes it polls. Dialog choices still use actual visible/enabled accessibility actions.
- The four affected classes finish only the Activity owned by their scenario and wait for DESTROYED before closing ActivityScenario. Existing Done actions, worker/record lifetimes, private appearance restoration, orientation and IME restoration remain. A cleanup error is suppressed onto the original failure rather than hiding it.
- Geometry sampling always requires focus, including landscape. Before a coordinate tap the test takes another stable sample and retains all original size, inset, visibility, IME and click-count assertions. No failing scenario is skipped and existing wait budgets are not enlarged.
- Before cleanup, failures can save bounded window/activity/IME diagnostics, a screenshot and accessibility window identity/focus metadata without node text. These are test-only, individually best effort and cannot replace an assertion. No extra generic success status is emitted to the instrumentation reporter.
- A single Bash wrapper runs inside emulator-runner so its failure trap executes before the emulator is killed. It preserves the original test exit code, collects device logs and copies only the generated safe filenames from cache/ui-test-failures. CI always retains HTML and raw Android results; failure-only device evidence is included when present. This permits accurate counts and inspection of the actual interrupting window if a different environmental interruption occurs.

## Original-package local reproduction

The exact original CI artifact 11229532752 was downloaded and fixed locally:

| APK | Bytes | SHA-256 |
|---|---:|---|
| Main Debug | 7890119 | `b8343805742ad3eb8003f9ab7ed0a216c5e6a01465ad554345fed10a8397f189` |
| androidTest | 1165952 | `4b9e259cb12e777e6161b5d440073b1cae925574f19cdfff00fdd98b93a0bba6` |

A new task-owned API 35 google_apis/x86_64 pixel_7 AVD was used, serial emulator-5564, name Three_Setup_CI37015190116_API35_a29fa2bd. The local emulator is 37.1.11, compared with CI 37.2.12; that difference is retained. No existing user device was modified, no privileged fixture was enabled, and the initial run only used the workflow's animation settings. No screen timeout, error-dialog suppression or forced window focus was added.

The original full default suite passed 148 cases with 64 original opt-in skips and no failure, out of 212 actual cases, in 116.809 seconds. A subsequent run of the four affected UI classes passed 19/19 in 70.411 seconds. This confirms that the cloud problem is intermittent, not that the old tests are reliable.

A second targeted original-package run was interrupted by a separately observed ADB transport failure. The host received 16 starts and 15 successes before exit 255. Guest system_server/adbd PIDs and uptime continued; adbd logged the connection loss/reconnection. UiAutomation then failed with DeadObjectException and the guest recorded four secondary failures. That incomplete run is not counted as a pass or described as reproducing the original seven cloud assertions. The original appearance recovery journal reported restored. Evidence is under ignored build/ci-37015190116-repro-a29fa2bd/.

## Local repair checks and remote verification

The repaired tree passed all 394 JVM tests with zero failures/errors/skips, Debug and androidTest assembly, and Debug lint with 0 errors / 51 warnings. The new shell wrapper passed syntax validation and three isolated smoke cases: successful tests make no diagnostic calls, a test failure retains its exit code, and diagnostic command failures still retain the original failing exit code. The fake-device smoke also verifies that traversal filenames are not copied.

The local fixed Debug pair has main SHA-256 `0b466f5306dc69015b926dd3d14ea03f9d115e435038ab4c6c666b1bb36ddd3d` (7890119 bytes) and test SHA-256 `1833646f445f70970ab6413c9c7ecd2c5cc38eefaa3d6a9261bf7a2877b60320` (1669707 bytes). Its signing identity differs from the CI-generated debug key; only the new task-owned AVD's two test-owned packages are replaced for validation, without a signature bypass or touching user packages.

That fixed pair passed all 19 selected UI cases, with no failure or skip, in 67.626 seconds. This run kept a single logcat connection and used no external accessibility/window poller. The device remained online, both installed hashes matched the fixed files, the appearance plan was restored, and no active instrumentation or install session remained. Results and restoration checks are in build/ci-37015190116-repro-a29fa2bd/fixed-ui-validation/. No failure-only diagnostic files were expected on that passing run.

The GitHub checks attached to the repair commit provide the full remote matrix result and its API 35 repetition. Both passing and failing HTML/raw reports are now retained rather than relying on the progress counter. Historical failing runs remain visible. The published APK and tag are immutable, and no host remote branch is pushed.
