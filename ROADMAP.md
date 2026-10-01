# AutoJs6 3-Setup Installer 插件 Roadmap

本文是 `AutoJs6-Plugin-Three-Setup-Installer` (显示名 `3-Setup Installer`; 将 AutoJs6 宿主的安装器能力整体迁出为独立插件, 补齐 Shizuku / Root 特权安装, 并为脚本提供全局对象 `installer`) 的可执行状态表.
以 2026-09-30 的宿主本地代码快照 (`AutoJs6 master@c8047fa1bf`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5298`), 平台版本插件 `1.8.3`, 参考项目 InstallerX (`iamr0s/InstallerX`, 2023-08-04 归档, GPL-3.0) 与 InstallerX-Revived (`wxxsfxyzm/InstallerX-Revived`, `main`, GPL-3.0) 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 授权方式), 证据等级见附录 E.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(兄弟)` `D:/idea-projects/AutoJs6-Plugin-APK-Inspector`, `(文档)` 文档 / d.ts / Ace / 离线文档四个关联仓库, `(索引)` `D:/idea-projects/AutoJs6-Official-Plugins-Index`, `(测试)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 D 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策".
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 同时遵循 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` (Three 系列命名), `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` (独立设置页) 与 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标); 新仓库 AGENTS 的裁剪版即本仓库 `AGENTS.md`; 之后的工程约定以 `AGENTS.md` 为准, 本文件只记录 "改什么" 与证据.
7. 参考项目 InstallerX / InstallerX-Revived 均为 GPL-3.0, 本仓库为 MPL-2.0: 只参考其架构, 行为与选项目录, 不复制其源码, 资源与文案 (D29).

---

## 1. 固定决策

以下决策 D1-D12 已由维护者于 2026-09-30 分三轮确认, 后续阶段不再重新讨论; D13-D31 为据此派生的技术决策, D32-D36 为维护者于 2026-09-30 对附录 D 与首页设计的拍板, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 仓库 `AutoJs6-Plugin-Three-Setup-Installer`, 显示名 `3-Setup Installer` (Three 系列, `ThreeSetup` 恰好 10 个字母); 脚本全局对象 `installer` (别名 `$installer`); 插件 ID `three-setup-installer`, engine `installer`, variant `default`; applicationId 与 Kotlin 包 `io.github.supermonster003.autojs6.plugin.three.setup.installer` (点分写法, 与 3-Stove Agent / 3-Adapt A11y 一致); 机器标识写 `three` / `Three`, 面向人的文本写 `3-Setup Installer`, 宿主向导回退标题写 `Three Setup Installer`. 图标源图由维护者提供两张黑白透明 PNG (亮色模式深色图案, 暗色模式浅色图案), 已入库为 `.python/icons/three-setup-ic-launcher-{light,dark}.png` |
| D2 | 授权方式 | 1.0.0 纳入 `none` (系统 PackageInstaller 会话 + 用户确认), `shizuku`, `root` 三种; Dhizuku 列入 1.1.0 (P8) |
| D3 | 宿主保留范围 | 能力迁出后, 宿主 (无插件时) 只保留: 入口 (文件管理器安装按钮, 插件中心安装, 打包后安装, 自更新), 安装包只读信息 (`ApkInfoDialogManager` 与清单查看), 以及交给系统安装器的交接; 不再保留会话式安装器, 状态存储, 通知与恢复提示等完整实现 |
| D4 | 外部入口 | "用 AutoJs6 打开 APK" 的导出意图过滤器 (`ACTION_VIEW` / `ACTION_INSTALL_PACKAGE`, 安装包 MIME 与扩展名) 仅由插件注册; 宿主删除 `PackageInstallerEntryActivity` 及 `installerEnabled` 占位符, 不再出现在系统的安装器选择列表 |
| D5 | 调用风格 | 脚本 API 采用同步 + Async 双形态 (`install` / `installAsync` 等, 参数与结果一致); 另提供会话形态 `installer.session(...)` 返回 EventEmitter 以监听阶段与进度 |
| D6 | 1.0.0 特权选项 | 批量安装 + 安装后删除源文件; 特权静默卸载; 安装标志: 允许降级 / 允许测试包 / 绕过低 targetSdk 拦截; 安装者包名 + 目标用户选择. 未选的能力 (通知栏安装, 授予全部运行时权限, 请求更新所有权, DexOpt, 签名门禁, 黑名单, 权限预览, 按来源配置文件) 列入 P8 / P9 |
| D7 | 界面归属 | 安装过程的确认 / 进度 / 结果对话框由插件拥有; 宿主与脚本只发起请求并接收结果 |
| D8 | 授权语义 | `authorizer` 默认 `'auto'` (按 D14 顺序自动选择可用者), 脚本可显式指定 `'none' | 'shizuku' | 'root'`, 显式指定时不回退 |
| D9 | AAB | 识别 `.aab` 并给出信息 (格式, 包名, 版本, 模块), 但不安装, 报 `UNSUPPORTED_FORMAT` 并说明需要 bundletool 生成 `.apks` |
| D10 | 解析代码 | 宿主与 APK Inspector 各自持有的 `AndroidPackageArchive` / `BundletoolTocDecoder` 等近似副本抽取为共享 AAR (宿主模块 `plugin-api/package-archive-parser`), 本插件与宿主消费同一份; APK Inspector 的迁移为后续条目 |
| D11 | 特权服务 | Shizuku `UserService` + libsu `RootService`, 两者实现同一 AIDL `IPrivilegedInstaller`, 通过隐藏 API `IPackageInstaller` / `IPackageManager` / `IUserManager` 完成安装, 卸载, 用户列表与默认安装器锁定 |
| D12 | 默认安装器 | 1.0.0 包含 "设为默认安装器" 的特权锁定 (Shizuku / Root); 无特权时引导到系统 "默认打开" 设置 |
| D13 | 身份派生表 | 见第 4.4 节; 全部值在 Gradle, Manifest, 契约常量, 资源, 文档, 测试, 宿主注册与官方索引中 MUST 一致 |
| D14 | `auto` 顺序 | `shizuku` (已安装, 服务运行中, 已授权) -> `root` (`su` 可用且授权成功) -> `none`; 顺序与各项启用状态可在插件设置页调整并作为默认; 脚本显式 `authorizer` 优先于设置 |
| D15 | 特权进程实现 | 主路径为 Binder 隐藏 API (按 AOSP 签名自写 `priv/hidden` 反射适配器, 调用设备自带的 Stub, 不打包 `android.*` 类, `HiddenApiBypass` 解除限制), 不用 `pm install` 命令拼接; P0.2 已在 Shizuku API 24 / 31 / 35 与 Root API 28 验证, 不触发 `pm` 退路; 后续 OEM 失败时按附录 E.2 重新评估 |
| D16 | 数据传递 | 宿主 -> 插件用只读 `ParcelFileDescriptor` + JSON 元数据, 不传绝对路径; 插件 -> 特权进程用普通 pipe PFD, 特权进程通过框架 `PackageInstaller.Session.openWrite` 流式写入并 fsync, 兼容旧版 FileBridge, 避免 reliable pipe 附带的 socket 被 Magisk / app SELinux 边界拒绝 (P0.2 实测); 外部 `ACTION_VIEW` 入口的 `content://` / `file://` 由插件自行打开 |
| D17 | `none` 路径 | 标准 `PackageInstaller.Session` (`MODE_FULL_INSTALL`, API 31+ `USER_ACTION_REQUIRED`, API 33+ `PACKAGE_SOURCE_LOCAL_FILE`), `STATUS_PENDING_USER_ACTION` 的系统确认由插件自身 Activity 接管; 插件持有 `REQUEST_INSTALL_PACKAGES` 与 `REQUEST_DELETE_PACKAGES` |
| D18 | 交互模式 | `interaction: 'auto' | 'dialog' | 'silent'`; `auto` 在特权可用时优先静默, 若系统仍要求确认则允许确认并在 `notes` 记录, 实际结果的 `interaction` 为 `dialog` (2026-10-01 维护者确认); 无特权时为 `dialog`. 显式 `silent` 在无特权或系统要求确认时返回 `AUTHORIZER_REQUIRED`, 不降级为对话框; 通知栏模式列入 P8 |
| D19 | 脚本 API 形态 | `install` / `installAsync`, `session`, `uninstall` / `uninstallAsync`, `inspect` / `inspectAsync`, `authorizer` (状态 / 请求), `isDefault` / `setDefault` / `setDefaultAsync`, `users`, `isAvailable`; 错误类型 `InstallerError` (`code`, `status`, `systemMessage`); 草案见附录 A |
| D20 | 分包与批量 | 数组参数 = 多个独立安装包的批量安装, 逐个返回结果; `{ splits: [...] }` 对象 = 一个应用的分包集合, 作为一个会话安装 |
| D21 | 宿主退化路由 | 宿主新增 `core/plugin/installer/PackageInstallRouter` 单入口, 全部安装调用方改走它: 插件可用 -> Binder 会话; 不可用 -> `ACTION_VIEW` + FileProvider URI 交给系统安装器, 并在插件中心 / 文件管理器提示安装 3-Setup Installer |
| D22 | 安装完成观察 | 宿主向导与插件中心等待安装完成改为双通道: 插件会话回调 (插件路径) + `PACKAGE_ADDED` / `PACKAGE_REPLACED` 动态广播 (系统安装器路径), 不再依赖被删除的 `PackageInstallStatusCoordinator` |
| D23 | 锁定实现 | Shizuku / Root 路径调用 `IPackageManager.addPreferredActivity`, 对 `ACTION_VIEW` / `ACTION_INSTALL_PACKAGE` x `content` / `file` 共四个 APK MIME filter 逐一设置并解析确认; 六参数重载用 `removeExisting=true` 精确替换. API 24 / 28 的五参数重载不能精确替换 MIME 首选项, 仅清除本插件首选项; 若竞争者已有匹配默认项则返回 `DEFAULT_REQUIRES_CLEAR=-1`, 由 P5 引导先在系统设置清除. 不清除其它包的所有默认项. P0.2 实测 shell / root 调用 `addPersistentPreferredActivity` 均抛 `SecurityException`, 1.0.0 不承诺持久锁定, 仍列 P8 |
| D24 | 卸载实现 | 特权: `IPackageInstaller.uninstall(VersionedPackage, callerPackage, flags, IntentSender, userId)`, 支持 `keepData` (`DELETE_KEEP_DATA`) 与 `user` (含 `all` -> `DELETE_ALL_USERS`); `none`: `ACTION_UNINSTALL_PACKAGE` + `EXTRA_RETURN_RESULT` 由插件 Activity 接管. 宿主既有 `app.uninstall` (`ACTION_DELETE`) 保持不变 |
| D25 | 安装后删除 | `deleteSource` 由文件所有者执行: 脚本 API 的路径来源由宿主在收到 `COMPLETED` 后删除; 插件外部入口收到的 `content://` 用 `ContentResolver.delete` / `DocumentsContract.deleteDocument` 尽力删除, 失败只记录不报错. 维护者于 2026-10-01 确认 `keepSourceOnFailure` 为固定策略: 安装失败, 取消或超时的未成功项始终保留来源, 不新增可关闭的设置或公开选项; 已确认成功项仍按前述逐项删除规则处理 |
| D26 | 插件 UI 形态 | 安装对话框为插件自有 Activity (对话框主题, `excludeFromRecents`, 独立 task), 三段: 确认 (图标, 名称, 包名, 版本 旧 -> 新, 大小, targetSdk, 签名匹配状态, 分包选择, 选项开关), 进度, 结果; 后台阶段用前台服务通知承载进度 |
| D27 | 设置页 | 遵循独立设置页规范: 外观四项 (语言 / 夜间 / 主题色 / 启动器图标, 默认跟随 AutoJs6) 在前; 安装组: 授权方式顺序与启用, 默认交互, 默认选项 (降级 / 测试包 / 绕过低 targetSdk / 安装者包名 / 目标用户 / 安装后删除), 默认安装器状态卡; 通知; 关于 / 发行历史 / 更新检查 |
| D28 | 共享解析 AAR 边界 | `plugin-api/package-archive-parser` 只含格式识别, 容器元数据, `.apks` TOC 解码与分包选择, APK / AAB 清单显示解码, 签名方案探测, 检查上限 (宿主 `ui/main/scripts` 的 6 个文件及其 JVM 测试); APK Inspector 专有的签名验证, 原生库摘要, 16 KB 就绪度, DEX 摘要不进入 |
| D29 | 许可证边界 | 插件 MPL-2.0; InstallerX 系 GPL-3.0 只做架构 / 行为 / 选项目录参考, 不复制源码, 资源, 字符串; 隐藏 API 存根按 AOSP 接口签名自写并在 `THIRD_PARTY_NOTICES.md` 注明 Apache-2.0 来源; 第三方运行时依赖: Shizuku-API 13.1.5, libsu (建仓时 Maven Central 最新稳定版), HiddenApiBypass 6.1 |
| D30 | 错误码与阶段 | 错误码集合与安装阶段枚举见附录 B.4 / B.5, 宿主 `installer-api` 与脚本 `InstallerError.code` 使用同一字符串 |
| D31 | 契约上限 | 单次批量 <= 32 个安装包, 单个分包集合 <= 64 个文件, JSON 文档 <= 64 KiB, 安装者包名 <= 255 字节, 用户确认默认超时 5 分钟, 单会话默认超时 30 分钟, 并发会话 <= 4 |
| D32 | 附录 D 拍板 (2026-09-30) | Q1 / Q2 / Q4 / Q5 / Q6 / Q9 按推荐值实施; Q3 按推荐值且文档必须明确提示 "特权可用时脚本默认静默安装"; Q4 另要求 `app.uninstall` 与 `installer.uninstall` 的文档互相提示对方的存在与用途简述; Q8 推送按最新指示执行, 当前插件与宿主均仅本地提交, 暂不推送远端 (2026-10-01 更新) |
| D33 | 首页 | 插件拥有首页 `HomeActivity` (Three 系列惯例, 即 launcher 入口): 顶部两张状态卡 (授权方式: Shizuku / Root 可用与授权状态, 一键请求授权; 默认安装器: 当前处理者, 锁定 / 解锁), 下方为进行中与最近安装任务列表, FAB 选择安装包 (多选进入批量队列), 顶栏溢出菜单提供已安装应用与设置; 更新检查与发行历史保留在设置页 |
| D34 | 安装历史 | 持久化到插件私有存储, 上限 200 条 (包名 / 版本 / 结果 / 时间 / 来源 (宿主 / 脚本 / 外部 / 首页) / 授权方式 / 失败时的错误码与系统消息), 可单条删除与清空, 不保存安装包内容, 进程重建后恢复 |
| D35 | 默认安装器入口 | 首页状态卡可直接锁定 / 解锁; 设置页保留一行进入同一 `DefaultInstallerActivity` 详情页 (无特权时的系统设置引导与 OEM 限制说明), 取代原 Q7 的 "仅设置页一行" |
| D36 | 首页附加能力 | 已安装应用列表页 (搜索 / 排序, 特权静默卸载与保留数据开关, 复用 P2.4 引擎); `ACTION_SEND` / `SEND_MULTIPLE` 接收安装包进入安装对话框 (与 P3.2 的 `ACTION_VIEW` 并列); 批量安装队列 (多选后逐个安装, 首页显示队列进度, 可取消剩余与跳过失败项, 复用 P2.5 批量语义) |

---

## 2. 范围与非目标

范围 (1.0.0):

- 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz`, 含 APK 的 `.zip`; `.aab` 只识别不安装 (D9).
- 授权方式: `none` / `shizuku` / `root`, `auto` 自动选择 (D2, D14).
- 安装选项: 批量, 分包集合, 安装后删除源文件, 允许降级, 允许测试包, 绕过低 targetSdk 拦截, 安装者包名, 目标用户 (当前 / 指定 / 全部) (D6).
- 特权静默卸载, 保留数据 (D6, D24).
- 设为默认安装器 (D12, D23).
- 插件自有安装对话框, 外部 `ACTION_VIEW` 入口, 独立设置页 (D7, D4, D27).
- 宿主: 契约模块, 共享解析 AAR, 退化路由, 删除旧安装器, 脚本 API `installer`, 文档 / d.ts (D10, D21, D19).

非目标 (1.0.0, 列入 P8 / P9 或不做):

- Dhizuku 授权 (P8), 通知栏安装模式 (P8), 持久化默认安装器 (P8).
- 授予全部运行时权限, 请求更新所有权, DexOpt, 签名门禁, 黑名单, 权限预览, 按来源配置文件, Live Activity (P9).
- 作为系统包管理器 (`priv-app`) 运行, LSPosed 模块配合, OEM 限制绕过: 不做.
- 宿主的 `app.uninstall` / `app.getApkInfo` 语义变更: 不做, 只补充 `installer` 的新能力.
- 替换 Shizuku 脚本全局对象或宿主 `WrappedShizuku`: 不做, 插件自持 Shizuku 客户端.

---

## 3. 现状诊断

### 3.1 宿主安装器现状 (`app/src/main/java/org/autojs/autojs/ui/main/scripts/`)

| 文件 | 行数 | 角色 | 迁出后 |
| --- | --- | --- | --- |
| `PackageInstallerActivity.kt` | 633 | 非导出协调者: 导入到 `cacheDir/package-installer/`, 检查包, 请求未知来源权限, 提交会话; 同一时间只允许一个操作 (`OPERATION_ACTIVE`); companion 提供 `install(context, File)`, `install(context, Uri)`, `openExternalPackage`, `addAbortListener`, `supportedMimeType`, 扩展名表 `apk, apks, xapk, apkm, apkz, aab`, MIME 表, 内部 action `org.autojs.autojs.action.INSTALL_LOCAL_PACKAGE` / `INSTALL_PACKAGE_URI` / `VIEW_EXTERNAL_PACKAGE` | 删除 (能力进插件) |
| `PackageInstallerEntryActivity.kt` | 46 | 导出的 `ACTION_VIEW` / `ACTION_INSTALL_PACKAGE` 入口, Manifest 两组 filter (7 种 MIME; zip / octet-stream + `pathPattern`), `android:enabled="${installerEnabled}"` (app 为 true, inrt 为 false) | 删除 (D4) |
| `AndroidPackageSessionInstaller.kt` | 191 | `install(context, archive, stagingDirectory): Int`; `MODE_FULL_INSTALL`, `USER_ACTION_REQUIRED` (S+), `PACKAGE_SOURCE_LOCAL_FILE` (T+); 签名者与分包一致性检查 | 删除, 逻辑在插件 `none` 路径重写 (D17) |
| `PackageInstallStatusCoordinator.kt` / `Receiver.kt` / `Activity.kt` / `Store.kt` | 228 / 14 / 274 / 109 | 回调 intent, 通知通道 `package-installation-status`, 恢复提示, 监听器; 系统确认 intent sender 拉起与结果展示; `noBackupFilesDir/package-install-status.json` | 删除 (D3); 向导等待改 D22 |
| `AndroidPackageArchive.kt` | 1137 | 格式枚举, `stageSelectedApks`, `createDisplayApk`, `PackageDeviceSpec.from`, `AndroidPackageArchiveInspector.inspect`, `ManifestSummaryParser` | 抽入共享 AAR (D28) |
| `BundletoolTocDecoder.kt` / `ApkManifestDisplayDecoder.kt` / `AabManifestDisplayDecoder.kt` / `ApkSignatureDetector.kt` / `PackageInspectionLimits.kt` | 1730 / 1027 / 934 / 168 / 14 | `.apks` TOC 选择, 二进制 XML 与 protobuf 清单解码, 签名方案探测, 上限 | 抽入共享 AAR (D28) |
| `ApkInfoDialogManager.kt` | 504 | 只读信息对话框, 带 "安装" 按钮 (L132-135), 入口 `showApkInfoDialog` / `showApkInfoDialogWithActions` / `showPackageInfoDialog(installAction, archiveActions)` | 保留 (D3), 安装按钮改走路由 (D21) |
| `DisplayManifestActivity.kt` | 135 | 清单查看 | 保留 |

调用方 (全部改走 `PackageInstallRouter`, D21): `App.kt:140` (`PackageInstallStatusCoordinator.initialize`, 删除), `network/UpdateChecker.java:283` (自更新), `ui/project/BuildActivity.java:3138` 与 `:3153` (打包后安装 / 信息), `model/explorer/ExplorerItem.java:100-106`, `ui/explorer/ExplorerItemViewHolder.kt:584, 618-622`, `ui/main/scripts/ExplorerFragment.kt:95`, `core/plugin/center/PluginInstaller.kt:48-81, 214` (文件 / URL 安装与下载), `core/plugin/center/PluginInstallActions.kt:13-16`, `core/plugin/center/wizard/PluginInstallWizardInstaller.kt:232`, `core/plugin/center/wizard/PluginInstallWizardInstallAwaiter.kt:34-123` (依赖 Coordinator 记录与 abort 监听), `ui/main/plugin/PluginFragment.kt:44-47, 290-293`, `core/plugin/center/PluginCenterActivity.kt:52-55, 128-132`, `PluginCenterFragment.kt:169`, `PluginInfoDialogManager.kt:87, 186, 412`, `app/src/debug/.../compat/ActivityLaunchCompatibilityProbe.kt:179`.

其它事实:

- 脚本侧目前没有任何安装 API: `runtime/api/augment/app/App.kt` 只有 `uninstall` (`ACTION_DELETE`, L795), `uninstallDual` (L808), `getApkInfo` (`getPackageArchiveInfo`, L647), `isInstalled` / `isDualInstalled`, `viewFile`, `launchAppDetailsSettings`. 文档 `api/app.md` 与 `aj6-int-app.d.ts` 同样没有 `install`.
- 宿主安装从不使用 Shizuku / Root, 始终经系统 `PackageInstaller` 会话并要求用户确认.
- 没有任何安装器偏好项 (`res/xml`, `Pref.kt` 均无), 唯一开关是 `installerEnabled` 占位符 (`app/build.gradle.kts:981, 1006`).
- Manifest: `REQUEST_INSTALL_PACKAGES` (L164), `REQUEST_DELETE_PACKAGES` (L165), `QUERY_ALL_PACKAGES` (L211-213), `moe.shizuku.manager.permission.API_V23` (L292), `ShizukuProvider` (L1088-1099); `<queries>` (L37-100) 无安装器条目.
- 字符串: `apk_info_*` (L15-31), `error_android_package_*` (L296-299), `error_package_installation_*` (L452-454), `error_request_install_packages_permission_denied` (L475), `text_activity_not_found_for_apk_installing` (L892), `text_failed_to_install` (L1343), `text_install*` (L1468-1475), `text_package_installation*` (L1683-1687), `text_permission_desc_request_install_packages` (L1754), `text_reading_android_package` (L1935), 9 种翻译目录.
- 遗留无调用者: `util/IntentUtils.kt:150-166 installApk`, `com/stardust/util/IntentUtil.java:106-123 installApk / installApkOrToast`.
- 测试: JVM `AabManifestDisplayDecoderTest`, `AndroidPackageArchiveInspectorTest`, `ApkManifestDisplayDecoderTest`, `ApkSignatureDetectorTest`, `BundletoolTocDecoderTest`, `LargePackageInspectionTest`, `PackageInstallStatusRecordTest`; 设备 `AndroidPackageArchiveDeviceTest`, `PackageInstallStatusStoreDeviceTest`, `ui/dialog/InfoDialogInstrumentationTest`; 文档 `docs/dev/package-inspection-roadmap.md`.

### 3.2 宿主特权基础设施

- Shizuku: `runtime/api/WrappedShizuku.kt` (绑定 `core/shizuku/UserService` 为 `shizuku-service-for-<pkg>` 进程, `hasPermission` / `isRunning` / `isOperational` / `requestPermission` / `execCommand`), AIDL `app/src/main/aidl/org/autojs/autojs/core/shizuku/IUserService.aidl` (`execCommand` 等 shell 语义), `permission/ShizukuPermission.kt`, 抽屉开关 `ui/main/drawer/DrawerFragment.kt:594-613`; 依赖 `dev.rikka.shizuku:api` / `provider` 13.1.5 (`gradle/libs.versions.toml:69,137-138`).
- Root: `util/RootUtils.java` (`isRootAvailable`, `RootMode`), `runtime/api/ProcessShell.java` (`su` 进程), vendored `:libs:root-shell-1_6`; 宿主没有 libsu, `RootService`, `Sui`.
- 结论: 宿主的 Shizuku / Root 只做 shell 命令, 没有隐藏 API Binder 通道; 插件不复用 `WrappedShizuku` (跨进程无法共享 UserService 绑定), 自持 Shizuku 客户端与 libsu.

### 3.3 兄弟仓库可复用事实

- 宿主契约与客户端范式 (Angus Mail, 2026-09-18 起): `plugin-api/mail-api` (AIDL `IMailPlugin` + `MailActions` / `MailIds` / `MailContract` / `MailCapabilityKeys` / `MailErrorCodes`, `MailAidlOrderTest` / `MailContractTest`), 宿主 `core/plugin/mail/MailPluginHost.kt` (`AidlPluginHost` 泛型, `discover` / `probe` / `selectOrThrow` / `call` / `callWithDedicatedBindingLease`, 含信任与最低宿主版本校验), 每脚本 `runtime/api/mail/MailService.kt` (`Closeable`), augment `runtime/api/augment/mail/Mail.kt` (`AugmentableKey("mail")`, 在 `ScriptRuntime.kt:1050` 装配), 协议文档 `docs/dev/mail-plugin-protocol-v1.md`. 本插件按同一范式落地 `installer-api` / `InstallerPluginHost` / `InstallerService` / `Installer.kt`.
- Three 系列身份 (3-Stove Agent): 点分 applicationId, `ThreeStoveAgent*` 类前缀, `resValue` 五键, `locks/host-api-aars.lock` 配置期 SHA-256 校验, 四 alias 启动器图标, 独立设置 / 关于 / 发行历史 Activity (代码构建的 AppCompat + Material 3 视图, 无 Compose).
- APK Inspector: 解析器副本已分叉 (`AndroidPackageArchive.kt` 与宿主相差 703 行, `ApkSignatureDetector.kt` 相差 90 行, 另有 `AndroidPackageArchiveValidator`, `ContainerMetadata`, `AabBundleConfigDecoder` 等专有文件); `ExternalViewerActivity` 的 MIME 列表 (`application/vnd.android.package-archive`, `application/x-apks`, `application/vnd.apkm`, `application/xapk-package-archive`, `application/x-apkz`, `application/x-aab`, `application/vnd.android.aab`) 可直接复用于本插件外部入口.
- Readium EPUB Reader: 一个包同时暴露 Explorer Action 与脚本能力服务的先例; 本插件不注册 Explorer Action engine, 文件管理器安装按钮由宿主路由直接经 `installer` 契约调用 (D21), 避免双 engine.
- 官方索引: `official-repositories.json` 按字母序插入仓库名; 首个发布后新增 `release-manifests/io.github.supermonster003.autojs6.plugin.three.setup.installer/<versionCode>.json`.

### 3.4 参考项目事实 (2026-09-30 核实)

- InstallerX (归档): 安装模式 对话框 / 通知栏 / 自动; 选项 声明安装者, 安装到所有用户, 允许测试包, 允许降级, 安装后自动删除; Android 5.0-13.
- InstallerX-Revived (`main`): 授权方式 `Global / None / Root / Shizuku / Dhizuku / Customize` (`domain/settings/model/config/Authorization.kt`); 安装模式 `Dialog / AutoDialog / Notification / AutoNotification / Ignore`; 配置模型 (`ConfigModel.kt`) 含 `installer` / `installerMode (Self / Initiator / Custom)`, `installRequester`, `targetUserId`, `forAllUser`, `allowTestOnly`, `allowDowngrade`, `bypassLowTargetSdk`, `allowAllRequestedPermissions`, `allowSigMismatch` / `allowSigUnknown`, `requestUpdateOwnership`, `autoDelete` / `autoDeleteZip`, `enableManualDexopt` / `dexoptMode`, `splitChooseAll` / `apkChooseAll`, `requireBiometricAuth`, `installReason`, `packageSource`; 安装标志枚举 (`InstallOption.kt`) 映射 `INSTALL_ALLOW_TEST`, `INSTALL_ALL_USERS`, `INSTALL_REQUEST_DOWNGRADE | INSTALL_ALLOW_DOWNGRADE`, `INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS`, `INSTALL_DONT_KILL_APP`, `INSTALL_FULL_APP` 等; 锁定默认安装器 (`DefaultPrivilegedService.setDefaultInstaller`): 对 `ACTION_VIEW` 与 `ACTION_INSTALL_PACKAGE` 各构造 filter, `queryIntentActivities` 后对竞争包 `clearPackagePreferredActivities`, 再 `addPreferredActivity` (API 31+ 带 `removeExisting`), 仅在 `canCallSystemRestrictedPreferredApis` 时调用 `addPersistentPreferredActivity` (其 `SystemApp` / `UserService` / `ShizukuHook` 运行时均为 false); Dhizuku 路径用 `DevicePolicyManager.addPersistentPreferredActivity`; HyperOS 需要有效安装者时默认声明 `com.android.shell`; 支持层级 SDK 34-37 完整, 26-33 有限; 依赖 Shizuku 13.1.5, Dhizuku-API 2.6.0, HiddenApiBypass 6.1, 隐藏 API 存根模块 `hidden-api/` (`IPackageInstaller`, `IPackageInstallerSession`, `IPackageManager`, `PackageInstallerHidden`, `PackageManagerHidden`, `ParceledListSlice`, `UserInfo`); Root 走 `app_process` 而非 libsu.
- 框架限制 (按事实写入文档, 不夸大): user 版本 ROM 上降级只对 debuggable 应用生效, 否则返回 `INSTALL_FAILED_VERSION_DOWNGRADE`; `addPersistentPreferredActivity` 要求调用方 uid 为 1000; 绕过低 targetSdk 拦截为 API 34+ 的 `INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK`; 目标用户与 `INSTALL_ALL_USERS` 仅特权路径可用.

### 3.5 缺口

- 没有共享解析 AAR, 没有 `installer-api` 契约, 没有宿主客户端与脚本 API, 没有退化路由: 12 处调用方直接依赖 `PackageInstallerActivity`.
- 没有隐藏 API 存根, 没有 libsu, 没有面向安装的 Shizuku UserService.
- 宿主向导 `PluginInstallWizardInstallAwaiter` 与 `PackageInstallStatusCoordinator` 强耦合.
- 文档 / d.ts 没有 `installer` 页面与声明.

---

## 4. 目标架构

### 4.1 数据流

```text
脚本 installer.install(source, options)
  -> 宿主 InstallerService (每脚本, Closeable, 规范化参数, 打开 PFD)
  -> 宿主 InstallerPluginHost (AidlPluginHost<IInstallerPlugin>, 信任 / 版本 / 能力协商)
  -> 插件 ThreeSetupInstallerPluginService (IInstallerPlugin.Stub, HostCallerGuard, 上限校验)
  -> 插件 InstallCoordinator (解析 -> 授权方式选择 -> 交互模式 -> 引擎)
       |- none:    PackageInstaller.Session + 插件 UserActionActivity 接管系统确认
       |- shizuku: Shizuku.bindUserService(IPrivilegedInstaller) -> 隐藏 API IPackageInstaller
       `- root:    libsu RootService(IPrivilegedInstaller)      -> 隐藏 API IPackageInstaller
  -> IInstallerSessionCallback (stage / progress / completed / failed)
  -> 宿主 InstallSession (EventEmitter) / 同步等待 / Promise

宿主入口 (文件管理器 / 插件中心 / 打包 / 自更新)
  -> PackageInstallRouter: 插件可用 -> 同上 Binder 会话 (interaction = dialog)
                           不可用   -> ACTION_VIEW + FileProvider -> 系统安装器 + 引导安装插件

外部 ACTION_VIEW (任意文件管理器 / 浏览器)
  -> 插件 ExternalInstallActivity -> InstallCoordinator (interaction = dialog)
```

