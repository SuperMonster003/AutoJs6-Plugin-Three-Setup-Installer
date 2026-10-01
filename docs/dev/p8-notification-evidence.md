# P8 通知栏安装模式验收

日期: 2026-10-02. 对应原 P8 的通知栏安装模式条目. 本文区分通知路由检查与真实安装, 不以 PendingIntent 或界面测试替代安装成功.

## 设备与原始基线

- 独占设备: Sony XQ-AT72, serial `QV710AF65F`, Android API 31, `arm64-v8a`, user 固件. 用户列表为 0 / 10, 当前用户为 0.
- 原主包: Release build 51, `debuggable=false`. APK `autojs6-plugin-three-setup-installer-v1.0.0-cf864c74.apk`, SHA-256 `547334493ecd76f9815167c2159e5730de5ac14cf310674d5cbd7dfa30ae29d0`.
- 原 Shizuku server PID 为 `27810`; 开始时平台活动安装 session 为空, 两用户均无本次固定 fixture 或其保留数据, 无插件前台服务.
- 原完整 preferred XML 包含 32 条记录, 其中原插件四条最近使用记录保持保护. 不修改默认安装器或通过 chooser 写入新的最近使用记录.
- 原系统基线和原测试 APK 备份保存在 `build/p8-notification-QV710AF65F-64e739a0/`. 原测试 APK 已存在, 因而本轮恢复其原包, 不将它当作本轮新安装的应用卸载.
- Release 私有目录不能由 run-as 读取. 保数据覆盖 Debug 后, 在运行任何 instrumentation 或加载历史之前读取私有文件, 得到 8 条历史记录, 没有 shared_prefs 文件. 以这次读取为私有数据基线, 不声称直接读取过 Release 的私有历史.

本轮第一份 Debug APK SHA-256 为 `3ab8bf87c15c53ae1cde4bb71e5e0729bfbf27a7cf4bd7728b3ac0ee8f0e9f34`, androidTest APK 为 `42db7ec2a8d71e9a3c7cd9dee7519f2e39acce593f3df7975e6d2466dc4092e6`. 后续新增真实流程测试需要增量构建, 对应 APK 摘要另行记录.

## 通知路由与拒绝检查

初始 `ui.NotificationInstallDeviceTest` 4/4 通过, 无跳过, 耗时 0.183 秒 (`notification-routing-instrumentation.log`). 随后补入真实隔离 channel 拒绝, 在固定的 Debug 51 / androidTest 上 5/5 通过, 无跳过, 耗时 0.155 秒 (`notification-routing-and-channel.log`). 两个固定 APK 分别为 `tested-debug.apk` / `tested-androidTest.apk`, SHA-256:

- Debug: `e6aa6e5685b31a32a0cb3f76aba3be9d6a5af5cccbb5c2d106b0b5117cbf27a1`.
- androidTest: `9ffc932a7f0b2602cc90cb18fd913291cd31c9699549afd90f38ed2b4be7d8ef`.

路由检查覆盖:

- 初始确认实际发布含安装 / 取消两个动作的通知, 没有指向插件安装 Activity 的 contentIntent; 旧 prompt token 不能批准当前项目, 当前 token 只能提交一次选择.
- 系统确认 ticket 发布通知后仍未附着 Activity, 未调用自动启动路径. 不可变 PendingIntent 和取消动作对应同一活动 ticket; 取消得到 `USER_CANCELLED`.
- 已关闭的系统 ticket 不能重新发布确认, 不创建新安装会话.
- 新建独占的 `IMPORTANCE_NONE` 测试 channel, 调用与生产共用的 channel 可用性检查, 得到 `NOTIFICATION_UNAVAILABLE`, 随后删除该测试 channel. 原安装 channel 和应用总通知状态保持原样. 此项证明 channel 策略拒绝, 不是把用户安装 channel 切成关闭后调用完整请求.

初始确认检查使用未启动持久化的隔离 Record, 不写历史且不执行安装. 检查后 8 条历史原始字节相同, 全部 32 条 preferred 记录的 canonical XML 相同. 主机读取 XML 的换行序列可能不同, 不以 raw 文本换行差异推断系统默认项变化.

另外, 主线程在新接入的 Samsung SM-A566B / API 36 上执行真实请求拒绝用例 `notificationUnavailableFixture=true`, 1/1 通过, 耗时 0.166 秒. 初始及最终 `POST_NOTIFICATIONS` 均为 `granted=false`, 未为测试切换用户通知设置. 日志明确记录 `NOTIFICATION_UNAVAILABLE`, `sourceOpened=false`, `platformSessionCreated=false`, `historyUnchanged=true`. 证据位于 `build/p8-samsung-f8cb29acf84f/notification-denied.log`, `permissions-before.txt` 与 `permissions-after.txt`. 该实验仅证明通知不可用时的请求边界, 不代表在 Samsung 上完成实际包安装.

