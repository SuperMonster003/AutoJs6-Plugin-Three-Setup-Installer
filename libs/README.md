# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants)
- `package-archive-parser.aar` (host module `plugin-api/package-archive-parser`: the shared package archive parser, roadmap P1.1)
- `installer-api.aar` (host module `plugin-api/installer-api`: the installer contract V2)

Current provenance: `common-plugin-api.aar` is the release AAR assembled from AutoJs6 6.8.0 / 5298
(host commit `86d9bfa26b`, 2026-09-27); the module itself last changed in host commit `9c3ba2e520`
(2026-09-15), so the artifact is byte-identical to the one staged by the other official plugins.
`package-archive-parser.aar` is the 272,518-byte release build from host commit `3876677baee5`,
AutoJs6 6.8.0 / 5309, assembled on 2026-10-02. It adds the optional manifest shared-user identity
and validates security-relevant root attributes, namespaces, compiled resource IDs and conflicts.
`installer-api.aar` is the 32,967-byte release AAR from AutoJs6 6.8.0 / build 5308, commit
`e173241b8874`, assembled on 2026-10-02. V3 retains all eleven V1/V2 AIDL transactions and negotiates
advanced installation options on the same live Binder. The base capability and unchanged result
envelope remain V1; minimum/maximum version and feature capabilities distinguish V2/V3 additions.
Absent V3 options are not injected into older requests. The `optimizing` stage and optional
update-owner/optimization observations do not change a confirmed installation's success.
`persistentConfigured` remains a historical receipt rather than a live-policy assertion.
`InstallerIds.REQUIRED_HOST_VERSION_CODE` stays 5299. The common plugin AAR is unchanged.
License and individual hashes are in `../THIRD_PARTY_NOTICES.md` and `../locks/host-api-aars.lock`.

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
