3-Setup Installer는 독립 홈 화면, AutoJs6의 진입점과 스크립트, 외부 앱의 패키지 열기 및 공유 요청을 통해 Android 앱을 설치, 업데이트, 검사 및 제거합니다. Android 확인과 Shizuku, Root 또는 Dhizuku를 통한 특권 작업을 지원합니다.

1.1.0은 아래 설치, 앱 관리 및 스크립트 기능을 구현합니다. 공식 GitHub Release 게시와 플러그인 센터 등록은 아직 완료되지 않았습니다. 호스트 연동에는 AutoJs6 >= 6.8.0 (5299), `installer` 스크립트 API에는 빌드 5300 이상이 필요합니다. 기기 검증 범위와 남은 검증 항목은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다. Dhizuku, 알림 설치 및 영구 기본 설치 프로그램의 스크립트 옵션에는 installer V2와 AutoJs6 6.8.0 빌드 5307 이상이 필요합니다. 기본 호스트 연동은 빌드 5299, V1 스크립트 메서드는 5300 이상을 계속 지원합니다.

### 사용 방법

1. Android 7.0 이상에서 공식 게시 후 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases)의 APK를 설치하거나 공식 색인 등록 후 AutoJs6 플러그인 센터의 설치 마법사를 사용하세요. 게시 전 테스트에는 관리자가 제공한 빌드 또는 소스에서 직접 만든 빌드를 사용하세요. 런처 아이콘으로 독립 홈 화면을 열 수 있습니다.
2. 홈에서 권한과 기본 설치 프로그램 상태를 확인하고 하나 또는 여러 패키지를 선택하세요. 선택한 대화상자 또는 알림에서 확인하고 작업과 최근 기록을 볼 수 있습니다.
3. AutoJs6 연동에는 빌드 5299 (6.8.0) 이상을 사용하고 플러그인 센터에서 `3-Setup Installer`를 활성화하세요. 스크립트 API에는 빌드 5300 이상이 필요합니다. Dhizuku, 알림 설치 및 영구 기본 설치 프로그램의 스크립트 옵션에는 installer V2와 AutoJs6 6.8.0 빌드 5307 이상이 필요합니다. 기본 호스트 연동은 빌드 5299, V1 스크립트 메서드는 5300 이상을 계속 지원합니다.
4. AutoJs6의 설치 기능을 사용하거나 패키지를 열거나 공유할 때 3-Setup Installer를 선택하세요. 확인 대화상자가 나타나면 앱과 옵션을 검토한 후 설치하세요. 특권 방식을 선택할 때는 Shizuku, Root 또는 Dhizuku 권한을 준비하세요.
5. 홈 메뉴에서 설치된 앱과 설정으로 이동하세요. 로컬 설치 기본값, 외관, 아이콘 및 알림을 확인할 수 있으며 정보, 버전 기록 및 수동 업데이트 확인은 설정에 있습니다.

