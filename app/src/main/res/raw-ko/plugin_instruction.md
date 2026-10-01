3-Setup Installer는 독립 홈 화면, AutoJs6의 진입점과 스크립트, 외부 앱의 패키지 열기 및 공유 요청을 통해 Android 앱을 설치, 업데이트, 검사 및 제거합니다. Android 확인과 Shizuku 또는 Root를 통한 특권 작업을 지원합니다.

1.0.0: 독립 홈, 설정, 설치된 앱 관리, 순차 대기열 및 설치 기록을 제공하는 개발 미리보기입니다. 설치 확인, 진행 상황, 결과 및 포그라운드 알림을 지원합니다. 프로세스가 다시 시작되면 저장된 확정 결과를 유지하고 미완료 작업을 취소됨으로 표시하며, 자동으로 재개하거나 재시도하지 않습니다. `installer` 스크립트 API에는 AutoJs6 >= 6.8.0 (5300)이 필요하고, 기본 호스트 연동에는 빌드 5299이 필요합니다. 기기 검증 범위와 남은 검증 항목은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 확인하세요.

### 사용 방법

1. Android 7.0 이상에서 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 페이지의 플러그인 APK를 설치하세요. 독립 런처 진입점에서 홈 화면을 열 수 있습니다.
2. 홈에서 권한과 기본 설치 프로그램을 확인하고 추가 버튼으로 하나 또는 여러 패키지를 선택하세요. 설치 대화상자를 검토한 후 확인하고 홈에서 진행 상황과 최근 기록을 확인하세요.
3. AutoJs6 연동에는 빌드 5299 (6.8.0) 이상을 사용하고 플러그인 센터에서 `3-Setup Installer`를 활성화하세요. 스크립트 API에는 빌드 5300 이상이 필요합니다.
4. AutoJs6의 설치 기능을 사용하거나 패키지를 열거나 공유할 때 3-Setup Installer를 선택하세요. 확인 대화상자가 나타나면 앱과 옵션을 검토한 후 설치하세요. 특권 방식을 선택할 때는 Shizuku 또는 Root 권한을 준비하세요.
5. 홈 메뉴에서 설치된 앱과 설정으로 이동하세요. 로컬 설치 기본값, 외관, 아이콘 및 알림을 확인할 수 있으며 정보, 버전 기록 및 수동 업데이트 확인은 설정에 있습니다.

### 인증 방식

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku가 실행 중이어야 하며 (무선 디버깅, ADB 또는 Root로 시작), 플러그인에 권한이 부여되어야 합니다. shell 권한으로 자동 설치, 자동 제거 및 다른 사용자에 대한 작업을 수행할 수 있습니다.
- `root`: Root 관리자가 플러그인에 `su`를 허용해야 합니다. libsu Root 서비스를 통해 Shizuku와 같은 작업을 제공합니다. 일반 (user) 펌웨어에서 다운그레이드는 debuggable 앱에만 성공하며 이는 프레임워크 규칙이지 플러그인의 제한이 아닙니다.
- **참고:** 특권을 사용할 수 있을 때 `interaction: 'auto'` 호스트 요청은 확인 화면을 먼저 열지 않고 자동으로 설치합니다. Android가 확인을 요구하면 `auto`는 이를 허용하고 `notes`에 기록합니다. 설치 전 확인에는 `interaction: 'dialog'`를, 시스템 확인이 필요한 경우 실패하게 하려면 `interaction: 'silent'`를 사용하세요. 스크립트 API에도 같은 기본 동작이 적용됩니다.
- 설정은 권한 방식의 순서와 사용 여부, 설치 옵션 및 진행 알림 환경설정을 저장합니다. 홈 및 외부 설치는 기본적으로 `dialog`를 사용하며 명시적으로 저장한 `auto` 또는 `silent` 선택이 적용됩니다. 호스트/스크립트 요청은 명시적 옵션을 유지하고 스크립트 API의 기본값은 계속 `auto`입니다. 선택 내용은 확인한 뒤에만 저장됩니다.

### 호환성

- Android 7.0 (API 24) 이상. 기기 검증 현황과 남은 검증 범위는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)에 기록되어 있습니다.
- 낮은 targetSdk 차단 우회는 Android 14 (API 34)부터 존재합니다. 더 오래된 시스템에서는 이 옵션이 무시되고 결과에 표시됩니다.
- ROM 정책과 기존 기본 설정으로 인해 기본 설치 프로그램 변경이 제한될 수 있습니다. 1.0.0은 영구 고정을 보장하지 않습니다. 설치자 패키지 이름과 플러그인 활성화에 관한 FAQ를 확인하세요.

### 자주 묻는 질문

