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

3-Setup Installer は独立したホーム画面, AutoJs6 の入口とスクリプト, 外部アプリからのパッケージの開き方や共有を通じて Android アプリをインストール, 更新, 検査, アンインストールします. Android の確認と, Shizuku または Root による特権操作に対応します.

AutoJs6 は Binder サービスを通じてプラグインを検出し, パッケージファイルを読み取り専用のファイルディスクリプタとして渡します. プラグインはパッケージを解析し, 認可方式を選択し, 必要に応じて独自の確認と進捗ダイアログを表示し, 段階, 進捗, 結果を報告します. 特権操作は Shizuku ユーザーサービスまたは libsu Root サービス内で実行され, システムのパッケージインストーラーと直接やり取りします.

******

### 現在の状態

******

1.0.0: 開発プレビュー. 独立したホーム, 設定, インストール済みアプリ管理, 順次処理キュー, インストール履歴を提供します. インストールの確認, 進捗, 結果とフォアグラウンド通知に対応します. プロセス再起動後は保存済みの確定結果を保持し, 未完了タスクをキャンセル済みにします. 自動再開や再試行は行いません. `installer` スクリプト API には AutoJs6 >= 6.8.0 (5300) が必要です. 基本的なホスト連携にはビルド 5299 が必要です. 端末の検証範囲と残りの受け入れ項目は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください.

******

### 機能

******

この開発プレビューで利用できる機能:

- パッケージ形式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, および APK を含む ZIP アーカイブ. 分割パッケージは端末に合わせて選択され, `.aab` ファイルは認識と説明のみでインストールされません.
- 権限方式: `none` は Android の確認を使用し, `shizuku` と `root` は特権操作を提供します. `auto` は既定で利用可能な Shizuku, Root, システム確認の順に選択します. 設定で順序と有効な方式を変更できます. 明示した方式が別の方式に自動で置き換わることはありません.
- インストール成功後に元ファイルの削除を試行できます. ダウングレード, テストパッケージ, 低い targetSdk 制限の回避 (Android 14+), インストーラーの指定, 他のユーザーの選択には Shizuku または Root が必要で, Android の制限も適用されます.
- インストール済みアプリを名前またはパッケージ名で検索し, 名前, インストール日時, 更新日時で並べ替えられます. システムアプリの表示も可能です. アプリやシステムのアプリ情報を開くか, 内容を確認してアンインストールできます. Shizuku または Root では確認後に追加のシステム確認なしで削除し, 任意でデータを保持できます. その他の場合は Android の確認を使用します.
- 確認画面にアプリ情報, 新旧バージョン, 署名, 選択可能な APK コンポーネントを表示します. 進行中の処理はキャンセルでき, 結果には成功時の操作やコピー可能なエラー詳細を表示します. 一括インストールは項目ごとの状態を示します.
- 単一または複数のパッケージを開いたり共有できます. MT Manager が共有する APKS ファイルにも対応します. 複数のパッケージは順次処理キューに入り, 外部ソースの失敗項目は URI とアクセス権が利用可能な間に再試行できます.
- フォアグラウンドのインストール進捗, キャンセル操作, 結果の通知に対応します. 通知権限がなくてもインストールは妨げられません.
- 外観設定は言語, 夜間モード, テーマ色, ランチャーアイコンです. 最初の 3 項目は既定で AutoJs6 に従い, ローカルで変更できます. ホストが利用できない場合はシステムの言語と夜間モード, 既定色を使用します. アイコンは明色, 暗色, 自動, 透明の 4 モードで, 自動はシステムに従います. 表示はランチャーのキャッシュやマスクの影響を受けます.
- ホームの状態カードと設定は同じ既定インストーラーページを開きます. 特権による設定と解除, 特権がない場合のシステム設定案内を提供します. OEM の方針により変更できない場合や, 以前の処理アプリの解除が必要な場合があります. スクリプトの `installer.isDefault`, `installer.setDefault`, `setDefaultAsync` も利用でき, 端末の応答をそのまま報告します.
- スクリプト API `installer` (別名 `$installer`) は同期, `...Async`, セッション形式を提供し, 単一 / 一括 / 分割パッケージのインストール, アンインストール, 調査, 認可方式とユーザーの照会, 既定インストーラーの設定に対応します. 失敗は安定した `code` を持つ `InstallerError` です (AutoJs6 >= 6.8.0 (5300) が必要).
- 独立したホームに Shizuku/Root の利用可否と権限状態, 既定インストーラー, 進行中のタスクと最近のインストールを表示します. システムのファイル選択画面で複数のパッケージを選び, 順次インストールできます. 個別の失敗後の続行と, 残りの項目のキャンセルに対応します.
- 非公開のインストール履歴を最大 200 件保存します. パッケージ名, ラベル, 旧/新バージョン, 結果, 時刻, 起点 (ホスト/スクリプト/外部/ホーム), 権限方式と失敗情報を含みます. アプリやソースファイルを削除せずに 1 件ずつ削除, または履歴全体を消去できます. プロセス終了後の未完了項目はキャンセル済みとなり, 自動再開しません.
- 設定で権限方式の順序と有効状態, インストールオプション, 進捗通知を保存します. ホームと外部からのインストールは既定で `dialog` を使用し, 明示的に保存した `auto` または `silent` が適用されます. ホスト/スクリプトの明示オプションは維持され, スクリプト API の既定は引き続き `auto` です. 選択内容は確認後にのみ保存します.
- 設定からアプリ情報と 10 言語の内蔵リリース履歴を開けます. 手動更新確認は 12 時間間隔でプラグインの GitHub Releases API にアクセスし, 結果のキャッシュと無視するバージョンの管理に対応します. リリースページはブラウザーで開き, 更新の自動ダウンロードや自動インストールは行いません.

