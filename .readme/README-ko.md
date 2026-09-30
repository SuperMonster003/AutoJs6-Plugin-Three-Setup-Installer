<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6와 스크립트를 위해 Android 앱을 설치, 업데이트, 제거하며 Shizuku 또는 Root를 통한 무음 설치를 지원</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Setup Installer는 AutoJs6의 패키지 설치 프로그램을 대신합니다: 파일 관리자, 플러그인 센터, 스크립트 패키징 화면의 설치 버튼, `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 파일의 외부 "연결 프로그램" 진입점, 그리고 앱을 설치, 업데이트, 검사, 제거하는 스크립트 측 전역 객체 `installer`입니다. 일반적인 시스템 확인 외에도 Shizuku 또는 Root를 통해 무음으로 설치하고 제거할 수 있습니다.

AutoJs6는 Binder 서비스를 통해 플러그인을 찾고 패키지 파일을 읽기 전용 파일 디스크립터로 전달합니다. 플러그인은 패키지를 분석하고 인증 방식을 선택하며 필요하면 자체 확인 및 진행률 대화 상자를 표시하고 단계, 진행률, 결과를 보고합니다. 특권 작업은 시스템 패키지 설치 프로그램과 직접 통신하는 Shizuku 사용자 서비스 또는 libsu Root 서비스에서 실행됩니다.

******

### 현재 상태

******

버전 1.0.0은 P0 개발 미리보기입니다: 저장소 골격, AutoJs6 플러그인 센터가 인식하는 플러그인 신원, 특권 설치 검증 (spike). Binder 계약, 설치 엔진, 대화 상자, 스크립트 API, 설정 화면은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)의 단계에 따라 진행됩니다. AutoJs6 6.8.0 (build 5299) 이상이 필요합니다. P0에서 Shizuku와 Root를 통한 자동 설치, 업데이트, 제거 및 일반 기본 설치 프로그램 설정을 검증했습니다. 호스트와 스크립트의 설치 진입점은 아직 제공되지 않으며, 영구 기본 설정은 이번 버전에서 지원하지 않습니다.

******

### 기능

******

플러그인은 다음 기능을 제공합니다:

- 패키지 형식: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 및 APK가 들어 있는 ZIP 아카이브. 분할 패키지는 기기에 맞게 선택되며 `.aab` 파일은 인식과 설명만 하고 설치하지 않습니다.
- 인증 방식: `none` (사용자 확인이 있는 시스템 PackageInstaller 세션), `shizuku`, `root`. `auto`는 설정에서 구성한 순서대로 처음 사용 가능한 방식을 고르며 스크립트에서 명시적으로 지정할 수도 있습니다.
- 설치 옵션: 일괄 설치, 성공 후 원본 파일 삭제, 다운그레이드 허용, 테스트 전용 패키지 허용, 낮은 targetSdk 차단 우회 (Android 14+), 설치자 패키지 이름과 대상 사용자 (특권 인증 방식만).
- Shizuku 또는 Root를 통한 무음 제거 (데이터 유지 옵션 포함). 그 외에는 일반 시스템 대화 상자를 사용합니다.
- 기본 설치 프로그램으로 설정: Shizuku 또는 Root가 있으면 플러그인이 패키지 파일의 기본 처리기가 됩니다. 권한이 없으면 시스템의 "기본으로 열기" 페이지를 열어 줍니다.
- 스크립트 API `installer` (별칭 `$installer`)는 동기, `...Async`, 세션 형태를 제공하며 모든 실패는 안정적인 `code`를 가진 `InstallerError`입니다.

******

### 사용 방법

******

1. AutoJs6 build 5299 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `3-Setup Installer`가 인식되는지 확인하고 활성화합니다.
3. AutoJs6 파일 관리자에서 패키지 파일을 탭하거나, 아무 파일 관리자에서 3-Setup Installer로 패키지를 열거나, 스크립트에서 `installer.install(...)`을 호출합니다. 무음 설치가 필요하면 플러그인 안내에 따라 Shizuku를 시작하거나 Root를 허용하거나 플러그인 설정에서 인증 방식을 선택합니다.

******

### 인증 방식

******

각 인증 방식이 할 수 있는 일과 필요한 것:

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku 앱이 실행 중이고 (무선 디버깅, ADB 또는 Root로 시작) 플러그인에 권한이 부여되어야 합니다. shell 권한으로 동작하여 무음 설치, 무음 제거, 다른 사용자에 설치, 기본 설치 프로그램 잠금이 가능합니다.
- `root`: Root 관리자가 플러그인에 `su`를 허용해야 합니다. libsu Root 서비스를 통해 Shizuku와 같은 작업을 제공합니다. 일반 (user) 펌웨어에서 다운그레이드는 debuggable 앱에만 성공하며 이는 프레임워크 규칙이지 플러그인의 제한이 아닙니다.

******

### 빠른 시작

******

무음 설치, 다운그레이드 허용 업데이트, 세션 감시, 제거를 수행하는 스크립트 (로드맵 P4부터 사용 가능):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### 호환성

******

플러그인의 능력을 결정하는 플랫폼 사실:

- Android 7.0 (API 24) 이상. 호스트 빌드와 플러그인은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 나열된 기기 매트릭스에서 함께 검증됩니다.
- 낮은 targetSdk 차단 우회는 Android 14 (API 34)부터 존재합니다. 더 오래된 시스템에서는 이 옵션이 무시되고 결과에 표시됩니다.
- 일부 OEM 시스템은 어떤 앱이 기본 설치 프로그램이 될 수 있는지 제한하거나 신뢰하는 설치자 패키지 이름을 요구합니다 (HyperOS는 `com.android.shell`을 허용). 플러그인은 시스템의 응답을 그대로 보고합니다.

******

### 자주 묻는 질문

******

- **왜 설치할 때 여전히 확인을 요구하나요?** `none` 인증 방식은 항상 시스템 확인을 거칩니다. Shizuku를 시작하거나 Root를 허용한 다음 설정에서 해당 인증 방식을 선택하거나 스크립트에서 `authorizer: 'shizuku'`를 전달하세요.
- **`.aab`를 설치할 수 있나요?** 아니요. Android App Bundle은 배포 형식이므로 먼저 bundletool로 `.apks` 세트로 변환하세요. 플러그인은 `.aab` 파일을 인식하고 패키지와 모듈 정보를 표시합니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6만 접근할 수 있습니다. 외부 "연결 프로그램" 진입점은 패키지 파일만 받아들이며 스크립트를 실행하지 않습니다.
- REQUEST_INSTALL_PACKAGES와 REQUEST_DELETE_PACKAGES는 일반 설치 및 제거 대화 상자를 지원하고, QUERY_ALL_PACKAGES는 업데이트 전에 설치된 버전을 표시하고 서명을 비교하게 합니다.
- Shizuku와 Root는 사용자가 시작한 작업에만 사용됩니다. 특권 서비스는 상태를 보관하지 않고 작업 사이에 shell을 열어 두지 않으며 플러그인 외부에서 접근할 수 없습니다.
- 패키지 파일은 읽기 전용으로 열립니다. 플러그인은 네트워크 요청을 하지 않고 데이터를 수집하지 않으며 개인 저장소를 백업에서 제외합니다.

플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것입니다. 호스트는 이 식별자로 플러그인을 발견하고 호환성을 협상합니다:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5299 (6.8.0)
```

