<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>透過系統確認, Shizuku 或 Root 安裝, 更新與解除安裝 Android 應用程式</p>

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
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
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

3-Setup Installer 透過 AutoJs6 的安裝入口及安裝套件檔案的外部開啟或分享請求, 安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援一般 Android 確認以及透過 Shizuku 或 Root 進行特權安裝. 腳本 API, 獨立首頁與設定頁仍屬後續規劃.

AutoJs6 透過 Binder 服務發現外掛程式, 以唯讀檔案描述元交出安裝套件; 外掛程式解析安裝套件, 選擇授權方式, 視需要顯示自己的確認與進度對話方塊, 並回報階段, 進度與結果. 特權操作在 Shizuku 使用者服務或 libsu Root 服務中執行, 直接與系統套件安裝器對話.

******

### 目前狀態

******

1.0.0: P3 開發預覽. 已實作確認, 進度, 結果與批量對話框, 外部開啟與分享, 可選來源刪除, 系統確認及前景通知. 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試. 腳本 API, 獨立首頁與設定, 安裝歷史及預設安裝器設定仍屬後續規劃. 進度與裝置涵蓋範圍見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### 功能

******

本開發預覽已提供的能力, 後續功能另行標明:

- 安裝套件格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 壓縮檔; 分包按裝置選擇; `.aab` 檔案只識別與說明, 不安裝.
- 授權方式: `none` 使用 Android 確認; `shizuku` 與 `root` 提供特權操作. `auto` 依序選擇可用的 Shizuku, Root 和系統確認. 安裝對話框可選擇授權方式.
- 安裝成功後可選擇盡力刪除來源. 降級, 測試套件, 繞過低 targetSdk 限制 (Android 14+), 安裝者歸屬及其他目標使用者需要 Shizuku 或 Root, 並仍受 Android 規則限制.
- 透過 Shizuku 或 Root 靜默卸載並可選擇保留資料; 其他情況使用一般系統對話方塊.
- 確認介面顯示應用程式資訊, 新舊版本, 簽章和可勾選的 APK 分包; 進度可取消, 結果顯示成功操作或可複製的錯誤詳情. 批量安裝逐項顯示狀態.
- 可用插件開啟安裝套件, 或分享單個及多個安裝套件. 外部來源安裝失敗後, 在 URI 及其存取權限仍可用時可重試對應項目.
- 提供前景安裝進度, 取消操作及結果通知. 未授予通知權限不會阻止安裝.
- 對話框預設跟隨 AutoJs6 的語言, 夜間模式與主題色; 宿主無法使用時退回系統語言和夜間模式及約定預設顏色.
- 後續規劃: 預設安裝器設定, 包括透過特權選擇處理程式, 以及必要時引導系統預設設定.
- P4 規劃: 腳本 API `installer` (別名 `$installer`) 提供同步, `...Async` 與工作階段形態, 失敗以帶穩定 `code` 的 `InstallerError` 表示.
- P5 規劃: 獨立首頁與設定頁, 安裝歷史及已安裝應用程式管理.

******

### 使用方法

******

1. 在安裝了 AutoJs6 建置 5299 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `3-Setup Installer` 已被識別並啟用它.
3. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku 或 Root 授權.

******

### 授權方式

******

各授權方式能做什麼以及需要什麼:

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動) 並已向插件授權. 其 shell 權限支援靜默安裝, 靜默解除安裝及面向其他使用者的操作.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.
- **注意:** 特權可用時, 宿主請求使用 `interaction: 'auto'` 預設靜默安裝, 不會主動開啟確認介面. 若 Android 仍要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時使用 `interaction: 'dialog'`; 禁止系統確認時使用 `interaction: 'silent'`, 此時需要確認的安裝會失敗. 後續腳本 API 沿用相同預設語義.

******

### 快速開始

******

一個靜默安裝, 允許降級地更新, 監聽工作階段並卸載應用程式的腳本 (自路線圖 P4 起可用):

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

- Android 7.0 (API 24) 及以上. 裝置驗證情況和剩餘涵蓋範圍記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 略過低 targetSdk 封鎖自 Android 14 (API 34) 起存在; 更早的系統忽略該選項並在結果中註明.
- 部分 OEM 系統限制哪個應用程式可以成為預設安裝器, 或要求其信任的安裝者套件名稱 (HyperOS 接受 `com.android.shell`); 外掛程式按原樣回報系統的答覆.

******

### 常見問題

******

