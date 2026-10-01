# AutoJs6-Plugin-Three-Setup-Installer AGENTS.md

本文件是本仓库的工程约定, 由 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么". 同目录的 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` (Three 系列命名), `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` (独立设置页) 与 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标) 在对应能力落地时同样适用.

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库不包含原生库, 模型, 上游源码快照或 ABI 拆分; 参考规范中对应的 CONDITIONAL 条款不适用 (见第 5.4 节的省略理由).
- 本仓库拥有运行在特权进程中的安装服务 (Shizuku UserService 与 libsu RootService, 路线图 P0.2 / P2.2), 隐藏 API 存根 (P0.2), 插件自有的安装对话框与外部 `ACTION_VIEW` 入口 (P3), 独立设置页与发行历史 (P5). 这些条款以 CONDITIONAL 形式保留在第 9 节与第 14 节.
- 插件只在用户手动检查发行版本 (P5.3) 时访问固定的 GitHub Releases API, 其它功能完全离线. 不申请存储, 网络 (更新检查除外), 无障碍或麦克风权限.

## 2. 仓库身份

下列值在 Gradle, Manifest, Kotlin 常量 (`ThreeSetupInstallerPlugin`), 资源, 文档, 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `ManifestContractTest` 与 `ThreeSetupInstallerPluginRuntimeInfoTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-Three-Setup-Installer` |
| `rootProject.name` | `autojs6-plugin-three-setup-installer` |
| 应用标题 (不可翻译) | `3-Setup Installer` (机器标识写 `three` / `Three`, 面向人的文本写 `3-Setup Installer`, 宿主向导回退标题写 `Three Setup Installer`) |
| `applicationId` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.setup.installer` |
| 插件 ID / engine / variant | `three-setup-installer` / `installer` / `default` |
| Binder 服务类 | `ThreeSetupInstallerPluginService` (主进程) |
| 服务发现 action / category | `org.autojs.plugin.INSTALLER` / `installer` |
| INFO 服务 | `ThreeSetupInstallerPluginInfoService`, action `org.autojs.plugin.INFO`, category `installer` |
| 设置入口 action (P5.1) | `org.autojs.plugin.INSTALLER_SETTINGS` |
| 宿主契约标识 | AIDL 包 `org.autojs.plugin.installer.api`, 契约类 `InstallerContract` / `InstallerIds` / `InstallerActions` / `InstallerCapabilityKeys` / `InstallerErrorCodes` / `IInstaller*` 由宿主 `installer-api` AAR (路线图 P1.2) 决定; P0 阶段 `ThreeSetupInstallerPlugin` 以字面量声明同一组值, P1.2 落地后改为引用契约常量 |
| 共享解析 AAR | `package-archive-parser` (宿主 `plugin-api/package-archive-parser`, 路线图 P1.1) |
| 最低宿主 versionCode | `ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION` = `InstallerIds.REQUIRED_HOST_VERSION_CODE` = 5299 (AutoJs6 6.8.0 的 P1 构建: `installer-api`, `package-archive-parser` 与宿主客户端首次交付, 路线图 P1.5 回填于 2026-09-30) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 (建仓时 plugins.gradle.org 已发布 1.8.6, 与兄弟仓库统一升级时再更新) |
| 发布文件名 | `autojs6-plugin-three-setup-installer-v{VERSION_NAME}-{CRC32}.apk` (单 APK) |
| 图标源图 | `.python/icons/three-setup-ic-launcher-light.png` / `-dark.png` (1254 x 1254 RGBA, alpha 一致, 图案 `#272727` / `#D8D8D8`; light / dark 指使用它的模式); `ADAPTIVE_GLYPH = 0.41` |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主入口和公共契约.
- 不提交本地缓存, IDE 状态, 调试输出或无意生成的二进制文件.
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整. 本地构建 SHOULD 传入 `-Pautojs.gradle.build.number.auto.increment.enabled=false -Pautojs.gradle.build.time.update.enabled=false` (PowerShell 下加引号) 避免无意变更.
- 修改第三方依赖时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`), 并在 changelog 的 `dependency` 分类记录.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, 授权方式, 度量值), 不勾选没有证据的条目.

### 3.3 提交

- 维护者于 2026-10-01 最新指示: 本插件仓库当前仅作本地提交, 暂时不推送到 GitHub 远端. 后续会话继续遵守, 直至维护者明确恢复推送.
- 维护者于 2026-10-02 明确允许 P7 远端发布继续延迟时实施 P8 及后续项. 此授权仅调整阶段依赖, 不恢复任何仓库的远端推送, 标签或发布.
- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(priv): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 本地路径, 临时 APK 或无关改动.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-Three-Setup-Installer/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml (JVM / APK / lint + API 24 / 35 模拟器契约测试), markdown.yml
|-- .python/                    generate_markdown.py (+ .bat, check_markdown.bat), generate_launcher_icons.py, icons/
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/
|   |-- src/main/java/io/github/supermonster003/autojs6/plugin/three/setup/installer/
|   |   |-- ThreeSetupInstallerPlugin.kt                 身份常量
|   |   |-- ThreeSetupInstallerPluginInfoService.kt      IPluginInfoProvider
|   |   |-- ThreeSetupInstallerPluginService.kt          org.autojs.plugin.INSTALLER (P2.6 起带宿主身份校验的 IInstallerPlugin.Stub)
|   |   |-- ThreeSetupInstallerPluginInfo.kt / ThreeSetupInstallerPluginRuntimeInfo.kt
|   |   |-- WakeActivity.kt
|   |   `-- (路线图 4.2 节: binder/ source/ engine/ priv/ auth/ ui/ 随 P0.2 - P5 加入)
|   |-- src/main/res/           values*/ x 11, mipmap*/ (生成), raw*/plugin_instruction.md (生成), xml/
|   |-- src/test/               JVM 契约与资源守卫测试
|   |-- src/androidTest/        Binder 契约 instrumentation
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   |-- build.gradle.kts / proguard-rules.pro
|-- build-logic/                org.autojs.build.{utils,versions,signs,properties,jvm-convention,local-arr-register-convention}
|-- docs/dev/                   各阶段证据 (p0-spike-evidence.md 等)
|-- gradle/                     wrapper, libs.versions.toml
|-- libs/                       common-plugin-api.aar, package-archive-parser.aar, installer-api.aar (哈希锁定, 见 libs/README.md), README.md
|-- locks/host-api-aars.lock    宿主 AAR SHA-256 锁
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties, gradlew(.bat)
`-- sign.properties             本地签名配置, Git 忽略
```

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- 平台插件只在根 `settings.gradle.kts` 应用一次, 位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不重复应用. 禁止 `mavenLocal()`.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 声明 `com.android.application` 并 `apply false`; 模块只应用插件, 不写版本. 不声明 `org.jetbrains.kotlin.android` (AGP 内置 Kotlin 已覆盖).
- `app` 模块从 `version.properties` 与 `org.autojs.build.versions` 读取 compileSdk / minSdk / targetSdk / versionCode / versionName.
- 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`. 不提交 `gradle/data` 消费端覆盖.

平台验收命令 (Temurin 环境模拟, 必须只输出一段版本决策):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界

- Gradle 构建 MUST 自包含, 禁止引用兄弟仓库的 JAR / AAR 或 `flatDir`.
- 宿主 AAR 只从 `libs/` 消费, 由 `locks/host-api-aars.lock` 锁定 SHA-256; `app/build.gradle.kts` 在配置期拒绝缺失文件, debug 产物, 占位哈希, 多余锁条目与摘要不符. 更新 AAR 时同一提交内更新锁文件, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md`.
- libsu 来自 JitPack (根 `build.gradle.kts` 已声明仓库); Shizuku-API 与 HiddenApiBypass 来自 Maven Central. 新增依赖优先 Maven Central / Google Maven.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, 由 `.gitignore` 忽略 (`git check-ignore` 已验证).
- `appendDigestToReleasedFiles` 依赖 `assembleRelease`, 签名缺失时失败, 校验产物集合为单个 `autojs6-plugin-three-setup-installer-v{VERSION_NAME}.apk` 并追加 CRC32.
- 构建产物不入库 (`releases/` 被忽略).