### 4.2 插件包结构 (`app/src/main/java/io/github/supermonster003/autojs6/plugin/three/setup/installer/`)

```text
ThreeSetupInstallerPlugin.kt            身份常量 (引用 installer-api 的 InstallerIds / InstallerActions)
ThreeSetupInstallerPluginInfoService.kt IPluginInfoProvider
ThreeSetupInstallerPluginService.kt     IInstallerPlugin.Stub (Binder 路由)
WakeActivity.kt
binder/   HostCallerGuard, InstallerBinder (Bundle / JSON 编解码, 上限校验), SessionRegistry
source/   PackageSource (PFD / content / file), ArchiveOpener (zip / xapk / apkm / apks), SplitSelector (使用共享 AAR)
engine/   InstallEngine 接口, NoneInstallEngine, PrivilegedInstallEngine, UninstallEngine, DefaultInstallerLock
priv/     IPrivilegedInstaller.aidl (私有 AIDL), PrivilegedInstallerImpl, ShizukuUserService, RootInstallerService (libsu), hidden/ 存根
auth/     Authorizer 枚举, AuthorizerResolver (D14), ShizukuAuthorizer, RootAuthorizer, NoneAuthorizer
ui/       InstallDialogActivity, ExternalInstallActivity, UninstallDialogActivity, UserActionActivity, InstallForegroundService, HomeActivity (首页, D33), InstalledAppsActivity (D36)
history/  InstallHistoryStore (200 条持久化历史, D34)
queue/    InstallQueue (首页批量队列, D36)
ui/settings/ SettingsActivity, DefaultInstallerActivity, AboutActivity, ReleaseHistoryActivity, LauncherIcons
```

### 4.3 宿主包结构

```text
plugin-api/package-archive-parser/      共享解析 AAR (D28)
plugin-api/installer-api/               AIDL: IInstallerPlugin, IInstallerSession, IInstallerSessionCallback, IInstallerCallback
                                        常量: InstallerActions, InstallerIds, InstallerContract, InstallerCapabilityKeys, InstallerErrorCodes
core/plugin/installer/                  InstallerPluginHost, PackageInstallRouter, InstallerSessionClient, InstallerJson, InstallerErrorMapper,
                                        PackageChangeObserver (D22), ThreeSetupInstallerOfficialPlugin
runtime/api/installer/                  InstallerService, InstallerScriptOptions, InstallerScriptArguments, InstallerScriptValues
runtime/api/augment/installer/          Installer.kt (AugmentableKey("installer")), InstallSessionNativeObject, InstallerJsErrors, InstallerPromises
```

### 4.4 身份派生表 (D13)

| 占位符 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Setup-Installer` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-setup-installer` |
| `{APP_NAME}` | `3-Setup Installer` |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.setup.installer` |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-setup-installer` / `installer` / `default` |
| `{PLUGIN_SERVICE}` | `ThreeSetupInstallerPluginService` (主进程) |
| INFO 服务 | `ThreeSetupInstallerPluginInfoService`, action `org.autojs.plugin.INFO`, category `installer` |
| `{CAPABILITY_API}` | `installer-api` (宿主 `plugin-api/installer-api`, AIDL 包 `org.autojs.plugin.installer.api`) |
| `{SERVICE_ACTION}` / `{SERVICE_CATEGORY}` | `org.autojs.plugin.INSTALLER` / `installer` |
| 设置入口 action | `org.autojs.plugin.INSTALLER_SETTINGS` (官方插件设置契约 V1) |
| 类前缀 / 主题 | `ThreeSetupInstaller*` / `Theme.ThreeSetupInstaller` |
| `{REQUIRES_HOST_VERSION}` | P1.5 回填 (交付 `installer-api` 的宿主构建, >= 5299) |
| `{PLATFORM_VERSIONS_PLUGIN_VERSION}` | `1.8.3` |
| 发布文件名 | `autojs6-plugin-three-setup-installer-v{VERSION_NAME}-{CRC32}.apk` (单 APK, 无原生库, `supportedAbis = emptyArray()`) |
| README `icon_alt` | `autojs6-plugin-three-setup-installer-ic-launcher` |
| CI artifact | `three-setup-installer-debug-apks` |
| 图标源图 | `.python/icons/three-setup-ic-launcher-light.png` / `-dark.png` (1254 x 1254 RGBA, alpha 包围盒 `(261, 241, 993, 1013)` 即 732 x 772, 高 / 宽 1.055, 图案 `#272727` / `#D8D8D8`, alpha 一致) |
| 宿主向导条目 | `entry(official("three.setup.installer"), "Three Setup Installer", TOOLS)` |
| 官方索引 | `official-repositories.json` 插入 `AutoJs6-Plugin-Three-Setup-Installer` |

---

## 5. 阶段总览

| 阶段 | 目标 | 主要落点 | 前置 |
| --- | --- | --- | --- |
| P0 | 仓库骨架 + 特权安装 spike (Shizuku / Root 隐藏 API 静默安装, `addPreferredActivity` 可行性) | 插件 | 无 |
| P1 | 共享解析 AAR, `installer-api` 契约, 宿主客户端与退化路由, 删除宿主安装器, 协议文档 | 宿主 | P0 骨架 (可并行) |
| P2 | 插件核心: 来源与格式, 授权方式, 安装 / 卸载引擎, 批量与分包, Binder 路由 | 插件 | P0 spike; P1 契约 |
| P3 | 插件界面: 安装对话框, 外部入口, 卸载与用户确认, 前台服务通知 | 插件 | P2 |
| P4 | 脚本 API `installer` (同步 + Async + 会话, InstallerError, 示例) | 宿主 | P1, P2 |
| P5 | 独立应用形态: 设置页, 默认安装器页, 关于 / 发行历史 / 更新检查, 启动器图标 | 插件 | P2, P3 |
| P6 | 健壮性, 安全, 兼容矩阵, 性能, 体积 | 全部 | P3, P4, P5 |
| P7 | 文档, d.ts, Ace, 离线文档, README, changelog, GitHub 仓库, 官方索引, 1.0.0 发布 gate | 文档 + 发布 | P6 |
| P8 | 1.1.0: Dhizuku 授权, 持久化默认安装器, 通知栏安装模式, APK Inspector 迁移到共享 AAR | 插件 + 宿主 + 兄弟 | P7 本地 gate; 2026-10-02 维护者允许远端发布延迟时先行 |
| P9 | 1.2.0: 授予全部权限 / 更新所有权 / DexOpt / 签名门禁 / 黑名单 / 权限预览 / 按来源配置 | 插件 (+ 宿主小) | P8 |

建议会话切分: P0 一次 (骨架 + spike); P1 两到三次 (共享 AAR 为一次; 契约 + 客户端 + 路由为一次; 删除旧安装器 + 调用方改造 + 文档为一次); P2 两到三次 (来源与授权; 安装引擎; 卸载 / 批量 / 路由); P3 一到两次; P4 两次 (install / session / errors; uninstall / inspect / authorizer / setDefault / 示例); P5 一到两次; P6 一到两次; P7 一次; P8 两次; P9 两到三次.

当前进度 (2026-10-02): P0-P6原条目已完成所列范围验收. 本轮补齐API 24应用Root与同一Sony API 33 Shizuku的安装选项六格矩阵, Samsung/API 36证明实际低targetSdk拒绝及标志解除拒绝; XQ-AT72默认页/实际Files/取消/解锁通过且保留四条原插件最近使用. P6.3原设备表完整, Redmi实际Files由系统安装器接管等ROM限制作为实测结果保留, 不记为插件确认成功. 既有特权恢复API/元数据, API 24大字号/系统多窗口及未专项ColorOS边界仍见证据. P7.1/P7.2和P7.3本地构建检查已完成; 下一步仅剩P7.3远端仓库/tag/Release/官方索引/宿主推送, 当前仍仅本地提交, 不提前进入P8/P9.

---

## P0: 仓库骨架与特权安装 spike

目标: 让 `AutoJs6-Plugin-Three-Setup-Installer` 成为可构建, 可安装, 能被宿主插件中心发现的最小 APK, 并在阶段末用真机证明 "Shizuku UserService 与 libsu RootService 经隐藏 API 静默安装一个 APK" 与 "`addPreferredActivity` 在 shell / root 身份下可把本插件设为默认安装器" 两件事成立.

### P0.1 仓库骨架

- [x] (插件) 按第 4.4 节身份派生表确定全部标识并全仓库一致; 确认平台版本插件 `1.8.3` 可从公共仓库解析 (否则暂用已验证版本并记录); `{REQUIRES_HOST_VERSION}` 暂取 5298 (骨架所对宿主构建), P1.5 回填. (SOURCE 2026-09-30: `ThreeSetupInstallerPlugin.kt`, `.readme/common.json`, `app/build.gradle.kts` 的 `resValue` 五键, `settings.gradle.kts`; JVM: `ThreeSetupInstallerPluginRuntimeInfoTest` 3 用例交叉比对; 平台插件 1.8.3 解析并用于构建 (plugins.gradle.org 当日最新为 1.8.6, 与兄弟仓库统一升级时再更新); `REQUIRED_HOST_VERSION` = 5298 为临时值)
- [x] (插件) 以 3-Stove Agent (Three 身份, `locks/host-api-aars.lock` 配置期 SHA-256 校验, 四 alias 图标, 代码构建的设置 UI 套件) 与 Angus Mail (`installer-api` 消费方式, `HostCallerGuard`, 设置入口 action) 为模板生成: `settings.gradle.kts` (平台插件位于 `includeBuild("build-logic")` 之前, 无 `mavenLocal()`, `include(":app")`), 根 `build.gradle.kts`, `build-logic/`, `app/build.gradle.kts` (`resValue` 五键, `buildFeatures { aidl = true; resValues = true }`, `hostApiIds` 在 P1 前只含 `common-plugin-api`, P1.1 / P1.2 交付后追加 `package-archive-parser` 与 `installer-api`, `appendDigestToReleasedFiles`, 不启用 ABI 拆分, `nativeAlignment { expectNoNativeLibraries.set(true) }`, `getInfo()` 显式 `supportedAbis = emptyArray()`), `version.properties` (`VERSION_NAME=1.0.0`, SDK 37 / 37 / 24, `OVERRIDDEN_*=NONE`), `gradle.properties`, wrapper; 依赖: `dev.rikka.shizuku:api` / `provider` 13.1.5, `com.github.topjohnwu.libsu:core` / `service` (Maven Central 最新稳定版, 记录版本与 SHA-256), `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`, appcompat, material, gson; 无 Compose. (ANDROID_BUILD 2026-09-30: Gradle 9.5.0 / AGP 9.3.2 / Kotlin 2.3.20 / JDK 21, `:app:testDebugUnitTest` `:app:assembleDebug` `:app:assembleDebugAndroidTest` `:app:lintDebug` (0 错误 / 9 警告: 依赖新版本提示与 P5.4 前未引用的图标资源) 通过, `verifyDebugNativePageAlignment` 通过 (无原生库); libsu 只发布在 JitPack (Maven Central 无此坐标), 采用 6.0.0 (JitPack `latestOk`), 根 `build.gradle.kts` 已含 jitpack 仓库; debug APK 6.4 MiB (未压缩, 含 Material); `build-logic` 13 个约定源码与 3-Stove Agent 一致)
- [x] (插件) 从宿主复制 `.gitignore`, `sign.properties`, `app/sm003.jks` 到相同相对路径, `git check-ignore` 确认后两者与 `*.pre-platform-versions.bak` 不入库. (SOURCE 2026-09-30: `.gitignore` 取 3-Stove Agent 版本 (宿主版本忽略 `AGENTS.md`, 插件需要跟踪它); `git check-ignore -v` 确认 `sign.properties` (`/sign.properties`), `app/sm003.jks` (`*.jks`), `local.properties` 均被忽略, `*.pre-platform-versions.bak` 在规则中)
- [x] (插件) Manifest 骨架: `org.autojs.permission.PLUGIN`, `REQUEST_INSTALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`, `QUERY_ALL_PACKAGES` (显示已安装版本与签名比对, 在 `AGENTS.md` 记录理由), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` + 类型权限 (D26, 类型在 P3.4 定), `moe.shizuku.manager.permission.API_V23`; `<queries>`: `org.autojs.autojs6`, `moe.shizuku.privileged.api`, 以及 `ACTION_VIEW` 安装包 MIME 的 intent 查询 (用于状态检测); `ShizukuProvider` (`${applicationId}.shizuku`); `WAKE_ACTIVITY` + `WakeActivity`; `org.autojs.plugin.info.AUTHOR`; INFO 服务 (`org.autojs.plugin.INFO`, category `installer`, `requiresHostVersion`); `ThreeSetupInstallerPluginService` (`org.autojs.plugin.INSTALLER`, category `installer`, PLUGIN 权限); `allowBackup=false`; `localeConfig`; `supportsRtl`. 外部 `ACTION_VIEW` 入口与启动器 alias 在 P3.2 / P5.4 追加. (JVM: `ManifestContractTest` 6 用例; BINDER 2026-09-30: `ThreeSetupInstallerPluginContractTest` 5 用例 (Wake 契约, 无启动器入口, INFO `getInfo()` 往返含显式空 `supportedAbis` 与仅含 `requiresHostVersion` 的能力 Bundle, INSTALLER 服务占位 Binder 的 descriptor, Shizuku provider 注册) 在 AVD API 24 x86 与 Xiaomi 23046RP50C / API 35 通过; `cmd package query-services` 对两个 action + category `installer` 各命中唯一服务; `FOREGROUND_SERVICE` 已声明, 类型权限留 P3.4; `INSTALLER` 服务在 P1.2 前返回携带 `org.autojs.plugin.installer.api.IInstallerPlugin` descriptor 的占位 `Binder`)
- [x] (插件) 资源骨架: 10 语言 `strings.xml` (`plugin_description` 各语言, 句尾无点号, 不写 "适用于 AutoJs6", 例如 `安装, 更新和卸载 Android 应用, 支持通过 Shizuku 或 Root 静默安装` / `Installs, updates and uninstalls Android apps, with silent installation through Shizuku or Root`), `strings_donottranslate.xml` (`app_name=3-Setup Installer`), 按 `name` 升序, ASCII 标点; 图标: `.python/generate_launcher_icons.py` 从 `.python/icons/three-setup-ic-launcher-light.png` 提取 alpha, `ADAPTIVE_GLYPH = 0.41` (高 / 宽 1.055 时上限 `66 / (108 * sqrt(1 + 1.055^2)) = 0.4204`, 取 0.41 使半对角线 32.2 dp < 33 dp), 传统 / 圆盘图案宽 66%, `OPTICAL_X` / `OPTICAL_Y` 初值 0 并在 P5.4 用真机截图复核; 生成 `mipmap/ic_launcher.png` + `mipmap-night/` + `ic_launcher_system{,_light,_auto}` + 前景 / 单色层 + v26 XML, 两次运行字节一致, `--check` 通过. (JVM: `StringResourceParityTest` 4 用例 (键集合 / 排序 / 无句尾标点且不含 AutoJs6 / `locales_config` 与目录一致 / `values` 与 `values-en` 逐字相同), `ApplicationTextPunctuationTest` 扫描 xml / md / json 通过; 图标 2026-09-30: 15 个资源由生成器确定性产出并 `--check` 通过, 432 px 输出的 UI 图案包围盒 `(71, 63, 361, 369)` (290 x 306), 自适应前景 `(125, 120, 307, 312)` (182 x 192), 圆盘 `(0, 0, 432, 432)`; 宿主插件中心在 Xiaomi API 35 上显示深色圆盘系统图标 (Three 透明图标 allowlist 在 P1.3 宿主注册时加入); 光学居中未用真机复核, 留 P5.4)
- [x] (插件) `.readme/` + `.changelog/` (10 个 `lang_*.json` + 模板) + `.python/generate_markdown.py` (写入与 `--check`) + `.bat` 入口; 根 `README.md` 标明简体中文并与 `README-zh-Hans.md` 同源; `LICENSE` (MPL-2.0); `THIRD_PARTY_NOTICES.md` (common-plugin-api, installer-api, package-archive-parser, Shizuku-API Apache-2.0, libsu Apache-2.0, HiddenApiBypass Apache-2.0, AOSP 隐藏 API 存根 Apache-2.0, 明确写明未使用 InstallerX 代码). (DOCS 2026-09-30: 生成器取自 Angus Mail 并把列表键改为 `features` / `usage_steps` / `authorizer_points` / `compatibility_points` / `faq_items` / `security_points`; `py -3 .python/generate_markdown.py --check` = `MARKDOWN_OK languages=10 artifacts=36` (README + 10 语言 README + 11 份 `raw*/plugin_instruction.md` + 14 份 CHANGELOG); 10 语言 README 文案的样板键 (标题, 构建, 许可证等) 沿用 Angus Mail 的既有译文, 项目专属键为本仓库新写; `THIRD_PARTY_NOTICES.md` 含 common-plugin-api SHA-256, 三个运行时依赖, AOSP 存根来源与 GPL 参考项目的 "未复用代码" 声明; `installer-api` / `package-archive-parser` 条目待 P1)
- [x] (插件) 复制 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 为本仓库 `AGENTS.md` 并裁剪 (无原生库, 无 ABI 拆分, 有独立设置页与发行历史, 无联网 (更新检查除外), 有特权进程与隐藏 API, 有外部 `ACTION_VIEW` 入口, 权限理由表, GPL 参考项目的许可证边界 D29); 身份表引用第 4.4 节; 引用三份同目录规范. (SOURCE 2026-09-30: `AGENTS.md` 16 节, 第 2 节身份表, 第 6 节权限理由表, 第 9 节特权进程与许可证边界, 第 14 节验证顺序)
- [x] (插件) `git init`; 按 "身份与构建骨架 / 契约与 Binder / 资源与文档 (含本 ROADMAP 与图标源图) / 测试与 CI" 拆分初始提交, 每次提交前把 `VERSION_BUILD` 写为 "当前提交数 + 1", 最后校验 `VERSION_BUILD == git rev-list --count HEAD` 且工作树干净. (SOURCE 2026-09-30: `git init -b master`; 5 次提交 (build: Gradle 骨架, build-logic 与 AAR 锁 / docs: 资源, 图标与文案源 / feat: 插件身份, Binder 服务与 Wake Activity / test: JVM 与 instrumentation 测试, CI / docs: AGENTS 与路线图), 每次提交前 `VERSION_BUILD` 写为提交数 + 1, 末尾校验 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 为空; 提交 hash 见会话记录)
- [x] (测试) `PluginInfo` 纯数据映射 JVM 测试; Manifest 契约与服务发现 instrumentation 测试 (Wake Activity, INFO 服务, `INSTALLER` 服务的 exported / permission / action / category 唯一命中); 资源 ASCII 标点与 `values` / `values-en` 一致性守卫. (JVM 2026-09-30: `ThreeSetupInstallerPluginRuntimeInfoTest` 3 + `ManifestContractTest` 6 + `StringResourceParityTest` 4 + `ApplicationTextPunctuationTest` 1 = 14 用例, `:app:testDebugUnitTest` 通过; BINDER: `ThreeSetupInstallerPluginContractTest` 5 用例在 AVD API 24 与 Xiaomi API 35 通过; 宿主侧: Xiaomi API 35 的插件中心列出 "3-Setup Installer 1.0.0 (1)" 与 zh-Hans 描述并按官方签名自动启用 (截图 `docs/dev/p0-spike-evidence.md` 记录), engine `installer` 尚未在宿主注册, 列表项的 "设置" / "详情" 行为留 P1.3 验证)
- [x] (插件) CI: `build.yml` (JVM 测试, debug / androidTest / release APK, lint, API 24 x86 + API 35 x86_64 模拟器契约测试), `markdown.yml` (Windows 运行 `.python/check_markdown.bat`). (SOURCE 2026-09-30: `build.yml` (`assembleDebugUnitTest`, `verifyNativePageAlignment`, `testDebugUnitTest`, androidTest / release APK, lint debug / release, API 24 x86 + API 35 x86_64 模拟器 `connectedDebugAndroidTest`, artifact `three-setup-installer-debug-apks`), `markdown.yml` (Windows `check_markdown.bat`); 仓库尚未推送到 GitHub, 工作流未实际运行)

### P0.2 特权安装 spike

- [x] (插件) `priv/hidden/` 按 AOSP 签名自写适配器, 使用设备自带 `IPackageInstaller` / `IPackageInstallerSession` / `IPackageManager` / `ParceledListSlice` / `IUserManager` / `UserInfo`; 覆盖会话参数, 版本化卸载, 用户列表与两种 `addPreferredActivity` 签名; 安装标志集中在 `PrivilegedOptions`. 主进程绑定时与特权进程初始化时各一次 hidden API exemption, API < 28 不调用. 未使用的 `getInstallSourceInfo` 留 P2 inspect, 不提前加入死代码. (SOURCE / ANDROID_BUILD 2026-09-30: 无 `android.*` 存根进入 APK, 框架 Stub 决定 transaction 编号; debug / release 编译通过)
- [x] (插件) 私有 `IPrivilegedInstaller.aidl` 八个操作及 `attachClient` 死亡令牌, Shizuku 保留 `destroy` transaction; `PrivilegedInstallerImpl` 统一会话创建 / 流式写入 / fsync / 提交 / 放弃 / 卸载 / 默认项 / 用户列表. 只接受所属插件 UID, Binder 调用进入系统前清除调用者身份, 会话 <= 4 / 分包 <= 64, 截断与超长写入失败, 取消关闭运行中与排队中的流. UserService 本身已有 shell / root 身份, 与 RootService 均直接取系统 Binder; `ShizukuBinderWrapper` 只在主进程诊断中使用. (JVM: `PrivilegedOptionsTest` 3 用例; DEVICE: 四组设备均通过非法输入, 截断与排队取消检查)
- [x] (插件) `ShizukuUserService` 与 `RootInstallerService` 共用实现; UserService `daemon(false)`, 固定后缀, 版本戳; `PrivilegedServiceBinding` 主线程绑定 / 解绑, 非导出 Root 服务, Shizuku 保留退出 transaction 与 R8 构造入口. 重绑前通过 Binder death 确认旧进程结束, 避免旧连接断开回调误判新绑定. (DEVICE 2026-09-30: API 24 / 31 / 35 Shizuku, API 28 libsu Root 均通过解绑与重绑)
- [x] (测试) 两个 8,536 字节签名夹具 APK, 附 Manifest 源码与重建脚本; 测试拒绝覆盖预先存在的同包应用, 结束后只清理本次夹具. (DEVICE 2026-09-30: AVD API 24 x86 / AVD API 31 x86_64 / Xiaomi 23046RP50C API 35 的 Shizuku ADB, Sony G8441 API 28 + Magisk 26.4 的 libsu Root 各通过新装 / 更新 / 拒绝降级 / 静默卸载; 耗时与 serial 见证据表; Sony XQ-AT72 API 31 额外通过安装链路)
- [x] (测试) debug-only APK 入口与竞争 alias, 四种 action / scheme 的设置 / 解析 / 清理; 测试只在没有既有 APK 默认项的设备执行, 并覆盖插件内旧首选项的替换. (DEVICE 2026-09-30: API 24 / 31 / 35 shell 与 API 28 root 均命中 4/4; shell / root 持久首选项均 `SecurityException`; API 31 真机既有默认项完整保留, 用独立 AVD 补齐; 旧版 MIME 替换限制已回填 D23)
- [x] (文档) 两个 spike 通过, D11 成立, D15 / D16 / D23 按实测修订; 无需启用 `pm` 退路, 普通默认项支持 shell 与 root, 不承诺持久锁定. (DOCS 2026-09-30: `docs/dev/p0-spike-evidence.md`, AGENTS, 第三方声明, 10 语言 README / 插件说明 / changelog 同步; 后续安装参数矩阵和生命周期压力仍在 P2 / P6)

验收条件: `:app:assembleDebug` / `:app:testDebugUnitTest` / `:app:assembleDebugAndroidTest` / `:app:lintDebug` 通过; 宿主插件中心能发现并启用本插件 (INFO 往返); Shizuku 与 Root 静默安装 / 卸载各一次 (DEVICE); 默认安装器锁定 spike 至少一种身份通过; 证据写入本节与 `docs/dev/p0-spike-evidence.md`.

---

## P1: 共享解析 AAR, 宿主契约与退化

目标: 宿主拥有 `plugin-api/package-archive-parser` 与 `plugin-api/installer-api` 两个模块, 全部安装调用方经 `PackageInstallRouter`, 旧安装器代码删除, 无插件时交给系统安装器并引导安装插件; 宿主构建, 单元测试与 lint 通过.

### P1.1 共享解析 AAR `plugin-api/package-archive-parser`

- [x] (宿主) 新建 Android library 模块 (namespace `org.autojs.plugin.packagearchive`, 无 AIDL, 只依赖 Kotlin stdlib 与 `java.util.zip`), 在 `settings.gradle.kts` 的 `pluginApi` 列表注册; 将 `ui/main/scripts/` 的 `AndroidPackageArchive.kt` (含 `AndroidPackageFormat`, `ArchiveInstallationState`, `PackageDeviceSpec`, `AndroidPackageArchiveInspector`, `ManifestSummaryParser`), `BundletoolTocDecoder.kt`, `ApkManifestDisplayDecoder.kt`, `AabManifestDisplayDecoder.kt`, `ApkSignatureDetector.kt`, `PackageInspectionLimits.kt` 以 `git mv` 迁入并改包名; 去除对宿主 `R` / `Context` 的依赖 (字符串由调用方提供, `PackageDeviceSpec.from(context)` 改为接收 ABI 列表 / 密度 / 语言 / SDK 的纯数据工厂 + 宿主侧 `from(context)` 扩展). (SOURCE 2026-09-30: 6 个源文件与 6 个 JVM 测试以 `git mv` 迁入 `plugin-api/package-archive-parser`, 包名改为 `org.autojs.plugin.packagearchive`, 全部 `internal` 改为 public, `PackageDeviceSpec.from(context)` 留在模块内, Gson 保留为模块依赖; 宿主 6 处消费者显式导入; 宿主提交 3c31c686e6)
- [x] (宿主) 对照 APK Inspector 的分叉副本 (`AndroidPackageArchive.kt` 703 行差异, `ApkSignatureDetector.kt` 90 行差异, `AndroidPackageArchiveValidator.kt`), 只回收与格式识别 / 分包选择 / 容器校验有关且宿主也需要的修正, 逐条记录取舍到 `docs/dev/package-archive-parser-v1.md`; 不把签名验证, 原生库摘要, 16 KB 就绪度, DEX 摘要迁入 (D28). (DOCS 2026-09-30: 本阶段原样迁移宿主副本, 未回收 APK Inspector 的修正; `docs/dev/package-archive-parser-v1.md` 记录范围, 消费方式与空的取舍表, 随 P8 迁移时填写)
- [x] (宿主) 宿主 `app` 改为 `implementation(project(":plugin-api:package-archive-parser"))`, `ApkInfoDialogManager` / `DisplayManifestActivity` / `BuildActivity` 与文件管理器改用新包名; 六个 JVM 测试随模块迁移并通过; `AndroidPackageArchiveDeviceTest` 保留在宿主或迁入模块 androidTest. (ANDROID_BUILD 2026-09-30: `:app:compileAppDebugKotlin` / `:app:compileInrtDebugKotlin` 通过; `AndroidPackageArchiveDeviceTest` 保留在宿主 androidTest 并改为显式导入)
- [x] (宿主) 发布 AAR: `:plugin-api:package-archive-parser:assembleRelease`, 记录 SHA-256; 插件 `libs/package-archive-parser.aar` + `locks/host-api-aars.lock` 条目. (RELEASE 2026-09-30: `package-archive-parser-release.aar` 262,639 字节, SHA-256 `d979659e...ff2ab6`, 已入插件 `libs/package-archive-parser.aar` 与 `locks/host-api-aars.lock`, 插件提交 a54229e)
- [x] (测试) 模块 JVM 测试覆盖 `.apk` / `.apks` / `.xapk` / `.apkm` / `.apkz` / `.aab` / 含 APK 的 `.zip` 的格式识别与分包选择 (复用宿主既有夹具), 超限 (文件数, 大小, 时间) 的 `ArchiveProblemCode`. (JVM 2026-09-30: `:plugin-api:package-archive-parser:testDebugUnitTest` 52 用例通过 (AabManifestDisplayDecoderTest 8, AndroidPackageArchiveInspectorTest 11, ApkManifestDisplayDecoderTest 12, ApkSignatureDetectorTest 3, BundletoolTocDecoderTest 16, LargePackageInspectionTest 2), 夹具由 `ZipOutputStream` 构造)

### P1.2 契约模块 `plugin-api/installer-api`

- [x] (宿主) 新建 Android library (namespace `org.autojs.plugin.installer.api`, `aidl = true`, `api(project(":plugin-api:common-plugin-api"))`), 注册到 `pluginApi` 列表; AIDL: `IInstallerPlugin` (`getInfo`, `getCapabilities`, `getAuthorizerState`, `requestAuthorizer`, `inspect`, `openSession`, `uninstall`, `getDefaultInstallerState`, `setDefaultInstaller`, `getUsers`), `IInstallerSession` (`getId`, `getState`, `cancel`, `close`), `IInstallerSessionCallback` (`onStage`, `onProgress`, `onCompleted`, `onFailed`), `IInstallerCallback` (一次性结果); 草案见附录 B. (SOURCE 2026-09-30: 4 个 AIDL (`IInstallerPlugin` 10 方法, `IInstallerSession` 4, `IInstallerSessionCallback` 4 oneway, `IInstallerCallback` 2 oneway), `inspect` / `getUsers` 改为回调形式以免阻塞 Binder; 宿主提交 3c31c686e6)
- [x] (宿主) 常量: `InstallerActions` (`SERVICE_ACTION`, `SERVICE_CATEGORY`, `PLUGIN_PERMISSION`, `OPEN_SETTINGS`), `InstallerIds` (`PLUGIN_ID`, `ENGINE`, `VARIANT_DEFAULT`, `DEFAULT_PACKAGE_NAME`, `REQUIRED_HOST_VERSION_CODE`), `InstallerContract` (契约版本 1, Bundle key, JSON key, 阶段 / 授权方式 / 交互模式 / 用户选择字符串, 上限 D31), `InstallerCapabilityKeys` (`CONTRACT_VERSION`, `AUTHORIZERS`, `FEATURES`, `MAX_BATCH`, `MAX_SPLITS`), `InstallerErrorCodes` (附录 B.4). (SOURCE 2026-09-30: `InstallerActions` / `InstallerIds` (`REQUIRED_HOST_VERSION_CODE` 5299) / `InstallerContract` (键, 字段, 闭集, D31 上限) / `InstallerCapabilityKeys` / `InstallerErrorCodes` (20 码))
- [x] (测试) `InstallerAidlOrderTest` (transaction 顺序冻结), `InstallerContractTest` (常量, 上限, 错误码唯一, 与 `PluginCapabilityKeys` 无冲突). (JVM 2026-09-30: `:plugin-api:installer-api:testDebugUnitTest` 8 用例通过 (InstallerAidlOrderTest 1, InstallerContractTest 7))
- [x] (宿主) 发布 AAR 并写入插件 `libs/installer-api.aar` + 锁文件. (RELEASE 2026-09-30: `installer-api-release.aar` 30,918 字节, SHA-256 `0e9996f8...ecb72f`, 已入插件 `libs/` 与锁文件; 插件身份常量改由契约提供, 能力声明 `installerContractVersion` 1, 插件提交 a54229e)

### P1.3 宿主客户端, 路由与退化

- [x] (宿主) `core/plugin/installer/InstallerPluginHost.kt` (`AidlPluginHost<IInstallerPlugin>`: action / category / `IInstallerPlugin.Stub::asInterface`, 最低宿主版本, 信任校验), `ThreeSetupInstallerOfficialPlugin.kt` (官方包名 / 仓库 URL / 安装引导), `InstallerSessionClient.kt` (打开 PFD, 发起会话, 回调桥接, 取消, 超时), `InstallerJson.kt`, `InstallerErrorMapper.kt`. (SOURCE 2026-09-30: `InstallerPluginHost` (同步方法走池化绑定, 异步方法走专用租约 + 回调挂起 + 超时), `InstallerSessionHandle` + 回调 Binder (一次性终态, death recipient, `await` 超时取消), `InstallerSource` (File / content / file URI 只读 PFD), `InstallerJson` / `InstallerBundles` / `InstallerError` / `InstallerErrorMapper`, `ThreeSetupInstallerOfficialPlugin`; 宿主提交 3c31c686e6)
- [x] (宿主) `PackageInstallRouter.kt` (D21): `install(context, source: File | Uri, request: HostInstallRequest): HostInstallHandle`; 插件可用 -> `InstallerSessionClient` (`interaction = dialog`, 插件对话框); 插件缺失 / 禁用 / 不兼容 / 绑定失败 -> `ACTION_VIEW` + FileProvider `content://` + `FLAG_GRANT_READ_URI_PERMISSION` 交给系统安装器, 同时按状态给出 "安装 3-Setup Installer" / "启用" / "版本不兼容 (需要 x)" 引导 (`AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 9 节五态); 对 `.apks` / `.xapk` / `.apkm` / `.apkz` 无插件时不交给系统 (系统安装器不识别), 直接提示安装插件. (SOURCE 2026-09-30: `install(context, File)` / `install(context, Uri)` 即发即忘, `installAndAwait` 供向导; 插件可用 -> `dialog` 会话; 不可用 -> `.apk` 交系统安装器 (`ACTION_VIEW` + FileProvider, 每进程一次提示安装插件的 toast), `.apks` / `.xapk` / `.apkm` / `.apkz` 弹 `InstallerPluginUi` 五态引导 (缺失 / 禁用 / 未授权 / 不兼容 + 插件中心按钮), `.aab` 提示不可安装)
- [x] (宿主) `PackageChangeObserver.kt` (D22): 动态注册 `PACKAGE_ADDED` / `PACKAGE_REPLACED` (data scheme `package`, API 33+ `RECEIVER_NOT_EXPORTED`), 提供 `awaitInstalled(packageName, timeout)`; `PluginInstallWizardInstallAwaiter` 改为 "插件会话回调 或 观察器命中" 任一完成, 取消 / 超时语义保持. (SOURCE 2026-09-30: `awaitInstalled` 动态注册 `PACKAGE_ADDED` / `PACKAGE_REPLACED` (`RECEIVER_NOT_EXPORTED`), 5 分钟超时; `PluginInstallWizardInstallAwaiter` 重写为调用 `PackageInstallRouter.installAndAwait`, 不再依赖 Coordinator)
- [x] (宿主) 调用方改造 (第 3.1 节清单 12 处): `UpdateChecker`, `BuildActivity` (安装与信息对话框的安装按钮), `ExplorerItem` / `ExplorerItemViewHolder` / `ExplorerFragment`, `PluginInstaller` / `PluginInstallActions` / `PluginInstallWizardInstaller` / `PluginFragment` / `PluginCenterActivity` / `PluginCenterFragment` / `PluginInfoDialogManager`, debug `ActivityLaunchCompatibilityProbe`; `App.kt:140` 的 Coordinator 初始化删除. (SOURCE 2026-09-30: `ExplorerItem.install` x2, `UpdateChecker`, `BuildActivity`, `PluginInstaller` x3, `PluginInstallWizardInstaller`, `ApkInfoDialogManager` x2, debug 探针 (`install-ui` 走路由, `install` / `install-recovery` 路由随宿主安装器移除), `App.kt` 删除 Coordinator 初始化)
- [x] (宿主) 插件中心注册: `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` 增加 `installer`, `InstalledPluginRepository` 发现分派, Manifest `<queries>` 增加 `org.autojs.plugin.INSTALLER` intent, `PluginInstallWizardCatalog` 增加 `entry(official("three.setup.installer"), "Three Setup Installer", TOOLS)`, `PluginSettingsFragment` 的 "打开插件设置" 走 `org.autojs.plugin.INSTALLER_SETTINGS`. (SOURCE 2026-09-30: `SERVICE_ACTION_BY_ENGINE` 增加 `installer`, `InstalledPluginRepository` 增加 ServiceQuery 与发现分支, Manifest `<queries>` 增加 `org.autojs.plugin.INSTALLER` + `installer`, 向导目录 `entry(official("three.setup.installer"), "Three Setup Installer", TOOLS)`; `PluginSettingsFragment` 的插件设置入口随 P5.1 的插件设置页一起接入)
- [x] (测试) JVM: 路由五态决策表, 系统安装器交接 intent 构造 (MIME / flags / 不支持格式), 观察器超时与取消, JSON 编解码, 错误映射; 宿主 `testAppDebugUnitTest` 通过. (JVM 2026-09-30: `:app:testAppDebugUnitTest --tests org.autojs.autojs.core.plugin.installer.*` 14 用例通过 (InstallerJsonTest 9: 请求编码 / 上限 / 闭集 / 结果与错误解码 / 状态与用户文档 / JSON 上限, InstallerErrorMapperTest 3, PackageInstallRouterFormatTest 2: 扩展名识别与系统安装器可接受性); 观察器超时与系统安装器 intent 构造需要 Android 运行时, 留给 P1.4 的真机核对)

