# P9 build 63 final local delivery

Date: 2026-10-02. Source profiles and the final host/documentation synchronization complete the remaining two original P9 items. P7 remote publication remains delayed; no repository was pushed, tagged or published.

## Artifact and build gate

`releases/autojs6-plugin-three-setup-installer-v1.2.0-8b427100.apk`

- Version: 1.2.0 / build 63.
- Size: 2,140,911 bytes.
- SHA-256: `d8ae073dfaaa155ce31540663f55395639be13b8d5074c57f6057ef62b5b5fbe`.
- CRC32: `8b427100`, matching the filename.
- APK v2 signature verified; certificate SHA-256 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
- Signed R8, non-Debug, 730 ZIP entries and no native libraries. Independent runtime checks find no Debug-only entry points.

The prior build 60 APK was verified against SHA-256 `00adf451b1875b5b652975a740657e3079e0954e786056bec3a05ca705822b33` before moving that exact file into ignored `build/profiles-prior-release60/`. The releases directory contains only this final APK. A fixed copy, signing report and metadata are in `build/profiles-fixed-release63/`.

The prescribed Temurin gate used a single-use daemon, two workers and disabled build-number/time auto-updates. Exactly one platform decision reported Temurin 21.0.12.1+1. Debug, Debug androidTest, signed R8 Release, native-library validation, all 394 JVM tests and both lint variants passed in 4m 42s. JVM failures/errors/skips were all zero. Debug and Release lint each reported 0 errors / 51 warnings. The ten-language Markdown check verified 36 artifacts and the icon check verified 15 resources.

The separate platform-only release test APK built successfully in 14 seconds: 17,908 bytes, SHA-256 `0b687359d18974903c8461d1823f9f7425a283ecbab9a8e0553211fda50ba154`. It requires no production test keep rules or AndroidX dependencies. The frozen production APK remained unchanged during this test build.

Earlier integration errors and fixes are retained in [implementation evidence](p9-profiles-evidence.md). The final Temurin gate itself passed on its first run. Compiler/lint warnings are not described as zero and were not suppressed to pass this task.

## Public host script on the final R8 APK

Task-owned `Three_Setup_Dhizuku_P8_API31` / emulator-5562 ran the fixed official host 6.8.0/build 5312 from [integration evidence](p9-profiles-integration-evidence.md). Actual caller UID/PID were 10152/7638. The pre-existing Dhizuku owner and plugin authorization were retained. Neither Root nor Shizuku was started for this run.

`tools/p9-profiles-release-script.js` requires this dedicated API 31 emulator, user 0, official host identity, a fresh token and the fixed hashed APK fixtures. The desktop driver first journals the original profile file, which was absent. It seeds three ordered profiles scoped to the actual fixture package: a disabled Root rule, a script rule selecting Dhizuku/deleteSource=true/installReason=user, and a later any-origin Root rule that must not stack. The script never manages profiles through an invented public API.

Actual results:

1. A public session omitting authorizer and all per-package options negotiated V3, installed fixture v1 using Dhizuku and returned sourceDeleteRequested=true/sourceDeleted=true. The host-owned source file was actually absent afterward. Installation reported 12,228 ms, with preparing/writing/committing/completed stages and exactly one completion event.
2. A public update explicitly supplied Dhizuku, deleteSource=false, installer/installReason/packageSource=null, grantAllRequestedPermissions=false, requestUpdateOwnership=false and dexopt=none. Fixture v2 was actually installed in 1,447 ms; both deletion fields were false and the retained source hash was unchanged. This exercises the sparse request, nullable reset and effective cleanup result through the actual Rhino API.
3. Alternate-signer and unsigned sources both returned BLOCKED_BY_POLICY, remained byte-identical and did not replace v2. Profile defaults did not bypass mandatory signature policy.
4. Public Dhizuku uninstall removed the owned fixture. The report ended ok=true with installOutcomeNeedsInspection=false. Only the exact remaining owned sources, script and result were removed after hash checks; the plugin profile document was restored to its original absent state after verifying the exact seeded bytes. Host storage access returned to its original default mode, and preferred-handler XML was byte-identical.

The profile restoration used a temporary same-build Debug cover only on the task-owned AVD, then immediately restored the exact signed R8 APK. No user device received that Debug cover. Task-only terminal installation history remains in this dedicated image; this is not described as an unchanged history test. Device snapshots after cleanup found no Dhizuku session journal or owned package remaining. Original per-case runtime fixture audit journals remain as evidence, not active work.

The complete report, source identities and restored journal are in `build/profiles-release-script/5f90964b7b424e8bbffa3877b75ba1e2/`. Its recorded installed APK digest equals the final artifact above.

## Independent Release checks and scope

On the final restored API 31 R8 APK, the two platform-only Release checks passed without a skip. Plugin/probe UIDs were 10148/10149 and PIDs 8605/8623. INFO took 17 ms and the whole metadata round trip 67 ms. All six unauthorized business operations were refused, and source-profiles was present in the runtime capability set.

API 24/35 user-device results and restoration details are maintained in [device delivery evidence](p9-profiles-release-device-evidence.md). These checks verify the actual signed artifact and cross-UID/PID contract. The earlier six UI tests and four runtime/marker tests retain their fixed Debug artifact provenance, and the host's three PFD/session checks retain their own artifact identities. They are not relabeled as a full final-R8 functional matrix for all Android versions or ROMs. No new API 33/36/37 or Samsung run is claimed here.

Only the task-owned emulator-5562 is closed after final cleanup and identity checks. User-owned emulator-5554 and all other devices are left running. No device-owner, default-installer policy or user application is removed.

## Local commits and remaining roadmap

- `336478a` / build 61: source-profile implementation, settings, compatibility, source cleanup, tests and ten-language user documentation.
- `49b5e66` / build 62: final original P9 host/documentation synchronization and ecosystem evidence.
- Build 63: this final signed-artifact/device/script gate and its reproducible public script.

The final main-plugin VERSION_BUILD is checked against reachable commit count after committing, and its working tree is clean. All related repositories have their logical local commits, while the host's concurrent work and Ace's pre-existing releases directory remain intact. P7's publication, index and host-push items are the only unchecked original roadmap items. They remain delayed by the maintainer's instruction. No new device, product decision or manual action is required for this completed P9 scope.
