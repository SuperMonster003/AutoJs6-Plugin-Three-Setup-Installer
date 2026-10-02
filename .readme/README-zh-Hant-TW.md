<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>透過 Android 確認, Shizuku, Root 或 Dhizuku 安裝, 更新與解除安裝應用程式</p>

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

3-Setup Installer 可從獨立首頁, AutoJs6 入口與指令碼, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku, Root 或 Dhizuku 執行特權操作.

外掛獨立處理套件檢查, 安裝和結果, 可選擇對話方塊, 靜默或通知列互動. 通知列模式在通知中確認, 取消和顯示結果; Android 仍要求系統確認時, 僅在點擊對應通知後開啟.

******

### 目前狀態

******

1.2.0 已實作下述安裝, 應用程式管理與指令碼功能. 目前版本可從 [GitHub Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/tag/v1.2.0) 或 AutoJs6 外掛中心取得. 宿主整合需要 AutoJs6 >= 6.8.0 (5299), `installer` 指令碼 API 需要組建 5300 或更高版本. 裝置涵蓋範圍與剩餘驗收記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中. Dhizuku, 通知列安裝和持久預設安裝器的指令碼選項需要 AutoJs6 6.8.0 組建 5307 或以上及 installer V2 契約. 基本宿主整合仍支援組建 5299, V1 指令碼方法從組建 5300 起可用.

******

### 功能

******

1.2.0 已實作的功能:

- 安裝套件格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 壓縮檔; 分包按裝置選擇; `.aab` 檔案只識別與說明, 不安裝.
- `none` 使用 Android 確認. 新設定的 `auto` 按 `shizuku -> root -> dhizuku -> none` 選擇可用方式, 可調整順序和啟用狀態. 已儲存的舊三項設定保留原相對順序及啟用選擇, Dhizuku 插入 `none` 前但預設停用. 明確指定的授權方式不會改用其他方式.
- 安裝成功後可選擇盡力刪除來源. 降級, 測試套件, 繞過低 targetSdk 限制 (Android 14+), 安裝者歸屬及其他目標使用者需要 Shizuku 或 Root, 並仍受 Android 規則限制.
- 已安裝應用程式可按名稱或套件名稱搜尋, 按名稱, 安裝或更新時間排序, 並可顯示系統應用程式. 支援開啟應用程式, 系統詳情及確認解除安裝. Shizuku 和 Root 支援靜默解除安裝及選擇保留資料; Dhizuku 僅支援目前所有者使用者的特權解除安裝, 不支援 `keepData`.
- 確認介面顯示應用程式資訊, 新舊版本, 簽章和可勾選的 APK 分包; 進度可取消, 結果顯示成功操作或可複製的錯誤詳細資訊. 批次安裝逐項顯示狀態.
- 可開啟或分享單個及多個安裝套件, 包括 MT 管理器分享的 APKS 檔案. 多個套件進入循序佇列. 外部項目失敗後, 在 URI 及存取權限仍可用時可重試.
- `interaction: 'notification'` 僅用於安裝: 在通知中確認, 取消, 顯示進度和結果, 不彈出外掛安裝對話方塊. Android 系統確認仍需點擊對應通知. 必須允許通知並啟用應用程式通知及安裝管道, 否則以 `NOTIFICATION_UNAVAILABLE` 失敗. 其他互動模式不會因缺少通知權限而被阻止. 解除安裝不接受 `notification`.
- 外觀設定包括語言, 夜間模式, 主題色與啟動器圖示. 前三項預設跟隨 AutoJs6, 也可本機覆寫; 宿主不可用時改用系統語言和夜間模式及預設顏色. 圖示提供淺色, 深色, 自動與透明模式; 自動模式跟隨系統, 效果受啟動器快取與遮罩影響.
- 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- 指令碼 API `installer` (別名 `$installer`) 提供同步, `...Async` 與工作階段形態, 支援單項 / 批次 / 分包安裝, 解除安裝, 檢查, 授權方式與使用者查詢及預設安裝器設定; 失敗為帶穩定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)).
- 獨立首頁顯示 Shizuku/Root/Dhizuku 的可用與授權狀態, 目前預設安裝器, 進行中工作和最近安裝. 透過系統檔案選擇器多選套件後循序安裝, 可在單項失敗後繼續或取消剩餘項目.
- 私人安裝歷史最多保留 200 項, 包含套件名稱, 標籤, 新舊版本, 結果, 時間, 來源 (宿主/指令碼/外部/首頁), 授權方式及失敗詳細資料. 可逐項刪除或清空, 不解除安裝應用程式也不刪除來源檔案. 程序結束後未完成項目標為取消, 不會自動繼續執行.
- 設定儲存授權順序與啟用狀態, 安裝選項及通知偏好. 首頁/外部安裝預設使用 `dialog`, 可明確選擇 `auto`, `silent` 或 `notification`. 宿主介面安裝入口使用 `dialog`; 指令碼保留明確選項且預設仍為 `auto`. 修改在確認後儲存.
- 設定中提供關於頁面和十語言內建發行歷史. 手動更新檢查存取外掛的 GitHub Releases API, 間隔 12 小時, 支援快取結果與忽略版本管理. 發布頁在瀏覽器中開啟, 不會自動下載或安裝更新.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- 進階安裝選項: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` 及 `packageSource`. 平台或授權方式不支援時明確拒絕, 不靜默忽略. none 不追加手動編譯, 也不關閉 Android 自身的編譯.
- 成功結果可回報讀回的 `updateOwner` 及 `dexopt`. null 表示 Android 未向目前呼叫身分傳回 owner, 可能沒有 owner 或受可見性過濾, 不能證明全域不存在; 讀取失敗省略欄位並寫入 notes. DexOpt 狀態為 accepted/failed/cancelled/timeout/unavailable/unknown; accepted 包含系統略過, 不證明實際執行編譯. 附加步驟失敗不改變已確認的安裝成功.
- 簽章檢查與本機套件名稱/SharedUID 精確封鎖清單適用於全部入口, 使用 `BLOCKED_BY_POLICY`. 僅真實 dialog 可對目前項目一次放行 mismatch/unknown 簽章, Android 仍會驗證; 靜默/通知不能放行, 封鎖清單不可覆寫. 權限預覽顯示實際選取 APK 分包宣告的權限, 不代表已授予權限.
- 來源設定檔可在設定中命名, 啟停, 編輯及排序. 按實際套件名稱選擇首個同時符合來源和名稱前綴的已啟用設定檔, 不疊加多個設定檔; 任意來源包含首頁. 設定檔只提供 12 項每應用程式選項的局部預設值, 明確請求優先, 真實確認頁的最終選擇仍可調整.

******

### 使用方式

******

1. Android 7.0 及以上可在正式發布後從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝 APK, 或在官方索引收錄後使用 AutoJs6 外掛中心安裝精靈. 發布前可使用維護者提供的組建或從原始碼建置進行測試. 透過啟動器圖示開啟獨立首頁.
2. 在首頁檢查授權與預設安裝器狀態, 選擇單個或多個安裝套件. 透過所選對話方塊或通知確認, 並查看進行中工作與最近歷史.
3. 整合 AutoJs6 時, 使用組建 5299 (6.8.0) 或更高版本, 並在外掛中心啟用 `3-Setup Installer`. 指令碼 API 需要組建 5300 或更高版本. Dhizuku, 通知列安裝和持久預設安裝器的指令碼選項需要 AutoJs6 6.8.0 組建 5307 或以上及 installer V2 契約. 基本宿主整合仍支援組建 5299, V1 指令碼方法從組建 5300 起可用.
4. 使用 AutoJs6 的安裝操作, 或在開啟及分享安裝套件時選擇 3-Setup Installer. 出現確認對話框時, 先檢查應用程式與安裝選項再確認安裝. 選擇特權方式時, 請準備 Shizuku, Root 或 Dhizuku 授權.
5. 從首頁選單進入已安裝應用程式或設定. 可檢查本機安裝預設項, 外觀, 啟動器圖示和通知; 關於, 發行歷史與手動更新檢查位於設定中.

******

### 授權方式

******

各授權方式能做什麼以及需要什麼:

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 正在執行 (經無線偵錯, ADB 或 Root 啟動), 並個別向 3-Setup Installer 授權. 向 AutoJs6 授權不等於向本外掛授權. 安裝, 解除安裝及其他使用者操作使用正在執行的 Shizuku 服務身分.
- `root`: 需要已 Root 的裝置, 並由 Root 管理器向 3-Setup Installer 授予 `su`. 透過 libsu 提供特權安裝, 解除安裝, 使用者及預設安裝器操作. 每項請求是否允許仍由 Android 和 ROM 原則決定.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- **注意:** 指令碼預設使用 `interaction: 'auto'`, 特權可用時預設靜默安裝. 宿主介面的安裝入口使用 `dialog`. 若 Android 要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時, 明確使用 `interaction: 'dialog'`. 明確指定 `silent` 時, 若特權不可用或需要系統確認, 會以 `AUTHORIZER_REQUIRED` 失敗.
- 設定儲存授權順序與啟用狀態, 安裝選項及通知偏好. 首頁/外部安裝預設使用 `dialog`, 可明確選擇 `auto`, `silent` 或 `notification`. 宿主介面安裝入口使用 `dialog`; 指令碼保留明確選項且預設仍為 `auto`. 修改在確認後儲存.

******

### 快速入門

******

適用於 AutoJs6 >= 6.8.0 (5300) 的 `install`, `installAsync`, `session`, `uninstall` 和 `setDefault` 範例. 這些函式僅在傳入自行選擇的來源, 套件名稱或預設安裝器選項並呼叫時執行; 開頭的狀態查詢為唯讀操作.

```js
// 唯讀查詢可用狀態與相容資訊.
console.log(installer.status);

