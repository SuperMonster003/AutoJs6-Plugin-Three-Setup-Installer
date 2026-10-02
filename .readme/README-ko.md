<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Android 확인, Shizuku, Root 또는 Dhizuku로 앱 설치, 업데이트 및 제거</p>

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

3-Setup Installer는 독립 홈 화면, AutoJs6의 진입점과 스크립트, 외부 앱의 패키지 열기 및 공유 요청을 통해 Android 앱을 설치, 업데이트, 검사 및 제거합니다. Android 확인과 Shizuku, Root 또는 Dhizuku를 통한 특권 작업을 지원합니다.

플러그인이 호스트와 독립적으로 패키지 검사, 설치 및 결과를 처리합니다. 대화상자, 무음 또는 알림 모드를 선택할 수 있습니다. 알림 모드는 확인, 취소 및 결과를 알림으로 표시하며 필요한 Android 확인은 해당 알림을 눌러야 열립니다.

******

### 현재 상태

******

1.2.0은 아래 설치, 앱 관리 및 스크립트 기능을 구현합니다. 공식 GitHub Release 게시와 플러그인 센터 등록은 아직 완료되지 않았습니다. 호스트 연동에는 AutoJs6 >= 6.8.0 (5299), `installer` 스크립트 API에는 빌드 5300 이상이 필요합니다. 기기 검증 범위와 남은 검증 항목은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다. Dhizuku, 알림 설치 및 영구 기본 설치 프로그램의 스크립트 옵션에는 installer V2와 AutoJs6 6.8.0 빌드 5307 이상이 필요합니다. 기본 호스트 연동은 빌드 5299, V1 스크립트 메서드는 5300 이상을 계속 지원합니다.

******

### 기능

******

1.2.0에 구현된 기능:

- 패키지 형식: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 및 APK가 들어 있는 ZIP 아카이브. 분할 패키지는 기기에 맞게 선택되며 `.aab` 파일은 인식과 설명만 하고 설치하지 않습니다.
- `none`은 Android 확인을 사용합니다. 새 설정의 `auto`는 사용 가능한 `shizuku -> root -> dhizuku -> none` 순서로 선택하며 순서와 사용 여부를 바꿀 수 있습니다. 저장된 기존 세 방식의 설정은 상대 순서와 사용 여부를 유지하고 Dhizuku를 `none` 앞에 비활성 상태로 추가합니다. 명시적으로 지정한 방식은 다른 방식으로 바뀌지 않습니다.
- 설치 성공 후 원본 삭제를 선택적으로 시도합니다. 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회 (Android 14+), 설치 프로그램 지정 및 다른 대상 사용자에는 Shizuku 또는 Root가 필요하며 Android 제한도 적용됩니다.
- 설치된 앱을 이름이나 패키지로 검색하고 이름, 설치 시간 또는 업데이트 시간으로 정렬하며 시스템 앱을 표시할 수 있습니다. 앱이나 시스템 정보를 열고 제거를 확인할 수 있습니다. Shizuku와 Root는 무음 제거와 선택적 데이터 보존을 지원합니다. Dhizuku는 현재 소유자 사용자에서만 특권 제거를 지원하고 `keepData`는 지원하지 않습니다.
- 확인 화면에 앱 정보, 이전 및 새 버전, 서명과 선택 가능한 APK 구성요소를 표시합니다. 진행 중에 취소할 수 있으며, 결과에 성공 시 작업 또는 복사 가능한 오류 정보를 표시합니다. 일괄 설치는 항목별 상태를 표시합니다.
- 하나 또는 여러 설치 패키지를 열거나 공유할 수 있으며 MT Manager에서 공유한 APKS 파일도 지원합니다. 여러 패키지는 순차 대기열에 들어갑니다. 실패한 외부 항목은 URI와 접근 권한이 유효한 동안 재시도할 수 있습니다.
- `interaction: 'notification'`은 설치 전용입니다. 초기 확인, 취소, 진행 및 결과를 알림으로 표시하며 플러그인 설치 대화상자는 열지 않습니다. Android 확인에는 알림을 눌러야 합니다. 알림 권한, 앱 알림 및 설치 채널이 활성화되어야 하며 그렇지 않으면 `NOTIFICATION_UNAVAILABLE`로 실패합니다. 다른 모드는 알림 권한이 없어도 실행됩니다. 제거에는 `notification`을 사용할 수 없습니다.
- 외관 설정에는 언어, 야간 모드, 테마 색상 및 런처 아이콘이 있습니다. 처음 세 항목은 기본적으로 AutoJs6를 따르며 로컬 설정으로 변경할 수 있습니다. 호스트를 사용할 수 없으면 시스템 언어와 야간 모드 및 기본 색상을 사용합니다. 아이콘은 밝게, 어둡게, 자동, 투명 모드를 제공하며 자동 모드는 시스템을 따릅니다. 실제 표시는 런처 캐시와 마스크의 영향을 받습니다.
- 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- 스크립트 API `installer` (별칭 `$installer`)는 동기, `...Async` 및 세션 방식을 제공하며 단일 / 일괄 / 분할 설치, 제거, 검사, 권한 방식과 사용자 조회 및 기본 설치 프로그램 설정을 지원합니다. 실패는 안정적인 `code`를 가진 `InstallerError`입니다 (AutoJs6 >= 6.8.0 (5300) 필요).
- 독립 홈에는 Shizuku/Root/Dhizuku의 사용 가능 여부와 권한 상태, 현재 기본 설치 프로그램, 진행 중인 작업 및 최근 설치가 표시됩니다. 시스템 파일 선택기에서 여러 패키지를 선택해 순차 설치하고, 개별 실패 후 계속하거나 남은 항목을 취소할 수 있습니다.
- 비공개 설치 기록은 최대 200개 항목을 보관하며 패키지, 이름, 이전/새 버전, 결과, 시간, 출처 (호스트/스크립트/외부/홈), 권한 방식 및 실패 정보를 포함합니다. 앱이나 원본 파일을 삭제하지 않고 개별 기록을 삭제하거나 모두 지울 수 있습니다. 프로세스 종료 후 미완료 항목은 취소됨으로 표시되며 자동 재개되지 않습니다.
- 설정에 권한 방식 순서와 사용 여부, 설치 옵션 및 알림 환경설정을 저장합니다. 홈/외부 설치의 기본값은 `dialog`이며 `auto`, `silent`, `notification`을 명시할 수 있습니다. 호스트 UI는 `dialog`를 사용하고 스크립트는 명시한 옵션과 기본 `auto`를 유지합니다. 변경은 확인 후 저장됩니다.
- 설정에서 정보 페이지와 10개 언어로 제공되는 내장 버전 기록을 열 수 있습니다. 수동 업데이트 확인은 12시간 간격으로 플러그인의 GitHub Releases API를 사용하며 결과 캐시와 무시한 버전 관리를 지원합니다. 릴리스 페이지는 브라우저에서 열리고 업데이트가 자동 다운로드되거나 설치되지는 않습니다.
- `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.
- 고급 설치 옵션: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason`, `packageSource`. 지원하지 않는 플랫폼이나 권한 방식은 명시적으로 거부합니다. none은 수동 컴파일을 추가하지 않으며 Android의 기본 컴파일을 끄지 않습니다.
- 성공 결과에는 읽어 온 `updateOwner` 및 `dexopt`가 포함될 수 있습니다. null은 Android가 현재 호출 주체에 owner를 반환하지 않았음을 뜻하며 가시성 제한일 수 있어 전체적으로 없다는 증거가 아닙니다. 읽기 실패 시 필드를 생략하고 notes에 기록합니다. DexOpt 상태는 accepted/failed/cancelled/timeout/unavailable/unknown입니다. accepted에는 시스템의 건너뛰기도 포함되며 실제 컴파일을 보장하지 않습니다. 추가 작업 실패는 확인된 설치 성공을 바꾸지 않습니다.
- 서명 검사와 로컬 패키지 이름/SharedUID 정확 일치 차단 목록은 모든 진입점에 적용되며 `BLOCKED_BY_POLICY`를 사용합니다. 실제 dialog에서만 해당 항목의 mismatch/unknown 서명을 한 번 허용할 수 있고 Android의 서명 검증은 유지됩니다. 무음/알림 모드에서는 허용할 수 없고 차단 목록은 무시할 수 없습니다. 권한 미리 보기는 선택한 APK 분할의 선언이며 실제 부여 목록이 아닙니다.
- 설정에서 출처별 프로필 이름, 활성화, 편집 및 순서를 관리. 출처와 실제 패키지 접두사가 모두 일치하는 첫 활성 프로필을 적용하며 여러 프로필을 합치지 않음. 모든 출처에는 홈도 포함. 앱별 12개 옵션의 일부 기본값만 제공하며 명시적 요청이 우선하고 실제 확인 화면에서 최종 변경 가능.