### P1.4 删除宿主安装器

- [x] (宿主) 删除 `PackageInstallerActivity.kt`, `PackageInstallerEntryActivity.kt`, `AndroidPackageSessionInstaller.kt`, `PackageInstallStatusActivity.kt` / `Coordinator.kt` / `Receiver.kt` / `Store.kt` 及其 Manifest 声明 (L377-445), `installerEnabled` 占位符 (`app/build.gradle.kts:981, 1006`), `PackageInstallStatusRecordTest`, `PackageInstallStatusStoreDeviceTest`; 删除遗留 `IntentUtils.installApk` 与 `com/stardust/util/IntentUtil.installApk*`. (SOURCE 2026-09-30: 7 个类, 2 个测试, Manifest 四个组件 (L377-445) 与 `installerEnabled` 占位符 (app / inrt) 删除; `IntentUtils.installApk` 与 `com.stardust.util.IntentUtil.installApk` / `installApkOrToast` 删除; 宿主提交 3c31c686e6)
- [x] (宿主) `ApkInfoDialogManager` 保留只读信息, "安装" 按钮改为 `PackageInstallRouter.install`; 文件管理器安装按钮 (`explorer_file.xml` `@+id/install`) 保留, 动作改走路由; `ExplorerItemActionPolicy.installVisible` 语义不变. (SOURCE 2026-09-30: 两处 `installAction` 改为 `PackageInstallRouter.install(context, file)`, `DisplayManifestActivity` 与只读信息不变, `ExplorerItemActionPolicy.installVisible` 不变)
- [x] (宿主) 字符串清理: 删除仅被删除代码引用的 `error_package_installation_*`, `text_package_installation*`, 通知通道文案等 (10 语言目录逐一核对); 新增退化引导文案 (`text_installer_plugin_required`, `text_installer_plugin_unsupported_format` 等, 默认与 `values-en` 一致, 按 `name` 排序). (SOURCE 2026-09-30: 删除仅旧安装器使用的 14 个字符串 x 11 目录 (`error_android_package_*`, `error_package_installation_*`, `error_request_install_packages_permission_denied`, `text_activity_not_found_for_apk_installing`, `text_package_installation*`, `text_reading_android_package`); 新增 `error_installer_plugin_required_for_package_format` 与 `hint_installer_plugin_recommended` x 11 目录, 按 name 排序插入)
- [x] (宿主) `REQUEST_INSTALL_PACKAGES` 保留 (系统安装器交接需要调用方声明), `REQUEST_DELETE_PACKAGES` 保留 (`app.uninstall`); ProGuard / R8 规则中与删除类相关的 keep 清理. (SOURCE 2026-09-30: 两个权限保留; ProGuard 规则中没有与删除类相关的 keep)
- [x] (测试) 宿主 `:app:assembleAppDebug` / `:app:assembleInrtDebug` / `testAppDebugUnitTest` / `lintAppDebug` 通过; 在一台真机上验证: 无插件时文件管理器点击 `.apk` -> 信息对话框 -> 安装 -> 系统安装器; 点击 `.xapk` -> 提示安装插件; 插件中心从 URL 安装 -> 系统安装器; 有插件时以上三处均进入插件对话框. (BUILD / DEVICE 2026-10-01: Redmi 22120RN86C/API 33, 正式宿主 UID 10778/build 5304, 插件原本不存在, 后安装 build 41. 六个实际 UI case 各 1/1, 五次安装核验固定无代码 APK 的包名/v1/SHA-256, 无插件 XAPK 引导取消且包不存在. APK Inspector 保持启用时, APK 通过文件行 More > Information > Install 进入; 实测发现信息入口隐藏缺口, 宿主 522335e864 补充更多菜单且捕获文件身份. XAPK 使用真实行 Install, URL 经真实菜单/输入/下载/信息框, 无直接路由或假 UID. App/Inrt/全量 JVM/androidTest/lint 构建通过, 最终 lint 0 Error/Fatal, 2399 Warning, 3 Hint; 导航/启用/许可逐项恢复, 12 case 和 4 条自有历史已清理. 详见宿主 docs/dev/installer-entry-evidence.md.)

### P1.5 协议文档, changelog 与版本回填

- [x] (宿主) `docs/dev/installer-plugin-protocol-v1.md` (决策, Binder 面, Bundle key, JSON 文档, 操作, 错误码, 上限, 版本协商, 安全边界, 宿主客户端, 脚本 API 章节占位) 与 `docs/dev/package-archive-parser-v1.md`; 宿主 `.changelog` 10 语言: `feature` (脚本 `installer` 待 P4 再写), `improvement` (安装器能力迁出到 3-Setup Installer 插件, 无插件时交给系统安装器), `dependency` (模块化 `package-archive-parser`, `installer-api`). (DOCS 2026-09-30: 协议文档 (决策, Binder 面, Bundle key, JSON 文档, 操作, 错误码, 上限, 版本协商, 安全边界, 宿主客户端 P1.3 章节; 脚本 API 章节留待 P4) 与 `docs/dev/package-archive-parser-v1.md`; 宿主 `.changelog` 10 语言: `improvement` 安装器迁出与退化行为, `dependency` 模块化 Package Archive Parser / Installer API; 宿主提交 3c31c686e6)
- [x] (宿主) 宿主提交后读取 `VERSION_BUILD`, 回填 `InstallerIds.REQUIRED_HOST_VERSION_CODE`, 插件 `requiresHostVersion` meta-data / `ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION` / `.readme/common.json` / 测试夹具; 只暂存本效果的路径 (宿主工作树可能含其它会话改动). (SOURCE 2026-09-30: 宿主 `VERSION_BUILD` 手工写为 5299 并随提交 3c31c686e6 交付; `InstallerIds.REQUIRED_HOST_VERSION_CODE` = 5299, 插件 `ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION` 引用该常量, 两个 `requiresHostVersion` meta-data, `.readme/common.json`, 运行时信息测试同步; 插件提交 a54229e)

验收条件: 两个新模块 AAR 已发布到插件 `libs/` 并锁定; 宿主构建 / 单测 / lint 通过; 真机退化路径三处验证; `docs/dev` 两份文档与 10 语言 changelog 已写; `REQUIRED_HOST_VERSION_CODE` 已回填.

---

## P2: 插件核心

目标: 插件在无 UI 的前提下, 能通过 Binder 完成 "解析 -> 授权方式选择 -> 安装 / 卸载 -> 回调" 的全部路径, 三种授权方式与 D6 全部选项在真机上可用.

### P2.1 来源与格式

- [x] (插件) `PackageSource` 三态: 宿主 PFD (`IInstallerPlugin.openSession` 的 `ParcelFileDescriptor[]` + 元数据), `content://` (外部入口, `ContentResolver.openFileDescriptor`), `file://` / 路径 (仅外部入口的旧文件管理器, 需可读); 统一为 `SeekableSource` (PFD 用 `FileInputStream(fd)` + `FileChannel`), 不复制到私有目录除非容器需要随机访问且来源不可 seek (管道), 复制到 `cacheDir/staging/<sessionId>/` 并在会话结束清理. (SOURCE / DEVICE 2026-10-01: PackageSource / SeekableSource 已接入安装与 inspect; API 24 / 28 / 35 验证 PFD, content, file 与管道. procfs 禁止重开时需位置读暂存, 这是共享 File 解析器的必要适配; 见 docs/dev/p2-source-evidence.md)
- [x] (插件) `ArchiveOpener`: 用共享 AAR 识别格式; `.apk` 直接流式; `.apks` 经 `BundletoolTocDecoder.select(device)` 选分包; `.xapk` / `.apkm` / `.apkz` / `.zip` 用 `ZipFile` 列出 `*.apk` 条目, 按 `manifest.json` (xapk) / `info.json` (apkm) 或文件名 (`base.apk`, `split_config.*.apk`) 分类, 按 `PackageDeviceSpec` (ABI, 密度, 语言) 选择; `.aab` -> `UNSUPPORTED_FORMAT` (D9), `inspect` 仍返回模块与包信息. (JVM: ArchiveOpenerTest 18 项及宿主解析器 54 项; DEVICE: 三台设备的真实签名 base / feature 容器; 宿主 0767971bc9 与插件 519710d, 见 docs/dev/p2-source-evidence.md)
- [x] (插件) `inspect(source)` 返回 `PackageArchiveInfo` JSON (附录 A.6): format, packageName, versionName, versionCode, label, icon (Base64 PNG, <= 64 KiB 且受整体 JSON 预算约束), minSdk, targetSdk, splits (名称 / 大小 / 是否选中 / 选择原因), signatureSchemes, installed (若已安装: versionName / versionCode / signerMatch / installer), size, problems. (BINDER / DEVICE 2026-10-01: API 24 / 28 / 35 APK 查询及已安装版本 / 签名匹配, `InstallerBinderDeviceTest`; 容器显示 APK 使用共享解析器, 完整格式夹具矩阵仍在下一项)
- [x] (测试) JVM: 各格式夹具 (复用共享 AAR 测试资源 + 本仓库最小 xapk / apkm 夹具), 分包选择, AAB 拒绝, 损坏 zip, 超限; 设备: PFD 来源与 content 来源各一次 inspect. (JVM / DEVICE 2026-10-01: 格式 / TOC / 选择 / AAB / 损坏 / 依赖 / 超限 / 内容改写用例通过; SourceFormatsDeviceTest 的 8 项在 API 24 / 28 / 35 全部通过, 无跳过; 见 docs/dev/p2-source-evidence.md)

### P2.2 授权方式

- [x] (插件) `Authorizer` 枚举 `NONE / SHIZUKU / ROOT`, `AuthorizerResolver.resolve(requested, settings)` 实现 D14 (显式指定不回退; `auto` 按设置顺序, 跳过被禁用项); 每种授权方式的 `state()` -> `{ available, running, granted, reason }` 与 `request()` (Shizuku: `Shizuku.requestPermission` + 结果监听; Root: libsu `Shell.getShell()` 触发授权; None: 恒 granted). (JVM / DEVICE 2026-10-01: 设置顺序 / 启用子集 / 状态决策表, Shizuku 与 Root state/request 往返通过; 见 docs/dev/p2-authorizer-evidence.md, commit 9a16601)
- [x] (插件) `ShizukuAuthorizer`: `Shizuku.addBinderReceivedListener` / `addBinderDeadListener`, `isPreV11` 拒绝, UserService 绑定缓存与 `DeadObjectException` 重绑, 版本不匹配时 `AUTHORIZER_UNAVAILABLE` 附 `reason`. (JVM / DEVICE: pre-v11 明确不可用, received/dead 按 Binder 身份失效; 每次绑定使用独立 tag, API 24 / 35 各连续 8 次立即重绑通过, 旧服务全部退出; 见 docs/dev/p2-authorizer-evidence.md)
- [x] (插件) `RootAuthorizer`: libsu `Shell.Builder` (`FLAG_MOUNT_MASTER` 不需要, 超时 10 s), `RootService.bind` 缓存, 无 su 时 `AUTHORIZER_UNAVAILABLE`, 拒绝时 `AUTHORIZER_DENIED`. (JVM / DEVICE: Root 10 s 与调用方期限, 无 su / 拒绝 / 超时区分, 授权与服务启动串行使用 shell 并及时关闭; API 28 Magisk / libsu 往返和无残留 shell 断言通过)
- [x] (插件) 首次并发绑定共享同一待连接记录, 超时 / 中断的单个等待者不破坏其他等待者, 最后一个等待者退出时关闭待连接服务, Binder 死亡后重绑. (JVM: `SharedBindingCacheTest` 5 项; DEVICE: `PrivilegedClientDeviceTest`, API 35 Shizuku 与 API 28 Root 各验证四个并发获取使用相同 Binder, 关闭后重绑获得新 Binder)
- [x] (测试) JVM: 解析顺序决策表 (设置顺序 x 各状态 x 显式指定); 设备: Shizuku ADB 模式 (AVD) 与 Root (已 root 真机或 AVD `-writable-system` + Magisk, 记录方式) 各一次 `state` / `request` 往返. (JVM: 授权相关 33 项全部通过; DEVICE: AVD API 24 / x86 / Shizuku ADB uid 2000 与 Sony G8441 / API 28 / arm64-v8a / Magisk Root 的 state/request, 并发绑定与关闭后重绑; Xiaomi API 35 另做同组回归. 沿用已有授权, 未声称重新弹出已授予权限的对话框)

### P2.3 安装引擎

进度补充 (2026-09-30): `InstallEngine.kt` 已补齐普通与特权单包引擎, 含分包写入 / 进度 / 标志 / 取消 / 超时 / 状态映射; 三种授权方式的基础设备测试已通过. 调用接口, 依赖的未提交 P2 文件与测试证据见 `docs/dev/install-engine-handoff.md`. 以下条目仍按整体集成与完整设备矩阵验收, 不提前勾选.

审阅修订 (2026-10-01): 私有 `release` 与 `abandon` 分离, 系统终态只释放记录; `none` 平台策略拒绝映射 `BLOCKED_BY_POLICY`; EPIPE 明确归因目标安装器停止读取并保留服务端首次写入失败日志. 按维护者确认细化 D18, `auto` 可接收系统确认而显式 `silent` 仍严格; status 4 的 `INSTALL_FAILED_VERSION_DOWNGRADE` 映射 `INSTALL_FAILED`, 保留原始状态与消息. 写入进度满值仅表示数据已交给会话流, P3 必须继续展示提交 / 校验阶段.

小项完善 (2026-10-01, N1-N4): Record 的框架句柄关闭由同步标志保证只调用一次; P0 设备测试的终态清理改用 `release`; D32 的十语言提示增加醒目的 "注意" 并明示默认不主动弹出确认; 特权 UID 按引擎实例与 Binder 身份缓存, 重绑时刷新. API 35 Shizuku 与 API 28 Root 各 8 项针对性回归通过, 见交接文档.

