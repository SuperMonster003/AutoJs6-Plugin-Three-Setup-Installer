3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku 或 Root 执行特权操作.

1.0.0: 开发预览, 已提供独立首页, 设置, 已安装应用管理, 串行队列及安装历史. 支持安装确认, 进度, 结果和前台通知. 进程重启后保留已确认并保存的结果, 未完成任务标为取消, 不会自动续装或重试. `installer` 脚本 API 需要 AutoJs6 >= 6.8.0 (5300); 宿主基础接入需要构建 5299. 设备覆盖与剩余验收见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

### 使用方法

1. 在 Android 7.0 或更高版本上, 从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 页面安装插件 APK. 独立启动器入口可打开首页.
2. 在首页查看授权与默认安装器状态, 点击添加按钮选择单个或多个安装包. 检查安装对话框后确认, 并在首页查看进度和最近历史.
3. 接入 AutoJs6 时, 使用构建 5299 (6.8.0) 或更高版本, 并在插件中心启用 `3-Setup Installer`. 脚本 API 需要构建 5300 或更高版本.
4. 使用 AutoJs6 的安装操作, 或在打开及分享安装包时选择 3-Setup Installer. 出现确认对话框时, 先检查应用与安装选项再确认安装. 选择特权方式时, 请准备 Shizuku 或 Root 授权.
5. 从首页菜单进入已安装应用或设置. 可检查本地安装默认项, 外观, 启动器图标和通知; 关于, 发行历史与手动更新检查位于设置中.

### 授权方式

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 正在运行 (经无线调试, ADB 或 Root 启动) 并已向插件授权. 其 shell 权限支持静默安装, 静默卸载及面向其他用户的操作.
- `root`: 需要 Root 管理器向插件授予 `su`; 通过 libsu Root 服务提供与 Shizuku 相同的操作. 在普通 (user) 固件上降级仍只对 debuggable 应用生效, 这是框架规则而非插件限制.
- **注意:** 特权可用时, 宿主请求使用 `interaction: 'auto'` 默认静默安装, 不会主动打开确认界面. 若 Android 仍要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时使用 `interaction: 'dialog'`; 禁止系统确认时使用 `interaction: 'silent'`, 此时需要确认的安装会失败. 脚本 API 沿用相同默认语义.
- 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存.

### 兼容性

- Android 7.0 (API 24) 及以上. 设备验证情况和剩余覆盖范围记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 绕过低 targetSdk 拦截自 Android 14 (API 34) 起存在; 更早的系统忽略该选项并在结果中注明.
- ROM 策略与现有默认项可能限制默认安装器更改. 1.0.0 不承诺持久锁定; 安装者包名和插件激活的注意事项见下方常见问题.

### 常见问题

- **为什么安装仍然要求确认?** `none` 始终使用系统确认. 准备好授权后, 可在安装对话框中选择 Shizuku 或 Root. Android 或设备策略仍可能要求系统确认.
- **能安装 `.aab` 吗?** 不能. Android App Bundle 是发布格式, 请先用 bundletool 转换为 `.apks` 集合. 插件会识别 `.aab` 文件并显示其包名与模块信息.
- **HyperOS 的安装者包名该怎么填?** Shizuku 经 ADB 或无线调试启动时, 不指定安装者包名会使用 `com.android.shell`. 已测 Xiaomi 23046RP50C / HyperOS / API 35 的静默新装与更新均记录此值. 显式指定 `com.android.shell` 或插件自身包名也都成功, 查询到的安装者与请求一致. 其他包名或 ROM 版本仍以系统答复为准.
- **ColorOS 或其他系统提示插件需要激活时怎么办?** 新装或强制停止后, Android 可能让应用保持停止状态, 等待用户交互. 请在 AutoJs6 插件中心使用提供的激活入口, 或从启动器图标打开 3-Setup Installer 后重试. 这遵循 [Android 的停止状态规则](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 专项行为尚未完成实机验证.
- **为什么设置默认安装器会失败?** ROM 可能拒绝更改. 旧版 Android 已有 APK 默认处理者时, 可能需要先按页面引导在系统设置中清除原处理者的默认值. 如果系统没有提供清除入口, 插件无法保证替换成功. 即使 Shizuku 或 Root 可用, 1.0.0 也不承诺持久锁定.
- **为什么来源没有删除?** 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`.
- **可以重试或恢复吗?** 失败的外部 URI 项目在来源及访问权限仍可用时可以重试. 来源或访问权限释放后, 请重新打开安装包. 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试. 再次开始前请检查应用的实际安装状态.

### 权限与安全

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 外部 "打开方式" 入口只接受安装包文件, 从不运行脚本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用于 Android 确认. QUERY_ALL_PACKAGES 用于已安装应用管理, 版本与签名比对以及默认安装器检测.
- FOREGROUND_SERVICE 与 FOREGROUND_SERVICE_DATA_SYNC 支持后台安装工作; POST_NOTIFICATIONS 用于进度与结果通知. 缺少通知权限不会阻止安装.
- Shizuku 与 Root 只用于你发起的操作; 特权服务不保存状态, 操作之间不保持打开的 shell, 也不会被插件之外的任何方访问.
- 安装, 检查, 历史和应用管理均可离线使用. INTERNET 仅在用户手动检查版本时访问插件固定的 GitHub Releases API, 间隔 12 小时. 不后台检查更新, 不上传安装包.
- 安装包来源以只读方式打开. 历史只保存有限的应用元数据与结果, 不保存安装包内容或来源 URI, 错误中的路径会脱敏. 插件私有存储不参与备份. 删除历史不会卸载对应应用或删除来源.

安装指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
