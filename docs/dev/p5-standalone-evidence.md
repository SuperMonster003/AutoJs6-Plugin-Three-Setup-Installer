# P5 独立应用实现与设备证据

日期: 2026-10-01. 对应原有 P5.0-P5.4, 没有增加, 分拆或丢弃路线图小节. 本轮只作本地提交, 没有推送仓库或发布发行版.

## 实现范围

| 原条目 | 已交付行为 |
| --- | --- |
| P5.0 首页 | `HomeActivity` 的授权与默认安装器状态卡, 任务实时进度, 私有安装历史, SAF 多选 FAB, 已安装应用与设置菜单. 任务行按 token 复用, 进度更新不会反复创建控件或移动焦点. |
| P5.0 历史 | `InstallHistoryStore` 有界原子 JSON, 最近 200 条, 主线程观察回调, 逐项终态与旧/新版本, 来源与授权方式, 失败摘要. 单条删除/清空需要确认, epoch 与墓碑拒绝晚写. 新进程将未完成项标为 cancelled, 不重放任务或保存可复用安装授权. 错误摘要中的路径和 URI 被脱敏. |
| P5.0 队列与分享 | `InstallQueue` 复用既有批量引擎, 失败继续或取消剩余项, 进程内存活; Home / 外部打开 / SEND / SEND_MULTIPLE 进入同一展示与历史. 分享方传来的选项或来源身份不能覆盖插件设置. 单项 SEND_MULTIPLE 仍保留 batch 语义. |
| P5.0 已安装应用 | 当前用户应用异步加载, 标签/包名搜索, 名称/安装时间/更新时间排序, 系统应用开关, 按需图标与 4 MiB 缓存. 展开后可打开, 看系统信息或确认卸载; 特权与 none 路径复用 P2 卸载引擎. |
| P5.1 设置 | 语言, 夜间, 主题色, 启动器图标; 授权顺序和启用状态, 默认交互与安装选项, 进度通知偏好, 默认安装器/关于/发行历史/手动更新入口. 普通选择先确认后保存, 草稿取消不写入, 重建关闭旧对话框. 设置入口 action 受 PLUGIN 权限保护并转发到同一页面. |
| P5.2 默认安装器 | 首页与设置复用 `DefaultInstallerController` 和 `DefaultInstallerLock.get(context)`, 显示当前处理者, 特权设置/清除, 无特权时转系统详情页并说明步骤. API 24/28 已有竞争默认项时展示需要手动清除的真实结果. |
| P5.3 关于与版本 | 关于透明图标容器, 版本, 作者, 仓库, 本地许可证与第三方声明; 十语言离线发行历史. 手动更新仅访问固定 GitHub Releases API, 10 秒连接/读取超时, 256 KiB 响应上限, 取消, 稳定版本校验, 12 小时间隔与忽略版本. 无后台检查或自动下载安装. |
| P5.4 启动器 | 四个独立 alias 指向 Home, Auto 默认; DONT_KILL_APP 切换, 可变快捷方式归属迁移, 失败回滚, MY_PACKAGE_REPLACED 修复. 继续使用既有图标生成器和透明应用内图案. |

语言, 夜间和主题色默认跟随 AutoJs6; 启动器 Auto 根据系统资源配置选择亮暗, 不依赖宿主主题. 本地 Home / 外部安装默认 `dialog`, 保留打开安装包时的信息/确认界面; 用户明确保存的 auto / silent 仍生效. 脚本默认 auto 语义不变. 关闭可选进度通知不阻止 Android 要求的前台服务通知和必要的用户操作/结果提示.

本轮新增 161 个字符串 key, 默认 English 与显式 English 一致, 共 11 个 Android values 目录. 十语言 README, 插件说明与 changelog 已同步. 未增加 Gradle 依赖; INTERNET 仅用于上述手动更新检查.

## 构建与静态检查