- [x] (插件) `InstallEngine` 接口: `install(request, sources, listener)`; `NoneInstallEngine` (D17): `PackageInstaller.Session` 创建 / 写入 (每个分包一个 `openWrite`, 1 MiB 缓冲, 进度按字节; 2026-10-01 从 8 MiB 调整以降低并发内存占用并细化进度) / `commit(IntentSender)` -> `STATUS_PENDING_USER_ACTION` 交 `UserActionActivity` (P3.3) / 结果广播 -> 回调; `PrivilegedInstallEngine`: 经 `IPrivilegedInstaller` 创建会话 (`installFlags` 映射: `allowDowngrade` -> `INSTALL_REQUEST_DOWNGRADE | INSTALL_ALLOW_DOWNGRADE`, `allowTestOnly` -> `INSTALL_ALLOW_TEST`, `bypassLowTargetSdk` -> `INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK` (API 34+, 低版本忽略并在结果 `notes` 说明), `user: 'all'` -> `INSTALL_ALL_USERS`, `installer` -> `installerPackageName`, `user: <id>` -> `userId`), 写入用返回的 PFD, `commit` 后经 `IntentSender` (插件 `PendingIntent` 广播) 取结果. (SOURCE / DEVICE 2026-10-01: P3.3 已接入独立 UserActionActivity, 未知来源设置和通知回退; API 24 none 真实安装 / 更新, API 28 Root 与 API 35 Shizuku 回归通过. 安装标志的完整设备交叉矩阵仍按下方测试项保留未完成, 见 docs/dev/p3-ui-evidence.md)
- [x] (插件) 结果规范化: `PackageInstaller.STATUS_*` -> 错误码 (附录 B.4), `EXTRA_STATUS_MESSAGE` 原样进 `systemMessage`, `EXTRA_PACKAGE_NAME` 进结果; 安装成功后按目标用户读取已安装版本, 按契约字段 `versionCode` / `previousVersionCode` 返回. (JVM: `InstallEngineTest`, `InstallFailureTest`, `InstallSessionTest`; DEVICE 2026-10-01: API 24 none / API 35 Shizuku / API 28 Root 的批量新装与更新返回正确旧版本; `Result.interaction` 原样用于成功结果; 见 `docs/dev/p2-session-evidence.md`)
- [x] (插件) `deleteSource` (D25) 与 `keepSourceOnFailure`; 会话超时 (D31) 与取消 (`abandon`). (SOURCE / JVM / DEVICE 2026-10-01: 维护者确认 keepSourceOnFailure 为固定保留策略, 不新增开关或公开选项, 已在原 D25 澄清. 外部成功删除/提供方拒删沿用 P3 实装证据, 宿主 PFD 所有权不变. 新组合在 API 24/35 各 1/1, 显式 deleteSource=true 的坏包失败/确认取消/provider query 和 open 超时四状态均保留源 SHA-256/长度且删除调用为 0; 独立 URI 的一次拒删控制验证计数有效且隔离, 自有历史精确恢复, 平台 session 集合不变. 成功项清理与晚到取消的边界已有 JVM, 已创建会话的 abandon 另有 P2/P6 生命周期证据. 见 docs/dev/p2-source-preservation-evidence.md.)
- [x] (测试) 设备矩阵: 三种授权方式 x (新装 / 更新 / 降级 / 测试包 / 分包集合 xapk) 于 AVD API 24 与一台 API 33+ 真机; 记录降级在 user 版本 ROM 的实际结果 (预期 `INSTALL_FAILED_VERSION_DOWNGRADE`); `bypassLowTargetSdk` 用 targetSdk 22 的夹具 APK 于 API 34+ 验证. (部分 DEVICE 2026-10-01: 本轮统一 Core 在 API 24 none 5/5, Shizuku 6/6, API 35 none 5/5, 额外 Sony API 33 Root 6/6. API 33 user/KernelSU 实际接受带双标志的非 debuggable 降级, 与原 API 28/35 拒绝结果分别记录, 不一概推断 user ROM 拒绝. API 35 none 接受 targetSdk 22, 不作为绕过拦截的证明. 本轮未补齐每种授权方式在两类 API 上的所有组合, 原 checkbox 保留; 详见 docs/dev/p6-matrix-evidence.md.) (补充 DEVICE 2026-10-01: API 35 Shizuku 完整8项和API 33 Sony none 5项均无跳过通过. API 24 su仅root/shell可执行, 插件UID无权限; Sony同机Shizuku尚未授权, 一次超时及随后中断不计通过. API 35仍不拦截targetSdk 22, 保留绕过证明缺口. 三设备许可/默认项/历史/系统session/server恢复核验通过, 见 docs/dev/p2-matrix-completion-evidence.md.) (完成 DEVICE 2026-10-02: 维护者补齐环境后, API 24 Root完整8/8和同一Sony API 33 Shizuku Root完整8/8, 配合该两设备既有none/Shizuku/Root证据完成六格. Samsung SM-A566B/API 36真实拒绝targetSdk 22, 同一Shizuku请求开启bypass后实际安装v1成功; none/Shizuku专项各1/1. 不改写HyperOS原本接受的结果. 见 docs/dev/p2-authorized-matrix-evidence.md 和 docs/dev/p2-samsung-low-target-evidence.md.)

### P2.4 卸载引擎

- [x] (插件) `UninstallEngine` (D24): 特权 `uninstall(packageName, flags(keepData -> DELETE_KEEP_DATA, user 'all' -> DELETE_ALL_USERS), userId, sender)`; `none` -> 非导出的 `UninstallDialogActivity` 拉起 `ACTION_UNINSTALL_PACKAGE` + `EXTRA_RETURN_RESULT`, 结果按契约映射 `USER_CANCELLED` / `UNINSTALL_FAILED`; 系统状态和失败原因透传. (JVM: `UninstallEngineTest` 6 项; DEVICE: `UninstallDialogDeviceTest` 2 项, 实际三路径卸载见下一项. P3 的特权显式 dialog 确认仍由上层接入)
- [x] (测试) 设备: 三种授权方式各卸载一次 (含 `keepData` 后重装数据仍在的断言). (DEVICE 2026-10-01: AVD API 24 / x86 / none, Xiaomi 23046RP50C / API 35 / arm64-v8a / Shizuku ADB, Sony G8441 / API 28 / arm64-v8a / libsu Root; Root 单独验证写入标记 -> keepData 卸载 -> 重装 -> 标记内容仍在. 全部只操作自建夹具, 见 `docs/dev/p2-session-evidence.md`)

### P2.5 批量, 分包与用户

- [x] (插件) 批量核心 (D20): `InstallSession` 按请求项串行调用 `engine.install`, 同项描述符合并为分包集合, 共享批量 deadline, `continueOnError` 默认 true, 聚合独立结果, 取消剩余项保留已确认成功, 终态回调前清理来源和暂存, 池线程复用前清中断标志. (JVM: `InstallSessionTest` 14 项, `RequestDocumentsTest` 4 项; DEVICE: 三种授权方式的三项请求 (v1 APK / 坏包 / v2 APK), 来源管道取消与线程复用, 见 `docs/dev/p2-session-evidence.md`)
- [x] (插件) 批量 Binder 接入: 一个 `openSession` 调用携带 <= 32 项, 每项 <= 64 描述符, 由 P2.6 注册并转发阶段 / 进度 / 终态回调. (JVM / BINDER: 请求限制, 四并发与客户端死亡; DEVICE: 三种授权方式经 V1 路由完成 v1 / 坏包 / v2, 详见 `docs/dev/p2-binder-evidence.md`; 真正的分包夹具矩阵仍待 P2.3 验收)
- [x] (插件) `getUsers()` 核心 (特权: `IUserManager.getUsers` -> id / name / isPrimary / isRunning; `none`: 仅当前用户); `user` 参数校验 (不存在的 id -> `INVALID_ARGUMENT`). (JVM: `DeviceUsersTest` 2 项; DEVICE: 上述三台设备的当前用户与无效用户校验; 特权已安装版本查询增加私有 AIDL transaction 10, 接收明确 userId. 公开 Binder 方法随 P2.6 接入, 次用户安装仍待下一项)
- [x] (测试) JVM: 批量上限与部分失败聚合; 设备: 3 个 APK 批量 (Shizuku), 双开 / 工作资料用户存在时 `user` 安装一次. (JVM / DEVICE 2026-10-01: API 35 Shizuku 单会话安装 3 个不同包并逐个核验版本; API 24 Shizuku 安装到临时工作资料 user 10, 确认 user 0 未安装. 工作资料和夹具已清理. 原有部分失败 / 上限用例继续通过; 见 docs/dev/p2-core-evidence.md)

### P2.6 Binder 路由与上限

- [x] (插件) `ThreeSetupInstallerPluginService` (`IInstallerPlugin.Stub`): 按宿主协议用 `HostCallerGuard` 校验官方包名 / UID / 相同签名 / 实际宿主版本, 会话方法检查所有者; D31 输入与只读 PFD 校验; 四并发会话, 有界工作队列, `linkToDeath` 取消并清理, 终态十分钟回收. 能力声明 `CONTRACT_VERSION=1`, 三种 AUTHORIZERS, MAX_BATCH / MAX_SPLITS, `FEATURES=[batch,splits,silent-uninstall,users,inspect,delete-source,default-installer]`. (SOURCE / JVM / BINDER 2026-10-01, 见 `docs/dev/p2-binder-evidence.md`; `delete-source` / `default-installer` 已随 P3.2 正式入口交付并由 API 24 / 35 契约回归验证)
- [x] (插件) 默认安装器协调层: 只读状态不拉起特权进程, 设置结果验证全部四个 APK filter, 保留 `DEFAULT_REQUIRES_CLEAR`, 只解除本插件默认项, 无正式 APK 入口时拒绝 enable 且不声明能力. (JVM: `DefaultInstallerTest` 3 项; BINDER: API 24 / 28 / 35 只读状态; 真实默认项操作已有 P0 证据, 正式入口启用仍在 P3 / P5) (P3.2 DEVICE 2026-10-01: 正式 ExternalInstallActivity 已接入; API 24 Shizuku 在无既有 APK 默认项的前提下设置并验证全部四个 filter, 再只清除本插件默认项. 专用设置页仍在 P5)
- [x] (测试) instrumentation: 发现 / 绑定 / descriptor; 敌意输入 (超长数组, 非法 JSON, 未知枚举, 空 / 可写 / 关闭的 PFD) 返回 `INVALID_ARGUMENT`; 四并发与第五项拒绝; 客户端进程死亡取消会话. (BINDER 2026-10-01: `InstallerBinderDeviceTest` 于 API 24 / 28 / 35; 跨进程测试使用非导出 debug 入口和独立回调进程, 生产入口拒绝插件 UID 冒充宿主. 关闭的 PFD 在同进程入口测试, 因其无法被正常封送. 详见 `docs/dev/p2-binder-evidence.md`)
- [x] (宿主 / 插件) 完整宿主界面联调: API 34+ 前台宿主绑定的 `BIND_ALLOW_ACTIVITY_STARTS` 授权与 P3 通知回退, 三处宿主入口的实际安装 / 退化, 正式外部入口和默认项能力; 不能用测试身份或同进程真实包测试替代. (DEVICE 2026-10-01: 原 API 35 正式宿主 UID=10890/生产 PackageInstallRouter 前台确认与安装, P3 通知回退, 外部入口及默认项证据保留. 新增 Redmi API 33 正式宿主 UID=10778 的有/无插件三入口实际 UI 共六项全通过, 宿主 522335e864/build 5304, 见 P1.4 和宿主 docs/dev/installer-entry-evidence.md. API 35 设备原有 overlayPermission=true, 不将该路径或 API 33 六项宣称为隔离证明绑定标志是唯一放行因素; 见 docs/dev/p3-ui-evidence.md 与 docs/dev/p3-notification-evidence.md.)

验收条件: P2 全部条目在 AVD API 24 与至少一台 API 33+ 真机上按授权方式矩阵通过; JVM 测试覆盖来源 / 格式 / 解析顺序 / 批量 / 上限; 证据写入 `docs/dev/p2-core-evidence.md`.

---

## P3: 插件界面

目标: 插件拥有完整的安装 / 卸载对话框, 外部 `ACTION_VIEW` 入口, 系统确认接管与前台服务通知; 宿主与脚本以 `interaction = dialog` 发起时用户看到的是插件界面.

### P3.1 安装对话框

- [x] (插件) `InstallDialogActivity` (对话框主题 `Theme.ThreeSetupInstaller.Dialog`, `excludeFromRecents`, `launchMode=standard` + `documentLaunchMode=intoExisting` + 会话 token URI 区分 task (平台必要适配, 见 docs/dev/p3-ui-evidence.md)): 确认段 (D26: 图标, 名称, 包名, 版本 旧 -> 新 或 "新安装", 大小, minSdk / targetSdk, 签名匹配 (与已安装签名一致 / 不一致 / 未安装), 分包列表可勾选, 选项开关 (授权方式, 降级, 测试包, 绕过低 targetSdk, 安装后删除, 目标用户) 默认取设置页), 进度段 (阶段文案 + 百分比 + 取消), 结果段 (成功: 打开 / 完成; 失败: 错误码 + 系统消息 + 复制). (SOURCE / JVM / DEVICE 2026-10-01: 完整信息 / 选项 / 分包复验 / 取消 / 结果操作已接入 Binder 与外部会话; 默认选项消费统一存储格式, 编辑设置页留在 P5. 见 docs/dev/p3-ui-evidence.md)
- [x] (插件) 批量对话框: 列表逐项状态, 全部取消, 单项重试; AAB 项显示 "无法安装 AAB" 与信息入口 (D9). (SOURCE / DEVICE 2026-10-01: 逐项状态, 全部取消, AAB 信息与说明已实现; 外部来源在 URI 授权仍有效时可独立重试. 已释放的宿主 PFD 提示调用方重新发起, 不延长描述符所有权. API 24 实际坏包 / 正常包 / 修复来源后重试通过)
- [x] (插件) 遵循独立设置页规范的对话框几何 (24 dp 圆角, 手机左右 24 dp, 宽屏 560 dp, 内容滚动按钮固定), 中性色表面, 主题色只用于控件; 10 语言文案; RTL / 大字号 / 夜间 / 进程重建 (会话 id 持久到 `SavedStateHandle`, 重建后从 `SessionRegistry` 恢复). (SOURCE / JVM / DEVICE 2026-10-01: 完成私有有界快照与只读恢复, 14 项新增 JVM 和 12 项安装界面用例通过; API 28 / 35 实际更换 PID 后恢复已保存确认结果, 未完成项中断且不自动重新执行. 两台设备各 3 项真实 IME / RTL / 字号 2 / 夜间 / 横屏几何通过. 既有 API 24 几何证据保留, 本轮新增 API 24 实机复验因模拟器启动被自动审批拒绝未执行. 见 docs/dev/p3-recovery-evidence.md 和 docs/dev/p3-appearance-evidence.md)
- [x] (测试) instrumentation: 确认 -> 安装 -> 结果的 happy path (none 路径在 AVD), 取消, 旋转重建, 失败结果展示. (DEVICE 2026-10-01: AVD API 24 none 真实安装 / 更新 / 删除与拒绝删除 / 批量重试; InstallDialogDeviceTest 8 项含取消, 重建和结果展示. P3 UI 组合 47 项无跳过通过, 见 docs/dev/p3-ui-evidence.md)

### P3.2 外部入口

- [x] (插件) `ExternalInstallActivity` (`exported=true`, 无权限保护, `Theme.NoDisplay` 后转 `InstallDialogActivity`): 两组 intent-filter 从宿主原 `PackageInstallerEntryActivity` 迁来 (`ACTION_VIEW` + `ACTION_INSTALL_PACKAGE`, `content` / `file` scheme, 7 种 MIME; `content` + `application/zip` / `application/octet-stream` + 大小写 `pathPattern` 覆盖 6 种扩展名); 多 URI (`ACTION_SEND_MULTIPLE`) 作为批量. (SOURCE / DEVICE 2026-10-01: 正式导出入口与受限 URI 转交完成; 7 MIME x action / scheme, 6 扩展名大小写与分享的解析矩阵通过, 宿主 5299 不再出现于 APK 处理列表) (build 35 兼容修正: Files by Google 使用数字 ID URI, 通用 ZIP/octet-stream filter 不再要求 pathPattern; 保留 content scheme 与精确 MIME, 实际格式仍由解析器验证. 该必要适配及八种专用 MIME 回归见 docs/dev/p3-external-entry-evidence.md.)
- [x] (插件) 外部来源的安全处理: 只读打开, 不信任文件名, 大小上限与共享 AAR 的检查上限; `file://` 在 API 24+ 仅接受可读路径, 失败给出 `SOURCE_UNREADABLE` 文案. (SOURCE / DEVICE 2026-10-01: 只读打开, URI / 数量 / 大小限制, 实际内容识别和共享解析器检查, 取消与错误展示已接入, 见 ExternalInstallDeviceTest 和 docs/dev/p3-ui-evidence.md)
- [x] (测试) 设备: 从系统文件管理器与浏览器下载列表各打开一次 `.apk` / `.xapk`; 宿主已删除入口后, 系统 "打开方式" 列表只出现插件. (MANUAL / DEVICE 2026-10-01: 维护者在 QV710AF65F / API 31 确认浏览器 APK/XAPK 与 Files by Google APK 的候选/安装/启动使用均成功; 已澄清 apkx 为 xapk 笔误. Files 的 XAPK 在 build 33 因 opaque content URI 未匹配, build 35 修复后由代理从真实 Downloads 列表打开到插件确认页并取消, APKM 同样复核. API 24 / 35 入口/来源回归各 4/4, 含官方宿主不占用安装入口断言. 文件管理器按维护者指定使用 Files by Google, 未冒充 AOSP DocumentsUI 或所有 OEM 覆盖; 详见 docs/dev/p3-external-entry-evidence.md, 先前浏览器启动受限记录保留在历史证据中.)

### P3.3 用户确认与卸载对话框

- [x] (插件) `UserActionActivity`: 接管 `STATUS_PENDING_USER_ACTION` 的 intent sender (`startActivityForResult`), 未知来源权限缺失时先引导 `ACTION_MANAGE_UNKNOWN_APP_SOURCES` 再重试, 超时 (D31) 后取消会话. (SOURCE / JVM / DEVICE 2026-10-01: token 桥接, 权限设置返回, 超时, 任务清理与通知回退完成; 最终结果以 PackageInstaller 广播为准, 不把 API 24 的 RESULT_CANCELED 当成安装失败. 见 docs/dev/p3-ui-evidence.md)
- [x] (插件) `UninstallDialogActivity`: `none` 路径承载 `ACTION_UNINSTALL_PACKAGE`; 特权路径的确认对话框 (脚本以 `interaction = dialog` 卸载时) 显示应用信息与 `keepData` 开关. (SOURCE / JVM / DEVICE 2026-10-01: 系统卸载桥接保留, 特权确认使用插件外观和 keepData 草稿; 旋转不重复启动, 选择值进入 UninstallEngine. API 24 / 28 / 35 桥接和特权确认回归通过)
- [x] (测试) 设备: none 路径新装 (含未知来源引导), 用户取消, 超时; 特权卸载确认. (DEVICE 2026-10-01: 既有 API 24 新装 / 取消, 超时 / 卸载与 API 35 实际拒绝仍有效. 维护者允许 Play Protect 扫描后, API 31 AVD 通过真实 Settings 授权并以原 session 2078773323 安装成功, created=1 / systemConfirmationStarts=1, OK (1 test), 4.015 秒, 无跳过. 独立驱动恢复 package / UID 原权限并核验夹具与测试历史清理, 四项结果均 true; 首次扫描与后续缓存结论的运行边界分别记录, 不把先前 API 28 / 35 的失败或跳过计为成功. 见 docs/dev/p3-unknown-source-evidence.md)

### P3.4 前台服务与通知

- [x] (插件) `InstallForegroundService`: 会话进入写入阶段时启动, 类型在 `dataSync` 与 `specialUse` 之间按 API 34+ 实测选定 (附录 D Q6), 通知显示阶段与进度, 完成后结束; 通知通道 `installation`; API 33+ `POST_NOTIFICATIONS` 缺失时静默降级 (不阻塞安装). (SOURCE / JVM / DEVICE 2026-10-01: 采用 dataSync, API 35 两次真实 2 GiB 写入与后台存活通过; 共享服务 / 取消 / 完成 / 通知发布竞态 / onTimeout 清理已实现, 通知拒绝不阻塞. 见 docs/dev/p3-notification-evidence.md)
- [x] (测试) 设备: 静默安装 2 GiB 级 xapk 期间切到后台, 进程未被杀且进度通知更新; API 34+ 无异常. (DEVICE 2026-10-01: API 35 Shizuku 同一次真实 2 GiB xapk 安装写入 2,147,500,874 字节, 后台 11,679 ms / 46 个 FGS 样本 / PID 31136 不变, 可见通知记录 27 个不同进度值 (0 至 98, 含中间进度), 无字节回调暂停, 操作 27,500 ms, 1 项通过无跳过. 临时通知许可已恢复为原有效禁用状态. 见 docs/dev/p3-notification-evidence.md)

验收条件: P3 全部对话框在 AVD API 24 / 真机 API 33+ 走通; 外部入口在系统列表出现且宿主不再出现; 前台服务在 API 34+ 合规; 证据写入 `docs/dev/p3-ui-evidence.md`.

---

## P4: 脚本 API `installer`

目标: 宿主脚本可通过 `installer` (`$installer`) 完成安装 / 卸载 / 检查 / 授权方式查询 / 默认安装器 / 用户列表, 同步 + Async + 会话三种形态, 错误为 `InstallerError`; 无插件时所有方法抛 (或拒绝) `PLUGIN_UNAVAILABLE`.

### P4.1 服务层与 augment

- [x] (宿主) `runtime/api/installer/InstallerService.kt` (每脚本, `Closeable`, 脚本停止时取消全部会话并释放 PFD), `InstallerScriptOptions.kt` (选项规范化: `authorizer`, `interaction`, `allowDowngrade`, `allowTestOnly`, `bypassLowTargetSdk`, `installer`, `user`, `deleteSource`, `splits`, `timeout`, `continueOnError`), `InstallerScriptArguments.kt` (来源规范化: 字符串路径 / `content://` / `java.io.File` / `android.net.Uri` / 数组 / `{ splits }`), `InstallerScriptValues.kt`.
- [x] (宿主) `runtime/api/augment/installer/Installer.kt` (`AugmentableKey("installer")`, `$installer` 别名与其它模块一致), `InstallSessionNativeObject.kt` (EventEmitter: `stage`, `progress`, `complete`, `error`, `cancel`; 方法 `cancel()`, `wait(timeout?)`, 属性 `id`, `state`, `result`), `InstallerJsErrors.kt` (`InstallerError` 构造与 `code` / `status` / `systemMessage` / `packageName`), `InstallerPromises.kt`; 在 `ScriptRuntime.augment` 装配 (与 `Mail` 相邻).
- [x] (宿主) 方法: `install` / `installAsync` (单个与数组重载), `session`, `uninstall` / `uninstallAsync`, `inspect` / `inspectAsync`, `authorizer` 对象 (`state(name?)`, `request(name)` / `requestAsync`, `available`), `isDefault` / `setDefault` / `setDefaultAsync`, `users` / `usersAsync`, `isAvailable`, `status` 属性; 草案见附录 A.
- [x] (测试) JVM: 选项 / 来源规范化边界 (空, 非法枚举, 超限数组, Unicode 路径), 错误映射, 数组与 `{ splits }` 判别; 宿主 `testAppDebugUnitTest` 通过. (JVM 2026-10-01: 宿主 3,272 项, 0 失败, 6 项既有跳过; 其中 installer 82 项全过; 插件 201 项全过, 含单元素批量协议回归.)

### P4.2 会话形态与同步等待

- [x] (宿主) 同步形态阻塞当前脚本线程 (Looper 脚本用 `Condition` 等待, 与 `mail` 一致), 停止脚本取消未完成调用; Async 形态回调在脚本线程; `session` 形态事件在脚本线程分发, 未监听 `error` 时不抛到全局 (记录到控制台).
- [x] (测试) 设备 (需 P2 插件): `install` 同步 / Async / session 三形态各一次 (Shizuku), `uninstall`, `inspect` (含 AAB), 停止脚本时会话被取消. (DEVICE 2026-10-01: API 35 真机 13 项全过无跳过, 含三形态安装 / Async 更新与卸载 / APK 与 AAB / 成功留源或删除 / 短 wait / cancel / wait 中 stop / 管道关闭 / 无插件错误; API 24 来源与终态竞争 8 项及无特权脚本 4 项全过. 见 docs/dev/p4-script-api-evidence.md.)

### P4.3 示例与守卫

- [x] (宿主) 示例脚本 `assets-app/sample/应用/静默安装应用.js`, `批量安装应用.js`, `设为默认安装器.js` (10 语言示例标题按既有示例目录约定); `assets-app/doc` 不在此改 (P7 生成).
- [x] (宿主) 宿主 `.changelog` 10 语言 `feature`: `installer 模块, 用于安装, 更新与卸载应用, 支持 Shizuku / Root 静默安装 (需要 3-Setup Installer 插件) (参阅 项目文档 > [安装器](链接))`.

验收条件: 三形态 + 全部方法在真机走通; JVM 测试覆盖规范化与错误; 示例脚本可运行; changelog 已写. (本轮完成 P4.1 至 P4.3 实现与上述设备测试; 同步 getter / setter 的无插件守卫已验收, 实际改写系统首选项及新授予特权的成功路径仍沿用 P2 / P5 / P6 设备矩阵, 未宣称已覆盖. 示例沿用现有资产中文文件名, 当前无 10 语言标题映射消费者. 四仓文档同步按宿主 AGENTS.md 完成, 对应原 P7.1 条目.)

---

## P5: 独立应用形态

目标: 插件可从启动器打开, 设置页遵循独立设置页规范, 默认安装器页可锁定 / 解锁, 关于 / 发行历史 / 更新检查齐备, 启动器图标四选项.

### P5.0 首页, 安装历史与批量队列 (D33 / D34 / D36)

