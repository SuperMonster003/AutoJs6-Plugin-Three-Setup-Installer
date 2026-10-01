3-Setup Installer는 AutoJs6의 패키지 설치 프로그램을 대신합니다: 파일 관리자, 플러그인 센터, 스크립트 패키징 화면의 설치 버튼, `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 파일의 외부 "연결 프로그램" 진입점, 그리고 앱을 설치, 업데이트, 검사, 제거하는 스크립트 측 전역 객체 `installer`입니다. 일반적인 시스템 확인 외에도 Shizuku 또는 Root를 통해 무음으로 설치하고 제거할 수 있습니다.

1.0.0: P2 개발 미리 보기: 설치, 패키지 정보 및 사용자 조회, 제거 핵심 기능을 호스트 서비스에 연결하고 명시적 확인과 세션 자동 정리를 지원합니다. 전체 호스트 진입점 검증, 완전한 화면, 외부 열기, 기본 설치 관리자 활성화, 스크립트 API 및 설정은 개발 중입니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### 사용 방법

1. AutoJs6 build 5299 (6.8.0) 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `3-Setup Installer`가 인식되는지 확인하고 활성화합니다.
3. AutoJs6 파일 관리자에서 패키지 파일을 탭하거나, 아무 파일 관리자에서 3-Setup Installer로 패키지를 열거나, 스크립트에서 `installer.install(...)`을 호출합니다. 무음 설치가 필요하면 플러그인 안내에 따라 Shizuku를 시작하거나 Root를 허용하거나 플러그인 설정에서 인증 방식을 선택합니다.

### 인증 방식

- `none`: 표준 PackageInstaller 세션. Android가 매번 사용자 확인을 요구하며 분할 패키지를 지원하고 특권 옵션은 사용할 수 없습니다.
- `shizuku`: Shizuku 앱이 실행 중이고 (무선 디버깅, ADB 또는 Root로 시작) 플러그인에 권한이 부여되어야 합니다. shell 권한으로 동작하여 무음 설치, 무음 제거, 다른 사용자에 설치, 기본 설치 프로그램 잠금이 가능합니다.
- `root`: Root 관리자가 플러그인에 `su`를 허용해야 합니다. libsu Root 서비스를 통해 Shizuku와 같은 작업을 제공합니다. 일반 (user) 펌웨어에서 다운그레이드는 debuggable 앱에만 성공하며 이는 프레임워크 규칙이지 플러그인의 제한이 아닙니다.
- **주의:** 권한이 있으면 스크립트 API는 기본적으로 무인 설치를 수행하며 확인 대화 상자를 먼저 표시하지 않습니다. Android가 확인을 요구하면 `interaction: 'auto'`는 시스템 확인을 허용하고 `notes`에 기록합니다. 설치 전 확인이 필요하면 `interaction: 'dialog'`를 명시하세요. `interaction: 'silent'`는 시스템 확인을 표시하지 않으며, 확인이 필요하면 실패합니다.

설치 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요.
