# P7 remote publication

Date: 2026-10-02, Asia/Shanghai. The maintainer explicitly authorized P7 publication after P8/P9 implementation and then separately confirmed that the AutoJs6 host must remain local-only. The permitted remote changes are this plugin's source/tag/Release and the official plugin index. No host branch or other related repository is pushed by this task.

## Published source and release

The first public release uses the current verified **1.2.0 / build 63**, rather than issuing an older 1.0.0 artifact from the original P7 scheduling. The roadmap item is retained and its release version is updated to reflect the deferred ordering; no new roadmap item is added or split.

- Repository: [AutoJs6-Plugin-Three-Setup-Installer](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer), public, default branch master.
- Release: [v1.2.0 - 3-Setup Installer](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/tag/v1.2.0), release ID 401825139.
- Published at 2026-10-02 21:05:08 Asia/Shanghai (13:05:08 UTC), stable and not a draft.
- Source commit: `1e63028234e9542f13a1fec8aa75eaa09cf25114`.
- Annotated tag object: `b2f857118fb92d3debbd8638538e24b70ac034b9`, peeling to that exact source commit.

The remote branch initially pointed to a0f4df9 and was 53 commits behind the tested source. The source branch and new tag were pushed atomically after a dry run, without force or rewriting an existing tag. Repository metadata now describes system confirmation, Shizuku, Root and Dhizuku installation. Readme and publication-record changes after the tag are separate source commits and do not replace the tagged APK.

## Immutable assets and public retrieval

| Asset | Bytes | SHA-256 |
|---|---:|---|
| autojs6-plugin-three-setup-installer-v1.2.0-8b427100.apk | 2140911 | `d8ae073dfaaa155ce31540663f55395639be13b8d5074c57f6057ef62b5b5fbe` |
| SHA256SUMS | 123 | `0d814a063a707c4e511c7e1f7b667ff1ee85b1ef2ff151c93be33e1e15b50119` |

The APK is the unchanged signed R8 artifact from [the build 63 gate](p9-profiles-release-evidence.md). Its v2 signer certificate SHA-256 is `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`, its CRC32 is 8b427100, and it contains no native libraries. Final asset IDs are 605631267 and 605671001 respectively.

The original APK and checksum receipt were uploaded to a draft. GitHub's reported asset digests, names, lengths and uploaded state were checked before publishing. After publication their public download URLs were fetched without authentication, and their bytes were hashed again. The APK also remains identical to the local fixed build 63 copy. The initial verifier used a draft-only untagged URL and received 404; reading the published release's final browser_download_url corrected the verifier. Nothing was republished or replaced to address that local URL mistake.

A separate consumer check found that Python's Windows text output had given the original SHA256SUMS.txt receipt CRLF endings. GNU sha256sum treated the CR as part of the APK filename. A new LF-only SHA256SUMS was checked locally, uploaded, publicly downloaded and checked again with the actual GNU utility before removing only the identified unusable 124-byte receipt (asset 605631342). The APK, its tag and its digest never changed. The final download set is the APK and the 123-byte LF checksum receipt shown above.

Release notes explain the feature set, Android API 24 minimum, host build boundaries 5299/5300/5307/5308/5312, authorizer/platform limits and the actual validation scope. Those host numbers describe required integration, not a claim that a publicly released host APK already contains it.

## Host publication decision

The host checkout initially had a3b5741691 at master. Its tracked private/master was 948 commits behind, including unrelated feature work, while the public repository had incompatible history. This was presented as a concrete separate scope decision. The maintainer explicitly retained local-only host work. Only read-only fetch/status/history inspection was performed there; neither private nor public host branches were pushed. Concurrent host workspace files remain outside the plugin publication commit.

## Verification records

Local ignored `build/p7-publication/` contains the release plan, draft/uploaded/published API responses, final public download receipts, release notes, checksum file and GitHub Actions snapshots. Credentials are obtained through the existing Git credential helper only for GitHub API access; they are not printed, saved in these records, embedded in URLs or sent with public CDN downloads.

## Official index

Index commit [229b1afa10287ae1e53de2792415c13af23a8029](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/commit/229b1afa10287ae1e53de2792415c13af23a8029) adds this repository in alphabetical order, updates the project count from 45 to 46, adds the exact build-63 release admission manifest and regenerates the published index. All 46 generator/native-alignment unit tests passed. Full generation covers 46 required and featured repositories and 62 distribution entries, with no removed plugin. The only unrelated generated normalization changes two existing audio-player alignment provenance values from declared to measured; their versions, assets and alignment values do not change.

