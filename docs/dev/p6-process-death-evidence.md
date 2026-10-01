# P6.1 进程死亡与特权连接恢复

日期: 2026-10-01. 本文记录原 P6.1 第一项的生产语义, 真实设备故障与验证范围. 不将合成 Binder 异常当作真实进程死亡, 不因部分设备通过而提前勾选整个死亡矩阵. 宿主 V1 契约和自带 AAR 未变; 新增的方法只属于插件自有的私有特权 AIDL.

## 生产语义与恢复边界

- `PrivilegedClient` 只在没有副作用的连接握手期间允许一次重绑. 两次握手共用同一超时预算, 通过真实 `getUid` Binder 调用验证连接. 权限拒绝, 空绑定, 显式关闭, 超时和中断不被猜成可重试的 Binder 死亡. 失败握手只能退役自己取得的连接, 不能破坏其它调用者后来建立的替代连接.
- `createSession`, `openWrite`, `commit` 和 `uninstall` 均不在重试边界内. 创建或提交已被系统执行但答复丢失时, 不猜测成功, 不自动重放操作. 安装或卸载等待结果期间发现实际 Binder 已死, 返回 `AUTHORIZER_UNAVAILABLE`, 不继续等到泛化的 `TIMEOUT`. 已入队的真实系统终态仍优先于随后观察到的死亡或取消.
- 特权进程遭 `SIGKILL` 后不能执行 `finally`, 普通平台 session 也不会可靠地随它消失. 主进程仍活且已经收到本次 sessionId 和完整恢复信息时, 可以在旧 Binder 确认死亡后进行一次 5 秒预算的连接获取, 只放弃该 session. 不重新创建, 写入, 提交或安装.
- 恢复信息由创建该 session 的特权实例读取并保存. 新实例重新查询系统 `SessionInfo`, 精确核对 sessionId, originatingUid, 真实 installerUid, userId, createdMillis, installerPackageName, appPackageName 和 size. originatingUid 为实际发起私有服务请求的插件 UID; installerUid 从系统字段读取, 不以当前进程 UID 冒充系统记录. 私有 `abandonRecoveredSession` 对全部字段核对通过后才操作这一个 id; 原 `abandon` 仍只接受本实例持有的记录.
- AOSP 到 API 33 才在 `SessionInfo` 暴露真实 installerUid. 代码只在完整字段可读取时启用恢复; API 24-32 以及缺失字段的 OEM 实现不使用削弱的匹配规则, 不扫描或接管全局 session. 创建答复丢失, 恢复信息尚未返回, 主进程同时死亡或授权服务器不可用时, 也不承诺自动回收. 这些窗口不自动重试安装, 不猜测安装结果.
- Shizuku 的 bootstrap 在服务器 Binder 死亡时直接执行 `System.exit(0)`, 不先调用插件的 `destroy`. 自有 `ShizukuUserService` 因此注册正常 VM 退出 hook, 调用既有会话清理; 并发 `close` 最多等首次清理 5 秒, 防止退出 hook 看见 closed 标志就提前结束. 主进程解绑失败时, 也仅向本次已附着且仍存活的自有 Binder 调用原有受 UID 保护的 `destroy`. 退出 hook 不覆盖 `SIGKILL`, 也不保证失去响应的系统服务能完成清理. 该行为依据 [Shizuku 官方 ServiceStarter](https://github.com/RikkaApps/Shizuku/blob/master/starter/src/main/java/moe/shizuku/starter/ServiceStarter.java) 及本轮 API 24 实测, 没有复制其实现.

## 真实特权进程测试的安全边界

`PrivilegedProcessDeathDeviceTest` 显式要求 `peerDeathAuthorizer=shizuku|root`. 每项先确认没有活动插件任务, 并使用既有全用户夹具归属守卫, 拒绝修改任何预存夹具或保留数据. 仅使用仓库内无代码 v1 APK, 校验包名, 版本, 无 dex/so 及 SHA-256 `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69`.

每次被终止的 PID 来自本次真实绑定的 `getProcessIdentity`, 再核对 ownerUid, 特权 UID, `/proc` 中的完整插件进程名前缀和进程出生 ticks. 发出信号前重新读取同一 Binder 身份和 `/proc` 值. 普通 peer 测试只杀本插件的 UserService/RootService, 不停止或重启真机已有的 Shizuku server. 没有向 Release 增加杀进程接口.

四类 peer 用例分别验证:

1. 已真实打开平台写入流并写出字节, 在 pipe 仍开放时杀特权进程. 原操作返回 `AUTHORIZER_UNAVAILABLE`, 实际提交数为 0. API 33/35 在测试清理之前确认生产恢复已让原 session 消失. 之后才由测试显式发起一个全新的请求, 验证新连接可以真正安装夹具.
2. 真正提交并收到系统 `STATUS_SUCCESS`, 但通过测试专用接收器扣留该回执, 然后杀特权进程. 引擎等候路径及时返回 `AUTHORIZER_UNAVAILABLE`, 创建和提交各 1 次, 已安装夹具仍为 v1. 这是有明确回执故障注入的真实进程终止测试, 不冒充自然丢包或普通成功 UI 流程. 它说明回执不明时不能自动重复安装.
3. 对两个实际 UserService/RootService 依次发出 `SIGKILL`, 随后在真实死 Binder 上执行只读调用. 生产握手函数最多获取两个连接, 第二次死亡后终止, 平台创建数为 0.
4. 对一个真实但未提交的平台 session 的八项恢复字段逐一篡改, 再测试未知字段及错误字段类型, 共 10 次拒绝. 每次拒绝后 session 都保持存在, 提交数为 0; 最后由本次原始连接清理.

## 初轮故障与修复后的 peer 证据

| 设备/组合 | 实际结果 | 本地日志 |
| --- | --- | --- |
| Xiaomi 23046RP50C / API 35 / `968e9f18`, Shizuku, 初轮写入死亡 | 原测试 1/1 通过, 但测得平台 orphan, 不代表生产回收完成 | `build/p6-peer-writing-shizuku35.log` |
| 同设备, 初轮握手死亡及提交后回执故障 | 2/2 通过, 真实死亡与不重放边界成立 | `build/p6-peer-commit-handshake-shizuku35.log` |
| 同设备, 完整恢复核验版本 | 四个 peer 用例通过, 另一个临时 server-stop 方法因未提供 PID 有意跳过; 不能将 runner 的 `OK (5 tests)` 写成五项均通过 | `build/p6-peer-recovery-shizuku35.log` |
| Sony XQ-DQ72 / API 33 / `QV770340J7`, KernelSU Root, 初轮 | 三项失败, 命令结果为空导致进程归属 guard 拒绝发信号; 不计进程死亡通过 | `build/p6-peer-root33-initial.log` |
| 同设备, 首次完整恢复版本 | 显式选择四个 peer 方法, 恢复字段 guard 通过, 另外三项仍因未收集命令 stdout 失败, 无跳过 | `build/p6-peer-recovery-root33.log` |
| 同设备, 显式收集命令输出后的最终版本 | 四个 peer 用例 4/4 通过, 无跳过, 6.182 秒 | `build/p6-peer-root33-final.log` |

初轮 Shizuku 写入死亡为 PID `30311`, session `145230168`, 提交数 0, `platformSessionStillPresent=true`. 该 session 由测试依据本次真实返回 id 精确清理后, 测试的新请求 `899606922` 安装成功. 此结果促成生产恢复实现, 没有掩盖或删除最初的回收缺口.

恢复版本的 Shizuku 写入死亡为 PID `401`, 原 session `437641077`, `platformSessionStillPresent=false`, 新请求为 `890332102`. 两次真实握手死亡 PID 为 `574` 和 `647`, connectionAttempts=2 / platformCreates=0. 提交后故障 PID 为 `732`, 创建和提交均为 1, 940 ms 内返回 `AUTHORIZER_UNAVAILABLE`; 初轮对应测试为 315 ms. 恢复字段拒绝用例使用 session `1875032665`, rejectedVariants=10 / commits=0. 四项均输出来源和全用户夹具清理成功. 整个类耗时 6.322 秒, 包含一个有意跳过的方法.

Root 探针的初步 NUL 假设未被复测支持: 即使先在 su shell 内将 `/proc/cmdline` 的 NUL 转为换行, 结果仍为空. 后续审查确认 `Shell.newJob()` 未显式收集 stdout; 修复为 `.to(output, errors)` 后才读取结果. `tr` 保留用于避免二进制分隔符跨越文本 API, 不把它当作已证实的根因. 插件进程名, UID 和出生时间核验不放松. 初轮提交后用例在归属 guard 之前已经安装了本次夹具, 失败后继续执行限定本次 session/来源/夹具的 finally 清理, 不把初轮失败当作安装或进程死亡验收完成.

最终 Root 写入死亡为 PID `30224` / UID 0 / 插件 ownerUid 10623, 原 session `1149385708` 在生产恢复后消失, 测试显式发起的新请求 `1377009717` 真正安装成功. 两次握手死亡 PID 为 `30425` 和 `30474`, connectionAttempts=2 / platformCreates=0. 提交后故障 PID 为 `30581`, 创建和提交各 1 次, 942 ms 内返回 `AUTHORIZER_UNAVAILABLE`, 系统实际安装的夹具为 v1. 恢复字段拒绝用例为 session `694511168`, 10 次拒绝期间提交数 0, 平台 session 保留直到本次主动清理. 四项均有全用户夹具/来源/自有特权服务清理成功证据, 真机 Shizuku server 未改动.

## 临时 Shizuku server 停止

独立方法 `stoppingOnlyTheExplicitTemporaryEmulatorShizukuServerCancelsItsLiveWrite` 还要求显式 `temporaryShizukuServerPid`, API 24 和 `ro.kernel.qemu=1`. 它只允许本轮从停止基线临时启动的 AVD server, 验证进程名精确为 `shizuku_server`, UID 2000, 出生 ticks 和当前实际 Shizuku Binder. 已存在的真机 server 不属于该测试授权范围.

初轮对 `emulator-5554` 的临时 PID `13984` 在真实写入阶段停止 server, 已观察到插件私有 Binder 死亡和 `AUTHORIZER_UNAVAILABLE`, 创建 1 次/提交 0 次, 但原平台 session 仍存在, 测试明确失败. 日志为 `build/p6-shizuku-stop24-initial.log`. 原因不是私有 Binder 仍活, 而是 bootstrap 正常退出没有执行插件的会话清理. 测试 finally 只清理其已取回的 session id 和自有服务, server 保持停止.

退出 hook 和解绑补充清理后的复测使用重新临时启动的 PID `14441`, startTicks `2057872`, session `259044325`. 最终 1/1 通过, 无跳过, 耗时 0.821 秒, 日志 `build/p6-shizuku-stop24-final.log`. 私有 Binder 已死, 原 session 在测试清理前已由生产路径放弃, 返回 `AUTHORIZER_UNAVAILABLE`, 提交数 0, 无操作重放. 最后全用户夹具和来源清理通过, 临时 server 保持停止, 既有真机 server 均未操作. 这是 server 被终止后 UserService 的正常 VM 退出清理, 不代表 API 24 对直接 `SIGKILL` 私有进程也能执行退出 hook 或进行完整元数据恢复.

## JVM 与故障注入回归

`BindingHandshakeTest` 六项覆盖单次替换, 第二次死亡终止, 共享预算, 迟到连接, 显式关闭和拒绝/中断不重试. `SharedBindingCacheTest` 新增迟到失败不得退役新连接的回归. `PrivilegedLifecycleDeviceTest` 新增两个明确标为 fake Binder 的丢失创建/提交答复断言, 各自只允许一次操作; 这些仅作行为边界回归, 不计入真实 PID 死亡证据.

首轮 Debug/androidTest 构建因设备测试泛型被 Unit 上下文错误推导而失败, 明确指定 `IPrivilegedInstaller` 后统一构建通过, 见 `build/p6-lifecycle-debug-build.log` 和 `build/p6-lifecycle-debug-build-verified.log`. 上述六项 JVM 在首轮即已通过. 最终综合测试与构建数量以本轮最终验证记录为准.

## 宿主死亡与插件主进程死亡

`tools/run-process-death-acceptance.py` 使用两种独立路径, 不以私有测试 CallerGuard 冒充正式宿主身份:

- `plugin-main`: 受 DUMP 保护且只进入 Debug 的 `InstallProcessDeathProbeActivity`, 通过生产 DescriptorInstallEnvironment/InstallSession/PrivilegedInstallEngine 创建真实平台 session. 包装私有接口只记录返回的 id/身份和拒绝意外提交, 在实际写入至少一批字节后等待驱动终止本进程. READY 之前必须观察到 FGS, 未完成历史和恢复快照已经落盘. 主进程被杀后, 驱动从系统 active session 区核验本次 id 消失; 新 PID 只读恢复为取消, 再实际启动空状态的 InstallForegroundService, 确认它停止且没有新 worker/安装. 夹具始终未安装, 提交次数为 0.
- `host`: 宿主 Debug 的 DUMP Activity 在 `org.autojs.autojs6:installer_process_probe` 中运行, UID 是正式 AutoJs6 UID. 它使用正式 InstallerPluginHost 的选择, 签名/信任校验和专用 session lease. 来源由另一个非导出宿主进程 `:installer_process_source` 提供, 仅接受专用 100 MiB 无代码夹具, 送出 512 KiB 后保持写端. 插件检查私有磁盘上的实际 256-512 KiB 暂存及其源文件前缀 SHA-256 后, 驱动才杀调用进程; 写端不会同时关闭, 因此不会用 EOF 先到产生的 INVALID_PACKAGE 代替 Binder 死亡取消. 本路径核验 CANCELLED 和暂存即时删除, 不创建平台 session. 这证明实际官方宿主 UID 的调用进程死亡, 不是终止用户的 AutoJs6 主进程.

驱动只在核对 PID, UID, `/proc/cmdline` 和进程出生 ticks 后发信号. 部分 Sony 设备拒绝 `run-as` 向相同 UID 应用发信号: shell 的无副作用 `kill -0` 明确返回 Permission denied, toybox 却显示 unknown pid. 新调试模式只在本 case 已 READY, expectedPid 等于自身且会话仍在预定阻塞点时执行 `Process.killProcess(myPid)`. 它不接受任意 PID, 不进入 Release, 也不使用 Root shell 来绕过设备的信号权限. 驱动记录 EACCES 和采用的机制.

原 APK session 只是精确 id 的核验对象. API 35 的 6 条既有 OEM session, Root API 33 的 20 条 2024 年系统 staged session 都作为只读基线保存, 不要求全局 session 为空, 不进行全局清理. 新进程的暂存回收沿用 24 小时策略; 探针明确只给自己已经证明归属的孤儿目录加速修改时间, 再执行生产 cleanStale. 因而这里验证的是过期残留最终清理, 不是声称所有暂存都在重启时立即删除.

正式宿主默认禁用插件时, 测试使用独立的 prepare/restore 事务. 修改前用 AtomicFile + fd.sync 保存固定 Three Installer enable key 的存在性和布尔值, 宿主 UID 和插件签名指纹. 通过正式 PluginTrustManager/PluginEnableStore 后才临时启用, 不更改 trust, priority 或其它插件项. 测试和清理失败也独立恢复这一个 key; 原先不存在则删除该 key, 原先存在则恢复原布尔值. 未恢复 journal 阻止新测试覆盖原值.

首个完整主进程死亡结果为 Xiaomi API 35 / Shizuku: `build/process-death-acceptance/20261001-194206-960bd62d41c04a338c14958174d7df3f/result.json`, `passed=true`. 插件 PID `30398 -> 31018`, UID 10290, 特权 PID 31467/UID 2000, session `200685017`, 实际写入 8,536 字节, 创建 1 次/提交 0 次. 原 session 消失, 6 条既有 OEM session 均仍存在; 历史恢复为 cancelled/interrupted, 空 FGS 退出, 自有历史/暂存/来源 case 目录清理完成.

正式宿主调用进程死亡在 Xiaomi API 35 的完整结果为 `build/process-death-acceptance/20261001-195224-9be594506a0942f99a1bdefa6479abfc/result.json`, `passed=true`. 调用进程 PID 516/UID 10890, 来源进程 PID 2608, 插件 PID 31933/UID 10290; 来源送出 524,288 字节, 实际私有暂存为 262,144 字节且摘要与夹具前缀相等. 本机宿主 UID 的 run-as signal 也返回 EACCES, 使用严格 guarded self-death 后, 正式 Binder 会话返回 CANCELLED, 暂存立即删除. 不发生平台创建或安装; 所有自有历史/来源 case 清理, 原本存在且为 false 的 enable key 准确恢复为存在/false, 签名指纹未变.

幂等 self-death 修订后的 Root 主进程完整复测为 Sony XQ-DQ72 / API 33 / KernelSU: `build/process-death-acceptance/20261001-195513-c2c3e2b763a84dffaa89ec283627deb7/result.json`, `passed=true`. 插件 PID `32495 -> 353`, UID 10623, 特权 PID 32673/UID 0, session `606294901`, 实际写入 8,536 字节, 创建 1 次/提交 0 次. 记录 `run-as signal EACCES` 后仅终止已核对的自身 PID; 系统在新进程重建旧控制 Intent 时不会再杀进程或污染恢复错误. 原 session 消失, 20 条既有系统 session 均保留, 历史恢复 cancelled/interrupted, 空 FGS 停止且没有重放. 自有历史/过期暂存/source case 全部清理, 所有用户无夹具残留.

驱动初轮失败保留为失败证据: Windows adb 标准输入在 APK 第一枚 0x1a 字节处截断, 改为 adb push 同步协议加 run-as cp/hash 后解决; trusted filesDir 先 canonical 化以兼容系统数据目录别名. Root API 33 的首次 signal 尝试被 EACCES 拒绝, 未杀进程, 随后原会话 CANCELLED 并完成限定归属清理; 不将该轮计作死亡通过. Sony 对控制 Intent 的旧 top 实例递送未执行预期清理时, 驱动改为 NEW_TASK|MULTIPLE_TASK 创建自己独立的短命控制 Activity, 不清除其它任务. 随后 Root self-death 已真实终止原 PID 并回收 session, 但系统在新 PID 重建旧 die Activity, 其安全 guard 拒绝再次杀进程时写入旧错误, 导致整轮驱动判失败. 现对 expectedPid 不同的新进程仅结束控制页, 不写错误或重复发信号; 原 PID 的所有 READY/身份 guard 保持. 宿主首轮因正式插件启用策略拒绝而未创建会话; 原 false 偏好当时没有改动. 这些失败 case 的本地 result/恢复记录仍保留在 build/process-death-acceptance 下, 不覆盖以上完整通过证据.
