3-Setup Installer は AutoJs6 のインストール機能や外部からのパッケージの表示と共有要求を通じて, Android アプリのインストール, 更新, 調査, アンインストールを行います. 通常の Android 確認と, Shizuku または Root による特権インストールに対応します. スクリプト API, 独立したホームと設定ページは今後の予定です.

1.0.0: P3 開発プレビュー. 確認, 進捗, 結果, 一括処理のダイアログ, 外部から開く操作と共有, 任意の元ファイル削除, システム確認, フォアグラウンド通知を実装しています. プロセスの再起動後, 復元画面には保存済みの確定結果が表示され, 未完了の項目は中断として示されます. 復元画面は読み取り専用で, インストールや再試行を自動で実行しません. スクリプト API, 独立したホームと設定, インストール履歴, 既定インストーラーの設定は今後の予定です. 進捗と端末の確認範囲は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください. AutoJs6 >= 6.8.0 (5299).

### 使い方

1. AutoJs6 build 5299 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `3-Setup Installer` が認識されていることを確認して有効にします.
3. AutoJs6 のインストール操作を使用するか, パッケージを開くときや共有するときに 3-Setup Installer を選択します. 確認ダイアログが表示された場合は, アプリとオプションを確認してからインストールします. 特権方式を選ぶ場合は Shizuku または Root の認可を準備してください.

### 認可方式

- `none`: 標準の PackageInstaller セッション. Android は毎回ユーザーに確認を求め, 分割パッケージに対応し, 特権オプションは利用できません.
- `shizuku`: Shizuku が動作中であること (ワイヤレスデバッグ, ADB, Root で起動) と, プラグインへの許可が必要です. shell 権限によりサイレントインストール, アンインストール, 他のユーザーに対する操作を行えます.
- `root`: Root マネージャーがプラグインに `su` を許可している必要があります. libsu Root サービスを通じて Shizuku と同じ操作を提供します. 通常 (user) ファームウェアでのダウングレードは debuggable なアプリにのみ成功しますが, これはフレームワークの規則でありプラグインの制限ではありません.
- **注意:** 特権が利用できる場合, `interaction: 'auto'` のホスト要求は確認画面を自動で開かずにサイレントインストールします. Android が確認を要求した場合, `auto` はそれを許可し `notes` に記録します. インストール前の確認には `interaction: 'dialog'`, システム確認が必要な場合に失敗させるには `interaction: 'silent'` を指定します. 今後のスクリプト API も同じ既定動作に従います.

インストール手順と現在の進捗は [プロジェクト README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) を参照してください.