- 插件 JVM: 243 项, 0 failures / 0 errors / 0 skipped; 最后源码检查 `build/p5-source-final-check.log`, Debug 和 androidTest APK 构建通过.
- Debug / 混淆 Release / androidTest 与两种 lint: `build/p5-release-lint-build.log`, 1 分 30 秒. Debug 0 errors / 22 warnings, Release 0 errors / 23 warnings. 最终版本打包结果见本文末尾.
- Markdown 生成器与图标确定性检查通过. 私有签名信息, 构建输出, 测试截图和设备日志不入库.
- 宿主 `testAppDebugUnitTest`: 3273 项, 6 项既有 skipped, 0 failures / errors. `build/p5-host-build.log` 为本次源码检查; `build/p5-host-final-build.log` 为最终 build 5302 的 appDebug / androidTest 打包, 53 秒通过. 本轮没有将此前未完成的宿主全量 lint 重新计为通过.

## 设备验收

| 设备与范围 | 实际结果与本地日志 |
| --- | --- |
| API 24 AVD `emulator-5554`, x86 | Home, HistoryQueue, InstalledApps, Settings, LauncherIconResource, LauncherIconSelection, PluginContract 七类合计 20/20 通过, 无跳过, 9.095 秒. `build/p5-device-api24.log`. |
| Xiaomi 23046RP50C `968e9f18`, API 35, arm64-v8a | 同组 20/20 通过, 无跳过, 8.112 秒. `build/p5-device-api35.log`. |
| API 24 历史持久化 | 单独新增 1/1 通过, 0.762 秒, `build/p5-history-cap-device.log`. 私有隔离 Store 连续写入 7 x 32 = 224 条, 验证内存/磁盘/重新加载均只留最新 200 条, 清空后旧 ticket 晚写不能复活记录, 回调在主线程. 不触碰用户历史. |
| API 35 Shizuku 卸载界面 | `InstalledAppsUninstallDeviceTest`, 1/1 通过, 3.322 秒, `build/p5-uninstall-ui-api35.log`. 自建固定夹具安装后, 在真实列表搜索/展开/点击卸载/插件确认, keepData=false, 验证包消失与成功 UI. 只清理本次夹具/源文件/历史, 未操作用户应用. |
| API 24 / API 35 首页最终状态 | `HomeActivityDeviceTest` 各 3/3 通过, 1.965 / 2.349 秒, `build/p5-home-final-api24.log` / `build/p5-home-final-api35.log`. 新增七种 Root/Shizuku 状态渲染断言, 不实际申请权限. 修正无 su 时 Root 显示运行中的问题, 不改变公共 AuthorizerState.running 的含义. |

基础组合覆盖: 队列不可读项继续与取消, 关闭展示后晚到真实成功仍记入历史, 来源身份与分享 grants; 应用搜索/排序/系统开关与重建; 设置先选后确定/取消/非法草稿; 十语言历史与离线法务文档; 四种图标在真实 APK 中的资源类型与 night/undefined 配置, 实际 alias 唯一性, PID 保持和可变快捷方式归属.

## 真实窗口, RTL, 大字号和 IME

`StandaloneAppearanceDeviceTest` 使用实际 Activity 和现有系统键盘. 覆盖 Home, Settings, DefaultInstaller, InstalledApps, About, ReleaseHistory 六页, 量测顶部和滚到底部后的文字与固定控件, 检查标题字号/省略/方向/表面色/insets, 并通过真实触摸验证首页菜单, 设置末行和主题色取消按钮. 主题色输入测试只修改未保存草稿.

| 运行 | 实际结论 |
| --- | --- |
| API 35 英语 / 浅色 / 字号 1 / 默认 Navajo 色, 阿拉伯语 / 深色 / 字号 2 / 360 dp 窄内容 | 2/2 通过, 无跳过, 39.187 秒. `build/p5-appearance-api35-awake.log`; 包含真实 HEX 输入键盘, 确认/取消按钮均在 IME 上方, 取消后偏好未变. |
| API 24 英语 / 浅色 / 字号 1 | 1/1 通过, 无跳过, 18.734 秒. `build/p5-appearance-api24-light.log`; 六页顶部/底部与真实键盘均通过. |

