******

### リリース履歴

******

# v1.0.0

###### 2026/09/30

* `ヒント` P0 開発プレビュー: リポジトリの骨組み, AutoJs6 プラグインセンターに認識されるプラグイン ID, 特権インストールの検証 (spike). Binder 契約, インストールエンジン, ダイアログ, スクリプト API, 設定画面は ROADMAP.md の段階に従って進みます.
* `機能` プラグイン ID `three-setup-installer` (engine `installer`), INFO サービス, Wake Activity, およびホスト検出用の `org.autojs.plugin.INSTALLER` サービスの骨組み
* `機能` 10 言語の README, プラグインセンター説明, 変更履歴
* `依存関係` Shizuku 認可方式のために Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) を追加
* `依存関係` Root 認可方式のために libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) を追加
* `依存関係` 特権サービスが非公開のパッケージインストーラー API へアクセスするために AndroidHiddenApiBypass 6.1 を追加
* `依存関係` 共有プラグイン契約として `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5298, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