******

### 사용 방법

******

1. Android 7.0 이상에서 공식 게시 후 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases)의 APK를 설치하거나 공식 색인 등록 후 AutoJs6 플러그인 센터의 설치 마법사를 사용하세요. 게시 전 테스트에는 관리자가 제공한 빌드 또는 소스에서 직접 만든 빌드를 사용하세요. 런처 아이콘으로 독립 홈 화면을 열 수 있습니다.
2. 홈에서 권한과 기본 설치 프로그램 상태를 확인하고 하나 또는 여러 패키지를 선택하세요. 선택한 대화상자 또는 알림에서 확인하고 작업과 최근 기록을 볼 수 있습니다.
3. AutoJs6 연동에는 빌드 5299 (6.8.0) 이상을 사용하고 플러그인 센터에서 `3-Setup Installer`를 활성화하세요. 스크립트 API에는 빌드 5300 이상이 필요합니다. Dhizuku, 알림 설치 및 영구 기본 설치 프로그램의 스크립트 옵션에는 installer V2와 AutoJs6 6.8.0 빌드 5307 이상이 필요합니다. 기본 호스트 연동은 빌드 5299, V1 스크립트 메서드는 5300 이상을 계속 지원합니다.
4. AutoJs6의 설치 기능을 사용하거나 패키지를 열거나 공유할 때 3-Setup Installer를 선택하세요. 확인 대화상자가 나타나면 앱과 옵션을 검토한 후 설치하세요. 특권 방식을 선택할 때는 Shizuku, Root 또는 Dhizuku 권한을 준비하세요.
5. 홈 메뉴에서 설치된 앱과 설정으로 이동하세요. 로컬 설치 기본값, 외관, 아이콘 및 알림을 확인할 수 있으며 정보, 버전 기록 및 수동 업데이트 확인은 설정에 있습니다.

******

### 인증 방식

******

