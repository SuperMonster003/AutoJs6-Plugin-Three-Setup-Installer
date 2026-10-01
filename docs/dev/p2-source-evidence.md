# P2.1 source and format evidence

Date: 2026-10-01. This record covers the source and format audit, JVM checks, and actual read-only
device executions. The final section distinguishes executed checks from the test definitions.

## Source audit and implementation

- `PackageSource` describes borrowed host descriptors, `content://` sources, and local file
  sources. `fromUri` accepts content, file, and legacy path inputs and rejects other schemes
  before contacting a provider.
- `PackageStaging.open` returns a closeable `SeekableSource`. It duplicates the borrowed
  descriptor and probes its `FileChannel` without changing the shared offset. A regular
  descriptor is exposed to the existing File-based shared parser through a private symbolic
  link to `/proc/self/fd/<owned fd>`. The link preserves the display-name extension.
- A pipe is copied once into the session's staging directory. A seekable descriptor is also
  copied when Android denies reopening it through procfs, for example after its inode's read
  permission has been revoked. This is a necessary extension of the P2.1 copying exception:
  the current shared parser requires `ZipFile(File)`, and an existing read grant on a descriptor
  does not grant permission to reopen the same inode. The view records
  `DESCRIPTOR_REOPEN_UNAVAILABLE` separately from `NOT_SEEKABLE`. This fallback uses positional
  channel reads from byte zero and preserves the host's offset.
- Copies enforce the declared length, a 4 GiB source limit, and available staging storage;
  stalled pipes are polled every 100 ms so cancellation and deadlines can be checked. Failed
  copies remove their partial file. Closing a view unlinks only the symbolic link and closes
  owned descriptors. Source deletion remains the owner's responsibility under D25.
- `DescriptorInstallEnvironment` retains source views through each package installation and
  closes them before removing the item's staging directory. `PackageInspector` uses the same
  path for descriptors, content URIs, and local files and closes views before returning.
- Direct views pin an inode but cannot make its bytes immutable. `ArchiveOpener` compares source
  digests before and after installation preparation; plain APKs carry the parser's SHA-256
  into `PlannedApk` for a final comparison during installation streaming. Containers retain
  the shared parser's verification of each extracted APK. These checks detect ordinary
  concurrent modification; they do not claim an atomic snapshot of an adversarially mutable
  file. Files must remain stable while installation is being prepared and written.

## Format and split audit

- The shared parser recognized known container extensions and metadata but rejected an ordinary
  ZIP containing APKs. `AndroidPackageArchiveInspector.detectContainerFormat` now recognizes
  APK entries as a generic APK set after the more specific checks. The plugin preserves
  `format: "zip"` for a generic set whose display name ends in `.zip`.
- APK, bundletool APKS, generic APKS, XAPK, APKM, APKZ, and ZIP use the shared parser. XAPK /
  APKM metadata are hints; package identity and version still come from APK manifests.
  Device ABI, density, and language selection remain in the shared module.
- `ArchiveOpener` rejects a selected container set over the public 64-component limit before
  extracting it. Explicit split sets enforce the same limit, one base, package and version
  consistency, and unique split names.
- The earlier explicit-set combiner discarded missing-dependency problems without checking
  whether the supplied set resolved them. It now rechecks `uses-split` and `configForSplit`
  references against the complete set and removes only the problems actually resolved by it.
  Container selection receives the same parent-feature check.
- AAB inspection retains package, version, and module names. Installation reports
  `UNSUPPORTED_FORMAT` with the requirement to generate APKS using bundletool.

## Verification definitions

`ArchiveOpenerTest` contains 18 JVM cases covering content-based APK recognition and digests,
the five generic container extensions, ABI / density / language selection, bundletool TOC,
untrusted metadata, inspect-only extraction, AAB refusal and modules, corrupt ZIP / APK data,
unknown archives, unsafe entry paths, the selected and scanned APK count limits, explicit split
sets and their dependencies, missing configuration parents, and source modification.
Fixtures are generated in temporary directories from minimal text APK manifests and minimal
AAPT2 / bundletool protobuf messages. No fixture contains third-party application content.

The shared module adds two `AndroidPackageArchiveInspectorTest` cases: ordinary ZIP and renamed
containers with APK entries are accepted, while a ZIP containing only documents stays
unsupported. The release AAR from host `0767971bc9` is pinned in `locks/host-api-aars.lock`;
the host module's 54 JVM tests, release assembly and lint passed on 2026-10-01.

`SourceFormatsDeviceTest` contains 8 read-only instrumentation cases:

| Case | Checks |
| --- | --- |
| `seekableDescriptorDoesNotCopyOrMoveTheHostOffset` | Direct view, borrowed descriptor ownership, stable offset, source digest and cleanup |
| `contentResolverInspectsMetadataAndCleansItsDescriptorView` | Real ContentResolver query/open, package metadata, size, cleanup |
| `nonSeekableContentIsCopiedOnceAndRemainsInspectable` | Pipe provider, unknown size, one staging copy, inspect and cleanup |
| `legacyFileUrisAndPathsKeepTheirOriginalFile` | File URI and raw path forms; deleting staging only unlinks the view |
| `inaccessibleProcReopenUsesPositionalStagingWithoutChangingTheHostOffset` | Permission-denied procfs reopen, positional fallback, borrowed descriptor ownership |
| `invalidSchemesAndSizeBoundsFailWithoutConsumingTheDescriptor` | Unsupported schemes, declared size mismatch, empty and over-limit inputs |
| `stalledPipeCancellationClosesOnlyOwnedDescriptorsAndDeletesPartialCopy` | Deadline enforcement for a stalled pipe and partial-copy cleanup |
| `genericContainerFormatsSelectAndExtractRealBinaryManifestSplits` | APKS / XAPK / APKM / APKZ / ZIP with the generated, signed base and feature APKs |

The debug-only, non-exported `SourceFixtureProvider` reads only the current test's private fixture
directory and supports read mode only. The device tests neither install nor uninstall packages.
The actual installation of the generated split fixtures belongs to the core installation matrix.

## Audit-time checks

- `git diff --check` passed in the plugin and host repositories. Git only reported the existing
  line-ending conversion notices.
- The refreshed integration report
  `app/build/test-results/testDebugUnitTest/TEST-io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpenerTest.xml`
  contains all 18 cases with zero failures, errors, or skips (2026-10-01). It includes the final
  configuration-parent and source-modification checks.
- `SourceFormatsDeviceTest`: all 8 cases passed without skips on AVD API 24 / x86,
  Xiaomi 23046RP50C / API 35 / arm64-v8a, and Sony G8441 / API 28 / arm64-v8a.
  SDK and ABI were read again in this session. Each device exercised both direct views and the
  forced permission-denied procfs fallback, plus actual content-provider and pipe sources.
- Integrated JVM: 126 tests with zero failures. Debug, androidTest, minified release and both lint
  variants passed. The aggregate and installation matrix are recorded in `p2-core-evidence.md`.
