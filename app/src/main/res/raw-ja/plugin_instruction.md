3-Setup Installer は AutoJs6 のパッケージインストーラーを引き継ぎます: ファイルマネージャー, プラグインセンター, スクリプトパッケージ化画面のインストールボタン, `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` ファイルの外部 "アプリで開く" エントリ, そしてアプリのインストール, 更新, 検査, アンインストールを行うスクリプト側のグローバルオブジェクト `installer` です. 通常のシステム確認に加えて, Shizuku または Root によりサイレントにインストールとアンインストールを行えます.

1.0.0: P2 開発プレビュー: インストール, パッケージ情報とユーザーの照会, アンインストールの基本機能をホストサービスに接続し, 明示的な確認とセッションの自動クリーンアップに対応. ホスト側の入口の総合検証, 完全な画面, 外部から開く機能, 既定のインストーラーの有効化, スクリプト API と設定は開発中です. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### 使い方

1. AutoJs6 build 5299 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `3-Setup Installer` が認識されていることを確認して有効にします.
3. AutoJs6 ファイルマネージャーでパッケージファイルをタップするか, 任意のファイルマネージャーから 3-Setup Installer でパッケージを開くか, スクリプトから `installer.install(...)` を呼び出します. サイレントインストールには, プラグインの案内に従って Shizuku を起動するか Root を許可するか, プラグイン設定で認可方式を選びます.

### 認可方式

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku アプリが起動中 (ワイヤレスデバッグ, ADB, または Root で開始) で, プラグインに権限が付与されている必要があります. shell 権限で動作し, サイレントインストール, サイレントアンインストール, 他ユーザーへのインストール, 既定インストーラーのロックが可能です.
- `root`: Root マネージャーがプラグインに `su` を許可している必要があります. libsu Root サービスを通じて Shizuku と同じ操作を提供します. 通常 (user) ファームウェアでのダウングレードは debuggable なアプリにのみ成功しますが, これはフレームワークの規則でありプラグインの制限ではありません.
- **注意:** 特権が利用可能な場合, スクリプト API はデフォルトでサイレントインストールを行い, 確認ダイアログを自発的に表示しません. Android が確認を要求する場合は, `interaction: 'auto'` がシステム確認を許可し, `notes` に記録します. インストール前の確認には `interaction: 'dialog'` を明示してください. `interaction: 'silent'` はシステム確認を表示せず, 確認が必要な場合は失敗します.

インストール手順と現在の進捗は [プロジェクト README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください.
