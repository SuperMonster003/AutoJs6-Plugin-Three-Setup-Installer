# P3 appearance evidence

Date: 2026-10-01.

Screenshot review of the real Arabic/font-2/night/IME case also exposed an English Cancel label on HyperOS, whose framework did not provide the requested translation. Cancellation text in installation, privileged uninstallation and notifications now uses the plugin's own action_cancel resource in all 11 resource directories. The geometry fixture consumes that same resource; this avoids depending on which framework languages an OEM includes.

The appearance support layer has passed the JVM and API 24 contract/view tests described below. Follow-ups also passed real-window and real-IME checks on API 35, API 28 and a user-started API 24 AVD, including private RTL/large-text/night configurations and landscape. The complete device and host-integration matrix remains open; these geometry fixtures do not establish a successful live host Provider read.

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

## Follow-up: real windows and IME on API 35 and API 28

`InstallerDialogWindowDeviceTest` uses the debug-only `DialogGeometryActivity` with the production dialog theme and kit. It focuses a real `EditText`, opens the device's existing IME, waits for stable actual window/inset measurements, enters text, and taps fixed actions at their measured screen positions. It checks full visibility of the surface, title, focused editor and actions against the actual available screen region, then closes the IME and checks restored bounds and body scrolling. It also tests a real landscape window. The tests preserve global/Application language, font scale, night mode and selected IME; Arabic, fontScale 2 and dark mode are private Activity-context test overrides, not changes to device settings.

- Xiaomi 23046RP50C / API 35: `build/p3-followup-api35-window.log` reports `OK (3 tests)` in 15.301 seconds. All three real-window cases passed.
- Sony G8441 / API 28: the three real-window cases individually passed in `build/p3-followup-api28-grant-window.log`. This was a mixed four-case group with one failed unknown-source permission case, so the group as a whole failed (`Tests run: 4, Failures: 1`, 75.975 seconds). It is not recorded as a fully passing group.

Selected measured rectangles are in screen pixels. Portrait IME measurements were the same for the current appearance and the private Arabic/fontScale-2/dark configuration:

| Device and state | Physical window | Available screen rectangle | IME bottom inset | Dialog surface rectangle |
|---|---|---|---:|---|
| API 35 portrait, IME hidden/restored | 1800 x 2880 | `(0,60)-(1800,2840)` | 0 | `(200,268)-(1600,2631)` |
| API 35 portrait, real IME shown | 1800 x 2880 | `(0,60)-(1800,1955)` | 925 | `(200,202)-(1600,1812)` |
| API 35 landscape | 2880 x 1800 | `(0,60)-(2880,1760)` | 0 | `(740,187)-(2140,1632)` |
| API 28 portrait, IME hidden/restored | 720 x 1280 | `(0,48)-(720,1184)` | 0 | `(48,133)-(672,1098)` |
| API 28 portrait, real IME shown | 720 x 1280 | `(0,48)-(720,749)` | 531 | `(48,101)-(672,696)` |
| API 28 landscape, navigation bar at right | 1280 x 720 | `(0,48)-(1184,720)` | 0 | `(48,98)-(1136,669)` |

The API 35 density was 2.5 and the 1400 px surface width therefore respected the 560 dp cap. The API 28 density was 2.0; its portrait surface retained 48 px / 24 dp on each side, and landscape excluded the 96 px right navigation bar. Real IME expansion reduced the available area once, while title/actions remained fully visible. Keyboard dismissal restored the pre-IME rectangle. Actual scrolling exposed the last row without moving the fixed title/actions, and measured-position taps reached the fixed button. These are actual-window observations, supplementing the earlier detached-view evidence.

## Follow-up: real windows and IME on the user-started API 24 AVD

On 2026-10-01, the user manually started the Android SDK built for x86 / API 24 AVD. `build/p4-api24-p3-followup.log` reports `OK (15 tests)` in 14.349 seconds: all 12 `InstallDialogDeviceTest` cases and all three `InstallerDialogWindowDeviceTest` cases passed, with no failures or skips. The three real-window cases cover portrait IME opening/restoration, private Arabic RTL/fontScale 2/night mode with the real IME, and landscape scrolling with fixed actions.

| API 24 state | Physical window | Available screen rectangle | IME bottom inset | Dialog surface rectangle |
| --- | --- | --- | ---: | --- |
| Portrait, IME hidden/restored | 1440 x 2560 | `(0,84)-(1440,2392)` | 0 | `(84,257)-(1356,2218)` |
| Portrait, real IME shown | 1440 x 2560 | `(0,84)-(1440,1499)` | 1061 | `(84,190)-(1356,1392)` |
| Landscape, navigation bar at right | 2560 x 1440 | `(0,84)-(2392,1440)` | 0 | `(216,186)-(2176,1338)` |

Portrait rectangles matched in the normal and private Arabic/fontScale-2/night configurations. Logs explicitly show `imeVisible=true`, a focused active editor, and fully visible title/actions while the keyboard is open. At density 3.5, the portrait margins are 84 px / 24 dp and the landscape surface width is 1960 px / 560 dp. Dismissing the keyboard restores the earlier rectangle; scrolling reveals the last row while fixed actions remain visible. These are actual-window results, separate from the older detached-view geometry measurements.

## Remaining evidence boundaries

- A live, enabled, correctly signed host Provider read and an actual host appearance change during an installation still need their own integration evidence. The Bundle contract test does not establish that access path.
- The real API 24/28/35 checks above cover the measured system-bar/IME boundaries and portrait/landscape configurations. They do not establish every physical cutout, floating keyboard or multi-window configuration, nor all OEM/system combinations.
- During the build-29 session, starting an additional emulator was rejected by automatic approval review with only the stated reason `blocked by policy`; no alternate launch bypassed that rejection. The later run used an AVD manually started by the user and supplies the previously missing API 24 real-IME evidence.
- The complete device matrix, TalkBack/D-pad interaction, all long translations, actual multi-window resizing and repeated appearance changes remain separate checks. The private Arabic/fontScale-2/dark window is a verified combination, not the whole matrix.
- Installation screen lifecycle, authoritative package results and host-UID routing are documented by the main P3 integration evidence, rather than inferred from this supporting layer.

No new dependency, permission or translated string was required by this appearance layer. Referenced build logs and generated test reports are local ignored artifacts; the concrete measurements and outcomes above are retained here.