### 5.4 不启用 ABI 拆分的理由

插件由 Kotlin 字节码与资源构成; Shizuku-API, libsu 与 HiddenApiBypass 均不含原生库, `nativeAlignment { expectNoNativeLibraries.set(true) }` 在每次构建校验. 因此不启用 ABI splits, `getInfo()` 显式返回 `supportedAbis = emptyArray()`, 16 KB 页对齐记为不适用.

## 6. Manifest 与激活协议

- `org.autojs.permission.PLUGIN`, `WAKE_ACTIVITY` meta-data, `WakeActivity` (exported, `Theme.NoDisplay`, PLUGIN 权限, WAKE action + DEFAULT category, 立即结束), `org.autojs.plugin.info.AUTHOR`, `NATIVE_PAGE_ALIGNMENT=0`.
- INFO 服务与 INSTALLER 服务均 exported, 受 PLUGIN 权限保护, 携带 `requiresHostVersion` meta-data.
- 权限清单与理由 (每次新增权限时更新本表, README 安全节与 `ManifestContractTest`):

| 权限 | 理由 |
|---|---|
| `REQUEST_INSTALL_PACKAGES` / `REQUEST_DELETE_PACKAGES` | `none` 授权方式的系统安装 / 卸载对话框 (D17 / D24) |
| `QUERY_ALL_PACKAGES` | 已安装应用列表, 版本与签名比对, 默认安装器状态检测 (D23 / D36); 插件经 GitHub 分发, 不受商店政策限制, 在 Manifest 以 `tools:ignore` 标注 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | 安装写入期间的 dataSync 前台服务与进度通知 (D26 / P3.4) |
| `POST_NOTIFICATIONS` | 进度与结果通知; 原有交互可降级, 显式 notification 安装必须可见, 缺失或 channel 关闭时返回 NOTIFICATION_UNAVAILABLE |
| `INTERNET` | 仅用户手动检查更新时访问本插件固定的 GitHub Releases API, 12 小时间隔与缓存结果; 无后台检查或安装包上传, 安装及应用管理保持离线 (P5.3) |
| `moe.shizuku.manager.permission.API_V23` | Shizuku 授权方式 (D2) |
| `com.rosan.dhizuku.permission.API` | Dhizuku 设备/资料所有者授权及安装 (P8, API 26+) |

