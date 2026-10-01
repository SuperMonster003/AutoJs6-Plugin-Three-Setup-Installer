3-Setup Installer 可從獨立首頁, AutoJs6 入口與指令碼, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku, Root 或 Dhizuku 執行特權操作.

1.2.0 已實作下述安裝, 應用程式管理與指令碼功能. 官方 GitHub Release 和外掛中心索引收錄仍待完成. 宿主整合需要 AutoJs6 >= 6.8.0 (5299), `installer` 指令碼 API 需要組建 5300 或更高版本. 裝置涵蓋範圍與剩餘驗收記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中. Dhizuku, 通知列安裝和持久預設安裝器的指令碼選項需要 AutoJs6 6.8.0 組建 5307 或以上及 installer V2 契約. 基本宿主整合仍支援組建 5299, V1 指令碼方法從組建 5300 起可用.

進階指令碼選項需要 AutoJs6 建置 5308+ 並協商 V3 與 `advanced-install-options`. 省略欄位保留原行為, 明確指定 `false`/`none` 仍需對應支援. 本機實作不表示已正式發行或全部 P9 項目完成.

### 使用方式

1. Android 7.0 及以上可在正式發布後從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝 APK, 或在官方索引收錄後使用 AutoJs6 外掛中心安裝精靈. 發布前可使用維護者提供的組建或從原始碼建置進行測試. 透過啟動器圖示開啟獨立首頁.
2. 在首頁檢查授權與預設安裝器狀態, 選擇單個或多個安裝套件. 透過所選對話方塊或通知確認, 並查看進行中工作與最近歷史.
3. 整合 AutoJs6 時, 使用組建 5299 (6.8.0) 或更高版本, 並在外掛中心啟用 `3-Setup Installer`. 指令碼 API 需要組建 5300 或更高版本. Dhizuku, 通知列安裝和持久預設安裝器的指令碼選項需要 AutoJs6 6.8.0 組建 5307 或以上及 installer V2 契約. 基本宿主整合仍支援組建 5299, V1 指令碼方法從組建 5300 起可用.
4. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku, Root 或 Dhizuku 授權.
5. 從首頁選單進入已安裝應用程式或設定. 可檢查本機安裝預設項, 外觀, 啟動器圖示和通知; 關於, 發行歷史與手動更新檢查位於設定中.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動), 並個別向 3-Setup Installer 授權. 向 AutoJs6 授權不等於向本外掛授權. 安裝, 解除安裝及其他使用者操作使用正在執行的 Shizuku 服務身分.
- `root`: 需要已 Root 的裝置, 並由 Root 管理器向 3-Setup Installer 授予 `su`. 透過 libsu 提供特權安裝, 解除安裝, 使用者及預設安裝器操作. 每項請求是否允許仍由 Android 和 ROM 原則決定.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- **注意:** 指令碼預設使用 `interaction: 'auto'`, 特權可用時預設靜默安裝. 宿主介面的安裝入口使用 `dialog`. 若 Android 要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時, 明確使用 `interaction: 'dialog'`. 明確指定 `silent` 時, 若特權不可用或需要系統確認, 會以 `AUTHORIZER_REQUIRED` 失敗.
- 設定儲存授權順序與啟用狀態, 安裝選項及通知偏好. 首頁/外部安裝預設使用 `dialog`, 可明確選擇 `auto`, `silent` 或 `notification`. 宿主介面安裝入口使用 `dialog`; 指令碼保留明確選項且預設仍為 `auto`. 修改在確認後儲存.

### 相容性

- Android 7.0 (API 24) 及以上. 裝置驗證情況和剩餘涵蓋範圍記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 略過低 targetSdk 封鎖自 Android 14 (API 34) 起存在; 更早的系統忽略該選項並在結果中註明.
- 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- 權限授予請求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安裝原因需要 API 26+, 來源標籤需要 API 33+, 請求更新擁有權需要 API 34+. 擁有權僅能在首次安裝啟用, 更新或其他使用者已有該套件時可能被忽略; false 不撤銷既有 owner.

### 常見問題

