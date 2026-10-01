<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Android の確認, Shizuku, Root, Dhizuku でアプリをインストール, 更新, アンインストール</p>

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

3-Setup Installer は独立したホーム画面, AutoJs6 の入口とスクリプト, 外部アプリからのパッケージの開き方や共有を通じて Android アプリをインストール, 更新, 検査, アンインストールします. Android の確認と, Shizuku, Root または Dhizuku による特権操作に対応します.

プラグインがホストとは独立してパッケージの検査, インストール, 結果表示を処理します. 対話, サイレント, 通知の各モードを選べます. 通知モードでは確認, キャンセル, 結果を通知に表示し, 必要な Android の確認画面はその通知をタップしたときだけ開きます.

******

### 現在の状態

******

1.1.0 は以下のインストール, アプリ管理, スクリプト機能を実装しています. GitHub Releases での正式公開とプラグインセンターへの登録は未完了です. ホスト連携には AutoJs6 >= 6.8.0 (5299), `installer` スクリプト API にはビルド 5300 以降が必要です. 端末の検証範囲と残りの受け入れ項目は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) に記録しています. Dhizuku, 通知インストール, 永続的な既定インストーラーのスクリプトオプションには installer V2 と AutoJs6 6.8.0 ビルド 5307 以降が必要です. 基本的なホスト連携はビルド 5299, V1 スクリプトは 5300 以降を引き続きサポートします.

******

### 機能

******

1.1.0 で実装された機能:

- パッケージ形式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, および APK を含む ZIP アーカイブ. 分割パッケージは端末に合わせて選択され, `.aab` ファイルは認識と説明のみでインストールされません.
- `none` は Android の確認を使用します. 新しい設定では `auto` が利用可能な `shizuku -> root -> dhizuku -> none` の順で選び, 順序や有効状態を変更できます. 保存済みの旧 3 方式の設定は相対順序と有効状態を保持し, Dhizuku を `none` の前に無効のまま追加します. 明示した方式から別方式への自動変更はありません.
- インストール成功後に元ファイルの削除を試行できます. ダウングレード, テストパッケージ, 低い targetSdk 制限の回避 (Android 14+), インストーラーの指定, 他のユーザーの選択には Shizuku または Root が必要で, Android の制限も適用されます.
- インストール済みアプリを名前やパッケージ名で検索し, 名前, インストール日時, 更新日時で並べ替えられます. システムアプリの表示, アプリや設定の起動, 削除の確認に対応します. Shizuku と Root はサイレント削除と任意のデータ保持に対応し, Dhizuku は現在の所有者ユーザーでの特権削除のみで `keepData` は使えません.
- 確認画面にアプリ情報, 新旧バージョン, 署名, 選択可能な APK コンポーネントを表示します. 進行中の処理はキャンセルでき, 結果には成功時の操作やコピー可能なエラー詳細を表示します. 一括インストールは項目ごとの状態を示します.
- 単一または複数のパッケージを開いたり共有できます. MT Manager が共有する APKS ファイルにも対応します. 複数のパッケージは順次処理キューに入り, 外部ソースの失敗項目は URI とアクセス権が利用可能な間に再試行できます.
- `interaction: 'notification'` はインストール専用です. 最初の確認, キャンセル, 進捗, 結果を通知で扱い, プラグインのインストールダイアログは表示しません. Android の確認には通知のタップが必要です. 通知権限, アプリの通知, インストールチャンネルが必要で, 無効なら `NOTIFICATION_UNAVAILABLE` になります. 他のモードは通知権限がなくても動作します. アンインストールでは `notification` は使えません.
- 外観設定は言語, 夜間モード, テーマ色, ランチャーアイコンです. 最初の 3 項目は既定で AutoJs6 に従い, ローカルで変更できます. ホストが利用できない場合はシステムの言語と夜間モード, 既定色を使用します. アイコンは明色, 暗色, 自動, 透明の 4 モードで, 自動はシステムに従います. 表示はランチャーのキャッシュやマスクの影響を受けます.
- 既定インストーラーページは通常の優先設定と永続ポリシーを区別します. 通常設定は Shizuku または Root を使い, ROM の制限を受けます. Dhizuku の永続設定は API 26-33 に対応し, API 34+ では所有者のコールバックを検証できないため変更前に拒否します. Root は対応端末のユーザー 0 で system UID の補助プロセスを使います. 競合する永続ポリシーは上書きしません. `persistentConfigured` は過去の設定成功の記録であり, 現在のシステムポリシーの証明ではありません. 受動的な照会は `preferred` または `none` のみを返します.
- スクリプト API `installer` (別名 `$installer`) は同期, `...Async`, セッション形式を提供し, 単一 / 一括 / 分割パッケージのインストール, アンインストール, 調査, 認可方式とユーザーの照会, 既定インストーラーの設定に対応します. 失敗は安定した `code` を持つ `InstallerError` です (AutoJs6 >= 6.8.0 (5300) が必要).
- 独立したホームに Shizuku/Root/Dhizuku の利用可否と権限状態, 既定インストーラー, 進行中のタスクと最近のインストールを表示します. システムのファイル選択画面で複数のパッケージを選び, 順次インストールできます. 個別の失敗後の続行と, 残りの項目のキャンセルに対応します.
- 非公開のインストール履歴を最大 200 件保存します. パッケージ名, ラベル, 旧/新バージョン, 結果, 時刻, 起点 (ホスト/スクリプト/外部/ホーム), 権限方式と失敗情報を含みます. アプリやソースファイルを削除せずに 1 件ずつ削除, または履歴全体を消去できます. プロセス終了後の未完了項目はキャンセル済みとなり, 自動再開しません.
- 設定に権限方式の順序と有効状態, インストールオプション, 通知設定を保存します. ホーム/外部からのインストールは既定で `dialog` を使い, `auto`, `silent`, `notification` を明示できます. ホスト UI は `dialog`, スクリプトは明示オプションと既定の `auto` を維持します. 変更は確認後に保存します.
- 設定からアプリ情報と 10 言語の内蔵リリース履歴を開けます. 手動更新確認は 12 時間間隔でプラグインの GitHub Releases API にアクセスし, 結果のキャッシュと無視するバージョンの管理に対応します. リリースページはブラウザーで開き, 更新の自動ダウンロードや自動インストールは行いません.
- `dhizuku`: Android 8.0 (API 26)+, 有効な Dhizuku デバイス/プロファイル所有者, このプラグインへの許可が必要です. 現在の所有者ユーザーのみを操作し, 実際の所有者パッケージをインストーラーとして記録します. shell/root 用のダウングレード, テストパッケージ, 低 targetSdk 制限回避, 他のユーザー, 任意のインストーラー指定, 削除時のデータ保持は非対応です. プラグインは所有者を設定しません.

