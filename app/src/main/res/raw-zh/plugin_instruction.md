3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku, Root 或 Dhizuku 执行特权操作.

1.2.0 已实现下述安装, 应用管理与脚本功能. 官方 GitHub Release 和插件中心索引准入仍待完成. 宿主接入需要 AutoJs6 >= 6.8.0 (5299), `installer` 脚本 API 需要构建 5300 或更高版本. 设备覆盖与剩余验收记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中. Dhizuku, 通知栏安装和持久默认安装器的脚本选项需要 AutoJs6 6.8.0 构建 5307 或以上及 installer V2 契约. 基础宿主接入仍支持构建 5299, V1 脚本方法从构建 5300 起可用.

高级脚本选项需要 AutoJs6 构建 5308+ 并协商 V3 与 `advanced-install-options`. 无配置覆盖时, 省略字段保持原行为; 显式 `false`/`none` 仍需对应支持. 正式发布和设备验收状态以路线图为准.

### 使用方法

1. Android 7.0 及以上可在正式发布后从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安装 APK, 或在官方索引准入后使用 AutoJs6 插件中心安装向导. 发布前可使用维护者提供的构建或从源码构建进行测试. 通过启动器图标打开独立首页.
2. 在首页检查授权与默认安装器状态, 选择单个或多个安装包. 通过所选对话框或通知确认, 并查看进行中任务与最近历史.
3. 接入 AutoJs6 时, 使用构建 5299 (6.8.0) 或更高版本, 并在插件中心启用 `3-Setup Installer`. 脚本 API 需要构建 5300 或更高版本. Dhizuku, 通知栏安装和持久默认安装器的脚本选项需要 AutoJs6 6.8.0 构建 5307 或以上及 installer V2 契约. 基础宿主接入仍支持构建 5299, V1 脚本方法从构建 5300 起可用.
4. 使用 AutoJs6 的安装操作, 或在打开及分享安装包时选择 3-Setup Installer. 出现确认对话框时, 先检查应用与安装选项再确认安装. 选择特权方式时, 请准备 Shizuku, Root 或 Dhizuku 授权.
5. 从首页菜单进入已安装应用或设置. 可检查本地安装默认项, 外观, 启动器图标和通知; 关于, 发行历史与手动更新检查位于设置中.

### 授权方式

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 正在运行 (经无线调试, ADB 或 Root 启动), 并单独向 3-Setup Installer 授权. 向 AutoJs6 授权不等于向本插件授权. 安装, 卸载及其他用户操作使用正在运行的 Shizuku 服务身份.
- `root`: 需要已 Root 的设备, 并由 Root 管理器向 3-Setup Installer 授予 `su`. 通过 libsu 提供特权安装, 卸载, 用户及默认安装器操作. 每项请求是否允许仍由 Android 和 ROM 策略决定.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.
- **注意:** 脚本默认使用 `interaction: 'auto'`, 特权可用时默认静默安装. 宿主界面的安装入口使用 `dialog`. 若 Android 要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时, 显式使用 `interaction: 'dialog'`. 显式 `silent` 在特权不可用或需要系统确认时以 `AUTHORIZER_REQUIRED` 失败.
- 设置保存授权顺序与启用状态, 安装选项及通知偏好. 首页/外部安装默认使用 `dialog`, 可显式选择 `auto`, `silent` 或 `notification`. 宿主界面安装入口使用 `dialog`; 脚本保留显式选项且默认仍为 `auto`. 修改在确认后保存.

### 兼容性

- Android 7.0 (API 24) 及以上. 设备验证情况和剩余覆盖范围记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 绕过低 targetSdk 拦截自 Android 14 (API 34) 起存在; 更早的系统忽略该选项并在结果中注明.
- 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.
- 权限授予请求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安装原因需要 API 26+, 来源标签需要 API 33+, 请求更新所有权需要 API 34+. 所有权仅能在初装时启用, 更新或其他用户已有该包时可能被忽略; false 不撤销既有 owner.
- none/Dhizuku 仅能检查当前用户的已安装签名, Shizuku/Root 进行全局查询. SharedUID 规则非空时, 无法排除其他用户已有该包会硬拒绝, 不能一次放行.

### 常见问题

