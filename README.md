<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>通过系统确认, Shizuku 或 Root 安装, 更新与卸载 Android 应用</p>

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

3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku 或 Root 执行特权操作.

AutoJs6 通过 Binder 服务发现插件, 以只读文件描述符交出安装包; 插件解析安装包, 选择授权方式, 按需显示自己的确认与进度对话框, 并回报阶段, 进度与结果. 特权操作在 Shizuku 用户服务或 libsu Root 服务中执行, 直接与系统包安装器对话.

******

### 当前状态

******

1.0.0: 开发预览, 已提供独立首页, 设置, 已安装应用管理, 串行队列及安装历史. 支持安装确认, 进度, 结果和前台通知. 进程重启后保留已确认并保存的结果, 未完成任务标为取消, 不会自动续装或重试. `installer` 脚本 API 需要 AutoJs6 >= 6.8.0 (5300); 宿主基础接入需要构建 5299. 设备覆盖与剩余验收见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

******

### 功能

******

本开发预览已提供的功能:

- 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 压缩包; 分包按设备选择; `.aab` 文件只识别与说明, 不安装.
- 授权方式: `none` 使用 Android 确认; `shizuku` 与 `root` 提供特权操作. `auto` 默认依次选择可用的 Shizuku, Root 和系统确认. 设置中可调整授权顺序与启用状态; 显式选择的方式不会静默回退.
- 安装成功后可选尽力删除来源. 降级, 测试包, 绕过低 targetSdk 拦截 (Android 14+), 安装者归属及其他目标用户需要 Shizuku 或 Root, 并仍受 Android 规则限制.
- 已安装应用支持按名称或包名搜索, 按名称, 安装时间或更新时间排序, 并可显示系统应用. 可打开应用或系统应用信息, 或检查后确认卸载. Shizuku 或 Root 可在确认后直接卸载并选择保留数据; 其它情况使用 Android 确认.
- 确认界面显示应用信息, 新旧版本, 签名和可勾选的 APK 分包; 进度可取消, 结果显示成功操作或可复制的错误详情. 批量安装逐项显示状态.
- 可打开或分享单个及多个安装包, 包括 MT 管理器分享的 APKS 文件. 多个安装包进入串行队列. 外部项目失败后, 在 URI 及访问权限仍可用时可重试.
- 提供前台安装进度, 取消操作及结果通知. 未授予通知权限不会阻止安装.
- 外观设置包括语言, 夜间模式, 主题色与启动器图标. 前三项默认跟随 AutoJs6, 也可本地覆盖; 宿主不可用时回退系统语言和夜间模式及默认颜色. 图标提供亮色, 暗色, 自动与透明模式; 自动模式跟随系统, 效果受启动器缓存与遮罩影响.
- 首页状态卡与设置进入同一默认安装器页面, 支持特权设定和清除, 无特权时提供系统设置引导. OEM 策略可能阻止更改或要求先清除原处理者. 脚本仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 结果如实反映设备响应.
- 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话形态, 支持单项 / 批量 / 分包安装, 卸载, 检查, 授权方式与用户查询及默认安装器设置; 失败为带稳定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)).
- 独立首页显示 Shizuku/Root 的可用与授权状态, 当前默认安装器, 进行中任务和最近安装. 通过系统文件选择器多选安装包后串行安装, 可在单项失败后继续或取消剩余项目.
- 私有安装历史最多保留 200 项, 包含包名, 标签, 新旧版本, 结果, 时间, 来源 (宿主/脚本/外部/首页), 授权方式及失败详情. 可单条删除或清空, 不卸载应用也不删除源文件. 进程退出后未完成项目标为取消, 不会自动续跑.
- 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存.
- 设置中提供关于页面和十语言内置发行历史. 手动更新检查访问插件的 GitHub Releases API, 间隔 12 小时, 支持缓存结果与忽略版本管理. 发布页在浏览器中打开, 不会自动下载或安装更新.

******

### 使用方法

******

1. 在 Android 7.0 或更高版本上, 从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 页面安装插件 APK. 独立启动器入口可打开首页.
2. 在首页查看授权与默认安装器状态, 点击添加按钮选择单个或多个安装包. 检查安装对话框后确认, 并在首页查看进度和最近历史.
3. 接入 AutoJs6 时, 使用构建 5299 (6.8.0) 或更高版本, 并在插件中心启用 `3-Setup Installer`. 脚本 API 需要构建 5300 或更高版本.
4. 使用 AutoJs6 的安装操作, 或在打开及分享安装包时选择 3-Setup Installer. 出现确认对话框时, 先检查应用与安装选项再确认安装. 选择特权方式时, 请准备 Shizuku 或 Root 授权.
5. 从首页菜单进入已安装应用或设置. 可检查本地安装默认项, 外观, 启动器图标和通知; 关于, 发行历史与手动更新检查位于设置中.

