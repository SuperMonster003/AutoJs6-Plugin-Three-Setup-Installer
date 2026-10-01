<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>通过 Android 确认, Shizuku, Root 或 Dhizuku 安装, 更新与卸载应用</p>

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

3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku, Root 或 Dhizuku 执行特权操作.

插件独立处理安装包检查, 安装和结果, 可选择对话框, 静默或通知栏交互. 通知栏模式在通知中确认, 取消和展示结果; Android 仍要求系统确认时, 仅在点击对应通知后打开.

******

### 当前状态

******

1.1.0 已实现下述安装, 应用管理与脚本功能. 官方 GitHub Release 和插件中心索引准入仍待完成. 宿主接入需要 AutoJs6 >= 6.8.0 (5299), `installer` 脚本 API 需要构建 5300 或更高版本. 设备覆盖与剩余验收记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中. Dhizuku, 通知栏安装和持久默认安装器的脚本选项需要 AutoJs6 6.8.0 构建 5307 或以上及 installer V2 契约. 基础宿主接入仍支持构建 5299, V1 脚本方法从构建 5300 起可用.

******

### 功能

******

1.1.0 已实现的功能:

- 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 压缩包; 分包按设备选择; `.aab` 文件只识别与说明, 不安装.
- `none` 使用 Android 确认. 新配置的 `auto` 按 `shizuku -> root -> dhizuku -> none` 选择可用方式, 可调整顺序和启用状态. 已保存的旧三项配置保留原相对顺序及启用选择, Dhizuku 插入 `none` 前但默认禁用. 显式指定的授权方式不回退.
- 安装成功后可选尽力删除来源. 降级, 测试包, 绕过低 targetSdk 拦截 (Android 14+), 安装者归属及其他目标用户需要 Shizuku 或 Root, 并仍受 Android 规则限制.
- 已安装应用可按名称或包名搜索, 按名称, 安装或更新时间排序, 并可显示系统应用. 支持打开应用, 系统详情及确认卸载. Shizuku 和 Root 支持静默卸载及可选保留数据; Dhizuku 仅支持当前所有者用户的特权卸载, 不支持 `keepData`.
- 确认界面显示应用信息, 新旧版本, 签名和可勾选的 APK 分包; 进度可取消, 结果显示成功操作或可复制的错误详情. 批量安装逐项显示状态.
- 可打开或分享单个及多个安装包, 包括 MT 管理器分享的 APKS 文件. 多个安装包进入串行队列. 外部项目失败后, 在 URI 及访问权限仍可用时可重试.
- `interaction: 'notification'` 仅用于安装: 在通知中确认, 取消, 展示进度和结果, 不弹出插件安装对话框. Android 系统确认仍需点击对应通知. 必须允许通知并启用应用通知及安装渠道, 否则以 `NOTIFICATION_UNAVAILABLE` 失败. 其它交互模式不会因缺少通知许可而被阻止. 卸载不接受 `notification`.
- 外观设置包括语言, 夜间模式, 主题色与启动器图标. 前三项默认跟随 AutoJs6, 也可本地覆盖; 宿主不可用时回退系统语言和夜间模式及默认颜色. 图标提供亮色, 暗色, 自动与透明模式; 自动模式跟随系统, 效果受启动器缓存与遮罩影响.
- 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话形态, 支持单项 / 批量 / 分包安装, 卸载, 检查, 授权方式与用户查询及默认安装器设置; 失败为带稳定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)).
- 独立首页显示 Shizuku/Root/Dhizuku 的可用与授权状态, 当前默认安装器, 进行中任务和最近安装. 通过系统文件选择器多选安装包后串行安装, 可在单项失败后继续或取消剩余项目.
- 私有安装历史最多保留 200 项, 包含包名, 标签, 新旧版本, 结果, 时间, 来源 (宿主/脚本/外部/首页), 授权方式及失败详情. 可单条删除或清空, 不卸载应用也不删除源文件. 进程退出后未完成项目标为取消, 不会自动续跑.
- 设置保存授权顺序与启用状态, 安装选项及通知偏好. 首页/外部安装默认使用 `dialog`, 可显式选择 `auto`, `silent` 或 `notification`. 宿主界面安装入口使用 `dialog`; 脚本保留显式选项且默认仍为 `auto`. 修改在确认后保存.
- 设置中提供关于页面和十语言内置发行历史. 手动更新检查访问插件的 GitHub Releases API, 间隔 12 小时, 支持缓存结果与忽略版本管理. 发布页在浏览器中打开, 不会自动下载或安装更新.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.

