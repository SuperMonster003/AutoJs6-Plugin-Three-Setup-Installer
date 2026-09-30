******

### 릴리스 기록

******

# v1.0.0

###### 2026/09/30

* `힌트` P0 개발 미리보기: 저장소 골격, AutoJs6 플러그인 센터가 인식하는 플러그인 신원, 특권 설치 검증 (spike). Binder 계약, 설치 엔진, 대화 상자, 스크립트 API, 설정 화면은 ROADMAP.md의 단계에 따라 진행됩니다.
* `기능` 플러그인 식별자 `three-setup-installer` (engine `installer`), INFO 서비스, Wake Activity 및 호스트 검색용 `org.autojs.plugin.INSTALLER` 서비스 골격
* `기능` 10개 언어의 README, 플러그인 센터 안내 및 변경 기록
* `개선` P0에서 Shizuku와 Root를 통한 자동 설치, 업데이트, 제거 및 일반 기본 설치 프로그램 설정을 검증했습니다. 호스트와 스크립트의 설치 진입점은 아직 제공되지 않으며, 영구 기본 설정은 이번 버전에서 지원하지 않습니다.
* `의존성` Shizuku 인증 방식을 위해 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 추가
* `의존성` Root 인증 방식을 위해 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 추가
* `의존성` 특권 서비스가 숨겨진 패키지 설치 API에 접근하도록 AndroidHiddenApiBypass 6.1 추가
* `의존성` 공유 플러그인 계약으로 `common-plugin-api.aar` (AutoJs6 모듈 `plugin-api/common-plugin-api`, 호스트 빌드 6.8.0 / 5298, MPL 2.0) 추가 및 `locks/host-api-aars.lock`에 해시 고정