### 인증 방식

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku가 실행 중이어야 하며 (무선 디버깅, ADB 또는 Root로 시작), 3-Setup Installer에 별도로 권한을 부여해야 합니다. AutoJs6에 부여한 권한은 이 플러그인에 적용되지 않습니다. 설치, 제거 및 다른 사용자에 대한 작업은 실행 중인 Shizuku 서비스의 권한을 사용합니다.
- `root`: Root 권한을 얻은 기기와 3-Setup Installer에 `su`를 허용하는 Root 관리자가 필요합니다. libsu로 특권 설치, 제거, 사용자 및 기본 설치 프로그램 작업을 제공합니다. 각 작업의 허용 여부는 Android와 ROM 정책이 결정합니다.
- `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.
- **참고:** 스크립트는 기본적으로 `interaction: 'auto'`를 사용하며 특권이 있으면 확인 창 없이 설치합니다. 호스트 UI의 설치 기능은 `dialog`를 사용합니다. Android가 확인을 요구하면 `auto`는 이를 허용하고 `notes`에 기록합니다. 설치 전에 확인하려면 `interaction: 'dialog'`를 지정하세요. 명시적 `silent`는 특권이 없거나 시스템 확인이 필요하면 `AUTHORIZER_REQUIRED`로 실패합니다.
- 설정에 권한 방식 순서와 사용 여부, 설치 옵션 및 알림 환경설정을 저장합니다. 홈/외부 설치의 기본값은 `dialog`이며 `auto`, `silent`, `notification`을 명시할 수 있습니다. 호스트 UI는 `dialog`를 사용하고 스크립트는 명시한 옵션과 기본 `auto`를 유지합니다. 변경은 확인 후 저장됩니다.

### 호환성

- Android 7.0 (API 24) 이상. 기기 검증 현황과 남은 검증 범위는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다.
- 낮은 targetSdk 차단 우회는 Android 14 (API 34)부터 존재합니다. 더 오래된 시스템에서는 이 옵션이 무시되고 결과에 표시됩니다.
- 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- `dhizuku`: Android 8.0 (API 26)+, 활성 Dhizuku 기기/프로필 소유자 및 이 플러그인에 부여한 권한이 필요합니다. 현재 소유자 사용자에서만 작동하고 실제 소유자 패키지를 설치자로 기록합니다. shell/root용 다운그레이드, 테스트 패키지, 낮은 targetSdk 제한 우회, 다른 사용자, 임의 설치자 지정 및 제거 시 데이터 보존 옵션은 지원하지 않습니다. 플러그인은 소유자를 설정하지 않습니다.

### 자주 묻는 질문

- **왜 여전히 확인이 필요한가요?** `none`은 항상 Android 확인이 필요하고 특권 방식도 Android 정책의 영향을 받습니다. `notification`은 알림 동작으로만 시스템 확인을 열며 Android 확인을 우회하지 않습니다.
- **`.aab`를 설치할 수 있나요?** 아니요. Android App Bundle은 배포 형식이므로 먼저 bundletool로 `.apks` 세트로 변환하세요. 플러그인은 `.aab` 파일을 인식하고 패키지와 모듈 정보를 표시합니다.
- **`allowDowngrade: true`인데 다운그레이드가 실패하는 이유는?** 이 옵션은 다운그레이드를 요청할 뿐이며 Android가 펌웨어, 권한을 실행하는 주체 및 debuggable 여부에 따라 결정합니다. 검증한 user 펌웨어에서 Sony G8441 / API 28과 Xiaomi 23046RP50C / API 35는 debuggable이 아닌 패키지의 다운그레이드를 거부했지만 Sony XQ-DQ72 / API 33의 Root 경로는 허용했습니다. 이는 각 기기의 결과입니다. 오류와 `systemMessage`를 확인하세요. Root도 모든 ROM에서 성공을 보장하지 않습니다.
- **HyperOS에서 어떤 설치자 패키지 이름을 사용할 수 있나요?** ADB 또는 무선 디버깅으로 시작한 Shizuku에서는 설치자 패키지 이름을 지정하지 않으면 `com.android.shell`을 사용합니다. 검증한 Xiaomi 23046RP50C / HyperOS / API 35에서는 확인 창 없는 새 설치와 업데이트 모두 이 값이 기록되었습니다. `com.android.shell` 또는 플러그인 자체 패키지 이름을 명시한 경우에도 성공했으며 요청한 값이 기록되었습니다. 다른 패키지 이름이나 ROM 버전은 시스템 응답에 따라 달라집니다.
- **ColorOS 또는 다른 ROM에서 플러그인 활성화가 필요하다고 하면?** 새로 설치하거나 강제 종료한 후 Android는 사용자가 상호작용할 때까지 앱을 중지 상태로 유지할 수 있습니다. AutoJs6 플러그인 센터에 활성화 작업이 표시되면 실행하거나, 런처 아이콘으로 3-Setup Installer를 연 다음 다시 시도하세요. 이는 [Android의 중지 상태 규칙](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED)을 따릅니다. ColorOS 고유 동작은 아직 실기기에서 검증하지 않았습니다.
- **기본 설치 프로그램 변경이 실패하거나 저장된 영구 표시가 현재 처리 앱과 다른 이유는?** 기본 설치 프로그램 페이지는 일반 기본값과 영구 정책을 구분합니다. 일반 기본값은 Shizuku 또는 Root를 사용하며 ROM 제한이 적용됩니다. Dhizuku 영구 정책은 API 26-33을 지원하며 API 34+는 소유자 콜백을 검증할 수 없어 변경 전에 거부합니다. Root는 지원 기기의 사용자 0에서만 system UID 보조 프로세스를 사용합니다. 경쟁하는 영구 정책을 덮어쓰지 않습니다. `persistentConfigured`는 이전 설정 성공 기록이며 현재 시스템 정책의 증거가 아닙니다. 수동 상태 조회는 `preferred` 또는 `none`만 보고합니다.
- **원본이 삭제되지 않은 이유는?** 설치 성공 후에만 삭제를 시도합니다. 설치 실패, 취소 또는 시간 초과 시에는 원본을 항상 유지합니다. 삭제 실패는 설치 성공 결과를 바꾸지 않으며 외부 제공자가 삭제를 거부할 수 있습니다. 스크립트의 `deleteSource`는 호스트가 경로 또는 `file://` 원본을 삭제하고 `content://` 원본은 유지합니다. `sourceDeleted`와 `notes`를 확인하세요. 일괄 처리에서 다른 항목이 실패하거나 나머지 대기열이 취소되어도 성공이 확인된 항목에는 `deleteSource`가 적용됩니다.
- **다시 시도하거나 이어서 실행할 수 있나요?** 실패한 외부 URI는 원본과 접근 권한을 사용할 수 있는 동안 다시 시도할 수 있습니다. 원본 또는 접근 권한이 해제되면 패키지를 다시 여세요. 프로세스가 다시 시작되면 복원된 화면에 저장된 확정 결과가 표시되고 미완료 항목은 중단으로 표시됩니다. 복원된 화면은 읽기 전용이며 설치나 재시도를 자동으로 실행하지 않습니다. 다시 시작하기 전에 실제 설치 상태를 확인하세요.

