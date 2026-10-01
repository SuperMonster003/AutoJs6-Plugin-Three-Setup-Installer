<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>システム確認, Shizuku, Root で Android アプリをインストール, 更新, アンインストール</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語

******

現在の README.md は以下の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### はじめに

******

3-Setup Installer は AutoJs6 のインストール機能や外部からのパッケージの表示と共有要求を通じて, Android アプリのインストール, 更新, 調査, アンインストールを行います. 通常の Android 確認と, Shizuku または Root による特権インストールに対応します. スクリプト API, 独立したホームと設定ページは今後の予定です.

AutoJs6 は Binder サービスを通じてプラグインを検出し, パッケージファイルを読み取り専用のファイルディスクリプタとして渡します. プラグインはパッケージを解析し, 認可方式を選択し, 必要に応じて独自の確認と進捗ダイアログを表示し, 段階, 進捗, 結果を報告します. 特権操作は Shizuku ユーザーサービスまたは libsu Root サービス内で実行され, システムのパッケージインストーラーと直接やり取りします.

******

### 現在の状態

******

1.0.0: P3 開発プレビュー. 確認, 進捗, 結果, 一括処理のダイアログ, 外部から開く操作と共有, 任意の元ファイル削除, システム確認, フォアグラウンド通知を実装しています. スクリプト API, 独立したホームと設定, インストール履歴, 既定インストーラーの設定は今後の予定です. 進捗と端末の確認範囲は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください. AutoJs6 >= 6.8.0 (5299).

******

### 機能

******

この開発プレビューで利用できる機能. 今後の機能は明記しています:

- パッケージ形式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, および APK を含む ZIP アーカイブ. 分割パッケージは端末に合わせて選択され, `.aab` ファイルは認識と説明のみでインストールされません.
- 認可方式: `none` は Android の確認を使用し, `shizuku` と `root` は特権操作を提供します. `auto` は利用可能な Shizuku, Root, システム確認の順に選択します. インストールダイアログで認可方式を選べます.
- インストール成功後に元ファイルの削除を試行できます. ダウングレード, テストパッケージ, 低い targetSdk 制限の回避 (Android 14+), インストーラーの指定, 他のユーザーの選択には Shizuku または Root が必要で, Android の制限も適用されます.
- Shizuku または Root によるサイレントアンインストール (データ保持オプション付き). それ以外は通常のシステムダイアログを使用します.
- 確認画面にアプリ情報, 新旧バージョン, 署名, 選択可能な APK コンポーネントを表示します. 進行中の処理はキャンセルでき, 結果には成功時の操作やコピー可能なエラー詳細を表示します. 一括インストールは項目ごとの状態を示します.
- パッケージをプラグインで開くか, 1 個または複数のパッケージを共有できます. 失敗した外部ソースは URI とアクセス権が利用できる間, 項目ごとに再試行できます.
- フォアグラウンドのインストール進捗, キャンセル操作, 結果の通知に対応します. 通知権限がなくてもインストールは妨げられません.
- ダイアログは既定で AutoJs6 の言語, ナイトモード, テーマ色に従います. ホストを利用できない場合はシステムの言語とナイトモード, 既定の色を使用します.
- 今後の予定: 特権による既定インストーラーの選択と, 必要に応じたシステムの既定設定への案内.
- P4 の予定: 同期, `...Async`, セッション形式のスクリプト API `installer` (別名 `$installer`) と, 安定した `code` を持つ `InstallerError`.
- P5 の予定: 独立したホームと設定ページ, インストール履歴, インストール済みアプリの管理.

******

### 使い方

******

1. AutoJs6 build 5299 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `3-Setup Installer` が認識されていることを確認して有効にします.
3. AutoJs6 のインストール操作を使用するか, パッケージを開くときや共有するときに 3-Setup Installer を選択します. 確認ダイアログが表示された場合は, アプリとオプションを確認してからインストールします. 特権方式を選ぶ場合は Shizuku または Root の認可を準備してください.

******

### 認可方式

******

各認可方式でできることと必要なもの:

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku が動作中であること (ワイヤレスデバッグ, ADB, Root で起動) と, プラグインへの許可が必要です. shell 権限によりサイレントインストール, アンインストール, 他のユーザーに対する操作を行えます.
- `root`: Root マネージャーがプラグインに `su` を許可している必要があります. libsu Root サービスを通じて Shizuku と同じ操作を提供します. 通常 (user) ファームウェアでのダウングレードは debuggable なアプリにのみ成功しますが, これはフレームワークの規則でありプラグインの制限ではありません.
- **注意:** 特権が利用できる場合, `interaction: 'auto'` のホスト要求は確認画面を自動で開かずにサイレントインストールします. Android が確認を要求した場合, `auto` はそれを許可し `notes` に記録します. インストール前の確認には `interaction: 'dialog'`, システム確認が必要な場合に失敗させるには `interaction: 'silent'` を指定します. 今後のスクリプト API も同じ既定動作に従います.