각 인증 방식이 할 수 있는 일과 필요한 것:

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku가 실행 중이어야 하며 (무선 디버깅, ADB 또는 Root로 시작), 3-Setup Installer에 별도로 권한을 부여해야 합니다. AutoJs6에 부여한 권한은 이 플러그인에 적용되지 않습니다. 설치, 제거 및 다른 사용자에 대한 작업은 실행 중인 Shizuku 서비스의 권한을 사용합니다.
- `root`: Root 권한을 얻은 기기와 3-Setup Installer에 `su`를 허용하는 Root 관리자가 필요합니다. libsu로 특권 설치, 제거, 사용자 및 기본 설치 프로그램 작업을 제공합니다. 각 작업의 허용 여부는 Android와 ROM 정책이 결정합니다.
- `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.
- **참고:** 스크립트는 기본적으로 `interaction: 'auto'`를 사용하며 특권이 있으면 확인 창 없이 설치합니다. 호스트 UI의 설치 기능은 `dialog`를 사용합니다. Android가 확인을 요구하면 `auto`는 이를 허용하고 `notes`에 기록합니다. 설치 전에 확인하려면 `interaction: 'dialog'`를 지정하세요. 명시적 `silent`는 특권이 없거나 시스템 확인이 필요하면 `AUTHORIZER_REQUIRED`로 실패합니다.
- 설정에 권한 방식 순서와 사용 여부, 설치 옵션 및 알림 환경설정을 저장합니다. 홈/외부 설치의 기본값은 `dialog`이며 `auto`, `silent`, `notification`을 명시할 수 있습니다. 호스트 UI는 `dialog`를 사용하고 스크립트는 명시한 옵션과 기본 `auto`를 유지합니다. 변경은 확인 후 저장됩니다.

******

### 빠른 시작

******

AutoJs6 >= 6.8.0 (5300)용 `install`, `installAsync`, `session`, `uninstall`, `setDefault` 예제입니다. 함수는 선택한 원본, 패키지 이름 또는 기본 설치 프로그램 옵션을 전달해 호출할 때만 실행됩니다. 처음의 상태 조회는 읽기 전용입니다.

```js
// 사용 가능 여부와 호환성을 읽기 전용으로 조회합니다.
console.log(installer.status);

// Shizuku 권한 필요. Android가 확인을 요구하면 silent는 실패합니다.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// 배열은 독립 패키지를 나타내며 각 항목에 결과가 있습니다.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// base APK를 포함한 모든 분할 파일은 한 앱에 속합니다.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// 호출 즉시 시작합니다. 취소하거나 기다리려면 반환된 세션을 보관하세요.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// 제거할 패키지 이름만 전달하세요. keepData는 데이터 보존을 요청합니다.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true는 이 플러그인을 기본값으로 설정, false는 해제합니다. ROM 제한 적용.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// V2 예제에는 호스트 빌드 5307이 필요합니다. Dhizuku는 활성 소유자, 알림 모드는 알림 권한이 필요합니다.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root 영구 모드는 사용자 0의 system UID 접근을 지원하는 기기가 필요합니다. 경쟁 정책은 유지합니다.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});

// 고급 예제: 호스트 V3, Root 및 해당 메타데이터용 API 33+ 필요. 함수를 호출하면 설치 시작
let installAdvancedChosen = source => installer.installAsync(source, {
    authorizer: 'root', interaction: 'dialog', deleteSource: false,
    grantAllRequestedPermissions: false, requestUpdateOwnership: false,
    dexopt: 'speed-profile', installReason: 'user', packageSource: 'local-file',
}).then(result => console.log(result.ok, result.updateOwner, result.dexopt, result.notes))
    .catch(error => console.error(error.code, error.systemMessage));

// 프로필 예제: 호스트 5312+ 필요. 파일 보존 및 세 상속 필드 재설정. 함수 호출 시 설치 시작
let installWithProfileResets = source => installer.installAsync(source, {
    interaction: 'dialog', deleteSource: false,
    installer: null, installReason: null, packageSource: null,
});
```

원본에는 경로, `file://` 또는 읽을 수 있는 `content://` URI를 사용할 수 있습니다. 배열은 독립적인 일괄 항목이며 `{ splits: [...] }`는 한 앱을 설치합니다. `session(...)`은 생성 즉시 시작하고 반환된 객체는 `cancel()`과 `wait()`를 지원합니다. 동기 호출은 `InstallerError`를 발생시킬 수 있고 UI 스레드에서는 사용할 수 없습니다. `installer.status` 읽기, `installer.session(...)` 생성 및 `session.wait()` 호출도 이 제한을 따릅니다. UI 스레드에서는 Async 메서드를 사용하거나 스크립트 작업 스레드에서 동기 작업을 실행하세요. 세션 객체는 이를 만든 스크립트 스레드에서만 사용해야 합니다. Promise 거부를 처리하고 각 일괄 결과의 `ok`와 `error`를 확인하세요. 플러그인이 없거나 호환되지 않으면 `PLUGIN_UNAVAILABLE`을 보고합니다. `setDefault`는 기본값 해제를 포함하여 요청한 상태에 도달했는지 반환합니다. `app.uninstall`은 기존 시스템 제거 바로가기로 유지됩니다. 특권 옵션에는 `installer.uninstall`을 사용하세요. 전체 옵션과 이벤트는 [installer API 문서](https://docs.autojs6.com/#/installer)를 확인하세요.

Dhizuku, 알림 설치 및 영구 기본 설치 프로그램의 스크립트 옵션에는 installer V2와 AutoJs6 6.8.0 빌드 5307 이상이 필요합니다. 기본 호스트 연동은 빌드 5299, V1 스크립트 메서드는 5300 이상을 계속 지원합니다.