### 권한과 보안

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6만 접근할 수 있습니다. 외부 "연결 프로그램" 진입점은 패키지 파일만 받아들이며 스크립트를 실행하지 않습니다.
- REQUEST_INSTALL_PACKAGES와 REQUEST_DELETE_PACKAGES는 Android 확인을 지원합니다. QUERY_ALL_PACKAGES는 설치된 앱 관리, 버전 및 서명 비교, 기본 설치 프로그램 감지에 사용됩니다.
- FOREGROUND_SERVICE와 FOREGROUND_SERVICE_DATA_SYNC는 설치 및 임시 원본 접근을 지원하고 POST_NOTIFICATIONS는 알림에 사용됩니다. `notification`은 알림과 설치 채널이 켜져 있어야 합니다. 다른 모드는 알림 권한이 없어도 실행됩니다.
- Shizuku, Root 및 Dhizuku는 요청한 작업에 사용됩니다. 플러그인은 기기/프로필 소유자를 설정하지 않습니다. 영구 규칙은 설정 또는 해제 요청으로만 변경되며 패키지를 업로드하지 않습니다.
- 설치, 검사, 기록 및 앱 관리는 오프라인으로 동작합니다. INTERNET은 사용자가 수동으로 버전을 확인할 때만 12시간 간격으로 플러그인의 고정 GitHub Releases API에 접근하는 데 사용됩니다. 백그라운드 업데이트 확인이나 패키지 업로드는 수행하지 않습니다.
- 패키지 원본은 읽기 전용으로 열립니다. 기록에는 제한된 앱 메타데이터와 결과만 저장되며 패키지 내용이나 원본 URI는 저장되지 않습니다. 오류의 경로는 가려집니다. 비공개 저장소는 백업에서 제외됩니다. 기록을 삭제해도 앱이나 원본은 삭제되지 않습니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요.