******

### 使い方

******

1. Android 7.0 以降で, 正式公開後に公式 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) から APK をインストールするか, 公式インデックスへの登録後に AutoJs6 のプラグイン導入ウィザードを使用してください. 公開前のテストには管理者提供のビルドまたはソースからのビルドを使用します. ランチャーのアイコンから独立したホームを開けます.
2. ホームで権限と既定インストーラーを確認し, 単一または複数のパッケージを選択します. 選んだダイアログまたは通知で確認し, タスクと履歴を確認できます.
3. AutoJs6 連携にはビルド 5299 (6.8.0) 以降を使用し, プラグインセンターで `3-Setup Installer` を有効にします. スクリプト API にはビルド 5300 以降が必要です. Dhizuku, 通知インストール, 永続的な既定インストーラーのスクリプトオプションには installer V2 と AutoJs6 6.8.0 ビルド 5307 以降が必要です. 基本的なホスト連携はビルド 5299, V1 スクリプトは 5300 以降を引き続きサポートします.
4. AutoJs6 のインストール操作を使用するか, パッケージを開くときや共有するときに 3-Setup Installer を選択します. 確認ダイアログが表示された場合は, アプリとオプションを確認してからインストールします. 特権方式を選ぶ場合は Shizuku, Root または Dhizuku の認可を準備してください.
5. ホームのメニューからインストール済みアプリと設定を開けます. ローカルのインストール既定値, 外観, アイコン, 通知を確認できます. アプリ情報, リリース履歴, 手動更新確認は設定にあります.

******

### 認可方式

******

