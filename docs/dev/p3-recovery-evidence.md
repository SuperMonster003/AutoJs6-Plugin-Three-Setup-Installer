# P3.1 进程重建后的展示恢复

日期: 2026-10-01. 本次继续原有 P3.1 条目, 不加入 P5 安装历史或自动续装队列, 不变更 V1 公共契约 / AAR.

## 恢复语义

InstallDialogActivity 继续校验 SavedStateHandle token 与私有 document URI. 当进程内 Record 不存在时, 异步读取该 token 的私有快照, 显示只读结果. 已收到且保存的逐项结果保留; 没有最终结果的项目显示中断 / 取消, 不查询当前已安装版本来猜测本次安装成功.

恢复流程不创建 Record, worker, PackageInstaller session 或授权请求, 不复用 PFD / URI 授权, 不自动安装或重试. 只读界面提供结果查看, 错误复制和完成, 不提供打开应用, 安装, 重试或取消旧 worker 的操作. 原有活进程旋转仍使用同一 Record 和确认草稿.

未完成快照不代表平台安装已回滚. 例如进程丢失时系统正在 commit, 恢复界面只知道尚未保存最终结果. 该项必须显示中断, 不能根据新进程中的应用版本猜测成功, 也不能重新提交.

## 存储与并发

- noBackupFilesDir/installation-ui 私有文件, 随机 UUID 文件名. 单条最多 64 KiB, 最多 128 条; 终态保留 10 分钟, 活动快照最多现有契约允许的会话期限加 10 分钟.
- 磁盘过期使用 wall clock, 不复用设备重启前的 elapsedRealtime 原点. 明显未来时间, 过期, 损坏, 超限和未知结构均拒绝恢复.
- 字段白名单只包含有限展示事实. 不保存 APK 内容, 来源 URI / 授权, PFD, 系统 Intent, 图标或可执行对象. 错误只保留稳定错误码, 数值状态及 INSTALL_FAILED_* / DELETE_FAILED_* 标识, 丢弃任意系统消息原文.
- 普通状态合并异步写入; 字节进度不写盘. 工作线程上的逐项结果和终态等待关键快照写入. IO 失败不把已经成功的系统安装改判为失败.
- 文件以临时文件, fsync, 备份与同目录重命名更新, 读取时处理未完成写入与备份. 不发布半截 JSON.
- 关闭 / 清除立即退休 ticket / generation, 拒绝旧请求继续写入. durable 写被更新状态替代时, 等待包含已确认结果的新快照, 不提前确认已经中止的旧写.
- Activity 在读取期间收到完成 / 返回操作时记录 closing; 旋转后继续关闭, 拒绝迟到的读取回调. 正常删除完成后结束窗口. 容量淘汰先退休旧终态 ticket, 再注册新记录.

## 逻辑和界面验证

InstallRecoveryTest 新增 14 项 JVM 用例: 编解码与字段白名单, URI 清理, UTF-8 / 64 KiB / 32 项边界, 原子写失败和备份恢复, 损坏与过期, 重启时间基准, 数量限制, 写入合并与乱序, close / clear 后迟到写入, durable 写被替代, 128 条容量换位. 插件 JVM 总计 194 项, 无失败或跳过.

InstallDialogDeviceTest 从 9 项增至 12 项, 新增部分成功只读恢复, 读取期间完成并旋转后拒绝迟到回调, 终态失败只恢复安全错误标识. API 28 / API 35 的 12 项均通过. 权限测试所在混合组的失败或跳过单独记录, 不计为界面恢复失败或整组通过.

真实 IME / 大字号 / RTL / 夜间 / 横屏的三项窗口测量已分别在 Sony API 28 与 Xiaomi API 35 通过, 见 [外观证据](p3-appearance-evidence.md). build 29 会话中的新 API 24 模拟器启动被自动审批拒绝, 只返回 blocked by policy; 当时未执行 API 24 新增恢复与真实 IME 的复验. 后续在用户自行启动的 AVD 上完成了下述复验.

## 后续 API 24 用户手动启动 AVD 复验

2026-10-01, 用户自行启动 Android SDK built for x86 / API 24 AVD 后, 原始日志 build/p4-api24-p3-followup.log 报告 OK (15 tests), 14.349 秒, 15 项通过, 0 失败 / 0 跳过. 其中 InstallDialogDeviceTest 12 项包括部分成功只读恢复, 读取期间关闭和旋转后拒绝迟到恢复, 终态失败的安全错误恢复, 缺少进程记录时的中断展示, 以及确认 / 取消和已确认结果保留. InstallerDialogWindowDeviceTest 的 3 项真实窗口测试同组全部通过.

这次复验补齐 API 24 的恢复界面和真实 IME 覆盖, 不改写之前被拒绝启动时的记录. 该组未运行跨进程 force-stop 探针, 不把 Activity 重建和合成快照用例计为 API 24 真实进程更替验证; 下节 API 28 / 35 的 PID 更替证据保持独立.

## 真实进程终止探针

debug-only InstallRecoveryProbeActivity 受 DUMP 权限保护. caseId 受限, seed 拒绝覆盖已有 case, clear 只删除该 case 已知 UUID 的快照. 该探针不进入 release.

探针种下的是明确标注 synthetic=true 的展示数据: 第一项模拟已确认成功, 第二项模拟 commit 尚无结果. 不执行真实安装. 测试驱动在快照写入后通过 ADB force-stop 插件, 再用新进程重新打开相同 token, 检查内存 Record 不存在而快照可恢复.

| 设备 | seed PID | 新进程 PID | 检查结果 |
| --- | ---: | ---: | --- |
| Xiaomi 23046RP50C / API 35 / arm64-v8a | 24697 | 25743 | memoryRecord=false, snapshotFound=true, interrupted=true, knownOutcomes=[true, unknown] |
| Sony G8441 / API 28 / arm64-v8a | 5734 | 5783 | memoryRecord=false, snapshotFound=true, interrupted=true, knownOutcomes=[true, unknown] |

原始 seed / restore JSON 位于忽略目录 build/p3-followup-recovery-{seed,restored}{28,35}.json. 探针 clear 后再核对该 case 文件和快照清理. 该证据证明真实进程更替下的展示恢复, 不冒充操作系统真实安装结果; 实际包安装回归与大包写入证据另行记录.
