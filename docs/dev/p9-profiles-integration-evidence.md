# P9 source-profile ecosystem synchronization

Date: 2026-10-02. This completes the fourth original P9 item after source-profile implementation. P7 remains delayed. All repository changes below are local commits, without pushes, tags or remote publication.

## Host and API

AutoJs6 commit `48376c3b64901de0e6b4e0c0a565a41341a60e0a`, build 5312, implements the optional V3 source-profiles negotiation, known explicit-field masks, the three nullable script resets and source cleanup shared by scripts and host UI. The host captures its negotiation fact before opening a session, so an immediate callback cannot lose that fact. Missing or malformed cleanup decisions and explicit false preserve sources, including same-inode aliases referenced by another retained or failed item. Unknown programmatic option-presence masks keep legacy behavior.

The installer-api module passed 10 JVM tests. The host installer suite passed 105 tests across 14 classes, with no failures/errors/skips, and production/androidTest Kotlin compiled. The checked-in API AAR, its exact digest and basic/profile host version boundaries are recorded in [implementation evidence](p9-profiles-evidence.md). The 11 AIDL transactions are unchanged. Shared parser source/digest remains the prior P9 policy artifact.

The fixed host APK is 6.8.0/build 5312, 42,392,808 bytes, SHA-256 `4a7bef4fdd9a21c295176eeed7bf304c61b77dd05fc4fd4e80455b51322c7ff0`, stored under AutoJs6 `build/p9-profiles-host5312-fixed/`. This is a workspace-built test artifact: its DEX also contains concurrent TerminalOutputPump and Inspector FilterProfileCatalog work. It is not claimed to be a clean artifact of the installer commit alone. Only task-owned emulator-5562 receives it; user-device host packages are not replaced.

On that API 31 AVD, the initial fixed host test APK (3,433,353 bytes, SHA-256 `f8c4b7fc83a42cae87f8e94ad44b3c880a2507f6bf1c7d55a2917ac695b29dfb`) passed both new session-handle methods:

- Immediate completion carries only the locally negotiated source-profile fact.
- Host UI honors negotiated source cleanup before releasing its owned descriptors.

The third initial method failed before reaching business assertions because Android SELinux denied creation of a hard link between app-private files. The denial was confirmed in device logs; changing directories would not address it. No permission or SELinux setting was relaxed and no assumption skip was substituted.

AutoJs6 test-only commit `f983f6f0a328e0dc99bb163f3dbfa3a71f17a344`, build 5313, replaces that fixture with a real private file and a test ContentProvider descriptor alias to the same inode. The provider accepts a one-use random ID and a duplicated descriptor, not an arbitrary host path; finally blocks remove the exact registration. The new method verifies differing ownership keys, identical real retention identity, and preservation of the owned file when the provider item says false. Its fixed test APK is 3,433,353 bytes, SHA-256 `59defd5b943ad8f7c512eb82654b28d5cb018fc24e1b50ded520ed4c21bf10d7`. Paired with the unchanged host5312 APK, that single method passed 1/1 without skips in 0.154 seconds. Hard-link decision cases remain JVM coverage; actual app-created hard links are not claimed as device coverage.

These are three passing Android regression methods across the two fixed test artifacts, with the original fixture failure retained. They use real Binder/PFD objects and local fake completion results; they do not install packages. Final real public-script installations against R8 are recorded separately in the release evidence. The host test rebuild encountered a missing unrelated incremental Kotlin class; its normal full fallback completed without cleaning shared outputs or editing the other task's sources.

## Documentation, declarations and plugins

| Repository | Version and local commit | Validation |
|---|---|---|
| AutoJs6-Documentation | 6.8.0 / content versionCode 88, `6bdbdad6aed8d89b854b3489a80ac0a6c7cdf481` | Full generation/freshness of 144 modules, search index and offline output checks. |
| AutoJs6-TypeScript-Declarations | 4.28.0, `18b3684033fd6f6c69c2e6c611f36c14e930e8a7` | Canonical declaration export and positive/negative TypeScript smoke checks. The existing Publish switch exports local files, not a registry release. |
| AutoJs6-Plugin-Ace-Editor | 1.20.0 / build 122, `9bf6851bc6652c92b2bb49e6ec7fe92d46a01a87` | Canonical declaration equality, LSP generation/runtime checks and 25 generated documents; build equals reachable commit count. |
| AutoJs6-Plugin-Offline-Docs | 6.8.6 / build 69, `9bcdfd1811ea04dc35744ac21fa7830d9da7c73b` | 2 JVM tests, Debug/Release payload gates and 25 generated documents; build equals reachable commit count. |

The offline payload is 200 files, 11,744,511 bytes, aggregate SHA-256 `65786402628484d27bb4b59f23646f90477f0306c171ff70b2b99ea3f8ac7ec9`. All three provenance records refer to the exact Documentation commit above, and its normalized generated files match the packaged payload.

The synchronized text describes first-match/non-stacking defaults, source plus actual package prefix, 12 per-package fields, three nullable resets, explicit value priority, processing-start snapshots, fresh retry checks and shared-source preservation. It retains the five advanced options and mandatory signature/blacklist behavior. No separate public script profile-management API was added. Main plugin ten-language README/changelog sources and 36 generated artifacts were included with feature commit `336478a` / build 61; old 1.1.0 and 1.0.0 entries were preserved.

Documentation, declarations and Offline Docs workspaces are clean. Ace retains its original untracked releases directory and its 21 files. The host's concurrent Terminal/Inspector/source/resource/changelog/sample changes remain outside these installer commits and its index was empty after the scoped commit. Unrelated generated declaration resources caused by those parallel sources were excluded from this task's downstream copies.

## Completion boundary

The original P9 four implementation/synchronization checkboxes are now complete within their recorded test scope. This synchronization evidence is a separate main-plugin build 62 commit. The final build-numbered, signed R8 artifact and device/script release checks are the next local delivery gate. No new Samsung or API 36/37 test coverage is asserted, and the P7 publication/index/host-push checkboxes stay open.
