<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>为 AutoJs6 及其脚本安装, 更新和卸载 Android 应用, 支持通过 Shizuku 或 Root 静默安装</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Setup Installer 接管 AutoJs6 的安装器: 文件管理器, 插件中心与脚本打包页的安装按钮, `.apk`, `.apks`, `.xapk`, `.apkm` 与 `.apkz` 文件的外部 "打开方式" 入口, 以及脚本侧用于安装, 更新, 检查与卸载应用的全局对象 `installer`. 除常规的系统确认外, 还可通过 Shizuku 或 Root 静默安装与卸载.

AutoJs6 通过 Binder 服务发现插件, 以只读文件描述符交出安装包; 插件解析安装包, 选择授权方式, 按需显示自己的确认与进度对话框, 并回报阶段, 进度与结果. 特权操作在 Shizuku 用户服务或 libsu Root 服务中执行, 直接与系统包安装器对话.

******

### 当前状态

******

1.0.0: P2 开发预览: 安装, 包信息查询, 用户查询和卸载核心已接入宿主服务, 支持显式确认和会话自动清理. 完整宿主入口验收, 完整界面, 外部打开, 默认安装器启用, 脚本 API 和设置仍在推进. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### 功能

******

目标能力, 按路线图分阶段交付:

- 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 压缩包; 分包按设备选择; `.aab` 文件只识别与说明, 不安装.
- 授权方式: `none` (系统 PackageInstaller 会话 + 用户确认), `shizuku` 与 `root`; `auto` 按设置页中的顺序选择第一个可用者, 脚本也可显式指定.
- 安装选项: 批量安装, 成功后删除源文件, 允许降级, 允许测试包, 绕过低 targetSdk 拦截 (Android 14+), 安装者包名与目标用户 (仅特权授权方式).
- 通过 Shizuku 或 Root 静默卸载并可选保留数据; 其它情况使用常规系统对话框.
- 设为默认安装器: 有 Shizuku 或 Root 时插件成为安装包文件的首选处理者; 无特权时为你打开系统的 "默认打开" 页面.
- 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话三种形态; 每个失败都是带稳定 `code` 的 `InstallerError`.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5299 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `3-Setup Installer` 已被识别并启用它.
3. 在 AutoJs6 文件管理器中点击安装包, 在任意文件管理器中用 3-Setup Installer 打开安装包, 或在脚本中调用 `installer.install(...)`. 需要静默安装时, 按插件提示启动 Shizuku 或授予 Root, 或在插件设置中选择授权方式.

******

### 授权方式

******

各授权方式能做什么以及需要什么:

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 应用处于运行状态 (经无线调试, ADB 或 Root 启动) 并已向插件授权; 以 shell 权限运行, 可静默安装, 静默卸载, 面向其他用户安装以及锁定默认安装器.
- `root`: 需要 Root 管理器向插件授予 `su`; 通过 libsu Root 服务提供与 Shizuku 相同的操作. 在普通 (user) 固件上降级仍只对 debuggable 应用生效, 这是框架规则而非插件限制.
- **注意:** 特权授权可用时, 脚本 API 默认静默安装, 不会主动弹出任何确认对话框. 若系统仍要求确认, `interaction: 'auto'` 会允许系统确认并写入 `notes`. 需要安装前确认时, 请显式使用 `interaction: 'dialog'`; 禁止系统确认时使用 `interaction: 'silent'`, 此时需要确认的安装会失败.

******

### 快速开始

******

一个静默安装, 允许降级地更新, 监听会话并卸载应用的脚本 (自路线图 P4 起可用):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### 兼容性

******

决定插件能力边界的平台事实:

- Android 7.0 (API 24) 及以上; 宿主构建与插件在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 列出的设备矩阵上一起验证.
- 绕过低 targetSdk 拦截自 Android 14 (API 34) 起存在; 更早的系统忽略该选项并在结果中注明.
- 部分 OEM 系统限制哪个应用可以成为默认安装器, 或要求其信任的安装者包名 (HyperOS 接受 `com.android.shell`); 插件按原样回报系统的答复.

******

### 常见问题

******