고급 스크립트 옵션에는 AutoJs6 5308+ 및 V3 `advanced-install-options`가 필요. 프로필 덮어쓰기가 없으면 생략 시 기존 동작을 유지하며 명시적 `false`/`none`도 지원이 필요. 공식 출시 및 장치 검증 상태는 로드맵 참조.

호스트 UI 및 스크립트 프로필은 AutoJs6 5312+ 및 V3 `source-profiles`가 필요. 이전 호스트나 협상하지 않은 요청은 기존 동작을 유지. 생략한 필드는 상속하고 명시적 `false`/`auto`/`current`/`none`은 덮어씀. null 재설정은 `installer`, `installReason`, `packageSource`만 허용하며 뒤의 두 필드는 고급 옵션 지원도 필요. `interaction`, `timeout`, `continueOnError`는 세션 전체 값으로 프로필에서 제외.

처리 시작 시 프로필과 자동 권한 순서를 고정하여 설정 편집이 진행 중 작업이나 같은 배치의 후속 항목을 바꾸지 않음. 확인된 재시도는 확인한 모든 옵션을 유지하고 미확인 재시도는 진입점 기본값과 프로필을 다시 읽음. 매번 소스와 서명을 다시 검사하며 일회성 정책 허용을 재사용하지 않음. 프로필로 서명, 블랙리스트, 권한 또는 Android 제한을 우회할 수 없음.

프로필 협상 후 성공 항목의 `sourceDeleteRequested`는 최종 삭제 요청이며 삭제 완료 증거가 아님. 호스트의 명시적 `deleteSource: false`는 삭제를 금지하며 결정값이 없거나 잘못된 형식이면 파일을 유지하고 notes에 기록. 호스트는 배치 결과 후 실패, 미처리 또는 보존 항목과 공유하는 파일 및 확인된 별칭을 유지. 로컬 외부 진입점도 중복 URI/정규화 경로를 유지. 실제 정리는 `sourceDeleted`로 표시하며 호스트는 content URI를 삭제하지 않음.

******

### 호환성

******

플러그인의 능력을 결정하는 플랫폼 사실:

- Android 7.0 (API 24) 이상. 기기 검증 현황과 남은 검증 범위는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다.
- 낮은 targetSdk 차단 우회는 Android 14 (API 34)부터 존재합니다. 더 오래된 시스템에서는 이 옵션이 무시되고 결과에 표시됩니다.
- 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.
- 권한 부여 요청과 none 이외의 DexOpt에는 Shizuku/Root가 필요하고 verify는 API 26+가 필요합니다. 설치 이유는 API 26+, 출처는 API 33+, 업데이트 소유권 요청은 API 34+가 필요합니다. 소유권은 최초 설치에서만 활성화되며 업데이트나 다른 사용자에게 이미 있는 패키지에서는 무시될 수 있습니다. false는 기존 owner를 해제하지 않습니다.
- none/Dhizuku의 설치된 서명 확인은 현재 사용자만 대상으로 하며 Shizuku/Root는 전체를 조회합니다. SharedUID 규칙이 있으면 다른 사용자의 기존 패키지를 배제할 수 없는 경우 예외 없이 거부합니다.

******

### 자주 묻는 질문

******