******

### 使用方法

******

1. Android 7.0 及以上可在正式发布后从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 安装 APK, 或在官方索引准入后使用 AutoJs6 插件中心安装向导. 发布前可使用维护者提供的构建或从源码构建进行测试. 通过启动器图标打开独立首页.
2. 在首页检查授权与默认安装器状态, 选择单个或多个安装包. 通过所选对话框或通知确认, 并查看进行中任务与最近历史.
3. 接入 AutoJs6 时, 使用构建 5299 (6.8.0) 或更高版本, 并在插件中心启用 `3-Setup Installer`. 脚本 API 需要构建 5300 或更高版本. Dhizuku, 通知栏安装和持久默认安装器的脚本选项需要 AutoJs6 6.8.0 构建 5307 或以上及 installer V2 契约. 基础宿主接入仍支持构建 5299, V1 脚本方法从构建 5300 起可用.
4. 使用 AutoJs6 的安装操作, 或在打开及分享安装包时选择 3-Setup Installer. 出现确认对话框时, 先检查应用与安装选项再确认安装. 选择特权方式时, 请准备 Shizuku, Root 或 Dhizuku 授权.
5. 从首页菜单进入已安装应用或设置. 可检查本地安装默认项, 外观, 启动器图标和通知; 关于, 发行历史与手动更新检查位于设置中.

******

### 授权方式

******

各授权方式能做什么以及需要什么:

- `none`: 标准 PackageInstaller 会话; Android 会要求用户确认每次安装, 支持分包, 不提供特权选项.
- `shizuku`: 需要 Shizuku 正在运行 (经无线调试, ADB 或 Root 启动), 并单独向 3-Setup Installer 授权. 向 AutoJs6 授权不等于向本插件授权. 安装, 卸载及其他用户操作使用正在运行的 Shizuku 服务身份.
- `root`: 需要已 Root 的设备, 并由 Root 管理器向 3-Setup Installer 授予 `su`. 通过 libsu 提供特权安装, 卸载, 用户及默认安装器操作. 每项请求是否允许仍由 Android 和 ROM 策略决定.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.
- **注意:** 脚本默认使用 `interaction: 'auto'`, 特权可用时默认静默安装. 宿主界面的安装入口使用 `dialog`. 若 Android 要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时, 显式使用 `interaction: 'dialog'`. 显式 `silent` 在特权不可用或需要系统确认时以 `AUTHORIZER_REQUIRED` 失败.
- 设置保存授权顺序与启用状态, 安装选项及通知偏好. 首页/外部安装默认使用 `dialog`, 可显式选择 `auto`, `silent` 或 `notification`. 宿主界面安装入口使用 `dialog`; 脚本保留显式选项且默认仍为 `auto`. 修改在确认后保存.

******

### 快速开始

******

适用于 AutoJs6 >= 6.8.0 (5300) 的 `install`, `installAsync`, `session`, `uninstall` 和 `setDefault` 示例. 这些函数仅在传入自行选择的来源, 包名或默认安装器选项并调用时执行; 开头的状态查询为只读操作.

