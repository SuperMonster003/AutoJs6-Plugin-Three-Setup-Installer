# P3 appearance evidence

Date: 2026-10-01.

The appearance support layer has passed the current JVM build and the API 24 contract/view tests described below. The complete visual and host-integration matrix remains open; injected settings and measured detached views are not evidence of a successful live host Provider read or complete system-window acceptance.

## Implementation

- `HostAppearanceReader` consumes the versioned `AutoJs6HostSettingsContract` from the locked `common-plugin-api` AAR. It validates protocol version, host package, required types, descriptor absence and the full language tag. Provider access stays on a single worker using the application context. Paused or destroyed activities discard their pending result. Failure clears the accepted snapshot, and the startup cache expires after 30 seconds.
- `AppearancePreferences` keeps independent plugin language, night and seed choices. Defaults follow AutoJs6; unavailable host values fall back to unmodified system resources and the original opaque `#FFDEAD` seed. P3 does not add the P5 settings screen.
- `HostAppearanceActivity` refreshes only the existing screen. It never starts an installer task. Activities consume `uiMode|locale|layoutDirection` changes in place, preserve their current session/draft, and rebuild widgets through `onAppearanceChanged`. Ordinary rotation remains under the screen's saved-state handling.
- `InstallerPalette` uses the Material 1.13.0 embedded HCT implementation and the agreed chroma/tone rule. Primary and accent use their respective host seeds. Neutral surfaces, dividers and error colors stay independent of the seed. The readable-accent fallback matches the other standalone Three plugins.
- `InstallerUiKit` creates new controls with the current palette, separate disabled roles, wrapping sp text and 48 dp touch targets. The source defines 24 dp dialog corners, at least 24 dp horizontal margins, a maximum width of 560 dp and an 85% height limit. The body scrolls while the title/actions stay fixed; long action labels can stack. Insets handling covers system bars, cutouts and the keyboard.

## Build and JVM results

`build/p3-complete-build.log` ends with `BUILD SUCCESSFUL in 1m 10s`. It includes `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:assembleRelease`, `:app:lintDebug` and `:app:lintRelease`. Successful lint tasks do not imply that every lint warning was removed.

The matching XML suites in `app/build/test-results/testDebugUnitTest/` report:

| Suite | Tests | Failures / errors / skips | Coverage |
|---|---:|---|---|
| `AppearancePreferencesTest` | 4 | 0 / 0 / 0 | Host following, unavailable-host fallback, independent local choices and invalid preference values |
| `InstallerPaletteTest` | 3 | 0 / 0 / 0 | Shared HCT tones, achromatic/extreme seeds, contrast, neutral surfaces and independent host primary/accent |

The final notification publication change was subsequently revalidated in `build/p3-api24-publications-final.log` (21 tests passed, no skips) and `build/p3-api35-publications-final.log` (14 tests passed, no skips). These runs include real background installation and notification-bridge checks; their measurements are recorded in `docs/dev/p3-notification-evidence.md`. They close the notification replay gap and do not expand the appearance/IME/edge-to-edge coverage below.

## API 24 device contract and geometry

`build/p3-api24-ui-final.log` records `OK (47 tests)` in 18.856 seconds. The relevant subset is four `HostAppearanceContractDeviceTest` cases and five `InstallerDialogGeometryDeviceTest` cases, all passed. The contract cases reject file descriptors, unknown protocols/hosts, missing or mistyped fields and malformed language tags; a valid injected snapshot applies locale, RTL and night together without changing the original context.

The geometry run used density 3.5. These are measured pixels from the log, with the requested viewport in dp:

| Requested viewport | Surface size | Body / scroll height | Result |
|---|---|---|---|
| 320 x 640 dp, long body | 952 x 1904 px = 272 x 544 dp | 18056 / 1433 px | 24 dp horizontal margins, 85% height, fixed title/actions |
| 1000 x 800 dp, long body | 1960 x 2380 px = 560 x 680 dp | 6136 / 1909 px | 560 dp width cap, 85% height, body scrolls |
| 480 x 320 dp, long body | 1512 x 952 px = 432 x 272 dp | 12136 / 481 px | Narrow landscape retains a positive scroll area and visible actions |
| 1000 x 800 dp, short body | 1960 x 603 px | 132 / 132 px | Natural content height; no artificial overflow or empty expansion |
| 320 x 640 dp, Arabic RTL, fontScale 2 | 952 x 1904 px | 71016 / 1134 px | Correct direction, scalable text, wrapping actions and at least 48 dp button targets |

The injected-insets case measured a 1400 x 2800 px root with padding 126/105/112/84 px (left/top/right/bottom); its surface remained inside the available region. This API 24 detached-view result demonstrates the system-bar padding path. Its bottom inset was 24 dp, not the requested synthetic 180 dp IME value, so it is not recorded as an actual keyboard-insets acceptance test. The control-tint case also passed the current tint and distinct disabled-state checks.

An earlier assertion assumed that 24 English rows must overflow every viewport. They can fit at the tablet width. The final test uses 80 rows to force actual long-content overflow while retaining the strict geometry assertions, and separately checks short-content wrapping.

## Remaining evidence boundaries

- A live, enabled, correctly signed host Provider read and an actual host appearance change during an installation still need their own integration evidence. The Bundle contract test does not establish that access path.
- Real status/navigation-bar, display-cutout and keyboard behavior on edge-to-edge API 35+ windows still needs visual/interactive acceptance. Detached-view measurement and injected bars are supplementary evidence.
- The complete device matrix, TalkBack/keyboard interaction, all long translations, actual multi-window resizing and repeated light/dark changes remain separate checks. The Arabic/fontScale-2 view case is one verified combination, not the whole matrix.
- Installation screen lifecycle, authoritative package results and host-UID routing are documented by the main P3 integration evidence, rather than inferred from this supporting layer.

No new dependency, permission or translated string was required by this appearance layer. Referenced build logs and generated test reports are local ignored artifacts; the concrete measurements and outcomes above are retained here.
