3-Setup Installer 接管 AutoJs6 的安装器: 文件管理器, 插件中心与脚本打包页的安装按钮, `.apk`, `.apks`, `.xapk`, `.apkm` 与 `.apkz` 文件的外部 "打开方式" 入口, 以及脚本侧用于安装, 更新, 检查与卸载应用的全局对象 `installer`. 除常规的系统确认外, 还可通过 Shizuku 或 Root 静默安装与卸载.

1.0.0: P2 开发预览: 安装, 包信息查询, 用户查询和卸载核心已接入宿主服务, 支持显式确认和会话自动清理. 完整宿主入口验收, 完整界面, 外部打开, 默认安装器启用, 脚本 API 和设置仍在推进. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

### 使用方法

1. 在安装了 AutoJs6 构建 5299 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `3-Setup Installer` 已被识别并启用它.
3. 在 AutoJs6 文件管理器中点击安装包, 在任意文件管理器中用 3-Setup Installer 打开安装包, 或在脚本中调用 `installer.install(...)`. 需要静默安装时, 按插件提示启动 Shizuku 或授予 Root, 或在插件设置中选择授权方式.

### 授权方式

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 应用处于运行状态 (经无线调试, ADB 或 Root 启动) 并已向插件授权; 以 shell 权限运行, 可静默安装, 静默卸载, 面向其他用户安装以及锁定默认安装器.
- `root`: 需要 Root 管理器向插件授予 `su`; 通过 libsu Root 服务提供与 Shizuku 相同的操作. 在普通 (user) 固件上降级仍只对 debuggable 应用生效, 这是框架规则而非插件限制.
- **注意:** 特权授权可用时, 脚本 API 默认静默安装, 不会主动弹出任何确认对话框. 若系统仍要求确认, `interaction: 'auto'` 会允许系统确认并写入 `notes`. 需要安装前确认时, 请显式使用 `interaction: 'dialog'`; 禁止系统确认时使用 `interaction: 'silent'`, 此时需要确认的安装会失败.

安装指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
