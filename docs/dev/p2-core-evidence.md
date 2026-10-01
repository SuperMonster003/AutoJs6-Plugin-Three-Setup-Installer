# P2 core integration evidence

Date: 2026-10-01 (Asia/Shanghai). This consolidates the earlier engine, session and Binder handoffs
and the subsequent source, authorizer and device-matrix work. P2 remains open where it depends on
the external entry, complete UI, source deletion or the full host-entry acceptance matrix.

## Accepted roadmap sections

- P2.1: descriptor / content / legacy-file sources, retained seekable views, cancellable pipe
  staging, format selection, inspect enrichment and format/limit tests. A necessary File-parser
  fallback for denied procfs reopening is explained in `p2-source-evidence.md`.
- P2.2: explicit and automatic authorization resolution, supported Shizuku versions, state and
  permission requests, bounded Root authorization, shell lifetime, shared first binding and
  reconnection. See `p2-authorizer-evidence.md` for the actual immediate-rebind failure and repair.
- P2.5: serial batch results, continue-on-error/cancellation, current and selected users,
  three distinct packages in one Shizuku batch, and a real managed-profile installation.
- Earlier P2.4 and the completed portions of P2.6 were audited and included in logical commits.
  Public production identity checks remain separate from debug-only test identities. No debug
  route or test provider is exported in the release APK.

## Build and JVM verification

- Plugin: 129 JVM tests, zero failures/errors. This includes 18 format cases, 33 authorizer cases,
  the streaming digest checks and eight session-registry cases. The latter exercise 2,048 rapid
  complete/close operations while the scheduled queue stays bounded by one reaper task.
- Debug, androidTest and minified release APK assembly pass. Native alignment checks confirm
  the plugin has no native libraries. Debug lint has 0 errors / 14 warnings; release lint has
  0 errors / 17 warnings. Existing warnings are retained instead of disabling checks.
- Host shared parser: 54 JVM tests, release AAR assembly and release lint pass. Host commit
  `0767971bc9` recognizes ordinary ZIP/renamed containers containing APKs. Its release AAR is
  pinned by SHA-256 in `locks/host-api-aars.lock`; public AIDL and contract AARs are unchanged.
- Ten-language Markdown generation/check and deterministic launcher-icon checks pass. Changelog
  entries describe the final P2 core behavior; README still identifies this as a development
  preview with UI, external opening, script API and settings unfinished.
- The final build uses VERSION_BUILD 17. Its five activation/discovery/PluginInfo contract tests
  pass on API 24 (`device-final-build-contract.log`) after rebuilding all APK variants and rerunning
  the 129 JVM tests and both lint variants.

## Device evidence

SDK, ABI and build type were read again before testing. All package operations used the signed,
code-free fixtures in androidTest assets. Tests refuse pre-existing fixture packages/retained
data; the core matrix checks all users and requires a valid package-list sentinel. Cleanup
verified that no core or spike fixture packages remained on the three devices.

| Device | API / ABI / build | Authorization | Confirmed coverage |
| --- | --- | --- | --- |
| AVD_API_24, emulator-5554 | 24 / x86 / userdebug | Shizuku ADB uid 2000 | All eight read-only source cases; state/request; test-only flags; debug/release downgrade behavior; real XAPK base + feature; installer attribution; managed-profile install; concurrent first binding and eight immediate rebinds; V1 Binder boundary and real operations |
| AVD_API_24, emulator-5554 | 24 / x86 / userdebug | none | Real system-confirmed low-target and XAPK installation; rejection of test-only without its privileged flag; V1 v1 / malformed / v2 batch followed by system-confirmed uninstall; Binder boundary/lifetime checks |
| Xiaomi 23046RP50C, 968e9f18 | 35 / arm64-v8a / user | Shizuku ADB uid 2000 | Eight source cases; state/request; test-only and downgrade flags; real XAPK; attribution; low-target observations below; three distinct packages in one batch; both cache/rebind cases; V1 operations with explicit plugin confirmation and declined-confirmation guard |
| Sony G8441, BH900ASK9E | 28 / arm64-v8a / user | libsu Root, existing Magisk setup | Eight source cases; state/request and no retained shell; test-only and downgrade flags; real XAPK; attribution; pre-34 bypass note; cache/rebind; V1 operations and explicit confirmation; session/keepData regression |