各認可方式でできることと必要なもの:

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku が動作中であること (ワイヤレスデバッグ, ADB, Root で起動) と, 3-Setup Installer 自体への許可が必要です. AutoJs6 への許可はこのプラグインには適用されません. インストール, アンインストール, 他のユーザーへの操作は動作中の Shizuku サービスの権限を使用します.
- `root`: Root 化した端末と, 3-Setup Installer に `su` を許可する Root 管理アプリが必要です. libsu を通じて特権インストール, アンインストール, ユーザーや既定インストーラーの操作を提供します. 各操作が許可されるかは Android と ROM のポリシーに従います.
- `dhizuku`: Android 8.0 (API 26)+, 有効な Dhizuku デバイス/プロファイル所有者, このプラグインへの許可が必要です. 現在の所有者ユーザーのみを操作し, 実際の所有者パッケージをインストーラーとして記録します. shell/root 用のダウングレード, テストパッケージ, 低 targetSdk 制限回避, 他のユーザー, 任意のインストーラー指定, 削除時のデータ保持は非対応です. プラグインは所有者を設定しません.
- **注意:** スクリプトの既定値は `interaction: 'auto'` で, 特権が利用できる場合はサイレントインストールします. ホストの UI からのインストールは `dialog` を使用します. Android が確認を要求すると, `auto` は確認を許可して `notes` に記録します. インストール前に確認するには `interaction: 'dialog'` を指定してください. 明示的な `silent` は特権がない場合やシステム確認が必要な場合に `AUTHORIZER_REQUIRED` で失敗します.
- 設定に権限方式の順序と有効状態, インストールオプション, 通知設定を保存します. ホーム/外部からのインストールは既定で `dialog` を使い, `auto`, `silent`, `notification` を明示できます. ホスト UI は `dialog`, スクリプトは明示オプションと既定の `auto` を維持します. 変更は確認後に保存します.

******

### クイックスタート

******

AutoJs6 >= 6.8.0 (5300) 向けの `install`, `installAsync`, `session`, `uninstall`, `setDefault` の例です. 関数は選択したソース, パッケージ名, 既定インストーラーの設定を渡して呼び出した場合だけ実行されます. 冒頭の状態照会は読み取り専用です.

```js
// 利用可否と互換性の読み取り専用照会.
console.log(installer.status);

// Shizuku の許可が必要. silent は Android が確認を要求すると失敗します.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// 配列は独立したパッケージを表し, 項目ごとに結果が返ります.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// base APK を含むすべての分割ファイルは 1 つのアプリに属します.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// 呼び出し時に開始. 戻り値を保持するとキャンセルや待機ができます.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// 削除するパッケージ名を指定. keepData はデータ保持を要求します.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true はこのプラグインを既定に設定, false は解除. ROM の制限があります.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// V2 の例にはホストビルド 5307 が必要. Dhizuku は有効な所有者, 通知モードは通知許可が必要です.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root の永続設定はユーザー 0 で system UID を利用できる端末向け. 競合ポリシーは保持します.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});
```

ソースにはパス, `file://`, 読み取り可能な `content://` URI を指定できます. 配列は独立した一括項目, `{ splits: [...] }` は 1 つのアプリの分割ファイルです. `session(...)` は作成時に開始し, 戻り値は `cancel()` と `wait()` を提供します. 同期呼び出しは `InstallerError` を投げる場合があり, UI スレッドでは使えません. `installer.status` の読み取り, `installer.session(...)` の作成, `session.wait()` の呼び出しも同様です. UI スレッドでは Async メソッドを使用するか, スクリプトのワーカースレッドで同期処理を実行してください. セッションオブジェクトは作成したスクリプトスレッドでのみ使用できます. Promise の拒否を処理し, 一括結果ごとの `ok` と `error` を確認してください. プラグインがない場合や非互換の場合は `PLUGIN_UNAVAILABLE` です. `setDefault` は既定設定の解除も含め, 要求した状態になったかを返します. `app.uninstall` は従来のシステム削除への入口です. 特権オプションには `installer.uninstall` を使用してください. 全オプションとイベントは [installer API ドキュメント](https://docs.autojs6.com/#/installer) を参照してください.

Dhizuku, 通知インストール, 永続的な既定インストーラーのスクリプトオプションには installer V2 と AutoJs6 6.8.0 ビルド 5307 以降が必要です. 基本的なホスト連携はビルド 5299, V1 スクリプトは 5300 以降を引き続きサポートします.

******

### 互換性

******

プラグインの能力を左右するプラットフォームの事実:

- Android 7.0 (API 24) 以降. 端末での確認状況と未確認の範囲は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) に記録しています.
- 低い targetSdk のブロック回避は Android 14 (API 34) から存在します. それより古いシステムではこのオプションは無視され, 結果に注記されます.
- 既定インストーラーページは通常の優先設定と永続ポリシーを区別します. 通常設定は Shizuku または Root を使い, ROM の制限を受けます. Dhizuku の永続設定は API 26-33 に対応し, API 34+ では所有者のコールバックを検証できないため変更前に拒否します. Root は対応端末のユーザー 0 で system UID の補助プロセスを使います. 競合する永続ポリシーは上書きしません. `persistentConfigured` は過去の設定成功の記録であり, 現在のシステムポリシーの証明ではありません. 受動的な照会は `preferred` または `none` のみを返します.
- `dhizuku`: Android 8.0 (API 26)+, 有効な Dhizuku デバイス/プロファイル所有者, このプラグインへの許可が必要です. 現在の所有者ユーザーのみを操作し, 実際の所有者パッケージをインストーラーとして記録します. shell/root 用のダウングレード, テストパッケージ, 低 targetSdk 制限回避, 他のユーザー, 任意のインストーラー指定, 削除時のデータ保持は非対応です. プラグインは所有者を設定しません.