- **為何安裝仍要求確認?** `none` 始終需要 Android 系統確認, 特權方式也可能受 Android 原則限制. `notification` 僅透過通知操作開啟系統確認, 不略過 Android 的確認要求.
- **能安裝 `.aab` 嗎?** 不能. Android App Bundle 是發佈格式, 請先用 bundletool 轉換為 `.apks` 集合. 外掛程式會識別 `.aab` 檔案並顯示其套件名稱與模組資訊.
- **為何設定 `allowDowngrade: true` 後仍可能降級失敗?** 此選項僅請求允許降級; Android 根據韌體, 授權身分及應用程式是否 debuggable 作出決定. 已測 user 韌體中, Sony G8441 / API 28 與 Xiaomi 23046RP50C / API 35 拒絕非 debuggable 套件降級, Sony XQ-DQ72 / API 33 的 Root 路徑則接受. 這些結果只代表對應裝置. 請檢查傳回的錯誤與 `systemMessage`; Root 不保證所有 ROM 都允許降級.
- **HyperOS 的安裝者套件名稱應如何填寫?** Shizuku 經 ADB 或無線偵錯啟動時, 不指定安裝者套件名稱會使用 `com.android.shell`. 已測 Xiaomi 23046RP50C / HyperOS / API 35 的靜默新安裝與更新均記錄此值. 明確指定 `com.android.shell` 或外掛程式自身套件名稱也都成功, 查詢到的安裝者與請求一致. 其他套件名稱或 ROM 版本仍以系統答覆為準.
- **ColorOS 或其他系統提示外掛程式需要啟用時怎麼辦?** 新安裝或強制停止後, Android 可能讓應用程式保持停止狀態, 等待使用者互動. 請在 AutoJs6 外掛程式中心使用提供的啟用入口, 或從啟動器圖示開啟 3-Setup Installer 後重試. 這遵循 [Android 的停止狀態規則](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 專項行為尚未完成實機驗證.
- **為何設定預設安裝器會失敗, 或儲存的持久標記與目前處理程式不同?** 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- **為什麼來源沒有刪除?** 僅在安裝成功後嘗試刪除. 安裝失敗, 取消或逾時始終保留來源. 刪除失敗不會改變安裝成功的結果, 外部來源提供者可能拒絕刪除. 指令碼的 `deleteSource` 由宿主刪除路徑或 `file://` 來源, 保留 `content://` 來源; 請檢查 `sourceDeleted` 和 `notes`. 批次中已確認成功的項目仍按 `deleteSource` 處理, 即使其他項目失敗或剩餘佇列被取消.
- **可以重試或恢復嗎?** 失敗的外部 URI 項目在來源及存取權限仍可用時可以重試. 來源或存取權限釋放後, 請重新開啟安裝套件. 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試. 再次開始前請檢查應用程式的實際安裝狀態.

### 權限與安全

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用於 Android 確認. QUERY_ALL_PACKAGES 用於已安裝應用程式管理, 版本與簽章比對以及預設安裝器偵測. 一般權限 ENFORCE_UPDATE_OWNERSHIP 用於明確請求更新擁有權, 不表示必然獲得 owner.
- FOREGROUND_SERVICE 和 FOREGROUND_SERVICE_DATA_SYNC 支援安裝工作及臨時來源存取; POST_NOTIFICATIONS 用於通知. `notification` 互動要求通知與安裝管道可用, 其他互動模式允許缺少通知權限.
- Shizuku, Root 與 Dhizuku 用於請求的操作. 外掛不會自動設定裝置/設定檔擁有者. 持久預設規則只透過請求的設定或清除操作修改; 不上傳安裝套件.
- 安裝, 檢查, 歷史和應用程式管理均可離線使用. INTERNET 僅在使用者手動檢查版本時存取外掛固定的 GitHub Releases API, 間隔 12 小時. 不在背景檢查更新, 不上傳安裝套件.
- 安裝套件來源以唯讀方式開啟. 歷史只儲存有限的應用程式中繼資料與結果, 不儲存套件內容或來源 URI, 錯誤中的路徑會遮蔽. 外掛私人儲存空間不參與備份. 刪除歷史不會解除安裝對應應用程式或刪除來源.
- 授予選項請求系統可授予的權限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安裝器可改變的 app-op. 不保證全部宣告權限, 不授予無障礙, 懸浮視窗或任意簽章權限. restricted/system-fixed/policy-fixed 限制仍有效, 不額外設定 restricted 權限 allowlist 標誌.

安裝指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
