# P5.2 默认安装器真实页面与文件管理器验收

日期: 2026-10-01. 对应原 P5.2 测试条目. 不以底层 setDefault 调用或合成 APK Intent 代替实际页面和文件管理器操作.

## 已完成范围

`DefaultInstallerUiDeviceTest` 通过实际 DefaultInstallerActivity 完成授权方式选择后取消, 再确认锁定, 最后确认取消默认. 每个阶段检查公开 PackageManager 对 VIEW/INSTALL_PACKAGE x content/file 四种组合的解析, 全部 preferred/last-chosen 记录及插件偏好.

锁定后暂时保持测试, 由外部驱动实际打开系统 DocumentsUI 中的自建 `fixture.apk`. 必须无需安装器选择器而直达本插件并显示固定夹具包名, 然后取消并完成, 才记录文件管理器成功. 测试本身收到 continue 文件不意味着该独立检查已通过, 必须结合窗口树和实际 Activity 数据核对.

| 设备 | 授权 | 页面锁定/解锁与实际 Files 直达 | 最终日志 |
| --- | --- | --- | --- |
| AVD API 24 / x86, emulator-5554 | Shizuku ADB | 1/1 通过, 21.577 秒, 四种解析与基线恢复均通过 | `build/p5-default-ui-api24-finger.log` |
| AVD API 31 / x86_64, emulator-5556 | Shizuku ADB | 1/1 通过, 83.519 秒, 四种解析与基线恢复均通过 | `build/p5-default-ui-api31-full.log` |
| Xiaomi 23046RP50C / API 35 / arm64-v8a, 968e9f18 | Shizuku ADB | 1/1 通过, 122.436 秒, 四种解析与基线恢复均通过 | `build/p5-default-ui-api35-full.log` |
| Sony XQ-DQ72 / API 33 / arm64-v8a, QV770340J7 | KernelSU Root | 1/1 通过, 28.445 秒, 保留已批准范围内的既有 last-chosen, 四种解析与基线恢复均通过 | `build/p5-default-ui-api33-visible-final.log` |

这些耗时包含外部操作者/驱动检查的等待, 不是默认设置接口的性能基准. Root 由维护者在安装插件验收包后明确在 KernelSU 授权. G8441 / BH900ASK9E 的既有 APK 选择记录保持, 未为完成本轮矩阵清除.

## 持久恢复与范围保护

任何写入之前, 先将公开 preferred 查询, `dumpsys package preferred-xml --full`, 四种解析和插件 SharedPreferences 原子保存并 fsync. 私有计划为 `files/p5-default-installer/restore-plan.json`, external-files 只作可读副本. pending 计划禁止下一轮覆盖; restore-only 可在崩溃后恢复, 不启动 Activity 或特权服务.

普通测试遇到任何已有 APK 首选/最近使用或本插件已有首选项即跳过, 不自动清除. 恢复只能清理可证明由本次测试添加的本插件四个 APK filter, 不清其它包或覆写用户偏好. 解锁后再次对全局默认项和偏好逐项比对. 本轮所有已开始的默认测试计划均已 restored.

文件管理器 hold 模式还要求本地交互设置原本就是 dialog, 否则在写默认项前跳过, 防止未来验收在用户选择 silent 时自动安装夹具. 此测试前置保护没有改用户设置, 最终编译与文档守卫见 `build/p5-final-probe-guard.log`.

普通参数:

```text
-e class io.github.supermonster003.autojs6.plugin.three.setup.installer.DefaultInstallerUiDeviceTest
-e defaultUiAuthorizer shizuku
-e defaultUiHoldMillis 180000
```

Root 改为 root. 真实 Files 操作由当前测试自己的 UiAutomation 提供有界窗口树, 避免连接第二个 UiAutomation. API 24 另启用受限点击桥, 仅可操作已知 DocumentsUI 包内的固定夹具/自有目录名称, 不支持插件 Install 按钮或任意文本/安装 Intent.

## API 24 的实际适配与失败记录

1. 第一次默认快照未带 `--full`, 新系统 XML 省略 always 字段, 测试在任何默认项写入前明确失败. 修正为完整 XML, 保留严格校验, 不猜字段值.
2. API 24 系统 Files 不支持新版 document 目录 URI 作为启动入口, 在 DocumentsContract.getRootId 抛 Invalid URI. 此为实际系统 Files 的启动失败, 记录在 `build/p6-device-preflight/api24-documents-crash.log`; 该次默认测试 finally 已恢复基线, 不能计为完整成功.
3. API 24 Files 实际展示已有公共 SD 卷 0000-0000, 因而将同一自建夹具放到该卷的独立目录并从 root URI 浏览. 没有改模拟器图形, 存储配置或用户文件.
4. 旧 Files 的目录行没有可点击的无障碍祖先, 常规 adb tap 未触发打开. 在限定节点中心使用 SOURCE_TOUCHSCREEN / TOOL_TYPE_FINGER 的 DOWN/UP 后, 目录和 APK 均实际打开. 事件接受与导航成功分别核对, 最终在真实插件确认页看到固定夹具包名.

中间 UI-only 的成功与独立 Files 驱动失败均保留日志, 不拼接为最终矩阵. 表中 API 24 最后一轮同时包含页面和 Files 证据.

## QV770340J7 既有 InstallerX 记录

早期简化 shell 预检只搜索 type, 漏掉新平台的 staticType, 因此不能把其空结果当作有效的无默认基线. 正式测试同时使用公开 API 与完整 XML, 正确发现 InstallerX 记录并保护性跳过. 安装新插件前的完整 XML 中曾有 always=true 条目, 后续观察为三条 always=false; 没有完整跟踪每条状态变化的时刻, 不推断全部变化均由某一次调用造成.