******

### クイックスタート

******

サイレントインストール, ダウングレード許可付きの更新, セッションの監視, アンインストールを行うスクリプト (ロードマップ P4 以降で利用可能):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### 互換性

******

プラグインの能力を左右するプラットフォームの事実:

- Android 7.0 (API 24) 以降. 端末での確認状況と未確認の範囲は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) に記録しています.
- 低い targetSdk のブロック回避は Android 14 (API 34) から存在します. それより古いシステムではこのオプションは無視され, 結果に注記されます.
- 一部の OEM システムは既定インストーラーになれるアプリを制限したり, 信頼するインストーラーパッケージ名を要求します (HyperOS は `com.android.shell` を受け入れます). プラグインはシステムの応答をそのまま報告します.

******

### よくある質問

******

- **まだインストール確認が必要なのはなぜですか?** `none` は必ずシステム確認を使用します. 認可を準備してからインストールダイアログで Shizuku または Root を選択してください. Android や端末のポリシーによってはシステム確認が必要です.
- **`.aab` はインストールできますか?** できません. Android App Bundle は配布形式なので, まず bundletool で `.apks` セットに変換してください. プラグインは `.aab` ファイルを認識し, パッケージとモジュールの情報を表示します.
- **元ファイルが削除されないのはなぜですか?** 削除はインストール成功後にのみ試行し, ソースの提供元が拒否する場合があります. インストール成功の結果は変わりません. AutoJs6 などの送信アプリがソースを所有する場合, 削除はそのアプリが担当します.
- **再試行や再開はできますか?** 失敗した外部 URI はソースとアクセス権が利用できる間, 再試行できます. ソースやアクセス権が解放された場合はパッケージを開き直してください. プロセスが失われると復元画面に中断を表示し, 自動で再インストールすることはありません. 再実行する前に実際のインストール状態を確認してください.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder エントリポイントは `org.autojs.permission.PLUGIN` 署名権限で保護され, AutoJs6 だけがアクセスできます. 外部 "アプリで開く" エントリはパッケージファイルのみを受け付け, スクリプトを実行することはありません.
- REQUEST_INSTALL_PACKAGES と REQUEST_DELETE_PACKAGES は通常のインストールとアンインストールのダイアログを支え, QUERY_ALL_PACKAGES により更新前にインストール済みバージョンの表示と署名の比較ができます.
- FOREGROUND_SERVICE と FOREGROUND_SERVICE_DATA_SYNC はバックグラウンドのインストール処理を支え, POST_NOTIFICATIONS は進捗と結果の通知に使用します. 通知権限がなくてもインストールは妨げられません.
- Shizuku と Root はユーザーが開始した操作にのみ使われます. 特権サービスは状態を保持せず, 操作間で shell を開いたままにせず, プラグイン外部から到達されることはありません.
- パッケージファイルは読み取り専用で開かれます. プラグインはネットワーク要求を行わず, データを収集せず, 私有ストレージをバックアップから除外します.

プラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストおよびプラグインの開発者向けです. ホストはこれらの識別子を使ってプラグインを検出し, 互換性を交渉します:

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

