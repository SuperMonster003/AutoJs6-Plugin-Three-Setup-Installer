# P9 build 60 local release gate

Date: 2026-10-02. This completes local delivery of the first two original P9 items. Source profiles and the original final synchronization checkbox remain open. P7 remote publication remains delayed; no repository was pushed, tagged or published.

## Final artifact and build

`releases/autojs6-plugin-three-setup-installer-v1.2.0-ce29bb60.apk`

- Version: 1.2.0 / build 60.
- Size: 2,074,763 bytes.
- SHA-256: `00adf451b1875b5b652975a740657e3079e0954e786056bec3a05ca705822b33`.
- CRC32: `ce29bb60`, matching the filename.
- APK v2 signature verified. Certificate SHA-256: `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
- R8 enabled, non-Debug, no debug entry points or native libraries. The archive has 730 entries; the declared native-page-alignment value remains zero because no native code is shipped.

The prior 1.1.0/build 57 APK was checked against its known digest and moved into the ignored `build/p9-pre-release/` directory. The release directory contains only the final 1.2.0 APK. A second immutable copy and its signature report are in `build/p9-fixed-release60/`.

The final Temurin parameter gate used a separate daemon, two workers, disabled build-number/time auto-updates and the prescribed vendor parameters. Its single IDE/platform version decision selected Temurin 21.0.12.1+1. Debug, androidTest, signed R8 Release, native-library checks, all 347 JVM tests and Debug/Release lint completed successfully in 4m 2s. JVM failures/errors/skips were all zero; both lint reports had 0 errors and 43 warnings. Ten-language Markdown generation checked 36 artifacts and the icon generator checked 15 resources.

The advanced-only commit tree was independently compiled and passed 333 JVM tests before commit 321e4ac. The final policy tree adds the remaining checks for 347. The final shared parser passed its separate 69-test module suite. Initial signature-array Kotlin typing errors, an ownership assertion that compared different caller identities, and API/test-annotation lint errors were retained and corrected. Lint was not suppressed and minSdk stayed 24. The test annotation was changed to `SdkSuppress(minSdkVersion = 28)` for its actual dedicated-device requirement.

The separate platform-only release test APK has 17,892 bytes, SHA-256 `dbffb97dbfaa08fa73542b0de18b86e664ed5019cfca0ab4dca666274be65fce`. Its build completed successfully without changing the frozen production APK. It does not add test-only keep rules or AndroidX dependencies to production.

## Same final APK on four Android versions

Each row ran the two independent release checks on the actual installed R8 artifact, with no skip. INFO/INSTALLER round trips used a separate probe UID and process, and all six unauthorized business-operation checks were rejected.

| Device | API / ABI | Plugin / probe UID | Plugin / probe PID | INFO / total | Result |
|---|---|---|---|---|---|
| User emulator-5554 | 24 / x86 | 10293 / 10294 | 28238 / 28256 | 15 / 70 ms | 2/2 |
| Task-owned emulator-5562 | 31 / x86_64 | 10148 / 10149 | 8668 / 8707 | 16 / 66 ms | 2/2 |
| Sony XQ-DQ72, QV770340J7 | 33 / arm64-v8a | 10623 / 10659 | 10006 / 10025 | 13 / 51 ms | 2/2 |
| Xiaomi 23046RP50C, 968e9f18 | 35 / arm64-v8a | 10290 / 10292 | 17933 / 16971 | 13 / 29 ms | 2/2 |

These checks do not turn the earlier Debug device cases into a final-R8 full feature matrix. Actual advanced-option and policy scenarios retain their own fixed APK provenance in [advanced options](p9-advanced-options-evidence.md), [API 24/33 device evidence](p9-advanced-device-evidence.md) and [policy evidence](p9-policy-evidence.md). Samsung had been reclaimed and was not required for this round; no new Samsung or API 36/37 verification is claimed.

## Actual host V3 operations on R8

The task-owned API 31 AVD kept its original Dhizuku owner. The official host's fixed build 5309 APK then invoked the final Release through the public script API, once per authorizer:

- Dhizuku: actual v1 installation, same-signer v2 update and uninstallation; four unsupported options rejected; alternate-signer and unsigned inputs rejected by BLOCKED_BY_POLICY. Host UID 10152/PID 6651 negotiated V3, observed preparing/writing/committing/completed, and received exactly one completion. Unrequested optional result fields stayed absent.
- Shizuku/shell: actual v1 installation with permission grant and manual speed compilation, v2 update and uninstallation. READ_CALENDAR was actually granted. The host received the nonterminal optimizing stage followed by one completion, and the structured `dexopt` object with filter speed and status accepted. Its old-platform Success message explicitly allows a skipped compilation; this case does not claim ART PERFORMED. Two unsupported metadata options and the two signature-risk inputs were rejected. Host UID 10152/PID 9286 negotiated V3.

The reports are in `build/p9-host-script/release-dhizuku-b2c47a7f453645c4871c278e6072d3ad/` and `release-shizuku-be618f3d3f6b4627ac4cfa167e953698/`, both `ok=true`, one terminal callback and no uncertain installation outcome. All fixed source hashes were verified before and after, the exact owned host files were removed, temporary host storage access returned to default, and preferred-handler XML was unchanged.

For the second case, the previously absent Shizuku manager was installed only on the task-owned AVD and the plugin was authorized through its real dialog. The manager APK was read from the existing Xiaomi installation without changing that source device: 3,442,426 bytes, SHA-256 `a05832ce3716afb1fcccf46f348006d2a296ca777e1ff3d223797dc74d06b31f`, with x86_64 support. Its new AVD server was shell UID 2000/PID 8214. The manager startup script's broad cleanup fallback was not executed; its verified starter used a dedicated task directory. The manager/device-owner distinction and original Dhizuku authorization were preserved.

## User-device state and test bookkeeping

The three user devices used in this round received the same final Release while retaining their main-app UIDs and data. Original test APKs were restored from each device's own backup. API 24/33 retained their original preferred/persistent rules, permissions/app-ops, history/preferences, 0/20 active system sessions and original privileged servers. Framework profile bookkeeping changed naturally on update and was recorded rather than rolled back. Detailed comparisons are in the API 24/33 evidence and `build/p9-backend-devices/final-delivery.json`.

On Xiaomi, the final original test APK is 17,708 bytes with SHA-256 `ea6edc22684f016f5b52d9f0a9e6cabd0e4c96097438b017250af42ac42bc5e0`. Main/test UIDs remain 10290/10292, 455 package/UID records including retained data match the original baseline, preferred XML is unchanged, and the original shell Shizuku PID 20102 remains. Seven active system sessions were compared immediately before and after final delivery; this is a delivery baseline, not a fabricated pre-test global session snapshot.

The earlier broad Binder/contract runner printed `OK (14 tests)`, but its raw result contains one assumption skip for the existing real-install test without `engineAuthorizer`. The corrected count is 13 passes and 1 skip. The skipped case is not used as installation evidence. Its two lifecycle tests also created six cancelled `waiting.apk` history rows. Final cleanup checked the exact six tokens, source/origin/authorizer, cancellation state, bounded test timestamps, and unchanged original rows before removing only those task rows. A temporary same-version Debug cover allowed the private-data audit; the final Release was immediately restored and its two release checks passed again. Original two history rows and both preference files are byte-identical to baseline, and no extra recovery files remain. The mutation journal and before/after hashes are in `build/p9-api35-b327c56eb8/binder-history-cleanup.json`. Future audits must inspect assumption status and journal history before running whole lifecycle suites on persistent devices.

The task-owned AVD ended with the same verified Release, no fixture packages, active install sessions or install foreground service, and the unchanged Dhizuku owner. Its script-only terminal history remains in that dedicated test image as evidence. After these checks, only emulator-5562 was shut down and its recorded process ended. The user's emulator-5554 and the separately appeared emulator-5574 were left running. Other user devices were not upgraded in this round.

## Local commits and continuation

Main logical commits are 321e4ac/build 58 for advanced options and b07b018/build 59 for installation policy. The final evidence/test-driver commit uses build 60, with the build number checked against reachable commits after committing. Cross-repository commit IDs, source-module provenance, preserved concurrent host work and the original Ace release directory are recorded in [integration evidence](p9-integration-evidence.md).

The next implementation target remains the original P9 source-profile item: host/script/external/package-prefix defaults and settings management. The original final synchronization item remains open for that behavior's eventual documentation, while the implemented V3 option synchronization is complete. No additional equipment, product decision or manual operation is required for the completed scope.
