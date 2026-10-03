******

### 發行歷史

******

# v1.2.0

###### 2026/10/02

* `新增` 進階安裝選項: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` 及 `packageSource`. 平台或授權方式不支援時明確拒絕, 不靜默忽略. none 不追加手動編譯, 也不關閉 Android 自身的編譯.
* `新增` 簽章檢查與本機套件名稱/SharedUID 精確封鎖清單適用於全部入口, 使用 `BLOCKED_BY_POLICY`. 僅真實 dialog 可對目前項目一次放行 mismatch/unknown 簽章, Android 仍會驗證; 靜默/通知不能放行, 封鎖清單不可覆寫. 權限預覽顯示實際選取 APK 分包宣告的權限, 不代表已授予權限.
* `新增` 來源設定檔可在設定中命名, 啟停, 編輯及排序. 按實際套件名稱選擇首個同時符合來源和名稱前綴的已啟用設定檔, 不疊加多個設定檔; 任意來源包含首頁. 設定檔只提供 12 項每應用程式選項的局部預設值, 明確請求優先, 真實確認頁的最終選擇仍可調整.
* `優化` 進階指令碼選項需要 AutoJs6 組建 5308+ 並協商 V3 與 `advanced-install-options`. 沒有設定檔覆蓋時, 省略欄位保留原行為; 明確的 `false`/`none` 仍需對應支援. 正式發佈及裝置驗收狀態以路線圖為準.
* `優化` 成功結果可回報讀回的 `updateOwner` 及 `dexopt`. null 表示 Android 未向目前呼叫身分傳回 owner, 可能沒有 owner 或受可見性過濾, 不能證明全域不存在; 讀取失敗省略欄位並寫入 notes. DexOpt 狀態為 accepted/failed/cancelled/timeout/unavailable/unknown; accepted 包含系統略過, 不證明實際執行編譯. 附加步驟失敗不改變已確認的安裝成功.
* `優化` 權限授予請求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安裝原因需要 API 26+, 來源標籤需要 API 33+, 請求更新擁有權需要 API 34+. 擁有權僅能在首次安裝啟用, 更新或其他使用者已有該套件時可能被忽略; false 不撤銷既有 owner.
* `優化` 授予選項請求系統可授予的權限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安裝器可改變的 app-op. 不保證全部宣告權限, 不授予無障礙, 懸浮視窗或任意簽章權限. restricted/system-fixed/policy-fixed 限制仍有效, 不額外設定 restricted 權限 allowlist 標誌.
* `優化` none/Dhizuku 僅能檢查目前使用者的已安裝簽章, Shizuku/Root 進行全域查詢. SharedUID 規則非空時, 無法排除其他使用者已有該套件會直接拒絕, 不能一次放行.
* `優化` 主程式介面及指令碼使用來源設定檔需要 AutoJs6 5312+ 及 V3 `source-profiles` 能力. 舊主程式或未協商的請求保留原行為. 省略欄位可繼承設定檔, 明確的 `false`/`auto`/`current`/`none` 覆蓋設定檔. 只有 `installer`, `installReason`, `packageSource` 接受 null 清除繼承; 後兩者的 null 仍需進階選項能力. `interaction`, `timeout`, `continueOnError` 屬於整次工作階段, 不進入設定檔.
* `優化` 任務開始處理時固定來源設定檔及自動授權排序, 編輯設定不會改變正在處理的任務或同批後續項目. 已確認的重試沿用完整確認選項, 未確認的重試重新讀取入口預設值及設定檔. 每次仍重新檢查來源和簽章, 不重用一次性策略放行. 設定檔不能繞過簽章, 黑名單, 授權或系統限制.
* `優化` 協商來源設定檔後, 成功項目的 `sourceDeleteRequested` 表示最終刪除請求, 不代表已刪除. 主程式明確的 `deleteSource: false` 禁止刪除; 決策欄位缺失或型別錯誤時保留檔案並記錄 notes. 主程式等待批次結果, 保留與失敗, 未處理或要求保留項目共用的檔案及可確認的別名. 本機外部入口也保留同批重複 URI/正規化路徑. `sourceDeleted` 才報告實際清理結果, content URI 不由主程式刪除.
* `優化` 啟動器與外掛中心圖示按統一視覺尺寸標準調整, 外掛中心採用透明背景和黑白或中性灰階圖案
* `相依性` 升級 installer-api.aar 至契約 V3 (MPL 2.0), 保留 V1/V2 與全部 11 個 AIDL 交易; 進階指令碼選項需要宿主建置 5308+. 同一 V3 契約附加可選 source-profiles 能力及來源設定檔欄位, 主程式接入從 5312 起, 十一項 AIDL 交易保持不變
* `相依性` 升級共用安裝套件解析器 (MPL 2.0), 核驗真實資訊清單根元素及 sharedUserId, 拒絕有歧義的輸入. 從真實 Android 命名空間元素讀取權限宣告, 排除註解及擴充命名空間偽宣告, 並拒絕編譯屬性衝突.

# v1.1.0

###### 2026/10/02

* `新增` `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
* `新增` 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
* `新增` `interaction: 'notification'` 僅用於安裝: 在通知中確認, 取消, 顯示進度和結果, 不彈出外掛安裝對話方塊. Android 系統確認仍需點擊對應通知. 必須允許通知並啟用應用程式通知及安裝管道, 否則以 `NOTIFICATION_UNAVAILABLE` 失敗. 其他互動模式不會因缺少通知權限而被阻止. 解除安裝不接受 `notification`.
* `優化` `none` 使用 Android 確認. 新設定的 `auto` 按 `shizuku -> root -> dhizuku -> none` 選擇可用方式, 可調整順序和啟用狀態. 已儲存的舊三項設定保留原相對順序及啟用選擇, Dhizuku 插入 `none` 前但預設停用. 明確指定的授權方式不會改用其他方式.
* `優化` 設定儲存授權順序與啟用狀態, 安裝選項及通知偏好. 首頁/外部安裝預設使用 `dialog`, 可明確選擇 `auto`, `silent` 或 `notification`. 宿主介面安裝入口使用 `dialog`; 指令碼保留明確選項且預設仍為 `auto`. 修改在確認後儲存.
* `相依性` 附加 Dhizuku API 2.6.0 (MIT), 提供裝置/設定檔擁有者授權方式
* `相依性` 升級 `installer-api.aar` 為契約 V2 (MPL 2.0), 保留 V1 協商並在末尾追加持久預設方法; 產物來源與 SHA-256 見第三方聲明; AutoJs6 >= 6.8.0 (5307).