API 35 原始窗口为 1800 x 2880, density=2.5; 窄内容宽度 900 px. 测试只限制内容根布局为 360 dp, 不改变真实 Activity Window, 因而不将本次记为系统多窗口验收. 字号/语言配置仅应用到 Activity 的独立 Resources; 不修改全局字号, 夜间, 超时, 亮度或输入法.

屏幕已变暗时, 第一次 DOWN 可能只用于恢复亮度, 随后的 UP 被系统拒绝. 设备日志确认 previousDisplayPolicy 2 -> 3 与 automatic [dim] -> automatic. 测试改用明确 KEYCODE_WAKEUP, 确认设备已解锁, 并在每个被验收窗口临时设置 FLAG_KEEP_SCREEN_ON, 最后恢复原窗口标志; 没有修改产品窗口行为. 另修正测试采样忽略 GONE 祖先与 selectable 文档抢回顶部焦点的问题, 保留实际点击和末行可见断言.

截图通过 adb pull 取回 `build/p5-ui-review/api35-final/p5-standalone-appearance/` 和 `build/p5-ui-review/api24-light/p5-standalone-appearance/`. 已人工复核 Home, Settings RTL, 主题色 IME 和 About 的图案容器. 目录保留之前失败诊断图, 只以上表日志列出的样本为最终验收证据.

### API 24 GPU 故障和恢复

早期 API 24 大字号/深色运行触发模拟器 libhwui RenderThread 的 SIGABRT, CanvasContext.cpp:505; 随后 Launcher 和 system_server 也发生相同原生故障. `build/p5-api24-native-crash.log` 保留现场. 该次不是成功用例, 不归因为已证明的插件 Java 异常, 也未通过关闭产品硬件加速或重启用户模拟器掩盖问题. 本轮未重复该高压力组合, API 24 大字号/深色专项仍待稳定图形环境复验.

原测试只有内存偏好快照, 进程退出使 finally 未执行. 维护者确认原语言/夜间/主题色全部跟随 AutoJs6 后, 已恢复为 language=host / darkMode=host / 无本地 color, 并读取文件确认. 后续测试新增 `StandaloneAppearanceRecovery`: 在任何外观变更前将完整带类型偏好, 四 alias 状态和可变快捷方式保存到私有 AtomicFile 并同步落盘, 保留外部 files 只读副本; pending 计划阻止新测试覆盖基线. finally 从计划恢复并逐项比对, 完成后标记 restored. 遭遇进程退出时可运行原测试的 `standaloneAppearanceRestoreOnly=true` 模式, 无需启动页面或键盘; 没有计划则报错, 不猜默认值.

最终核对: API 24 run `eb7b08ee-cb3b-4d32-a825-66b22b3faea5`, API 35 run `a3b5a85e-7095-40f0-bfde-397731a3a637` 的计划均为 restored. API 24 三项仍为 host/host/无本地 color. 用户启动的两台模拟器保留运行.

## 宿主改动与 AAR

宿主本地提交 `493b229f4c` (build 5301) 将 APK Inspector 从安装包信息主对话框收进右上角更多菜单, 位于 More information 之前; 点击单独展示完整详情. 异步 Inspector 结果到达后更新选项列表, 已打开的菜单使用同一显示/点击快照, 避免插入条目造成旧索引误触. 主对话框关闭时清理子对话框.

`InfoDialogInstrumentationTest` 6/6 通过, 18.169 秒, `AutoJs6/build/p5-host-menu-verified.log`; 包含异步插入时旧菜单回调对应关系. 首轮测试因弹出动画未稳定失败, 修正测试的可见性与稳定点击后完整复测, 未删除断言.

