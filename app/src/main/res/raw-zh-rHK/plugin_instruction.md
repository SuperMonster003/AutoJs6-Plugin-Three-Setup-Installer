3-Setup Installer 接管 AutoJs6 的安裝器: 檔案管理器, 外掛程式中心與腳本打包頁的安裝按鈕, `.apk`, `.apks`, `.xapk`, `.apkm` 與 `.apkz` 檔案的外部 "開啟方式" 入口, 以及腳本端用於安裝, 更新, 檢查與卸載應用程式的全域物件 `installer`. 除一般的系統確認外, 還可透過 Shizuku 或 Root 靜默安裝與卸載.

1.0.0: P2 開發預覽: 安裝, 安裝套件資訊查詢, 使用者查詢和解除安裝核心已接入主程式服務, 支援明確確認及工作階段自動清理. 完整主程式入口驗收, 完整介面, 外部開啟, 預設安裝器啟用, 腳本 API 和設定仍在推進. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### 使用方法

1. 在安裝了 AutoJs6 建置 5299 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `3-Setup Installer` 已被識別並啟用它.
3. 在 AutoJs6 檔案管理器中點選安裝套件, 在任意檔案管理器中用 3-Setup Installer 開啟安裝套件, 或在腳本中呼叫 `installer.install(...)`. 需要靜默安裝時, 依外掛程式提示啟動 Shizuku 或授予 Root, 或在外掛程式設定中選擇授權方式.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 應用程式處於執行狀態 (經無線偵錯, ADB 或 Root 啟動) 並已向外掛程式授權; 以 shell 權限執行, 可靜默安裝, 靜默卸載, 為其他使用者安裝以及鎖定預設安裝器.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.
- **注意:** 特權授權可用時, 腳本 API 預設靜默安裝, 不會主動彈出任何確認對話方塊. 若系統仍要求確認, `interaction: 'auto'` 會允許系統確認並寫入 `notes`. 需要安裝前確認時, 請明確使用 `interaction: 'dialog'`; 禁止系統確認時使用 `interaction: 'silent'`, 此時需要確認的安裝會失敗.

安裝指南與目前進度請參閱 [項目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