# v1.0.0

###### 2026/10/02

* `提示` 1.0.0 已實作下述安裝, 應用程式管理與指令碼功能. 官方 GitHub Release 和外掛中心索引收錄仍待完成. 宿主整合需要 AutoJs6 >= 6.8.0 (5299), `installer` 指令碼 API 需要組建 5300 或更高版本. 裝置涵蓋範圍與剩餘驗收記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
* `新增` 3-Setup Installer 可從獨立首頁, AutoJs6 入口與指令碼, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku 或 Root 執行特權操作
* `新增` 安裝套件格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 壓縮檔; 分包按裝置選擇; `.aab` 檔案只識別與說明, 不安裝
* `新增` 安裝成功後可選擇盡力刪除來源. 降級, 測試套件, 繞過低 targetSdk 限制 (Android 14+), 安裝者歸屬及其他目標使用者需要 Shizuku 或 Root, 並仍受 Android 規則限制
* `新增` 指令碼 API `installer` (別名 `$installer`) 提供同步, `...Async` 與工作階段形態, 支援單項 / 批次 / 分包安裝, 解除安裝, 檢查, 授權方式與使用者查詢及預設安裝器設定; 失敗為帶穩定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)). 指令碼預設使用 `interaction: 'auto'`, 特權可用時預設靜默安裝. 宿主介面的安裝入口使用 `dialog`. 若 Android 要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時, 明確使用 `interaction: 'dialog'`. 明確指定 `silent` 時, 若特權不可用或需要系統確認, 會以 `AUTHORIZER_REQUIRED` 失敗
* `新增` 獨立首頁顯示 Shizuku/Root 的可用與授權狀態, 目前預設安裝器, 進行中工作和最近安裝. 透過系統檔案選擇器多選套件後循序安裝, 可在單項失敗後繼續或取消剩餘項目
* `新增` 確認介面顯示應用程式資訊, 新舊版本, 簽章和可勾選的 APK 分包; 進度可取消, 結果顯示成功操作或可複製的錯誤詳細資訊. 批次安裝逐項顯示狀態
* `新增` 提供前景安裝進度, 取消操作及結果通知. 未授予通知權限不會阻止安裝
* `新增` 可開啟或分享單個及多個安裝套件, 包括 MT 管理器分享的 APKS 檔案. 多個套件進入循序佇列. 外部項目失敗後, 在 URI 及存取權限仍可用時可重試
* `新增` 已安裝應用程式支援依名稱或套件名稱搜尋, 依名稱, 安裝時間或更新時間排序, 並可顯示系統應用程式. 可開啟應用程式或系統應用程式資訊, 或檢查後確認解除安裝. Shizuku 或 Root 可在確認後直接解除安裝並選擇保留資料; 其他情況使用 Android 確認
* `新增` 首頁狀態卡與設定進入同一預設安裝器頁面, 支援特權設定和清除, 無特權時提供系統設定指引. OEM 原則可能阻止變更或要求先清除原處理程式. 指令碼仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 結果如實反映裝置回應
* `新增` 設定可儲存授權順序與啟用狀態, 安裝選項和進度通知偏好. 首頁及外部安裝預設使用 `dialog`; 明確儲存的 `auto` 或 `silent` 選擇會生效. 宿主/指令碼請求保留其明確選項, 指令碼 API 預設仍為 `auto`. 選擇項僅在確認後儲存
* `新增` 外觀設定包括語言, 夜間模式, 主題色與啟動器圖示. 前三項預設跟隨 AutoJs6, 也可本機覆寫; 宿主不可用時改用系統語言和夜間模式及預設顏色. 圖示提供淺色, 深色, 自動與透明模式; 自動模式跟隨系統, 效果受啟動器快取與遮罩影響
* `新增` 私人安裝歷史最多保留 200 項, 包含套件名稱, 標籤, 新舊版本, 結果, 時間, 來源 (宿主/指令碼/外部/首頁), 授權方式及失敗詳細資料. 可逐項刪除或清空, 不解除安裝應用程式也不刪除來源檔案. 程序結束後未完成項目標為取消, 不會自動繼續執行
* `新增` 設定中提供關於頁面和十語言內建發行歷史. 手動更新檢查存取外掛的 GitHub Releases API, 間隔 12 小時, 支援快取結果與忽略版本管理. 發布頁在瀏覽器中開啟, 不會自動下載或安裝更新
* `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
* `優化` 可隨機存取的來源避免完整快取副本, 串流來源按需暫存. 支援一般 ZIP 分割套件, AAB 僅供檢查, 拒絕內容發生變化的來源
* `優化` 僅在安裝成功後嘗試刪除. 安裝失敗, 取消或逾時始終保留來源. 刪除失敗不會改變安裝成功的結果, 外部來源提供者可能拒絕刪除. 指令碼的 `deleteSource` 由宿主刪除路徑或 `file://` 來源, 保留 `content://` 來源; 請檢查 `sourceDeleted` 和 `notes`. 批次中已確認成功的項目仍按 `deleteSource` 處理, 即使其他項目失敗或剩餘佇列被取消
* `優化` 同套件安裝工作階段跨使用者和授權方式循序執行, 等待時仍支援取消與逾時, 並在獨立入口安全清理超過 24 小時的非活動暫存目錄
* `優化` 建立特權連線時若連線意外中斷, 可自動重新連線一次; 已經開始的安裝或解除安裝不會自動重複
* `相依性` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用於 Shizuku 授權方式
* `相依性` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用於 Root 授權方式
* `相依性` 附加 AndroidHiddenApiBypass 6.1 用於特權服務存取隱藏的套件安裝器 API
* `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主建置 6.8.0 / 5298, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `相依性` 附加 `installer-api.aar` (AutoJs6, MPL 2.0) 提供安裝契約; 產物來源和 SHA-256 見第三方聲明
* `相依性` 附加 `package-archive-parser.aar` (AutoJs6, MPL 2.0) 提供 APK 與容器檢查及分包選擇; 產物來源和 SHA-256 見第三方聲明