- [x] (插件) `ui/HomeActivity` 为 launcher 入口 (四个 alias 的 `targetActivity`): 顶部授权方式状态卡 (Shizuku / Root 的可用 / 运行 / 授权三态, 一键请求, 未安装 Shizuku 时给出下载引导) 与默认安装器状态卡 (当前处理者, 锁定 / 解锁, 无特权时跳系统 "默认打开" 引导), 下方任务列表 (进行中会话实时进度 + 最近历史), FAB 选择安装包 (SAF `OpenMultipleDocuments`, MIME 列表复用 P3.2, 多选进入批量队列), 顶栏溢出菜单: 已安装应用, 设置; 空状态文案, TalkBack, RTL, 大字号, 进程重建; 中性色与主题色遵循独立设置页规范. (SOURCE / DEVICE 2026-10-01: 33f9341; 首页状态卡, 任务稳定复用, SAF 多选与菜单完成. API 24 / 35 最终 HomeActivityDeviceTest 各 3/3, 七种授权状态显示有断言; 真实窗口 / RTL / 字号 2 / IME 范围见 docs/dev/p5-standalone-evidence.md.)
- [x] (插件) `history/InstallHistoryStore`: JSON 文件 + 原子写, 上限 200 条 (超出丢弃最旧), 字段 包名 / 标签 / 版本 (旧 -> 新) / 结果 / 时间 / 来源 (host / script / external / home) / 授权方式 / 错误码与系统消息; 会话终态写入 (含批量逐项); 单条删除与清空确认; 不保存安装包内容或路径以外的信息. (SOURCE / JVM / DEVICE 2026-10-01: 原子 JSON, 来源/结果映射, 删除墓碑与晚写隔离完成; API 24 隔离私有存储实际写 224 条并验证最新 200 条, 清空及冷加载后无复活, 1/1 通过. 见 docs/dev/p5-standalone-evidence.md.)
- [x] (插件) `queue/InstallQueue`: 多选文件串行安装 (复用 P2.5 批量语义), 首页显示队列进度 (n / total, 当前项阶段), 取消剩余, 跳过失败项继续; 队列存活于进程内, 进程重建后未开始的项不自动续跑并在历史标记 `cancelled`. (SOURCE / JVM / DEVICE 2026-10-01: 复用 P2 批量执行, 全来源展示与历史接入; API 24 / 35 HistoryQueueDeviceTest 覆盖坏来源继续, 取消剩余, 晚到成功及来源身份保护. 新进程未完成记录 cancelled 且不自动续跑.)
- [x] (插件) `ui/InstalledAppsActivity`: 已安装应用列表 (图标 / 标签 / 包名 / 版本, 搜索, 按名称 / 安装时间 / 更新时间排序, 显示系统应用开关), 点击展开操作: 卸载 (特权静默 + 保留数据开关, 无特权时系统确认, 复用 P2.4), 打开, 应用信息; 列表异步加载并缓存图标. (SOURCE / JVM / DEVICE 2026-10-01: 当前用户异步列表, 按需图标缓存和确认卸载完成; API 24 / 35 搜索/排序/系统开关/重建用例通过; API 35 Shizuku 经真实列表和确认卸载自建夹具, 1/1 通过并完整清理.)
- [x] (插件) `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 接收安装包 (P3.2 的入口 Activity 增加 filter, MIME 同 `ACTION_VIEW`), 单项进入安装对话框, 多项进入批量队列. (SOURCE / DEVICE 2026-10-01: 分享转 InstallQueue, 保留受限 grants 与单项 batch 身份, 不接受分享方覆盖本地选项; 补 MT APKS MIME application/vnd.android.package-archives. 设备分享解析用例通过, 用户 MT APK 确认页反馈单独记录, 不冒充 XAPK 安装验收.)
- [x] (测试) JVM: 历史编解码, 上限裁剪, 来源与结果映射; instrumentation: 首页状态卡按授权状态渲染, 历史 200 条上限与清空, 队列取消与跳过, 已安装列表搜索与排序, `ACTION_SEND` 入口解析. (JVM / DEVICE 2026-10-01: 插件 243 JVM 无失败; P5 基础组合在 API 24 / 35 各 20/20, 无跳过; 后补历史 200 条/清空设备用例与真实卸载各 1/1, 最终 Home 状态各 3/3. 详细分组和运行边界见 docs/dev/p5-standalone-evidence.md.)

### P5.1 设置页

- [x] (插件) `SettingsActivity` (代码构建的分组平面列表, 复制 3-Stove Agent `ui/kit` 套件后裁剪): 外观组 (语言 / 夜间模式 / 主题色 / 启动器图标, 默认跟随 AutoJs6, 经官方 host settings 契约读取, 宿主不可用回退系统与 `#FFDEAD`); 安装组 (授权方式顺序与启用 (拖动或上下移动), 默认交互 (`auto` / `dialog` / `silent`), 允许降级, 允许测试包, 绕过低 targetSdk, 安装者包名 (空 = 本插件; HyperOS 提示 `com.android.shell`), 目标用户, 安装后删除源文件); 通知组 (进度通知开关); 关于组 (默认安装器状态卡入口, 关于, 发行历史, 检查更新). (SOURCE / DEVICE 2026-10-01: 33f9341; 设置页及本地默认选项完成, 语言/夜间/主题色跟随宿主, 启动器 Auto 遵循系统资源. Home/外部默认 dialog, 脚本默认 auto 不变; API 24 / 35 设置与真实窗口验收通过, 细分限制见证据文档.)
- [x] (插件) 先选后确定的对话框语义, 中性色与主题色规则, 72 dp 行高与 24 dp 留白, TalkBack 与 RTL; 设置项持久化到 `SharedPreferences` (授权顺序为 JSON 数组), `AuthorizerResolver` 读取. (SOURCE / JVM / DEVICE 2026-10-01: 确认后保存, 取消不写, 授权顺序/启用 JSON 与解析恢复, 中性色和既有 HCT 配色完成; API 24 / 35 选择/取消/重建用例通过, API 35 RTL / 字号 2 / 360 dp 窄内容与实际 HEX 键盘通过.)
- [x] (插件) 宿主设置入口: `InstallerSettingsActivity` (action `org.autojs.plugin.INSTALLER_SETTINGS`, PLUGIN 权限, `Theme.NoDisplay` 转发). (SOURCE / DEVICE 2026-10-01: Manifest 声明 INSTALLER_SETTINGS 与 PLUGIN 权限, NoDisplay Activity 转发 SettingsActivity; 宿主已有约定发现入口, PluginContract 在 API 24 / 35 检查通过.)
- [x] (测试) JVM: 授权顺序序列化与非法值恢复, HEX / RGB 解析; instrumentation: 先选后确定 / 取消不保存 / 重建后值保持. (JVM / DEVICE 2026-10-01: InstallerPreferencesTest, ThemeColorValue 与更新/历史策略测试通过; SettingsDeviceTest 在 API 24 / 35 覆盖确认/取消/非法输入/重建保持, 显示测试验证真实键盘取消草稿不保存.)

### P5.2 默认安装器页

- [x] (插件) `DefaultInstallerActivity` (从首页状态卡与设置页行进入, D35): 状态卡 (当前默认处理者组件名, 是否本插件, 检测方式 D23), "设为默认" / "取消默认" 按钮 (特权路径, 选择授权方式), 无特权时的引导 (打开系统应用详情 "默认打开" 并说明步骤), 结果与失败原因 (OEM 限制按事实展示, 不承诺); 从宿主 / 脚本 `setDefault` 复用同一 `DefaultInstallerLock`. (SOURCE / DEVICE 2026-10-01: 33f9341; 首页/设置/脚本复用同一 DefaultInstallerLock, 状态/特权确认/OEM 结果与无特权引导完成, 六页显示测试包含默认安装器页. 下项真实锁定矩阵仍未执行, 不以只读页面覆盖替代.)
- [x] (测试) 设备: Shizuku 与 Root 各锁定 / 解锁一次, 锁定后从系统文件管理器打开 `.apk` 直接进入插件; API 24 / 31 / 35 三台. (DEVICE 2026-10-01: Shizuku在API24/31/35, KernelSU Root在维护者新提供的XQ-DQ72 API33完成真实页面锁/解锁及系统DocumentsUI点击夹具直达插件确认. 四种公开解析, 全部其它默认项与偏好恢复均核验, 每台最终1/1无跳过通过; 独立Files证据与中间失败分开记录. Root经维护者授权后保留剩余两条InstallerX最近使用及原有通配历史, 未清MT或应用数据. 自有文件/取消历史已清理, AVD Shizuku恢复停止, 见 docs/dev/p5-default-installer-evidence.md.)

### P5.3 关于, 发行历史与更新检查

- [x] (插件) `AboutActivity` (圆角容器 + 透明图案, 版本 / 作者 / 仓库 / 许可证 / 第三方声明入口), `ReleaseHistoryActivity` (按 locale 读取 `doc/CHANGELOG-{tag}.md`, 回退英语), 更新检查 (固定 GitHub Releases API, 超时 / 取消 / 失败提示 / 忽略版本 / 12 小时频率限制, 对话框 Neutral 按钮打开内置发行历史, Positive 打开发布页). (SOURCE / JVM / DEVICE 2026-10-01: 33f9341; 十语言离线历史与法务文档, 固定端点手动检查, 12 小时间隔/忽略版本/取消/有界响应完成. HTTP 异常与取消使用 JVM 受控连接验证; 未发布或用真实新发行版做线上提示验收.)
- [x] (测试) instrumentation: 发行历史各语言加载; JVM: 版本比较与忽略逻辑. (JVM / DEVICE 2026-10-01: AppUpdatePolicyTest 的版本/忽略/频控/未发布/重定向/响应上限/取消通过; SettingsDeviceTest 在 API 24 / 35 加载全部十语言历史和本地法务文档.)

### P5.4 启动器与图标

- [x] (插件) `LauncherActivity` (MAIN / LAUNCHER 移至四个 alias: `AdaptiveLightIconAlias` / `AdaptiveDarkIconAlias` / `AdaptiveAutoIconAlias` (默认启用) / `TransparentIconAlias`), `LauncherIcons` 切换 (`DONT_KILL_APP`, 先启用后禁用, 快捷方式归属迁移), `LauncherIconUpdateReceiver` (`MY_PACKAGE_REPLACED` 幂等修复); 图标资源由 P0.1 生成器产出, 用真机截图复核光学居中并按需设置 `OPTICAL_X` / `OPTICAL_Y` 后重新生成. (SOURCE / DEVICE 2026-10-01: 33f9341; 四 alias 指向 HomeActivity, Auto 默认, 幂等更新与可变快捷方式迁移/失败回滚完成. 使用既有生成器, API 35 关于页真实透明图案容器截图复核居中, 未调整源图或光学偏移.)
- [x] (测试) `LauncherIconResourceTest` (透明 BitmapDrawable, 固定亮暗不随主题, 自动随配置且 undefined 回退暗色, API 26+ 自适应类型); instrumentation 四模式切换唯一入口 / 进程不死 / 重建持久化. (DEVICE 2026-10-01: API 24 / 35 LauncherIconResourceTest 与 LauncherIconSelectionDeviceTest 通过, 验证真实 APK 的 ActivityInfo 资源身份/亮暗/undefined, 四模式唯一入口, PID 不变, 持久化与可变快捷方式归属.)

验收条件: 设置页, 默认安装器页, 关于 / 发行历史 / 更新检查, 四 alias 图标在 AVD API 24 与真机 API 33+ 验收; 证据写入 `docs/dev/p5-standalone-evidence.md`.

---

## P6: 健壮性, 安全, 兼容矩阵, 性能与体积

目标: 敌意输入, 进程死亡, 存储不足, 超大包, OEM 差异与并发均有确定行为; 发布 APK 体积与安装耗时有记录.

### P6.1 健壮性

- [x] (插件) 进程死亡矩阵: 宿主死亡 (会话取消 + 暂存清理), 插件主进程死亡 (特权进程会话 `abandon`, 前台服务重建后不重复安装), 特权进程死亡 (`DeadObjectException` -> `AUTHORIZER_UNAVAILABLE`, 重绑一次后再失败), Shizuku 服务停止. (SOURCE / JVM / DEVICE 2026-10-01: 官方宿主 UID 独立调用进程死亡触发 CANCELLED 并清除 262,144 字节实际暂存; API 35 Shizuku/API 33 Root 主进程真实死亡后平台 session 消失, 新 PID 历史中断且空 FGS 不重放. 两种特权路径各四项真实 peer 测试通过, 握手最多重绑一次, 已开始的创建/写入/提交/卸载不重放. API 24 临时 Shizuku server 停止 1/1, 正常退出 hook 先清理会话. 完整系统身份具备时才能精确恢复已知 id, AOSP 为 API 33+, 不承诺低 API SIGKILL 或创建答复丢失窗口的自动清理. 见 docs/dev/p6-process-death-evidence.md.)
- [x] (插件) 存储不足 (`INSTALL_FAILED_INSUFFICIENT_STORAGE` -> `INSUFFICIENT_STORAGE`), 暂存目录清理策略 (启动时清理超过 24 小时的残留), 超大包 (>= 2 GiB xapk 流式写入不占用等量堆), 同一包名并发会话串行化. (SOURCE / JVM / DEVICE 2026-10-01: 同包安装跨用户/授权方式公平串行, 等待取消/超时与每项清理完成后释放; 独立入口首用回收24小时残留并保护活跃目录/不跟随symlink; 真实errno及私有Binder写失败原因映射空间不足. 新增17项JVM, 261项全量通过; API24/35/33空间故障注入均5/5, Root33/Shizuku35真实v1/v2并发及等待取消各2/2, 核验第二项previousVersionCode=1和夹具/源/历史清理. 既有2GiB流式设备证据保留, 本轮未重测堆峰值, 不将故障注入冒充整盘耗尽; 见 docs/dev/p6-robustness-evidence.md.)
- [x] (测试) instrumentation 覆盖上述矩阵; 用 `am kill` / `Shizuku` 停止模拟. (DEVICE 2026-10-01: 采用核验 PID/UID/名称/出生 ticks 后的 SIGKILL; OEM 拒绝 run-as signal 时使用仅 Debug/DUMP 的自终止入口, 不假造 Binder 死亡. Root peer 4/4, Shizuku peer 四项通过另有一项显式 server PID 缺失跳过, API 24 临时 server 停止 1/1; 三次独立 host/main 驱动完整通过并恢复原偏好/清理自有数据. API 24/35 生命周期和安全回归各 11/11. 初轮残留和探针故障均保留, 不计为通过; 268 JVM 全量通过, 证据及复测命令见 docs/dev/p6-process-death-evidence.md.)

### P6.2 安全

- [x] (插件) `HostCallerGuard` 校验 (签名 / 包名), 外部入口不信任文件名与 MIME, PFD 只读, 特权 Binder 只对本进程暴露 (UserService / RootService 的 Binder 不导出), 日志不记录路径以外的文件内容, `allowBackup=false`, 导出组件最小化审查表写入 `docs/dev/security-checklist.md`. (SOURCE / JVM / DEVICE 2026-10-01: HostCallerGuard 与私有 UID/session 守卫审阅, 合并导出组件和备份排除表见 docs/dev/security-checklist.md; 修复外部 provider 只请求 r 却未校验实际 FD 的缺口. 新安全四项在 API 24 / 35 / 33 均通过, 验证正式服务非宿主拒绝, 可写 FD 拒绝与关闭, 正常文件/content/pipe 只读兼容.)
- [x] (插件) 附录 E.2 回退路径 (`pm install-*` 命令) 若在 P0 启用, 命令参数全部白名单化, 不拼接用户字符串. (N/A / SOURCE 2026-10-01: P0.2 已决定不启用 pm install-* 回退, 本轮亦未增加; 因而没有待实现的命令拼接或白名单路径, 不把不适用记作已运行命令测试. 见 docs/dev/security-checklist.md.)
- [x] (测试) instrumentation: 非宿主调用方被拒绝; 静态检查导出组件与权限. (JVM / DEVICE 2026-10-01: 261 JVM 全量无失败, 含 ManifestContractTest/CallerPolicyTest/新增 SecurityConfigurationTest. SecurityBoundaryDeviceTest 四项在 API 24 / 35 的 9项组合与 API 33 的11项组合中均无跳过通过; 使用实际插件UID调用正式安装服务的八项操作, 逐项同步拒绝且无回调. 未冒充安装了未签名攻击APK.)

### P6.3 兼容矩阵

- [x] (测试) 设备池 (附录 E.1): AVD API 24 (Shizuku ADB / none), Sony G8441 API 28, Sony XQ-AT72 API 31, Redmi 22120RN86C API 33, Xiaomi 23046RP50C API 35 (HyperOS 安装者包名事实), AVD API 37 (16 KB 页, 仅验证插件本身安装与运行); 每台: none 新装 / 更新, 可用特权路径静默安装 / 卸载, 外部入口, 默认安装器锁定 (可用身份); 结果表写入 `docs/dev/p6-matrix-evidence.md`. (部分 DEVICE 2026-10-01: 原五台设备 none 新装/更新均通过, API 24/31/35 Shizuku 和 API 28 Root 静默新装/更新/卸载通过, 五台显式外部 Activity/取消各 1/1. API 24/35 默认页锁定/解锁各 1/1 且基线恢复; G8441/XQ-AT72 因原 APK preferred/last-chosen 保护性跳过. Redmi 没有可用特权身份, 不用额外 Root Sony 替代. HyperOS 三种请求安装者事实已记录. API 37 实际 PAGE_SIZE=16384 的 build 40 Release 安装/首页/跨进程契约限定行完成. 两台手机默认项及各 OEM 实际文件管理器/隐式入口仍有缺口, 原 checkbox 保留.) (补充 DEVICE 2026-10-01: G8441 Root默认页锁定/解锁及锁定期间真实Files直达确认后取消1/1, 原系统安装器最近使用完整保留. 新保留模式4项守卫在API 24/28/31各4/4. 未锁定G8441真实chooser有插件; Redmi真实Files则直接进入系统安装器, 退出且原状态不变. XQ-AT72原插件记录仍保护, 默认页未执行; 解析查询前后观察到stopapp旧首选降为最近使用, 未发出清除命令, 见 docs/dev/p6-default-history-evidence.md 和 docs/dev/p6-oem-external-completion-evidence.md.) (完成 DEVICE 2026-10-02: 维护者批准后, XQ-AT72 Shizuku真默认页/Files/取消/解锁1/1, 四条原插件最近使用及其他记录/8条历史均保留. 新6项guard在API 24/31各6/6, API 24普通Root默认页另1/1. 原六台范围的完整结果映射见 docs/dev/p6-approved-default-completion-evidence.md; Redmi实际系统路由限制/N/A身份明确保留, 不声称所有OEM入口都成功. 本轮未在线设备使用已记录证据, 不冒充同包全池重跑.)
- [x] (插件) OEM 差异按事实进入文案与 README 常见问题 (HyperOS 安装者包名, ColorOS 停止状态需激活, 部分 ROM 限制默认安装器). (DOCS / DEVICE 2026-10-01: 十语言 README/宿主插件说明同步兼容性和 FAQ. HyperOS 仅陈述实测 23046RP50C/API 35/Shizuku 的默认 shell 及两种显式安装者结果; ColorOS 激活提示按 Android 停止状态规则给出条件处理, 明确尚无 ColorOS 专项实机验证. 默认项文案保留旧 API/ROM 拒绝和先清原项限制, 1.0.0 不承诺持久锁定. 文档/图标生成校验通过, 不将通用规则或 protected skip 写成 OEM 实测.)

### P6.4 性能与体积

- [x] (测试) 记录: release APK 体积 (预期 < 3 MiB, libsu + Shizuku + HiddenApiBypass 无原生库), 100 MiB APK 在 Shizuku / Root / none 三路径的安装耗时, 特权进程冷启动耗时, 空闲 PSS; R8 规则 (隐藏 API 存根 `-keep`, libsu `RootService` 类名保留, AIDL Stub 保留) 与 release 构建的 `-PandroidTestRelease` 设备往返. (RELEASE / DEVICE 2026-10-01: 同一 build 40 签名 R8 APK 为 1,890,319 字节/约 1.803 MiB, 无原生库, CRC32 96bfec75. API 24/31/33/35/37 各两项实际跨 UID/PID 元数据/Parcelable/权限往返通过; 官方宿主 UID 正向写入 104,866,213 字节, Shizuku 35/Root 33/none 24 安装总时间分别 1,176/924/9,694 ms, none 含两次确认等待, 不作跨设备性能归因. 特权服务首次 getUsers 含冷启动/握手/查询为 320/363 ms, Root 授权另 23 ms; 各设备两次无安装 FGS 的空闲 PSS 和初轮失败见 docs/dev/p6-release-performance-evidence.md. 生产 R8 未增加测试 keep, 隐藏接口直接反射系统 Stub 而无打包存根; 测试使用独立平台 Java runner, 与正式用户/包操作分开核验.)

验收条件: 矩阵表完整, 未覆盖项明确列出; 安全检查表完成; 体积与耗时记录; 全部 JVM / instrumentation / lint 通过.

---

## P7: 文档, 发布与 1.0.0 gate

目标: 四个关联仓库同步, 插件 README / changelog 定稿, GitHub 仓库建立, 官方索引登记, 签名发布.

### P7.1 文档与声明

- [x] (文档) `D:/webstorm-projects/AutoJs6-Documentation`: `api/installer.md` (模块页, 结构参照 `api/mail.md`: 插件依赖说明, `PLUGIN_UNAVAILABLE`, 同步 / Async / 会话三形态, `installer` 与 `$installer`), `api/installerInstallOptionsType.md`, `api/installerInstallResultType.md`, `api/installerSessionType.md`, `api/installerPackageInfoType.md`, `api/installerErrorType.md` (或合并进模块页, 按既有 mail 页面粒度); `api/sidebar.md` / `api/toc.md` 登记; `api/app.md` 的 `uninstall` 与 `api/installer.md` 的 `uninstall` 互相提示对方的存在与用途简述 (D32); 模块页与 README / 插件说明明确提示 "特权授权可用时脚本安装默认静默进行, 需要确认时传 `interaction: 'dialog'`" (D32); 运行 `generator/auto-generate-for-autojs6.bat`, 随后提交文档仓库与 `AutoJs6-Plugin-Offline-Docs` (版本号自动变更).
- [x] (文档) `D:/webstorm-projects/AutoJs6-TypeScript-Declarations`: `declarations/autojs6/aj6-int-installer.d.ts` (`@Source` 指向宿主 `runtime/api/augment/installer/*.kt` 与 `runtime/api/installer/*.kt`, `Internal.Installer` 命名空间, 重载与事件类型), `index.d.ts` 引用; 运行 `D:/idea-projects/android-dts-generator/aj6dts.bat -Publish`; 声明仓库与 `AutoJs6-Plugin-Ace-Editor` 的 `aj6-int-installer.d.ts` 同步, 两仓库版本号 +1 且版本名称按语义升级 (新增模块 -> y+1), Ace 仓库执行 `:app:generateAutoJs6LspDeclarations`; 分别提交.
- [x] (宿主) `docs/dev/installer-plugin-protocol-v1.md` 补齐脚本 API 章节; 宿主 changelog 核对 (P1.5 / P4.3 已写条目合并整理, 日期为当日).

### P7.2 插件 README 与 changelog

- [x] (插件) `.readme/lang_*.json` 10 语言: 简介, 功能 (安装 / 更新 / 卸载 / 批量 / 分包 / 静默 / 默认安装器), 安装 (插件中心向导或 Release), 授权方式说明 (Shizuku / Root 各自前提), 脚本示例 (`installer.install`, `installAsync`, `session`, `uninstall`, `setDefault`), 兼容性 (Android 7.0+, 特权能力的框架限制按事实), 常见问题 (HyperOS 安装者, 降级限制, 默认安装器被 ROM 限制, AAB), 发行历史, 许可证与第三方声明; 生成器 `--check` 通过. (DOCS / JVM 2026-10-01: 十语言补齐五类API与分包/会话/错误示例, 区分脚本auto和宿主界面dialog, 明确线程归属, 单独授权/逐项来源保留/ROM限制/未发布状态; 10语言36生成物一致. 已对照宿主源码并经独立审阅.)
- [x] (插件) `.changelog` 10 语言 `v1.0.0` 定稿 (`feature` / `improvement` / `dependency` 分类, 依赖用 `附加` 术语记录 Shizuku-API / libsu / HiddenApiBypass / common-plugin-api / installer-api / package-archive-parser). (DOCS / JVM 2026-10-01: 合并为1条提示/15条功能/4条优化/6条依赖, 覆盖实际交付行为, 简中依赖均用附加; 发布/索引待完成状态与设备限制保持, 内置发行历史和README同步生成.)

### P7.3 发布 gate

- [x] (发布) 平台验收构建 (Temurin 参数) + `:app:testDebugUnitTest` + `:app:assembleDebugAndroidTest` + `:app:lintDebug` + `:app:appendDigestToReleasedFiles` (签名 APK, CRC32 文件名 `autojs6-plugin-three-setup-installer-v1.0.0-XXXXXXXX.apk`); `git diff --check`; `VERSION_BUILD == git rev-list --count HEAD`; 工作树干净. (RELEASE / JVM / DEVICE 2026-10-01: build 48平台检查通过, 268 JVM零失败/跳过, Debug/Release lint零错误, 签名R8包CRC32=8601e5e9, 1,902,783字节. API 24/35/37同包跨进程契约各2/2, 八台保留数据覆盖Release并核验. VERSION_BUILD与最终提交数48一致, 工作树干净; 仅本地gate, 不代表原P2/P6缺口或远端发布已完成. 见 docs/dev/p7-local-gate-evidence.md.) (刷新 RELEASE / DEVICE 2026-10-02: build51全部本地检查通过, 268 JVM零失败/跳过, 签名APK CRC32=cf864c74, 当前4台保留数据交付, API24/31/33契约各2/2, Samsung/API36/16KiB独立首页通过. VERSION_BUILD与最终提交数51一致且工作树干净, 见 docs/dev/p7-matrix-complete-release-evidence.md.)
- [ ] (发布) GitHub 仓库 `SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer` (功能性描述, 例如 `App installer plugin for AutoJs6 with Shizuku and Root silent installation`), 推送, tag `v1.0.0`, Release 附 APK 与 SHA-256.
- [ ] (索引) `official-repositories.json` 插入仓库名 (字母序, 总数 45 -> 46, README 计数同步), `release-manifests/io.github.supermonster003.autojs6.plugin.three.setup.installer/<versionCode>.json` 准入清单, 本地运行生成器验证后提交推送 `main`, 确认 Actions 生成成功.
- [ ] (宿主) 宿主提交 (`feat(installer): ...` 系列) 是否推送按维护者指示; 宿主 `PluginInstallWizardCatalog` 条目在索引可解析后生效.

验收条件: 四个关联仓库已提交; 插件 Release 与索引条目可被宿主插件中心向导发现并安装; 1.0.0 关闭.

---

## P8: 1.1.0 (Dhizuku, 持久化默认安装器, 通知栏模式, APK Inspector 迁移)

目标: 补齐维护者第一轮未纳入 1.0.0 的授权方式与安装模式, 并让兄弟仓库共用解析 AAR.

- [x] (插件) Dhizuku 授权方式: 依赖 `io.github.iamr0s:Dhizuku-API` (核实许可证后记录), `DhizukuAuthorizer` (`Dhizuku.init` / `requestPermission`), `DhizukuInstallEngine` (`DevicePolicyManager` 所有者上下文的 `PackageInstaller`), 能力上报 `AUTHORIZERS` 增加 `dhizuku`; 脚本 `authorizer: 'dhizuku'`; 契约版本 2 (末尾追加方法, 旧顺序不变). (2026-10-02, API 2.6.0/MIT, API31实际安装/更新/卸载和有归属的进程死亡恢复; V1/V2共存. 见 docs/dev/p8-dhizuku-session-evidence.md 与 p8-integration-evidence.md.)
- [x] (插件) 持久化默认安装器: Dhizuku 路径 `DevicePolicyManager.addPersistentPreferredActivity` / `clearPackagePersistentPreferredActivities`; Root 以 system 身份的 spike (libsu 自定义 `su 1000` 或 `app_process` uid 切换) 若成立则也提供; 状态卡区分 "偏好" 与 "持久化". (2026-10-02, Dhizuku/API31往返; Root/API24/Magisk真实UID1000成立并实现独立生产桥, Debug6/6及R8六操作通过. Dhizuku暂限API26-33; 本地回执不冒充实时策略. 见 docs/dev/p8-root-system-spike-evidence.md.)
- [x] (插件) 通知栏安装模式 (`interaction: 'notification'`): 无对话框, 通知承载确认 (none 路径仍需系统确认) 与结果; 设置页默认交互增加该项. (2026-10-02, Sony/API31 Shizuku及none各真实安装1/1, 跨UID来源/并发取消1/1, 系统实际重建后不重开来源; Samsung/API36通知拒绝1/1. 见 docs/dev/p8-notification-evidence.md.)
- [x] (兄弟) APK Inspector 改为消费 `package-archive-parser.aar`, 删除其分叉副本中与 AAR 重合的文件, 专有解析保留; 其 `ROADMAP.md` 与 changelog 记录; 宿主 `docs/dev/package-inspection-roadmap.md` 同步. (2026-10-02, Inspector 17fe20d / 1.2.2 / build 42, 231 JVM, Debug 3/3, 独立 Release 2/2; 证据见该仓库 docs/development/shared-parser-migration-evidence.md; 宿主文档 8c3045d24e.)
- [x] (文档) 文档 / d.ts / Ace / 离线文档同步 `dhizuku` 与 `notification`; 宿主与插件 changelog. (2026-10-02, 宿主V2/回执, Docs code86, d.ts4.26.1, Ace1.18.1/build120, Offline6.8.5/build67及插件1.1.0十语言全部同步; 独立仓库提交和验证见 docs/dev/p8-integration-evidence.md.)

---

## P9: 1.2.0 (高级安装选项)