******

### 授权方式

******

各授权方式能做什么以及需要什么:

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 正在运行 (经无线调试, ADB 或 Root 启动) 并已向插件授权. 其 shell 权限支持静默安装, 静默卸载及面向其他用户的操作.
- `root`: 需要 Root 管理器向插件授予 `su`; 通过 libsu Root 服务提供与 Shizuku 相同的操作. 在普通 (user) 固件上降级仍只对 debuggable 应用生效, 这是框架规则而非插件限制.
- **注意:** 特权可用时, 宿主请求使用 `interaction: 'auto'` 默认静默安装, 不会主动打开确认界面. 若 Android 仍要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时使用 `interaction: 'dialog'`; 禁止系统确认时使用 `interaction: 'silent'`, 此时需要确认的安装会失败. 脚本 API 沿用相同默认语义.
- 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存.

******

### 快速开始

******

安装, 批量与会话操作的模板函数 (需要 AutoJs6 >= 6.8.0 (5300)); 先自行选择并核实来源, 再调用对应函数. 下面的示例不会自动安装, 卸载或修改默认安装器.:

```js
// Read-only probe. The functions below run only when explicitly called with chosen sources.
console.log(installer.status);

// An already authorized Shizuku service is required; silent never falls back to a dialog.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// An array means independent applications, including an array containing one source.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// A source may also be { splits: [...] } for one application's split files.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};
```

******

### 兼容性

******

决定插件能力边界的平台事实:

- Android 7.0 (API 24) 及以上. 设备验证情况和剩余覆盖范围记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 绕过低 targetSdk 拦截自 Android 14 (API 34) 起存在; 更早的系统忽略该选项并在结果中注明.
- 部分 OEM 系统限制哪个应用可以成为默认安装器, 或要求其信任的安装者包名 (HyperOS 接受 `com.android.shell`); 插件按原样回报系统的答复.

******

### 常见问题

******

