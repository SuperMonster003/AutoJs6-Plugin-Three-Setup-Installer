# P8 Dhizuku identity and session recovery evidence

Date: 2026-10-02.

This evidence covers the isolated Dhizuku framework transport, durable session ownership,
and recovery after a creator process exits. It does not claim a completed installation is
rolled back after a process exits, or that recovery runs automatically at application startup.

## Implementation boundary

- Before initializing the Dhizuku API, the plugin reads the current system device/profile
  owner and verifies that the resolved Dhizuku provider belongs to that owner's package,
  UID and current user. It records the owner component, provider component and signing
  identity. The API's cached owner must still match the system owner. Each checked
  transaction verifies the captured identity and current grant under one transport lock.
  An owner that cannot be proved is unavailable; a package claiming another owner's
  provider authority is not trusted. This does not grant cross-user access.
- Framework managers and sessions are newly created objects. The plugin does not replace
  cached application/global managers. A failure while wrapping an opened platform session
  attempts to close that same session reference. Reflection wrappers and the Dhizuku API's
  plain `RuntimeException(RemoteException)` wrapper are removed before failure mapping;
  failed writes are not retried.
- A session is registered as active before creation. After the platform returns its ID,
  the plugin checks its metadata and atomically records it before opening the APK stream.
  The journal contains the known ID, owner/provider/signature/user identity, package,
  size, creation time where available, creator PID/start time, and an opaque random token.
  It does not contain the APK bytes, caller file path or source URI. The platform's
  `originatingUri` is a private `android-app://<plugin>/dhizuku-session/<UUID>` nonce.
- Recovery runs before the next valid Dhizuku install or uninstall, while the owner and
  grant are available. It queries only IDs in the plugin's private journal. A live creator
  or uncertain process identity is retained. Owner, nonce or package metadata mismatches,
  malformed records and identities that cannot be proved are retained. An absent session
  retires only its matching record. A matched orphan is abandoned by its exact ID.
- API 28+ exposes the nonce through the public `SessionInfo.originatingUri` getter. API
  26/27 normal execution still closes/abandons known sessions, but a surviving session
  after process death cannot prove the nonce and is retained. A lost `createSession`
  reply, or death before its returned ID is durably recorded, is likewise outside this
  recovery proof. The plugin does not scan and delete other sessions owned by Dhizuku.
- Recovery never replays an install or uninstalls a package. After commit, the platform
  might already have installed the package: abandoning a remaining known session cannot
  promise to undo that installation. This test round deliberately stops before commit.
  A confirmed terminal result is preserved even if record retirement later fails.

## Build and device

The integrated build used an intermediate `1.0.0 / 51` Debug build; this is not a release
announcement. The Gradle JVM suite passed 311 tests, 0 failures and 0 skips, including
5 journal codec/identity tests and 3 framework failure/provider tests. Debug and
androidTest compilation and packaging succeeded. The parent build log is
`build/p8-ready-integrated-build.log`.

Test APKs were copied before subsequent builds could overwrite their input paths:

| Artifact | SHA-256 |
| --- | --- |
| `build/p8-dhizuku-recovery/debug51.apk` | `e6aa6e5685b31a32a0cb3f76aba3be9d6a5af5cccbb5c2d106b0b5117cbf27a1` |
| `build/p8-dhizuku-recovery/androidTest51.apk` | `9ffc932a7f0b2602cc90cb18fd913291cd31c9699549afd90f38ed2b4be7d8ef` |

The only device used for these mutations was the newly created dedicated `emulator-5562`,
API 31, x86_64, user 0. `com.rosan.dhizuku` was already its device owner and had granted
the plugin through its normal UI. The tests did not activate/change the owner, change
unrelated settings or stop the owner process. Existing fixture packages and recovery
records are rejected before mutation. Only the signed fixed no-code spike APK and known
test-created session IDs were used.

## Device results

The following 6 tests completed with `OK (6 tests)`, 0 failures and 0 skips. Log:
`build/p8-dhizuku-recovery/device-integrated.log`.

| Test | Observed result |
| --- | --- |
| Cancellation after real APK stream progress | `CANCELLED` before commit; the exact session was abandoned; journal and session set returned to baseline. |
| Recovery while a same-process lease is active | The registered session was retained; explicit test cleanup then removed only that ID. |
| Real independent creator process death | A non-exported Debug service in a separate same-UID process wrote/fsynced 4096 bytes without commit. Recovery while that creator was alive retained it. After unbinding and killing only creator PID 12163, its Binder died but owner session 338186119 still existed. Recovery abandoned exactly that known ID. Unjournaled owner session 571284905 survived, then was explicitly abandoned by its creating test. |
| Dhizuku install/update/uninstall | Fixture v1 installation, v2 update and uninstall completed silently; recorded installer was the Dhizuku owner package; session set returned to baseline. |
| Dhizuku persistent default round trip | Four APK resolution forms selected the plugin after the acknowledged write; explicit clear restored the original defaults. This does not treat a saved receipt as proof of a later current policy. |
| Remote Binder V1/V2 envelope boundary | The V2 request/capability path and compatible V1 response envelope passed the dedicated remote-service test. |

The death probe is Debug-only, non-exported, same-UID gated, requires explicit fixture
opt-in and checks the matching signed test APK. It does not add a production endpoint
or kill the Dhizuku manager. The lifecycle test exercises a real OS process death;
the post-death recovery method is called directly by the instrumentation process.
Production recovery is triggered by the next Dhizuku install/uninstall as stated above.

## Cleanup and remaining coverage

Read-only `before.json` / `after.json` snapshots and full dumps are retained under
`build/p8-dhizuku-recovery/` (ignored local evidence). The audit verified:

- Identical user-0 package set including retained-data entries; no fixture package/data.
- Identical plugin UID, device owner and unknown-source appop.
- Identical complete ordinary preferred XML, SHA-256
  `b083a008a6f0ad5619c1946d52880711944d5abf7ce2f7a740008aaaadaddcc7`.
- No active platform sessions, no private preference/history/journal files, empty cache,
  no recovery-probe process and no installer foreground service.

The temporary empty probe parent directory was removed only with `rmdir` after all case
files were gone. The AVD was handed back to the coordinating task with the verified
Debug 51 build and its original owner/grant, for subsequent host and final Release checks.
It was not a user device requiring restoration to an older released plugin.

This round does not establish managed-profile operation, API 26/27 orphan recovery,
post-commit process-death outcome recovery, owner replacement on a real device, or
automatic startup cleanup. Provider spoofing and changed owner/signature/metadata are
covered by JVM rejection tests; they were not simulated by replacing this AVD's owner.
Subsequent persistent-policy changes and final Release/host checks require their own
evidence and are not silently included in this fixed-APK result.