- **為何安裝仍要求確認?** `none` 始終使用系統確認. 準備好授權後, 可在安裝對話框中選擇 Shizuku 或 Root. Android 或裝置原則仍可能要求系統確認.
- **能安裝 `.aab` 嗎?** 不能. Android App Bundle 是發佈格式, 請先用 bundletool 轉換為 `.apks` 集合. 外掛程式會識別 `.aab` 檔案並顯示其套件名稱與模組資訊.
- **為何來源沒有刪除?** 僅在安裝成功後嘗試刪除, 來源提供方可能拒絕. 這不會改變安裝成功的結果. 當來源由 AutoJs6 或其他傳送應用程式擁有時, 刪除由該來源應用程式負責.
- **可以重試或恢復嗎?** 失敗的外部 URI 項目在來源及存取權限仍可用時可以重試. 來源或存取權限釋放後, 請重新開啟安裝套件. 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試. 再次開始前請檢查應用程式的實際安裝狀態.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 與 REQUEST_DELETE_PACKAGES 支撐一般的安裝與卸載對話方塊; QUERY_ALL_PACKAGES 讓外掛程式在更新前顯示已安裝版本並比對簽章.
- FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_DATA_SYNC 支援背景安裝工作; POST_NOTIFICATIONS 用於進度與結果通知. 缺少通知權限不會阻止安裝.
- Shizuku 與 Root 只用於你發起的操作; 特權服務不保存狀態, 操作之間不保持開啟的 shell, 也不會被外掛程式之外的任何一方存取.
- 安裝套件以唯讀方式開啟; 外掛程式不發起網路請求, 不收集資料, 並將私有儲存空間排除在備份之外.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼發現外掛並協商相容性:

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

`ThreeSetupInstallerPluginService` 回應 `org.autojs.plugin.INSTALLER` (category `installer`), 自路線圖 P1 起實作宿主 installer-api 契約 `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟用外掛程式.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/10/01_

- `提示` P3 開發預覽. 已實作確認, 進度, 結果與批量對話框, 外部開啟與分享, 可選來源刪除, 系統確認及前景通知. 腳本 API, 獨立首頁與設定, 安裝歷史及預設安裝器設定仍屬後續規劃. 進度與裝置涵蓋範圍見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).
- `新增` 外掛程式標識 `three-setup-installer` (engine `installer`), 含 INFO 服務, Wake Activity 以及供宿主發現的 `org.autojs.plugin.INSTALLER` 服務骨架
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
- `優化` 插件 ID, engine, 服務 action / category, Binder descriptor 與最低宿主版本改由宿主 installer-api 契約常量提供; 能力聲明加入安裝器契約版本 1, 最低宿主建置回填為 5299
- `優化` 可隨機存取的來源避免完整快取副本, 串流來源按需暫存. 支援一般 ZIP 分包, AAB 僅供檢查, 拒絕內容發生變化的來源.
- `優化` 明確選擇的授權方式不回退, 區分拒絕, 逾時與不相容, 並行請求共用授權過程與特權連線.
- `優化` 安裝與更新核心支援系統確認, Shizuku 和 Root, 可取消操作並傳回實際確認方式與系統處理結果.
- `優化` 解除安裝核心支援系統確認, Shizuku 和 Root, 特權解除安裝可選擇保留應用程式資料.
- `優化` 支援依序批次安裝, 失敗後繼續或取消剩餘項目, 並可透過特權方式驗證和選擇目標使用者.
- `優化` 主程式服務接入套件資訊查詢, 安裝, 解除安裝和使用者查詢, 支援明確確認, 呼叫方退出時取消, 最多四個並行工作階段及自動清理.
- `優化` 安裝對話框預設跟隨 AutoJs6 的語言, 夜間模式與主題色, 支援宿主無法使用時退回, 大字體和 RTL 版面.
- `優化` 背景安裝支援前景服務, 進度, 取消和結果通知; 未授予通知權限不會阻止安裝.
- `優化` 新增安裝確認, 進度與結果介面, 包含應用程式資訊, APK 分包選擇, 安裝選項, 錯誤複製和批量逐項狀態; 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試.
- `優化` 系統安裝確認支援未知來源權限引導和中斷處理; 特權解除安裝在確認前顯示應用程式資訊與保留資料選項.
- `優化` 支援外部開啟和分享單個或多個安裝套件, 在來源存取仍有效時重試失敗項目, 並可在成功後盡力刪除來源; 刪除遭拒不會改變安裝成功的結果.
- `依賴` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用於 Shizuku 授權方式
- `依賴` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用於 Root 授權方式
- `依賴` 附加 AndroidHiddenApiBypass 6.1 用於特權服務存取隱藏的套件安裝器 API
- `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主建置 6.8.0 / 5298, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `依賴` 附加 `package-archive-parser.aar` 與 `installer-api.aar` (AutoJs6 模組 `plugin-api/package-archive-parser` 與 `plugin-api/installer-api`, 宿主 P1 建置 6.8.0 / 5299, MPL 2.0), 與 `common-plugin-api.aar` 一同在 `locks/host-api-aars.lock` 中鎖定雜湊
- `依賴` 升級共用安裝包解析器, 支援普通 ZIP 分包容器

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

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

收集發佈產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與生成產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 本地化與文件生成

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部生成產物.

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