******

### 使い方

******

1. Android 7.0 以降の端末に公式 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) ページからプラグイン APK をインストールします. ランチャーの独立した入口からホームを開けます.
2. ホームで権限と既定インストーラーを確認し, 追加ボタンで単一または複数のパッケージを選択します. インストールダイアログの内容を確認し, ホームで進捗と最近の履歴を確認できます.
3. AutoJs6 連携にはビルド 5299 (6.8.0) 以降を使用し, プラグインセンターで `3-Setup Installer` を有効にします. スクリプト API にはビルド 5300 以降が必要です.
4. AutoJs6 のインストール操作を使用するか, パッケージを開くときや共有するときに 3-Setup Installer を選択します. 確認ダイアログが表示された場合は, アプリとオプションを確認してからインストールします. 特権方式を選ぶ場合は Shizuku または Root の認可を準備してください.
5. ホームのメニューからインストール済みアプリと設定を開けます. ローカルのインストール既定値, 外観, アイコン, 通知を確認できます. アプリ情報, リリース履歴, 手動更新確認は設定にあります.

******

### 認可方式

******

各認可方式でできることと必要なもの:

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku が動作中であること (ワイヤレスデバッグ, ADB, Root で起動) と, プラグインへの許可が必要です. shell 権限によりサイレントインストール, アンインストール, 他のユーザーに対する操作を行えます.
- `root`: Root マネージャーがプラグインに `su` を許可している必要があります. libsu Root サービスを通じて Shizuku と同じ操作を提供します. 通常 (user) ファームウェアでのダウングレードは debuggable なアプリにのみ成功しますが, これはフレームワークの規則でありプラグインの制限ではありません.
- **注意:** 特権が利用できる場合, `interaction: 'auto'` のホスト要求は確認画面を自動で開かずにサイレントインストールします. Android が確認を要求した場合, `auto` はそれを許可し `notes` に記録します. インストール前の確認には `interaction: 'dialog'`, システム確認が必要な場合に失敗させるには `interaction: 'silent'` を指定します. スクリプト API も同じ既定動作に従います.
- 設定で権限方式の順序と有効状態, インストールオプション, 進捗通知を保存します. ホームと外部からのインストールは既定で `dialog` を使用し, 明示的に保存した `auto` または `silent` が適用されます. ホスト/スクリプトの明示オプションは維持され, スクリプト API の既定は引き続き `auto` です. 選択内容は確認後にのみ保存します.

******

### クイックスタート

******

インストール, 一括処理, セッション用のテンプレート関数 (AutoJs6 >= 6.8.0 (5300) が必要). 呼び出す前に対象を選択して確認してください. この例は自動でインストール, アンインストール, 既定インストーラーの変更を実行しません.:

```js
// Read-only probe. The functions below run only when explicitly called with chosen sources.
console.log(installer.status);

// An already authorized Shizuku service is required; silent never falls back to a dialog.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// An array means independent applications, including an array containing one source.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// A source may also be { splits: [...] } for one application's split files.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};
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
- **元ファイルが削除されない理由は?** 削除はインストール成功後だけ試み, 削除失敗で成功結果を変更しません. 外部の提供元が拒否する場合があります. スクリプトの `deleteSource` はホストがパスや `file://` の元ファイルを削除し, `content://` と失敗した項目は保持します. `sourceDeleted` と `notes` を確認してください.
- **再試行や再開はできますか?** 失敗した外部 URI はソースとアクセス権が利用できる間, 再試行できます. ソースやアクセス権が解放された場合はパッケージを開き直してください. プロセスの再起動後, 復元画面には保存済みの確定結果が表示され, 未完了の項目は中断として示されます. 復元画面は読み取り専用で, インストールや再試行を自動で実行しません. 再実行する前に実際のインストール状態を確認してください.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder エントリポイントは `org.autojs.permission.PLUGIN` 署名権限で保護され, AutoJs6 だけがアクセスできます. 外部 "アプリで開く" エントリはパッケージファイルのみを受け付け, スクリプトを実行することはありません.
- REQUEST_INSTALL_PACKAGES と REQUEST_DELETE_PACKAGES は Android の確認に使用します. QUERY_ALL_PACKAGES はインストール済みアプリの管理, バージョンと署名の比較, 既定インストーラーの検出に使用します.
- FOREGROUND_SERVICE と FOREGROUND_SERVICE_DATA_SYNC はバックグラウンドのインストール処理を支え, POST_NOTIFICATIONS は進捗と結果の通知に使用します. 通知権限がなくてもインストールは妨げられません.
- Shizuku と Root はユーザーが開始した操作にのみ使われます. 特権サービスは状態を保持せず, 操作間で shell を開いたままにせず, プラグイン外部から到達されることはありません.
- インストール, 検査, 履歴, アプリ管理はオフラインで動作します. INTERNET は手動でバージョンを確認するときだけ, 12 時間間隔でプラグインの固定 GitHub Releases API にアクセスするために使用します. バックグラウンド更新確認やパッケージのアップロードは行いません.
- パッケージソースは読み取り専用で開きます. 履歴には限定されたアプリ情報と結果を保存し, パッケージの内容やソース URI は保存しません. エラー内のパスは伏せられます. 非公開ストレージはバックアップ対象外です. 履歴の削除はアプリやソースを削除しません.

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