- **为什么安装仍要求确认?** `none` 始终需要 Android 系统确认, 特权方式也可能受 Android 策略限制. `notification` 仅通过通知动作打开系统确认, 不绕过 Android 的确认要求.
- **能安装 `.aab` 吗?** 不能. Android App Bundle 是发布格式, 请先用 bundletool 转换为 `.apks` 集合. 插件会识别 `.aab` 文件并显示其包名与模块信息.
- **为什么设置 `allowDowngrade: true` 后仍可能降级失败?** 此选项仅请求允许降级; Android 根据固件, 授权身份及应用是否 debuggable 作出决定. 已测 user 固件中, Sony G8441 / API 28 与 Xiaomi 23046RP50C / API 35 拒绝非 debuggable 包降级, Sony XQ-DQ72 / API 33 的 Root 路径则接受. 这些结果只代表对应设备. 请检查返回的错误与 `systemMessage`; Root 不保证所有 ROM 都允许降级.
- **HyperOS 的安装者包名该怎么填?** Shizuku 经 ADB 或无线调试启动时, 不指定安装者包名会使用 `com.android.shell`. 已测 Xiaomi 23046RP50C / HyperOS / API 35 的静默新装与更新均记录此值. 显式指定 `com.android.shell` 或插件自身包名也都成功, 查询到的安装者与请求一致. 其他包名或 ROM 版本仍以系统答复为准.
- **ColorOS 或其他系统提示插件需要激活时怎么办?** 新装或强制停止后, Android 可能让应用保持停止状态, 等待用户交互. 请在 AutoJs6 插件中心使用提供的激活入口, 或从启动器图标打开 3-Setup Installer 后重试. 这遵循 [Android 的停止状态规则](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 专项行为尚未完成实机验证.
- **为什么设置默认安装器会失败, 或保存的持久标记与当前处理者不同?** 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- **为什么来源没有删除?** 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`. 批量中已确认成功的项目仍按 `deleteSource` 处理, 即使其他项目失败或剩余队列被取消.
- **可以重试或恢复吗?** 失败的外部 URI 项目在来源及访问权限仍可用时可以重试. 来源或访问权限释放后, 请重新打开安装包. 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试. 再次开始前请检查应用的实际安装状态.

### 权限与安全

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 外部 "打开方式" 入口只接受安装包文件, 从不运行脚本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用于 Android 确认. QUERY_ALL_PACKAGES 用于已安装应用管理, 版本与签名比对以及默认安装器检测. 普通权限 ENFORCE_UPDATE_OWNERSHIP 用于显式请求更新所有权, 不表示必然获得 owner.
- FOREGROUND_SERVICE 和 FOREGROUND_SERVICE_DATA_SYNC 支持安装工作及临时来源访问; POST_NOTIFICATIONS 用于通知. `notification` 交互要求通知与安装渠道可用, 其它交互模式允许缺少通知许可.
- Shizuku, Root 与 Dhizuku 用于请求的操作. 插件不会自动配置设备/资料所有者. 持久默认规则只通过请求的设置或清除操作修改; 不上传安装包.
- 安装, 检查, 历史和应用管理均可离线使用. INTERNET 仅在用户手动检查版本时访问插件固定的 GitHub Releases API, 间隔 12 小时. 不后台检查更新, 不上传安装包.
- 安装包来源以只读方式打开. 历史只保存有限的应用元数据与结果, 不保存安装包内容或来源 URI, 错误中的路径会脱敏. 插件私有存储不参与备份. 删除历史不会卸载对应应用或删除来源.
- 授予选项请求系统可授予的权限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安装器可改变的 app-op. 不保证全部声明权限, 不授予无障碍, 悬浮窗或任意签名权限. restricted/system-fixed/policy-fixed 限制仍有效, 不额外设置 restricted 权限 allowlist 标志.
- 签名门禁与本地包名/SharedUID 精确黑名单适用于全部入口, 使用 `BLOCKED_BY_POLICY`. 仅真实 dialog 可对当前项目一次放行 mismatch/unknown 签名, Android 仍会验签; 静默/通知不能放行, 黑名单不可覆盖. 权限预览显示实际选中 APK 分包声明的权限, 不代表已授予权限.

安装指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
