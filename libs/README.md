# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants)
- `package-archive-parser.aar` (host module `plugin-api/package-archive-parser`: the shared package archive parser, roadmap P1.1)
- `installer-api.aar` (host module `plugin-api/installer-api`: the installer contract V1)

Current provenance: `common-plugin-api.aar` is the release AAR assembled from AutoJs6 6.8.0 / 5298
(host commit `86d9bfa26b`, 2026-09-27); the module itself last changed in host commit `9c3ba2e520`
(2026-09-15), so the artifact is byte-identical to the one staged by the other official plugins.
`package-archive-parser.aar` is the release build from host commit `0767971bc9`, AutoJs6 6.8.0 /
5299, assembled on 2026-10-01 to recognize ordinary ZIP split containers.
`installer-api.aar` is the 30,940-byte release AAR from the AutoJs6 6.8.0 / build 5300 P4 working
tree, assembled on 2026-10-01. Its optional JSON batch marker preserves the distinction between
a single application and a one-item batch. The AIDL surface and installer contract version remain
V1, and `InstallerIds.REQUIRED_HOST_VERSION_CODE` remains 5299. The shared plugin contract and
package parser artifacts are unchanged by this refresh.
License and individual hashes are in `../THIRD_PARTY_NOTICES.md` and `../locks/host-api-aars.lock`.

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
