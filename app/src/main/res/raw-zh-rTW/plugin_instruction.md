3-Setup Installer 透過 AutoJs6 的安裝入口及安裝套件檔案的外部開啟或分享請求, 安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援一般 Android 確認以及透過 Shizuku 或 Root 進行特權安裝. 指令碼 API, 獨立首頁與設定頁仍屬後續規劃.

1.0.0: P3 開發預覽. 已實作確認, 進度, 結果與批次對話框, 外部開啟與分享, 可選來源刪除, 系統確認及前景通知. 指令碼 API, 獨立首頁與設定, 安裝歷程及預設安裝器設定仍屬後續規劃. 進度與裝置涵蓋範圍見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### 使用方式

1. 在安裝了 AutoJs6 建置 5299 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `3-Setup Installer` 已被識別並啟用它.
3. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku 或 Root 授權.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動) 並已向插件授權. 其 shell 權限支援靜默安裝, 靜默解除安裝及面向其他使用者的操作.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.
- **注意:** 特權可用時, 宿主請求使用 `interaction: 'auto'` 預設靜默安裝, 不會主動開啟確認介面. 若 Android 仍要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時使用 `interaction: 'dialog'`; 禁止系統確認時使用 `interaction: 'silent'`, 此時需要確認的安裝會失敗. 後續指令碼 API 沿用相同預設語義.

安裝指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