- [ ] (插件) `grantAllRequestedPermissions` (`INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS`, 特权), `requestUpdateOwnership` (API 34+ `setRequestUpdateOwnership`), `dexopt` (`pm compile` 或 `performDexOptMode`, 特权), 安装原因 / 包来源 (`setInstallReason`, `setPackageSource`).
- [ ] (插件) 签名门禁 (签名不一致 / 未知签名时默认拒绝并可在对话框放行), 包名 / SharedUID 黑名单 (设置页), 权限预览 (对话框展开 `uses-permission` 列表, 复用共享 AAR 清单解码).
- [ ] (插件) 按来源的配置文件 (宿主 / 脚本 / 外部入口 / 指定包名前缀 -> 默认授权方式与选项), 设置页管理.
- [ ] (宿主 + 文档) 脚本选项与文档 / d.ts 同步; 契约版本 3.

---

## 附录 A: 脚本 API 草案

### A.1 全局对象

```ts
declare const installer: Installer;
declare const $installer: Installer;

interface Installer {
    install(source: InstallSource, options?: InstallOptions): InstallResult;
    install(sources: InstallSource[], options?: InstallOptions): InstallResult[];
    installAsync(source: InstallSource, options?: InstallOptions): Promise<InstallResult>;
    installAsync(sources: InstallSource[], options?: InstallOptions): Promise<InstallResult[]>;
    session(source: InstallSource | InstallSource[], options?: InstallOptions): InstallSession;

    uninstall(packageName: string, options?: UninstallOptions): UninstallResult;
    uninstallAsync(packageName: string, options?: UninstallOptions): Promise<UninstallResult>;

    inspect(source: InstallSource): PackageArchiveInfo;
    inspectAsync(source: InstallSource): Promise<PackageArchiveInfo>;

    readonly authorizer: {
        readonly available: AuthorizerName[];
        state(name?: AuthorizerName): AuthorizerState;
        request(name: AuthorizerName, timeout?: number): boolean;
        requestAsync(name: AuthorizerName, timeout?: number): Promise<boolean>;
    };

    isDefault(): boolean;
    setDefault(enabled?: boolean, options?: { authorizer?: AuthorizerName }): boolean;
    setDefaultAsync(enabled?: boolean, options?: { authorizer?: AuthorizerName }): Promise<boolean>;

    users(): DeviceUser[];
    usersAsync(): Promise<DeviceUser[]>;

    isAvailable(): boolean;
    readonly status: { available: boolean; pluginVersion?: string; contractVersion?: number; reason?: string };
}
```

### A.2 来源与选项

```ts
type InstallSource = string | java.io.File | android.net.Uri | { splits: Array<string | java.io.File | android.net.Uri> };
type AuthorizerName = 'auto' | 'none' | 'shizuku' | 'root';   // 1.1.0: 'dhizuku'

interface InstallOptions {
    authorizer?: AuthorizerName;                 // 默认 'auto' (D8, D14)
    interaction?: 'auto' | 'dialog' | 'silent';  // 默认 'auto' (D18); 1.1.0: 'notification'
    allowDowngrade?: boolean;                    // 特权; user 版本 ROM 仅对 debuggable 应用生效
    allowTestOnly?: boolean;                     // 特权
    bypassLowTargetSdk?: boolean;                // 特权, API 34+
    installer?: string;                          // 安装者包名, 特权; 空 = 插件自身
    user?: number | 'current' | 'all';           // 特权; 默认 'current'
    deleteSource?: boolean;                      // D25; 默认 false
    continueOnError?: boolean;                   // 批量; 默认 true
    timeout?: number;                            // 毫秒; 默认 30 分钟 (D31)
}

interface UninstallOptions {
    authorizer?: AuthorizerName;
    interaction?: 'auto' | 'dialog' | 'silent';
    keepData?: boolean;                          // 特权
    user?: number | 'current' | 'all';           // 特权
    timeout?: number;
}
```

### A.3 结果

```ts
interface InstallResult {
    ok: boolean;
    packageName?: string;
    versionName?: string;
    versionCode?: number;
    previousVersionCode?: number;                // 更新时
    authorizer: Exclude<AuthorizerName, 'auto'>;
    interaction: 'dialog' | 'silent';
    durationMillis: number;
    sourceDeleted?: boolean;
    notes?: string[];                            // 例如 "bypassLowTargetSdk 在 API 33 上被忽略"
    error?: InstallerError;                      // 批量 / continueOnError 时失败项携带
}

interface UninstallResult { ok: boolean; packageName: string; authorizer: string; error?: InstallerError }
```

### A.4 会话 (EventEmitter)

```ts
interface InstallSession extends EventEmitter {
    readonly id: string;
    readonly state: 'pending' | 'preparing' | 'confirming' | 'writing' | 'committing' | 'completed' | 'failed' | 'cancelled';
    readonly result?: InstallResult | InstallResult[];
    cancel(): boolean;
    wait(timeout?: number): InstallResult | InstallResult[];
    on(event: 'stage', listener: (stage: InstallSession['state'], detail: { index?: number; packageName?: string }) => void): this;
    on(event: 'progress', listener: (progress: number, detail: { index?: number; bytesWritten: number; totalBytes: number }) => void): this;
    on(event: 'complete', listener: (result: InstallResult | InstallResult[]) => void): this;
    on(event: 'error', listener: (error: InstallerError) => void): this;
    on(event: 'cancel', listener: () => void): this;
}
```

### A.5 错误

```ts
interface InstallerError extends Error {
    readonly code: InstallerErrorCode;           // 附录 B.4
    readonly status?: number;                    // PackageInstaller.STATUS_* 或 -1
    readonly systemMessage?: string;             // EXTRA_STATUS_MESSAGE 原文, 例如 INSTALL_FAILED_VERSION_DOWNGRADE
    readonly packageName?: string;
    readonly authorizer?: string;
}
```

### A.6 检查信息与其它类型

```ts
interface PackageArchiveInfo {
    format: 'apk' | 'apks' | 'xapk' | 'apkm' | 'apkz' | 'zip' | 'aab' | 'unknown';
    installable: boolean;                        // aab / unknown 为 false
    packageName?: string; versionName?: string; versionCode?: number; label?: string;
    minSdk?: number; targetSdk?: number; size: number;
    icon?: android.graphics.Bitmap;
    splits?: Array<{ name: string; size: number; selected: boolean; reason?: string }>;
    signatureSchemes?: string[];                 // 'v1' | 'v2' | 'v3' | 'v3.1' | 'v4'
    installed?: { versionName: string; versionCode: number; signerMatch: 'match' | 'mismatch' | 'unknown'; installer?: string };
    problems?: string[];
}

interface AuthorizerState { name: string; available: boolean; running: boolean; granted: boolean; reason?: string }
interface DeviceUser { id: number; name: string; primary: boolean; running: boolean }
```

### A.7 示例

```js
// 静默安装 (自动选择 Shizuku / Root, 无特权时弹出插件对话框)
let r = installer.install('/sdcard/Download/app.apk');
console.log(r.ok, r.packageName, r.authorizer);

// 指定 Shizuku, 允许降级, 安装后删除
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? '完成' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// 会话形态
let s = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
s.on('progress', p => console.log(`${Math.round(p * 100)}%`)).on('complete', r => console.log(r.versionName));

// 批量, 卸载, 默认安装器
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

---

## 附录 B: 契约草案 (`plugin-api/installer-api`)

### B.1 Binder 面 (V1, 顺序冻结)

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `IInstallerPlugin` | `PluginInfo getInfo()` | 与 INFO 服务一致 |
| | `Bundle getCapabilities()` | `CONTRACT_VERSION`, `AUTHORIZERS` (String[]), `FEATURES` (String[]), `MAX_BATCH`, `MAX_SPLITS` |
| | `Bundle getAuthorizerState(String authorizer)` | `available` / `running` / `granted` / `reason` |
| | `void requestAuthorizer(String authorizer, IInstallerCallback cb)` | 结果 `granted` boolean |
| | `Bundle inspect(ParcelFileDescriptor source, String metadataJson)` | 返回 `resultJson` (A.6) |
| | `IInstallerSession openSession(ParcelFileDescriptor[] sources, String requestJson, IInstallerSessionCallback cb)` | 请求含来源清单 (每个 PFD 的显示名, 大小, 归属项), 选项 (A.2), `interaction`, `hostSessionId` |
| | `void uninstall(String requestJson, IInstallerCallback cb)` | `packageName`, `keepData`, `user`, `authorizer`, `interaction` |
| | `Bundle getDefaultInstallerState()` | `component`, `isSelf`, `method` |
| | `void setDefaultInstaller(boolean enable, String authorizer, IInstallerCallback cb)` | |
| | `Bundle getUsers(String authorizer)` | `usersJson` |
| `IInstallerSession` | `String getId()`, `String getState()`, `boolean cancel()`, `void close()` | `close` 释放暂存 |
| `IInstallerSessionCallback` | `void onStage(String sessionId, String stage, String detailJson)`, `void onProgress(String sessionId, float progress, String detailJson)`, `void onCompleted(String sessionId, String resultJson)`, `void onFailed(String sessionId, String errorJson)` | oneway |
| `IInstallerCallback` | `void onResult(String resultJson)`, `void onError(String errorJson)` | oneway |

### B.2 Bundle / JSON key

`InstallerContract` 集中定义: `KEY_CONTRACT_VERSION`, `KEY_RESULT_JSON`, `KEY_ERROR_JSON`, `KEY_AUTHORIZER`, `KEY_INTERACTION`, `KEY_SOURCES`, `KEY_OPTIONS`, `KEY_HOST_SESSION_ID`, `KEY_SPLITS`, `KEY_USER`, `KEY_INSTALLER_PACKAGE`, `KEY_DELETE_SOURCE`, `KEY_TIMEOUT_MILLIS`; 阶段常量 `STAGE_PREPARING` / `CONFIRMING` / `WRITING` / `COMMITTING` / `COMPLETED` / `FAILED` / `CANCELLED`; 授权方式 `AUTHORIZER_AUTO` / `NONE` / `SHIZUKU` / `ROOT`; 交互 `INTERACTION_AUTO` / `DIALOG` / `SILENT`; 用户 `USER_CURRENT` / `USER_ALL`.

### B.3 线程与生命周期

- 所有 `IInstallerPlugin` 方法可在任意线程调用, 插件内部转到单一调度线程; 回调 oneway, 宿主在 Binder 线程收到后转脚本线程.
- 会话对象随 `close()` 或宿主死亡 (`linkToDeath`) 释放; 未 `close` 的会话在完成 10 分钟后由插件回收.
- PFD 归宿主所有, 宿主在收到 `onCompleted` / `onFailed` 后关闭; 插件在 `openSession` 内 `dup` 后自持.

### B.4 错误码

| 代码 | 含义 |
| --- | --- |
| `PLUGIN_UNAVAILABLE` | 插件缺失 / 禁用 / 未授权 / 版本不兼容 / 绑定失败 / 进程退出 (宿主侧生成) |
| `INVALID_ARGUMENT` | 参数形态或上限错误 |
| `SOURCE_NOT_FOUND` / `SOURCE_UNREADABLE` | 来源不存在或无法打开 |
| `UNSUPPORTED_FORMAT` | AAB 或无法识别的容器 |
| `INVALID_PACKAGE` | 清单解析失败, 分包不一致, 签名者不一致 |
| `INCOMPATIBLE_DEVICE` | 没有匹配当前设备的分包 / minSdk 高于设备 |
| `AUTHORIZER_UNAVAILABLE` | 指定或自动选择的授权方式不可用 (未安装 / 未运行 / 版本过低) |
| `AUTHORIZER_DENIED` | 用户拒绝授权 |
| `AUTHORIZER_REQUIRED` | `silent` 或特权选项在无特权时被请求 |
| `USER_CANCELLED` | 用户在对话框或系统确认中取消 |
| `USER_ACTION_TIMEOUT` | 等待用户确认超时 |
| `SIGNATURE_MISMATCH` | `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 等签名冲突 |
| `INSUFFICIENT_STORAGE` | `INSTALL_FAILED_INSUFFICIENT_STORAGE` |
| `INSTALL_FAILED` | 其它系统失败, `status` 与 `systemMessage` 透传 |
| `UNINSTALL_FAILED` | 卸载失败 (系统应用 / 设备管理员 / 保护包) |
| `BLOCKED_BY_POLICY` | 用户限制 / 设备策略阻止 |
| `TIMEOUT` | 会话超时 |
| `CANCELLED` | 脚本停止或 `cancel()` |
| `INTERNAL` | 插件内部错误 |

### B.5 上限常量 (D31)

`MAX_BATCH_SOURCES = 32`, `MAX_SPLITS_PER_PACKAGE = 64`, `MAX_JSON_BYTES = 65536`, `MAX_INSTALLER_PACKAGE_LENGTH = 255`, `DEFAULT_USER_ACTION_TIMEOUT_MILLIS = 300000`, `DEFAULT_SESSION_TIMEOUT_MILLIS = 1800000`, `MAX_CONCURRENT_SESSIONS = 4`, `MAX_ICON_BYTES = 65536`.

### B.6 版本协商

- `CONTRACT_VERSION` 从 1 起; 宿主读取能力 Bundle 后按版本启用方法; 新方法追加到 AIDL 末尾, `InstallerAidlOrderTest` 冻结顺序.
- `REQUIRED_HOST_VERSION_CODE` 为交付 `installer-api` 的宿主构建号; 插件 `requiresHostVersion` meta-data 与之一致.

---

## 附录 C: 宿主改动清单 (按文件)

| 文件 | 改动 | 阶段 |
| --- | --- | --- |
| `settings.gradle.kts` | `pluginApi` 增加 `package-archive-parser`, `installer-api` | P1.1 / P1.2 |
| `plugin-api/package-archive-parser/**` | 新模块 (6 个源文件 + 6 个 JVM 测试迁入) | P1.1 |
| `plugin-api/installer-api/**` | 新模块 (4 个 AIDL, 5 个常量文件, 2 个测试) | P1.2 |
| `app/build.gradle.kts` | 依赖两个新模块; 删除 `installerEnabled` 占位符 (L981, L1006) | P1.1 / P1.4 |
| `app/src/main/AndroidManifest.xml` | 删除 L377-445 四个安装器组件; `<queries>` 增加 `org.autojs.plugin.INSTALLER` | P1.3 / P1.4 |
| `ui/main/scripts/{AndroidPackageArchive,BundletoolTocDecoder,ApkManifestDisplayDecoder,AabManifestDisplayDecoder,ApkSignatureDetector,PackageInspectionLimits}.kt` | `git mv` 到共享模块 | P1.1 |
| `ui/main/scripts/{PackageInstallerActivity,PackageInstallerEntryActivity,AndroidPackageSessionInstaller,PackageInstallStatusActivity,PackageInstallStatusCoordinator,PackageInstallStatusReceiver,PackageInstallStatusStore}.kt` | 删除 | P1.4 |
| `ui/main/scripts/ApkInfoDialogManager.kt`, `DisplayManifestActivity.kt` | 改用共享模块; 安装按钮走路由 | P1.1 / P1.4 |
| `core/plugin/installer/**` | 新增 `InstallerPluginHost`, `PackageInstallRouter`, `InstallerSessionClient`, `InstallerJson`, `InstallerErrorMapper`, `PackageChangeObserver`, `ThreeSetupInstallerOfficialPlugin` | P1.3 |
| `App.kt:140` | 删除 Coordinator 初始化 | P1.4 |
| `network/UpdateChecker.java:283` | 走路由 | P1.3 |
| `ui/project/BuildActivity.java:3138, 3153` | 走路由 | P1.3 |
| `model/explorer/ExplorerItem.java:100-106`, `ui/explorer/ExplorerItemViewHolder.kt:584, 618-622`, `ui/main/scripts/ExplorerFragment.kt:95` | 走路由 | P1.3 |
| `core/plugin/center/{PluginInstaller,PluginInstallActions,PluginCenterActivity,PluginCenterFragment,PluginInfoDialogManager}.kt`, `ui/main/plugin/PluginFragment.kt` | 走路由 | P1.3 |
| `core/plugin/center/wizard/{PluginInstallWizardInstaller,PluginInstallWizardInstallAwaiter}.kt` | 走路由; 等待改 D22 | P1.3 |
| `core/plugin/center/wizard/PluginInstallWizardCatalog.kt` | 增加 `three.setup.installer` 条目 | P1.3 |
| `core/plugin/center/{PluginCenterViewModel,InstalledPluginRepository,PluginSettingsFragment}.kt` | engine `installer` 的发现与设置入口 | P1.3 |
| `app/src/debug/.../ActivityLaunchCompatibilityProbe.kt:179` | 走路由或删除探针 | P1.3 |
| `util/IntentUtils.kt:150-166`, `com/stardust/util/IntentUtil.java:106-123` | 删除遗留 `installApk` | P1.4 |
| `res/values*/strings.xml` (10 目录) | 删除仅旧安装器使用的条目; 新增退化引导文案 | P1.4 |
| `app/src/test/.../PackageInstallStatusRecordTest.kt`, `androidTest/.../PackageInstallStatusStoreDeviceTest.kt` | 删除 | P1.4 |
| `runtime/ScriptRuntime.kt` | `installer: InstallerService` 字段与 `Installer(this, installer).augment(target)` | P4.1 |
| `runtime/api/installer/**`, `runtime/api/augment/installer/**` | 新增 | P4.1 |
| `assets-app/sample/应用/*.js` | 三个示例 | P4.3 |
| `docs/dev/installer-plugin-protocol-v1.md`, `docs/dev/package-archive-parser-v1.md`, `docs/dev/package-inspection-roadmap.md` | 新增 / 同步 | P1.5 / P8 |
| `.changelog/lang_*.json` (10) | `feature` / `improvement` / `dependency` | P1.5 / P4.3 / P7.1 |

---

## 附录 D: 待决事项

### Q1 (P1 前): 共享解析模块的命名与 APK Inspector 迁移时机

- 现状: 宿主与 APK Inspector 各持一份, 后者已分叉 (第 3.3 节).
- 推荐: 模块名 `plugin-api/package-archive-parser` (D28); APK Inspector 迁移放在 P8, 不阻塞 1.0.0.
- 拍板 (2026-09-30): 按推荐值实施; P1.1 已用 `plugin-api/package-archive-parser`, APK Inspector 迁移留在 P8.

### Q2 (P2 前): `auto` 顺序默认值

- 现状: InstallerX-Revived 由用户在配置文件中选择, 无固定顺序; 宿主双开卸载路径为 Shizuku -> Root -> 普通 shell.
- 推荐: `shizuku -> root -> none` (D14), 设置页可调.
- 拍板 (2026-09-30): 按推荐值实施 (D14).

### Q3 (P2 前): 特权可用时脚本 `interaction` 默认是否为 `silent`

- 现状: D18 定义 `auto` 在特权可用时优先静默, 系统仍要求确认时允许确认; 显式 `silent` 不允许转为确认 (2026-10-01 补充确认).
- 风险: 脚本无提示地安装应用; 但脚本本身已是用户授权运行的自动化, 且 `dialog` 可显式指定.
- 推荐: 维持 D18; 宿主入口 (文件管理器 / 插件中心) 固定 `dialog`.
- 拍板 (2026-09-30): 按推荐值实施 (D18), 且文档 (模块页, README, 插件说明) 必须明确提示用户: 特权授权可用时脚本安装默认静默进行, 需要确认时显式传 `interaction: 'dialog'` (D32).
- 补充拍板 (2026-10-01): `auto` 的系统确认回退需记录 `notes`, 实际结果为 `dialog`; 需要严格无确认时显式传 `silent`. 10 语言 README / 插件说明与宿主协议文档已同步此区别.

### Q4 (P1 前): 宿主 `app.uninstall` 是否在插件可用时改走特权静默卸载

- 现状: `app.uninstall` 为 `ACTION_DELETE` 系统对话框.
- 推荐: 不改 (D24), 保持既有语义; 需要静默用 `installer.uninstall`.
- 拍板 (2026-09-30): 按推荐值实施 (D24); `app.uninstall` 与 `installer.uninstall` 的文档互相提示对方的存在与用途简述 (D32).

### Q5 (P1 前): 宿主自更新是否经插件

- 现状: `UpdateChecker` 下载后调用 `PackageInstallerActivity.install`.
- 推荐: 经路由 (D21): 插件可用时进入插件对话框 (可选静默由插件设置决定), 否则系统安装器.
- 拍板 (2026-09-30): 按推荐值实施 (D21), P1.3 已接入.

### Q6 (P3 前): 前台服务类型

- 现状: API 34+ 要求声明类型; 候选 `dataSync` (语义接近, 有时长限制但足够) 与 `specialUse` (需 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 说明).
- 推荐: `dataSync`, 实测超时行为后定.
- 拍板 (2026-09-30): 按推荐值实施, `dataSync` 实测后定.
- 实施 (2026-10-01): API 35 两次真实 2 GiB 安装确认 dataSync 可启动并持续保护后台写入, 正式选定此类型; 保留系统累计超时清理实现, 不宣称已耗尽系统时长预算. 见 docs/dev/p3-notification-evidence.md.

### Q7 (P5 前): 默认安装器锁定的 UI 位置

- 现状: InstallerX-Revived 在首页状态卡; 本插件无首页, 启动器入口直达设置页.
- 推荐: 设置页 "关于" 组之前独立一行 "默认安装器" 进入 `DefaultInstallerActivity`.
- 拍板 (2026-09-30): 插件需要首页 (D33); 默认安装器入口为首页状态卡 + 设置页行 (D35); 首页附加能力见 D36; 首页设计允许适度自行发挥.

### Q8 (P7 前): GitHub 仓库创建与推送时机, 宿主提交是否推送

- 推荐: 插件仓库在 P0.1 初始提交后即创建远端并推送 (便于 CI 运行); 宿主提交按既有惯例本地保留, 由维护者决定推送.
- 拍板 (2026-09-30): 插件仓库可随时推送 (远端 `SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer` 于 2026-09-30 创建并推送); 宿主仓库只本地提交, 不推送.
- 最新指示 (2026-10-01): 插件仓库当前亦仅作本地提交, 暂时避免推送到 GitHub 远端. 此指示取代此前插件可随时推送的授权, 直至维护者明确恢复推送; 宿主仍只本地提交.
- 最新指示 (2026-10-02): P7 继续延迟, 可以开始 P8 及后续项实施. 仅解除 P8 对远端发布的等待, P7 原发布/索引/推送条目继续保留未完成; 不新增, 分拆或丢弃阶段条目.

### Q9 (P8 前): Root 以 system 身份 (uid 1000) 调用 `addPersistentPreferredActivity` 是否纳入

- 现状: InstallerX-Revived 未实现 (`canCallSystemRestrictedPreferredApis` 恒 false).
- 推荐: P8 做一次 spike, 成立则纳入, 否则只保留 Dhizuku 路径.
- 拍板 (2026-09-30): 按推荐值实施, P8 做一次 spike.
- 实施 (2026-10-02): API24/Magisk/Enforcing 下真实 system UID/GID1000 设置四项持久策略成立, 已纳入独立生产桥; 仅主用户0与设备实际支持的环境, 不更改共享RootService身份或SELinux. 未知/竞争策略保护, 已知重复项幂等与明确清除, 取消及R8实测范围见 docs/dev/p8-root-system-spike-evidence.md.

---

## 附录 E: 证据等级, 设备池与退路

### E.1 证据等级

| 标签 | 可以证明 | 不能证明 |
| --- | --- | --- |
| `SOURCE` | 源码存在, 结构符合设计 | 编译或行为正确 |
| `JVM` | Android-free 逻辑的单元测试 (JUnit4) | Binder / 特权进程 / 真机行为 |
| `ANDROID_BUILD` | `assembleDebug` / `testDebugUnitTest` / `lintDebug` / `assembleRelease` 通过 | 真机行为 |
| `BINDER` | 指定设备上的 instrumentation: 发现, 绑定, 往返, 敌意输入 | 特权路径与 OEM 行为 |
| `DEVICE` | 指定设备与 API 级别上, 以指定授权方式完成真实安装 / 卸载 / 锁定 | 未列出设备 / API / 授权方式 |
| `DOCS` | README (10 语言), changelog, 协议文档, 文档 / d.ts / Ace / 离线文档已同步且版本号已更新 | - |
| `RELEASE` | 签名 APK, CRC32 文件名, GitHub Release, 官方索引 receipt | 未明确覆盖的设备 |

条目勾选时在其后追加证据, 格式示例: `[x] ... (JVM: SplitSelectorTest 9 用例; DEVICE: Xiaomi 23046RP50C / API 35 + shizuku, AVD API 24 + root, 2026-10-xx; commit abc1234)`.

当前可用设备池 (以当日 `adb devices -l` 为准, 端口与 API 的映射每次重新读取): Xiaomi 23046RP50C (API 35, HyperOS), Sony G8441 (API 28), Sony XQ-AT72 (API 31), Redmi 22120RN86C (API 33), AVD API 24 (x86) / API 37 (16 KB 页). Shizuku 在 AVD 用 ADB 模式启动; Root 优先使用已 root 真机, 无则用可写系统镜像的 AVD + Magisk 并在证据中注明.

### E.2 退路: `pm` 命令路径

触发条件见 P0.2 决策点. 形态: `IPrivilegedInstaller` 增加 `execPm(String[] args)`, 特权进程以 `pm install-create [-r] [-d] [-t] [--user N] [-i installer] [--bypass-low-target-sdk-block]`, `pm install-write -S <size> <session> <name> -` (stdin 流式), `pm install-commit`, `pm uninstall [-k] [--user N]` 执行 (`pm` 没有设置默认安装器的子命令, 锁定仍走 Binder); 参数白名单化, 输出解析 `Success` / `Failure [<reason>]`; 能力 `FEATURES` 上报 `pm-fallback`.

---

## 附录 F: 参考与许可证边界

- InstallerX (`https://github.com/iamr0s/InstallerX`, GPL-3.0, 2023-08-04 归档): 安装模式与选项目录, "锁定为默认安装器" 的产品语义.
- InstallerX-Revived (`https://github.com/wxxsfxyzm/InstallerX-Revived`, GPL-3.0, 文档站 `https://wxxsfxyzm.github.io/InstallerX-Revived-Website/zh/`): 授权方式枚举, `ConfigModel` 选项目录, `InstallOption` 标志映射, `setDefaultInstaller` 的 `addPreferredActivity` 流程与 `addPersistentPreferredActivity` 的 uid 限制, HyperOS 安装者包名事实, 依赖版本 (Shizuku 13.1.5, Dhizuku-API 2.6.0, HiddenApiBypass 6.1).
- Shizuku-API (`https://github.com/RikkaApps/Shizuku-API`, Apache-2.0): `UserService`, `ShizukuBinderWrapper`, 权限请求.
- libsu (`https://github.com/topjohnwu/libsu`, Apache-2.0): `RootService`, `Shell`.
- AndroidHiddenApiBypass (`https://github.com/LSPosed/AndroidHiddenApiBypass`, Apache-2.0).
- AOSP `frameworks/base` (Apache-2.0): `IPackageInstaller.aidl`, `IPackageInstallerSession.aidl`, `IPackageManager.aidl`, `PackageManager` 隐藏 `INSTALL_*` 常量, `PackageInstaller.SessionParams` 隐藏字段; 存根按接口签名自写.
- 宿主先例: `docs/dev/mail-plugin-protocol-v1.md`, `docs/dev/official-plugin-settings-contract-v1.md`, `docs/dev/package-inspection-roadmap.md`; 兄弟仓库 `AutoJs6-Plugin-Angus-Mail/ROADMAP.md`, `AutoJs6-Plugin-Three-Stove-Agent/ROADMAP.md`.
- 许可证边界 (D29): 本仓库不引入 GPL 代码; 任何来自参考项目的 "实现思路" 在提交说明与 `THIRD_PARTY_NOTICES.md` 中只以 "参考" 表述; 若未来需要复用其代码, 必须先由维护者决定改变许可证或取得授权.

