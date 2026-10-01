<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 とそのスクリプトのために Android アプリをインストール, 更新, アンインストールし, Shizuku または Root によるサイレントインストールに対応</p>

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

3-Setup Installer は AutoJs6 のパッケージインストーラーを引き継ぎます: ファイルマネージャー, プラグインセンター, スクリプトパッケージ化画面のインストールボタン, `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` ファイルの外部 "アプリで開く" エントリ, そしてアプリのインストール, 更新, 検査, アンインストールを行うスクリプト側のグローバルオブジェクト `installer` です. 通常のシステム確認に加えて, Shizuku または Root によりサイレントにインストールとアンインストールを行えます.

AutoJs6 は Binder サービスを通じてプラグインを検出し, パッケージファイルを読み取り専用のファイルディスクリプタとして渡します. プラグインはパッケージを解析し, 認可方式を選択し, 必要に応じて独自の確認と進捗ダイアログを表示し, 段階, 進捗, 結果を報告します. 特権操作は Shizuku ユーザーサービスまたは libsu Root サービス内で実行され, システムのパッケージインストーラーと直接やり取りします.

******

### 現在の状態

******

1.0.0: P2 開発プレビュー: インストール, パッケージ情報とユーザーの照会, アンインストールの基本機能をホストサービスに接続し, 明示的な確認とセッションの自動クリーンアップに対応. ホスト側の入口の総合検証, 完全な画面, 外部から開く機能, 既定のインストーラーの有効化, スクリプト API と設定は開発中です. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### 機能

******

ロードマップの段階に沿って提供する予定の機能:

- パッケージ形式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, および APK を含む ZIP アーカイブ. 分割パッケージは端末に合わせて選択され, `.aab` ファイルは認識と説明のみでインストールされません.
- 認可方式: `none` (ユーザー確認付きのシステム PackageInstaller セッション), `shizuku`, `root`. `auto` は設定で構成した順序で最初に利用可能なものを選び, スクリプトから明示的に指定することもできます.
- インストールオプション: 一括インストール, 成功後にソースファイルを削除, ダウングレードを許可, テスト専用パッケージを許可, 低い targetSdk のブロックを回避 (Android 14+), インストーラーパッケージ名と対象ユーザー (特権認可方式のみ).
- Shizuku または Root によるサイレントアンインストール (データ保持オプション付き). それ以外は通常のシステムダイアログを使用します.
- 既定のインストーラーに設定: Shizuku または Root がある場合, プラグインがパッケージファイルの優先ハンドラーになります. 特権がない場合はシステムの "デフォルトで開く" ページを開きます.
- スクリプト API `installer` (別名 `$installer`) は同期形, `...Async` 形, セッション形を提供し, すべての失敗は安定した `code` を持つ `InstallerError` です.

******

### 使い方

******

1. AutoJs6 build 5299 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `3-Setup Installer` が認識されていることを確認して有効にします.
3. AutoJs6 ファイルマネージャーでパッケージファイルをタップするか, 任意のファイルマネージャーから 3-Setup Installer でパッケージを開くか, スクリプトから `installer.install(...)` を呼び出します. サイレントインストールには, プラグインの案内に従って Shizuku を起動するか Root を許可するか, プラグイン設定で認可方式を選びます.

******

### 認可方式

******

各認可方式でできることと必要なもの:

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku アプリが起動中 (ワイヤレスデバッグ, ADB, または Root で開始) で, プラグインに権限が付与されている必要があります. shell 権限で動作し, サイレントインストール, サイレントアンインストール, 他ユーザーへのインストール, 既定インストーラーのロックが可能です.
- `root`: Root マネージャーがプラグインに `su` を許可している必要があります. libsu Root サービスを通じて Shizuku と同じ操作を提供します. 通常 (user) ファームウェアでのダウングレードは debuggable なアプリにのみ成功しますが, これはフレームワークの規則でありプラグインの制限ではありません.
- **注意:** 特権が利用可能な場合, スクリプト API はデフォルトでサイレントインストールを行い, 確認ダイアログを自発的に表示しません. Android が確認を要求する場合は, `interaction: 'auto'` がシステム確認を許可し, `notes` に記録します. インストール前の確認には `interaction: 'dialog'` を明示してください. `interaction: 'silent'` はシステム確認を表示せず, 確認が必要な場合は失敗します.

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

