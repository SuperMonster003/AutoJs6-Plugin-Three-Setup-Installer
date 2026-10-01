# P6.1 空间错误, 暂存回收与同包并发

日期: 2026-10-01. 本轮推进原 P6.1 的第二个生产条目, 不将其余进程死亡/设备矩阵提前勾选. 公共 installer-api AAR 与 V1 协议未改变.

## 同包安装会话串行化

`PackageInstallLocks` 在进程内按包名维护公平信号量. Android 用户之间共享 APK 代码, 因此 userId 和授权方式都不拆分锁. 插件确认完成后获取, 覆盖旧版本采样, 真实平台会话写入/等待/放弃/关闭, 新版本采样和当前项来源清理. 每个 batch 项结束即释放, 反向包顺序的两个批量请求不会各持多把锁. 不同包仍可并行.

等待不扩张既有四会话名额, 并沿用原始 timeout 与取消. 确认 UI 不持包锁; 确认结束后清 prompt 并发布既有 PREPARING, 避免等待时保留可点击的安装按钮. 锁覆盖安装会话, 本轮没有扩展为安装/卸载共同锁.

## 暂存与空间不足

- 暂存目录首用时清理超过 24 小时的残留, 覆盖没有启动宿主 Binder 服务的独立入口. 创建和清理同步, 后续清理保护进程内活跃目录, 未来时间不误删, symlink 本身可移除但不跟随到目标目录.
- Android 目录创建使用 Os.mkdir 保留 ENOSPC/EDQUOT, 没有用 mkdir=false 的模糊结果推断空间不足. 新建暂存目录隔离仍使用安全前缀和随机 UUID.
- `StorageErrors` 只从异常 cause 链中的真实 ErrnoException 判断空间错误, 防止循环 cause 链. 暂存/解压/普通系统写入接入统一 INSUFFICIENT_STORAGE 映射. 对已锁定 parser AAR 的固定空间预检消息做全等兼容; 任意源文本包含 ENOSPC 不被当成 errno.
- OEM 将 `INSTALL_FAILED_INSUFFICIENT_STORAGE:` 放在 generic failure 时保留该具体原因.
- 特权服务把结构化空间错误编码成精确的私有 IllegalStateException 标记, 主进程只在私有 Binder 调用边界识别. 私有 AIDL 末尾追加 `checkWriteStatus(sessionId)=11`, 原 transaction 不变, 继续执行插件 UID/session 归属检查.
- writer 先记录目的端异常再关闭 pipe 读端, 因而主进程收到 EPIPE 时即能查询真实空间原因, 不等待 Future 完成. 查询前后复查取消; 普通查询失败保留 INSTALL_FAILED, 不被误映射为内部错误或空间不足.

## 自动化与设备证据

首轮全量 JVM 261 项, 无失败/跳过, Debug/androidTest 构建通过 (`build/p6-first-build.log`, 25 秒). 本条新增 17 个 JVM 用例, 覆盖同包/跨授权/跨用户门禁, 不同包并行, 等锁取消/超时/失败释放/批量逆序, 暂存时限与并发回收, writer 原因发布时序, 私有标记作用域和错误消息精确匹配. 安全条目另新增一个 JVM 用例.

`StorageFailureDeviceTest` 五项在 API 24 / API 35 / API 33 通过, 使用真实 Android ErrnoException/EDQUOT, pipe EPIPE, Parcel 异常编码和私有 symlink. 写失败 Future 故意尚未退出时, 原错误已可读取; 另验证取消优先和非空间异常不被改类. 这是有界故障注入, 没有填满设备磁盘, 不冒充真实整盘耗尽测试.

| 设备 | 组合与结果 | 日志 |
| --- | --- | --- |
| API 24 x86 AVD `emulator-5554` | 安全 4 + 空间/清理 5, 9/9, 无跳过, 0.180 秒 | `build/p6-security-storage-api24.log` |
| Xiaomi 23046RP50C / API 35 / `968e9f18` | 安全 4 + 空间/清理 5, 9/9, 无跳过, 0.111 秒 | `build/p6-security-storage-api35.log` |
| Sony XQ-DQ72 / API 33 / `QV770340J7`, KernelSU Root | 安全 4 + 空间/清理 5 + 真实并发安装 2, 11/11, 无跳过, 5.460 秒 | `build/p6-root-api33.log` |
| Xiaomi 23046RP50C / API 35, Shizuku | 真实并发安装 2/2, 无跳过, 4.637 秒 | `build/p6-shizuku-concurrency-api35.log` |

真实并发测试固定使用自建无代码夹具 v1/v2, 校验 SHA-256/包名/版本/无 dex 或 so, 任意用户预存夹具或保留数据即拒绝操作. 使用真实 DescriptorInstallEnvironment/PrivilegedInstallEngine; 第一项实际写出字节后暂停, 第二项实际解析来源后连续观察 500 ms, 确认未创建平台会话或提前采样旧版本. 第一项成功并释放平台 session, 清理 item 后, 第二项才创建会话. 最终普通 PackageManager 和特权服务双读均为 v2.

- Root 串行运行: 第一平台 session `81006009`, 第二 `1184244908`, firstReleaseOrder=2 / secondCreateOrder=4, 第二项 previousVersionCode=1 / installedVersionCode=2.
- Shizuku 串行运行: 第一平台 session `678113348`, 第二 `286658281`, 同样 releaseOrder=2 / createOrder=4 和 previousVersionCode=1 / installedVersionCode=2.
- 等待项取消用例在两种授权下均确认 cancelledPlatformSessions=0, 第一项仍真实安装 v1, 随后重试真正更新到 v2, 没有遗留包锁.
- 两种授权均输出 CLEANUP: fixtureAbsentAcrossUsers=true / ownedSourcesRemoved=true / historyUnchanged=true / workersSettled=true. 核心测试不接入展示历史, 不安装或卸载用户应用.

提交前复核补充: EPIPE 的私有错误查询如果明确抛出 DeadObjectException, 保留既有 AUTHORIZER_UNAVAILABLE, 不降为普通 INSTALL_FAILED; 查询期间的取消仍优先. 新增真实 pipe + 注入 Binder 死亡的断言后, StorageFailureDeviceTest 共六项, API 24 / API 35 各 6/6 无跳过通过, 0.108 / 0.054 秒, 日志 `build/p6-storage-final-api24.log` / `p6-storage-final-api35.log`. 这是错误映射回归, 不计为真实进程死亡矩阵完成. 全量 261 JVM 与 Debug/androidTest 在 `build/p6-build37-verified.log` 再次通过; 首次文档标点守卫失败已修正为仓库规定的 ASCII 标点.

Root 设备由维护者本轮提供, 其在 KernelSU 为插件明确授予 Root 后才执行. G8441 既有 APK 选择记录未被改动.

## 2 GiB 与仍未覆盖的范围

固定大小缓冲的流式写入保留, 本轮没有增加整包堆缓存. 既有 P3 真实 2 GiB XAPK 证据见 `p3-notification-evidence.md`: 来源 2147501419 字节, 实际写入/安装总计 2147500874 字节, API 35 安装成功, 同次可见前台通知验收 30.269 秒. 本轮对此作源码复核, 没有重新运行大包或测新的堆峰值, 不把历史数据写成本轮性能测量.

P6.1 的宿主/插件/特权进程死亡与 Shizuku 服务停止矩阵, P6.3 全设备组合, P6.4 新的 100 MiB 三授权耗时/冷启动/PSS/Release 往返仍需继续. 所以仅本生产子项及 P6.2 按实际证据推进, 不宣称 P6 全部完成.