- `<queries>`: 宿主包, Shizuku / Dhizuku 管理器包, 以及 `ACTION_VIEW` + 安装包 MIME 的 intent (默认安装器状态检测). Dhizuku API 合并其已知 provider 查询. 新增跨包访问时先加 queries.
- `rikka.shizuku.ShizukuProvider` 以 `${applicationId}.shizuku` 为 authority, exported 且受 `INTERACT_ACROSS_USERS_FULL` 保护, 这是 Shizuku 的固定注册方式.
- 外部 `ACTION_VIEW` 入口 (P3.2) 与启动器 alias (P5.4) 为 CONDITIONAL: 外部入口不受 PLUGIN 权限保护但只接受安装包 URI, 不执行脚本; 启动器 MAIN / LAUNCHER 只放在四个 alias 上.

## 7. PluginInfo 与能力协商

- `name` 来自不可翻译的 `app_name`, `description` 来自当前 locale 的 `plugin_description`, `instruction` 来自 `@raw/plugin_instruction`, `versionName` / `versionCode` 来自已安装包, `versionDate` 来自 `plugin_version_date` resValue, `id` / `engine` / `variant` 来自 `ThreeSetupInstallerPlugin`.
- `supportedAbis = emptyArray()` 在 `getInfo()` 中显式写出.
- `capabilities` 自 P1.2 起含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION` 与 `InstallerCapabilityKeys.CONTRACT_VERSION`; `AUTHORIZERS`, `FEATURES`, `MAX_BATCH`, `MAX_SPLITS` 随 P2.6 的真实 Binder 路由一起声明 (路线图附录 B), 不提前声明尚未实现的能力.
- 新增可选方法时先协商能力, 不通过捕获异常猜测协议版本.
- P8 实现 V1/V2 共存: 最小能力版本为 1, 最大版本为 2; V1 AIDL 顺序冻结, V2 仅在末尾追加 setDefaultInstallerV2. 新请求选项须按同一 Binder 的实时能力协商; 未改变结构的结果 envelope 保持版本 1. 基础宿主仍为 5299, V1 脚本为 5300, 完整 V2 状态字段由 5307 起提供.

## 8. Binder 与公共 API 设计

- 常量, option key, capability key, 错误码, 上限集中在宿主 `installer-api` 契约 (路线图附录 B); 插件不散落字符串字面量.
- 全部输入按路线图 D31 上限校验: 批量 <= 32, 分包 <= 64, JSON <= 64 KiB, 安装者包名 <= 255, 并发会话 <= 4. 超限返回 `INVALID_ARGUMENT`, 不崩溃.
- 文件只以 `ParcelFileDescriptor` (只读) 或 `content://` 传递, 不依赖绝对路径 (D16). PFD 归宿主所有, 插件在会话内 `dup` 后自持并在结束时关闭.
- 已发布 AIDL 只在末尾追加方法, 由宿主 `InstallerAidlOrderTest` 冻结顺序.
- 宿主进程死亡 (`linkToDeath`) 时取消会话并清理暂存; 会话终态对象未 `close` 时 10 分钟后由单个注册表任务回收, 不为每个已关闭对象保留独立定时器.
- 可 seek 来源优先通过持有的描述符读取, 不改变宿主文件偏移; 管道或系统禁止 procfs 重开的来源才按需暂存. 安装准备与写入时校验内容摘要, 发现来源变化则拒绝提交; 来源仍须在准备与写入期间保持稳定.

