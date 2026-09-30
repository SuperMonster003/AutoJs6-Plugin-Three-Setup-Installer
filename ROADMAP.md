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

以下决策 D1-D12 已由维护者于 2026-09-30 分三轮确认, 后续阶段不再重新讨论; D13-D31 为据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定.

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
| D18 | 交互模式 | `interaction: 'auto' | 'dialog' | 'silent'`; `auto` = 特权可用时 `silent`, 否则 `dialog`; `silent` 在无特权时报 `AUTHORIZER_REQUIRED`, 不降级为对话框; 通知栏模式列入 P8 |
| D19 | 脚本 API 形态 | `install` / `installAsync`, `session`, `uninstall` / `uninstallAsync`, `inspect` / `inspectAsync`, `authorizer` (状态 / 请求), `isDefault` / `setDefault` / `setDefaultAsync`, `users`, `isAvailable`; 错误类型 `InstallerError` (`code`, `status`, `systemMessage`); 草案见附录 A |
| D20 | 分包与批量 | 数组参数 = 多个独立安装包的批量安装, 逐个返回结果; `{ splits: [...] }` 对象 = 一个应用的分包集合, 作为一个会话安装 |
| D21 | 宿主退化路由 | 宿主新增 `core/plugin/installer/PackageInstallRouter` 单入口, 全部安装调用方改走它: 插件可用 -> Binder 会话; 不可用 -> `ACTION_VIEW` + FileProvider URI 交给系统安装器, 并在插件中心 / 文件管理器提示安装 3-Setup Installer |
| D22 | 安装完成观察 | 宿主向导与插件中心等待安装完成改为双通道: 插件会话回调 (插件路径) + `PACKAGE_ADDED` / `PACKAGE_REPLACED` 动态广播 (系统安装器路径), 不再依赖被删除的 `PackageInstallStatusCoordinator` |
| D23 | 锁定实现 | Shizuku / Root 路径调用 `IPackageManager.addPreferredActivity`, 对 `ACTION_VIEW` / `ACTION_INSTALL_PACKAGE` x `content` / `file` 共四个 APK MIME filter 逐一设置并解析确认; 六参数重载用 `removeExisting=true` 精确替换. API 24 / 28 的五参数重载不能精确替换 MIME 首选项, 仅清除本插件首选项; 若竞争者已有匹配默认项则返回 `DEFAULT_REQUIRES_CLEAR=-1`, 由 P5 引导先在系统设置清除. 不清除其它包的所有默认项. P0.2 实测 shell / root 调用 `addPersistentPreferredActivity` 均抛 `SecurityException`, 1.0.0 不承诺持久锁定, 仍列 P8 |
| D24 | 卸载实现 | 特权: `IPackageInstaller.uninstall(VersionedPackage, callerPackage, flags, IntentSender, userId)`, 支持 `keepData` (`DELETE_KEEP_DATA`) 与 `user` (含 `all` -> `DELETE_ALL_USERS`); `none`: `ACTION_UNINSTALL_PACKAGE` + `EXTRA_RETURN_RESULT` 由插件 Activity 接管. 宿主既有 `app.uninstall` (`ACTION_DELETE`) 保持不变 |
| D25 | 安装后删除 | `deleteSource` 由文件所有者执行: 脚本 API 的路径来源由宿主在收到 `COMPLETED` 后删除; 插件外部入口收到的 `content://` 用 `ContentResolver.delete` / `DocumentsContract.deleteDocument` 尽力删除, 失败只记录不报错 |
| D26 | 插件 UI 形态 | 安装对话框为插件自有 Activity (对话框主题, `excludeFromRecents`, 独立 task), 三段: 确认 (图标, 名称, 包名, 版本 旧 -> 新, 大小, targetSdk, 签名匹配状态, 分包选择, 选项开关), 进度, 结果; 后台阶段用前台服务通知承载进度 |
| D27 | 设置页 | 遵循独立设置页规范: 外观四项 (语言 / 夜间 / 主题色 / 启动器图标, 默认跟随 AutoJs6) 在前; 安装组: 授权方式顺序与启用, 默认交互, 默认选项 (降级 / 测试包 / 绕过低 targetSdk / 安装者包名 / 目标用户 / 安装后删除), 默认安装器状态卡; 通知; 关于 / 发行历史 / 更新检查 |
| D28 | 共享解析 AAR 边界 | `plugin-api/package-archive-parser` 只含格式识别, 容器元数据, `.apks` TOC 解码与分包选择, APK / AAB 清单显示解码, 签名方案探测, 检查上限 (宿主 `ui/main/scripts` 的 6 个文件及其 JVM 测试); APK Inspector 专有的签名验证, 原生库摘要, 16 KB 就绪度, DEX 摘要不进入 |
| D29 | 许可证边界 | 插件 MPL-2.0; InstallerX 系 GPL-3.0 只做架构 / 行为 / 选项目录参考, 不复制源码, 资源, 字符串; 隐藏 API 存根按 AOSP 接口签名自写并在 `THIRD_PARTY_NOTICES.md` 注明 Apache-2.0 来源; 第三方运行时依赖: Shizuku-API 13.1.5, libsu (建仓时 Maven Central 最新稳定版), HiddenApiBypass 6.1 |
| D30 | 错误码与阶段 | 错误码集合与安装阶段枚举见附录 B.4 / B.5, 宿主 `installer-api` 与脚本 `InstallerError.code` 使用同一字符串 |
| D31 | 契约上限 | 单次批量 <= 32 个安装包, 单个分包集合 <= 64 个文件, JSON 文档 <= 64 KiB, 安装者包名 <= 255 字节, 用户确认默认超时 5 分钟, 单会话默认超时 30 分钟, 并发会话 <= 4 |

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
ui/       InstallDialogActivity, ExternalInstallActivity, UninstallDialogActivity, UserActionActivity, InstallForegroundService
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
| P8 | 1.1.0: Dhizuku 授权, 持久化默认安装器, 通知栏安装模式, APK Inspector 迁移到共享 AAR | 插件 + 宿主 + 兄弟 | P7 |
| P9 | 1.2.0: 授予全部权限 / 更新所有权 / DexOpt / 签名门禁 / 黑名单 / 权限预览 / 按来源配置 | 插件 (+ 宿主小) | P8 |

建议会话切分: P0 一次 (骨架 + spike); P1 两到三次 (共享 AAR 为一次; 契约 + 客户端 + 路由为一次; 删除旧安装器 + 调用方改造 + 文档为一次); P2 两到三次 (来源与授权; 安装引擎; 卸载 / 批量 / 路由); P3 一到两次; P4 两次 (install / session / errors; uninstall / inspect / authorizer / setDefault / 示例); P5 一到两次; P6 一到两次; P7 一次; P8 两次; P9 两到三次.