// 需要 Shizuku 授權; silent 在 Android 要求確認時失敗.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// 陣列表示獨立安裝套件, 每個項目分別傳回結果.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// 所有分包屬於同一個應用程式, 包括其 base APK.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// 呼叫後立即開始; 保留傳回的工作階段可取消或等待.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// 僅傳入確定要解除安裝的套件名稱; keepData 請求保留資料.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true 設本外掛為預設, false 清除其預設項; 受 ROM 限制.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// V2 範例需要宿主組建 5307. Dhizuku 需要已啟用擁有者, 通知模式需要通知權限.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root 持久模式要求裝置支援使用者 0 的 system UID 存取; 保留衝突原則.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});

// 進階範例: 需要宿主 V3, Root 及這些中繼資料欄位所需的 API 33+; 呼叫函式才開始安裝
let installAdvancedChosen = source => installer.installAsync(source, {
    authorizer: 'root', interaction: 'dialog', deleteSource: false,
    grantAllRequestedPermissions: false, requestUpdateOwnership: false,
    dexopt: 'speed-profile', installReason: 'user', packageSource: 'local-file',
}).then(result => console.log(result.ok, result.updateOwner, result.dexopt, result.notes))
    .catch(error => console.error(error.code, error.systemMessage));

// 來源設定檔範例: 需要主程式 5312+; 保留檔案並清除三個繼承欄位, 呼叫函式才開始安裝
let installWithProfileResets = source => installer.installAsync(source, {
    interaction: 'dialog', deleteSource: false,
    installer: null, installReason: null, packageSource: null,
});
```

來源可為路徑, `file://` 或有讀取權限的 `content://` URI. 陣列表示獨立批次項目, `{ splits: [...] }` 表示一個應用程式的分包. `session(...)` 建立後立即開始, 傳回物件支援 `cancel()` 和 `wait()`. 同步呼叫可能擲出 `InstallerError`, 且不能在 UI 執行緒執行. 讀取 `installer.status`, 建立 `installer.session(...)` 和呼叫 `session.wait()` 同樣受此限制. UI 執行緒請使用 Async 方法, 或在指令碼工作執行緒執行同步操作. 工作階段物件只能在建立它的指令碼執行緒使用. 請處理 Promise 拒絕, 並逐項檢查批次結果的 `ok` 與 `error`. 外掛缺失或不相容時回報 `PLUGIN_UNAVAILABLE`. `setDefault` 傳回是否達到請求狀態, 清除預設項成功也傳回 true. `app.uninstall` 仍是宿主的系統解除安裝快捷入口, 需要特權選項時使用 `installer.uninstall`. 完整選項與事件見 [installer API 文件](https://docs.autojs6.com/#/installer).

Dhizuku, 通知列安裝和持久預設安裝器的指令碼選項需要 AutoJs6 6.8.0 組建 5307 或以上及 installer V2 契約. 基本宿主整合仍支援組建 5299, V1 指令碼方法從組建 5300 起可用.