## 9. 特权进程, 隐藏 API 与许可证边界 (CONDITIONAL, P0.2 起)

- Shizuku `UserService` 与 libsu `RootService` 实现同一私有 AIDL `IPrivilegedInstaller`, 共用 `PrivilegedInstallerImpl`; 特权 Binder 只对本应用进程暴露, 不导出.
- 隐藏 API 适配 (`priv/hidden/`) 按 AOSP 接口签名自写, 通过反射调用设备自带 Stub, 不复制框架实现, 不打包 `android.*` 存根; 在 `THIRD_PARTY_NOTICES.md` 注明 Apache-2.0 签名来源. `HiddenApiBypass` 只在 API 28+ 调用, 每进程一次.
- 主路径为 Binder 隐藏 API (D15); `pm` 命令回退 (路线图附录 E.2) 只在 P0.2 决策点触发时启用, 参数白名单化. API 24 / 25 的用户运行状态使用 `IActivityManager`, API 26+ 使用 `IUserManager`; 反射异常必须转换为 Binder 支持的异常再跨进程返回.
- 参考项目 InstallerX / InstallerX-Revived 为 GPL-3.0 (D29): 只参考架构, 行为与选项目录, MUST NOT 复制其源码, 资源, 字符串; 提交说明与文档只以 "参考" 表述.
- 特权服务只持有正在执行的会话, 不持久化状态; 客户端 Binder 死亡与服务关闭时放弃会话, 关闭运行与排队写入的所有描述符. 操作之间不保持打开的 shell; 授权探测与 RootService 启动统一管理 shell 生命周期. Shizuku 每次新绑定使用独立 tag, 避免已关闭服务的延迟死亡通知影响新连接. 日志不记录安装包内容, 只记录格式, 大小, 耗时与错误码.
- 特权写入用普通 pipe 转交框架 `Session.openWrite` / `fsync`, 不直接写入隐藏接口的 FileBridge PFD, 不使用跨 Magisk / app 的 reliable pipe socket. 默认项只精确替换 APK filter; 不清除其它包的所有首选项, 旧 API 返回 `DEFAULT_REQUIRES_CLEAR` 时由后续 UI 引导处理.
- Dhizuku API 为 MIT, 其 GPL 管理器由用户独立安装, 不随插件打包. API 26 以下不初始化 API, 不因其 minSdk 声明提高本插件的最低版本. 核对实时系统 owner 与 provider 的包, UID, 当前用户和签名身份; 只包装新建框架对象, 不污染进程全局 PackageManager. 仅支持当前用户, 安装者归属实际 owner; 不冒充 shell/root 的安装 flags 或 keepData.
- Dhizuku owner 创建的系统 session 可跨插件进程死亡存活. journal 只记录明确归属的 session 与随机来源标记, 不存原始包来源; 恢复只处理已知 id, 必须核对 owner, user, 包信息和标记, 不碰活跃进程的会话, 不重放安装. API 26/27 及创建响应丢失时无法证明归属的会话不得猜测清理.
- 持久默认只在操作确认后记本地回执. persistentConfigured 只表示上次成功配置, 普通状态读取不得据此宣称当前策略. Dhizuku 暂限 API 26-33; API 34+ 缺少 owner PolicyUpdateReceiver 最终结果时预写入拒绝. 部分失败记录不确定性, 不通过清除未知旧策略补偿.
- Root 持久默认使用独立的 system UID 1000 进程, 固定 user 0/本插件组件/四个 APK filter, 不改变共享 RootService 身份. 写入前必须核对握手与策略基线, 明确提交后才可修改; 不覆盖竞争策略, 不降级 SELinux. 取消或进程死亡不重放操作, 不确定结果保留审计.
- notification 只用于安装. 确认, 取消与系统确认均对应当前会话/单次 token; none 的系统确认仍需用户点击通知打开, 不自动弹出插件安装对话框. 外部临时 URI 授权须在 NoDisplay Activity 结束前交给前台服务, 重启不得恢复 worker 或来源授权.