## 真实 Shizuku / none 安装

使用上述固定 Debug 51 / androidTest. 固定无代码夹具 `fixture-v1.apk`, 包名 `io.github.supermonster003.autojs6.installer.spike.fixture`, SHA-256 `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69`.

`NotificationInstallFlowDeviceTest.realInstallationIsApprovedFromNotificationsAndReturnsTheInstalledPackage`:

- Shizuku 1/1 通过, 耗时 3.725 秒, 记录 token `b2a3eb95-8f6b-4bb5-a5af-e4143547ee1c`. 初始通知批准后完成真实静默安装, 查询到版本 1, 结果 `interaction=notification`, 未创建 `InstallDialogActivity`, 未出现系统确认或扫描. 日志 `notification-shizuku-real.log`.
- none 1/1 通过, 耗时 1.956 秒, 记录 token `115c8b74-2e47-4af1-b949-4f9b11b0a8d3`. 初始通知批准后出现系统确认通知; ticket `24c1fcf1-e0b8-43f3-af54-4aa0465f48c7`, 平台 session `2104230607`. 检查到通知动作执行前 ticket 未附着 Activity, 然后打开真实系统确认并批准固定夹具, 查询到版本 1. 结果 `interaction=notification`, 无插件安装对话框, 未出现扫描. 日志 `notification-none-real.log`.

通知操作通过真实发布通知内的 `PendingIntent.send()` 分发, 不声称使用坐标点击了系统通知栏. 系统安装确认由 UI 自动化操作本次固定夹具对应的按钮. 每次安装均核验实际已安装包与版本, 因而不以通知 PendingIntent 检查代替安装成功.

每项开始前拒绝所有用户中的既有夹具或保留数据. `FixturePackageOwnership` 精确清理本次夹具, `FixtureHistoryOwnership` 只清理本次 token 并验证其他记录与持久化结果. 两项均确认源包保留. none 来源安装许可仅通过既有许可 journal 准备和恢复, journal `build/install-permission-journals/d2c78716d68f41ad845b6c992473a710/journal.json` 已恢复 `default/default`, `pending=false`. 两项后历史 8 条原始字节与基线相同, 全部 32 条 preferred canonical XML 相同.

## 跨 UID 来源授权

独立 androidTest APK 中的非导出 provider 提供唯一 nonce 的固定夹具 URI, 由仅有 DUMP 权限者可启动的 NoDisplay relay 明确授权给生产 `ExternalInstallActivity`. provider 与插件 UID 分别为 15159 / 15158. 用例先确认未授权时不能读取, 临时将原本为空的设置写为 `default_interaction=notification`, 并在修改前保存带设备, UID, nonce 与原状态的恢复 journal. 只取消通知确认, 不提交平台安装 session.

保留以下首次失败, 不覆盖其日志:

1. `notification-cross-uid-grant.log`: 在任何设置修改前, Sony CTA 将非导出 provider 的授权拒绝表现为 `FileNotFoundException: No content provider`, 而用例最初只接受 `SecurityException`. 后续限定兼容为 provider 已注册, UID 不同, `checkUriPermission=DENIED` 且异常精确为该 URI 的 `No content provider`; 不接受任意 IO 错误. 同时将独立测试 APK 的 helper 改为纯 Java, 避免依赖主 APK 才有的 Kotlin runtime. 此轮没有证明发生 Kotlin 崩溃.
2. `notification-cross-uid-grant-java.log`: Debug 52 SHA-256 `103d5bd33d0bb57e34736442ff71c5e3ae7a22646d1e40ce4295dedb8c62535a`, androidTest SHA-256 `2dc0dcbeef9fb44d21575d6f21a55789e0c1c4bc02f37a763af35ebe9fb3646e`. nonce `37c220d5-a06e-4a55-890e-9a05a8acfab5`, record `c3fa07d0-067c-42cb-8052-e59fc24305b7`. 已证实生产 NoDisplay 销毁后 FGS 存在, 可再次打开跨 UID URI 且内容摘要与固定夹具相同, provider 查询核对真实双方 UID, 尚未创建平台安装 session. 取消及精确历史清理成功. 整体用例仍失败: URI 释放等待未达成, 随后清理中的带 shell 引号命令被 `UiAutomation` 当作字面参数处理, 清理异常遮蔽先前原因. 后续改为平面 run-as 命令, 并保留原异常及专属 grant owner 诊断.