宿主本地提交 `bf102da416` (build 5302) 增加可选 JSON `sourceOrigin`, 宿主与脚本分别标记 host / script. V1 AIDL, 协议版本及最低基础宿主 build 5299 保持; 旧请求缺失该字段按 host 兼容. 没有新增 JS 签名, 本轮无需再修改 P4 已同步的 d.ts / Ace / 四套文档仓库.

插件自带 `installer-api.aar` 为该宿主 release 模块的 31,086 字节产物, SHA-256 `90a337b1cd645270e41368af0d6dc28a73509e505d4b049fdbeececa0997b441`; 锁文件, libs/README 与第三方声明同步. 另外两个自带 AAR 不变, 构建不引用兄弟仓库路径.

P5 首页, 设置, 默认安装器控制器, 关于和图标入口互相引用并共用资源/Manifest; 本轮作为一个可构建的独立应用功能提交, 避免将各页面导航临时拆成未实现入口. P3 授权验收和 P5 显示恢复测试/路线图证据分别提交. 此提交分组只处理实现依赖, 不改变原路线图小节或测试门槛.

## MT Manager 反馈和仍需完成的验收

QV710AF65F / Sony XQ-AT72 / API 31 的原问题来自设备仍安装 build 5, 外部打开进入旧 `.spike.SpikeInstallerActivity`. 覆盖为 P4 build 30 Release 后, 维护者已明确确认 MT 打开 APK 出现插件安装信息/确认界面. 这只证明第三方管理器的 APK 确认入口, 不代表点击确认后的安装成功, 也不替代系统文件管理器和浏览器下载列表.

MT 的 APKS MIME `application/vnd.android.package-archives` 已加入本轮 VIEW / SEND 匹配与 SAF MIME 提示. APKS 与 XAPK 是不同容器; 未收到用户对新版 APKS 成功安装或 XAPK 的最终反馈. 不自动安装 `/storage/emulated/0/MT2/apks/` 中的用户应用, 不以启动器是否出现图标作为唯一成功标准.

最终已覆盖安装 build 33 Release 到 QV710AF65F. 更新前确认前台为 Xperia Launcher, 插件无活动 task/service, 平台 Active install sessions 为空; 未中断用户确认或安装. 覆盖后核对 versionCode=33, APKS MIME 的 VIEW / SEND 均匹配 ExternalInstallActivity, launcher 唯一自动入口可查询. `build/p5-qv-release33-deployment.json` 记录这一结果; 已请求维护者用原 MT 步骤验证 APKS 确认界面, 无需为入口检查安装应用.

仍保留原有验收边界:

- P5.2 Shizuku / Root 锁定和解锁后由系统文件管理器直接打开 APK, API 24 / 31 / 35 的完整组合未执行. 已有 P0/P2 底层调用证据不冒充该界面端到端矩阵.
- API 24 大字号/深色专项受 GPU 故障影响; 实际系统多窗口未测. 本轮 360 dp 内容约束不等同于系统多窗口.
- 更新策略/HTTP 拒绝/取消/上限经过 JVM 测试, 各语言历史经过设备加载; 未发布 GitHub release 或实际验证存在新发行版时的线上更新提示.
- P3.2 系统文件管理器与浏览器下载列表分别打开 APK/XAPK, P1.4 宿主三入口完整有/无插件组合, P2/P6 其余设备矩阵继续按原条目推进.

推荐手动反馈包含: 设备, 插件 build, 来源应用, 文件扩展名, 是否有插件确认页, 点击确认后的系统授权/扫描页面, 最终成功或错误码, 系统应用信息中的包名/版本. 如安装失败, 保留源文件即可, 无需反复等待启动器图标.

## 最终本地交付

`build/p5-final-build33.log`: build 33 的 243 JVM, Debug / androidTest / 混淆 Release, 两种 lint 与带摘要 APK 收集任务全部通过, 1 分 47 秒. Debug 0 errors / 22 warnings, Release 0 errors / 23 warnings. 另经 apksigner 校验 APK Signature Scheme v2 有效; Manifest versionName=1.0.0 / versionCode=33, 无 native library, 未带 debug 标志.