******

### よくある質問

******

- **まだ確認が必要なのはなぜですか?** `none` は常に Android の確認を必要とし, 特権方式でも Android のポリシーが適用されます. `notification` は通知の操作からのみシステム確認を開き, Android の確認を回避しません.
- **`.aab` はインストールできますか?** できません. Android App Bundle は配布形式なので, まず bundletool で `.apks` セットに変換してください. プラグインは `.aab` ファイルを認識し, パッケージとモジュールの情報を表示します.
- **`allowDowngrade: true` でもダウングレードに失敗する理由は?** この設定は許可を要求するだけです. Android がファームウェア, 権限の実行主体, アプリの debuggable 属性に応じて判断します. 検証した user ファームウェアでは Sony G8441 / API 28 と Xiaomi 23046RP50C / API 35 が非 debuggable パッケージのダウングレードを拒否し, Sony XQ-DQ72 / API 33 の Root は許可しました. これらは各端末での結果です. エラーと `systemMessage` を確認してください. Root でもすべての ROM で成功するとは限りません.
- **HyperOS ではどのインストーラーパッケージ名を使えますか?** ADB やワイヤレスデバッグで起動した Shizuku では, 名前を指定しないと `com.android.shell` を使います. 検証した Xiaomi 23046RP50C / HyperOS / API 35 では, サイレントでの新規インストールと更新でこの値が記録されました. `com.android.shell` とプラグイン自身のパッケージ名を明示した場合も成功し, 指定どおりに記録されました. 他のパッケージ名や ROM バージョンではシステムの応答に従います.
- **ColorOS などでプラグインの有効化を求められたら?** 新規インストールや強制停止の後, Android はユーザーが操作するまでアプリを停止状態に保つことがあります. AutoJs6 のプラグインセンターで有効化の操作が表示されたら実行するか, ランチャーのアイコンから 3-Setup Installer を開いて再試行してください. これは [Android の停止状態の規則](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED) に従います. ColorOS 固有の動作は実機で未検証です.
- **既定設定が失敗したり, 保存済みの永続設定表示と現在の処理アプリが異なる理由は?** 既定インストーラーページは通常の優先設定と永続ポリシーを区別します. 通常設定は Shizuku または Root を使い, ROM の制限を受けます. Dhizuku の永続設定は API 26-33 に対応し, API 34+ では所有者のコールバックを検証できないため変更前に拒否します. Root は対応端末のユーザー 0 で system UID の補助プロセスを使います. 競合する永続ポリシーは上書きしません. `persistentConfigured` は過去の設定成功の記録であり, 現在のシステムポリシーの証明ではありません. 受動的な照会は `preferred` または `none` のみを返します.
- **元ファイルが削除されない理由は?** 削除はインストール成功後だけ試みます. インストールの失敗, キャンセル, タイムアウトでは必ず元ファイルを保持します. 削除失敗でインストールの成功結果は変わらず, 外部の提供元が削除を拒否する場合があります. スクリプトの `deleteSource` はホストがパスや `file://` の元ファイルを削除し, `content://` は保持します. `sourceDeleted` と `notes` を確認してください. 一括処理では, 他の項目が失敗したり残りがキャンセルされた場合も, 成功が確定した項目には `deleteSource` が適用されます.
- **再試行や再開はできますか?** 失敗した外部 URI はソースとアクセス権が利用できる間, 再試行できます. ソースやアクセス権が解放された場合はパッケージを開き直してください. プロセスの再起動後, 復元画面には保存済みの確定結果が表示され, 未完了の項目は中断として示されます. 復元画面は読み取り専用で, インストールや再試行を自動で実行しません. 再実行する前に実際のインストール状態を確認してください.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder エントリポイントは `org.autojs.permission.PLUGIN` 署名権限で保護され, AutoJs6 だけがアクセスできます. 外部 "アプリで開く" エントリはパッケージファイルのみを受け付け, スクリプトを実行することはありません.
- REQUEST_INSTALL_PACKAGES と REQUEST_DELETE_PACKAGES は Android の確認に使用します. QUERY_ALL_PACKAGES はインストール済みアプリの管理, バージョンと署名の比較, 既定インストーラーの検出に使用します.
- FOREGROUND_SERVICE と FOREGROUND_SERVICE_DATA_SYNC はインストールと一時的なソースアクセスを支え, POST_NOTIFICATIONS は通知に使用します. `notification` は通知とチャンネルが有効である必要があります. 他のモードでは通知権限は必須ではありません.
- Shizuku, Root, Dhizuku は要求された操作に使用します. プラグインはデバイス/プロファイル所有者を設定しません. 永続ルールは設定または解除の要求時だけ変更し, パッケージをアップロードしません.
- インストール, 検査, 履歴, アプリ管理はオフラインで動作します. INTERNET は手動でバージョンを確認するときだけ, 12 時間間隔でプラグインの固定 GitHub Releases API にアクセスするために使用します. バックグラウンド更新確認やパッケージのアップロードは行いません.
- パッケージソースは読み取り専用で開きます. 履歴には限定されたアプリ情報と結果を保存し, パッケージの内容やソース URI は保存しません. エラー内のパスは伏せられます. 非公開ストレージはバックアップ対象外です. 履歴の削除はアプリやソースを削除しません.