## 10. 字符串资源

- 11 个目录: `values/` (默认英语) 与 `values-en/` 逐字相同, 另有 ar, es, fr, ja, ko, ru, zh, zh-rHK, zh-rTW.
- `strings.xml` 按 `name` 升序; 不可翻译项 (`app_name`) 放 `strings_donottranslate.xml`; plurals 放 `plurals.xml`.
- 全部 locale 使用 ASCII 标点 (含日语, 韩语, 阿拉伯语的逗号与句号), 省略号写 `...` 并加 `tools:ignore="TypographyEllipsis"` (lint 已全局禁用该检查). `ApplicationTextPunctuationTest` 扫描 `app/src/main`, `.readme`, `.changelog`, `docs`, `README.md`, `ROADMAP.md`, `AGENTS.md`, `THIRD_PARTY_NOTICES.md` 的 xml / md / json.
- `plugin_description` 句尾无点号, 不含 "AutoJs6" 字样 (`StringResourceParityTest`).
- 图标由 `.python/generate_launcher_icons.py` 从 `.python/icons/three-setup-ic-launcher-light.png` 确定性生成, 不手工编辑 `mipmap*/` 输出; 修改源图或比例后重新生成, 运行 `--check`, 并更新第 2 节与 changelog.

## 11. README, 插件说明与 changelog

- `.readme/lang_*.json` (10 语言, 键集合一致, 列表键 `features` / `usage_steps` / `authorizer_points` / `compatibility_points` / `faq_items` / `security_points`) 与 `.changelog/lang_*.json` 是唯一文案源; 生成物 (`README.md`, `.readme/README-*.md`, `app/src/main/assets/doc/CHANGELOG*.md`, `app/src/main/res/raw*/plugin_instruction.md`) 不手工编辑.
- 修改 JSON 或模板后运行 `py .python/generate_markdown.py` 再 `--check`; CI `markdown.yml` 在 Windows 上执行 `.python/check_markdown.bat`.
- 根 `README.md` 为简体中文, 与 `.readme/README-zh-Hans.md` 同源.
- changelog 分类只用 `hint` / `feature` / `fix` / `improvement` / `dependency`; 简体中文依赖条目用 `附加` / `升级` / `降级` / `替换` / `移除`; 当前版本 key 为 `v{VERSION_NAME}` (忽略后缀), `released_date` 为当日 `YYYY/MM/DD`; 涉及 feature / fix / improvement / dependency 的提交 MUST 更新 10 语言 JSON.
- 文案面向使用者, 不写内部类拆分, Binder 传输细节或测试数量; 行为变化, 权限, 默认值与兼容性必须如实记录; 不夸大特权能力 (降级, 默认安装器等框架限制按事实写).

## 12. 独立界面与设置 (CONDITIONAL, P3 / P5 起)

- 安装对话框, 外部入口, 设置页, 默认安装器页, 关于与发行历史遵循 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`: 外观四项顺序 (语言 / 夜间模式 / 主题色 / 启动器图标) 默认跟随 AutoJs6, 先选后确定, 中性表面 + 主题色控件, 24 dp 圆角对话框, 72 dp 行高.
- 发行历史页按 locale 读取 `doc/CHANGELOG-{tag}.md`, 回退英语; 更新对话框 Neutral 打开内置发行历史, Positive 打开发布页; 更新检查有超时, 取消, 失败提示, 忽略版本与 12 小时频率限制.
- 启动器图标四 alias 见 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md`; Three 的 `ic_launcher` 保持透明 BitmapDrawable, 不创建同名自适应 XML.

## 13. 测试要求

### 13.1 JVM

