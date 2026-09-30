******

### 發行歷史

******

# v1.0.0

###### 2026/09/30

* `提示` P0 開發預覽: 倉庫骨架, 可被 AutoJs6 外掛程式中心識別的外掛程式身分, 以及特權安裝 spike. Binder 契約, 安裝引擎, 對話方塊, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
* `新增` 外掛程式標識 `three-setup-installer` (engine `installer`), 含 INFO 服務, Wake Activity 以及供宿主發現的 `org.autojs.plugin.INSTALLER` 服務骨架
* `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
* `優化` P0 已驗證 Shizuku 和 Root 靜默安裝, 更新, 解除安裝及一般預設安裝器設定. 宿主與腳本安裝入口尚未開放, 本版本仍不支援持久預設項.
* `優化` 插件 ID, engine, 服務 action / category, Binder descriptor 與最低宿主版本改由宿主 installer-api 契約常量提供; 能力聲明加入安裝器契約版本 1, 最低宿主建置回填為 5299
* `依賴` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用於 Shizuku 授權方式
* `依賴` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用於 Root 授權方式
* `依賴` 附加 AndroidHiddenApiBypass 6.1 用於特權服務存取隱藏的套件安裝器 API
* `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主建置 6.8.0 / 5298, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `依賴` 附加 `package-archive-parser.aar` 與 `installer-api.aar` (AutoJs6 模組 `plugin-api/package-archive-parser` 與 `plugin-api/installer-api`, 宿主 P1 建置 6.8.0 / 5299, MPL 2.0), 與 `common-plugin-api.aar` 一同在 `locks/host-api-aars.lock` 中鎖定雜湊