- **왜 여전히 확인이 필요한가요?** `none`은 항상 Android 확인이 필요하고 특권 방식도 Android 정책의 영향을 받습니다. `notification`은 알림 동작으로만 시스템 확인을 열며 Android 확인을 우회하지 않습니다.
- **`.aab`를 설치할 수 있나요?** 아니요. Android App Bundle은 배포 형식이므로 먼저 bundletool로 `.apks` 세트로 변환하세요. 플러그인은 `.aab` 파일을 인식하고 패키지와 모듈 정보를 표시합니다.
- **`allowDowngrade: true`인데 다운그레이드가 실패하는 이유는?** 이 옵션은 다운그레이드를 요청할 뿐이며 Android가 펌웨어, 권한을 실행하는 주체 및 debuggable 여부에 따라 결정합니다. 검증한 user 펌웨어에서 Sony G8441 / API 28과 Xiaomi 23046RP50C / API 35는 debuggable이 아닌 패키지의 다운그레이드를 거부했지만 Sony XQ-DQ72 / API 33의 Root 경로는 허용했습니다. 이는 각 기기의 결과입니다. 오류와 `systemMessage`를 확인하세요. Root도 모든 ROM에서 성공을 보장하지 않습니다.
- **HyperOS에서 어떤 설치자 패키지 이름을 사용할 수 있나요?** ADB 또는 무선 디버깅으로 시작한 Shizuku에서는 설치자 패키지 이름을 지정하지 않으면 `com.android.shell`을 사용합니다. 검증한 Xiaomi 23046RP50C / HyperOS / API 35에서는 확인 창 없는 새 설치와 업데이트 모두 이 값이 기록되었습니다. `com.android.shell` 또는 플러그인 자체 패키지 이름을 명시한 경우에도 성공했으며 요청한 값이 기록되었습니다. 다른 패키지 이름이나 ROM 버전은 시스템 응답에 따라 달라집니다.
- **ColorOS 또는 다른 ROM에서 플러그인 활성화가 필요하다고 하면?** 새로 설치하거나 강제 종료한 후 Android는 사용자가 상호작용할 때까지 앱을 중지 상태로 유지할 수 있습니다. AutoJs6 플러그인 센터에 활성화 작업이 표시되면 실행하거나, 런처 아이콘으로 3-Setup Installer를 연 다음 다시 시도하세요. 이는 [Android의 중지 상태 규칙](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED)을 따릅니다. ColorOS 고유 동작은 아직 실기기에서 검증하지 않았습니다.
- **기본 설치 프로그램 변경이 실패하거나 저장된 영구 표시가 현재 처리 앱과 다른 이유는?** 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- **원본이 삭제되지 않은 이유는?** 설치 성공 후에만 삭제를 시도합니다. 설치 실패, 취소 또는 시간 초과 시에는 원본을 항상 유지합니다. 삭제 실패는 설치 성공 결과를 바꾸지 않으며 외부 제공자가 삭제를 거부할 수 있습니다. 스크립트의 `deleteSource`는 호스트가 경로 또는 `file://` 원본을 삭제하고 `content://` 원본은 유지합니다. `sourceDeleted`와 `notes`를 확인하세요. 일괄 처리에서 다른 항목이 실패하거나 나머지 대기열이 취소되어도 성공이 확인된 항목에는 `deleteSource`가 적용됩니다.
- **다시 시도하거나 이어서 실행할 수 있나요?** 실패한 외부 URI는 원본과 접근 권한을 사용할 수 있는 동안 다시 시도할 수 있습니다. 원본 또는 접근 권한이 해제되면 패키지를 다시 여세요. 프로세스가 다시 시작되면 복원된 화면에 저장된 확정 결과가 표시되고 미완료 항목은 중단으로 표시됩니다. 복원된 화면은 읽기 전용이며 설치나 재시도를 자동으로 실행하지 않습니다. 다시 시작하기 전에 실제 설치 상태를 확인하세요.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6만 접근할 수 있습니다. 외부 "연결 프로그램" 진입점은 패키지 파일만 받아들이며 스크립트를 실행하지 않습니다.
- REQUEST_INSTALL_PACKAGES와 REQUEST_DELETE_PACKAGES는 Android 확인을 지원합니다. QUERY_ALL_PACKAGES는 설치된 앱 관리, 버전 및 서명 비교, 기본 설치 프로그램 감지에 사용됩니다. 일반 권한 ENFORCE_UPDATE_OWNERSHIP은 명시적 업데이트 소유권 요청에 사용하며 owner 할당을 보장하지 않습니다.
- FOREGROUND_SERVICE와 FOREGROUND_SERVICE_DATA_SYNC는 설치 및 임시 원본 접근을 지원하고 POST_NOTIFICATIONS는 알림에 사용됩니다. `notification`은 알림과 설치 채널이 켜져 있어야 합니다. 다른 모드는 알림 권한이 없어도 실행됩니다.
- Shizuku, Root 및 Dhizuku는 요청한 작업에 사용됩니다. 플러그인은 기기/프로필 소유자를 설정하지 않습니다. 영구 규칙은 설정 또는 해제 요청으로만 변경되며 패키지를 업로드하지 않습니다.
- 설치, 검사, 기록 및 앱 관리는 오프라인으로 동작합니다. INTERNET은 사용자가 수동으로 버전을 확인할 때만 12시간 간격으로 플러그인의 고정 GitHub Releases API에 접근하는 데 사용됩니다. 백그라운드 업데이트 확인이나 패키지 업로드는 수행하지 않습니다.
- 패키지 원본은 읽기 전용으로 열립니다. 기록에는 제한된 앱 메타데이터와 결과만 저장되며 패키지 내용이나 원본 URI는 저장되지 않습니다. 오류의 경로는 가려집니다. 비공개 저장소는 백업에서 제외됩니다. 기록을 삭제해도 앱이나 원본은 삭제되지 않습니다.
- 권한 옵션은 시스템이 부여할 수 있는 권한을 요청하며 Android 14의 USE_FULL_SCREEN_INTENT처럼 설치 관리자가 변경할 수 있는 app-op을 포함할 수 있습니다. 모든 선언 권한을 보장하거나 접근성, 오버레이, 임의의 서명 권한을 부여하지 않습니다. restricted/system-fixed/policy-fixed 제약을 유지하고 restricted 권한 allowlist 플래그를 추가하지 않습니다.
- 서명 검사와 로컬 패키지 이름/SharedUID 정확 일치 차단 목록은 모든 진입점에 적용되며 `BLOCKED_BY_POLICY`를 사용합니다. 실제 dialog에서만 해당 항목의 mismatch/unknown 서명을 한 번 허용할 수 있고 Android의 서명 검증은 유지됩니다. 무음/알림 모드에서는 허용할 수 없고 차단 목록은 무시할 수 없습니다. 권한 미리 보기는 선택한 APK 분할의 선언이며 실제 부여 목록이 아닙니다.

공식 게시 후 플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

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

`ThreeSetupInstallerPluginService`는 `org.autojs.plugin.INSTALLER` (category `installer`)에 응답하며 호스트 installer-api 계약 `org.autojs.plugin.installer.api.IInstallerPlugin`를 구현합니다. `ThreeSetupInstallerPluginInfoService`는 `org.autojs.plugin.INFO`에 PluginInfo로 응답합니다. `WakeActivity`는 호스트가 플러그인을 활성화하는 데 사용됩니다.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.2.0

_2026/10/02_