- **为什么安装仍然要求确认?** `none` 始终使用系统确认. 准备好授权后, 可在安装对话框中选择 Shizuku 或 Root. Android 或设备策略仍可能要求系统确认.
- **能安装 `.aab` 吗?** 不能. Android App Bundle 是发布格式, 请先用 bundletool 转换为 `.apks` 集合. 插件会识别 `.aab` 文件并显示其包名与模块信息.
- **为什么来源没有删除?** 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`.
- **可以重试或恢复吗?** 失败的外部 URI 项目在来源及访问权限仍可用时可以重试. 来源或访问权限释放后, 请重新打开安装包. 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试. 再次开始前请检查应用的实际安装状态.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 外部 "打开方式" 入口只接受安装包文件, 从不运行脚本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用于 Android 确认. QUERY_ALL_PACKAGES 用于已安装应用管理, 版本与签名比对以及默认安装器检测.
- FOREGROUND_SERVICE 与 FOREGROUND_SERVICE_DATA_SYNC 支持后台安装工作; POST_NOTIFICATIONS 用于进度与结果通知. 缺少通知权限不会阻止安装.
- Shizuku 与 Root 只用于你发起的操作; 特权服务不保存状态, 操作之间不保持打开的 shell, 也不会被插件之外的任何方访问.
- 安装, 检查, 历史和应用管理均可离线使用. INTERNET 仅在用户手动检查版本时访问插件固定的 GitHub Releases API, 间隔 12 小时. 不后台检查更新, 不上传安装包.
- 安装包来源以只读方式打开. 历史只保存有限的应用元数据与结果, 不保存安装包内容或来源 URI, 错误中的路径会脱敏. 插件私有存储不参与备份. 删除历史不会卸载对应应用或删除来源.

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

- `提示` 开发预览, 已提供独立首页, 设置, 已安装应用管理, 串行队列及安装历史. 支持安装确认, 进度, 结果和前台通知. 进程重启后保留已确认并保存的结果, 未完成任务标为取消, 不会自动续装或重试. `installer` 脚本 API 需要 AutoJs6 >= 6.8.0 (5300); 宿主基础接入需要构建 5299. 设备覆盖与剩余验收见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)
- `新增` 3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku 或 Root 执行特权操作
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `新增` 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话形态, 支持单项 / 批量 / 分包安装, 卸载, 检查, 授权方式与用户查询及默认安装器设置; 失败为带稳定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300))
- `新增` 独立首页显示 Shizuku/Root 的可用与授权状态, 当前默认安装器, 进行中任务和最近安装. 通过系统文件选择器多选安装包后串行安装, 可在单项失败后继续或取消剩余项目
- `新增` 私有安装历史最多保留 200 项, 包含包名, 标签, 新旧版本, 结果, 时间, 来源 (宿主/脚本/外部/首页), 授权方式及失败详情. 可单条删除或清空, 不卸载应用也不删除源文件. 进程退出后未完成项目标为取消, 不会自动续跑
- `新增` 已安装应用支持按名称或包名搜索, 按名称, 安装时间或更新时间排序, 并可显示系统应用. 可打开应用或系统应用信息, 或检查后确认卸载. Shizuku 或 Root 可在确认后直接卸载并选择保留数据; 其它情况使用 Android 确认
- `新增` 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存
- `新增` 首页状态卡与设置进入同一默认安装器页面, 支持特权设定和清除, 无特权时提供系统设置引导. OEM 策略可能阻止更改或要求先清除原处理者. 脚本仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 结果如实反映设备响应
- `新增` 设置中提供关于页面和十语言内置发行历史. 手动更新检查访问插件的 GitHub Releases API, 间隔 12 小时, 支持缓存结果与忽略版本管理. 发布页在浏览器中打开, 不会自动下载或安装更新
- `修复` 系统缺少对应翻译时, 取消操作文案未跟随插件语言的问题
- `修复` 修复 base.apk 等必选分包在禁用状态下勾选标记不可见的问题, 覆盖亮色与暗色模式
- `修复` 修复 Files by Google 等文件提供方使用不含扩展名的内容 URI 和通用 ZIP/二进制 MIME 类型时, 安装包容器的打开候选中不显示插件的问题
- `修复` 拒绝未按只读模式返回安装包的文件提供程序, 并及时释放无效来源句柄
- `修复` 修复暂存, 解压和特权管道写入时空间不足被误报为安装包无效或普通管道错误的问题
- `修复` 修复 Shizuku 或 Root 连接中断后操作未及时结束的问题, 现在会提示授权方式不可用
- `优化` 插件 ID, engine, 服务 action / category, Binder descriptor 与最低宿主版本改由宿主 installer-api 契约常量提供; 能力声明加入安装器契约版本 1, 最低宿主构建回填为 5299
- `优化` 可随机访问的来源避免完整缓存副本, 流来源按需暂存. 支持普通 ZIP 分包, AAB 仅供检查, 拒绝内容发生变化的来源.
- `优化` 显式选择的授权方式不回退, 区分拒绝, 超时与不兼容, 并发请求共享授权过程与特权连接.
- `优化` 安装与更新核心支持系统确认, Shizuku 和 Root, 可取消操作并返回实际确认方式与系统处理结果.
- `优化` 卸载核心支持系统确认, Shizuku 和 Root, 特权卸载可选择保留应用数据.
- `优化` 支持串行批量安装, 失败后继续或取消剩余项, 并可通过特权方式校验和选择目标用户.
- `优化` 宿主服务接入包信息查询, 安装, 卸载和用户查询, 支持显式确认, 调用方退出时取消, 最多四个并发会话及自动清理.
- `优化` 外观设置包括语言, 夜间模式, 主题色与启动器图标. 前三项默认跟随 AutoJs6, 也可本地覆盖; 宿主不可用时回退系统语言和夜间模式及默认颜色. 图标提供亮色, 暗色, 自动与透明模式; 自动模式跟随系统, 效果受启动器缓存与遮罩影响
- `优化` 后台安装支持前台服务, 进度, 取消和结果通知; 未授予通知权限不会阻止安装.
- `优化` 新增安装确认, 进度与结果界面, 包含应用信息, APK 分包选择, 安装选项, 错误复制和批量逐项状态; 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试.
- `优化` 系统安装确认支持未知来源权限引导和中断处理; 特权卸载在确认前显示应用信息与保留数据选项.
- `优化` 支持外部打开和分享单个或多个安装包, 在来源访问仍有效时重试失败项目, 并可在成功后尽力删除来源; 删除被拒绝不会改变安装成功的结果.
- `优化` 打开 MT 管理器分享的 APKS 安装包时支持 application/vnd.android.package-archives MIME 类型
- `优化` 同包安装会话跨用户和授权方式串行执行, 等待时仍支持取消与超时, 并在独立入口安全清理超过 24 小时的非活动暂存目录
- `优化` 建立特权连接时若连接意外中断, 可自动重连一次; 已经发起的安装或卸载不会自动重复
- `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
- `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
- `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
- `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 附加 `package-archive-parser.aar` 与 `installer-api.aar` (AutoJs6 模块 `plugin-api/package-archive-parser` 与 `plugin-api/installer-api`, MPL 2.0), 与 `common-plugin-api.aar` 一同在 `locks/host-api-aars.lock` 中锁定哈希
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
