3-Setup Installer 通过 AutoJs6 的安装入口及安装包文件的外部打开或分享请求, 安装, 更新, 检查与卸载 Android 应用. 支持常规 Android 确认以及通过 Shizuku 或 Root 进行特权安装. 兼容宿主已提供 `installer` 脚本 API; 独立首页与设置页仍属于后续规划.

1.0.0: 开发预览. 已实现确认, 进度, 结果与批量对话框, 外部打开与分享, 可选来源删除, 系统确认及前台通知. 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试. `installer` 脚本 API 需要 AutoJs6 >= 6.8.0 (5300). 独立首页与设置, 安装历史及默认安装器设置界面仍属于后续规划. 进度与设备覆盖见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). 插件基础兼容性: AutoJs6 >= 6.8.0 (5299).

### 使用方法

1. 在安装了 AutoJs6 构建 5299 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `3-Setup Installer` 已被识别并启用它.
3. 使用 AutoJs6 的安装操作, 或在打开及分享安装包时选择 3-Setup Installer. 出现确认对话框时, 先检查应用与安装选项再确认安装. 选择特权方式时, 请准备 Shizuku 或 Root 授权.

### 授权方式

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 正在运行 (经无线调试, ADB 或 Root 启动) 并已向插件授权. 其 shell 权限支持静默安装, 静默卸载及面向其他用户的操作.
- `root`: 需要 Root 管理器向插件授予 `su`; 通过 libsu Root 服务提供与 Shizuku 相同的操作. 在普通 (user) 固件上降级仍只对 debuggable 应用生效, 这是框架规则而非插件限制.
- **注意:** 特权可用时, 宿主请求使用 `interaction: 'auto'` 默认静默安装, 不会主动打开确认界面. 若 Android 仍要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时使用 `interaction: 'dialog'`; 禁止系统确认时使用 `interaction: 'silent'`, 此时需要确认的安装会失败. 脚本 API 沿用相同默认语义.

安装指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
