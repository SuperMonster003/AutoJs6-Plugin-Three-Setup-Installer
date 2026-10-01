# P2.3 三授权矩阵补充与保留缺口

日期: 2026-10-01, Asia/Shanghai. 本轮沿用原 P2.3 的完整矩阵, 补充 API 35 Shizuku 完整安装选项和 API 33 none. API 24 的现有系统 su 不能由插件 UID 使用, 因此原三授权跨 API 条目仍未完成. 本文没有将不可用身份或未授权尝试算作通过, 没有替换原设备范围.

## 构建与设备基线

执行前重新读取 SDK, ABI, build type, 全部用户及保留包数据, 默认项 XML, 活动安装 session, Shizuku server 和安装许可. 三台设备开始时均为同一签名的 build 44 Release, 已安装 APK 的 SHA-256 与本地发行包逐字节一致:

`3ab3c7691da231a723b59988d6861f0091184d88f742942bf2b7b47225b63e9a`.

| 设备 / serial | API / ABI / build type | 插件 UID | 原活动 session 数 | 原 Shizuku server |
| --- | --- | ---: | ---: | --- |
| AVD / `emulator-5554` | 24 / x86 / userdebug | 10293 | 0 | 未运行 |
| Xiaomi 23046RP50C / `968e9f18` | 35 / arm64-v8a / user | 10290 | 6 | shell UID, PID 20102 |
| Sony XQ-DQ72 / `QV770340J7` | 33 / arm64-v8a / user | 10623 | 20 | root UID, PID 6403 |