```js
// 只读查询可用状态与兼容信息.
console.log(installer.status);

// 需要 Shizuku 授权; silent 在 Android 要求确认时失败.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// 数组表示独立安装包, 每个项目分别返回结果.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// 所有分包属于同一个应用, 包括其 base APK.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// 调用后立即开始; 保留返回的会话可取消或等待.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// 仅传入确定要卸载的包名; keepData 请求保留数据.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true 设本插件为默认, false 清除本插件默认项; 受 ROM 限制.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// V2 示例需要宿主构建 5307. Dhizuku 需要已激活所有者, 通知模式需要通知许可.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root 持久模式要求设备支持用户 0 的 system UID 访问; 保留竞争策略.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});
```

来源可为路径, `file://` 或有读取权限的 `content://` URI. 数组表示独立批量项目, `{ splits: [...] }` 表示一个应用的分包. `session(...)` 创建后立即开始, 返回对象支持 `cancel()` 和 `wait()`. 同步调用可能抛出 `InstallerError`, 且不能在 UI 线程执行. 读取 `installer.status`, 创建 `installer.session(...)` 和调用 `session.wait()` 同样受此限制. UI 线程请使用 Async 方法, 或在脚本工作线程执行同步操作. 会话对象只能在创建它的脚本线程使用. 请处理 Promise 拒绝, 并逐项检查批量结果的 `ok` 与 `error`. 插件缺失或不兼容时报告 `PLUGIN_UNAVAILABLE`. `setDefault` 返回是否达到请求状态, 清除默认项成功也返回 true. `app.uninstall` 仍是宿主的系统卸载快捷入口, 需要特权选项时使用 `installer.uninstall`. 完整选项与事件见 [installer API 文档](https://docs.autojs6.com/#/installer).

Dhizuku, 通知栏安装和持久默认安装器的脚本选项需要 AutoJs6 6.8.0 构建 5307 或以上及 installer V2 契约. 基础宿主接入仍支持构建 5299, V1 脚本方法从构建 5300 起可用.

******

### 兼容性

******

决定插件能力边界的平台事实:

- Android 7.0 (API 24) 及以上. 设备验证情况和剩余覆盖范围记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- 绕过低 targetSdk 拦截自 Android 14 (API 34) 起存在; 更早的系统忽略该选项并在结果中注明.
- 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.

******

### 常见问题

******

- **为什么安装仍要求确认?** `none` 始终需要 Android 系统确认, 特权方式也可能受 Android 策略限制. `notification` 仅通过通知动作打开系统确认, 不绕过 Android 的确认要求.
- **能安装 `.aab` 吗?** 不能. Android App Bundle 是发布格式, 请先用 bundletool 转换为 `.apks` 集合. 插件会识别 `.aab` 文件并显示其包名与模块信息.
- **为什么设置 `allowDowngrade: true` 后仍可能降级失败?** 此选项仅请求允许降级; Android 根据固件, 授权身份及应用是否 debuggable 作出决定. 已测 user 固件中, Sony G8441 / API 28 与 Xiaomi 23046RP50C / API 35 拒绝非 debuggable 包降级, Sony XQ-DQ72 / API 33 的 Root 路径则接受. 这些结果只代表对应设备. 请检查返回的错误与 `systemMessage`; Root 不保证所有 ROM 都允许降级.
- **HyperOS 的安装者包名该怎么填?** Shizuku 经 ADB 或无线调试启动时, 不指定安装者包名会使用 `com.android.shell`. 已测 Xiaomi 23046RP50C / HyperOS / API 35 的静默新装与更新均记录此值. 显式指定 `com.android.shell` 或插件自身包名也都成功, 查询到的安装者与请求一致. 其他包名或 ROM 版本仍以系统答复为准.
- **ColorOS 或其他系统提示插件需要激活时怎么办?** 新装或强制停止后, Android 可能让应用保持停止状态, 等待用户交互. 请在 AutoJs6 插件中心使用提供的激活入口, 或从启动器图标打开 3-Setup Installer 后重试. 这遵循 [Android 的停止状态规则](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). ColorOS 专项行为尚未完成实机验证.
- **为什么设置默认安装器会失败, 或保存的持久标记与当前处理者不同?** 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- **为什么来源没有删除?** 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`. 批量中已确认成功的项目仍按 `deleteSource` 处理, 即使其他项目失败或剩余队列被取消.
- **可以重试或恢复吗?** 失败的外部 URI 项目在来源及访问权限仍可用时可以重试. 来源或访问权限释放后, 请重新打开安装包. 进程重启后, 恢复界面显示已确认并保存的结果, 未完成项目标为中断. 恢复界面为只读, 不会自动安装或重试. 再次开始前请检查应用的实际安装状态.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 外部 "打开方式" 入口只接受安装包文件, 从不运行脚本.
- REQUEST_INSTALL_PACKAGES 和 REQUEST_DELETE_PACKAGES 用于 Android 确认. QUERY_ALL_PACKAGES 用于已安装应用管理, 版本与签名比对以及默认安装器检测.
- FOREGROUND_SERVICE 和 FOREGROUND_SERVICE_DATA_SYNC 支持安装工作及临时来源访问; POST_NOTIFICATIONS 用于通知. `notification` 交互要求通知与安装渠道可用, 其它交互模式允许缺少通知许可.
- Shizuku, Root 与 Dhizuku 用于请求的操作. 插件不会自动配置设备/资料所有者. 持久默认规则只通过请求的设置或清除操作修改; 不上传安装包.
- 安装, 检查, 历史和应用管理均可离线使用. INTERNET 仅在用户手动检查版本时访问插件固定的 GitHub Releases API, 间隔 12 小时. 不后台检查更新, 不上传安装包.
- 安装包来源以只读方式打开. 历史只保存有限的应用元数据与结果, 不保存安装包内容或来源 URI, 错误中的路径会脱敏. 插件私有存储不参与备份. 删除历史不会卸载对应应用或删除来源.

正式发布后, 请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

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

`ThreeSetupInstallerPluginService` 响应 `org.autojs.plugin.INSTALLER` (category `installer`), 实现宿主 installer-api 契约 `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.1.0

_2026/10/02_

- `新增` `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.
- `新增` 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
- `新增` `interaction: 'notification'` 仅用于安装: 在通知中确认, 取消, 展示进度和结果, 不弹出插件安装对话框. Android 系统确认仍需点击对应通知. 必须允许通知并启用应用通知及安装渠道, 否则以 `NOTIFICATION_UNAVAILABLE` 失败. 其它交互模式不会因缺少通知许可而被阻止. 卸载不接受 `notification`.
- `优化` `none` 使用 Android 确认. 新配置的 `auto` 按 `shizuku -> root -> dhizuku -> none` 选择可用方式, 可调整顺序和启用状态. 已保存的旧三项配置保留原相对顺序及启用选择, Dhizuku 插入 `none` 前但默认禁用. 显式指定的授权方式不回退.
- `优化` 设置保存授权顺序与启用状态, 安装选项及通知偏好. 首页/外部安装默认使用 `dialog`, 可显式选择 `auto`, `silent` 或 `notification`. 宿主界面安装入口使用 `dialog`; 脚本保留显式选项且默认仍为 `auto`. 修改在确认后保存.
- `依赖` 附加 Dhizuku API 2.6.0 (MIT), 提供设备/资料所有者授权方式
- `依赖` 升级 `installer-api.aar` 为契约 V2 (MPL 2.0), 保留 V1 协商并在末尾追加持久默认方法; 产物来源与 SHA-256 见第三方声明; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `提示` 1.0.0 已实现下述安装, 应用管理与脚本功能. 官方 GitHub Release 和插件中心索引准入仍待完成. 宿主接入需要 AutoJs6 >= 6.8.0 (5299), `installer` 脚本 API 需要构建 5300 或更高版本. 设备覆盖与剩余验收记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
- `新增` 3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku 或 Root 执行特权操作
- `新增` 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 压缩包; 分包按设备选择; `.aab` 文件只识别与说明, 不安装
- `新增` 安装成功后可选尽力删除来源. 降级, 测试包, 绕过低 targetSdk 拦截 (Android 14+), 安装者归属及其他目标用户需要 Shizuku 或 Root, 并仍受 Android 规则限制
- `新增` 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话形态, 支持单项 / 批量 / 分包安装, 卸载, 检查, 授权方式与用户查询及默认安装器设置; 失败为带稳定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)). 脚本默认使用 `interaction: 'auto'`, 特权可用时默认静默安装. 宿主界面的安装入口使用 `dialog`. 若 Android 要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时, 显式使用 `interaction: 'dialog'`. 显式 `silent` 在特权不可用或需要系统确认时以 `AUTHORIZER_REQUIRED` 失败
- `新增` 独立首页显示 Shizuku/Root 的可用与授权状态, 当前默认安装器, 进行中任务和最近安装. 通过系统文件选择器多选安装包后串行安装, 可在单项失败后继续或取消剩余项目
- `新增` 确认界面显示应用信息, 新旧版本, 签名和可勾选的 APK 分包; 进度可取消, 结果显示成功操作或可复制的错误详情. 批量安装逐项显示状态
- `新增` 提供前台安装进度, 取消操作及结果通知. 未授予通知权限不会阻止安装
- `新增` 可打开或分享单个及多个安装包, 包括 MT 管理器分享的 APKS 文件. 多个安装包进入串行队列. 外部项目失败后, 在 URI 及访问权限仍可用时可重试
- `新增` 已安装应用支持按名称或包名搜索, 按名称, 安装时间或更新时间排序, 并可显示系统应用. 可打开应用或系统应用信息, 或检查后确认卸载. Shizuku 或 Root 可在确认后直接卸载并选择保留数据; 其它情况使用 Android 确认
- `新增` 首页状态卡与设置进入同一默认安装器页面, 支持特权设定和清除, 无特权时提供系统设置引导. OEM 策略可能阻止更改或要求先清除原处理者. 脚本仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 结果如实反映设备响应
- `新增` 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存
- `新增` 外观设置包括语言, 夜间模式, 主题色与启动器图标. 前三项默认跟随 AutoJs6, 也可本地覆盖; 宿主不可用时回退系统语言和夜间模式及默认颜色. 图标提供亮色, 暗色, 自动与透明模式; 自动模式跟随系统, 效果受启动器缓存与遮罩影响
- `新增` 私有安装历史最多保留 200 项, 包含包名, 标签, 新旧版本, 结果, 时间, 来源 (宿主/脚本/外部/首页), 授权方式及失败详情. 可单条删除或清空, 不卸载应用也不删除源文件. 进程退出后未完成项目标为取消, 不会自动续跑
- `新增` 设置中提供关于页面和十语言内置发行历史. 手动更新检查访问插件的 GitHub Releases API, 间隔 12 小时, 支持缓存结果与忽略版本管理. 发布页在浏览器中打开, 不会自动下载或安装更新
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `优化` 可随机访问的来源避免完整缓存副本, 流来源按需暂存. 支持普通 ZIP 分包, AAB 仅供检查, 拒绝内容发生变化的来源
- `优化` 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`. 批量中已确认成功的项目仍按 `deleteSource` 处理, 即使其他项目失败或剩余队列被取消
- `优化` 同包安装会话跨用户和授权方式串行执行, 等待时仍支持取消与超时, 并在独立入口安全清理超过 24 小时的非活动暂存目录
- `优化` 建立特权连接时若连接意外中断, 可自动重连一次; 已经发起的安装或卸载不会自动重复
- `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
- `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
- `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
- `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 附加 `installer-api.aar` (AutoJs6, MPL 2.0) 提供安装契约; 产物来源和 SHA-256 见第三方声明
- `依赖` 附加 `package-archive-parser.aar` (AutoJs6, MPL 2.0) 提供 APK 与容器检查及分包选择; 产物来源和 SHA-256 见第三方声明

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

开发者可使用以下命令构建并验证插件. 正式发布前, 使用维护者提供的构建或本地构建进行测试; 正式 APK 将通过 Releases 及完成索引准入后的插件中心分发.

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