第二次失败后, instrumentation 退出时已确认该 nonce URI 授权与 FGS 消失, 历史原始字节/偏好/全部 preferred 回到基线. 仅在核验精确 nonce, journal 设备与 UID, 原状态及夹具 SHA-256 后, 删除本次遗留的 provider cache 与已归档 journal. `state.json` 的 `grantFirstJavaCleanup` 记录了检查结果. 诊断版 Debug 52 / 1.1.0 随后复现相同失败, 目前不把整个跨 UID 用例记为通过.

诊断 APK SHA-256 分别为 `f4ca0458797fd458a9c1f68b8bafb8eaf71478940c2048bd862b1b089ead9130` / `1abbfcab9b3c5b318469000e9b661ca0d5b2f96ea56085b45e3b3ff4163a892c`. 日志 `notification-cross-uid-grant-diagnostic.log`, 1/1 失败, 耗时 13.318 秒. nonce `6634c680-7079-47ae-8c6d-45eca6973502`, token `2b1f0e42-48ac-4977-a862-0448318f127d`. 这次原错误未被清理异常遮蔽: FGS 通知已消失, URI 权限仍为 GRANTED. 仅该 URI 的 readOwner 明确为 `InstallForegroundService StartItem id=1`, `owned=0x1`, `global=0x0`, `persisted=0x0`, 没有 Activity owner. 本次清理正常完成, instrumentation 退出后授权消失, 精确 journal 已归档并移除.

此结果与 Android 12 主线实现一致: `START_NOT_STICKY` 完成时移除 delivered StartItem, 未撤销其 URI owner; 最终 stop 清理 delivered 列表时已找不到该项. 依据 [ActiveServices.java](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-12.0.0_r1/services/core/java/com/android/server/am/ActiveServices.java) 的 `serviceDoneExecutingLocked` 和 [ServiceRecord.java](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-12.0.0_r1/services/core/java/com/android/server/am/ServiceRecord.java) 的 `findDeliveredStart` / `clearDeliveredStartsLocked`, 并以本机精确 owner 诊断确认实际症状.

生产修复只让携带临时 URI grant 的合法 start 返回 `START_REDELIVER_INTENT`, 保留到服务停止时可撤销的 StartItem. 重投或未知 token 不会读取来源或创建安装; 无活动 writer 时立即 stop, 空 registry 的 onCreate 也不发布 0 任务通知. 若已有其他合法 writer, 保留其授权到共用服务统一停止, 不以 `stopSelf(startId)` 或全局 revoke 撤掉较早 start 的有效授权. 活动任务登记与 idle-stop 共用锁, 已正常停止的旧服务实例不会把新任务误标为授权丢失.

修复后的 Debug 52 SHA-256 `fd7f2d5d8cd7265eb9936c67537e7df0ffa3e8ea0426c825d5813e206046e629`, androidTest SHA-256 `79432f8de7ed594cd585418ed163ff5f8bfe05b5012da92ee5b4efdc156a7473`. `notification-cross-uid-grant-fixed.log` 的完整用例 1/1 通过, 耗时 3.7 秒:

- nonce `2245d3b0-41ab-439e-8279-a41e8e6f7b98`, record `55b1d252-8d25-4cfb-8cc2-c7eac4f28e35`, NoDisplay 销毁后再次读取固定内容成功.
- 向同一 FGS 交付同源 URI 的未知 token start, 原合法通知记录, FGS 与授权均保留, 没有创建平台安装 session. 这直接覆盖了误用 stopSelf(startId) 可能撤销较早合法授权的并发边界.
- 取消真正 owner 后, 服务停止且 URI 权限变为 DENIED; 再次读取被拒绝. 历史/普通偏好/全部 preferred/session 均按基线检查, 精确 journal 已归档并移除.

## 真实主进程死亡与系统重建

仅在 DUMP 保护的既有 debug Activity 中新增固定夹具控制模式, Release 没有此入口. 控制 journal 绑定 UID, SDK, fingerprint, nonce, PID/startTicks, 原历史摘要与唯一 token. 自杀前要求唯一活动项正在等待本夹具确认, 没有平台安装 session, 原历史未变. 先结束并移除独立控制 task, 确认 Activity 已销毁且 appTasks 不再包含该 task, 才由本进程终止自身.