- Android 7.0 (API 24) 以降. ホストビルドとプラグインは [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) に記載の端末マトリクスで一緒に検証されます.
- 低い targetSdk のブロック回避は Android 14 (API 34) から存在します. それより古いシステムではこのオプションは無視され, 結果に注記されます.
- 一部の OEM システムは既定インストーラーになれるアプリを制限したり, 信頼するインストーラーパッケージ名を要求します (HyperOS は `com.android.shell` を受け入れます). プラグインはシステムの応答をそのまま報告します.

******

### よくある質問

******

- **なぜインストール時にまだ確認を求められるのですか?** `none` 認可方式は常にシステム確認を経由します. Shizuku を起動するか Root を許可し, 設定でその認可方式を選ぶか, スクリプトで `authorizer: 'shizuku'` を渡してください.
- **`.aab` はインストールできますか?** できません. Android App Bundle は配布形式なので, まず bundletool で `.apks` セットに変換してください. プラグインは `.aab` ファイルを認識し, パッケージとモジュールの情報を表示します.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder エントリポイントは `org.autojs.permission.PLUGIN` 署名権限で保護され, AutoJs6 だけがアクセスできます. 外部 "アプリで開く" エントリはパッケージファイルのみを受け付け, スクリプトを実行することはありません.
- REQUEST_INSTALL_PACKAGES と REQUEST_DELETE_PACKAGES は通常のインストールとアンインストールのダイアログを支え, QUERY_ALL_PACKAGES により更新前にインストール済みバージョンの表示と署名の比較ができます.
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

- `ヒント` P2 開発プレビュー: インストール, パッケージ情報とユーザーの照会, アンインストールの基本機能をホストサービスに接続し, 明示的な確認とセッションの自動クリーンアップに対応. ホスト側の入口の総合検証, 完全な画面, 外部から開く機能, 既定のインストーラーの有効化, スクリプト API と設定は開発中です.
- `機能` プラグイン ID `three-setup-installer` (engine `installer`), INFO サービス, Wake Activity, およびホスト検出用の `org.autojs.plugin.INSTALLER` サービスの骨組み
- `機能` 10 言語の README, プラグインセンター説明, 変更履歴
- `改善` P0 で Shizuku と Root によるサイレントインストール, 更新, アンインストール, 通常の既定インストーラー設定を検証しました. ホストとスクリプトからのインストールはまだ利用できず, 永続的な既定設定は本バージョンの対象外です.
- `改善` プラグイン ID, engine, サービスの action / category, Binder descriptor と最低ホストバージョンをホストの installer-api 契約定数から取得するように変更; 能力宣言にインストーラ契約バージョン 1 を追加し, 最低ホストビルドを 5299 に更新
- `改善` ランダムアクセス可能な入力元は全体のキャッシュコピーを省き, ストリームは必要に応じて一時保存します. ZIP 内の分割 APK に対応し, AAB は情報確認のみとし, 内容が変わった入力元は拒否します.
- `改善` 明示的に選んだ認証方式から別方式へは切り替えません. 拒否, タイムアウト, 非互換を区別し, 同時リクエストで認証処理と特権接続を共有します.
- `改善` インストールと更新のコア機能でシステム確認, Shizuku, Root に対応しました. キャンセルでき, 実際の確認方法とシステムの結果を返します.
- `改善` アンインストールのコア機能でシステム確認, Shizuku, Root に対応し, 特権方式ではデータを残すこともできます.
- `改善` 複数パッケージを順番にインストールし, 失敗後の続行や残りのキャンセルに対応しました. 特権方式では対象ユーザーの検証と選択ができます.
- `改善` ホストサービスから情報確認, インストール, アンインストール, ユーザー照会を利用できます. 明示的な確認, 呼び出し元の終了時のキャンセル, 最大 4 セッションの同時実行と自動クリーンアップに対応しました.
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
