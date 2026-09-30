<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 及其腳本安裝, 更新和解除安裝 Android 應用程式, 支援透過 Shizuku 或 Root 靜默安裝</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Setup Installer 接管 AutoJs6 的安裝器: 檔案管理器, 外掛程式中心與腳本打包頁的安裝按鈕, `.apk`, `.apks`, `.xapk`, `.apkm` 與 `.apkz` 檔案的外部 "開啟方式" 入口, 以及腳本端用於安裝, 更新, 檢查與解除安裝應用程式的全域物件 `installer`. 除一般的系統確認外, 還可透過 Shizuku 或 Root 靜默安裝與解除安裝.

AutoJs6 透過 Binder 服務發現外掛程式, 以唯讀檔案描述元交出安裝套件; 外掛程式解析安裝套件, 選擇授權方式, 視需要顯示自己的確認與進度對話方塊, 並回報階段, 進度與結果. 特權操作在 Shizuku 使用者服務或 libsu Root 服務中執行, 直接與系統套件安裝器對話.

******

### 目前狀態

******

版本 1.0.0 為 P0 開發預覽: 儲存庫骨架, 可被 AutoJs6 外掛程式中心識別的外掛程式身分, 以及特權安裝 spike. Binder 契約, 安裝引擎, 對話方塊, 腳本 API 與設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 的階段推進. 需要 AutoJs6 6.8.0 (build 5298) 或更高版本. P0 已驗證 Shizuku 和 Root 靜默安裝, 更新, 解除安裝及一般預設安裝器設定. 宿主與腳本安裝入口尚未開放, 本版本仍不支援持久預設項.

******

### 功能

******

外掛程式提供以下能力:

- 安裝套件格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 壓縮檔; 分包按裝置選擇; `.aab` 檔案只識別與說明, 不安裝.
- 授權方式: `none` (系統 PackageInstaller 工作階段 + 使用者確認), `shizuku` 與 `root`; `auto` 按設定頁中的順序選擇第一個可用者, 腳本也可明確指定.
- 安裝選項: 批次安裝, 成功後刪除來源檔案, 允許降級, 允許測試套件, 略過低 targetSdk 封鎖 (Android 14+), 安裝者套件名稱與目標使用者 (僅特權授權方式).
- 透過 Shizuku 或 Root 靜默解除安裝並可選擇保留資料; 其他情況使用一般系統對話方塊.
- 設為預設安裝器: 有 Shizuku 或 Root 時外掛程式成為安裝套件檔案的偏好處理者; 無特權時為你開啟系統的 "預設開啟" 頁面.
- 腳本 API `installer` (別名 `$installer`) 提供同步, `...Async` 與工作階段三種形態; 每個失敗都是帶穩定 `code` 的 `InstallerError`.

******

### 使用方式

******

1. 在安裝了 AutoJs6 建置 5298 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `3-Setup Installer` 已被識別並啟用它.
3. 在 AutoJs6 檔案管理器中點選安裝套件, 在任意檔案管理器中用 3-Setup Installer 開啟安裝套件, 或在腳本中呼叫 `installer.install(...)`. 需要靜默安裝時, 依外掛程式提示啟動 Shizuku 或授予 Root, 或在外掛程式設定中選擇授權方式.

******

### 授權方式

******

各授權方式能做什麼以及需要什麼:

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 應用程式處於執行狀態 (經無線偵錯, ADB 或 Root 啟動) 並已向外掛程式授權; 以 shell 權限執行, 可靜默安裝, 靜默解除安裝, 為其他使用者安裝以及鎖定預設安裝器.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.

******

### 快速入門

******

一個靜默安裝, 允許降級地更新, 監聽工作階段並解除安裝應用程式的腳本 (自路線圖 P4 起可用):

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

### 相容性

******

決定外掛程式能力邊界的平台事實:

- Android 7.0 (API 24) 及以上; 宿主建置與外掛程式在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 列出的裝置矩陣上一起驗證.
- 略過低 targetSdk 封鎖自 Android 14 (API 34) 起存在; 更早的系統忽略該選項並在結果中註明.
- 部分 OEM 系統限制哪個應用程式可以成為預設安裝器, 或要求其信任的安裝者套件名稱 (HyperOS 接受 `com.android.shell`); 外掛程式按原樣回報系統的答覆.

******

### 常見問題

******

- **為什麼安裝仍然要求確認?** `none` 授權方式始終經過系統確認. 啟動 Shizuku 或授予 Root, 然後在設定中選擇該授權方式, 或在腳本中傳入 `authorizer: 'shizuku'`.
- **能安裝 `.aab` 嗎?** 不能. Android App Bundle 是發佈格式, 請先用 bundletool 轉換為 `.apks` 集合. 外掛程式會識別 `.aab` 檔案並顯示其套件名稱與模組資訊.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 與 REQUEST_DELETE_PACKAGES 支撐一般的安裝與解除安裝對話方塊; QUERY_ALL_PACKAGES 讓外掛程式在更新前顯示已安裝版本並比對簽章.
- Shizuku 與 Root 只用於你發起的操作; 特權服務不保存狀態, 操作之間不保持開啟的 shell, 也不會被外掛程式之外的任何一方存取.
- 安裝套件以唯讀方式開啟; 外掛程式不發起網路請求, 不收集資料, 並將私有儲存空間排除在備份之外.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼探索外掛並協商相容性:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5298 (6.8.0)
```

`ThreeSetupInstallerPluginService` 回應 `org.autojs.plugin.INSTALLER` (category `installer`), 自路線圖 P1 起實作宿主 installer-api 契約 `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟用外掛程式.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/30_

- `提示` P0 開發預覽: 儲存庫骨架, 可被 AutoJs6 外掛程式中心識別的外掛程式身分, 以及特權安裝 spike. Binder 契約, 安裝引擎, 對話方塊, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
- `新增` 外掛程式標識 `three-setup-installer` (engine `installer`), 含 INFO 服務, Wake Activity 以及供宿主發現的 `org.autojs.plugin.INSTALLER` 服務骨架
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
- `優化` P0 已驗證 Shizuku 和 Root 靜默安裝, 更新, 解除安裝及一般預設安裝器設定. 宿主與腳本安裝入口尚未開放, 本版本仍不支援持久預設項.
- `相依性` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用於 Shizuku 授權方式
- `相依性` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用於 Root 授權方式
- `相依性` 附加 AndroidHiddenApiBypass 6.1 用於特權服務存取隱藏的套件安裝器 API
- `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主建置 6.8.0 / 5298, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置與驗證

******

本節面向希望從原始碼建置外掛的開發者; 一般使用者直接安裝 Releases 頁面的預建 APK 即可.

建置 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並建置 instrumentation 測試 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集發行產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與產生的產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更新版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 在地化與文件產生

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 產生的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部產生的產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- 安裝器模組文件: https://docs.autojs6.com/#/installer
- InstallerX 與 InstallerX Revived (架構參考, GPL-3.0, 未重用程式碼): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
