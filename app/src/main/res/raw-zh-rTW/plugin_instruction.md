3-Setup Installer 接管 AutoJs6 的安裝器: 檔案管理器, 外掛程式中心與腳本打包頁的安裝按鈕, `.apk`, `.apks`, `.xapk`, `.apkm` 與 `.apkz` 檔案的外部 "開啟方式" 入口, 以及腳本端用於安裝, 更新, 檢查與解除安裝應用程式的全域物件 `installer`. 除一般的系統確認外, 還可透過 Shizuku 或 Root 靜默安裝與解除安裝.

版本 1.0.0 為 P0 開發預覽: 儲存庫骨架, 可被 AutoJs6 外掛程式中心識別的外掛程式身分, 以及特權安裝 spike. Binder 契約, 安裝引擎, 對話方塊, 腳本 API 與設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 的階段推進. 需要 AutoJs6 6.8.0 (build 5299) 或更高版本. P0 已驗證 Shizuku 和 Root 靜默安裝, 更新, 解除安裝及一般預設安裝器設定. 宿主與腳本安裝入口尚未開放, 本版本仍不支援持久預設項.

### 使用方式

1. 在安裝了 AutoJs6 建置 5299 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `3-Setup Installer` 已被識別並啟用它.
3. 在 AutoJs6 檔案管理器中點選安裝套件, 在任意檔案管理器中用 3-Setup Installer 開啟安裝套件, 或在腳本中呼叫 `installer.install(...)`. 需要靜默安裝時, 依外掛程式提示啟動 Shizuku 或授予 Root, 或在外掛程式設定中選擇授權方式.

### 授權方式

- `none`: 標準 PackageInstaller 工作階段; Android 會要求使用者確認每次安裝, 支援分包, 不提供特權選項.
- `shizuku`: 需要 Shizuku 應用程式處於執行狀態 (經無線偵錯, ADB 或 Root 啟動) 並已向外掛程式授權; 以 shell 權限執行, 可靜默安裝, 靜默解除安裝, 為其他使用者安裝以及鎖定預設安裝器.
- `root`: 需要 Root 管理器向外掛程式授予 `su`; 透過 libsu Root 服務提供與 Shizuku 相同的操作. 在一般 (user) 韌體上降級仍只對 debuggable 應用程式生效, 這是框架規則而非外掛程式限制.

安裝指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