正式公開後のプラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

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

`ThreeSetupInstallerPluginService` は `org.autojs.plugin.INSTALLER` (category `installer`) に応答し, ホストの installer-api 契約 `org.autojs.plugin.installer.api.IInstallerPlugin` を実装します. `ThreeSetupInstallerPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo で応答します. `WakeActivity` はホストによるプラグインの有効化に使われます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.1.0

_2026/10/02_

- `機能` `dhizuku`: Android 8.0 (API 26)+, 有効な Dhizuku デバイス/プロファイル所有者, このプラグインへの許可が必要です. 現在の所有者ユーザーのみを操作し, 実際の所有者パッケージをインストーラーとして記録します. shell/root 用のダウングレード, テストパッケージ, 低 targetSdk 制限回避, 他のユーザー, 任意のインストーラー指定, 削除時のデータ保持は非対応です. プラグインは所有者を設定しません.
- `機能` 既定インストーラーページは通常の優先設定と永続ポリシーを区別します. 通常設定は Shizuku または Root を使い, ROM の制限を受けます. Dhizuku の永続設定は API 26-33 に対応し, API 34+ では所有者のコールバックを検証できないため変更前に拒否します. Root は対応端末のユーザー 0 で system UID の補助プロセスを使います. 競合する永続ポリシーは上書きしません. `persistentConfigured` は過去の設定成功の記録であり, 現在のシステムポリシーの証明ではありません. 受動的な照会は `preferred` または `none` のみを返します.
- `機能` `interaction: 'notification'` はインストール専用です. 最初の確認, キャンセル, 進捗, 結果を通知で扱い, プラグインのインストールダイアログは表示しません. Android の確認には通知のタップが必要です. 通知権限, アプリの通知, インストールチャンネルが必要で, 無効なら `NOTIFICATION_UNAVAILABLE` になります. 他のモードは通知権限がなくても動作します. アンインストールでは `notification` は使えません.
- `改善` `none` は Android の確認を使用します. 新しい設定では `auto` が利用可能な `shizuku -> root -> dhizuku -> none` の順で選び, 順序や有効状態を変更できます. 保存済みの旧 3 方式の設定は相対順序と有効状態を保持し, Dhizuku を `none` の前に無効のまま追加します. 明示した方式から別方式への自動変更はありません.
- `改善` 設定に権限方式の順序と有効状態, インストールオプション, 通知設定を保存します. ホーム/外部からのインストールは既定で `dialog` を使い, `auto`, `silent`, `notification` を明示できます. ホスト UI は `dialog`, スクリプトは明示オプションと既定の `auto` を維持します. 変更は確認後に保存します.
- `依存関係` デバイス/プロファイル所有者による権限方式用に Dhizuku API 2.6.0 (MIT) を追加
- `依存関係` `installer-api.aar` を契約 V2 (MPL 2.0) に更新. V1 の協議を維持し, 永続設定メソッドを末尾に追加. 出所と SHA-256 はサードパーティ通知に記載; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `ヒント` 1.0.0 は以下のインストール, アプリ管理, スクリプト機能を実装しています. GitHub Releases での正式公開とプラグインセンターへの登録は未完了です. ホスト連携には AutoJs6 >= 6.8.0 (5299), `installer` スクリプト API にはビルド 5300 以降が必要です. 端末の検証範囲と残りの受け入れ項目は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) に記録しています.
- `機能` 3-Setup Installer は独立したホーム画面, AutoJs6 の入口とスクリプト, 外部アプリからのパッケージの開き方や共有を通じて Android アプリをインストール, 更新, 検査, アンインストールします. Android の確認と, Shizuku または Root による特権操作に対応します
- `機能` パッケージ形式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, および APK を含む ZIP アーカイブ. 分割パッケージは端末に合わせて選択され, `.aab` ファイルは認識と説明のみでインストールされません
- `機能` インストール成功後に元ファイルの削除を試行できます. ダウングレード, テストパッケージ, 低い targetSdk 制限の回避 (Android 14+), インストーラーの指定, 他のユーザーの選択には Shizuku または Root が必要で, Android の制限も適用されます
- `機能` スクリプト API `installer` (別名 `$installer`) は同期, `...Async`, セッション形式を提供し, 単一 / 一括 / 分割パッケージのインストール, アンインストール, 調査, 認可方式とユーザーの照会, 既定インストーラーの設定に対応します. 失敗は安定した `code` を持つ `InstallerError` です (AutoJs6 >= 6.8.0 (5300) が必要). スクリプトの既定値は `interaction: 'auto'` で, 特権が利用できる場合はサイレントインストールします. ホストの UI からのインストールは `dialog` を使用します. Android が確認を要求すると, `auto` は確認を許可して `notes` に記録します. インストール前に確認するには `interaction: 'dialog'` を指定してください. 明示的な `silent` は特権がない場合やシステム確認が必要な場合に `AUTHORIZER_REQUIRED` で失敗します
- `機能` 独立したホームに Shizuku/Root の利用可否と権限状態, 既定インストーラー, 進行中のタスクと最近のインストールを表示します. システムのファイル選択画面で複数のパッケージを選び, 順次インストールできます. 個別の失敗後の続行と, 残りの項目のキャンセルに対応します
- `機能` 確認画面にアプリ情報, 新旧バージョン, 署名, 選択可能な APK コンポーネントを表示します. 進行中の処理はキャンセルでき, 結果には成功時の操作やコピー可能なエラー詳細を表示します. 一括インストールは項目ごとの状態を示します
- `機能` フォアグラウンドのインストール進捗, キャンセル操作, 結果の通知に対応します. 通知権限がなくてもインストールは妨げられません
- `機能` 単一または複数のパッケージを開いたり共有できます. MT Manager が共有する APKS ファイルにも対応します. 複数のパッケージは順次処理キューに入り, 外部ソースの失敗項目は URI とアクセス権が利用可能な間に再試行できます
- `機能` インストール済みアプリを名前またはパッケージ名で検索し, 名前, インストール日時, 更新日時で並べ替えられます. システムアプリの表示も可能です. アプリやシステムのアプリ情報を開くか, 内容を確認してアンインストールできます. Shizuku または Root では確認後に追加のシステム確認なしで削除し, 任意でデータを保持できます. その他の場合は Android の確認を使用します
- `機能` ホームの状態カードと設定は同じ既定インストーラーページを開きます. 特権による設定と解除, 特権がない場合のシステム設定案内を提供します. OEM の方針により変更できない場合や, 以前の処理アプリの解除が必要な場合があります. スクリプトの `installer.isDefault`, `installer.setDefault`, `setDefaultAsync` も利用でき, 端末の応答をそのまま報告します
- `機能` 設定で権限方式の順序と有効状態, インストールオプション, 進捗通知を保存します. ホームと外部からのインストールは既定で `dialog` を使用し, 明示的に保存した `auto` または `silent` が適用されます. ホスト/スクリプトの明示オプションは維持され, スクリプト API の既定は引き続き `auto` です. 選択内容は確認後にのみ保存します
- `機能` 外観設定は言語, 夜間モード, テーマ色, ランチャーアイコンです. 最初の 3 項目は既定で AutoJs6 に従い, ローカルで変更できます. ホストが利用できない場合はシステムの言語と夜間モード, 既定色を使用します. アイコンは明色, 暗色, 自動, 透明の 4 モードで, 自動はシステムに従います. 表示はランチャーのキャッシュやマスクの影響を受けます
- `機能` 非公開のインストール履歴を最大 200 件保存します. パッケージ名, ラベル, 旧/新バージョン, 結果, 時刻, 起点 (ホスト/スクリプト/外部/ホーム), 権限方式と失敗情報を含みます. アプリやソースファイルを削除せずに 1 件ずつ削除, または履歴全体を消去できます. プロセス終了後の未完了項目はキャンセル済みとなり, 自動再開しません
- `機能` 設定からアプリ情報と 10 言語の内蔵リリース履歴を開けます. 手動更新確認は 12 時間間隔でプラグインの GitHub Releases API にアクセスし, 結果のキャッシュと無視するバージョンの管理に対応します. リリースページはブラウザーで開き, 更新の自動ダウンロードや自動インストールは行いません
- `機能` 10 言語の README, プラグインセンター説明, 変更履歴
- `改善` ランダムアクセス可能な入力元は全体のキャッシュコピーを省き, ストリームは必要に応じて一時保存します. ZIP 内の分割 APK に対応し, AAB は情報確認のみとし, 内容が変わった入力元は拒否します
- `改善` 削除はインストール成功後だけ試みます. インストールの失敗, キャンセル, タイムアウトでは必ず元ファイルを保持します. 削除失敗でインストールの成功結果は変わらず, 外部の提供元が削除を拒否する場合があります. スクリプトの `deleteSource` はホストがパスや `file://` の元ファイルを削除し, `content://` は保持します. `sourceDeleted` と `notes` を確認してください. 一括処理では, 他の項目が失敗したり残りがキャンセルされた場合も, 成功が確定した項目には `deleteSource` が適用されます
- `改善` 同一パッケージのインストールをユーザーと認証方式をまたいで直列化. 待機中もキャンセルとタイムアウトを維持し, 単独起動時にも 24 時間を超えた未使用の一時ディレクトリを安全に削除
- `改善` 特権接続の確立中に接続が中断した場合は 1 回だけ自動で再接続; 開始済みのインストールやアンインストールは自動で繰り返さない
- `依存関係` Shizuku 認可方式のために Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) を追加
- `依存関係` Root 認可方式のために libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) を追加
- `依存関係` 特権サービスが非公開のパッケージインストーラー API へアクセスするために AndroidHiddenApiBypass 6.1 を追加
- `依存関係` 共有プラグイン契約として `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5298, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` インストール契約用に `installer-api.aar` (AutoJs6, MPL 2.0) を追加. 出所と SHA-256 はサードパーティ通知に記載
- `依存関係` APK とコンテナの検査および分割ファイルの選択用に `package-archive-parser.aar` (AutoJs6, MPL 2.0) を追加. 出所と SHA-256 はサードパーティ通知に記載

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

開発者は以下のコマンドでプラグインをビルドし検証できます. 正式公開前のテストには管理者提供のビルドまたはローカルビルドを使用してください. 正式 APK は Releases と, インデックス登録後のプラグインセンターで配布します.

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