- `ManifestContractTest` (权限, queries, meta-data, 组件导出与权限, 服务发现契约, Shizuku provider), `ThreeSetupInstallerPluginRuntimeInfoTest` (PluginInfo 纯数据映射与身份常量对齐 `common.json` / `build.gradle.kts` / `settings.gradle.kts`), `StringResourceParityTest`, `ApplicationTextPunctuationTest`.
- P1 起: 来源与格式识别, 授权方式解析顺序, 选项规范化与上限, 错误映射; P5 起: 授权顺序序列化, HEX / RGB 解析, 版本比较.

### 13.2 Android instrumentation

- `ThreeSetupInstallerPluginContractTest`: Wake Activity 契约, INFO 服务 `getInfo()` 往返 (含显式空 `supportedAbis` 与能力 Bundle), INSTALLER 服务 descriptor, Shizuku provider 注册, P0 无启动器入口.
- P0.2 起: 特权 spike 的真机往返 (Shizuku / Root 静默安装, 更新, 卸载; `addPreferredActivity` 可行性); P2 起: Binder happy path, 敌意输入, 上限, 宿主死亡; P3 / P5 起: 对话框与设置页.
- 设备池与证据等级见 `ROADMAP.md` 附录 E; 多台设备时用明确 serial, 每次会话重新读取 SDK / ABI; 不卸载用户的已安装应用, 不清空启动器数据.
- `PrivilegedInstallerDeviceTest` 必须显式传 `privilegedAuthorizer=shizuku|root`, 普通 CI 自动跳过; `tools/run-privileged-spike.ps1` 拒绝把失败或跳过当作成功. 夹具若预先存在立即拒绝操作, 默认项测试若已有 APK 首选项则跳过. debug-only spike 入口与状态接收器不得进入 release.

### 13.3 CI

- `build.yml`: JVM 测试编译, `verifyNativePageAlignment`, `testDebugUnitTest`, androidTest 与 release APK, lint debug / release; API 24 x86 与 API 35 x86_64 模拟器运行契约测试 (特权路径不在 CI 模拟器上运行).
- `markdown.yml`: Windows 上 `check_markdown.bat`.

## 14. 验证顺序

```powershell
py .python/generate_markdown.py --check
py .python/generate_launcher_icons.py --check
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:testDebugUnitTest
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:connectedDebugAndroidTest
```

- 纯文档改动只需前两条; 涉及源码的改动至少跑 JVM 测试与 debug 装配; 涉及 Manifest, Binder 或特权服务的改动必须在至少一台真机或 AVD 上跑 instrumentation.
- Release 前额外执行 `:app:appendDigestToReleasedFiles`, 检查 `releases/` 只有一个已签名 APK 且 CRC32 与内容一致.
- 任何未执行的验证都在最终说明中明确列出原因.

## 15. 许可证, 安全与隐私

- `LICENSE` 为 MPL-2.0 完整文本, README 徽章与 `THIRD_PARTY_NOTICES.md` 一致.
- `allowBackup=false` 且 `dataExtractionRules` 排除全部数据; 导出组件最小化并受签名权限保护 (Shizuku provider 除外, 其权限由 Shizuku 约定).
- 安装包只读打开; 不记录安装包内容或用户文件正文; 特权操作只在用户或宿主发起时执行.
- 第三方组件的版本, 来源, 许可证与 SHA-256 (宿主 AAR) 记录在 `THIRD_PARTY_NOTICES.md`.

## 16. 参考项目路由

- 构建骨架, Three 身份, AAR 锁, 四 alias 图标, 代码构建的设置 UI 套件: `D:/idea-projects/AutoJs6-Plugin-Three-Stove-Agent`
- 宿主契约消费方式, `CallerGuard`, 设置入口 action, 文档生成器与 README 结构: `D:/idea-projects/AutoJs6-Plugin-Angus-Mail`
- 安装包解析器副本与外部 `ACTION_VIEW` 的 MIME 列表: `D:/idea-projects/AutoJs6-Plugin-APK-Inspector`
- 宿主入口, 插件发现, 安装引导与被迁出的安装器实现: `D:/idea-projects/AutoJs6` (`ui/main/scripts/`, `core/plugin/`, `plugin-api/`)
- 参考项目 (只读架构参考, GPL-3.0): `https://github.com/iamr0s/InstallerX`, `https://github.com/wxxsfxyzm/InstallerX-Revived`

参考时以这些仓库的当前代码为准; 复制骨架后必须替换身份字段, URL, 文案, 常量, 版本与测试数据.
