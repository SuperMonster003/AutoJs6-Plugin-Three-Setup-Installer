# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants)
- `package-archive-parser.aar` (host module `plugin-api/package-archive-parser`: the shared package archive parser, roadmap P1.1)
- `installer-api.aar` (host module `plugin-api/installer-api`: the installer contract V1, roadmap P1.2)

Current provenance: `common-plugin-api.aar` is the release AAR assembled from AutoJs6 6.8.0 / 5298
(host commit `86d9bfa26b`, 2026-09-27); the module itself last changed in host commit `9c3ba2e520`
(2026-09-15), so the artifact is byte-identical to the one staged by the other official plugins.
`package-archive-parser.aar` and `installer-api.aar` are the release builds of the host P1 line
(installer contract V1, `InstallerIds.REQUIRED_HOST_VERSION_CODE` 5299) assembled on 2026-09-30;
all three must come from the same host contract line and are re-staged together.
License and individual hashes are in `../THIRD_PARTY_NOTICES.md` and `../locks/host-api-aars.lock`.

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
