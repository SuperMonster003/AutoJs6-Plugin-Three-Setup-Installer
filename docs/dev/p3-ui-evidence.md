# P3 安装界面与外部入口证据

日期: 2026-10-01. 本轮沿用 ROADMAP.md 的 P3.1-P3.4, 不增加或拆分路线图条目. 宿主与插件的公开 V1 AIDL / AAR / 哈希锁均未改变.

## 实现与状态边界

- 安装确认显示名称, 图标, 包名, 旧 / 新版本, SDK, 大小, 签名比较, 分包与安装选项. 用户选择的分包, 授权方式, 安装标志和用户会进入实际引擎, 不能只改变界面. base 不可取消, 选择后重新验证 split 依赖.
- 进度与批量逐项结果来自实际会话事件; commit 与系统确认阶段使用不确定进度, 不以写入 100% 冒充安装成功. 结果可复制错误和打开当前用户已安装应用. 其他用户的应用不误用当前用户启动 Intent.
- 外部入口支持 VIEW / INSTALL_PACKAGE / SEND / SEND_MULTIPLE, 最多 32 个 content / file 来源. URI 以只读方式打开, 原始 Intent 中的安装选项和其他组件不参与执行. 只转发来源 URI 的 ClipData 与读取 / 写入授权标志, 不转发任意嵌套 Intent.
- 可重试的外部来源重新创建独立会话, 保留该项用户选择的选项; 前次已公布结果不会被改写. 重试需要来源仍存在且 URI 访问有效. 宿主 PFD 的所有权在会话结束后已释放, 界面明确提示由调用方重新发起, 不持有无限期描述符.
- 仅在系统确认安装成功后尽力删除所选外部来源. 提供方拒绝删除时保留安装成功, 返回 sourceDeleted=false 与说明; UI 显示来源未删除. 失败的来源保留. keepSourceOnFailure 的设置页仍属 P5, 不据此勾选 P2.3 的整个来源策略条目.
- Binder, 外部入口和单项重试共享四个安装名额. 关闭, 取消, 超时与终态分别处理, 实际来源清理结束后释放运行资源. 安装通知发布和移除串行化, 避免完成后的延迟通知重新出现.
- 外部来源在各项 prepare 时懒打开, 单项不可读不错误归属前一项, continueOnError=false 时不读取后续来源. 独立 deadline 主动取消 ContentResolver 的 CancellationSignal; provider 取消在专用线程执行, 不堵塞界面或错误中断复用的安装线程.
- SavedStateHandle 保存随机会话 token, 旋转恢复同一进程中的会话和确认草稿, 不重新执行安装. 进程已丢失时显示中断, 不猜测安装结果或自动再次提交. 跨进程持久化恢复仍未实现, P3.1 对应整项保留未完成.

外观与通知的独立记录分别见 [p3-appearance-evidence.md](p3-appearance-evidence.md) 和 [p3-notification-evidence.md](p3-notification-evidence.md).

## 必要的平台适配

