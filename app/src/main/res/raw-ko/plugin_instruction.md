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

### 권한과 보안

- Binder 진입점은 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6만 접근할 수 있습니다. 외부 "연결 프로그램" 진입점은 패키지 파일만 받아들이며 스크립트를 실행하지 않습니다.
- REQUEST_INSTALL_PACKAGES와 REQUEST_DELETE_PACKAGES는 Android 확인을 지원합니다. QUERY_ALL_PACKAGES는 설치된 앱 관리, 버전 및 서명 비교, 기본 설치 프로그램 감지에 사용됩니다.
- FOREGROUND_SERVICE와 FOREGROUND_SERVICE_DATA_SYNC는 백그라운드 설치 작업을 지원하며 POST_NOTIFICATIONS는 진행률 및 결과 알림에 사용됩니다. 알림 권한이 없어도 설치를 차단하지 않습니다.
- Shizuku와 Root는 사용자가 시작한 작업에만 사용됩니다. 특권 서비스는 상태를 보관하지 않고 작업 사이에 shell을 열어 두지 않으며 플러그인 외부에서 접근할 수 없습니다.
- 설치, 검사, 기록 및 앱 관리는 오프라인으로 동작합니다. INTERNET은 사용자가 수동으로 버전을 확인할 때만 12시간 간격으로 플러그인의 고정 GitHub Releases API에 접근하는 데 사용됩니다. 백그라운드 업데이트 확인이나 패키지 업로드는 수행하지 않습니다.
- 패키지 원본은 읽기 전용으로 열립니다. 기록에는 제한된 앱 메타데이터와 결과만 저장되며 패키지 내용이나 원본 URI는 저장되지 않습니다. 오류의 경로는 가려집니다. 비공개 저장소는 백업에서 제외됩니다. 기록을 삭제해도 앱이나 원본은 삭제되지 않습니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요.