The eight source cases and two state/request cases passed individually on all three devices.
The initial mixed run found two additional issues, so its whole-suite result is not described
as passing. The meaningful final combinations were:

| Run (ignored log under build/) | Passed | Intentional skips | What the combination establishes |
| --- | ---: | ---: | --- |
| device-api24-final.log | 16 | 0 | Seven core matrix cases, both binding-cache cases and seven Binder cases after the user-query and rebind fixes |
| device-api35-final.log | 12 | 0 | Authorization, both binding-cache cases, Binder and declined-confirmation cases; real route uses explicit dialog |
| device-api28-final.log | 11 | 1 | Same selection using Root; only the Shizuku-specific eight-generation case skips |
| device-api24-none-final.log | 10 | 5 | Three applicable core cases plus seven Binder cases; privileged-only cases skip |
| device-api35-three-batch.log | 1 | 0 | Three different signed package identities installed in one serial session, each version verified |
| device-api35-regression.log | 14 | 1 | Seven-case matrix and earlier cache/Binder selection; managed-profile case skips because no disposable profile exists on the real device |
| device-api28-core.log | 16 | 1 | Sources, authorization and seven-case matrix; managed-profile case skips |
| device-api28-regression.log | 14 | 0 | Earlier cache case, Binder and session/keepData tests |

The API 24 none driver initially looked for the wrong system button id. It was corrected to
recognize the system package-installer button/text only while the fixture label is visible.
The final run passes with an explicit `confirmFixture=true` opt-in. Product confirmation behavior
was not altered. No device lock was bypassed.

## Platform observations and repairs

- Android 7 has no `IUserManager.isUserRunning(int)`. The new managed-profile case exposed this;
  API 24/25 now query `IActivityManager.isUserRunning(userId, 0)`. Reflection failures are wrapped
  in a Binder-supported exception instead of escaping as an unmarshallable checked exception.
  The repeated test installed only for managed user 10 and confirmed no installation for user 0.
  The temporary profile created on the AVD was removed afterward; the user list returned to 0 only.
- Debuggable v2 -> v1 is refused without allowDowngrade and succeeds with it on the privileged
  matrix. With both downgrade flags, non-debuggable packages remain refused with
  INSTALL_FAILED_VERSION_DOWNGRADE on the API 35 HyperOS and API 28 Sony user builds. The API 24
  userdebug image accepts that downgrade. Reported outcomes and installed versions are asserted.
- HyperOS API 35 accepts targetSdk 22 even without bypassLowTargetSdk. The test records that fact,
  removes its first installation, then verifies a fresh install with the bypass flag. This proves
  flag acceptance on that ROM, not removal of a block that was never present. API 24/28 verify
  that the option is ignored with a result note. A blocking API 34+ platform remains a matrix gap.
- Shizuku's delayed death notification could disconnect a new service using the old local
  connection tag. Each binding generation now has a unique tag, while concurrent callers still
  share that generation. Both AVD and Xiaomi pass the eight-generation regression.
- Closed terminal handles previously remained referenced by individual ten-minute timers.
  One registry-level reaper now owns expiry, and closing a lease clears its stop callback.

## Remaining scope

Source deletion/keepSourceOnFailure and its external ownership path, the complete UserActionActivity
and unknown-source grant flow, progress/result UI, foreground notification fallback, production
host UID launches on API 34+ and all host entry points remain open. The full three-authorizer/API
matrix is not complete; real `user: all` installation was not claimed. The current UI is the basic
explicit confirmation delivered with the earlier P2.6 integration.

Continue with the original P3 sections and finish the dependent P2 items when their UI and external
entry paths exist. The roadmap structure and phase sequence are unchanged.
