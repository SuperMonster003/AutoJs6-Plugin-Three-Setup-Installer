3-Setup Installer는 AutoJs6 설치 기능과 외부 패키지 열기 또는 공유 요청을 통해 Android 앱을 설치, 업데이트, 검사 및 제거합니다. 일반 Android 확인과 Shizuku 또는 Root를 통한 권한 설치를 지원합니다. 호환되는 호스트 빌드에서 `installer` 스크립트 API를 제공합니다. 독립 홈 및 설정 페이지는 향후 계획입니다.

1.0.0: 개발 미리보기. 확인, 진행률, 결과 및 일괄 대화상자, 외부 열기와 공유, 선택적 원본 삭제, 시스템 확인 및 포그라운드 알림이 구현되었습니다. 프로세스가 다시 시작되면 복원된 화면에 저장된 확정 결과가 표시되고 미완료 항목은 중단으로 표시됩니다. 복원된 화면은 읽기 전용이며 설치나 재시도를 자동으로 실행하지 않습니다. `installer` 스크립트 API에는 AutoJs6 >= 6.8.0 (5300)이 필요합니다. 독립 홈과 설정, 설치 기록 및 기본 설치 프로그램 설정 화면은 향후 계획입니다. 진행 상황과 기기 검증 범위는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요. 플러그인 기본 호환성: AutoJs6 >= 6.8.0 (5299).

### 사용 방법

1. AutoJs6 build 5299 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `3-Setup Installer`가 인식되는지 확인하고 활성화합니다.
3. AutoJs6의 설치 기능을 사용하거나 패키지를 열거나 공유할 때 3-Setup Installer를 선택하세요. 확인 대화상자가 나타나면 앱과 옵션을 검토한 후 설치하세요. 특권 방식을 선택할 때는 Shizuku 또는 Root 권한을 준비하세요.

### 인증 방식

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku가 실행 중이어야 하며 (무선 디버깅, ADB 또는 Root로 시작), 플러그인에 권한이 부여되어야 합니다. shell 권한으로 자동 설치, 자동 제거 및 다른 사용자에 대한 작업을 수행할 수 있습니다.
- `root`: Root 관리자가 플러그인에 `su`를 허용해야 합니다. libsu Root 서비스를 통해 Shizuku와 같은 작업을 제공합니다. 일반 (user) 펌웨어에서 다운그레이드는 debuggable 앱에만 성공하며 이는 프레임워크 규칙이지 플러그인의 제한이 아닙니다.
- **참고:** 특권을 사용할 수 있을 때 `interaction: 'auto'` 호스트 요청은 확인 화면을 먼저 열지 않고 자동으로 설치합니다. Android가 확인을 요구하면 `auto`는 이를 허용하고 `notes`에 기록합니다. 설치 전 확인에는 `interaction: 'dialog'`를, 시스템 확인이 필요한 경우 실패하게 하려면 `interaction: 'silent'`를 사용하세요. 스크립트 API에도 같은 기본 동작이 적용됩니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요.