当前进度 (2026-09-30): P0.1 / P0.2 已完成, 证据见 `docs/dev/p0-spike-evidence.md`. 下一阶段从 P1.1 共享解析 AAR 开始; 宿主契约, 安装入口, 脚本 API 与产品界面尚未交付.

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

- [ ] (宿主) 新建 Android library 模块 (namespace `org.autojs.plugin.packagearchive`, 无 AIDL, 只依赖 Kotlin stdlib 与 `java.util.zip`), 在 `settings.gradle.kts` 的 `pluginApi` 列表注册; 将 `ui/main/scripts/` 的 `AndroidPackageArchive.kt` (含 `AndroidPackageFormat`, `ArchiveInstallationState`, `PackageDeviceSpec`, `AndroidPackageArchiveInspector`, `ManifestSummaryParser`), `BundletoolTocDecoder.kt`, `ApkManifestDisplayDecoder.kt`, `AabManifestDisplayDecoder.kt`, `ApkSignatureDetector.kt`, `PackageInspectionLimits.kt` 以 `git mv` 迁入并改包名; 去除对宿主 `R` / `Context` 的依赖 (字符串由调用方提供, `PackageDeviceSpec.from(context)` 改为接收 ABI 列表 / 密度 / 语言 / SDK 的纯数据工厂 + 宿主侧 `from(context)` 扩展).
- [ ] (宿主) 对照 APK Inspector 的分叉副本 (`AndroidPackageArchive.kt` 703 行差异, `ApkSignatureDetector.kt` 90 行差异, `AndroidPackageArchiveValidator.kt`), 只回收与格式识别 / 分包选择 / 容器校验有关且宿主也需要的修正, 逐条记录取舍到 `docs/dev/package-archive-parser-v1.md`; 不把签名验证, 原生库摘要, 16 KB 就绪度, DEX 摘要迁入 (D28).
- [ ] (宿主) 宿主 `app` 改为 `implementation(project(":plugin-api:package-archive-parser"))`, `ApkInfoDialogManager` / `DisplayManifestActivity` / `BuildActivity` 与文件管理器改用新包名; 六个 JVM 测试随模块迁移并通过; `AndroidPackageArchiveDeviceTest` 保留在宿主或迁入模块 androidTest.
- [ ] (宿主) 发布 AAR: `:plugin-api:package-archive-parser:assembleRelease`, 记录 SHA-256; 插件 `libs/package-archive-parser.aar` + `locks/host-api-aars.lock` 条目.
- [ ] (测试) 模块 JVM 测试覆盖 `.apk` / `.apks` / `.xapk` / `.apkm` / `.apkz` / `.aab` / 含 APK 的 `.zip` 的格式识别与分包选择 (复用宿主既有夹具), 超限 (文件数, 大小, 时间) 的 `ArchiveProblemCode`.

### P1.2 契约模块 `plugin-api/installer-api`

- [ ] (宿主) 新建 Android library (namespace `org.autojs.plugin.installer.api`, `aidl = true`, `api(project(":plugin-api:common-plugin-api"))`), 注册到 `pluginApi` 列表; AIDL: `IInstallerPlugin` (`getInfo`, `getCapabilities`, `getAuthorizerState`, `requestAuthorizer`, `inspect`, `openSession`, `uninstall`, `getDefaultInstallerState`, `setDefaultInstaller`, `getUsers`), `IInstallerSession` (`getId`, `getState`, `cancel`, `close`), `IInstallerSessionCallback` (`onStage`, `onProgress`, `onCompleted`, `onFailed`), `IInstallerCallback` (一次性结果); 草案见附录 B.
- [ ] (宿主) 常量: `InstallerActions` (`SERVICE_ACTION`, `SERVICE_CATEGORY`, `PLUGIN_PERMISSION`, `OPEN_SETTINGS`), `InstallerIds` (`PLUGIN_ID`, `ENGINE`, `VARIANT_DEFAULT`, `DEFAULT_PACKAGE_NAME`, `REQUIRED_HOST_VERSION_CODE`), `InstallerContract` (契约版本 1, Bundle key, JSON key, 阶段 / 授权方式 / 交互模式 / 用户选择字符串, 上限 D31), `InstallerCapabilityKeys` (`CONTRACT_VERSION`, `AUTHORIZERS`, `FEATURES`, `MAX_BATCH`, `MAX_SPLITS`), `InstallerErrorCodes` (附录 B.4).
- [ ] (测试) `InstallerAidlOrderTest` (transaction 顺序冻结), `InstallerContractTest` (常量, 上限, 错误码唯一, 与 `PluginCapabilityKeys` 无冲突).
- [ ] (宿主) 发布 AAR 并写入插件 `libs/installer-api.aar` + 锁文件.

### P1.3 宿主客户端, 路由与退化