進階指令碼選項需要 AutoJs6 組建 5308+ 並協商 V3 與 `advanced-install-options`. 沒有設定檔覆蓋時, 省略欄位保留原行為; 明確的 `false`/`none` 仍需對應支援. 正式發佈及裝置驗收狀態以路線圖為準.

主程式介面及指令碼使用來源設定檔需要 AutoJs6 5312+ 及 V3 `source-profiles` 能力. 舊主程式或未協商的請求保留原行為. 省略欄位可繼承設定檔, 明確的 `false`/`auto`/`current`/`none` 覆蓋設定檔. 只有 `installer`, `installReason`, `packageSource` 接受 null 清除繼承; 後兩者的 null 仍需進階選項能力. `interaction`, `timeout`, `continueOnError` 屬於整次工作階段, 不進入設定檔.

任務開始處理時固定來源設定檔及自動授權排序, 編輯設定不會改變正在處理的任務或同批後續項目. 已確認的重試沿用完整確認選項, 未確認的重試重新讀取入口預設值及設定檔. 每次仍重新檢查來源和簽章, 不重用一次性策略放行. 設定檔不能繞過簽章, 黑名單, 授權或系統限制.

協商來源設定檔後, 成功項目的 `sourceDeleteRequested` 表示最終刪除請求, 不代表已刪除. 主程式明確的 `deleteSource: false` 禁止刪除; 決策欄位缺失或型別錯誤時保留檔案並記錄 notes. 主程式等待批次結果, 保留與失敗, 未處理或要求保留項目共用的檔案及可確認的別名. 本機外部入口也保留同批重複 URI/正規化路徑. `sourceDeleted` 才報告實際清理結果, content URI 不由主程式刪除.

******

### 相容性

******

決定外掛程式能力邊界的平台事實:

- Android 7.0 (API 24) 及以上. 裝置驗證情況和剩餘涵蓋範圍記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 略過低 targetSdk 封鎖自 Android 14 (API 34) 起存在; 更早的系統忽略該選項並在結果中註明.
- 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- 權限授予請求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安裝原因需要 API 26+, 來源標籤需要 API 33+, 請求更新擁有權需要 API 34+. 擁有權僅能在首次安裝啟用, 更新或其他使用者已有該套件時可能被忽略; false 不撤銷既有 owner.
- none/Dhizuku 僅能檢查目前使用者的已安裝簽章, Shizuku/Root 進行全域查詢. SharedUID 規則非空時, 無法排除其他使用者已有該套件會直接拒絕, 不能一次放行.

******

### 常見問題

******