Core 测试复用本轮开始时已构建的 build 44 Debug 和 androidTest, 两者的构建时间晚于 Core/Fixture 测试源码. Release, Debug 和测试 APK 的证书 SHA-256 均为 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`, apksigner 验证通过. 主包采用保留数据覆盖安装, 没有卸载主包或清除应用数据. 本轮没有更改生产安装引擎或 Core 测试源码, 也没有改动 AVD 的系统或 Root 环境.

原始基线和最终审计保存在忽略目录 `build/p2-matrix-completion/` 的 `before`, `debug`, `tested`, `restored` 子目录, 每台设备均有摘要和原始系统输出. 私有历史与偏好文件在覆盖 Debug 后立即读取摘要, 在测试后再次比对; 这部分证据是 Debug 前后精确不变, 不宣称 Release 沙箱下读取了不可访问的私有文件.

## 本轮实际通过

| 日志, 均相对于 `build/p2-matrix-completion/` | 身份 / 设备 | 通过 / 失败 / 跳过 | instrumentation 耗时 |
| --- | --- | --- | ---: |
| `core-shizuku-968e9f18.log` | Shizuku ADB / API 35 Xiaomi | 8 / 0 / 0 | 10.543 秒 |
| `core-none-QV770340J7.log` | none / API 33 Sony | 5 / 0 / 0 | 7.896 秒 |

API 35 的八项为 `testOnlyRequiresAnExplicitPrivilegedFlag`, `debuggableDowngradeRequiresTheFlag`, `lowTargetSdkBypassAndPre34Note`, `xapkInstallsBothBaseAndFeatureSplit`, `newInstallUpdateAndPrivilegedUninstall`, `releaseDowngradeRecordsTheActualRomPolicy`, `explicitInstallerAttributionIsRecorded`, `shellInstallerAttributionRecordsTheActualRomPolicy`. API 33 none 选择前五项, 没有把特权专有方法的保护性跳过包含在通过数中. 计数逐个检查带 `test` 字段的最终 instrumentation 状态, 不把测试内的 `sendStatus(0)` 证据消息当作额外通过.

结果的实际含义:

- API 35 Shizuku 完成固定无代码 release v1 新装, v2 更新及生产严格 `silent` 卸载. 新装 161 ms, 更新 364 ms, 卸载 135 ms, 仅作为单次操作观察, 不作为跨设备性能基准.
- API 35 Shizuku 测试包未带标志时拒绝, 带 `allowTestOnly` 后安装成功; debuggable v2 -> v1 未带标志时拒绝, 带 `allowDowngrade` 后成功. 非 debuggable v2 -> v1 即使带两项降级标志仍返回 `INSTALL_FAILED_VERSION_DOWNGRADE`, 当前版本保持 v2.
- API 35 Shizuku 的 XAPK 实际安装 base 和 `feature.extra` 两个 APK, 包管理器核验 split 名和版本. 默认安装者为 `com.android.shell`; 显式 shell 与插件自身两种安装者请求均静默成功, 实际归属分别等于请求值. 没有修改系统默认安装器.
- API 35 Shizuku 仍接受没有 `bypassLowTargetSdk` 的 targetSdk 22. 测试卸载这次固定夹具后, 使用该标志重新安装并验证成功. 这证明标志被接受, 不证明绕过了原本存在的系统拦截.
- API 33 none 完成真实系统确认的新装, 更新和 XAPK 两分包安装; 测试包和 debuggable 降级的正常请求被系统拒绝, 显式请求对应特权标志返回 `AUTHORIZER_REQUIRED`. 这两项通过表示拒绝语义正确, 不表示 none 具备特权安装能力. targetSdk 22 普通安装成功, 该 API 不涉及 API 34+ 的低 targetSdk 绕过.
- API 33 none 新装 557 ms, 更新 801 ms, 结果均为 `dialog`, 实际安装者是插件包名. 之后移除固定夹具仅属于测试清理, 不作为本轮生产 none 卸载验证.

## 不可用身份与未授权尝试

API 24 的 shell 只读查询可见 `/system/xbin/su`, 权限为 `-rwsr-x---`, 所有者 root, 组 shell. 在保留数据覆盖 Debug 后, `run-as` 实际进入插件 UID 10293 的 `untrusted_app` 上下文; 该身份查询和执行同一个 su 均返回 `Permission denied`. shell 身份能看见 su 不等于插件可以通过 libsu 获得 Root. 因此没有发起 API 24 Root 的夹具安装, 没有运行 `adb root`, 没有安装 Magisk, 没有调整文件权限, 没有启动临时 Shizuku server.

API 33 Sony 的原 Shizuku server 虽然运行在 root UID, 但本插件的 `moe.shizuku.manager.permission.API_V23` 原为 `granted=false`. 补充同机 Shizuku 的初次尝试进入授权等待, 首项在 30 秒后返回 `Shizuku authorization timed out`, 第二项开始再次等待时停止了本次插件测试进程. 日志 `core-shizuku-QV770340J7.log` 保留第一项失败及 `Process crashed` 的主动中断终态: 0 通过, 1 已报告失败, 其余未完成; 不能写成 8 项失败, 8 项跳过或完整通过.

停止时尚未创建任何安装 session 或安装固定夹具. 只移除本次新建且仍为空的 `cache/core-matrix-31223752231211` 目录, 并在核对 Activity 组件和唯一新任务后移除本次 Shizuku `RequestPermissionActivity` 的 task 349. 没有点击授予或拒绝授权, 没有停止 Shizuku manager/server, `API_V23` 最终仍为 `granted=false`. 未授权尝试不会覆盖先前该设备 KernelSU Root 6/6 的真实证据.

## 许可与最终恢复

API 33 none 的未知来源测试前置许可使用 `tools/install-permission-journal.py`, 在写入前持久保存 journal `a2ead944d8dd4146aada8357fd73e288`. 原 package 和 UID 模式均为 `default`; 测试后恢复为相同值, `pending=false`, `phase=restored`. 这属于临时预授权矩阵, 不作为首次进入 Settings 授权流程的证据. API 24 的原全局未知来源开关 `1` 与 API 35 的原安装 app-op 均未改变.

三台设备的 Debug 收尾审计均通过:

- 全部枚举用户的安装包集合与基线相同, 所有固定 Core/Spike 夹具及保留数据不存在.
- 私有历史与偏好文件的路径集合和 SHA-256 全部相同, 没有遗留 `core-matrix-*` 来源目录.
- 全部 preferred/last-chosen 的完整 XML 摘要不变, 未清除或替换任何用户默认记录.
- 活动系统 session ID 集合分别保持原 0 / 6 / 20 个, 无固定夹具 session 或安装前台服务.
- 两台真机的原 server PID 20102 / 6403 保留, API 24 仍无 server, 用户 AVD 保持运行.

最后向三台设备保留数据覆盖原 `autojs6-plugin-three-setup-installer-v1.0.0-3e230912.apk`. `restored/<serial>.json` 均核验 `versionCode=44`, `debuggable=false`, 已安装 APK 的 SHA-256 等于上文发行摘要, 插件 UID 保持, 包集合/默认项/安装许可/session/server 基线仍一致. 不把恢复原 build 44 描述为本轮新版本发布.

API 24 在最终恢复前额外运行本轮新的 `DefaultInstallerHistoryGuardDeviceTest`, 4/4, 无失败或跳过, 0.107 秒, 日志 `default-history-guard-emulator-5554.log`. 该组只验证保留旧记录的保护条件, 不写系统默认项或启动特权服务, 属于 P6 默认页测试保护补充; 不计入上表 P2 安装选项的通过数. 执行后再次核对历史/偏好/默认项基线, 再恢复 Release.

## 原矩阵剩余条件

结合 [既有 P2 核心证据](p2-core-evidence.md) 和 [P6 设备证据](p6-matrix-evidence.md), API 24 的 none / Shizuku, API 35 的 none / Shizuku, API 33 Sony 的 Root 及本轮 none 已有各自真实的安装选项结果. 仍不能勾选原 P2.3 完整矩阵:

1. API 24 缺少可由插件应用 UID 经 libsu 使用的 Root 环境, 原 AVD 的 shell 专用 su 不满足条件. 若继续完成原矩阵, 需要维护者提供或明确安排一个保留用户环境的 API 24 Root 测试环境; 本轮没有擅自改造当前 AVD.
2. 按同一台 API 33+ 真机逐种授权验收时, Sony 的 none / Root 有证据, Shizuku 尚未授予本插件. 本轮不为补齐矩阵改变现有授权设置; Xiaomi Shizuku 的通过也不虚构成 Sony Shizuku 通过.
3. 现有 API 35 ROM 未阻断 targetSdk 22. 若要证明 `bypassLowTargetSdk` 实际消除了拒绝, 仍需有原生拦截的 API 34+ 特权测试环境. 当前结果仅覆盖该 ROM 上标志可用和 API 34 前的忽略语义.

没有新增, 分拆或丢弃路线图小节. 以上环境与授权条件需要在会话结束时如实交接, 不能靠改变成功定义收口.