---

## 会话记录

### 2026-09-30

- 完成: 宿主安装器现状盘点 (`ui/main/scripts` 约 20 个类, 12 处调用方, Manifest 与字符串, 无脚本安装 API, 无安装器偏好, 安装从不使用 Shizuku / Root); 宿主特权基础设施盘点 (`WrappedShizuku` / `UserService` 仅 shell 语义, Root 为 `:libs:root-shell-1_6`, 无 libsu); 兄弟仓库范式盘点 (Angus Mail 契约 / 客户端 / augment, 3-Stove Agent 身份与设置 UI, APK Inspector 解析器分叉与 MIME 列表, Readium 双能力先例, 官方索引登记方式); 参考项目核实 (InstallerX 归档事实, InstallerX-Revived 的授权方式 / 配置模型 / 安装标志 / 默认安装器锁定实现 / 依赖版本); 维护者三轮拍板 D1-D12; 派生 D13-D31; 图标源图入库并改名为 `three-setup-ic-launcher-{light,dark}.png` (1254 x 1254, alpha 一致, `#272727` / `#D8D8D8`); 本 Roadmap.
- 说明: 本日更早的一次会话已写过一版部分完成的 Roadmap (至 P3.1) 但文件未能保留, 本版为从头完整重写, 决策内容与该版一致.
- 未做: 仓库骨架与 `git init` (P0.1), 任何宿主改动, 特权安装 spike (P0.2); 平台版本插件 1.8.3 的公共仓库可解析性与 libsu 当前稳定版本号未核实, 均留在 P0.1.
- 待维护者: 附录 D 的 Q1 / Q4 / Q5 在 P1 前拍板, Q2 / Q3 在 P2 前拍板; 确认是否在 P0.1 后立即创建 GitHub 仓库 (Q8).
- 下次会话建议起点: P0.1 全部条目 (骨架, `git init` 与初始提交) + P0.2 spike (隐藏 API 存根, `IPrivilegedInstaller`, Shizuku / Root 真机静默安装, `addPreferredActivity` 可行性); spike 通过后同一会话可开始 P1.1 共享解析 AAR.

### 2026-09-30 (P0.1 已交付记录回填)

- 之前的会话已完成 P0.1, 对应提交 `7f24555`, `5d5f942`, `54f03a9`, `66d58df`, `8db3334`; 原会话记录未追加, 本次按已有源码与证据回填.
- 基础验收: 14 JVM 用例, API 24 / 35 各 5 个 Binder 契约用例, debug 构建与 lint, 10 语言文档与 15 项图标检查. 详见证据文件 P0.1 节.

### 2026-09-30 (P0.2 特权验证)

- 完成: P0.2 六项, 私有 AIDL, Shizuku / libsu 共享实现, AOSP 签名适配, 流式写入与清理, 默认安装器验证, 可重建夹具, 显式指定设备的复测脚本.
- 修正: FileBridge 不能当普通文件写入; reliable pipe 的 socket 在 API 28 Magisk 到 app 的 SELinux 传递中被拒绝; 快速解绑 / 重绑需要确认旧 Binder 死亡; 取消时须关闭排队任务的输出流; API 24 / 28 不支持 MIME `replacePreferredActivity`, 有竞争默认项时返回需先清除状态. 均记录在证据文件.
- 验证: 17 JVM 用例, 四组设备各 3 个特权用例, API 31 真机额外安装链路; debug / androidTest / release 构建与 lint; 基础 Binder 契约回归; Markdown 与图标检查. API 31 真机的默认项用例跳过以保留用户偏好, 独立 API 31 AVD 已补齐.
- 范围: 本节包含跨版本接口, 两种特权进程及真实设备验收, 本次集中闭合 P0.2; 未修改宿主, 未创建远端或推送. 宿主调用 / 脚本安装入口仍未开放.
- 下次会话建议起点: P1.1 共享解析 AAR, 顺利时连做 P1.2 契约模块. Q1 / Q4 / Q5 保留为 P1 的维护者决策点; P2 / P5 消费 D23 的 `DEFAULT_REQUIRES_CLEAR` 状态, P6 补充大包 / OEM / 并发生命周期矩阵.

### 2026-09-30 (P1 宿主契约与安装器迁出)

- 完成: P1.1 共享解析模块 `plugin-api/package-archive-parser` (6 源文件 + 6 测试迁入, 52 用例), P1.2 契约模块 `plugin-api/installer-api` (4 AIDL, 5 常量文件, 8 用例), P1.3 宿主客户端 (`core/plugin/installer` 11 个文件, 14 用例) 与插件中心四处注册, P1.4 删除宿主安装器 (7 类, 2 测试, Manifest 四组件, `installerEnabled` 占位符, 遗留 `installApk`, 14 x 11 字符串) 并把 12 处调用方接到 `PackageInstallRouter`, P1.5 两份 `docs/dev` 文档, 宿主 10 语言 changelog, `VERSION_BUILD` 5299 回填到两侧; 宿主提交 3c31c686e6 (未推送). 插件侧: 两个 AAR 入 `libs/` 并锁定, 身份常量改由契约提供, 能力声明契约版本 1, `requiresHostVersion` 5299, `THIRD_PARTY_NOTICES` / `libs/README` / AGENTS / 10 语言 changelog 同步; 插件提交 a54229e (build 7) 与本记录提交 (build 8), 未推送.
- 派生决策 (按附录 D 推荐值实施, 待维护者确认): Q1 模块名 `plugin-api/package-archive-parser`, APK Inspector 迁移留 P8; Q4 `app.uninstall` 保持 `ACTION_DELETE`; Q5 自更新经路由. 另: `inspect` / `getUsers` 改为回调形式 (附录 B.1 已同步), 无插件时 `.aab` 直接提示不可安装, 每进程只提示一次安装插件.
- 验证: 宿主 `:app:compileAppDebugKotlin` / `:app:compileInrtDebugKotlin` / `:app:assembleAppDebug` 通过, `:app:lintAppDebug` 0 错误 / 2392 警告 (P1 前 2403, 新代码无警告; 默认 4 GB 守护进程堆在 lintAnalyzeAppDebug 阶段耗尽, 以 12 GB 堆单独重跑 8 分 41 秒通过); 插件 `:app:testDebugUnitTest` 17 用例, `:app:assembleDebug` / `:app:assembleDebugAndroidTest` 通过, `:app:lintDebug` 0 错误 / 12 警告 (PrivateApi 3, UnusedResources 5 (P5.4 alias 前的图标资源), 依赖新版本 4), `generate_markdown.py --check` 与 `generate_launcher_icons.py --check` 通过. 证据: `docs/dev/p1-host-evidence.md`.
- 未做: P1.4 的真机三处退化路径 (留待 P2 后与插件路径一并验收); 插件 androidTest 未在设备上重跑 (契约测试的能力断言已按 `installerContractVersion` 更新); 宿主与插件仓库均未推送; 宿主设置页的插件设置入口 (随 P5.1).
- 下次会话建议起点: P2.1 来源与格式 + P2.2 授权方式 (复用 P0.2 的 `priv/`), 顺利时连做 P2.3 安装引擎; P2.6 用 `IInstallerPlugin.Stub` 替换占位 Binder 时同时声明 `AUTHORIZERS` / `FEATURES` / `MAX_*` 能力.

### 2026-09-30 (附录 D 拍板与首页设计)

- 维护者回复附录 D 全部 9 项并经选择题确定首页设计, 回填为 D32-D36: Q1 / Q2 / Q4 / Q5 / Q6 / Q9 按推荐值; Q3 附加文档提示; Q4 附加两处卸载文档互相提示; Q7 首页 = 状态卡 + 任务列表 + FAB, 持久化历史 200 条, 默认安装器入口为首页状态卡 + 设置页行, 附加已安装应用列表 / `ACTION_SEND` 接收 / 批量队列; Q8 插件仓库可随时推送 (远端已创建并推送), 宿主仓库只本地提交.
- 路线图变更: 第 1 节增加 D32-D36, 第 4.2 节增加 `history/` 与 `queue/`, P5 增加 P5.0 首页小节, P5.2 与 P7.1 文案同步; P1.4 真机验证按建议延后到 P2 之后.
- 下次会话建议起点不变: P2.1 + P2.2, 顺利时连做 P2.3.

### 2026-10-01 (P2 批量会话, 用户与卸载)

- 完成: `InstallSession` 按项复用单包引擎, 批量 deadline / 部分失败 / 取消 / 终态清理 / 线程中断复位; PFD 所有权与同名来源隔离, 停滞管道可取消; 结果使用实际 interaction 并查询目标用户版本; `DeviceUsers` 与运行状态 / 目标校验; 普通系统确认与特权 `UninstallEngine`, keepData / all-user 标志与错误透传. P2.4 两项, P2.3 结果规范化和 P2.5 两个核心子项按证据勾选; P2.5 的 Binder 接入单独保留未完成.
- 验证: 66 JVM 用例, debug / androidTest / 混淆 release 构建, debug lint 0 错误 / 13 警告, release lint 0 错误 / 16 警告, 十语言 changelog 与文档 / 图标检查. API 35 Shizuku 与 API 24 none 各 12 项通过 + 1 项 Root-only 有意跳过, API 28 Root 的 13 项覆盖全部通过 (两台真机的卸载桥接测试修正主线程构造后单独复测). 真实批量均为 v1 / 坏包 / v2, 随后卸载; Root 额外验证保留数据卸载后重装的内容. 详见 `docs/dev/p2-session-evidence.md`.
- 未完成: 默认安装器协调层, `IInstallerPlugin.Stub` / CallerGuard / SessionRegistry / 宿主死亡与并发上限, inspect 增强和完整格式 / 分包 / 安装标志 / 次用户矩阵, P3 的完整插件确认 UI 与通知回退. 现有宿主 Binder 仍是占位实现, 未提前声明对应 capabilities, 不把内部引擎测试视为公开 Binder 路径验收.
- 工作区: 保留会话开始时已有的未提交 P2 代码和审阅改动, 按 AGENTS 3.3 不将未审计的旧工作混入自动提交; 本次也暂不提交, VERSION_BUILD 保持 10, 未推送或修改宿主. 下一次从默认安装器协调层与 P2.6 路由开始, 同时审计 `PrivilegedClient` 首次并发绑定, 接入显式 dialog 前必须补齐插件确认.

### 2026-10-01 (P2.6 Binder 路由与确认接入)

- 完成: 用 V1 `IInstallerPlugin.Stub` 替换占位服务, 接入全部方法, 官方宿主身份 / 所有者 / 请求封装 / 只读 PFD 校验, 四并发会话 / 有界队列 / 客户端死亡 / 十分钟回收; 能力按真实路由声明. `PrivilegedClient` 首次并发获取共享连接并覆盖取消 / 超时 / 重绑; 默认安装器协调层保留旧 API 清除提示并检查四个 filter; inspect 增强图标 / 签名 / 已安装信息; 十语言基础显式确认 Activity. 正式外部入口前不声明 default-installer, 来源删除路径前不声明 delete-source.
- 验证: 81 JVM 用例, debug / androidTest / 混淆 release 构建, 两种 lint 零错误 (13 / 16 警告), 十语言文档与图标检查. API 24 / 28 / 35 的 V1 边界 / 发现用例通过; Shizuku / Root 各验证真实路由包操作及四并发连接 / 关闭后重绑; AVD none auto / dialog 路由真实包操作通过, 拒绝插件确认时没有创建系统安装会话. API 28 签名字段兼容与解析器签名格式差异已修正. 逐项证据和跳过 / 复测范围见 `docs/dev/p2-binder-evidence.md`.
- 边界: 跨进程传输测试使用非导出的 debug 测试身份, 真实包操作通过主进程 V1 路由测试; 生产入口另测拒绝非宿主 UID. 尚未用官方宿主 UID 完整发起界面流程. API 35 首次显式确认因安全锁屏超时; 维护者随后解锁设备, API 35 Shizuku 与 API 28 Root 各补测显式确认安装 / 更新 / 卸载及拒绝确认, 均 OK (2 tests), 无跳过. 未绕过锁屏; Sony 的成功复测由 androidTest 内限定夹具的可访问性节点驱动, 不更改产品确认逻辑.
- 下一步: P3 完整信息 / 进度 / 结果界面, 外部入口及正式默认安装器启用, 系统确认接管 / 前台通知; 宿主 `AidlPluginHost.buildBindFlags` 缺少 API 34+ 的 `BIND_ALLOW_ACTIVITY_STARTS`, 需按 installer 范围启用并与后台通知回退一起验收. P2 的格式 / 分包 / 标志 / 次用户矩阵继续保留未完成.
- 文档与工作区: README / 插件说明 / changelog 已同步到 P2 状态; 宿主协议文档仅补充响应预算下的可选元数据省略规则, 不改 AIDL / AAR / 宿主代码. 保留此前未提交工作, 本次未提交或推送, VERSION_BUILD 保持 10.


### 2026-10-01 (P2 来源, 授权与批量验收收尾)

- 按原有条目完成 P2.1, P2.2 与 P2.5 的剩余验收, 新增勾选 8 项; 未增加, 拆分或丢弃路线图小节. 同时审计并按 P2.1-P2.6 六个逻辑提交纳入前几轮遗留的全部核心工作, 先前会话中的 "未提交" 说明保留为当时记录.
- 来源: 可 seek 来源优先直接读取, 无法重开的描述符与管道按需暂存; 保持宿主描述符所有权和偏移; 补普通 ZIP, 显式/容器 split 依赖与 64 上限, 解析前后和写入摘要检查. 共享解析器修复在宿主 0767971bc9, release AAR 已更新哈希锁.
- 授权: 明确不兼容 / 拒绝 / 超时, Root 授权与服务启动共同管理 shell 生命周期. API 24 连续回归实际发现 Shizuku 旧连接死亡通知影响新绑定, 已通过独立 tag 隔离并以 8 次立即重绑回归确认. 会话终态回收改为单个周期任务, 防止快速完成/关闭时积累延时引用.
- 设备: API 24 none / Shizuku, API 35 Shizuku, API 28 Root 验证来源, 安装标志, 真分包, 批量, 用户, Binder 与显式确认. API 24 工作资料暴露旧版用户状态 API 差异, 已修正并验证只安装到 user 10; 临时资料和全部测试夹具已清理. API 35 额外通过 3 个不同应用的单次批量安装.
- 验证: 插件 129 JVM 用例, 宿主解析器 54 JVM 用例, debug / androidTest / 混淆 release 与 lint (0 错误, debug 14 / release 17 警告), 十语言 Markdown 与图标检查通过. 分设备通过数, 有意跳过项, 首轮失败与修复过程见 docs/dev/p2-core-evidence.md; 未把整组中失败的初次运行记为通过.
- 保留未完成: P2.3 的完整系统确认接管, 来源删除与全设备矩阵; P2.6 正式宿主 UID 和三处入口联调; P3 的完整信息/进度/结果界面, 外部入口, 未知来源引导与前台通知. HyperOS API 35 未设置低 targetSdk 绕过标志就接受 targetSdk 22, 已验证带标志的新装但不声称解除实际拦截; user: all 的真实安装也未宣称完成.
- 下一步: 按原顺序从 P3.1 继续, 连同 P3.2/P3.3/P3.4 完成后收尾依赖它们的 P2 项目. 宿主本轮仅提交共享解析器修复及协议说明 (55712c7009), 未改公开 AIDL, 未推送任一仓库. 插件最终 VERSION_BUILD=17, 与本分支提交数对齐.

### 2026-10-01 (P3 安装界面, 外部入口与前台通知)

- 完成 P3.1-P3.4 的主要实现: 插件信息 / 确认 / 进度 / 结果与批量界面, 逐项选择实际进入引擎, 外部打开 / 分享与正式默认安装器, 系统确认与未知来源桥接, keepData 卸载确认, 宿主外观和 dataSync 通知. 原有条目新增勾选 9 项 (含 P2.3 引擎接管), 未增加或分拆小节.
- 平台适配: 文档任务要求 standard, 使用会话 token URI + intoExisting 达成原定任务隔离. API 24 确认 Activity 的 RESULT_CANCELED 不代表最终拒绝, 改等 PackageInstaller 广播. 宿主仅安装插件开启 API 34+ BIND_ALLOW_ACTIVITY_STARTS, 后台使用直接 Activity 通知入口.
- 设备: API 24 none 实装 / 来源删除 / 删除拒绝 / 重试, API 28 Root 真实 Binder dialog, API 35 Shizuku 真实宿主 UID 前台安装及未知来源设置拒绝通过. API 35 两次完成两枚 1 GiB APK 组成的真实 xapk 安装, 最终实际写入 2,147,500,874 字节, 后台 11,556 ms / 44 FGS 样本, 进程不变. 保留用户通知拒绝状态; API 24 单独验证可见通知更新. 详见 docs/dev/p3-ui-evidence.md.
- 构建: 插件 180 JVM 用例, debug / androidTest / 混淆 release 与两种 lint 通过; 宿主 3204 JVM 用例中 6 个既有跳过, 无失败, assembleAppDebug 通过. 十语言源文案 / 生成产物和图标检查通过. 实际用例分组, 首轮失败与修复, 后续回归见证据文档.
- 未完成边界保留原 checkbox: 跨进程持久化恢复与真实 IME / API 35 全几何, 文件管理器和浏览器下载 UI 的 APK / XAPK 矩阵, 实际首次授予未知来源后继续安装, 同一次 2 GiB 的可见通知, 宿主三处入口与 P2 完整标志 / 用户矩阵. P4 / P5 尚未开始.
- 提交: 插件按外观, 通知, 安装 UI, 系统 / 卸载确认, 外部入口, 设备验收与文档分开提交; 宿主前台绑定修复为 1e6d8d09eb, 探针任务清理为 53faf617a7. 遵循维护者本轮最新指示, 两仓库当前均仅本地提交, 不推送 GitHub. 插件最终 VERSION_BUILD=24, 与可达提交数对齐.
- 下一步: 优先补齐上述原有验收项和进程恢复, 然后按 P4 接入宿主 installer / $installer 脚本 API; 继续保持原有路线图结构.

### 2026-10-01 (P3 恢复与真实窗口 / 通知验收)

- 完成原 P3.1 剩余的展示恢复与真实窗口验证, 原 P3.4 的同次大包可见通知验收, 新增勾选 2 个原有条目; 未新增, 分拆或丢弃路线图小节. P4 尚未开始.
- 恢复: 私有有界原子快照, 关键结果落盘, 进度不写盘, 关闭 / 清除拒绝晚写, 设备重启后不复用 elapsedRealtime 原点. 新进程只读显示保存的已确认结果, 未完成项中断, 不恢复 worker / 描述符 / URI 授权, 不猜测成功或自动安装. API 35 PID 24697 -> 25743, API 28 PID 5734 -> 5783 的真实进程终止与恢复均通过; 探针明确使用合成展示数据, 不冒充实际系统安装结果.
- 窗口与通知: API 28 / 35 的真实键盘, RTL, 字号 2, 夜间, 横屏和固定按钮触摸各 3 项通过; API 35 同次 2 GiB 后台写入显示 27 个通知进度值, 46 个 FGS 样本. 详见三份专项证据与 docs/dev/p3-ui-evidence.md 的本轮补充.
- 授权: 新的真实 Settings 授权测试保持单 session / 单确认, 严格限于自建无代码夹具和本次任务. Sony 在授权和系统确认后遇到额外上传扫描, 测试只拒绝并等失败终态; HyperOS 在开关后遇到额外权限页, 测试不操作该页. 两台最终组合各 15 项通过 + 1 项有意跳过, 0 失败, 不把跳过计成功. 初次挂起夹具 session 已清理, 未改全局扫描设置或上传 APK.
- 验证: 插件 194 JVM 用例, debug / androidTest / 混淆 release, 两种 lint, 十语言 Markdown 和图标检查通过. 最终版本构建与具体警告数见本轮综合证据. 宿主和其他仓库未修改.
- 保留未完成: 实际文件管理器 / 浏览器下载列表 APK / XAPK 矩阵, 首次授予未知来源后完整安装成功, 宿主三入口与 P2 剩余矩阵. 本轮模拟器启动及 Chrome 启动均被自动审批拒绝, 仅返回 blocked by policy, 未绕过; 新 API 24 恢复 / 真实 IME 复验未执行.
- 工作区与下一步: 继续仅本地提交, 不推送 GitHub. 插件按恢复界面, 授权验收, 大包通知与路线图证据及取消文案修复分成 5 笔提交, VERSION_BUILD=29 与可达提交数对齐. 下一轮继续原有验收边界, 再推进 P4 服务层与脚本 augment.


### 2026-10-01 (P5 独立应用, P3 授权收尾与宿主信息菜单)

- 在上一轮 P4 与四套关联文档已完成的基础上, 交付 P5.0-P5.4 的独立首页, 私有 200 条历史, 队列与分享, 已安装应用管理, 设置/默认安装器/关于/发行历史/手动更新及四 alias 图标. 新增勾选原 P5 15 项和 P3.3 测试 1 项, 不新增, 分拆或丢弃线路图小节.
- P5 验证: 插件 243 JVM, API 24 / 35 基础组合各 20/20 无跳过, 历史上限与清空 1/1, Shizuku 真实列表卸载夹具 1/1, 最终首页七种状态显示各 3/3. 六页真实窗口在 API 35 的浅色/阿拉伯语/深色/字号 2/360 dp 窄内容/IME 2/2, API 24 浅色/IME 1/1. API 24 大字号曾触发模拟器 libhwui 原生崩溃, 未记成功; 维护者确认原偏好后已恢复默认跟随宿主, 后续测试改为先保存持久恢复计划. 未更改 AVD 图形配置或关闭用户启动的模拟器.
- P3.3: 维护者允许 Play Protect 扫描后, API 31 实际 Settings 授权并在原 platformSession 安装成功, created=1 / confirmation=1. 由独立驱动在 runner 退出后恢复 AppOps, 原权限/夹具/历史清理核验通过. 首次扫描和后次缓存结论的边界如实保留, 见 docs/dev/p3-unknown-source-evidence.md.
- 用户反馈: QV710AF65F 原为 build 5 实验包, 覆盖 P4 build 30 Release 后已确认 MT APK 出现安装信息/确认页. 最终已覆盖 build 33 Release 并在设备上查询确认 MT APKS MIME 的 VIEW/SEND 均匹配插件, 实际 MT APKS 确认页反馈待补; 既有 APK 反馈不等于安装完成, APKS 不替代原 XAPK 矩阵. 不自动安装用户目录中的 AutoJsPro / 微信等文件.
- 宿主: 493b229f4c 将 APK Inspector 收入更多菜单并置于 More information 上方, 异步菜单快照避免索引错位, InfoDialogInstrumentationTest 6/6. bf102da416 增加可选 sourceOrigin 以区分 host/script 历史, V1 AIDL 不变; 自带 release AAR 与哈希锁已同步. 宿主最终 build 5302, 3273 JVM 中 6 个既有跳过, 无失败; 最终 appDebug/androidTest 构建通过.
- 提交与边界: 插件独立界面依赖图为 33f9341, P3 授权验收为 67b296f, 显示恢复与证据另作提交; 最终 VERSION_BUILD=33 与提交数对齐. 本轮源码功能共用 Manifest/资源/导航, 为保持提交可构建一起提交, 原路线图仍逐项记录验收. 两仓库仅本地提交, 不推送 GitHub. 完整证据见 docs/dev/p5-standalone-evidence.md.
- 下一步: 保留 P5.2 跨 API Shizuku/Root 默认锁定矩阵, P3.2 系统文件管理器/浏览器 APK/XAPK, P1.4 宿主三入口组合, API 24 大字号复验与系统多窗口未测边界, 按原线路图继续 P6. 暂无新的产品决策或设备采购要求.

### 2026-10-01 (MT APKS 必选分包勾选显示)

- 维护者确认 QV710AF65F 的 build 33 经 MT 系统建议入口已显示 APKS 安装信息/确认页. 该反馈记录为第三方文件管理器入口成功, 不替代原 P3.2 的系统文件管理器/浏览器 APK/XAPK 安装矩阵, 原 checkbox 不变.
- 修复 base.apk 禁用状态下看起来未选中的问题. 实际设备节点为 checked=true / enabled=false, 原因是方框和勾选标记都使用同一 disabledText 颜色. 禁用勾选标记改为对填充可读的中性色, 保留分包必选约束和实际安装选择.
- 新设备断言能在旧 APK 上准确复现失败; 修复后 API 24 / 35 各 7/7 通过, 包括浅色/深色标记对比度和真实确认页重建前后 base.apk 保持已选中且不可取消. 插件 JVM 243 项通过; 十语言 changelog 和生成物同步, 详见 docs/dev/p5-standalone-evidence.md 的 build 34 补充.
- 本次为 P3 安装界面的单一缺陷修复, 使用 VERSION_BUILD=34 的独立本地提交, 不推送远端. 未自动安装用户 APKS 中的应用.


### 2026-10-01 (Files by Google 容器入口兼容与 P3.2 验收)

- 维护者确认 MT APKS 微信安装成功, Installation successful, com.tencent.mm / 8.0.72; 浏览器 APK/XAPK/APKM 与 Files by Google APK 均可安装使用, 并明确 apkx 是 xapk 笔误. 实机只读查询补充 Facebook 582.0.0.0.30 和 Instagram 449.0.0.52.84 的已安装版本.
- 复现 Files by Google 的 XAPK/APKM 候选缺失: application/octet-stream + content://com.google.android.apps.nbu.files.provider/2/<数字ID>, URI 没有扩展名. 通用 ZIP/二进制 filter 移除 pathPattern 依赖, 内容解析与只读来源边界保持; 未增加通配 MIME 或网络入口.
- build 35 已覆盖 QV710AF65F, 从真实 Files Downloads 页面分别打开 XAPK/APKM, 两者均可选择插件并进入正确包名/版本的确认页. 系统第二次把最近使用的插件提升到标题区域, 仍使用 Just once, 未设默认项. 代理在确认页取消并结束, 未重复安装用户应用; 三个用户应用版本保持, 活动 session/安装前台服务为零. build 34 的 base.apk 勾选修复也已交付并经实际截图复核.
- 原 P3.2 测试条目据上述反馈与真实 Files 复核勾选, 不增加/拆分/丢弃路线图小节. 范围明确为维护者指定的 Files by Google, 不声称所有 OEM/AOSP 文件管理器完成验收. 新 opaque URI 测试在旧 APK 准确失败, 修复后 API 24 / 35 各 4/4, 无跳过; JVM 243 项, Debug/androidTest/签名 Release 与两种 lint 通过, 详见 docs/dev/p3-external-entry-evidence.md.
- 插件 VERSION_BUILD=35, 对应一个本地修复提交, 工作区改动按本次外部入口意图收口; 宿主与其它插件未修改, 未推送远端. 下一步仍按 P5.2 默认安装器矩阵和 P6 原有条目推进.


### 2026-10-01 (P5.2 默认安装器与 P6 安全/健壮性)