- **왜 여전히 설치 확인이 필요한가요?** `none`은 항상 시스템 확인을 사용합니다. 권한을 준비한 후 설치 대화상자에서 Shizuku 또는 Root를 선택하세요. Android 또는 기기 정책에 따라 시스템 확인이 필요할 수 있습니다.
- **`.aab`를 설치할 수 있나요?** 아니요. Android App Bundle은 배포 형식이므로 먼저 bundletool로 `.apks` 세트로 변환하세요. 플러그인은 `.aab` 파일을 인식하고 패키지와 모듈 정보를 표시합니다.
- **HyperOS에서 어떤 설치자 패키지 이름을 사용할 수 있나요?** ADB 또는 무선 디버깅으로 시작한 Shizuku에서는 설치자 패키지 이름을 지정하지 않으면 `com.android.shell`을 사용합니다. 검증한 Xiaomi 23046RP50C / HyperOS / API 35에서는 확인 창 없는 새 설치와 업데이트 모두 이 값이 기록되었습니다. `com.android.shell` 또는 플러그인 자체 패키지 이름을 명시한 경우에도 성공했으며 요청한 값이 기록되었습니다. 다른 패키지 이름이나 ROM 버전은 시스템 응답에 따라 달라집니다.
- **ColorOS 또는 다른 ROM에서 플러그인 활성화가 필요하다고 하면?** 새로 설치하거나 강제 종료한 후 Android는 사용자가 상호작용할 때까지 앱을 중지 상태로 유지할 수 있습니다. AutoJs6 플러그인 센터에 활성화 작업이 표시되면 실행하거나, 런처 아이콘으로 3-Setup Installer를 연 다음 다시 시도하세요. 이는 [Android의 중지 상태 규칙](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED)을 따릅니다. ColorOS 고유 동작은 아직 실기기에서 검증하지 않았습니다.
- **기본 설치 프로그램 설정이 실패하는 이유는?** ROM이 변경을 거부할 수 있습니다. 이전 Android 버전에서 기존 APK 기본 처리 앱이 있다면, 페이지 안내에 따라 시스템 설정에서 이전 앱의 기본값을 먼저 지워야 할 수 있습니다. 시스템에 기본값 삭제 기능이 없으면 교체를 보장할 수 없습니다. Shizuku 또는 Root를 사용할 수 있어도 1.0.0은 영구 고정을 보장하지 않습니다.
- **원본이 삭제되지 않은 이유는?** 설치 성공 후에만 삭제를 시도합니다. 설치 실패, 취소 또는 시간 초과 시에는 원본을 항상 유지합니다. 삭제 실패는 설치 성공 결과를 바꾸지 않으며 외부 제공자가 삭제를 거부할 수 있습니다. 스크립트의 `deleteSource`는 호스트가 경로 또는 `file://` 원본을 삭제하고 `content://` 원본은 유지합니다. `sourceDeleted`와 `notes`를 확인하세요.
- **다시 시도하거나 이어서 실행할 수 있나요?** 실패한 외부 URI는 원본과 접근 권한을 사용할 수 있는 동안 다시 시도할 수 있습니다. 원본 또는 접근 권한이 해제되면 패키지를 다시 여세요. 프로세스가 다시 시작되면 복원된 화면에 저장된 확정 결과가 표시되고 미완료 항목은 중단으로 표시됩니다. 복원된 화면은 읽기 전용이며 설치나 재시도를 자동으로 실행하지 않습니다. 다시 시작하기 전에 실제 설치 상태를 확인하세요.

### 권한과 보안

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6만 접근할 수 있습니다. 외부 "연결 프로그램" 진입점은 패키지 파일만 받아들이며 스크립트를 실행하지 않습니다.
- REQUEST_INSTALL_PACKAGES와 REQUEST_DELETE_PACKAGES는 Android 확인을 지원합니다. QUERY_ALL_PACKAGES는 설치된 앱 관리, 버전 및 서명 비교, 기본 설치 프로그램 감지에 사용됩니다.
- FOREGROUND_SERVICE와 FOREGROUND_SERVICE_DATA_SYNC는 백그라운드 설치 작업을 지원하며 POST_NOTIFICATIONS는 진행률 및 결과 알림에 사용됩니다. 알림 권한이 없어도 설치를 차단하지 않습니다.
- Shizuku와 Root는 사용자가 시작한 작업에만 사용됩니다. 특권 서비스는 상태를 보관하지 않고 작업 사이에 shell을 열어 두지 않으며 플러그인 외부에서 접근할 수 없습니다.
- 설치, 검사, 기록 및 앱 관리는 오프라인으로 동작합니다. INTERNET은 사용자가 수동으로 버전을 확인할 때만 12시간 간격으로 플러그인의 고정 GitHub Releases API에 접근하는 데 사용됩니다. 백그라운드 업데이트 확인이나 패키지 업로드는 수행하지 않습니다.
- 패키지 원본은 읽기 전용으로 열립니다. 기록에는 제한된 앱 메타데이터와 결과만 저장되며 패키지 내용이나 원본 URI는 저장되지 않습니다. 오류의 경로는 가려집니다. 비공개 저장소는 백업에서 제외됩니다. 기록을 삭제해도 앱이나 원본은 삭제되지 않습니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요.