- 本地文件: `releases/autojs6-plugin-three-setup-installer-v1.0.0-74362257.apk`.
- 大小: 1,878,855 字节.
- CRC32: `74362257`, 与文件名一致, releases 中恰好一个 APK.
- SHA-256: `57831bd507c79eaf31d9fc5c70bda7c11b7e7e27842383c4ade8972dd17fd64c`.
- 本地提交: P5 功能 `33f9341`, P3.3 验收 `67b296f`, 最后显示恢复测试与本证据/路线图提交使用 build 33. APK 未提交, 未推送或创建远端发行版.

## MT APKS 必选分包勾选显示修复 (build 34)

维护者后续确认 QV710AF65F 的 build 33 已能通过 MT 的系统建议入口显示 APKS 安装信息/确认页, 同时报告 base.apk 不能取消但看起来未选中. 这补充了第三方文件管理器 APKS 确认入口的人工证据, 不等于完成安装, 也不替代系统文件管理器/浏览器 APK/XAPK 矩阵.

在该实际确认页只读检查到 base.apk 的 accessibility 状态为 checked=true / enabled=false, 其它可选分包为 checked=true / enabled=true. 原截图 `build/base-checkbox/qv-before.png` 显示灰色实心框没有可见勾选标记. 原因是 InstallerUiKit 给禁用的方框和勾选图标都使用 disabledText, 两者对比度为 1:1; InstallChoices 的必选约束及实际选中集合没有缺失 base.apk.

修复为禁用勾选图标选择对方框填充有足够对比度的中性色, 保持必选项已选中且不可取消, 不修改分包选择或安装语义. 十语言 changelog 与生成文档已同步.

- 回归测试先对 API 35 上旧版本执行, 精确失败于 `Required split checkmark disappears in dark=false`, 1 项失败; `build/base-checkbox/old-build-regression-api35.log` 证明新断言能捕获原缺陷.
- 修复后 API 24 / API 35 各 7/7 通过, 无跳过, 2.388 / 1.441 秒; 日志为 `build/base-checkbox/fixed-api24.log` / `fixed-api35.log`. 包括六项对话框几何/控件检查和一项真实确认页重建用例. 新断言检查浅色/深色禁用勾选标记对比度至少 3:1, 恢复启用后正常着色, 以及 base.apk 在重建前后始终已选中且禁用.
- JVM 243 项与 Debug/androidTest 构建通过, `build/base-checkbox/debug-build34.log`. 签名 Release 与两种 lint 在 39 秒内通过, Debug 0 errors / 22 warnings, Release 0 errors / 23 warnings, 见 `build/base-checkbox/release-build34.log`.
- build 34 产物为 `releases/autojs6-plugin-three-setup-installer-v1.0.0-62466b78.apk`, 1,879,779 字节, CRC32 `62466b78`, SHA-256 `3704eb55af43e68032f9b3bb70663ce035eb2fe33c56e971d095d27689e26596`, apksigner v2 校验通过. 收尾时 QV710AF65F 仍在旧确认页, 新包尚未覆盖该设备; 等页面退出后部署.
- 未自动确认用户 APKS 的安装. 已请求维护者取消当前确认页, 覆盖修复包前再次确认没有活动安装. build 33 的旧生成 APK 移至本地 `build/base-checkbox/build33-74362257.apk`, 不入 Git.

后续维护者反馈已通过 MT 完成微信 APKS 安装, 并提供成功状态/包名/版本和浏览器/Files 的额外测试. build 35 修复 Files 的 opaque content URI 匹配后已覆盖 QV710AF65F, 同时交付本节的勾选显示修复; 实际 XAPK/APKM 确认页和 base.apk 的可见勾选均已复核. 完整反馈, 复现过程与新产物见 `p3-external-entry-evidence.md`, 本节此前尚未覆盖的说明保留为 build 34 会话结束时的状态.