- 补齐原 P5.2 默认页面验收: Shizuku API24/31/35和维护者新提供的KernelSU Root API33均完成页面锁/解锁, 系统DocumentsUI实际点击自建APK直达插件确认, 取消后原默认项与偏好恢复. 首次缺失full XML字段, API24旧Files目录URI与输入事件差异, Root首帧可见性等中间失败分别保留, 不将UI-only通过与Files失败拼接为成功.
- 经维护者授权尝试清除Root设备的InstallerX APK打开记录后, 系统仍保留两条last-chosen. 严格核对固定审计与当前状态后, 使用限定该审计的显式保留例外完成Root测试, 原有通配last-chosen也逐项保留; 没有清MT/应用数据/域链接或恢复旧always=true. G8441原配置未动. 详见docs/dev/p5-default-installer-evidence.md.
- P6.2: 只读请求之外校验provider实际FD模式, 拒绝可写句柄并释放, 正式服务八操作拒绝非宿主, 备份与实际合并导出组件审查. 安全四项在API24/35/33均通过. pm回退未启用, 条件项标N/A而非虚构命令测试. 本地提交7c4b814/build36.
- P6.1第二生产项: 同包安装跨用户/授权串行且等待可取消/超时, 独立入口回收24小时非活动暂存并防symlink跟随, 本地和私有Binder空间错误保真, EPIPE诊断保留明确Binder死亡错误. Root33和Shizuku35实际v1/v2并发及等待取消各2/2, 最终空间注入API24/35各6/6. 历史2GiB真实流式证据保留, 不冒充本轮堆峰值测试. 本地提交a6f61b7/build37.
- 收尾: 全量261 JVM, Debug/androidTest/签名Release和两种lint通过 (0错误, 22/23警告), 十语言生成与图标检查通过. 自建包/来源/五条外部取消历史均按归属清理, 四份默认项journal为restored; 临时启动的两台AVD Shizuku恢复停止, 模拟器保留运行. QV710AF65F与QV770340J7已覆盖build38 Release, 无spike入口, 原默认项保持.
- 新增勾选原条目5项 (P5.2测试1, P6.1生产1, P6.2三项含1项N/A), 未增删或拆分路线图. 插件最终VERSION_BUILD=38与可达提交数对齐, 仅本地提交, 宿主/其它仓库本轮未改且未推送. 下一步继续P6进程死亡与完整设备/性能/Release往返, 以及P1宿主三入口遗留验收.

### 2026-10-01 (P6.1 实际进程死亡与恢复)

- 完成原 P6.1 剩余两项. 只读握手最多重绑一次且共用超时预算; 安装/卸载等待期间发现特权 Binder 死亡及时返回 AUTHORIZER_UNAVAILABLE, 已发起操作不自动重放. 实测发现 SIGKILL 的平台 session 残留, 新增完整系统身份校验后精确回收已知 id; AOSP 需要 API 33+, 旧系统/信息丢失窗口不猜测归属.
- 真实设备: API 35 Shizuku peer 四项通过, server-stop opt-in 一项有意跳过; API 33 KernelSU Root peer 四项无跳过通过. 两侧各拒绝十项篡改的恢复字段. API 24 临时 Shizuku server 停止首次暴露 bootstrap 正常退出未清理, 加入 VM shutdown hook 后 1/1 通过, server 恢复停止. 原真机 Shizuku server 未停止.
- 官方宿主 UID 10890 的独立调用进程死亡, 实际 262,144 字节暂存立即删除; API 35 Shizuku/API 33 Root 插件主进程死亡后本次平台 session 消失, 历史恢复为中断, 空 FGS 重建后退出且不重放. 原 6/20 条 OEM/系统 session 保留, 所有自有来源/历史/case 清理, 宿主单个临时启用 key 精确恢复. 24 小时过期暂存使用仅归属本次的时间加速验收, 不声称重启即删除全部暂存.
- 宿主 9545a7f4aa/build 5303 仅增加 Debug 正式 UID 探针和持久恢复工具, 不变更公开 JS/AIDL, 不需要文档/d.ts/Ace/离线文档契约同步. 插件本逻辑提交为 build 39, 十语言使用者日志同步. 268 JVM 无失败或跳过, API 24/35 生命周期/安全各 11/11, Debug/androidTest/Release 编译及两种 lint 通过; 详细失败与复测证据见 docs/dev/p6-process-death-evidence.md.
- 未增删或拆分路线图. 当前继续 P6.4 签名 Release 正式往返/100 MiB 性能与维护者手动启动的 API 37/16 KB 模拟器验收, 不进入 P7 发布, 所有仓库仅本地提交.

### 2026-10-01 (P6.4 Release, 性能与 API 37 / 16 KiB)

- 完成原 P6.4 一项, API 37 的限定安装/运行行也有实测证据, 原 P6.3 整体继续保留未完成. 没有新增, 分拆或丢弃路线图条目. P6.1 行为提交为 65ee559/build 39, 本次测试和性能工具另作 build 40 逻辑提交.
- Release 专用源集和纯 Java/Android runner 在 API 24/31/33/35/37 各 2/2 通过, 校验同一非 Debug APK 和跨 UID 的元数据/Parcelable/五操作拒绝. 初始非法进程名, 被 R8 裁掉的 AndroidX 测试依赖, API 31 AVD 缺宿主权限定义均定位并复测; 不通过放宽权限或生产 test keep 来通过检查. 正向安装来自宿主 9545a7f4aa/build 5303 的真实 UID.
- 100 MiB STORED 无代码单 APK 在 Shizuku API 35, KernelSU Root API 33 与 none API 24 的最终安装时间为 1.176/0.924/9.694 秒, 每次实际写入 104,866,213 字节且系统确认安装版本. none 包含插件和系统两次确认等待. 设备不同, 不把差异归因于授权方式. 首次特权 getUsers 含服务冷启动为 320/363 ms, Root 授权独立 23 ms; 三台两次实际空闲 PSS 已记录.
- API 24 首轮摘要命令缺失与第二轮 PowerShell 返回值形状问题未计为最终 driver 验收, 改成 PC 侧 64 KiB 有界二进制摘要流后完整复跑. 五次实际成功安装生成的五条测试历史按新增 ID/包/版本/来源/授权/结果耗时逐条归属移除, API 24/35/33 原 2/2/0 条历史逐对象保留. 全部本轮夹具/来源和单 key 启用偏好恢复完成, 原设备默认项未改, 原真机 Shizuku server 保持, 临时 AVD server 恢复停止.
- 签名本地发行包为 autojs6-plugin-three-setup-installer-v1.0.0-96bfec75.apk, 1,890,319 字节, SHA-256 50b06cec175bd0a715179a4e09707ebf205154552d3370af48a8f37375ca976d, v2 签名验证通过, 无原生库/Debug 探针. QV710AF65F/QV770340J7 和验收设备已覆盖最终 build 40 Release; API 37 AVD 保持运行. 工作区按逻辑本地提交, 不推送或发布 GitHub.
- 详情见 docs/dev/p6-release-performance-evidence.md 和 docs/dev/p6-matrix-evidence.md. 下一轮从原 P6.3 完整 OEM 矩阵与 P1.4 宿主三入口组合继续, 同时保留 P2 尚未完成的原边界. 当前不需要新的产品决策或设备采购; G8441/Redmi/XQ-AT72 的后续逐项测试若涉及现有用户偏好, 继续按既有归属与恢复约定处理.

### 2026-10-01 (P2.3 固定来源保留策略)

- 维护者确认 keepSourceOnFailure 为固定策略: 失败, 取消或超时的未成功项始终保留来源, 不增加可关闭开关或公开选项. 在原 D25 和原 P2.3 条目中澄清, 未新增, 分拆或丢弃条目. 十语言来源 FAQ 同步, 公共 AIDL/脚本签名和生产删除路径不变.
- 新组合设备用例在 API 24/35 各 1/1, 无跳过; 显式 deleteSource=true 的坏包, 用户取消, 查询/打开两种超时均不发起删除, 原 SHA-256/长度保持. 独立拒删控制计数为 1, 实际四项计数为 0, 每项历史精确恢复且不创建平台 session. 结合 P3 成功删除/拒删和既有生命周期证据, 原 P2.3 来源策略条目完成; 三授权完整跨 API 矩阵仍单独保留.
- 本逻辑提交使用 build 41. 当前继续 P6.3 OEM 验收与 P1/P2 宿主三入口联调, 所有仓库只作本地提交.

### 2026-10-01 (P1.4/P2.6 宿主三入口收口)

- 原 P1.4 宿主构建/三入口和 P2.6 完整联调两项完成, 不增删或分拆路线图. 正式宿主 build 5304 在 Redmi API 33/UID 10778 上完成有/无插件各三项, 五次真实安装均核验固定 APK 摘要和版本, 无插件 XAPK 只显示引导. URL 实际经过下载, 所有入口由生产 UI 发起.
- 启用 APK Inspector 时信息图标隐藏且无更多菜单替代入口, 已在宿主 522335e864 修复. 同一提交包含捕获文件身份的菜单回调, 十语言 fix/生成文档, 六入口测试和证据. App/Inrt/全量 JVM/androidTest/lint 已通过, 最终 lint 0 Error/Fatal, 2399 Warning, 3 Hint.
- 最终导航/启用/安装许可恢复, 自有来源和 12 case 归档清理, 4 条精确归属历史移除, HTTP 子进程/reverse 停止; 用户应用, Inspector 设置和默认记录保留. 宿主另一个终端会话的全部 Git index 条目在本轮限定路径提交前后相同, 没有纳入安装器提交.
- 插件此文档提交为 build 42, 宿主证据见 AutoJs6/docs/dev/installer-entry-evidence.md. API 35 绑定/通知等原证据按原范围使用, P2.3 跨授权/API 的完整矩阵和 P6.3 OEM 完整矩阵继续保留未完成. 全部只作本地提交.

### 2026-10-01 (P2/P6 原设备安装矩阵推进)

- build 43 汇总原五台设备 none 新装/更新, 可用特权路径实际静默安装/更新/卸载, 外部显式 Activity 各 1/1, API 24/35 默认页锁定/解锁. 三种授权方式的统一 Core 在本轮四组分别 5/5, 6/6, 5/5, 6/6; 额外 Sony API 33 Root 不替代原 Redmi, 两台原手机默认记录受保护而跳过.
- HyperOS 默认安装者为 shell, 显式 shell 和插件自身两种请求均静默成功且实际归属相同. Sony API 33 user ROM 的 Root 接受非 debuggable 降级, 按设备事实记录, 不改写其他 ROM 的原拒绝结果. P2.3/P6.3 完整交叉矩阵仍保留未完成, 没有扩大低 targetSdk 或显式 Activity 的证据口径.
- 固定夹具 Play Protect 扫描驱动与重复卸载收尾修正, 初轮失败保留且最终独立复测通过. 安装许可工具在写入前持久 journal, 核对 UID/原模式/外部变化, 同目标串行; 六份全部恢复. 最终 8 台只读清理复核通过, 用户系统 session/server 和默认记录保持, 详情见 docs/dev/p6-matrix-evidence.md.

### 2026-10-01 (P6.3 OEM 文案与 build 44 Release 收尾)

- 原 P6.3 文案/FAQ 条目完成, 十语言 README 和插件说明同步. HyperOS 使用实际 shell/显式安装者结果, ColorOS 仅给出停止状态下的条件处理并明示未实机验证, 默认安装器保留旧 API/ROM 限制. 本轮完成来源策略/P1.4/P2.6/兼容文案等原条目, P2.3 和 P6.3 的完整矩阵边界保持, 不新增, 分拆或丢弃路线图.
- 逻辑提交为 f1e9916/build 41 (固定来源保留验证), 080b931/build 42 (宿主六入口验收), 7ead859/build 43 (原设备矩阵与安全测试驱动), 本次 build 44 (OEM 说明与最终 Release 证据). 宿主 522335e864/build 5304 独立提交, 其他会话终端改动和 index 均保留, 所有仓库仅本地提交.
- 最终 268 项 JVM 全通过, Debug/Release lint 为 0 错误和 22/29 警告. Debug/androidTest/R8 Release/Release androidTest 均构建通过, 文档 10 语言/36 产物和图标 15 项生成一致. API 24/35/37 同一 Release 各 2/2 跨进程契约通过, API 37 实际 16,384 字节页. 未重复本轮无生产引擎变更的 100 MiB 性能采样, 原 build 40 性能证据保持其版本边界.
- 最终包 autojs6-plugin-three-setup-installer-v1.0.0-3e230912.apk, 1,904,499 字节, SHA-256 3ab3c7691da231a723b59988d6861f0091184d88f742942bf2b7b47225b63e9a, v2 签名通过, 无原生库/Debug 入口. 八台设备均保留数据覆盖 build 44 Release, 最终清理/许可/原 server 和系统 session 复核通过; QV710AF65F/QV770340J7 不留 Debug 主包, 用户 AVD 继续运行.
- 证据见 docs/dev/p6-matrix-evidence.md, docs/dev/p2-source-preservation-evidence.md 及宿主 docs/dev/installer-entry-evidence.md. 下一步继续原 P2.3 三授权跨 API 缺口和 P6.3 默认项/OEM 实际外部入口. G8441/XQ-AT72 的既有 APK 打开记录仍受保护, 此前仅针对 QV770340J7 的清除许可不扩展到它们; 后续需要清除时另行列明影响并取得维护者授权. 当前无需新增测试设备或产品决策, 不进入 P7 发布.

### 2026-10-01 (P2.3 三授权矩阵继续验收)

- 原P2.3补充API 35 Shizuku完整选项8/8和API 33 Sony none 5/5, 无失败或跳过; 包含实际新装/更新/XAPK分包与正确拒绝语义. 不把none的拒绝计作具备特权, 不把API 35对低targetSdk的原本接受描述成绕过拦截.
- API 24实际插件UID无法执行shell专用su, 不修改AVD或安装Root管理器. Sony API 33的Shizuku未向插件授权, 尝试超时并主动中断, 保留日志和未完成状态, 不扩大已有Root授权. 三设备所有用户包集合/默认记录/历史/偏好/安装许可/原系统session/server基线核验一致, 恢复原build 44 Release.
- 证据见 docs/dev/p2-matrix-completion-evidence.md. 本次仅本地提交build 45, 原P2.3完整矩阵仍保留未完成; 不新增, 分拆或丢弃路线图. 本会话继续P6默认项与实际外部入口验收, 并整理原P7.2文案, 不执行远端发布.

### 2026-10-01 (P6.3 保留历史的默认页与实际 Files)

- 测试增加显式保留第三方no-scheme/always=false记录的模式, 普通保护性跳过保持. 必须无本插件既有记录且四种解析均为系统Resolver, public记录逐项有唯一shell证明; 真实默认/未知always/同scheme/重复或缺失证明均拒绝, 锁定前持久计划及确认前/恢复后全量比对保持. 新4项守卫在API 24/28/31各4/4, 另经子代理独立审阅.
- G8441 Root最终完整1/1: 真页面锁定, 系统Files从自有目录打开固定APK直达插件确认, 取消后解锁, 原系统安装器最近使用/所有默认项/偏好恢复. 精确取消token的历史清理1/1持久成功, 自有来源摘要核验后移除. 两次Files驱动失败和页面单独通过分别保留, 不混记为完整通过.
- 原配置下G8441实际chooser含插件; Redmi实际Files直接进入系统安装器风险提示, 未确认风险或安装, 返回后原默认XML/许可/session不变. XQ-AT72仍保护本插件原记录, 未执行默认页测试. 该机只读解析查询前后stopapp旧首选变为最近使用, 无清除命令, 保留前后证据与归因边界.
- 本次build 46按P6.3测试/证据单独本地提交, 原完整矩阵仍未勾选. 详情见 docs/dev/p6-default-history-evidence.md 与 docs/dev/p6-oem-external-completion-evidence.md. 不推送, 不改动P8/P9或路线图结构.

### 2026-10-01 (P7.2 十语言说明与发行历史定稿)

- 原P7.2两项完成, 不新增线路图条目. README补齐install/installAsync/session/uninstall/setDefault和分包/事件示例, 顶层仅只读状态查询; 明确同步查询/会话创建/wait不能在UI线程, 会话对象绑定创建线程. 依据实际宿主路由区分脚本auto默认特权静默与宿主界面dialog确认, 避免误导宿主按钮默认静默.
- 十语言授权前提/失败来源保留/逐项批量删除/设备降级差异/默认安装器限制及未公开发布状态同步. v1.0.0发行历史收敛为用户可读行为和六项依赖, 内部迁移过程不再作为功能说明. 版本/最低宿主在历史中固定, 不随未来模板值漂移.
- 文档10语言36产物与图标15项校验通过, 源码事实经独立子代理复核, 先前268项JVM/Debug装配/lint通过. 本次build 47仅本地文档提交. 下一步执行原P7.3的本地构建检查, 保留P2/P6缺口和远端发布禁令, 不提前进入P8.

### 2026-10-01 (P7.3 本地构建检查与 build 48 交付)

- 原P7.3本地构建条目完成. Temurin平台参数输出单组决策, Debug/androidTest/签名R8 Release/独立Release测试APK及原生库检查通过; 268 JVM零失败/跳过, Debug lint 0错误/22警告, 普通Release 0错误/23警告, Release测试源集0错误/29警告. 文档/图标生成器及最终文档标点检查通过.
- 同一build 48 Release在API 24/35/37各2/2真实跨UID/PID契约往返, API 37实际16,384字节页. 八台设备全部保留数据覆盖该Release, 摘要/版本/UID/无Debug核验通过. 原许可/全默认记录/系统session/server与交付基线相同, 无固定夹具/安装FGS; API 24交付前外部新增的终端测试包单独记录并保留, 不回滚其他工作.
- 产物autojs6-plugin-three-setup-installer-v1.0.0-8601e5e9.apk, 1,902,783字节, SHA-256=6cc923343072e52ad9cf27fcc939ce35bf8488394ad6b2f4b7f4e4cc8b9f3efe, v2签名通过. releases只保留本包, 旧包核验摘要后移入忽略目录. 详见 docs/dev/p7-local-gate-evidence.md.
- 本轮四个逻辑提交对应build 45-48, 仅本插件仓库变更, 未推送或发布. VERSION_BUILD=48与可达提交数一致, 工作树干净. P2.3/P6.3完整矩阵及P7.3远端各项保留未完成, 不改动原小节结构, 不进入P8/P9.
- 后续所需条件: API 24应用可用Root; XQ-DQ72向插件授予Shizuku以补同机三授权; 原生拦截targetSdk 22的API 34+特权环境; XQ-AT72默认页可验收基线. 该机插件既有最近使用仍保护, 不推定有清除许可. Redmi实际Files路由限制如实保留. 下一会话从这些原矩阵缺口继续, 远端发布仍待维护者解除仅本地限制.

### 2026-10-02 (P2.3 三授权与实际低 targetSdk 拦截收口)

- 维护者完成API 24应用可用Root和Sony XQ-DQ72插件Shizuku授权, 并提供在线Samsung SM-A566B, 明确允许调整XQ-AT72的最近使用记录. 本会话按这些新条件验收, 不再沿用前次阻塞状态; 仅本地提交限制保持.
- API 24 libsu Root与Sony API 33 Shizuku Root分别授权/绑定1/1和完整Core8/8, 配合该两设备已有四组证据完成原六格. 两组均实际接受带双标志的非debuggable降级; Root Shizuku默认安装者是插件包名, 不套用ADB shell身份. 自有夹具/来源/权限/默认/历史/系统session/server恢复核验通过.
- Samsung/API 36/16 KiB在未开bypass时由none与Shizuku真实返回INSTALL_FAILED_DEPRECATED_SDK_VERSION, 同一Shizuku身份开启标志后实际安装v1成功, 两个专项各1/1. 正常安装并授权Shizuku, 未改系统安全开关; 临时安装许可恢复, 本轮server按身份停止, 新管理器/插件授权保留. 细节见两份新P2证据.
- 原P2.3矩阵现已勾选, 不增加/拆分/丢弃条目. 本次build 49按P2.3证据单独本地提交. P6.3的授权后默认页/Files收口另作逻辑提交, 不进行远端发布.

### 2026-10-02 (P6.3 授权后默认页与原矩阵收口)

- 按维护者第4项许可, 测试新增仅QV710AF65F/API31/user0生效的approved模式, 精确绑定原四条VIEW/no-scheme/always=false记录. 原普通/第三方模式保护保持, 不进入旧InstallerX维护分支. 锁定前持久保存, 清理前原记录完整比对, 恢复后全量一致校验; 新增可清理项严格精确APK MIME且四种组合唯一, 防止通配MIME误判.
- XQ-AT72真默认页与实际系统Files直达确认后取消, 再解锁完整1/1, 21.674秒. 原四条插件最近使用/stopapp旧记录/全部其他默认和偏好保持; 精确token历史清理1/1后原8条历史完全一致, 自有来源已删除, 无安装. 未永久清除原记录.
- 新6项守卫在API 24/31各6/6, 普通Root默认页在API24另1/1并恢复基线, 无失败/跳过. 268项JVM/Debug/androidTest及lint通过, 0错误/22警告. 审查发现的新增项MIME通配判断已收紧并有回归, 改动均为测试辅助.
- 原P6.3设备表按既有证据与本次缺口验收收口, Redmi实际Files系统路由限制和无特权N/A保留, 不把限制写成成功. 原API37仍只按插件安装/运行范围, 其他未在线设备不冒充新包复验. 原条目已勾选, 详情见 docs/dev/p6-approved-default-completion-evidence.md.
- 本次build50按P6.3测试/证据单独本地提交. 下一步刷新最终签名Release与本地gate, 远端发布仍受维护者仅本地提交指示限制.

### 2026-10-02 (build 51 本地交付与后续发布边界)

- 最终Temurin平台构建/268 JVM/Debug与签名R8 Release/独立Release测试APK/两种lint/原生库检查通过. 十语言发行日期同步当日并重新生成, 文档与图标校验通过. 初次ROADMAP顿号导致标点守卫失败, 修正后完整重跑零失败/跳过; 保留失败日志.
- 当前四台在线设备保留数据覆盖Release51, 实际版本/摘要/UID/无Debug核验通过, 交付前后默认/许可/用户包集合/原系统session/server保持. API24/31/33真实跨UID/PID契约各2/2; Samsung无宿主不计该项, 但实际Release首页/16KiB运行已核验, 临时alias判定脚本问题修正并保留记录. 其他离线设备保持既有版本/证据, 不重启或关闭用户AVD.
- APK autojs6-plugin-three-setup-installer-v1.0.0-cf864c74.apk, 1,902,783字节, SHA-256=547334493ecd76f9815167c2159e5730de5ac14cf310674d5cbd7dfa30ae29d0, v2签名通过. releases仅此包, 旧包摘要核验后归档于忽略目录. 详见 docs/dev/p7-matrix-complete-release-evidence.md.
- 本轮build49-51三笔逻辑提交, VERSION_BUILD=51与可达提交数一致, 工作树干净; 本插件以外仓库未修改. 用户提供的四项条件已验收完成, 不再重复请求. 下一步为原P7.3远端发布/索引/宿主推送, 现有仅本地提交指示未解除, 因而本轮不推送或发布, 不跳过P7进入P8/P9.

### 2026-10-02 (P8 先行授权与 APK Inspector 共享解析迁移)

- 维护者本轮明确允许 P7 延迟时继续 P8 及后续项. 原 P7 远端条目保持未完成, 所有仓库只本地提交, 不推送, 创建标签或发布. 原条目结构保持.
- 原 P8 的 APK Inspector 迁移项完成. 兄弟仓库 17fe20d / 1.2.2 / build 42 消费同一 SHA-256 为 1441bbcee8468362b0ee41f7b3d5ab47eb87b4df78a1388bb1134223055f46e7 的共享 release AAR, 删除重复解码器/预算/TOC及容器副本; 签名, 显式 sidecar 所有权, 图标, DEX, 原生库与 16 KiB 等专有分析保留. 宿主文档提交 8c3045d24e 单独同步.
- Inspector 231 JVM, Python 4项, 25文档校验, Sony API33 Debug 3/3及独立 Release 2/2通过; Debug/Release lint各0错误/37警告, 签名R8构建通过. 产物 autojs6-plugin-apk-inspector-v1.2.2-c81f2584.apk, 4,393,049字节, SHA-256 d0bd86f9c1bc29fdcab95504a2a0ca84f171818437eaca8f52d85dc4fa8d94b1. 设备原UID/用户数据/默认/20个OEM session/原Shizuku server保持; instrumentation显式NO_ISOLATED_STORAGE审计项与API33默认deny有效权限相同, 不假称全部app-op原始行相同.
- 插件本次仅将这项跨仓库证据作为 build52 文档提交. P8 其余代码与设备验收正在进行, 不将中间 Debug 包描述为最终1.1.0交付; 对应完成状态和最终包另行记录.

### 2026-10-02 (P8 五项实施与跨仓库同步)

- 原 P8 五项全部完成, 没有新增, 拆分或丢弃条目. 版本升至1.1.0, V1最低宿主5299/V1脚本5300保留, V2新脚本选项建议完整宿主5307+. Dhizuku API2.6.0/MIT, 精确owner/provider身份, 当前用户与真实安装归属, 旧排序/禁用迁移及已知session恢复均已实现. 独立API31 AVD由本轮创建, 没有更换用户设备owner.
- 持久默认实现Dhizuku/API26-33与Root/system/user0, Root Q9实际成立后纳入生产. 被动状态不从历史回执推断实时策略; API34+ Dhizuku策略缺少owner最终结果时写前拒绝. 已知重复filter可幂等/明确清除, 未知原始XML不认领, 不确定结果不重放或盲目补偿.
- 通知安装在Sony/API31上两种授权均实际成功. 跨UID测试定位并修复FGS停止后的URI owner残留, 同时保护合法并发start; 实际主进程31355退出后系统因服务重建31969, 没有重新读取来源或安装. 系统实际redelivered=false仍由未知token/空任务集合拒绝, 不只依赖标志. 原8历史/32条默认/偏好/server/来源权限均核验恢复. 新Samsung/API36重新建立基线, 三项兼容/接口及实际通知拒绝共4项通过.
- 主插件运行时按Dhizuku/V2, 持久默认, notification三笔逻辑提交, 分阶段保留能力边界和十语言changelog; 前两阶段独立源树的JVM/Kotlin/androidTest编译通过, 最终完整实现312 JVM通过. 完整README/插件说明/跨库证据另作文档提交, 保留1.0.0历史.
- 关联仓库包括Inspector17fe20d, 宿主494e3e9493/bd9817980f/8c3045d24e/bb49c86ab129, Docs2ada5142, d.ts d789d42, Ace9f66fd62, Offline4d8115c5. 具体版本/验证/离线200文件摘要见 docs/dev/p8-integration-evidence.md. 宿主其他会话的Rhino等变更及Ace原有releases/保留, 不混入本任务提交.
- 同一最终签名R8候选1.1.0/build57已完成Root实际main六操作和正式宿主UID的Dhizuku/persistent脚本往返. 最终构建, APK摘要, 多设备Release契约及交付核验单独记录. P7远端发布继续延迟, P9四个原条目尚未实施; 下一步从原P9高级选项开始, 继续仅本地提交.

### 2026-10-02 (P8 build57 本地发行包收尾)

- 最终包 autojs6-plugin-three-setup-installer-v1.1.0-c2238464.apk, 1,965,043字节, SHA-256 d1300f2b1ac97c240bc02e58752909d1fa5b78a18397d3894ac5ce1aef8df31e, v2签名/CRC/无native/非Debug核验通过. 312 JVM, Debug/Release各0 lint错误与40警告, 文档36/图标15产物一致. API26缺失size读回和Debug旧API任务标识的gate失败已修正, 记录全部失败与复验, 没有压制NewApi或提高minSdk.
- API24/31/36同包独立契约各2/2, 包括V2新增事务的非宿主拒绝; 最终R8的Root主入口六次往返与真实宿主UID的Dhizuku脚本实际安装/卸载/持久往返通过. 通知完整实装与生命周期按各自固定Debug版本记载, 最后Sony保数据交付同包; 不扩大成所有功能/ROM的最终R8全矩阵.
- 用户设备原默认/历史/授权状态核验保护, 新三星按新基线配置宿主/插件并移除临时测试包, 专用自建AVD已关闭, 用户AVD保持. 主插件build52-57六笔本地提交, 最终计数57且工作区干净. 关联仓库各自提交, 其他会话与Ace既有文件保留. 详情见 docs/dev/p8-release-evidence.md.
- P8原五项完成, 本轮不进入繁杂的P9四项实现. 下一步从原P9高级选项/契约V3继续; P7远端发布仍按维护者指示延迟. 当前没有新的设备, 关键决策或手动操作待维护者处理.