`ThreeSetupInstallerPluginService` は `org.autojs.plugin.INSTALLER` (category `installer`) に応答し, ロードマップ P1 以降はホストの installer-api 契約 `org.autojs.plugin.installer.api.IInstallerPlugin` を実装します. `ThreeSetupInstallerPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo で応答します. `WakeActivity` はホストによるプラグインの有効化に使われます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/10/01_

- `ヒント` P3 開発プレビュー. 確認, 進捗, 結果, 一括処理のダイアログ, 外部から開く操作と共有, 任意の元ファイル削除, システム確認, フォアグラウンド通知を実装しています. スクリプト API, 独立したホームと設定, インストール履歴, 既定インストーラーの設定は今後の予定です. 進捗と端末の確認範囲は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください. AutoJs6 >= 6.8.0 (5299).
- `機能` プラグイン ID `three-setup-installer` (engine `installer`), INFO サービス, Wake Activity, およびホスト検出用の `org.autojs.plugin.INSTALLER` サービスの骨組み
- `機能` 10 言語の README, プラグインセンター説明, 変更履歴
- `改善` プラグイン ID, engine, サービスの action / category, Binder descriptor と最低ホストバージョンをホストの installer-api 契約定数から取得するように変更; 能力宣言にインストーラ契約バージョン 1 を追加し, 最低ホストビルドを 5299 に更新
- `改善` ランダムアクセス可能な入力元は全体のキャッシュコピーを省き, ストリームは必要に応じて一時保存します. ZIP 内の分割 APK に対応し, AAB は情報確認のみとし, 内容が変わった入力元は拒否します.
- `改善` 明示的に選んだ認証方式から別方式へは切り替えません. 拒否, タイムアウト, 非互換を区別し, 同時リクエストで認証処理と特権接続を共有します.
- `改善` インストールと更新のコア機能でシステム確認, Shizuku, Root に対応しました. キャンセルでき, 実際の確認方法とシステムの結果を返します.
- `改善` アンインストールのコア機能でシステム確認, Shizuku, Root に対応し, 特権方式ではデータを残すこともできます.
- `改善` 複数パッケージを順番にインストールし, 失敗後の続行や残りのキャンセルに対応しました. 特権方式では対象ユーザーの検証と選択ができます.
- `改善` ホストサービスから情報確認, インストール, アンインストール, ユーザー照会を利用できます. 明示的な確認, 呼び出し元の終了時のキャンセル, 最大 4 セッションの同時実行と自動クリーンアップに対応しました.
- `改善` インストールダイアログが AutoJs6 の言語, ナイトモード, テーマ色に対応. ホスト不在時の代替表示, 大きな文字, RTL レイアウトもサポート.
- `改善` バックグラウンドのインストールにフォアグラウンド実行と進捗, キャンセル, 結果の通知を追加. 通知権限がなくてもインストールは継続.
- `改善` アプリ情報, APK コンポーネント選択, オプション, エラーのコピー, 一括処理の項目別状態を備えた確認, 進捗, 結果画面を追加. プロセス消失時は中断を表示し, 自動再インストールしません.
- `改善` システムのインストール確認に提供元の許可案内と中断処理を追加. 特権アンインストールではアプリ情報とデータ保持の選択を確認前に表示.
- `改善` 単一または複数パッケージの表示と共有, アクセス可能な外部ソースの再試行, 成功後の任意の元ファイル削除に対応. 削除が拒否されてもインストール成功を維持.
- `依存関係` Shizuku 認可方式のために Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) を追加
- `依存関係` Root 認可方式のために libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) を追加
- `依存関係` 特権サービスが非公開のパッケージインストーラー API へアクセスするために AndroidHiddenApiBypass 6.1 を追加
- `依存関係` 共有プラグイン契約として `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5298, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` `package-archive-parser.aar` と `installer-api.aar` (AutoJs6 モジュール `plugin-api/package-archive-parser` と `plugin-api/installer-api`, ホスト P1 ビルド 6.8.0 / 5299, MPL 2.0) を追加し, `common-plugin-api.aar` とともに `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` 同梱のパッケージ解析器を更新し, 通常の ZIP 分割パッケージに対応

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

このセクションはソースからプラグインをビルドしたい開発者向けです. 通常のユーザーは Releases ページのビルド済み APK をインストールするだけで済みます.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM ユニットテストを実行し, インストルメンテーションテスト APK をビルドする:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする:

```powershell
.\gradlew.bat :app:assembleRelease
```

リリース成果物を収集し, ファイル名にバージョンと CRC32 ダイジェストを追加する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントのソースと生成物が同期していることを検証する (CI でも実施):

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降と Android SDK 37 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

******

### ローカライズとドキュメント生成

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

`.readme/` と `.changelog/` の言語 JSON ファイルが README, プラグインセンターの説明, 変更履歴の唯一のソースです. 常にこれらの JSON ソースを編集して `py .python/generate_markdown.py` を再実行してください. 生成された README, `plugin_instruction.md`, 変更履歴は手で編集しません. `py .python/generate_markdown.py --check` を実行するとすべての生成物を検証できます.

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE) の下で提供されます. サードパーティのコンポーネントとそのライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md) に記載しています.

******

### リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- インストーラーモジュールのドキュメント: https://docs.autojs6.com/#/installer
- InstallerX と InstallerX Revived (アーキテクチャの参考, GPL-3.0, コードの再利用なし): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
