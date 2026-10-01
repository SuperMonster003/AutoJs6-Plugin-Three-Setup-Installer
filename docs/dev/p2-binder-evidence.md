# P2 Binder routing and coordination evidence

Date: 2026-10-01 (Asia/Shanghai). This follows `p2-session-evidence.md`.

Integration update: the route and earlier handoff are now included in the logical P2 commits.
The registry uses one periodic expiry task, clears closed-lease callbacks, and passes the
2,048-completion queue-bound test. Final device results, including explicit confirmation on both
real devices and the opt-in fixture-only system-confirmation driver, are in `p2-core-evidence.md`.

## Delivered scope

`ThreeSetupInstallerPluginService` now returns `IInstallerPlugin.Stub`. All V1 methods route to real
implementations: identity/capabilities, authorizer state/request, inspect, installation sessions,
uninstall, default-installer state/set and users. The service no longer returns the placeholder.

- Metadata discovery remains cheap. Other methods enforce the installed official host package, its
  actual UID and minimum version, and the same nonempty signer set as the plugin. Session methods
  also require the UID that opened the session. A forged hostVersionCode does not replace the package
  manager check. This follows the stricter host protocol rather than accepting every permission holder.
- Request envelopes, JSON types and byte ceilings, item/split counts, matching host/request IDs, enums
  and PFDs are validated before scheduling package work. Descriptors must be readable regular files
  or pipes opened read-only. On actual remote calls the router closes the unmarshalled transport
  copies after duplicating them; a same-process call preserves the caller's descriptor.
- `SessionRegistry` permits four active installation sessions. Close/death cancels work but does not
  free its slot until its worker finishes cleanup. Terminal handles expire after ten minutes and
  retained handles have an independent limit of 128. The shared executor has four workers and a
  bounded queue of 16; queued cancellations still execute cleanup instead of being dropped by
  Future.cancel. Service shutdown cancels its sessions and outstanding one-shot operations.
- Callback Binder death cancels work. Registration handles death before worker start, and cleanup
  unlinks death recipients. Session status, progress and terminal callbacks use the contract envelopes.
- `SharedBindingCache` shares in-flight and established privileged connections. One waiter timing
  out or being interrupted does not disconnect other waiters. The last abandoned pending waiter
  retires the transport; late callbacks cannot resurrect it. Dead cached Binders are replaced. Root
  permission requests use a maximum ten-second shell startup timeout and close shells they create.
- `PackageInspector` returns the archive document, installed version/installer, signer comparison,
  signature scheme list and a bounded PNG icon. Ordinary APKs are reused for display metadata;
  containers extract their display APK through the shared parser. The full JSON budget takes
  precedence over including an optional icon. API 28 uses legacy signature fields when the modern
  signing-info field is absent; both flags are requested. Scheme tokens are normalized to lowercase.
- `DefaultInstaller` performs cheap state queries, preserves legacy requiresClear, checks all four
  APK filters after an enable, checks cancellation after privileged binding and only clears this
  plugin's defaults. Without the formal external APK entry, enable is refused. No user's default
  preferences were changed during this increment.
- Capabilities now advertise none/shizuku/root, batch/splits/silent-uninstall/users/inspect and the
  negotiated batch/split limits. The default-installer and delete-source features remain withheld
  until the corresponding external/source-owner paths are complete.
- `ConfirmationActivity` provides a basic, localized explicit confirmation, with package/version,
  resolved authorizer/user and requested privileged options. InstallSession passes the resolved target
  into its confirmation delegate; privileged uninstall uses the same confirmation owner. Cancellation,
  timeout and Activity destruction cannot approve the request. It is non-exported, uses 24 dp corners,
  bounded width, scrollable body and fixed buttons. Full P3 progress/results/options and P5 appearance
  settings are still pending.
- Result budgeting preserves confirmed installation outcomes. If optional metadata would exceed
  64 KiB, it omits long version names/notes/error text and explains the omission while preserving ok,
  version code, authorizer, actual interaction and error code/status. The host protocol document has
  the same clarification; no AIDL, public constants, AAR or host source code changed. This is a protocol
  documentation clarification, so no new host product changelog entry was added.

## Verification

81 JVM cases pass, including these 15 added cases:

| Test | Cases | Evidence |
| --- | --- | --- |
| `SharedBindingCacheTest` | 5 | Concurrent acquisition, one waiter timing out, last waiter interruption, stale/dead connection replacement and shutdown races |
| `SessionRegistryTest` | 4 | Capacity held through cleanup, terminal retention/expiry, early death/shutdown and retained-memory ceiling |
| `CallerPolicyTest` | 2 | Official package/UID/version/signer combinations and session owner isolation |
| `DefaultInstallerTest` | 3 | Missing entry/privileges, legacy clear requirement invalidation and refusal of partial preference writes |
| `InstallSessionTest` addition | 1 | Oversized optional metadata preserves confirmed success and a bounded result |

Debug, androidTest and minified release assembly pass. Lint remains at zero errors, 13 debug warnings
and 16 release warnings. Ten README/instruction/changelog locales and eleven string resource directories
are synchronized. Markdown and all 15 icon checks pass. The private test services are declared only
in the debug manifest, are non-exported, and have separate processes with valid identifier names.

Device SDK/ABI were read again:

| Device | Serial | SDK / ABI | Coverage |
| --- | --- | --- | --- |
| Xiaomi 23046RP50C | `968e9f18` | 35 / arm64-v8a | Binder boundaries and discovery, real Shizuku auto/dialog install/inspect/uninstall, confirmation refusal, four-client privileged connection sharing and rebind |
| Sony G8441 | `BH900ASK9E` | 28 / arm64-v8a | Binder boundaries and discovery, real Root auto/dialog install/inspect/uninstall, confirmation refusal, four-client privileged connection sharing and rebind |
| AVD_API_24 | `emulator-5554` | 24 / x86 | Binder boundaries and discovery, real none auto/dialog install/inspect/uninstall, plugin confirmation refusal before platform session allocation |

