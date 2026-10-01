3-Setup Installer 可從獨立首頁, AutoJs6 入口與腳本, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku 或 Root 執行特權操作.

1.0.0: 開發預覽, 已提供獨立首頁, 設定, 已安裝應用程式管理, 循序佇列及安裝歷史. 支援安裝確認, 進度, 結果和前景通知. 程序重新啟動後保留已確認並儲存的結果, 未完成工作標為取消, 不會自動續裝或重試. `installer` 腳本 API 需要 AutoJs6 >= 6.8.0 (5300); 宿主基本接入需要組建 5299. 裝置覆蓋與餘下驗收見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

### 使用方法

1. 在 Android 7.0 或更高版本上, 從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 頁面安裝插件 APK. 獨立啟動器入口可開啟首頁.
2. 在首頁查看授權與預設安裝器狀態, 按新增按鈕選擇單個或多個安裝套件. 檢查安裝對話框後確認, 並在首頁查看進度和最近歷史.
3. 接入 AutoJs6 時, 使用組建 5299 (6.8.0) 或更高版本, 並在插件中心啟用 `3-Setup Installer`. 腳本 API 需要組建 5300 或更高版本.
4. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku 或 Root 授權.
5. 從首頁選單進入已安裝應用程式或設定. 可檢查本地安裝預設項, 外觀, 啟動器圖示和通知; 關於, 發行歷史與手動更新檢查位於設定中.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動) 並已向插件授權. 其 shell 權限支援靜默安裝, 靜默解除安裝及面向其他使用者的操作.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.
- **注意:** 特權可用時, 宿主請求使用 `interaction: 'auto'` 預設靜默安裝, 不會主動開啟確認介面. 若 Android 仍要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時使用 `interaction: 'dialog'`; 禁止系統確認時使用 `interaction: 'silent'`, 此時需要確認的安裝會失敗. 腳本 API 沿用相同預設語義.
- 設定可儲存授權次序與啟用狀態, 安裝選項和進度通知偏好. 首頁及外部安裝預設使用 `dialog`; 明確儲存的 `auto` 或 `silent` 選擇會生效. 宿主/腳本請求保留其明確選項, 腳本 API 預設仍為 `auto`. 選擇項僅在確認後儲存.

### 相容性

- Android 7.0 (API 24) 及以上. 裝置驗證情況和剩餘涵蓋範圍記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 略過低 targetSdk 封鎖自 Android 14 (API 34) 起存在; 更早的系統忽略該選項並在結果中註明.
- ROM 原則與現有預設項可能限制預設安裝器變更. 1.0.0 不承諾持久鎖定; 安裝者套件名稱和外掛程式激活的注意事項見下方常見問題.

### 常見問題

- **為何安裝仍要求確認?** `none` 始終使用系統確認. 準備好授權後, 可在安裝對話框中選擇 Shizuku 或 Root. Android 或裝置原則仍可能要求系統確認.
- **能安裝 `.aab` 嗎?** 不能. Android App Bundle 是發佈格式, 請先用 bundletool 轉換為 `.apks` 集合. 外掛程式會識別 `.aab` 檔案並顯示其套件名稱與模組資訊.
- **HyperOS 的安裝者套件名稱應如何填寫?** Shizuku 經 ADB 或無線偵錯啟動時, 不指定安裝者套件名稱會使用 `com.android.shell`. 已測 Xiaomi 23046RP50C / HyperOS / API 35 的靜默新安裝與更新均記錄此值. 明確指定 `com.android.shell` 或外掛程式自身套件名稱也都成功, 查詢到的安裝者與請求一致. 其他套件名稱或 ROM 版本仍以系統答覆為準.
- **ColorOS 或其他系統提示外掛程式需要激活時怎麼辦?** 新安裝或強制停止後, Android 可能讓應用程式保持停止狀態, 等待使用者互動. 請在 AutoJs6 外掛程式中心使用提供的激活入口, 或從啟動器圖示開啟 3-Setup Installer 後重試. 這遵循 [Android 的停止狀態規則](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 專項行為尚未完成實機驗證.
- **為何設定預設安裝器會失敗?** ROM 可能拒絕變更. 舊版 Android 已有 APK 預設處理程式時, 可能需要先按頁面指引在系統設定中清除原處理程式的預設值. 如果系統沒有提供清除入口, 外掛程式無法保證替換成功. 即使 Shizuku 或 Root 可用, 1.0.0 也不承諾持久鎖定.
- **為甚麼來源沒有刪除?** 僅在安裝成功後嘗試刪除. 安裝失敗, 取消或逾時始終保留來源. 刪除失敗不會改變安裝成功的結果, 外部來源提供者可能拒絕刪除. 腳本的 `deleteSource` 由宿主刪除路徑或 `file://` 來源, 保留 `content://` 來源; 請檢查 `sourceDeleted` 和 `notes`.
- **可以重試或恢復嗎?** 失敗的外部 URI 項目在來源及存取權限仍可用時可以重試. 來源或存取權限釋放後, 請重新開啟安裝套件. 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試. 再次開始前請檢查應用程式的實際安裝狀態.

### 權限與安全

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用於 Android 確認. QUERY_ALL_PACKAGES 用於已安裝應用程式管理, 版本與簽章比對以及預設安裝器偵測.
- FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_DATA_SYNC 支援背景安裝工作; POST_NOTIFICATIONS 用於進度與結果通知. 缺少通知權限不會阻止安裝.
- Shizuku 與 Root 只用於你發起的操作; 特權服務不保存狀態, 操作之間不保持開啟的 shell, 也不會被外掛程式之外的任何一方存取.
- 安裝, 檢查, 歷史和應用程式管理均可離線使用. INTERNET 僅在使用者手動檢查版本時存取插件固定的 GitHub Releases API, 間隔 12 小時. 不在背景檢查更新, 不上傳安裝套件.
- 安裝套件來源以唯讀方式開啟. 歷史只儲存有限的應用程式中繼資料與結果, 不儲存套件內容或來源 URI, 錯誤中的路徑會遮蔽. 插件私人儲存空間不參與備份. 刪除歷史不會解除安裝對應應用程式或刪除來源.

安裝指南與目前進度請參閱 [項目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
