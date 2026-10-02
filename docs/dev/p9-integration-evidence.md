# P9 V3 host and documentation integration

Date: 2026-10-02. All changes are local commits. P7's remote push, tag, publication and index work remains delayed. The original source-profile item is still pending; this document records synchronization of the implemented advanced options and local installation policy, not completion of all P9 work.

## Host contract

| Repository | Commit | Scope |
|---|---|---|
| AutoJs6 | `e173241b887456f38d034245b3793fd739b9cc66` / build 5308 | V3 option vocabulary, unchanged public AIDL transaction order, live version/capability checks, script normalization and compatibility tests. |
| AutoJs6 | `3876677baee5311fcee9003ab9c45a2d7515a43d` / build 5309 | Shared manifest parser verifies the actual root and sharedUserId identity, rejecting ambiguous input. |
| AutoJs6 | `e86186920d` | Caller-visible ownership semantics, fixed host artifact provenance and synchronized implementation guidance. |
| AutoJs6 | `0d21303f6759fd4d83b595a8dbc7eed241c67731` | Permission preview reads actual Android manifest elements and rejects conflicting compiled attributes, without treating comments or unrelated namespaces as declarations. |

The V3 installer API AAR has 32,967 bytes and SHA-256 `39971b5d9f29901075259e50da73367b1122c5f9453b4a9528d664c8cbfe575c`. The main plugin consumes the checked-in release AAR and its hash lock, without a sibling-repository build dependency. Final parser provenance is maintained in `libs/README.md`, `locks/host-api-aars.lock` and `THIRD_PARTY_NOTICES.md`.

The initial host checks passed 9 installer-api JVM tests, 62 shared-parser tests and 96 installer-runtime tests. App Debug and all unit-test Kotlin sources compiled. The fixed host build also contains concurrent inspector source work, so its binary is not attributed to a clean build of only commit 3876677bae. No unrelated inspector/terminal edits or staged files were adopted into installer commits.

| Fixed test artifact | Bytes | SHA-256 |
|---|---:|---|
| Host build 5309 universal Debug | 42,236,039 | `a2737d3ead27e64b2629336c71dab346085650ec2ed683137b1fb0819588f6ff` |
| Host build 5309 androidTest | 3,433,353 | `0275ee5a927ad2c9db2a5187f1603a69d9a388f9ebe8aff3b915ba96f155cff0` |

Only the task-owned API 31 AVD received that host binary. Existing user-device host apps were not replaced for these tests. A later shared-parser correctness fix does not change the frozen host binary's provenance.

## Documentation and editing tools

The final shared-parser release AAR has 275,454 bytes, SHA-256 `41f9348190895b772b1b8af9aa2c82c5543f62646000598ca08e9b68360259a3`. Its complete module suite passed 69 tests without failure or skip, covering all three actual permission element names including the two SDK 23 aliases. The host workspace had a concurrent uncommitted version-file change to 5310; that file was preserved and excluded from the module commit. This library has no independent host versionCode and does not change the previously frozen host APK or minimum V3 host requirement. An early in-progress test count of 54 was corrected to the completed initial parser run's 62; the final refresh adds seven cases for 69.

| Repository | Version and local commit | Validation |
|---|---|---|
| `D:/webstorm-projects/AutoJs6-Documentation` | 6.8.0 / content versionCode 87, `695f21a0b9cec6c60ce94c504cb5b8f4a517bb40` | 144-module normalization/full generation, freshness checks, search-index syntax, official build-driver dry run and offline verification. |
| `D:/webstorm-projects/AutoJs6-TypeScript-Declarations` | 4.27.0, `396a9706eff8565421b0f82b6e514df2689c151c` | Existing declaration-export workflow with `-Publish -SkipBuild`; TypeScript positive and negative smoke cases. This publish option exports local declarations, not a remote release. |
| `D:/idea-projects/AutoJs6-Plugin-Ace-Editor` | 1.19.0 / build 121, `10e6dd31de05cc05cf588c738fb21836c6e73a07` | Declaration mirror equality, LSP generation/runtime checks and 25 generated documents. Build number equals reachable commit count. |
| `D:/idea-projects/AutoJs6-Plugin-Offline-Docs` | 6.8.5 / build 68, `7e11fb394acb1394f11b393435b070ca6924ca43` | 2 JVM tests without failure/skip, Debug/Release payload gates and 25 generated documents. Build number equals reachable commit count. |

Offline documentation contains 200 files, byte-equal to the LF-normalized documentation output, 11,737,025 bytes in total, aggregate SHA-256 `5007ae8d62dfb04a242af4ac6be646b42b3c1a49c12c335c6c492cfe8bb22811`. The original untracked Ace `releases/` directory remains untouched. Other three documentation workspaces were clean after their task commits.

The main plugin's ten language JSON sources and 36 generated README, plugin-instruction and changelog artifacts describe the five options, SDK/authorizer limits, optional results, signature checks, blacklists and selected-split permission preview. The previous 1.1.0 and 1.0.0 changelog objects were compared against the initial HEAD and preserved. The new normal permission ENFORCE_UPDATE_OWNERSHIP is explained in the security section.

## Real host script

`tools/p9-release-script.js` only accepts the driver's task-owned API 31 environment and fixed, hashed advanced fixtures. Before invocation the driver verifies the AVD identity, sole user, actual Dhizuku owner and absence of fixture packages, including retained records. It temporarily grants the task-installed host storage access and restores its prior default mode afterward; it does not change the device owner or default-installer policy.

The initial real-host Debug run used plugin SHA-256 `4fb2149a6d81feddad0e048f49cf39901412429e1e6afb901a12cac4a82a3a26`. Official host UID 10152/PID 6024 negotiated V3. Four unsupported option combinations returned the expected AUTHORIZER_REQUIRED/INVALID_ARGUMENT codes without installing the fixture. A V3 session with explicit false/none values and install reason `user` installed v1, emitted preparing/writing/committing/completed and exactly one completion, and omitted unrequested optional results. A same-signer update installed v2. An alternate-signer update and an unsigned APK both returned BLOCKED_BY_POLICY, leaving v2 unchanged. Privileged Dhizuku uninstall then removed the task fixture.

All four source APK hashes remained unchanged. The driver verified and removed its exact private script/source/result files, restored the storage mode, and compared complete ordinary preferred-handler XML before and after. Its task-only terminal installation history remains on the dedicated AVD as an observation, separate from user-device cleanup. This run does not stand in for the later real signature-review dialog test or the final signed R8 run.

No source-profile API is advertised. Original P9 item four remains open until the remaining P9 behavior and its documentation are synchronized; the completed V3 portion is recorded here without splitting the roadmap item.

The final signed R8 runs also completed through both Dhizuku and Shizuku/shell, including the live optimizing event and structured compilation result reaching the public script API. Exact final-artifact checks and device restoration are in [the build 60 release evidence](p9-release-evidence.md).