路线图最初提出 singleTask 按 session id 区分任务. Android 文档任务以组件和 Intent data 匹配, NEW_DOCUMENT / intoExisting 要求 launchMode=standard. 因此实际使用 standard + documentLaunchMode=intoExisting + 私有随机 token URI, 同 token 复用同一任务, 不同 token 独立. 仍保留 excludeFromRecents. 此调整是实现原有会话隔离目标的必要适配, 不是新增范围. 依据: [Android Recents screen](https://developer.android.com/guide/components/activities/recents).

Android 14+ 的前台宿主必须显式允许绑定的安装插件启动确认界面. 宿主 AidlPluginHost 新选项默认 false, 只有 InstallerPluginHost 开启; 插件在直接启动未附着时提供直接 Activity PendingIntent 通知. 后台请求不承诺自动弹出界面. 依据: [Android background activity starts](https://developer.android.com/guide/components/activities/secure-bal).

系统安装确认的 Activity result 不作为最终安装结果. API 24 实测同意安装后仍可返回 RESULT_CANCELED; 以 PackageInstaller 最终广播判定成功或拒绝. 两种 Activity result 都只表示确认界面返回, 拒绝由 STATUS_FAILURE_ABORTED 或明确的未知来源权限拒绝判定. 不据 Activity result 重建安装会话.

系统确认与未知来源设置使用非导出桥接 Activity, 服务端保留真实 Intent, 通知只携带随机 token. 任务取消时结束自己启动的子 Activity; 只有桥接 Activity 自己是 task root 时才 finishAndRemoveTask, 防止关闭调用者的任务.

## 构建与纯逻辑验证

- 插件 testDebugUnitTest: 180 项, 0 失败 / 0 错误 / 0 跳过. 包含 UI 选项, 分包依赖复验, 用户选择, 逐项不可变结果, 来源删除回调, 确认状态, 通知状态与发布竞态等.
- 插件 assembleDebug / assembleDebugAndroidTest / assembleRelease / lintDebug / lintRelease 全部成功. 混淆 release 保持单 APK, native page alignment 检查确认无原生库. 本轮构建参数关闭 build number / build time 自动修改.
- 最终 VERSION_BUILD=24 再次运行上述六个任务通过; 两种 lint 均 0 错误, debug 20 / release 22 个警告. 十语言 Markdown 与 15 个图标资源的生成一致性检查通过.
- 宿主 assembleAppDebug 与 testAppDebugUnitTest 成功. JVM 共 3204 项, 6 项既有跳过, 无失败. 初次全量测试暴露向导目录计数仍断言 45, 已同步 P1 增加安装插件后的 46, 并增加安装插件身份 / 分类 / 非 standard 与 minimal 套餐断言.
- 十语言 JSON 为文案源, README / 插件说明 / changelog 由生成器产生; 宿主只调用自身生成器的纯渲染函数更新发行历史, 不刷新在线元数据.

## 设备验证

设备全部显式指定 serial. 未操作用户已有应用; 真实安装夹具无代码, 无显式权限和应用入口. 测试在操作前检查所有用户的安装记录和保留数据, 结束后卸载自己创建的夹具. 未修改真机的通知许可或未知来源许可.

| 设备 | 环境 | 已通过范围 |
| --- | --- | --- |
| AVD API 24 / x86 | none 与已授权 Shizuku ADB uid 2000 | 系统新装, 外部来源删除 / 拒绝删除, 批量失败后继续与单项重试, 对话框重建, 系统 / 卸载桥接, 外观与几何, 通知 |
| Xiaomi 23046RP50C / API 35 / arm64-v8a | Shizuku ADB uid 2000, 用户 ROM | 真实宿主 UID 前台安装, 系统设置拒绝与任务清理, 2 GiB 真实 xapk 后台安装, 无通知权限降级 |
| Sony G8441 / API 28 / arm64-v8a | Magisk / libsu Root | Binder dialog 真实安装 / 更新 / 卸载, 拒绝确认, 安装 UI 与系统桥接 |

忽略目录 build 中保留本次原始运行日志. 它们不作为代码入库; 下表列明最终成功运行和有意跳过, 不把首轮失败记录为整组成功.

| 日志 | 结果 | 说明 |
| --- | --- | --- |
| p3-api24-ui-final.log | 47 通过, 无跳过 | InstallDialog 8, ExternalInstall 7, UserAction 10, UserActionNotification 5, UninstallConfirmation 1, UninstallDialog 4, geometry 5, appearance 4, notification intent 3 |
| p3-api28-root-final.log | 27 通过, 无跳过 | 真实 Root Binder dialog 与确认 / 卸载桥接 |
| p3-api35-final-bridges.log | 5 通过, 1 跳过 | 实际未知来源设置拒绝, 确认任务清理, 特权卸载确认, 正式入口解析; 真机未 opt-in 修改默认安装器 |
| p3-api24-foreground-entry-final.log | 5 通过, 1 跳过 | 短包真实后台 FGS 与可见通知, task 清理, 正式默认安装器设定 / 清除; 未传 largeFixturePath, 大包项跳过 |
| p3-api24-publications-final.log | 21 通过, 无跳过 | 最终通知发布锁修复后复测契约, 外部来源, 通知桥接与短包后台写入 |
| p3-api35-publications-final.log | 14 通过, 无跳过 | 最终通知发布锁修复后复测契约, 通知桥接, task / 实际未知来源取消及 2 GiB 写入 |
| p3-api24-external-complete.log | 28 通过, 无跳过 | 最终 ExternalInstall 11, InstallDialog 9, SourceFormats 8; 真实 none 来源安装与删除, provider query / open 主动超时, 取消后线程复用, 不可读中项继续 / 单项重试, 失败即停, 接受确认后的并发取消 |
| p3-api35-external-complete.log | 14 通过, 无跳过 | 四项新增外部来源回归, InstallDialog 9, 真实宿主 UID 安装; 最终 case 的原插件开关为 false 且已恢复 |

正式入口解析测试覆盖 7 种 MIME x VIEW / INSTALL_PACKAGE x content / file, zip / octet-stream 与六种扩展名的大写 / 小写, 单项与多项分享. 宿主 5299 已不在 APK 处理列表. API 24 AVD 仅在没有任何既有 APK 默认处理器的前提下, 经 Shizuku 设置正式 ExternalInstallActivity 的四个 APK filter, 验证后只清除本插件默认项. 因此可以声明 default-installer 与外部来源 delete-source 能力; 专用设置 UI 仍属 P5.

## 真实宿主 UID 联调

宿主提交 1e6d8d09eb 交付 Android 14+ 绑定标志与 debug-only 测试入口. DUMP 权限保护的 installer-integration 路由只接受固定的无代码夹具, 调用正式 PackageInstallRouter.installAndAwait. 插件生产服务的包名 / 签名 / UID 校验未注入替代身份.

API 35 实测 UID=10890, targetSdk=37, 前台 importance=100, 启动时有窗口焦点. 插件出现自己的确认界面, 点击固定夹具的确认后返回 installerSucceeded=true. 宿主原有插件开关为 false, 完成后 installerPreferenceRestored=true. 后续取消指令只匹配同一个随机测试 case.

宿主探针以每 case 唯一 data URI 和 NEW_DOCUMENT 创建自己的任务, 重复运行不会复用旧 case. 宿主 53faf617a7 让匹配的取消指令关闭该探针的 Activity, 仅当它是 task root 时移除自己的任务. 最终 case `p3-installer-1cb2ba15-9589-483d-9ed1-769b4c38b97f` 在 task 2931 / host PID 31045 再次安装成功, 开关恢复为 false.

该设备宿主已有 overlayPermission=true, 因此此证据证明生产前台调用链成功, 不单独声称隔离证明 BIND_ALLOW_ACTIVITY_STARTS 是唯一放行因素. 测试尚未逐一点击文件管理器安装按钮, 插件中心 URL 安装与打包后安装的真实 UI, P2.6 / P1.4 三处入口矩阵仍未完成.

## 2 GiB 后台安装

tools/build-large-install-fixture.ps1 生成两枚带各 1 GiB 资产的签名 APK, 再组成 STORED xapk. targetSdk=35; 无代码, 无权限, 无应用入口. 测试验证实际 APK 和最终已安装 base / split 大小, 拒绝用容器填充或小包代替大包. 密钥与产物留在忽略目录.

- xapk: 2,147,501,419 字节; SHA-256 `f7e75b0161e3ad633d2457ad5426ea253c2d793e333e219b9ab5a5d8dc243ec8`.
- 两枚 APK 总量与实际写入: 2,147,500,874 字节.
- 最终 API 35 运行: PID 16607 未变, 44 个 FGS 样本, 真实 HOME 后 Activity STOPPED / 无焦点, 后台 11,556 ms, 安装操作 28,938 ms, 成功后卸载清理.
- 该真机保持 POST_NOTIFICATIONS 拒绝与通知栏禁用, 安装未被阻塞. API 24 的独立短包实装记录了可见进度 [0, 100], 8 个 FGS 样本, 后台 1,535 ms; 其首字节处 1,200 ms 等待只为观测, 不作为大包证据.

P3.4 的完整测试项要求同一次大包运行中通知可见并更新. 上述两次运行不合并冒充该矩阵已经完成, 因此该复合测试项仍保留未勾选. dataSync 类型已在 API 35 实际启动与持续写入成功; 实现处理 onTimeout 并先结束服务, 未主动耗尽系统累计时长预算.

## 首轮失败与修复记录

- API 24 系统确认同意后返回 RESULT_CANCELED, 已改以最终安装广播为准, 真实 none 安装与删除 / 重试路径复测通过.
- 宽屏几何测试的 24 行短内容实际容得下, 不应断言必须滚动. 已改用 80 行长内容, 另加短内容自然高度用例, 保持按钮 / 边界断言. 合成 IME 在 API 24 的测量没有体现请求高度, 不宣称完成真实键盘验收.
- Activity recreate 采样曾取到销毁中的旧实例, 已等候同一 token 的新 resumed Activity, 未放宽草稿与一次确认断言.
- Android 15 对 targetSdk 28 小夹具合成 POST_NOTIFICATIONS 请求项. 验证改为检查每枚源 APK 的显式权限为空, 保留 framework 无代码 / 无组件断言; targetSdk 35 的大包未出现该兼容注入.
- API 24 的 UI automation 缓存根节点曾误报仍在前台. logcat 确认 HOME / Launcher / Activity stopped; 最终以真实生命周期 STOPPED 与无窗口焦点共同判定, 复测成功.
- API 35 的未知来源设置页不暴露预期应用名称节点. 现使用属于插件桥接 Activity 的 AppTask, 确认顶层为 Settings, 只结束自己拥有的设置任务; 返回 USER_CANCELLED, 权限状态与源文件未变.
- 通知完成事件与关闭可能交错, 已统一发布 / 移除的锁, 增加 6 项纯逻辑竞态测试并完成两设备实际复验.
- 外部来源先前整批预打开会把不可读中项错误归属首项, 且 provider 阻塞不会主动到达 deadline 检查. 改为逐项懒打开和主动 CancellationSignal 取消后, API 24 / 35 验证实际超时, 用户取消, 线程复用, 逐项错误与 stopOnError.
- 确认已接受但 worker 尚未清空 prompt 时, 第二次点击取消曾被吞掉. 现原子区分拒绝确认和取消执行, 两线程竞争只调用一次取消回调; 确定性设备测试通过.
- 新 provider 夹具最初只重载 openFile, 而只读 content 请求会经 typed asset 路径. 已显式把 openTypedAssetFile / openAssetFile 的 signal 传到阻塞点, 并通过 provider 自身状态确认进入等待后才检查 deadline, 没有放宽时间上限. 损坏 .apk 的预期为 INVALID_PACKAGE, 不要求产生已成功解析的元数据.
- 首次重跑宿主探针复用了旧任务, 没有执行新 case. 唯一文档任务和只关闭本 case 的清理修正后, 正式宿主路径连续回归成功. 这些修复前失败的日志不计为整组通过.

## 保留未完成的原有条目

P3.1 跨进程恢复与真实键盘 / API 35 全部几何边界, P3.2 系统文件管理器和浏览器下载列表各打开 APK / XAPK, P3.3 真机首次授予未知来源后继续安装的实际界面流程, P3.4 同一次 2 GiB 安装的可见通知更新, 以及 P2 的安装标志 / 多用户完整矩阵与宿主三处入口. 未启用 P4 脚本 API 或 P5 首页 / 设置页, 未提前宣称 1.0.0 可发布.

## 清理与提交范围

结束时再次枚举三台测试设备的全部用户, 没有残留自建安装夹具或其保留数据. 已删除真机上的 2 GiB 来源和临时中转副本, 宿主探针 APK 与四份已记入日志的探针 JSON; 仅关闭本轮启动的 AVD_API_24. 真机通知仍为 ignore, 未知来源仍为 default, 宿主插件开关仍由测试恢复为原值.

实现, 测试, 十语言文案与证据按逻辑作本地提交. 遵循维护者 2026-10-01 的最新指示, 插件当前暂不推送 GitHub 远端; 宿主亦未推送. AGENTS.md 与 ROADMAP.md Q8 已记录该约束.