- `ヒント` 開発プレビュー. 独立したホーム, 設定, インストール済みアプリ管理, 順次処理キュー, インストール履歴を提供します. インストールの確認, 進捗, 結果とフォアグラウンド通知に対応します. プロセス再起動後は保存済みの確定結果を保持し, 未完了タスクをキャンセル済みにします. 自動再開や再試行は行いません. `installer` スクリプト API には AutoJs6 >= 6.8.0 (5300) が必要です. 基本的なホスト連携にはビルド 5299 が必要です. 端末の検証範囲と残りの受け入れ項目は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください
- `機能` 3-Setup Installer は独立したホーム画面, AutoJs6 の入口とスクリプト, 外部アプリからのパッケージの開き方や共有を通じて Android アプリをインストール, 更新, 検査, アンインストールします. Android の確認と, Shizuku または Root による特権操作に対応します
- `機能` 10 言語の README, プラグインセンター説明, 変更履歴
- `機能` スクリプト API `installer` (別名 `$installer`) は同期, `...Async`, セッション形式を提供し, 単一 / 一括 / 分割パッケージのインストール, アンインストール, 調査, 認可方式とユーザーの照会, 既定インストーラーの設定に対応します. 失敗は安定した `code` を持つ `InstallerError` です (AutoJs6 >= 6.8.0 (5300) が必要)
- `機能` 独立したホームに Shizuku/Root の利用可否と権限状態, 既定インストーラー, 進行中のタスクと最近のインストールを表示します. システムのファイル選択画面で複数のパッケージを選び, 順次インストールできます. 個別の失敗後の続行と, 残りの項目のキャンセルに対応します
- `機能` 非公開のインストール履歴を最大 200 件保存します. パッケージ名, ラベル, 旧/新バージョン, 結果, 時刻, 起点 (ホスト/スクリプト/外部/ホーム), 権限方式と失敗情報を含みます. アプリやソースファイルを削除せずに 1 件ずつ削除, または履歴全体を消去できます. プロセス終了後の未完了項目はキャンセル済みとなり, 自動再開しません
- `機能` インストール済みアプリを名前またはパッケージ名で検索し, 名前, インストール日時, 更新日時で並べ替えられます. システムアプリの表示も可能です. アプリやシステムのアプリ情報を開くか, 内容を確認してアンインストールできます. Shizuku または Root では確認後に追加のシステム確認なしで削除し, 任意でデータを保持できます. その他の場合は Android の確認を使用します
- `機能` 設定で権限方式の順序と有効状態, インストールオプション, 進捗通知を保存します. ホームと外部からのインストールは既定で `dialog` を使用し, 明示的に保存した `auto` または `silent` が適用されます. ホスト/スクリプトの明示オプションは維持され, スクリプト API の既定は引き続き `auto` です. 選択内容は確認後にのみ保存します
- `機能` ホームの状態カードと設定は同じ既定インストーラーページを開きます. 特権による設定と解除, 特権がない場合のシステム設定案内を提供します. OEM の方針により変更できない場合や, 以前の処理アプリの解除が必要な場合があります. スクリプトの `installer.isDefault`, `installer.setDefault`, `setDefaultAsync` も利用でき, 端末の応答をそのまま報告します
- `機能` 設定からアプリ情報と 10 言語の内蔵リリース履歴を開けます. 手動更新確認は 12 時間間隔でプラグインの GitHub Releases API にアクセスし, 結果のキャッシュと無視するバージョンの管理に対応します. リリースページはブラウザーで開き, 更新の自動ダウンロードや自動インストールは行いません
- `修正` 対応するシステム翻訳がない端末で, キャンセル操作の表示がプラグインの言語に従わない問題
- `修正` base.apk などの必須分割 APK が無効状態でもチェックマークを表示するよう修正. ライト/ダークモードの両方に対応
- `修正` Files by Google などが拡張子のない content URI と汎用 ZIP/バイナリ MIME タイプを使用する場合に, インストールパッケージを開く候補にプラグインが表示されない問題を修正
- `改善` プラグイン ID, engine, サービスの action / category, Binder descriptor と最低ホストバージョンをホストの installer-api 契約定数から取得するように変更; 能力宣言にインストーラ契約バージョン 1 を追加し, 最低ホストビルドを 5299 に更新
- `改善` ランダムアクセス可能な入力元は全体のキャッシュコピーを省き, ストリームは必要に応じて一時保存します. ZIP 内の分割 APK に対応し, AAB は情報確認のみとし, 内容が変わった入力元は拒否します.
- `改善` 明示的に選んだ認証方式から別方式へは切り替えません. 拒否, タイムアウト, 非互換を区別し, 同時リクエストで認証処理と特権接続を共有します.
- `改善` インストールと更新のコア機能でシステム確認, Shizuku, Root に対応しました. キャンセルでき, 実際の確認方法とシステムの結果を返します.
- `改善` アンインストールのコア機能でシステム確認, Shizuku, Root に対応し, 特権方式ではデータを残すこともできます.
- `改善` 複数パッケージを順番にインストールし, 失敗後の続行や残りのキャンセルに対応しました. 特権方式では対象ユーザーの検証と選択ができます.
- `改善` ホストサービスから情報確認, インストール, アンインストール, ユーザー照会を利用できます. 明示的な確認, 呼び出し元の終了時のキャンセル, 最大 4 セッションの同時実行と自動クリーンアップに対応しました.
- `改善` 外観設定は言語, 夜間モード, テーマ色, ランチャーアイコンです. 最初の 3 項目は既定で AutoJs6 に従い, ローカルで変更できます. ホストが利用できない場合はシステムの言語と夜間モード, 既定色を使用します. アイコンは明色, 暗色, 自動, 透明の 4 モードで, 自動はシステムに従います. 表示はランチャーのキャッシュやマスクの影響を受けます
- `改善` バックグラウンドのインストールにフォアグラウンド実行と進捗, キャンセル, 結果の通知を追加. 通知権限がなくてもインストールは継続.
- `改善` アプリ情報, APK コンポーネント選択, オプション, エラーのコピー, 一括処理の項目別状態を備えた確認, 進捗, 結果画面を追加. プロセスの再起動後, 復元画面には保存済みの確定結果が表示され, 未完了の項目は中断として示されます. 復元画面は読み取り専用で, インストールや再試行を自動で実行しません.
- `改善` システムのインストール確認に提供元の許可案内と中断処理を追加. 特権アンインストールではアプリ情報とデータ保持の選択を確認前に表示.
- `改善` 単一または複数パッケージの表示と共有, アクセス可能な外部ソースの再試行, 成功後の任意の元ファイル削除に対応. 削除が拒否されてもインストール成功を維持.
- `改善` MT Manager から共有された APKS パッケージを開く際に application/vnd.android.package-archives MIME 型に対応
- `依存関係` Shizuku 認可方式のために Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) を追加
- `依存関係` Root 認可方式のために libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) を追加
- `依存関係` 特権サービスが非公開のパッケージインストーラー API へアクセスするために AndroidHiddenApiBypass 6.1 を追加
- `依存関係` 共有プラグイン契約として `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5298, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` `package-archive-parser.aar` と `installer-api.aar` (AutoJs6 モジュール `plugin-api/package-archive-parser` と `plugin-api/installer-api`, MPL 2.0) を追加し, `common-plugin-api.aar` とともに `locks/host-api-aars.lock` でハッシュを固定
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