维护者明确允许清除这三条 APK 打开记录后, 系统设置页只提供网页链接管理, 没有旧式 APK 清除按钮. 显式测试分支严格绑定本设备/固定目标包/已核对的三条记录, 临时采用 SET_PREFERRED_APPLICATIONS 调用公开系统接口, 随后立即放弃该身份. 没有清应用数据, 没有修改网页链接设置, 没有 Root/隐藏 Binder 清理回退.

清除尝试后仍剩两条无 scheme 的 APK last-chosen, 因而原清除测试如实失败, 不声称三条全清. 私有审计 `approved-installerx-clear-9808cd47-4af6-4eca-a2ac-43f0a9a00d93.json` 保留 before/after. 本地 Android 33 SDK 的 Settings.clearPackagePreferredActivities 实现只删除指定包的 always=true 项; 两条 false 历史留下符合该行为. 第三条消失的具体原因不能只凭这些快照归因于公开 clear.

之后的只读核验通过: 原审计和当前剩余记录精确对应, 四种解析都是系统 Resolver, 其它首选项/插件偏好/域链接状态未变. 对本次已批准操作使用显式, 固定 audit 的保留例外, 把两条 InstallerX 历史和原有的非 APK 专用通配 last-chosen 全部纳入基线, 不清除它们. 通配记录必须有对应 shell always=false 证据, 不能把 hasDataType 的通配匹配误当成真正 APK 专用首选项. 普通测试的严格跳过规则不变, 实际 always=true 或新的/未核实记录不能通过例外.

最终 Root 测试只设置和撤销本插件的四项, 全部保留记录逐项相等. 实际 Files 直达确认后取消, 不把保留历史说成清除, 不把 Root 成功推广为所有 OEM 都支持默认锁定.

## 夹具, 历史与服务收尾

实际操作的夹具是仓库固定无代码 fixture-v1.apk. 外部目录均使用独立随机前缀 `ThreeSetupDefaultProbe-c8f366495cd3`; 已逐一核对源摘要后删除四台设备的自有文件/空目录, API 24 的公共 SD 及原 emulated 副本均已清理. 未执行夹具安装, 未安装或卸载用户应用.

外部确认产生的取消历史按精确 runId/token 逐条删除, 先验证固定夹具包名/external/cancelled及测试时间, 再通过 Store.remove 等持久回调. 对用户其它历史和默认项/偏好再次比对. 一次 Root 确认在测试 teardown 后 77 ms 才完成异步历史写入, 初次严格时间检查拒绝删除; 后续仅给结束/更新时间增加 1000 ms 有界尾部, 开始时间仍严格属于原 run. 已按同一 token 成功清理. 没有全局清空.

| 设备 | 最终 runId | 已清理的外部 token |
| --- | --- | --- |
| API 24 | 43908cc1-b1b2-4bfb-a7a9-12ff98016cbd | e30fa3ee-527f-477d-9240-9d377b9960ff |
| API 31 | 6ea44f1d-8770-4ef4-8a83-11a149855cba | 99e05134-3c64-4ed1-a74e-6fb6ad797126 |
| API 35 | 86d96626-5207-4986-a302-3f01606d6c1d | e9b71e0a-dcd6-4303-8b21-81788e4d5cf1 |
| Root API 33 | e24691ea-dbd1-4884-8803-451454c334fd | f357b1df-e674-4714-bd7a-1ef1ac7a7515 |

Root 首次驱动在确认页动画结束前读取了不可见的 Cancel 节点, 独立驱动拒绝点击; 当次页面锁/解锁虽成功, 不计完整 Files 验收. 该次 token `01510e1a-c476-4e9c-909c-bfb53bb44526` 也已独立清理, 然后等待 Cancel 真正可见启用的完整重跑通过.

原本停止的 API 24/31 Shizuku 仅为本轮临时启动, 已按记录的自有 server PID 停止并核验无 server 进程. 用户启动的模拟器保持运行. API 35 原有 Shizuku 和用户授予的 KernelSU 权限保留.

原 P5.2 测试条目据四台设备的实际证据关闭. 本轮没有补齐 P6 的全部进程死亡, 全设备兼容或性能测量矩阵; 不进入发布阶段.

## 最终构建与设备交付

最终 build 38 的 261 JVM, Debug/androidTest/混淆 Release, 两种 lint 和摘要 APK 收集任务通过, `build/p6-final-build38-complete.log`, 55 秒. Debug 0 errors / 22 warnings, Release 0 errors / 23 warnings. 两次早期文档标点检查失败均为新证据文本的中文顿号, 已统一为规定的 ASCII 标点; 最终全量零失败/跳过. Markdown 十语言 36 产物与 15 个图标资源检查通过.

签名产物为 `releases/autojs6-plugin-three-setup-installer-v1.0.0-aef12e30.apk`, 1,886,251 字节, CRC32 `aef12e30`, SHA-256 `96372f691e4c329170a29c2a9cb1fe23edf6135984725b4f38d4a510a7eb9a6d`. apksigner v2 校验通过, release Manifest 无 debug 标志和 spike 入口, 没有原生库.

QV710AF65F 与 QV770340J7 均已覆盖为 build 38 Release, 替换前无插件进行中的安装, 替换前后系统默认项完整 XML 相等. Root 设备上还存在 20 条旧的系统 staged session, 属于 UID 1000 且时间早于本轮, 不属于插件或本次夹具; 它们逐字保持, 没有为了收尾删除系统记录. 四台默认验收设备的私有恢复计划已归档到本地忽略目录并再次核验 restored.