- `기능` 고급 설치 옵션: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason`, `packageSource`. 지원하지 않는 플랫폼이나 권한 방식은 명시적으로 거부합니다. none은 수동 컴파일을 추가하지 않으며 Android의 기본 컴파일을 끄지 않습니다.
- `기능` 서명 검사와 로컬 패키지 이름/SharedUID 정확 일치 차단 목록은 모든 진입점에 적용되며 `BLOCKED_BY_POLICY`를 사용합니다. 실제 dialog에서만 해당 항목의 mismatch/unknown 서명을 한 번 허용할 수 있고 Android의 서명 검증은 유지됩니다. 무음/알림 모드에서는 허용할 수 없고 차단 목록은 무시할 수 없습니다. 권한 미리 보기는 선택한 APK 분할의 선언이며 실제 부여 목록이 아닙니다.
- `기능` 설정에서 출처별 프로필 이름, 활성화, 편집 및 순서를 관리. 출처와 실제 패키지 접두사가 모두 일치하는 첫 활성 프로필을 적용하며 여러 프로필을 합치지 않음. 모든 출처에는 홈도 포함. 앱별 12개 옵션의 일부 기본값만 제공하며 명시적 요청이 우선하고 실제 확인 화면에서 최종 변경 가능.
- `개선` 고급 스크립트 옵션에는 AutoJs6 5308+ 및 V3 `advanced-install-options`가 필요. 프로필 덮어쓰기가 없으면 생략 시 기존 동작을 유지하며 명시적 `false`/`none`도 지원이 필요. 공식 출시 및 장치 검증 상태는 로드맵 참조.
- `개선` 성공 결과에는 읽어 온 `updateOwner` 및 `dexopt`가 포함될 수 있습니다. null은 Android가 현재 호출 주체에 owner를 반환하지 않았음을 뜻하며 가시성 제한일 수 있어 전체적으로 없다는 증거가 아닙니다. 읽기 실패 시 필드를 생략하고 notes에 기록합니다. DexOpt 상태는 accepted/failed/cancelled/timeout/unavailable/unknown입니다. accepted에는 시스템의 건너뛰기도 포함되며 실제 컴파일을 보장하지 않습니다. 추가 작업 실패는 확인된 설치 성공을 바꾸지 않습니다.
- `개선` 권한 부여 요청과 none 이외의 DexOpt에는 Shizuku/Root가 필요하고 verify는 API 26+가 필요합니다. 설치 이유는 API 26+, 출처는 API 33+, 업데이트 소유권 요청은 API 34+가 필요합니다. 소유권은 최초 설치에서만 활성화되며 업데이트나 다른 사용자에게 이미 있는 패키지에서는 무시될 수 있습니다. false는 기존 owner를 해제하지 않습니다.
- `개선` 권한 옵션은 시스템이 부여할 수 있는 권한을 요청하며 Android 14의 USE_FULL_SCREEN_INTENT처럼 설치 관리자가 변경할 수 있는 app-op을 포함할 수 있습니다. 모든 선언 권한을 보장하거나 접근성, 오버레이, 임의의 서명 권한을 부여하지 않습니다. restricted/system-fixed/policy-fixed 제약을 유지하고 restricted 권한 allowlist 플래그를 추가하지 않습니다.
- `개선` none/Dhizuku의 설치된 서명 확인은 현재 사용자만 대상으로 하며 Shizuku/Root는 전체를 조회합니다. SharedUID 규칙이 있으면 다른 사용자의 기존 패키지를 배제할 수 없는 경우 예외 없이 거부합니다.
- `개선` 호스트 UI 및 스크립트 프로필은 AutoJs6 5312+ 및 V3 `source-profiles`가 필요. 이전 호스트나 협상하지 않은 요청은 기존 동작을 유지. 생략한 필드는 상속하고 명시적 `false`/`auto`/`current`/`none`은 덮어씀. null 재설정은 `installer`, `installReason`, `packageSource`만 허용하며 뒤의 두 필드는 고급 옵션 지원도 필요. `interaction`, `timeout`, `continueOnError`는 세션 전체 값으로 프로필에서 제외.
- `개선` 처리 시작 시 프로필과 자동 권한 순서를 고정하여 설정 편집이 진행 중 작업이나 같은 배치의 후속 항목을 바꾸지 않음. 확인된 재시도는 확인한 모든 옵션을 유지하고 미확인 재시도는 진입점 기본값과 프로필을 다시 읽음. 매번 소스와 서명을 다시 검사하며 일회성 정책 허용을 재사용하지 않음. 프로필로 서명, 블랙리스트, 권한 또는 Android 제한을 우회할 수 없음.
- `개선` 프로필 협상 후 성공 항목의 `sourceDeleteRequested`는 최종 삭제 요청이며 삭제 완료 증거가 아님. 호스트의 명시적 `deleteSource: false`는 삭제를 금지하며 결정값이 없거나 잘못된 형식이면 파일을 유지하고 notes에 기록. 호스트는 배치 결과 후 실패, 미처리 또는 보존 항목과 공유하는 파일 및 확인된 별칭을 유지. 로컬 외부 진입점도 중복 URI/정규화 경로를 유지. 실제 정리는 `sourceDeleted`로 표시하며 호스트는 content URI를 삭제하지 않음.
- `의존성` installer-api.aar를 계약 V3 (MPL 2.0)로 업그레이드하며 V1/V2와 11개 AIDL 트랜잭션 유지. 고급 스크립트 옵션은 호스트 빌드 5308+ 필요. 같은 V3 계약에 선택적 source-profiles 기능과 프로필 필드를 추가. 호스트 5312부터 지원하며 11개 AIDL 트랜잭션은 유지
- `의존성` 공유 패키지 파서 (MPL 2.0)를 업그레이드하여 실제 매니페스트 루트와 sharedUserId를 검증하고 모호한 입력 거부. 실제 Android 네임스페이스 요소에서 권한 선언을 읽고 주석 및 확장 네임스페이스의 가짜 선언을 제외하며 컴파일된 속성 충돌을 거부.

#### v1.1.0

_2026/10/02_

- `기능` `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.
- `기능` 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- `기능` `interaction: 'notification'`은 설치 전용입니다. 초기 확인, 취소, 진행 및 결과를 알림으로 표시하며 플러그인 설치 대화상자는 열지 않습니다. Android 확인에는 알림을 눌러야 합니다. 알림 권한, 앱 알림 및 설치 채널이 활성화되어야 하며 그렇지 않으면 `NOTIFICATION_UNAVAILABLE`로 실패합니다. 다른 모드는 알림 권한이 없어도 실행됩니다. 제거에는 `notification`을 사용할 수 없습니다.
- `개선` `none`은 Android 확인을 사용합니다. 새 설정의 `auto`는 사용 가능한 `shizuku -> root -> dhizuku -> none` 순서로 선택하며 순서와 사용 여부를 바꿀 수 있습니다. 저장된 기존 세 방식의 설정은 상대 순서와 사용 여부를 유지하고 Dhizuku를 `none` 앞에 비활성 상태로 추가합니다. 명시적으로 지정한 방식은 다른 방식으로 바뀌지 않습니다.
- `개선` 설정에 권한 방식 순서와 사용 여부, 설치 옵션 및 알림 환경설정을 저장합니다. 홈/외부 설치의 기본값은 `dialog`이며 `auto`, `silent`, `notification`을 명시할 수 있습니다. 호스트 UI는 `dialog`를 사용하고 스크립트는 명시한 옵션과 기본 `auto`를 유지합니다. 변경은 확인 후 저장됩니다.
- `의존성` 기기/프로필 소유자 권한 방식을 위한 Dhizuku API 2.6.0 (MIT) 추가
- `의존성` `installer-api.aar`을 계약 V2 (MPL 2.0)로 업그레이드. V1 협상을 유지하고 영구 기본값 메서드를 끝에 추가. 출처와 SHA-256은 서드파티 고지에 기록; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `힌트` 1.0.0은 아래 설치, 앱 관리 및 스크립트 기능을 구현합니다. 공식 GitHub Release 게시와 플러그인 센터 등록은 아직 완료되지 않았습니다. 호스트 연동에는 AutoJs6 >= 6.8.0 (5299), `installer` 스크립트 API에는 빌드 5300 이상이 필요합니다. 기기 검증 범위와 남은 검증 항목은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다.
- `기능` 3-Setup Installer는 독립 홈 화면, AutoJs6의 진입점과 스크립트, 외부 앱의 패키지 열기 및 공유 요청을 통해 Android 앱을 설치, 업데이트, 검사 및 제거합니다. Android 확인과 Shizuku 또는 Root를 통한 특권 작업을 지원합니다
- `기능` 패키지 형식: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 및 APK가 들어 있는 ZIP 아카이브. 분할 패키지는 기기에 맞게 선택되며 `.aab` 파일은 인식과 설명만 하고 설치하지 않습니다
- `기능` 설치 성공 후 원본 삭제를 선택적으로 시도합니다. 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회 (Android 14+), 설치 프로그램 지정 및 다른 대상 사용자에는 Shizuku 또는 Root가 필요하며 Android 제한도 적용됩니다
- `기능` 스크립트 API `installer` (별칭 `$installer`)는 동기, `...Async` 및 세션 방식을 제공하며 단일 / 일괄 / 분할 설치, 제거, 검사, 권한 방식과 사용자 조회 및 기본 설치 프로그램 설정을 지원합니다. 실패는 안정적인 `code`를 가진 `InstallerError`입니다 (AutoJs6 >= 6.8.0 (5300) 필요). 스크립트는 기본적으로 `interaction: 'auto'`를 사용하며 특권이 있으면 확인 창 없이 설치합니다. 호스트 UI의 설치 기능은 `dialog`를 사용합니다. Android가 확인을 요구하면 `auto`는 이를 허용하고 `notes`에 기록합니다. 설치 전에 확인하려면 `interaction: 'dialog'`를 지정하세요. 명시적 `silent`는 특권이 없거나 시스템 확인이 필요하면 `AUTHORIZER_REQUIRED`로 실패합니다
- `기능` 독립 홈에는 Shizuku/Root의 사용 가능 여부와 권한 상태, 현재 기본 설치 프로그램, 진행 중인 작업 및 최근 설치가 표시됩니다. 시스템 파일 선택기에서 여러 패키지를 선택해 순차 설치하고, 개별 실패 후 계속하거나 남은 항목을 취소할 수 있습니다
- `기능` 확인 화면에 앱 정보, 이전 및 새 버전, 서명과 선택 가능한 APK 구성요소를 표시합니다. 진행 중에 취소할 수 있으며, 결과에 성공 시 작업 또는 복사 가능한 오류 정보를 표시합니다. 일괄 설치는 항목별 상태를 표시합니다
- `기능` 포그라운드 설치 진행률, 취소 작업 및 결과 알림을 제공합니다. 알림 권한을 거부해도 설치를 차단하지 않습니다
- `기능` 하나 또는 여러 설치 패키지를 열거나 공유할 수 있으며 MT Manager에서 공유한 APKS 파일도 지원합니다. 여러 패키지는 순차 대기열에 들어갑니다. 실패한 외부 항목은 URI와 접근 권한이 유효한 동안 재시도할 수 있습니다
- `기능` 설치된 앱을 이름 또는 패키지 이름으로 검색하고, 이름, 설치 시간 또는 업데이트 시간으로 정렬하며 시스템 앱을 표시할 수 있습니다. 앱이나 시스템 앱 정보를 열거나, 내용을 검토하고 제거를 확인할 수 있습니다. Shizuku 또는 Root는 확인 후 추가 시스템 확인 없이 제거하고 선택적으로 데이터를 보존할 수 있으며, 그 외에는 Android 확인을 사용합니다
- `기능` 홈 상태 카드와 설정은 같은 기본 설치 프로그램 페이지를 엽니다. 특권으로 기본값을 설정하거나 해제하고, 특권이 없으면 시스템 설정 안내를 제공합니다. OEM 정책에 따라 변경이 차단되거나 이전 처리 앱의 기본값 해제가 필요할 수 있습니다. 스크립트의 `installer.isDefault`, `installer.setDefault`, `setDefaultAsync`도 유지되며 기기의 응답을 그대로 보고합니다
- `기능` 설정은 권한 방식의 순서와 사용 여부, 설치 옵션 및 진행 알림 환경설정을 저장합니다. 홈 및 외부 설치는 기본적으로 `dialog`를 사용하며 명시적으로 저장한 `auto` 또는 `silent` 선택이 적용됩니다. 호스트/스크립트 요청은 명시적 옵션을 유지하고 스크립트 API의 기본값은 계속 `auto`입니다. 선택 내용은 확인한 뒤에만 저장됩니다
- `기능` 외관 설정에는 언어, 야간 모드, 테마 색상 및 런처 아이콘이 있습니다. 처음 세 항목은 기본적으로 AutoJs6를 따르며 로컬 설정으로 변경할 수 있습니다. 호스트를 사용할 수 없으면 시스템 언어와 야간 모드 및 기본 색상을 사용합니다. 아이콘은 밝게, 어둡게, 자동, 투명 모드를 제공하며 자동 모드는 시스템을 따릅니다. 실제 표시는 런처 캐시와 마스크의 영향을 받습니다
- `기능` 비공개 설치 기록은 최대 200개 항목을 보관하며 패키지, 이름, 이전/새 버전, 결과, 시간, 출처 (호스트/스크립트/외부/홈), 권한 방식 및 실패 정보를 포함합니다. 앱이나 원본 파일을 삭제하지 않고 개별 기록을 삭제하거나 모두 지울 수 있습니다. 프로세스 종료 후 미완료 항목은 취소됨으로 표시되며 자동 재개되지 않습니다
- `기능` 설정에서 정보 페이지와 10개 언어로 제공되는 내장 버전 기록을 열 수 있습니다. 수동 업데이트 확인은 12시간 간격으로 플러그인의 GitHub Releases API를 사용하며 결과 캐시와 무시한 버전 관리를 지원합니다. 릴리스 페이지는 브라우저에서 열리고 업데이트가 자동 다운로드되거나 설치되지는 않습니다
- `기능` 10개 언어의 README, 플러그인 센터 안내 및 변경 기록
- `개선` 임의 접근이 가능한 원본은 전체 캐시 복사를 생략하고, 스트림은 필요할 때 임시 저장합니다. ZIP 분할 패키지를 지원하며, AAB는 정보 확인만 허용하고 내용이 변경된 원본은 거부합니다
- `개선` 설치 성공 후에만 삭제를 시도합니다. 설치 실패, 취소 또는 시간 초과 시에는 원본을 항상 유지합니다. 삭제 실패는 설치 성공 결과를 바꾸지 않으며 외부 제공자가 삭제를 거부할 수 있습니다. 스크립트의 `deleteSource`는 호스트가 경로 또는 `file://` 원본을 삭제하고 `content://` 원본은 유지합니다. `sourceDeleted`와 `notes`를 확인하세요. 일괄 처리에서 다른 항목이 실패하거나 나머지 대기열이 취소되어도 성공이 확인된 항목에는 `deleteSource`가 적용됩니다
- `개선` 같은 패키지 설치를 사용자와 권한 부여 방식에 관계없이 순차 실행하며 대기 중 취소와 시간 제한을 유지. 독립 실행 시에도 24시간이 지난 비활성 임시 디렉터리를 안전하게 정리
- `개선` 권한 연결을 설정하는 중 연결이 끊기면 한 번만 자동으로 다시 연결; 이미 시작한 설치나 제거 작업은 자동으로 반복하지 않음
- `의존성` Shizuku 인증 방식을 위해 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 추가
- `의존성` Root 인증 방식을 위해 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 추가
- `의존성` 특권 서비스가 숨겨진 패키지 설치 API에 접근하도록 AndroidHiddenApiBypass 6.1 추가
- `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5298, MPL 2.0) 추가 및 `locks/host-api-aars.lock`에 해시 고정
- `의존성` 설치 계약을 위한 `installer-api.aar` (AutoJs6, MPL 2.0) 추가. 출처와 SHA-256은 서드파티 고지에 기록
- `의존성` APK 및 컨테이너 검사와 분할 파일 선택을 위한 `package-archive-parser.aar` (AutoJs6, MPL 2.0) 추가. 출처와 SHA-256은 서드파티 고지에 기록

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

개발자는 아래 명령으로 플러그인을 빌드하고 검증할 수 있습니다. 공식 게시 전에는 관리자가 제공한 빌드 또는 로컬 빌드를 테스트에 사용하세요. 공식 APK는 Releases 및 색인 등록 후 플러그인 센터를 통해 배포됩니다.

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