- **为什么安装仍然要求确认?** `none` 授权方式始终经过系统确认. 启动 Shizuku 或授予 Root, 然后在设置中选择该授权方式, 或在脚本中传入 `authorizer: 'shizuku'`.
- **能安装 `.aab` 吗?** 不能. Android App Bundle 是发布格式, 请先用 bundletool 转换为 `.apks` 集合. 插件会识别 `.aab` 文件并显示其包名与模块信息.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 外部 "打开方式" 入口只接受安装包文件, 从不运行脚本.
- REQUEST_INSTALL_PACKAGES 与 REQUEST_DELETE_PACKAGES 支撑常规的安装与卸载对话框; QUERY_ALL_PACKAGES 让插件在更新前显示已安装版本并比对签名.
- Shizuku 与 Root 只用于你发起的操作; 特权服务不保存状态, 操作之间不保持打开的 shell, 也不会被插件之外的任何方访问.
- 安装包以只读方式打开; 插件不发起网络请求, 不收集数据, 并将私有存储排除在备份之外.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

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

`ThreeSetupInstallerPluginService` 响应 `org.autojs.plugin.INSTALLER` (category `installer`), 自路线图 P1 起实现宿主 installer-api 契约 `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/10/01_

- `提示` P2 开发预览: 安装, 包信息查询, 用户查询和卸载核心已接入宿主服务, 支持显式确认和会话自动清理. 完整宿主入口验收, 完整界面, 外部打开, 默认安装器启用, 脚本 API 和设置仍在推进.
- `新增` 插件标识 `three-setup-installer` (engine `installer`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.INSTALLER` 服务骨架
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `优化` P0 已验证 Shizuku 和 Root 静默安装, 更新, 卸载及普通默认安装器设置. 宿主与脚本安装入口尚未开放, 本版本仍不支持持久默认项.
- `优化` 插件 ID, engine, 服务 action / category, Binder descriptor 与最低宿主版本改由宿主 installer-api 契约常量提供; 能力声明加入安装器契约版本 1, 最低宿主构建回填为 5299
- `优化` 可随机访问的来源避免完整缓存副本, 流来源按需暂存. 支持普通 ZIP 分包, AAB 仅供检查, 拒绝内容发生变化的来源.
- `优化` 显式选择的授权方式不回退, 区分拒绝, 超时与不兼容, 并发请求共享授权过程与特权连接.
- `优化` 安装与更新核心支持系统确认, Shizuku 和 Root, 可取消操作并返回实际确认方式与系统处理结果.
- `优化` 卸载核心支持系统确认, Shizuku 和 Root, 特权卸载可选择保留应用数据.
- `优化` 支持串行批量安装, 失败后继续或取消剩余项, 并可通过特权方式校验和选择目标用户.
- `优化` 宿主服务接入包信息查询, 安装, 卸载和用户查询, 支持显式确认, 调用方退出时取消, 最多四个并发会话及自动清理.
- `优化` 安装对话框默认跟随 AutoJs6 的语言, 夜间模式与主题色, 支持宿主不可用时回退, 大字号和 RTL 布局.
- `优化` 后台安装支持前台服务, 进度, 取消和结果通知; 未授予通知权限不会阻止安装.
- `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
- `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
- `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
- `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 附加 `package-archive-parser.aar` 与 `installer-api.aar` (AutoJs6 模块 `plugin-api/package-archive-parser` 与 `plugin-api/installer-api`, 宿主 P1 构建 6.8.0 / 5299, MPL 2.0), 与 `common-plugin-api.aar` 一同在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 升级共享安装包解析器, 支持普通 ZIP 分包容器

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

本节面向希望从源码构建插件的开发者; 普通用户直接安装 Releases 页面的预构建 APK 即可.

构建 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并构建 instrumentation 测试 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集发布产物并在文件名后追加版本与 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档源与生成产物是否同步 (CI 同样执行此检查):

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

******

### 本地化与文档生成

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

`.readme/` 与 `.changelog/` 下的语言 JSON 文件是 README, 插件中心说明与更新日志的唯一文案源. 请始终修改这些 JSON 源文件并重新运行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 与更新日志产物不得手工编辑. 运行 `py .python/generate_markdown.py --check` 可校验全部生成产物.

******

### 许可证

******

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- 安装器模块文档: https://docs.autojs6.com/#/installer
- InstallerX 与 InstallerX Revived (架构参考, GPL-3.0, 未复用代码): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