`ThreeSetupInstallerPluginService`는 `org.autojs.plugin.INSTALLER` (category `installer`)에 응답하며 로드맵 P1부터 호스트 installer-api 계약 `org.autojs.plugin.installer.api.IInstallerPlugin`를 구현합니다. `ThreeSetupInstallerPluginInfoService`는 `org.autojs.plugin.INFO`에 PluginInfo로 응답합니다. `WakeActivity`는 호스트가 플러그인을 활성화하는 데 사용됩니다.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/09/30_

- `힌트` P0 개발 미리보기: 저장소 골격, AutoJs6 플러그인 센터가 인식하는 플러그인 신원, 특권 설치 검증 (spike). Binder 계약, 설치 엔진, 대화 상자, 스크립트 API, 설정 화면은 ROADMAP.md의 단계에 따라 진행됩니다.
- `기능` 플러그인 식별자 `three-setup-installer` (engine `installer`), INFO 서비스, Wake Activity 및 호스트 검색용 `org.autojs.plugin.INSTALLER` 서비스 골격
- `기능` 10개 언어의 README, 플러그인 센터 안내 및 변경 기록
- `개선` P0에서 Shizuku와 Root를 통한 자동 설치, 업데이트, 제거 및 일반 기본 설치 프로그램 설정을 검증했습니다. 호스트와 스크립트의 설치 진입점은 아직 제공되지 않으며, 영구 기본 설정은 이번 버전에서 지원하지 않습니다.
- `개선` 플러그인 ID, engine, 서비스 action / category, Binder descriptor 및 최소 호스트 버전을 호스트 installer-api 계약 상수에서 가져오도록 변경; 기능 선언에 설치기 계약 버전 1을 추가하고 최소 호스트 빌드를 5299로 갱신
- `의존성` Shizuku 인증 방식을 위해 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 추가
- `의존성` Root 인증 방식을 위해 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 추가
- `의존성` 특권 서비스가 숨겨진 패키지 설치 API에 접근하도록 AndroidHiddenApiBypass 6.1 추가
- `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5298, MPL 2.0) 추가 및 `locks/host-api-aars.lock`에 해시 고정
- `의존성` `package-archive-parser.aar` 및 `installer-api.aar` (AutoJs6 모듈 `plugin-api/package-archive-parser` 및 `plugin-api/installer-api`, 호스트 P1 빌드 6.8.0 / 5299, MPL 2.0) 추가 및 `common-plugin-api.aar`와 함께 `locks/host-api-aars.lock`에 해시 고정

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

이 섹션은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다. 일반 사용자는 Releases 페이지의 미리 빌드된 APK를 설치하면 됩니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 계측 테스트 APK 빌드:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

릴리스 산출물을 수집하고 파일 이름에 버전과 CRC32 다이제스트를 추가:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서 소스와 생성된 산출물이 동기화되어 있는지 검증 (CI에서도 적용):

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상과 Android SDK 37이 필요합니다. Gradle과 플러그인 버전은 `version.properties`와 `io.github.supermonster003.autojs6-platform-versions`로 중앙에서 관리됩니다.

******

### 현지화와 문서 생성

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/`와 `.changelog/`의 언어 JSON 파일이 README, 플러그인 센터 안내, 변경 기록의 유일한 소스입니다. 항상 이 JSON 소스를 편집하고 `py .python/generate_markdown.py`를 다시 실행하세요. 생성된 README, `plugin_instruction.md`, 변경 기록 산출물은 절대 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`를 실행하면 모든 생성 산출물을 검증할 수 있습니다.

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE)에 따라 제공됩니다. 서드파티 구성 요소와 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- 설치 프로그램 모듈 문서: https://docs.autojs6.com/#/installer
- InstallerX와 InstallerX Revived (아키텍처 참고, GPL-3.0, 코드 재사용 없음): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
