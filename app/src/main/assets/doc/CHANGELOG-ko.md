******

### 릴리스 기록

******

# v1.0.0

###### 2026/10/01

* `힌트` P3 개발 미리보기. 확인, 진행률, 결과 및 일괄 대화상자, 외부 열기와 공유, 선택적 원본 삭제, 시스템 확인 및 포그라운드 알림이 구현되었습니다. 스크립트 API, 독립 홈과 설정, 설치 기록 및 기본 설치 프로그램 설정은 향후 계획입니다. 진행 상황과 기기 검증 범위는 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)를 참고하세요. AutoJs6 >= 6.8.0 (5299).
* `기능` 플러그인 식별자 `three-setup-installer` (engine `installer`), INFO 서비스, Wake Activity 및 호스트 검색용 `org.autojs.plugin.INSTALLER` 서비스 골격
* `기능` 10개 언어의 README, 플러그인 센터 안내 및 변경 기록
* `개선` 플러그인 ID, engine, 서비스 action / category, Binder descriptor 및 최소 호스트 버전을 호스트 installer-api 계약 상수에서 가져오도록 변경; 기능 선언에 설치기 계약 버전 1을 추가하고 최소 호스트 빌드를 5299로 갱신
* `개선` 임의 접근이 가능한 원본은 전체 캐시 복사를 생략하고, 스트림은 필요할 때 임시 저장합니다. ZIP 분할 패키지를 지원하며, AAB는 정보 확인만 허용하고 내용이 변경된 원본은 거부합니다.
* `개선` 명시적으로 선택한 권한 방식은 다른 방식으로 전환하지 않습니다. 거부, 시간 초과, 호환성 문제를 구분하며, 동시 요청은 권한 요청 처리와 특권 연결을 공유합니다.
* `개선` 설치 및 업데이트 핵심 기능이 시스템 확인, Shizuku, Root를 지원합니다. 취소할 수 있으며, 실제 확인 방식과 시스템 처리 결과를 반환합니다.
* `개선` 제거 핵심 기능이 시스템 확인, Shizuku, Root를 지원하며, 특권 권한을 사용할 때 데이터를 유지할 수 있습니다.
* `개선` 여러 패키지를 순서대로 설치하면서 실패 후 계속 진행하거나 남은 항목을 취소할 수 있습니다. 특권 권한으로 대상 사용자를 검증하고 선택할 수 있습니다.
* `개선` 호스트 서비스에서 패키지 정보 확인, 설치, 제거, 사용자 조회를 지원합니다. 명시적 확인, 호출자 종료 시 취소, 최대 4개 동시 세션과 자동 정리를 제공합니다.
* `개선` 설치 대화상자가 AutoJs6 언어, 야간 모드 및 테마 색상을 따르며, 호스트가 없을 때의 대체 표시와 큰 글꼴 및 RTL 레이아웃을 지원합니다.
* `개선` 백그라운드 설치에 포그라운드 실행과 진행률, 취소 및 결과 알림을 제공합니다. 알림 권한을 거부해도 설치를 차단하지 않습니다.
* `개선` 앱 정보, APK 구성요소 선택, 옵션, 오류 복사 및 일괄 항목 상태가 포함된 확인, 진행률, 결과 대화상자를 추가했습니다. 프로세스가 종료되면 중단을 표시하고 자동 재설치하지 않습니다.
* `개선` 시스템 설치 확인에 알 수 없는 출처 권한 안내와 중단 처리를 추가했습니다. 특권 제거는 확인 전에 앱 정보와 데이터 유지 선택을 표시합니다.
* `개선` 하나 또는 여러 패키지 열기와 공유, 접근 가능한 외부 원본 재시도, 성공 후 선택적 원본 삭제를 지원합니다. 삭제가 거부되어도 설치 성공 결과는 유지됩니다.
* `의존성` Shizuku 인증 방식을 위해 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 추가
* `의존성` Root 인증 방식을 위해 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 추가
* `의존성` 특권 서비스가 숨겨진 패키지 설치 API에 접근하도록 AndroidHiddenApiBypass 6.1 추가
* `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5298, MPL 2.0) 추가 및 `locks/host-api-aars.lock`에 해시 고정
* `의존성` `package-archive-parser.aar` 및 `installer-api.aar` (AutoJs6 모듈 `plugin-api/package-archive-parser` 및 `plugin-api/installer-api`, 호스트 P1 빌드 6.8.0 / 5299, MPL 2.0) 추가 및 `common-plugin-api.aar`와 함께 `locks/host-api-aars.lock`에 해시 고정
* `의존성` 일반 ZIP 분할 패키지를 인식하도록 내장 패키지 분석기 업데이트