首先保留一次未通过的驱动尝试: nonce `1691c3ed-2cf7-43c3-a12a-c00f4d4b0f7c`, creator PID `30499`, `run-as kill -9` 被 Sony 拒绝并返回 `unknown pid`, 紧随其后的 ps/proc 仍显示相同进程. 没有把该尝试计为进程死亡. 原请求随后达到 300 秒确认时限, 以 `USER_ACTION_TIMEOUT` 结束, URI grant 与 FGS 在任何包升级前均已消失. 新 debug helper 的 abort 模式精确清理其已终止的 token/临时设置/来源缓存, 不声称这次 abort 证明了重投.

最终真实进程重建使用 Debug 1.1.0 / build 57, APK SHA-256 `0e73a824d0635ff288bbd683df55146b1defd18846ec921790c498267525688a`, 测试 APK 沿用上述 `79432f8d...a7473`. 证据目录 `restart-125ddf2c-e102-431c-aa36-efe1acb3159a/`:

- record `69ddf026-1770-4a80-bb0e-1d46f6afc436`, creator PID `31355`, startTicks `65764843`. journal 记录 `deathControlActivityDestroyed=true`, `deathControlTaskRemoved=true`.
- 系统在进程死亡后记录 crashed `InstallForegroundService` 的 `start-requested` 重启, 8046ms 后明确为该 service 创建新 PID `31969`. 此观察窗口没有启动 Activity 或 instrumentation; 之后才调用清理控制入口.
- 新进程记录拒绝未知来源 owner, `otherWriters=false`, 然后停止. 本机的 start 标志实际 `redelivered=false`, 因而未知 token/空 registry 守卫不可只依赖 `START_FLAG_REDELIVERY`.
- provider 在死亡前后均仅有相同的 3 条 query/open 事件, 无新的来源读取. 没有安装 fixture, 没有创建平台 session; URI 授权与 FGS 均消失. 恢复历史把唯一未完成项目标为 interrupted/cancelled, 不重放工作.
- 精确移除本 token 与恢复快照/暂存目录, 恢复原空偏好, 删除本 nonce 的 provider 缓存并归档控制 journal. 原 8 条历史字节完全相同.

## 收尾状态

最终 Debug 57 审计已经完成: 原 8 条历史 SHA-256 仍为 `d911dd158b41b35643a45a5f37429b2669ab7f03a7eaf0b3bf2b30a5eca08520`, shared_prefs 无文件, 全部 32 条 preferred canonical XML 相同, 用户 0/10 的包集合均未改变且无 fixture 或其保留数据. 临时来源授权, FGS 与平台活动安装 session 均为空, 原 Shizuku server PID `27810` 保持不变, 安装来源 AppOps 为原 `default`. 结果保存在 `final-debug-baseline-report.json`.

原先存在的测试 APK 已保数据恢复, 并核对设备所装 APK SHA-256 回到 `8e253348315d83e0b77bd41dfe99ed4669c13ef63be01a3fa72b762602fd8c51`.

主线程决定最终交付同一签名的 1.1.0 / Release 57, 因而不再将主包退回最初的 Release 51. 已用保数据覆盖安装完成交付:

- APK: `releases/autojs6-plugin-three-setup-installer-v1.1.0-c2238464.apk`, 1,965,043 bytes.
- 最终 Release SHA-256: `d1300f2b1ac97c240bc02e58752909d1fa5b78a18397d3894ac5ce1aef8df31e`. 设备所装 base.apk 的摘要逐字相同, `versionName=1.1.0`, `versionCode=57`, `debuggable=false`.
- 此 Release 包与前述用于白盒流程/死亡控制的 Debug 测试包不同. 最终 Release 包含主线程后续 Dhizuku API 26 元数据读取兼容修正; 通知生产实现不变. 不把 Debug helper 用例冒称为在 R8 Release 中执行.
- 覆盖前再次直接读取 Debug 私有目录, 确认原 8 条历史字节和原空偏好. Release 非 debuggable, 覆盖后不声称再次通过 run-as 读取其私有目录.
- 覆盖后复核全部 preferred, 两用户包集合及各用户 installed 状态, 安装来源 AppOps, Shizuku PID, 原测试 APK 摘要, 平台活动 session, FGS 与临时 URI grant. 全部与原基线或明确交付目标相符, 无 fixture 遗留. `final-release57-report.json` 记录最终状态.

`QV710AF65F` 已释放. 本次没有修改用户的通知总开关, 原安装 channel, 默认安装器偏好, 原 8 条安装历史或其他用户包数据; 仅交付主插件新版本, 并恢复原测试 APK.
