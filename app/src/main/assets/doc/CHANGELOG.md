******

### 发行历史

******

# v1.2.0

###### 2026/10/02

* `新增` 高级安装选项: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` 和 `packageSource`. 平台或授权方式不支持时明确拒绝, 不静默忽略. none 不追加手动编译, 也不关闭 Android 自身的编译.
* `优化` 高级脚本选项需要 AutoJs6 构建 5308+ 并协商 V3 与 `advanced-install-options`. 省略字段保持原行为, 显式 `false`/`none` 仍需对应支持. 本地实现不表示已经正式发布或全部 P9 条目完成.
* `优化` 成功结果可报告读回的 `updateOwner` 和 `dexopt`. null 表示 Android 未向当前调用身份返回 owner, 可能没有 owner 或受可见性过滤, 不能证明全局不存在; 读取失败省略字段并写入 notes. DexOpt 状态为 accepted/failed/cancelled/timeout/unavailable/unknown; accepted 包含系统跳过, 不证明实际执行编译. 附加步骤失败不改变已经确认的安装成功.
* `优化` 权限授予请求及非 none 的 DexOpt 需要 Shizuku/Root, verify 需要 API 26+. 安装原因需要 API 26+, 来源标签需要 API 33+, 请求更新所有权需要 API 34+. 所有权仅能在初装时启用, 更新或其他用户已有该包时可能被忽略; false 不撤销既有 owner.
* `优化` 授予选项请求系统可授予的权限, 也可能包含 Android 14 的 USE_FULL_SCREEN_INTENT 等安装器可改变的 app-op. 不保证全部声明权限, 不授予无障碍, 悬浮窗或任意签名权限. restricted/system-fixed/policy-fixed 限制仍有效, 不额外设置 restricted 权限 allowlist 标志.
* `依赖` 升级 installer-api.aar 至契约 V3 (MPL 2.0), 保留 V1/V2 与全部 11 个 AIDL 事务; 高级脚本选项需要宿主构建 5308+
* `依赖` 升级共享安装包解析器 (MPL 2.0), 核验真实清单根元素及 sharedUserId, 拒绝有歧义的输入

# v1.1.0

###### 2026/10/02

* `新增` `dhizuku`: 需要 Android 8.0 (API 26)+, 已激活的 Dhizuku 设备/资料所有者, 并向本插件授权. 只操作当前所有者用户, 安装者归属使用真实所有者包名. 不提供 shell/root 的降级, 测试包, 绕过低 targetSdk, 其他用户, 任意安装者归属或卸载保留数据选项. 插件不自动配置所有者.
* `新增` 默认安装器页面区分普通偏好与持久化策略. 普通偏好通过 Shizuku 或 Root 设置, 仍受 ROM 限制. Dhizuku 的持久化策略支持 API 26-33; API 34+ 因无法核验所有者回调, 在修改前拒绝. Root 仅在受支持设备的用户 0 中使用 system UID 辅助进程. 不覆盖竞争的持久策略. `persistentConfigured` 仅记录此前成功配置的回执, 不是当前系统策略的证明; 被动查询只报告 `preferred` 或 `none`.
* `新增` `interaction: 'notification'` 仅用于安装: 在通知中确认, 取消, 展示进度和结果, 不弹出插件安装对话框. Android 系统确认仍需点击对应通知. 必须允许通知并启用应用通知及安装渠道, 否则以 `NOTIFICATION_UNAVAILABLE` 失败. 其它交互模式不会因缺少通知许可而被阻止. 卸载不接受 `notification`.
* `优化` `none` 使用 Android 确认. 新配置的 `auto` 按 `shizuku -> root -> dhizuku -> none` 选择可用方式, 可调整顺序和启用状态. 已保存的旧三项配置保留原相对顺序及启用选择, Dhizuku 插入 `none` 前但默认禁用. 显式指定的授权方式不回退.
* `优化` 设置保存授权顺序与启用状态, 安装选项及通知偏好. 首页/外部安装默认使用 `dialog`, 可显式选择 `auto`, `silent` 或 `notification`. 宿主界面安装入口使用 `dialog`; 脚本保留显式选项且默认仍为 `auto`. 修改在确认后保存.
* `依赖` 附加 Dhizuku API 2.6.0 (MIT), 提供设备/资料所有者授权方式
* `依赖` 升级 `installer-api.aar` 为契约 V2 (MPL 2.0), 保留 V1 协商并在末尾追加持久默认方法; 产物来源与 SHA-256 见第三方声明; AutoJs6 >= 6.8.0 (5307).

# v1.0.0

###### 2026/10/02

* `提示` 1.0.0 已实现下述安装, 应用管理与脚本功能. 官方 GitHub Release 和插件中心索引准入仍待完成. 宿主接入需要 AutoJs6 >= 6.8.0 (5299), `installer` 脚本 API 需要构建 5300 或更高版本. 设备覆盖与剩余验收记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) 中.
* `新增` 3-Setup Installer 可从独立首页, AutoJs6 入口与脚本, 以及安装包的外部打开和分享请求安装, 更新, 检查与卸载 Android 应用. 支持 Android 系统确认及通过 Shizuku 或 Root 执行特权操作
* `新增` 安装包格式: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` 以及包含 APK 的 ZIP 压缩包; 分包按设备选择; `.aab` 文件只识别与说明, 不安装
* `新增` 安装成功后可选尽力删除来源. 降级, 测试包, 绕过低 targetSdk 拦截 (Android 14+), 安装者归属及其他目标用户需要 Shizuku 或 Root, 并仍受 Android 规则限制
* `新增` 脚本 API `installer` (别名 `$installer`) 提供同步, `...Async` 与会话形态, 支持单项 / 批量 / 分包安装, 卸载, 检查, 授权方式与用户查询及默认安装器设置; 失败为带稳定 `code` 的 `InstallerError` (需要 AutoJs6 >= 6.8.0 (5300)). 脚本默认使用 `interaction: 'auto'`, 特权可用时默认静默安装. 宿主界面的安装入口使用 `dialog`. 若 Android 要求确认, `auto` 允许系统确认并记录到 `notes`. 需要安装前确认时, 显式使用 `interaction: 'dialog'`. 显式 `silent` 在特权不可用或需要系统确认时以 `AUTHORIZER_REQUIRED` 失败
* `新增` 独立首页显示 Shizuku/Root 的可用与授权状态, 当前默认安装器, 进行中任务和最近安装. 通过系统文件选择器多选安装包后串行安装, 可在单项失败后继续或取消剩余项目
* `新增` 确认界面显示应用信息, 新旧版本, 签名和可勾选的 APK 分包; 进度可取消, 结果显示成功操作或可复制的错误详情. 批量安装逐项显示状态
* `新增` 提供前台安装进度, 取消操作及结果通知. 未授予通知权限不会阻止安装
* `新增` 可打开或分享单个及多个安装包, 包括 MT 管理器分享的 APKS 文件. 多个安装包进入串行队列. 外部项目失败后, 在 URI 及访问权限仍可用时可重试
* `新增` 已安装应用支持按名称或包名搜索, 按名称, 安装时间或更新时间排序, 并可显示系统应用. 可打开应用或系统应用信息, 或检查后确认卸载. Shizuku 或 Root 可在确认后直接卸载并选择保留数据; 其它情况使用 Android 确认
* `新增` 首页状态卡与设置进入同一默认安装器页面, 支持特权设定和清除, 无特权时提供系统设置引导. OEM 策略可能阻止更改或要求先清除原处理者. 脚本仍可使用 `installer.isDefault`, `installer.setDefault` 和 `setDefaultAsync`, 结果如实反映设备响应
* `新增` 设置可保存授权顺序与启用状态, 安装选项和进度通知偏好. 首页及外部安装默认使用 `dialog`; 显式保存的 `auto` 或 `silent` 选择会生效. 宿主/脚本请求保留其显式选项, 脚本 API 默认仍为 `auto`. 选择项仅在确认后保存
* `新增` 外观设置包括语言, 夜间模式, 主题色与启动器图标. 前三项默认跟随 AutoJs6, 也可本地覆盖; 宿主不可用时回退系统语言和夜间模式及默认颜色. 图标提供亮色, 暗色, 自动与透明模式; 自动模式跟随系统, 效果受启动器缓存与遮罩影响
* `新增` 私有安装历史最多保留 200 项, 包含包名, 标签, 新旧版本, 结果, 时间, 来源 (宿主/脚本/外部/首页), 授权方式及失败详情. 可单条删除或清空, 不卸载应用也不删除源文件. 进程退出后未完成项目标为取消, 不会自动续跑
* `新增` 设置中提供关于页面和十语言内置发行历史. 手动更新检查访问插件的 GitHub Releases API, 间隔 12 小时, 支持缓存结果与忽略版本管理. 发布页在浏览器中打开, 不会自动下载或安装更新
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `优化` 可随机访问的来源避免完整缓存副本, 流来源按需暂存. 支持普通 ZIP 分包, AAB 仅供检查, 拒绝内容发生变化的来源
* `优化` 仅在安装成功后尝试删除. 安装失败, 取消或超时始终保留来源. 删除失败不会改变安装成功的结果, 外部来源提供方可能拒绝删除. 脚本的 `deleteSource` 由宿主删除路径或 `file://` 来源, 保留 `content://` 来源; 请检查 `sourceDeleted` 和 `notes`. 批量中已确认成功的项目仍按 `deleteSource` 处理, 即使其他项目失败或剩余队列被取消
* `优化` 同包安装会话跨用户和授权方式串行执行, 等待时仍支持取消与超时, 并在独立入口安全清理超过 24 小时的非活动暂存目录
* `优化` 建立特权连接时若连接意外中断, 可自动重连一次; 已经发起的安装或卸载不会自动重复
* `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
* `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
* `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `installer-api.aar` (AutoJs6, MPL 2.0) 提供安装契约; 产物来源和 SHA-256 见第三方声明
* `依赖` 附加 `package-archive-parser.aar` (AutoJs6, MPL 2.0) 提供 APK 与容器检查及分包选择; 产物来源和 SHA-256 见第三方声明