The installer entry has engine installer, variant default, ID three-setup-installer, requiresHostVersion 5299, version 1.2.0/code 63 and measured native alignment 0. Its asset includes the exact APK SHA-256, signer, package name and tag source commit. It adds no invented runtime component or ABI restriction. The admission manifest is [release-manifests/io.github.supermonster003.autojs6.plugin.three.setup.installer/63.json](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/blob/main/release-manifests/io.github.supermonster003.autojs6.plugin.three.setup.installer/63.json).

After the normal fast-forward push, [Plugin Index run 37011765516](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/actions/runs/37011765516) completed successfully. Its push-event update job ran the unit tests and full generator and found the committed output current; the PR-only job was correctly not selected. The remote main branch remains at 229b1afa.

The ordinary public main-branch raw URL was fetched without authentication. Its bytes exactly match the committed Git blob, SHA-256 `856cfa5c3e81df9293e741a18a5a793b2586ae4719b66b4549c1346139d3cdbd`. The earlier Windows working-file hash `258810914d2f6f1a1da22edd9103b59cb463c580654cc317b418c909157bdeca` differs only because of CRLF checkout endings; the published JSON and local parsed document are identical.

## Cloud test correction

The original tag and source pushes passed Markdown integrity and the JVM/debug/release/lint build. The full API 24 and API 35 Android suites each reported one failure in cancelledStalledSourceDoesNotCloseHostDescriptor at the old parent-directory assertion. Production StagingDirectories.create canonicalizes its root, while that assertion compared it to Context.cacheDir's uncanonicalized spelling. Android's /data/data and /data/user/0 aliases therefore produced a false mismatch after the timeout and host-descriptor checks had already passed.

Commit `f8b746b55df7d2dc9776ce5d803c0d8e0d1f48bb` / build 64 changes only that test assertion/comment and the required source build counter. Both sides now compare canonical directory identity, still rejecting a directory outside the staging root. No timeout, descriptor ownership or production containment check is relaxed. Debug androidTest Kotlin compiled locally in 38 seconds. The [complete cloud rerun](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/actions/runs/37012451323) finished successfully, including JVM/APK/lint and both emulator jobs. API 24 reports 266 completed cases, 58 skips and zero failures; API 35 reports 276 completed cases, 64 skips and zero failures. The skipped authorizer/device/explicit-fixture cases are not counted as successful installations. The separate [Markdown run](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/actions/runs/37012451064) also succeeded. The failed tag-run evidence is retained and the immutable tag is not moved merely to change its historical CI status.

## Actual host discovery and installation

A new, clean, task-owned API 31 AVD used the fixed local host5312 APK. Its actual first-launch wizard fetched the official online index, displayed this plugin, downloaded the public APK and performed a normal Android installation. Both downloaded copies and the installed base.apk match the published SHA-256 and size. The first confirmation was explicitly cancelled by the test driver after the permission page returned; the second run finished normally and the host reported Installed: 1 - 3-Setup Installer, then displayed version 1.2.0 (63) as enabled.

Play Protect's normal Scan app flow displayed This app looks safe before installation; it was not bypassed. That observed result is not a general safety guarantee. The temporary host unknown-source permission was restored to off, the production wizard cleared its download cache and preferred-handler XML stayed unchanged. Only the new task AVD was closed after exact ownership/identity checks; the user's API 24 AVD stayed running. Full fixed-host provenance, original cancellation, screenshots and final audit are in [live wizard evidence](p7-live-wizard-release-evidence.md).

## Source follow-up

The ten-language README and plugin-instruction sources now describe the published release and plugin-center availability. Their 36 generated artifacts are checked, while historical 1.0.0/1.1.0 changelog entries remain untouched. The roadmap retains its original items, records the first release as 1.2.0 because publication was deferred, and records the host no-push decision explicitly. AGENTS.md reflects this current scope.

The test correction is build 64; the publication record and documentation form the next source commit, build 65. The final source build counter equals its reachable commit count and the plugin/index workspaces are clean after their scoped commits and pushes. These follow-up source commits do not rebuild or replace the published build 63 APK, move v1.2.0 or alter its index admission. Both host remote master refs were re-read and remain at their original values. No further device, decision or manual action is required for this P7 scope.