- [ ] (宿主) `core/plugin/installer/InstallerPluginHost.kt` (`AidlPluginHost<IInstallerPlugin>`: action / category / `IInstallerPlugin.Stub::asInterface`, 最低宿主版本, 信任校验), `ThreeSetupInstallerOfficialPlugin.kt` (官方包名 / 仓库 URL / 安装引导), `InstallerSessionClient.kt` (打开 PFD, 发起会话, 回调桥接, 取消, 超时), `InstallerJson.kt`, `InstallerErrorMapper.kt`.
- [ ] (宿主) `PackageInstallRouter.kt` (D21): `install(context, source: File | Uri, request: HostInstallRequest): HostInstallHandle`; 插件可用 -> `InstallerSessionClient` (`interaction = dialog`, 插件对话框); 插件缺失 / 禁用 / 不兼容 / 绑定失败 -> `ACTION_VIEW` + FileProvider `content://` + `FLAG_GRANT_READ_URI_PERMISSION` 交给系统安装器, 同时按状态给出 "安装 3-Setup Installer" / "启用" / "版本不兼容 (需要 x)" 引导 (`AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 9 节五态); 对 `.apks` / `.xapk` / `.apkm` / `.apkz` 无插件时不交给系统 (系统安装器不识别), 直接提示安装插件.
- [ ] (宿主) `PackageChangeObserver.kt` (D22): 动态注册 `PACKAGE_ADDED` / `PACKAGE_REPLACED` (data scheme `package`, API 33+ `RECEIVER_NOT_EXPORTED`), 提供 `awaitInstalled(packageName, timeout)`; `PluginInstallWizardInstallAwaiter` 改为 "插件会话回调 或 观察器命中" 任一完成, 取消 / 超时语义保持.
- [ ] (宿主) 调用方改造 (第 3.1 节清单 12 处): `UpdateChecker`, `BuildActivity` (安装与信息对话框的安装按钮), `ExplorerItem` / `ExplorerItemViewHolder` / `ExplorerFragment`, `PluginInstaller` / `PluginInstallActions` / `PluginInstallWizardInstaller` / `PluginFragment` / `PluginCenterActivity` / `PluginCenterFragment` / `PluginInfoDialogManager`, debug `ActivityLaunchCompatibilityProbe`; `App.kt:140` 的 Coordinator 初始化删除.
- [ ] (宿主) 插件中心注册: `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` 增加 `installer`, `InstalledPluginRepository` 发现分派, Manifest `<queries>` 增加 `org.autojs.plugin.INSTALLER` intent, `PluginInstallWizardCatalog` 增加 `entry(official("three.setup.installer"), "Three Setup Installer", TOOLS)`, `PluginSettingsFragment` 的 "打开插件设置" 走 `org.autojs.plugin.INSTALLER_SETTINGS`.
- [ ] (测试) JVM: 路由五态决策表, 系统安装器交接 intent 构造 (MIME / flags / 不支持格式), 观察器超时与取消, JSON 编解码, 错误映射; 宿主 `testAppDebugUnitTest` 通过.

### P1.4 删除宿主安装器

- [ ] (宿主) 删除 `PackageInstallerActivity.kt`, `PackageInstallerEntryActivity.kt`, `AndroidPackageSessionInstaller.kt`, `PackageInstallStatusActivity.kt` / `Coordinator.kt` / `Receiver.kt` / `Store.kt` 及其 Manifest 声明 (L377-445), `installerEnabled` 占位符 (`app/build.gradle.kts:981, 1006`), `PackageInstallStatusRecordTest`, `PackageInstallStatusStoreDeviceTest`; 删除遗留 `IntentUtils.installApk` 与 `com/stardust/util/IntentUtil.installApk*`.
- [ ] (宿主) `ApkInfoDialogManager` 保留只读信息, "安装" 按钮改为 `PackageInstallRouter.install`; 文件管理器安装按钮 (`explorer_file.xml` `@+id/install`) 保留, 动作改走路由; `ExplorerItemActionPolicy.installVisible` 语义不变.
- [ ] (宿主) 字符串清理: 删除仅被删除代码引用的 `error_package_installation_*`, `text_package_installation*`, 通知通道文案等 (10 语言目录逐一核对); 新增退化引导文案 (`text_installer_plugin_required`, `text_installer_plugin_unsupported_format` 等, 默认与 `values-en` 一致, 按 `name` 排序).
- [ ] (宿主) `REQUEST_INSTALL_PACKAGES` 保留 (系统安装器交接需要调用方声明), `REQUEST_DELETE_PACKAGES` 保留 (`app.uninstall`); ProGuard / R8 规则中与删除类相关的 keep 清理.
- [ ] (测试) 宿主 `:app:assembleAppDebug` / `:app:assembleInrtDebug` / `testAppDebugUnitTest` / `lintAppDebug` 通过; 在一台真机上验证: 无插件时文件管理器点击 `.apk` -> 信息对话框 -> 安装 -> 系统安装器; 点击 `.xapk` -> 提示安装插件; 插件中心从 URL 安装 -> 系统安装器; 有插件时以上三处均进入插件对话框.

### P1.5 协议文档, changelog 与版本回填

- [ ] (宿主) `docs/dev/installer-plugin-protocol-v1.md` (决策, Binder 面, Bundle key, JSON 文档, 操作, 错误码, 上限, 版本协商, 安全边界, 宿主客户端, 脚本 API 章节占位) 与 `docs/dev/package-archive-parser-v1.md`; 宿主 `.changelog` 10 语言: `feature` (脚本 `installer` 待 P4 再写), `improvement` (安装器能力迁出到 3-Setup Installer 插件, 无插件时交给系统安装器), `dependency` (模块化 `package-archive-parser`, `installer-api`).
- [ ] (宿主) 宿主提交后读取 `VERSION_BUILD`, 回填 `InstallerIds.REQUIRED_HOST_VERSION_CODE`, 插件 `requiresHostVersion` meta-data / `ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION` / `.readme/common.json` / 测试夹具; 只暂存本效果的路径 (宿主工作树可能含其它会话改动).

验收条件: 两个新模块 AAR 已发布到插件 `libs/` 并锁定; 宿主构建 / 单测 / lint 通过; 真机退化路径三处验证; `docs/dev` 两份文档与 10 语言 changelog 已写; `REQUIRED_HOST_VERSION_CODE` 已回填.

---

## P2: 插件核心

目标: 插件在无 UI 的前提下, 能通过 Binder 完成 "解析 -> 授权方式选择 -> 安装 / 卸载 -> 回调" 的全部路径, 三种授权方式与 D6 全部选项在真机上可用.

### P2.1 来源与格式

- [ ] (插件) `PackageSource` 三态: 宿主 PFD (`IInstallerPlugin.openSession` 的 `ParcelFileDescriptor[]` + 元数据), `content://` (外部入口, `ContentResolver.openFileDescriptor`), `file://` / 路径 (仅外部入口的旧文件管理器, 需可读); 统一为 `SeekableSource` (PFD 用 `FileInputStream(fd)` + `FileChannel`), 不复制到私有目录除非容器需要随机访问且来源不可 seek (管道), 复制到 `cacheDir/staging/<sessionId>/` 并在会话结束清理.
- [ ] (插件) `ArchiveOpener`: 用共享 AAR 识别格式; `.apk` 直接流式; `.apks` 经 `BundletoolTocDecoder.select(device)` 选分包; `.xapk` / `.apkm` / `.apkz` / `.zip` 用 `ZipFile` 列出 `*.apk` 条目, 按 `manifest.json` (xapk) / `info.json` (apkm) 或文件名 (`base.apk`, `split_config.*.apk`) 分类, 按 `PackageDeviceSpec` (ABI, 密度, 语言) 选择; `.aab` -> `UNSUPPORTED_FORMAT` (D9), `inspect` 仍返回模块与包信息.
- [ ] (插件) `inspect(source)` 返回 `PackageArchiveInfo` JSON (附录 A.6): format, packageName, versionName, versionCode, label, icon (Base64 PNG, <= 64 KiB), minSdk, targetSdk, splits (名称 / 大小 / 是否选中 / 选择原因), signatureSchemes, installedVersion (若已安装: versionName / versionCode / signerMatch / installer), size, problems.
- [ ] (测试) JVM: 各格式夹具 (复用共享 AAR 测试资源 + 本仓库最小 xapk / apkm 夹具), 分包选择, AAB 拒绝, 损坏 zip, 超限; 设备: PFD 来源与 content 来源各一次 inspect.

### P2.2 授权方式

- [ ] (插件) `Authorizer` 枚举 `NONE / SHIZUKU / ROOT`, `AuthorizerResolver.resolve(requested, settings)` 实现 D14 (显式指定不回退; `auto` 按设置顺序, 跳过被禁用项); 每种授权方式的 `state()` -> `{ available, running, granted, reason }` 与 `request()` (Shizuku: `Shizuku.requestPermission` + 结果监听; Root: libsu `Shell.getShell()` 触发授权; None: 恒 granted).
- [ ] (插件) `ShizukuAuthorizer`: `Shizuku.addBinderReceivedListener` / `addBinderDeadListener`, `isPreV11` 拒绝, UserService 绑定缓存与 `DeadObjectException` 重绑, 版本不匹配时 `AUTHORIZER_UNAVAILABLE` 附 `reason`.
- [ ] (插件) `RootAuthorizer`: libsu `Shell.Builder` (`FLAG_MOUNT_MASTER` 不需要, 超时 10 s), `RootService.bind` 缓存, 无 su 时 `AUTHORIZER_UNAVAILABLE`, 拒绝时 `AUTHORIZER_DENIED`.
- [ ] (测试) JVM: 解析顺序决策表 (设置顺序 x 各状态 x 显式指定); 设备: Shizuku ADB 模式 (AVD) 与 Root (已 root 真机或 AVD `-writable-system` + Magisk, 记录方式) 各一次 `state` / `request` 往返.

### P2.3 安装引擎

- [ ] (插件) `InstallEngine` 接口: `install(request, sources, listener)`; `NoneInstallEngine` (D17): `PackageInstaller.Session` 创建 / 写入 (每个分包一个 `openWrite`, 8 MiB 缓冲, 进度按字节) / `commit(IntentSender)` -> `STATUS_PENDING_USER_ACTION` 交 `UserActionActivity` (P3.3) / 结果广播 -> 回调; `PrivilegedInstallEngine`: 经 `IPrivilegedInstaller` 创建会话 (`installFlags` 映射: `allowDowngrade` -> `INSTALL_REQUEST_DOWNGRADE | INSTALL_ALLOW_DOWNGRADE`, `allowTestOnly` -> `INSTALL_ALLOW_TEST`, `bypassLowTargetSdk` -> `INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK` (API 34+, 低版本忽略并在结果 `notes` 说明), `user: 'all'` -> `INSTALL_ALL_USERS`, `installer` -> `installerPackageName`, `user: <id>` -> `userId`), 写入用返回的 PFD, `commit` 后经 `IntentSender` (插件 `PendingIntent` 广播) 取结果.
- [ ] (插件) 结果规范化: `PackageInstaller.STATUS_*` -> 错误码 (附录 B.4), `EXTRA_STATUS_MESSAGE` 原样进 `systemMessage`, `EXTRA_PACKAGE_NAME` 进结果; 安装成功后读取已安装 `PackageInfo` 填 `installedVersionCode` 等.
- [ ] (插件) `deleteSource` (D25) 与 `keepSourceOnFailure`; 会话超时 (D31) 与取消 (`abandon`).
- [ ] (测试) 设备矩阵: 三种授权方式 x (新装 / 更新 / 降级 / 测试包 / 分包集合 xapk) 于 AVD API 24 与一台 API 33+ 真机; 记录降级在 user 版本 ROM 的实际结果 (预期 `INSTALL_FAILED_VERSION_DOWNGRADE`); `bypassLowTargetSdk` 用 targetSdk 22 的夹具 APK 于 API 34+ 验证.

### P2.4 卸载引擎

- [ ] (插件) `UninstallEngine` (D24): 特权 `uninstall(packageName, flags(keepData -> DELETE_KEEP_DATA, user 'all' -> DELETE_ALL_USERS), userId, sender)`; `none` -> `UninstallDialogActivity` 拉起 `ACTION_UNINSTALL_PACKAGE` + `EXTRA_RETURN_RESULT`, 结果映射 `USER_CANCELLED` / `INSTALL_FAILED`; 系统应用 / 设备管理员 / 被保护包的失败原因透传.
- [ ] (测试) 设备: 三种授权方式各卸载一次 (含 `keepData` 后重装数据仍在的断言).

### P2.5 批量, 分包与用户

- [ ] (插件) 批量 (D20): 一个 Binder 调用携带 <= 32 个来源, 插件内串行执行, 每项独立回调与结果, 单项失败不中断其余 (`continueOnError` 默认 true); 分包集合 `{ splits }` 作为一个会话.
- [ ] (插件) `getUsers()` (特权: `IUserManager.getUsers` -> id / name / isPrimary / isRunning; `none`: 仅当前用户); `user` 参数校验 (不存在的 id -> `INVALID_ARGUMENT`).
- [ ] (测试) JVM: 批量上限与部分失败聚合; 设备: 3 个 APK 批量 (Shizuku), 双开 / 工作资料用户存在时 `user` 安装一次.

### P2.6 Binder 路由与上限

- [ ] (插件) `ThreeSetupInstallerPluginService` (`IInstallerPlugin.Stub`): `HostCallerGuard` (调用方为宿主签名 / 官方包名或 PLUGIN 权限持有者), 所有输入按 D31 校验 (数组长度, JSON 大小, 字符串长度, 枚举值, PFD 非空), 并发会话 <= 4, `SessionRegistry` 管理生命周期 (宿主进程死亡 -> `linkToDeath` 取消会话并清理暂存), 能力 Bundle (`CONTRACT_VERSION=1`, `AUTHORIZERS=[none,shizuku,root]`, `FEATURES=[batch,splits,delete-source,silent-uninstall,default-installer,users,inspect]`).
- [ ] (测试) instrumentation: 发现 / 绑定 / descriptor; 敌意输入 (超长数组, 非法 JSON, 未知枚举, 关闭的 PFD) 均返回 `INVALID_ARGUMENT` 而不崩溃; 宿主进程模拟死亡后会话被取消.

验收条件: P2 全部条目在 AVD API 24 与至少一台 API 33+ 真机上按授权方式矩阵通过; JVM 测试覆盖来源 / 格式 / 解析顺序 / 批量 / 上限; 证据写入 `docs/dev/p2-core-evidence.md`.

---

## P3: 插件界面

目标: 插件拥有完整的安装 / 卸载对话框, 外部 `ACTION_VIEW` 入口, 系统确认接管与前台服务通知; 宿主与脚本以 `interaction = dialog` 发起时用户看到的是插件界面.

### P3.1 安装对话框

- [ ] (插件) `InstallDialogActivity` (对话框主题 `Theme.ThreeSetupInstaller.Dialog`, `excludeFromRecents`, `launchMode=singleTask` 按会话 id 区分 task): 确认段 (D26: 图标, 名称, 包名, 版本 旧 -> 新 或 "新安装", 大小, minSdk / targetSdk, 签名匹配 (与已安装签名一致 / 不一致 / 未安装), 分包列表可勾选, 选项开关 (授权方式, 降级, 测试包, 绕过低 targetSdk, 安装后删除, 目标用户) 默认取设置页), 进度段 (阶段文案 + 百分比 + 取消), 结果段 (成功: 打开 / 完成; 失败: 错误码 + 系统消息 + 复制).
- [ ] (插件) 批量对话框: 列表逐项状态, 全部取消, 单项重试; AAB 项显示 "无法安装 AAB" 与信息入口 (D9).
- [ ] (插件) 遵循独立设置页规范的对话框几何 (24 dp 圆角, 手机左右 24 dp, 宽屏 560 dp, 内容滚动按钮固定), 中性色表面, 主题色只用于控件; 10 语言文案; RTL / 大字号 / 夜间 / 进程重建 (会话 id 持久到 `SavedStateHandle`, 重建后从 `SessionRegistry` 恢复).
- [ ] (测试) instrumentation: 确认 -> 安装 -> 结果的 happy path (none 路径在 AVD), 取消, 旋转重建, 失败结果展示.

### P3.2 外部入口

- [ ] (插件) `ExternalInstallActivity` (`exported=true`, 无权限保护, `Theme.NoDisplay` 后转 `InstallDialogActivity`): 两组 intent-filter 从宿主原 `PackageInstallerEntryActivity` 迁来 (`ACTION_VIEW` + `ACTION_INSTALL_PACKAGE`, `content` / `file` scheme, 7 种 MIME; `content` + `application/zip` / `application/octet-stream` + 大小写 `pathPattern` 覆盖 6 种扩展名); 多 URI (`ACTION_SEND_MULTIPLE`) 作为批量.
- [ ] (插件) 外部来源的安全处理: 只读打开, 不信任文件名, 大小上限与共享 AAR 的检查上限; `file://` 在 API 24+ 仅接受可读路径, 失败给出 `SOURCE_UNREADABLE` 文案.
- [ ] (测试) 设备: 从系统文件管理器与浏览器下载列表各打开一次 `.apk` / `.xapk`; 宿主已删除入口后, 系统 "打开方式" 列表只出现插件.

### P3.3 用户确认与卸载对话框

- [ ] (插件) `UserActionActivity`: 接管 `STATUS_PENDING_USER_ACTION` 的 intent sender (`startActivityForResult`), 未知来源权限缺失时先引导 `ACTION_MANAGE_UNKNOWN_APP_SOURCES` 再重试, 超时 (D31) 后取消会话.
- [ ] (插件) `UninstallDialogActivity`: `none` 路径承载 `ACTION_UNINSTALL_PACKAGE`; 特权路径的确认对话框 (脚本以 `interaction = dialog` 卸载时) 显示应用信息与 `keepData` 开关.
- [ ] (测试) 设备: none 路径新装 (含未知来源引导), 用户取消, 超时; 特权卸载确认.

### P3.4 前台服务与通知

- [ ] (插件) `InstallForegroundService`: 会话进入写入阶段时启动, 类型在 `dataSync` 与 `specialUse` 之间按 API 34+ 实测选定 (附录 D Q6), 通知显示阶段与进度, 完成后结束; 通知通道 `installation`; API 33+ `POST_NOTIFICATIONS` 缺失时静默降级 (不阻塞安装).
- [ ] (测试) 设备: 静默安装 2 GiB 级 xapk 期间切到后台, 进程未被杀且进度通知更新; API 34+ 无异常.

验收条件: P3 全部对话框在 AVD API 24 / 真机 API 33+ 走通; 外部入口在系统列表出现且宿主不再出现; 前台服务在 API 34+ 合规; 证据写入 `docs/dev/p3-ui-evidence.md`.

---

## P4: 脚本 API `installer`

目标: 宿主脚本可通过 `installer` (`$installer`) 完成安装 / 卸载 / 检查 / 授权方式查询 / 默认安装器 / 用户列表, 同步 + Async + 会话三种形态, 错误为 `InstallerError`; 无插件时所有方法抛 (或拒绝) `PLUGIN_UNAVAILABLE`.

### P4.1 服务层与 augment

- [ ] (宿主) `runtime/api/installer/InstallerService.kt` (每脚本, `Closeable`, 脚本停止时取消全部会话并释放 PFD), `InstallerScriptOptions.kt` (选项规范化: `authorizer`, `interaction`, `allowDowngrade`, `allowTestOnly`, `bypassLowTargetSdk`, `installer`, `user`, `deleteSource`, `splits`, `timeout`, `continueOnError`), `InstallerScriptArguments.kt` (来源规范化: 字符串路径 / `content://` / `java.io.File` / `android.net.Uri` / 数组 / `{ splits }`), `InstallerScriptValues.kt`.
- [ ] (宿主) `runtime/api/augment/installer/Installer.kt` (`AugmentableKey("installer")`, `$installer` 别名与其它模块一致), `InstallSessionNativeObject.kt` (EventEmitter: `stage`, `progress`, `complete`, `error`, `cancel`; 方法 `cancel()`, `wait(timeout?)`, 属性 `id`, `state`, `result`), `InstallerJsErrors.kt` (`InstallerError` 构造与 `code` / `status` / `systemMessage` / `packageName`), `InstallerPromises.kt`; 在 `ScriptRuntime.augment` 装配 (与 `Mail` 相邻).
- [ ] (宿主) 方法: `install` / `installAsync` (单个与数组重载), `session`, `uninstall` / `uninstallAsync`, `inspect` / `inspectAsync`, `authorizer` 对象 (`state(name?)`, `request(name)` / `requestAsync`, `available`), `isDefault` / `setDefault` / `setDefaultAsync`, `users` / `usersAsync`, `isAvailable`, `status` 属性; 草案见附录 A.
- [ ] (测试) JVM: 选项 / 来源规范化边界 (空, 非法枚举, 超限数组, Unicode 路径), 错误映射, 数组与 `{ splits }` 判别; 宿主 `testAppDebugUnitTest` 通过.

### P4.2 会话形态与同步等待

- [ ] (宿主) 同步形态阻塞当前脚本线程 (Looper 脚本用 `Condition` 等待, 与 `mail` 一致), 停止脚本取消未完成调用; Async 形态回调在脚本线程; `session` 形态事件在脚本线程分发, 未监听 `error` 时不抛到全局 (记录到控制台).
- [ ] (测试) 设备 (需 P2 插件): `install` 同步 / Async / session 三形态各一次 (Shizuku), `uninstall`, `inspect` (含 AAB), 停止脚本时会话被取消.

### P4.3 示例与守卫

- [ ] (宿主) 示例脚本 `assets-app/sample/应用/静默安装应用.js`, `批量安装应用.js`, `设为默认安装器.js` (10 语言示例标题按既有示例目录约定); `assets-app/doc` 不在此改 (P7 生成).
- [ ] (宿主) 宿主 `.changelog` 10 语言 `feature`: `installer 模块, 用于安装, 更新与卸载应用, 支持 Shizuku / Root 静默安装 (需要 3-Setup Installer 插件) (参阅 项目文档 > [安装器](链接))`.

验收条件: 三形态 + 全部方法在真机走通; JVM 测试覆盖规范化与错误; 示例脚本可运行; changelog 已写.

---

## P5: 独立应用形态

目标: 插件可从启动器打开, 设置页遵循独立设置页规范, 默认安装器页可锁定 / 解锁, 关于 / 发行历史 / 更新检查齐备, 启动器图标四选项.

### P5.1 设置页

- [ ] (插件) `SettingsActivity` (代码构建的分组平面列表, 复制 3-Stove Agent `ui/kit` 套件后裁剪): 外观组 (语言 / 夜间模式 / 主题色 / 启动器图标, 默认跟随 AutoJs6, 经官方 host settings 契约读取, 宿主不可用回退系统与 `#FFDEAD`); 安装组 (授权方式顺序与启用 (拖动或上下移动), 默认交互 (`auto` / `dialog` / `silent`), 允许降级, 允许测试包, 绕过低 targetSdk, 安装者包名 (空 = 本插件; HyperOS 提示 `com.android.shell`), 目标用户, 安装后删除源文件); 通知组 (进度通知开关); 关于组 (默认安装器状态卡入口, 关于, 发行历史, 检查更新).
- [ ] (插件) 先选后确定的对话框语义, 中性色与主题色规则, 72 dp 行高与 24 dp 留白, TalkBack 与 RTL; 设置项持久化到 `SharedPreferences` (授权顺序为 JSON 数组), `AuthorizerResolver` 读取.
- [ ] (插件) 宿主设置入口: `InstallerSettingsActivity` (action `org.autojs.plugin.INSTALLER_SETTINGS`, PLUGIN 权限, `Theme.NoDisplay` 转发).
- [ ] (测试) JVM: 授权顺序序列化与非法值恢复, HEX / RGB 解析; instrumentation: 先选后确定 / 取消不保存 / 重建后值保持.

### P5.2 默认安装器页

- [ ] (插件) `DefaultInstallerActivity`: 状态卡 (当前默认处理者组件名, 是否本插件, 检测方式 D23), "设为默认" / "取消默认" 按钮 (特权路径, 选择授权方式), 无特权时的引导 (打开系统应用详情 "默认打开" 并说明步骤), 结果与失败原因 (OEM 限制按事实展示, 不承诺); 从宿主 / 脚本 `setDefault` 复用同一 `DefaultInstallerLock`.
- [ ] (测试) 设备: Shizuku 与 Root 各锁定 / 解锁一次, 锁定后从系统文件管理器打开 `.apk` 直接进入插件; API 24 / 31 / 35 三台.

### P5.3 关于, 发行历史与更新检查

- [ ] (插件) `AboutActivity` (圆角容器 + 透明图案, 版本 / 作者 / 仓库 / 许可证 / 第三方声明入口), `ReleaseHistoryActivity` (按 locale 读取 `doc/CHANGELOG-{tag}.md`, 回退英语), 更新检查 (固定 GitHub Releases API, 超时 / 取消 / 失败提示 / 忽略版本 / 12 小时频率限制, 对话框 Neutral 按钮打开内置发行历史, Positive 打开发布页).
- [ ] (测试) instrumentation: 发行历史各语言加载; JVM: 版本比较与忽略逻辑.

### P5.4 启动器与图标

- [ ] (插件) `LauncherActivity` (MAIN / LAUNCHER 移至四个 alias: `AdaptiveLightIconAlias` / `AdaptiveDarkIconAlias` / `AdaptiveAutoIconAlias` (默认启用) / `TransparentIconAlias`), `LauncherIcons` 切换 (`DONT_KILL_APP`, 先启用后禁用, 快捷方式归属迁移), `LauncherIconUpdateReceiver` (`MY_PACKAGE_REPLACED` 幂等修复); 图标资源由 P0.1 生成器产出, 用真机截图复核光学居中并按需设置 `OPTICAL_X` / `OPTICAL_Y` 后重新生成.
- [ ] (测试) `LauncherIconResourceTest` (透明 BitmapDrawable, 固定亮暗不随主题, 自动随配置且 undefined 回退暗色, API 26+ 自适应类型); instrumentation 四模式切换唯一入口 / 进程不死 / 重建持久化.

验收条件: 设置页, 默认安装器页, 关于 / 发行历史 / 更新检查, 四 alias 图标在 AVD API 24 与真机 API 33+ 验收; 证据写入 `docs/dev/p5-standalone-evidence.md`.

---

## P6: 健壮性, 安全, 兼容矩阵, 性能与体积

目标: 敌意输入, 进程死亡, 存储不足, 超大包, OEM 差异与并发均有确定行为; 发布 APK 体积与安装耗时有记录.

### P6.1 健壮性

- [ ] (插件) 进程死亡矩阵: 宿主死亡 (会话取消 + 暂存清理), 插件主进程死亡 (特权进程会话 `abandon`, 前台服务重建后不重复安装), 特权进程死亡 (`DeadObjectException` -> `AUTHORIZER_UNAVAILABLE`, 重绑一次后再失败), Shizuku 服务停止.
- [ ] (插件) 存储不足 (`INSTALL_FAILED_INSUFFICIENT_STORAGE` -> `INSUFFICIENT_STORAGE`), 暂存目录清理策略 (启动时清理超过 24 小时的残留), 超大包 (>= 2 GiB xapk 流式写入不占用等量堆), 同一包名并发会话串行化.
- [ ] (测试) instrumentation 覆盖上述矩阵; 用 `am kill` / `Shizuku` 停止模拟.

### P6.2 安全

- [ ] (插件) `HostCallerGuard` 校验 (签名 / 包名), 外部入口不信任文件名与 MIME, PFD 只读, 特权 Binder 只对本进程暴露 (UserService / RootService 的 Binder 不导出), 日志不记录路径以外的文件内容, `allowBackup=false`, 导出组件最小化审查表写入 `docs/dev/security-checklist.md`.
- [ ] (插件) 附录 E.2 回退路径 (`pm install-*` 命令) 若在 P0 启用, 命令参数全部白名单化, 不拼接用户字符串.
- [ ] (测试) instrumentation: 非宿主调用方被拒绝; 静态检查导出组件与权限.

### P6.3 兼容矩阵

- [ ] (测试) 设备池 (附录 E.1): AVD API 24 (Shizuku ADB / none), Sony G8441 API 28, Sony XQ-AT72 API 31, Redmi 22120RN86C API 33, Xiaomi 23046RP50C API 35 (HyperOS 安装者包名事实), AVD API 37 (16 KB 页, 仅验证插件本身安装与运行); 每台: none 新装 / 更新, 可用特权路径静默安装 / 卸载, 外部入口, 默认安装器锁定 (可用身份); 结果表写入 `docs/dev/p6-matrix-evidence.md`.
- [ ] (插件) OEM 差异按事实进入文案与 README 常见问题 (HyperOS 安装者包名, ColorOS 停止状态需激活, 部分 ROM 限制默认安装器).

### P6.4 性能与体积

- [ ] (测试) 记录: release APK 体积 (预期 < 3 MiB, libsu + Shizuku + HiddenApiBypass 无原生库), 100 MiB APK 在 Shizuku / Root / none 三路径的安装耗时, 特权进程冷启动耗时, 空闲 PSS; R8 规则 (隐藏 API 存根 `-keep`, libsu `RootService` 类名保留, AIDL Stub 保留) 与 release 构建的 `-PandroidTestRelease` 设备往返.

验收条件: 矩阵表完整, 未覆盖项明确列出; 安全检查表完成; 体积与耗时记录; 全部 JVM / instrumentation / lint 通过.

---

## P7: 文档, 发布与 1.0.0 gate

目标: 四个关联仓库同步, 插件 README / changelog 定稿, GitHub 仓库建立, 官方索引登记, 签名发布.

### P7.1 文档与声明

- [ ] (文档) `D:/webstorm-projects/AutoJs6-Documentation`: `api/installer.md` (模块页, 结构参照 `api/mail.md`: 插件依赖说明, `PLUGIN_UNAVAILABLE`, 同步 / Async / 会话三形态, `installer` 与 `$installer`), `api/installerInstallOptionsType.md`, `api/installerInstallResultType.md`, `api/installerSessionType.md`, `api/installerPackageInfoType.md`, `api/installerErrorType.md` (或合并进模块页, 按既有 mail 页面粒度); `api/sidebar.md` / `api/toc.md` 登记; `api/app.md` 的 `uninstall` 处交叉引用; 运行 `generator/auto-generate-for-autojs6.bat`, 随后提交文档仓库与 `AutoJs6-Plugin-Offline-Docs` (版本号自动变更).
- [ ] (文档) `D:/webstorm-projects/AutoJs6-TypeScript-Declarations`: `declarations/autojs6/aj6-int-installer.d.ts` (`@Source` 指向宿主 `runtime/api/augment/installer/*.kt` 与 `runtime/api/installer/*.kt`, `Internal.Installer` 命名空间, 重载与事件类型), `index.d.ts` 引用; 运行 `D:/idea-projects/android-dts-generator/aj6dts.bat -Publish`; 声明仓库与 `AutoJs6-Plugin-Ace-Editor` 的 `aj6-int-installer.d.ts` 同步, 两仓库版本号 +1 且版本名称按语义升级 (新增模块 -> y+1), Ace 仓库执行 `:app:generateAutoJs6LspDeclarations`; 分别提交.
- [ ] (宿主) `docs/dev/installer-plugin-protocol-v1.md` 补齐脚本 API 章节; 宿主 changelog 核对 (P1.5 / P4.3 已写条目合并整理, 日期为当日).

### P7.2 插件 README 与 changelog

- [ ] (插件) `.readme/lang_*.json` 10 语言: 简介, 功能 (安装 / 更新 / 卸载 / 批量 / 分包 / 静默 / 默认安装器), 安装 (插件中心向导或 Release), 授权方式说明 (Shizuku / Root 各自前提), 脚本示例 (`installer.install`, `installAsync`, `session`, `uninstall`, `setDefault`), 兼容性 (Android 7.0+, 特权能力的框架限制按事实), 常见问题 (HyperOS 安装者, 降级限制, 默认安装器被 ROM 限制, AAB), 发行历史, 许可证与第三方声明; 生成器 `--check` 通过.
- [ ] (插件) `.changelog` 10 语言 `v1.0.0` 定稿 (`feature` / `improvement` / `dependency` 分类, 依赖用 `附加` 术语记录 Shizuku-API / libsu / HiddenApiBypass / common-plugin-api / installer-api / package-archive-parser).

### P7.3 发布 gate

- [ ] (发布) 平台验收构建 (Temurin 参数) + `:app:testDebugUnitTest` + `:app:assembleDebugAndroidTest` + `:app:lintDebug` + `:app:appendDigestToReleasedFiles` (签名 APK, CRC32 文件名 `autojs6-plugin-three-setup-installer-v1.0.0-XXXXXXXX.apk`); `git diff --check`; `VERSION_BUILD == git rev-list --count HEAD`; 工作树干净.
- [ ] (发布) GitHub 仓库 `SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer` (功能性描述, 例如 `App installer plugin for AutoJs6 with Shizuku and Root silent installation`), 推送, tag `v1.0.0`, Release 附 APK 与 SHA-256.
- [ ] (索引) `official-repositories.json` 插入仓库名 (字母序, 总数 45 -> 46, README 计数同步), `release-manifests/io.github.supermonster003.autojs6.plugin.three.setup.installer/<versionCode>.json` 准入清单, 本地运行生成器验证后提交推送 `main`, 确认 Actions 生成成功.
- [ ] (宿主) 宿主提交 (`feat(installer): ...` 系列) 是否推送按维护者指示; 宿主 `PluginInstallWizardCatalog` 条目在索引可解析后生效.

验收条件: 四个关联仓库已提交; 插件 Release 与索引条目可被宿主插件中心向导发现并安装; 1.0.0 关闭.

---

## P8: 1.1.0 (Dhizuku, 持久化默认安装器, 通知栏模式, APK Inspector 迁移)

目标: 补齐维护者第一轮未纳入 1.0.0 的授权方式与安装模式, 并让兄弟仓库共用解析 AAR.

- [ ] (插件) Dhizuku 授权方式: 依赖 `io.github.iamr0s:Dhizuku-API` (核实许可证后记录), `DhizukuAuthorizer` (`Dhizuku.init` / `requestPermission`), `DhizukuInstallEngine` (`DevicePolicyManager` 所有者上下文的 `PackageInstaller`), 能力上报 `AUTHORIZERS` 增加 `dhizuku`; 脚本 `authorizer: 'dhizuku'`; 契约版本 2 (末尾追加方法, 旧顺序不变).
- [ ] (插件) 持久化默认安装器: Dhizuku 路径 `DevicePolicyManager.addPersistentPreferredActivity` / `clearPackagePersistentPreferredActivities`; Root 以 system 身份的 spike (libsu 自定义 `su 1000` 或 `app_process` uid 切换) 若成立则也提供; 状态卡区分 "偏好" 与 "持久化".
- [ ] (插件) 通知栏安装模式 (`interaction: 'notification'`): 无对话框, 通知承载确认 (none 路径仍需系统确认) 与结果; 设置页默认交互增加该项.
- [ ] (兄弟) APK Inspector 改为消费 `package-archive-parser.aar`, 删除其分叉副本中与 AAR 重合的文件, 专有解析保留; 其 `ROADMAP.md` 与 changelog 记录; 宿主 `docs/dev/package-inspection-roadmap.md` 同步.
- [ ] (文档) 文档 / d.ts / Ace / 离线文档同步 `dhizuku` 与 `notification`; 宿主与插件 changelog.

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
- 拍板: 待定.

### Q2 (P2 前): `auto` 顺序默认值

- 现状: InstallerX-Revived 由用户在配置文件中选择, 无固定顺序; 宿主双开卸载路径为 Shizuku -> Root -> 普通 shell.
- 推荐: `shizuku -> root -> none` (D14), 设置页可调.
- 拍板: 待定.

### Q3 (P2 前): 特权可用时脚本 `interaction` 默认是否为 `silent`

- 现状: D18 定义 `auto` = 特权可用则 `silent`.
- 风险: 脚本无提示地安装应用; 但脚本本身已是用户授权运行的自动化, 且 `dialog` 可显式指定.
- 推荐: 维持 D18; 宿主入口 (文件管理器 / 插件中心) 固定 `dialog`.
- 拍板: 待定.

### Q4 (P1 前): 宿主 `app.uninstall` 是否在插件可用时改走特权静默卸载

- 现状: `app.uninstall` 为 `ACTION_DELETE` 系统对话框.
- 推荐: 不改 (D24), 保持既有语义; 需要静默用 `installer.uninstall`.
- 拍板: 待定.

### Q5 (P1 前): 宿主自更新是否经插件

- 现状: `UpdateChecker` 下载后调用 `PackageInstallerActivity.install`.
- 推荐: 经路由 (D21): 插件可用时进入插件对话框 (可选静默由插件设置决定), 否则系统安装器.
- 拍板: 待定.

### Q6 (P3 前): 前台服务类型

- 现状: API 34+ 要求声明类型; 候选 `dataSync` (语义接近, 有时长限制但足够) 与 `specialUse` (需 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 说明).
- 推荐: `dataSync`, 实测超时行为后定.
- 拍板: 待定.

### Q7 (P5 前): 默认安装器锁定的 UI 位置

- 现状: InstallerX-Revived 在首页状态卡; 本插件无首页, 启动器入口直达设置页.
- 推荐: 设置页 "关于" 组之前独立一行 "默认安装器" 进入 `DefaultInstallerActivity`.
- 拍板: 待定.

### Q8 (P7 前): GitHub 仓库创建与推送时机, 宿主提交是否推送

- 推荐: 插件仓库在 P0.1 初始提交后即创建远端并推送 (便于 CI 运行); 宿主提交按既有惯例本地保留, 由维护者决定推送.
- 拍板: 待定.

### Q9 (P8 前): Root 以 system 身份 (uid 1000) 调用 `addPersistentPreferredActivity` 是否纳入

- 现状: InstallerX-Revived 未实现 (`canCallSystemRestrictedPreferredApis` 恒 false).
- 推荐: P8 做一次 spike, 成立则纳入, 否则只保留 Dhizuku 路径.
- 拍板: 待定.

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

- 之前的会话已完成 P0.1, 对应提交 `a5927f9`, `742e8be`, `4888af9`, `ea20a3b`, `e384d18`; 原会话记录未追加, 本次按已有源码与证据回填.
- 基础验收: 14 JVM 用例, API 24 / 35 各 5 个 Binder 契约用例, debug 构建与 lint, 10 语言文档与 15 项图标检查. 详见证据文件 P0.1 节.

### 2026-09-30 (P0.2 特权验证)

- 完成: P0.2 六项, 私有 AIDL, Shizuku / libsu 共享实现, AOSP 签名适配, 流式写入与清理, 默认安装器验证, 可重建夹具, 显式指定设备的复测脚本.
- 修正: FileBridge 不能当普通文件写入; reliable pipe 的 socket 在 API 28 Magisk 到 app 的 SELinux 传递中被拒绝; 快速解绑 / 重绑需要确认旧 Binder 死亡; 取消时须关闭排队任务的输出流; API 24 / 28 不支持 MIME `replacePreferredActivity`, 有竞争默认项时返回需先清除状态. 均记录在证据文件.
- 验证: 17 JVM 用例, 四组设备各 3 个特权用例, API 31 真机额外安装链路; debug / androidTest / release 构建与 lint; 基础 Binder 契约回归; Markdown 与图标检查. API 31 真机的默认项用例跳过以保留用户偏好, 独立 API 31 AVD 已补齐.
- 范围: 本节包含跨版本接口, 两种特权进程及真实设备验收, 本次集中闭合 P0.2; 未修改宿主, 未创建远端或推送. 宿主调用 / 脚本安装入口仍未开放.
- 下次会话建议起点: P1.1 共享解析 AAR, 顺利时连做 P1.2 契约模块. Q1 / Q4 / Q5 保留为 P1 的维护者决策点; P2 / P5 消费 D23 的 `DEFAULT_REQUIRES_CLEAR` 状态, P6 补充大包 / OEM / 并发生命周期矩阵.