The seven `InstallerBinderDeviceTest` cases and five discovery cases have passing coverage on all three
devices, across the initial run and focused reruns. On the real devices the additional
`PrivilegedClientDeviceTest` passes. On the AVD, `PluginConfirmationDeviceTest` passes instead. The final
noninteractive regression intentionally omits engineAuthorizer, so the real-operation case is skipped
there; the explicit-authorizer runs provide that evidence. Final noninteractive logs report OK (12
tests) on each real device with one deliberate skip, and OK (13 tests) on the AVD with one deliberate
skip. The completed real-operation reruns each report OK (1 test), without a skip.

Evidence boundaries:

- The non-exported debug Binder endpoint has a same-UID test guard and a separate server process.
  It verifies actual Parcel transport, read-only/nullable/oversized inputs, one-shot replies, four live
  sessions, a refused fifth session and real callback-process death. The production endpoint is tested
  separately and rejects the plugin UID impersonating the host.
- A closed PFD cannot be marshalled normally, so its INVALID_ARGUMENT result is tested through the
  local router. The source owner retains valid descriptors after inspect and session calls.
- Real installation/inspection/uninstallation runs through the V1 router in the plugin's main process
  with an injected test identity. This keeps status receivers and confirmation tickets in their
  production process. It is not an end-to-end run initiated by the official host's UI or UID.
- The real batch is v1 APK / malformed package / v2 APK. Successful items, previous/current versions,
  actual interaction and retained source ownership are checked. Inspect after installation verifies
  the installed version and signer match. Each run then removes its own fixture, and refuses to start
  if the fixture or retained data already exists.
- The AVD dialog run accepts the plugin's explicit confirmation before each valid item, then the
  system confirmation; uninstall uses its system dialog. The separate refusal case clicks Cancel
  in the plugin-owned fixture dialog, receives USER_CANCELLED and verifies that PackageInstaller
  allocated no session.

Issues found and resolved during verification: invalid hyphens in debug process identifiers prevented
APK installation, an uppercase parser scheme list was initially omitted, and API 28 needed the legacy
signature-field fallback. The first AVD auto rerun was cancelled by a stale automation coordinate
after a truncated XML read; using a pulled, fully parsed UI hierarchy fixed the driver and the focused
run passed. None of these failed attempts are counted as passing evidence.

An initial API 35 explicit-dialog attempt timed out behind secure keyguard without installing the
fixture. The maintainer then unlocked all real devices. Both API 35 Shizuku and API 28 Root subsequently
passed explicit-dialog installation/update/uninstallation and refusal before session allocation,
each reporting OK (2 tests), without skips. The lock was not bypassed. The first Sony dialog run
could not be driven reliably with an external accessibility reader; the successful rerun uses the
instrumentation runner's own accessibility connection and an explicit confirmFixture opt-in restricted
to the fresh fixture package in this plugin's window. The product's consent logic is unchanged.

Logs and screenshots are local build artifacts, not committed. Reproduce after installing both APKs:

```powershell
adb -s DEVICE_SERIAL shell am instrument -w -r -e engineAuthorizer shizuku -e class io.github.supermonster003.autojs6.plugin.three.setup.installer.InstallerBinderDeviceTest,io.github.supermonster003.autojs6.plugin.three.setup.installer.PrivilegedClientDeviceTest,io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPluginContractTest io.github.supermonster003.autojs6.plugin.three.setup.installer.test/androidx.test.runner.AndroidJUnitRunner
```

Use root or none as appropriate. `binderInteraction=dialog` exercises explicit plugin confirmation;
an unlocked device and approval of the dedicated fixture are required. For privileged fixture-only
dialog tests, `confirmFixture=true` lets the test runner click its own fixture confirmation buttons.
This option exists only in androidTest and refuses to start if fixture data already exists. The standalone
PluginConfirmationDeviceTest automates refusal and skips a locked device. Omit engineAuthorizer for
noninteractive boundary checks; do not count the resulting fixture-test skip as an operation pass.

## Remaining integration

P2 as a whole remains open: seekable-source optimization, content-origin and full format fixtures,
real split containers, downgrade/test/low-target options, secondary/all-user operations, authorizer
settings and protected-app/OEM matrices still need their own evidence.

The next focus is P3: complete install information/progress/results, external APK and share entry,
system-user-action ownership and foreground notifications. The external entry enables the default
installer capability; source deletion must still be performed by the source owner.

The current host `AidlPluginHost.buildBindFlags` adds BIND_AUTO_CREATE and the optional external-service
flag, but no BIND_ALLOW_ACTIVITY_STARTS. Per the [Android 14 activity-start rules](https://developer.android.com/about/versions/14/behavior-changes-14#background-activity-starts),
a visible host targeting API 34+ must explicitly grant its bound service that ability. P3 must add an
installer-scoped opt-in and verify foreground/background/locked behavior alongside notification
fallback. This is a source-based integration finding, not a completed cross-UID host UI test. Do not
claim all three host entry points work on API 34+ based on the internal tests above.

Earlier uncommitted work is preserved. VERSION_BUILD remains 10 (the current HEAD commit count).
No repository was pushed; the only host edit in this increment clarifies bounded response metadata
in the already-modified protocol document. Changes remain uncommitted for integration with the
pre-existing P2 work, consistent with the earlier handoffs and AGENTS.md section 3.3.
