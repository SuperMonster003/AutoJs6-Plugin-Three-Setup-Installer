3-Setup Installer 可從獨立首頁, AutoJs6 入口與指令碼, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku 或 Root 執行特權操作.

1.0.0: 開發預覽, 已提供獨立首頁, 設定, 已安裝應用程式管理, 循序佇列及安裝歷史. 支援安裝確認, 進度, 結果和前景通知. 程序重新啟動後保留已確認並儲存的結果, 未完成工作標為取消, 不會自動續裝或重試. `installer` 指令碼 API 需要 AutoJs6 >= 6.8.0 (5300); 宿主基本整合需要組建 5299. 裝置涵蓋範圍與剩餘驗收見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

### 使用方式

1. 在 Android 7.0 或更高版本上, 從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 頁面安裝外掛 APK. 獨立啟動器入口可開啟首頁.
2. 在首頁查看授權與預設安裝器狀態, 按新增按鈕選擇單個或多個安裝套件. 檢查安裝對話方塊後確認, 並在首頁查看進度和最近歷史.
3. 整合 AutoJs6 時, 使用組建 5299 (6.8.0) 或更高版本, 並在外掛中心啟用 `3-Setup Installer`. 指令碼 API 需要組建 5300 或更高版本.
4. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku 或 Root 授權.
5. 從首頁選單進入已安裝應用程式或設定. 可檢查本機安裝預設項, 外觀, 啟動器圖示和通知; 關於, 發行歷史與手動更新檢查位於設定中.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動) 並已向插件授權. 其 shell 權限支援靜默安裝, 靜默解除安裝及面向其他使用者的操作.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.
- **注意:** 特權可用時, 宿主請求使用 `interaction: 'auto'` 預設靜默安裝, 不會主動開啟確認介面. 若 Android 仍要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時使用 `interaction: 'dialog'`; 禁止系統確認時使用 `interaction: 'silent'`, 此時需要確認的安裝會失敗. 指令碼 API 沿用相同預設語義.
- 設定可儲存授權順序與啟用狀態, 安裝選項和進度通知偏好. 首頁及外部安裝預設使用 `dialog`; 明確儲存的 `auto` 或 `silent` 選擇會生效. 宿主/指令碼請求保留其明確選項, 指令碼 API 預設仍為 `auto`. 選擇項僅在確認後儲存.

### 權限與安全

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用於 Android 確認. QUERY_ALL_PACKAGES 用於已安裝應用程式管理, 版本與簽章比對以及預設安裝器偵測.
- FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_DATA_SYNC 支援背景安裝工作; POST_NOTIFICATIONS 用於進度與結果通知. 缺少通知權限不會阻止安裝.
- Shizuku 與 Root 只用於你發起的操作; 特權服務不保存狀態, 操作之間不保持開啟的 shell, 也不會被外掛程式之外的任何一方存取.
- 安裝, 檢查, 歷史和應用程式管理均可離線使用. INTERNET 僅在使用者手動檢查版本時存取外掛固定的 GitHub Releases API, 間隔 12 小時. 不在背景檢查更新, 不上傳安裝套件.
- 安裝套件來源以唯讀方式開啟. 歷史只儲存有限的應用程式中繼資料與結果, 不儲存套件內容或來源 URI, 錯誤中的路徑會遮蔽. 外掛私人儲存空間不參與備份. 刪除歷史不會解除安裝對應應用程式或刪除來源.

安裝指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