- **為何安裝仍要求確認?** `none` 始終需要 Android 系統確認, 特權方式也可能受 Android 原則限制. `notification` 僅透過通知操作開啟系統確認, 不略過 Android 的確認要求.
- **能安裝 `.aab` 嗎?** 不能. Android App Bundle 是發佈格式, 請先用 bundletool 轉換為 `.apks` 集合. 外掛程式會識別 `.aab` 檔案並顯示其套件名稱與模組資訊.
- **為何設定 `allowDowngrade: true` 後仍可能降級失敗?** 此選項僅請求允許降級; Android 根據韌體, 授權身分及應用程式是否 debuggable 作出決定. 已測 user 韌體中, Sony G8441 / API 28 與 Xiaomi 23046RP50C / API 35 拒絕非 debuggable 套件降級, Sony XQ-DQ72 / API 33 的 Root 路徑則接受. 這些結果只代表對應裝置. 請檢查傳回的錯誤與 `systemMessage`; Root 不保證所有 ROM 都允許降級.
- **HyperOS 的安裝者套件名稱應如何填寫?** Shizuku 經 ADB 或無線偵錯啟動時, 不指定安裝者套件名稱會使用 `com.android.shell`. 已測 Xiaomi 23046RP50C / HyperOS / API 35 的靜默新安裝與更新均記錄此值. 明確指定 `com.android.shell` 或外掛程式自身套件名稱也都成功, 查詢到的安裝者與請求一致. 其他套件名稱或 ROM 版本仍以系統答覆為準.
- **ColorOS 或其他系統提示外掛程式需要啟用時怎麼辦?** 新安裝或強制停止後, Android 可能讓應用程式保持停止狀態, 等待使用者互動. 請在 AutoJs6 外掛程式中心使用提供的啟用入口, 或從啟動器圖示開啟 3-Setup Installer 後重試. 這遵循 [Android 的停止狀態規則](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 專項行為尚未完成實機驗證.
- **為何設定預設安裝器會失敗, 或儲存的持久標記與目前處理程式不同?** 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- **為什麼來源沒有刪除?** 僅在安裝成功後嘗試刪除. 安裝失敗, 取消或逾時始終保留來源. 刪除失敗不會改變安裝成功的結果, 外部來源提供者可能拒絕刪除. 指令碼的 `deleteSource` 由宿主刪除路徑或 `file://` 來源, 保留 `content://` 來源; 請檢查 `sourceDeleted` 和 `notes`. 批次中已確認成功的項目仍按 `deleteSource` 處理, 即使其他項目失敗或剩餘佇列被取消.
- **可以重試或恢復嗎?** 失敗的外部 URI 項目在來源及存取權限仍可用時可以重試. 來源或存取權限釋放後, 請重新開啟安裝套件. 程序重新啟動後, 恢復介面顯示已確認並儲存的結果, 未完成項目標為中斷. 恢復介面為唯讀, 不會自動安裝或重試. 再次開始前請檢查應用程式的實際安裝狀態.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外部 "開啟方式" 入口只接受安裝套件檔案, 從不執行腳本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用於 Android 確認. QUERY_ALL_PACKAGES 用於已安裝應用程式管理, 版本與簽章比對以及預設安裝器偵測. 一般權限 ENFORCE_UPDATE_OWNERSHIP 用於明確請求更新擁有權, 不表示必然獲得 owner.
- FOREGROUND_SERVICE 和 FOREGROUND_SERVICE_DATA_SYNC 支援安裝工作及臨時來源存取; POST_NOTIFICATIONS 用於通知. `notification` 互動要求通知與安裝管道可用, 其他互動模式允許缺少通知權限.
- Shizuku, Root 與 Dhizuku 用於請求的操作. 外掛不會自動設定裝置/設定檔擁有者. 持久預設規則只透過請求的設定或清除操作修改; 不上傳安裝套件.
- 安裝, 檢查, 歷史和應用程式管理均可離線使用. INTERNET 僅在使用者手動檢查版本時存取外掛固定的 GitHub Releases API, 間隔 12 小時. 不在背景檢查更新, 不上傳安裝套件.
- 安裝套件來源以唯讀方式開啟. 歷史只儲存有限的應用程式中繼資料與結果, 不儲存套件內容或來源 URI, 錯誤中的路徑會遮蔽. 外掛私人儲存空間不參與備份. 刪除歷史不會解除安裝對應應用程式或刪除來源.
- 授予選項請求系統可授予的權限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安裝器可改變的 app-op. 不保證全部宣告權限, 不授予無障礙, 懸浮視窗或任意簽章權限. restricted/system-fixed/policy-fixed 限制仍有效, 不額外設定 restricted 權限 allowlist 標誌.
- 簽章檢查與本機套件名稱/SharedUID 精確封鎖清單適用於全部入口, 使用 `BLOCKED_BY_POLICY`. 僅真實 dialog 可對目前項目一次放行 mismatch/unknown 簽章, Android 仍會驗證; 靜默/通知不能放行, 封鎖清單不可覆寫. 權限預覽顯示實際選取 APK 分包宣告的權限, 不代表已授予權限.

正式發布後, 請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

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
minimum host build: 5299 (6.8.0)
```

`ThreeSetupInstallerPluginService` 回應 `org.autojs.plugin.INSTALLER` (category `installer`), 實作宿主 installer-api 契約 `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟用外掛程式.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.2.0

_2026/10/02_

- `新增` 進階安裝選項: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` 及 `packageSource`. 平台或授權方式不支援時明確拒絕, 不靜默忽略. none 不追加手動編譯, 也不關閉 Android 自身的編譯.
- `新增` 簽章檢查與本機套件名稱/SharedUID 精確封鎖清單適用於全部入口, 使用 `BLOCKED_BY_POLICY`. 僅真實 dialog 可對目前項目一次放行 mismatch/unknown 簽章, Android 仍會驗證; 靜默/通知不能放行, 封鎖清單不可覆寫. 權限預覽顯示實際選取 APK 分包宣告的權限, 不代表已授予權限.
- `新增` 來源設定檔可在設定中命名, 啟停, 編輯及排序. 按實際套件名稱選擇首個同時符合來源和名稱前綴的已啟用設定檔, 不疊加多個設定檔; 任意來源包含首頁. 設定檔只提供 12 項每應用程式選項的局部預設值, 明確請求優先, 真實確認頁的最終選擇仍可調整.
- `優化` 進階指令碼選項需要 AutoJs6 組建 5308+ 並協商 V3 與 `advanced-install-options`. 沒有設定檔覆蓋時, 省略欄位保留原行為; 明確的 `false`/`none` 仍需對應支援. 正式發佈及裝置驗收狀態以路線圖為準.
- `優化` 成功結果可回報讀回的 `updateOwner` 及 `dexopt`. null 表示 Android 未向目前呼叫身分傳回 owner, 可能沒有 owner 或受可見性過濾, 不能證明全域不存在; 讀取失敗省略欄位並寫入 notes. DexOpt 狀態為 accepted/failed/cancelled/timeout/unavailable/unknown; accepted 包含系統略過, 不證明實際執行編譯. 附加步驟失敗不改變已確認的安裝成功.
- `優化` 權限授予請求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安裝原因需要 API 26+, 來源標籤需要 API 33+, 請求更新擁有權需要 API 34+. 擁有權僅能在首次安裝啟用, 更新或其他使用者已有該套件時可能被忽略; false 不撤銷既有 owner.
- `優化` 授予選項請求系統可授予的權限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安裝器可改變的 app-op. 不保證全部宣告權限, 不授予無障礙, 懸浮視窗或任意簽章權限. restricted/system-fixed/policy-fixed 限制仍有效, 不額外設定 restricted 權限 allowlist 標誌.
- `優化` none/Dhizuku 僅能檢查目前使用者的已安裝簽章, Shizuku/Root 進行全域查詢. SharedUID 規則非空時, 無法排除其他使用者已有該套件會直接拒絕, 不能一次放行.
- `優化` 主程式介面及指令碼使用來源設定檔需要 AutoJs6 5312+ 及 V3 `source-profiles` 能力. 舊主程式或未協商的請求保留原行為. 省略欄位可繼承設定檔, 明確的 `false`/`auto`/`current`/`none` 覆蓋設定檔. 只有 `installer`, `installReason`, `packageSource` 接受 null 清除繼承; 後兩者的 null 仍需進階選項能力. `interaction`, `timeout`, `continueOnError` 屬於整次工作階段, 不進入設定檔.
- `優化` 任務開始處理時固定來源設定檔及自動授權排序, 編輯設定不會改變正在處理的任務或同批後續項目. 已確認的重試沿用完整確認選項, 未確認的重試重新讀取入口預設值及設定檔. 每次仍重新檢查來源和簽章, 不重用一次性策略放行. 設定檔不能繞過簽章, 黑名單, 授權或系統限制.
- `優化` 協商來源設定檔後, 成功項目的 `sourceDeleteRequested` 表示最終刪除請求, 不代表已刪除. 主程式明確的 `deleteSource: false` 禁止刪除; 決策欄位缺失或型別錯誤時保留檔案並記錄 notes. 主程式等待批次結果, 保留與失敗, 未處理或要求保留項目共用的檔案及可確認的別名. 本機外部入口也保留同批重複 URI/正規化路徑. `sourceDeleted` 才報告實際清理結果, content URI 不由主程式刪除.
- `相依性` 升級 installer-api.aar 至契約 V3 (MPL 2.0), 保留 V1/V2 與全部 11 個 AIDL 交易; 進階指令碼選項需要宿主建置 5308+. 同一 V3 契約附加可選 source-profiles 能力及來源設定檔欄位, 主程式接入從 5312 起, 十一項 AIDL 交易保持不變
- `相依性` 升級共用安裝套件解析器 (MPL 2.0), 核驗真實資訊清單根元素及 sharedUserId, 拒絕有歧義的輸入. 從真實 Android 命名空間元素讀取權限宣告, 排除註解及擴充命名空間偽宣告, 並拒絕編譯屬性衝突.

#### v1.1.0

_2026/10/02_

- `新增` `dhizuku`: 需要 Android 8.0 (API 26)+, 已啟用的 Dhizuku 裝置/設定檔擁有者, 並向此外掛授權. 只操作目前擁有者使用者, 安裝者歸屬使用真實擁有者套件名稱. 不提供 shell/root 的降級, 測試套件, 略過低 targetSdk, 其他使用者, 任意安裝者歸屬或解除安裝保留資料選項. 外掛不自動設定擁有者.
- `新增` 預設安裝器頁面區分一般偏好與持久化原則. 一般偏好透過 Shizuku 或 Root 設定, 仍受 ROM 限制. Dhizuku 的持久化原則支援 API 26-33; API 34+ 因無法驗證擁有者回呼, 在修改前拒絕. Root 僅在受支援裝置的使用者 0 中使用 system UID 輔助程序. 不覆寫衝突的持久原則. `persistentConfigured` 僅記錄此前成功設定的紀錄, 並非目前系統原則的證明; 被動查詢只回報 `preferred` 或 `none`.
- `新增` `interaction: 'notification'` 僅用於安裝: 在通知中確認, 取消, 顯示進度和結果, 不彈出外掛安裝對話方塊. Android 系統確認仍需點擊對應通知. 必須允許通知並啟用應用程式通知及安裝管道, 否則以 `NOTIFICATION_UNAVAILABLE` 失敗. 其他互動模式不會因缺少通知權限而被阻止. 解除安裝不接受 `notification`.
- `優化` `none` 使用 Android 確認. 新設定的 `auto` 按 `shizuku -> root -> dhizuku -> none` 選擇可用方式, 可調整順序和啟用狀態. 已儲存的舊三項設定保留原相對順序及啟用選擇, Dhizuku 插入 `none` 前但預設停用. 明確指定的授權方式不會改用其他方式.
- `優化` 設定儲存授權順序與啟用狀態, 安裝選項及通知偏好. 首頁/外部安裝預設使用 `dialog`, 可明確選擇 `auto`, `silent` 或 `notification`. 宿主介面安裝入口使用 `dialog`; 指令碼保留明確選項且預設仍為 `auto`. 修改在確認後儲存.
- `相依性` 附加 Dhizuku API 2.6.0 (MIT), 提供裝置/設定檔擁有者授權方式
- `相依性` 升級 `installer-api.aar` 為契約 V2 (MPL 2.0), 保留 V1 協商並在末尾追加持久預設方法; 產物來源與 SHA-256 見第三方聲明; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `提示` 1.0.0 已實作下述安裝, 應用程式管理與指令碼功能. 官方 GitHub Release 和外掛中心索引收錄仍待完成. 宿主整合需要 AutoJs6 >= 6.8.0 (5299), `installer` 指令碼 API 需要組建 5300 或更高版本. 裝置涵蓋範圍與剩餘驗收記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- `新增` 3-Setup Installer 可從獨立首頁, AutoJs6 入口與指令碼, 以及安裝套件的外部開啟和分享請求安裝, 更新, 檢查與解除安裝 Android 應用程式. 支援 Android 系統確認及透過 Shizuku 或 Root 執行特權操作
- `新增` 安裝套件格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 壓縮檔; 分包按裝置選擇; `.aab` 檔案只識別與說明, 不安裝
- `新增` 安裝成功後可選擇盡力刪除來源. 降級, 測試套件, 繞過低 targetSdk 限制 (Android 14+), 安裝者歸屬及其他目標使用者需要 Shizuku 或 Root, 並仍受 Android 規則限制
- `新增` 指令碼 API `installer` (別名 `$installer`) 提供同步, `...Async` 與工作階段形態, 支援單項 / 批次 / 分包安裝, 解除安裝, 檢查, 授權方式與使用者查詢及預設安裝器設定; 失敗為帶穩定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)). 指令碼預設使用 `interaction: 'auto'`, 特權可用時預設靜默安裝. 宿主介面的安裝入口使用 `dialog`. 若 Android 要求確認, `auto` 允許系統確認並記錄至 `notes`. 需要安裝前確認時, 明確使用 `interaction: 'dialog'`. 明確指定 `silent` 時, 若特權不可用或需要系統確認, 會以 `AUTHORIZER_REQUIRED` 失敗
- `新增` 獨立首頁顯示 Shizuku/Root 的可用與授權狀態, 目前預設安裝器, 進行中工作和最近安裝. 透過系統檔案選擇器多選套件後循序安裝, 可在單項失敗後繼續或取消剩餘項目
- `新增` 確認介面顯示應用程式資訊, 新舊版本, 簽章和可勾選的 APK 分包; 進度可取消, 結果顯示成功操作或可複製的錯誤詳細資訊. 批次安裝逐項顯示狀態
- `新增` 提供前景安裝進度, 取消操作及結果通知. 未授予通知權限不會阻止安裝
- `新增` 可開啟或分享單個及多個安裝套件, 包括 MT 管理器分享的 APKS 檔案. 多個套件進入循序佇列. 外部項目失敗後, 在 URI 及存取權限仍可用時可重試
- `新增` 已安裝應用程式支援依名稱或套件名稱搜尋, 依名稱, 安裝時間或更新時間排序, 並可顯示系統應用程式. 可開啟應用程式或系統應用程式資訊, 或檢查後確認解除安裝. Shizuku 或 Root 可在確認後直接解除安裝並選擇保留資料; 其他情況使用 Android 確認
- `新增` 首頁狀態卡與設定進入同一預設安裝器頁面, 支援特權設定和清除, 無特權時提供系統設定指引. OEM 原則可能阻止變更或要求先清除原處理程式. 指令碼仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 結果如實反映裝置回應
- `新增` 設定可儲存授權順序與啟用狀態, 安裝選項和進度通知偏好. 首頁及外部安裝預設使用 `dialog`; 明確儲存的 `auto` 或 `silent` 選擇會生效. 宿主/指令碼請求保留其明確選項, 指令碼 API 預設仍為 `auto`. 選擇項僅在確認後儲存
- `新增` 外觀設定包括語言, 夜間模式, 主題色與啟動器圖示. 前三項預設跟隨 AutoJs6, 也可本機覆寫; 宿主不可用時改用系統語言和夜間模式及預設顏色. 圖示提供淺色, 深色, 自動與透明模式; 自動模式跟隨系統, 效果受啟動器快取與遮罩影響
- `新增` 私人安裝歷史最多保留 200 項, 包含套件名稱, 標籤, 新舊版本, 結果, 時間, 來源 (宿主/指令碼/外部/首頁), 授權方式及失敗詳細資料. 可逐項刪除或清空, 不解除安裝應用程式也不刪除來源檔案. 程序結束後未完成項目標為取消, 不會自動繼續執行
- `新增` 設定中提供關於頁面和十語言內建發行歷史. 手動更新檢查存取外掛的 GitHub Releases API, 間隔 12 小時, 支援快取結果與忽略版本管理. 發布頁在瀏覽器中開啟, 不會自動下載或安裝更新
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
- `優化` 可隨機存取的來源避免完整快取副本, 串流來源按需暫存. 支援一般 ZIP 分割套件, AAB 僅供檢查, 拒絕內容發生變化的來源
- `優化` 僅在安裝成功後嘗試刪除. 安裝失敗, 取消或逾時始終保留來源. 刪除失敗不會改變安裝成功的結果, 外部來源提供者可能拒絕刪除. 指令碼的 `deleteSource` 由宿主刪除路徑或 `file://` 來源, 保留 `content://` 來源; 請檢查 `sourceDeleted` 和 `notes`. 批次中已確認成功的項目仍按 `deleteSource` 處理, 即使其他項目失敗或剩餘佇列被取消
- `優化` 同套件安裝工作階段跨使用者和授權方式循序執行, 等待時仍支援取消與逾時, 並在獨立入口安全清理超過 24 小時的非活動暫存目錄
- `優化` 建立特權連線時若連線意外中斷, 可自動重新連線一次; 已經開始的安裝或解除安裝不會自動重複
- `相依性` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用於 Shizuku 授權方式
- `相依性` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用於 Root 授權方式
- `相依性` 附加 AndroidHiddenApiBypass 6.1 用於特權服務存取隱藏的套件安裝器 API
- `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主建置 6.8.0 / 5298, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `相依性` 附加 `installer-api.aar` (AutoJs6, MPL 2.0) 提供安裝契約; 產物來源和 SHA-256 見第三方聲明
- `相依性` 附加 `package-archive-parser.aar` (AutoJs6, MPL 2.0) 提供 APK 與容器檢查及分包選擇; 產物來源和 SHA-256 見第三方聲明

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置與驗證

******

開發者可使用以下命令建置並驗證外掛. 正式發布前, 使用維護者提供的組建或本機建置進行測試; 正式 APK 將透過 Releases 及完成索引收錄後的外掛中心分發.

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
