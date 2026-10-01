# P2.3 API 24 Root 与 API 33 Shizuku 授权后验收

日期: 2026-10-02, Asia/Shanghai. 维护者已完成 API 24 的应用可用 Root 环境和 Sony XQ-DQ72 的插件 Shizuku 授权. 本轮重新读取当前设备状态, 不沿用上一轮不可用/未授权结论. 原 P2.3 中缺失的这两组完整 Core 验收均通过, 生产代码和该两组Core测试源码没有为本次安装选项验收更改.

## 新基线与实际授权

开始时仓库为 `master@9369405`, `VERSION_BUILD=48`, 工作区干净. 两台设备原主包都是同一签名的非 Debug Release 48, 已安装 APK 重新读取 SHA-256, 均为:

`6cc923343072e52ad9cf27fcc939ce35bf8488394ad6b2f4b7f4e4cc8b9f3efe`.

| 设备 / serial | API / ABI / ROM 类型 | 插件 UID | 当前授权环境 |
| --- | --- | ---: | --- |
| AVD / `emulator-5554` | 24 / x86 / userdebug | 10293 | 维护者已配置 `/sbin/su -> magisk`, 原 `magiskd` PID 1338 运行; 本轮插件 libsu Root 请求与 RootService 绑定实际通过 |
| Sony XQ-DQ72 / `QV770340J7` | 33 / arm64-v8a / user | 10623 | 插件 `API_V23` 原已为 `granted=true`; 既有 Shizuku server PID 6403, UID 0, 本轮 UserService 绑定实际通过 |

测试复用已构建的 Debug 48 和 androidTest APK. Debug 与测试 APK 均经 apksigner 验证, 证书 SHA-256 为 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`, 与正式包相同. 覆盖安装主包时保留全部数据, 没有卸载主包或清除设置. 该两组Core验收没有单独运行 Gradle, 没有安装 Root 管理器, 没有修改 su 权限或系统配置, 没有启动或停止任何 Shizuku server.

在安装固定夹具前先单独执行 `AuthorizerDeviceTest#selectedPrivilegedAuthorizerStateRequestAndBindingRoundTrip`:

| 日志, 均在 `build/p2-authorized-matrix/` | 结果 | 耗时 |
| --- | --- | ---: |
| `authorizer-root-emulator-5554.log` | 1/1, 无失败或跳过; 请求 Root 成功, 并发请求与绑定完成, Binder 可用, 最终无保留的授权/启动 shell | 0.327 秒 |
| `authorizer-shizuku-QV770340J7.log` | 1/1, 无失败或跳过; 请求 Shizuku 成功, 明确断言 `expectedShizukuUid=0`, UserService 绑定完成 | 0.680 秒 |

两次正常请求都使用维护者已经配置的授权, 没有出现需要代理点击的新授权框. 用例最后释放本次特权服务并等待对应 Binder 结束, 不停止原 server.

## 完整 Core 结果

两组均显式选择 `CoreInstallMatrixDeviceTest` 的同一八项, 以 `confirmFixture=true` 开启固定无代码夹具操作:

- `testOnlyRequiresAnExplicitPrivilegedFlag`.
- `debuggableDowngradeRequiresTheFlag`.
- `lowTargetSdkBypassAndPre34Note`.
- `xapkInstallsBothBaseAndFeatureSplit`.
- `newInstallUpdateAndPrivilegedUninstall`.
- `releaseDowngradeRecordsTheActualRomPolicy`.
- `explicitInstallerAttributionIsRecorded`.
- `shellInstallerAttributionRecordsTheActualRomPolicy`.

| 最终日志, 均在 `build/p2-authorized-matrix/` | 设备 / 身份 | 通过 / 失败 / 跳过 | 耗时 |
| --- | --- | --- | ---: |
| `core-root-emulator-5554.log` | AVD API 24 / libsu Root | 8 / 0 / 0 | 8.760 秒 |
| `core-shizuku-QV770340J7.log` | Sony API 33 / Shizuku Root UID 0 | 8 / 0 / 0 | 13.461 秒 |

计数只使用带测试方法名的完整 instrumentation 终态, 不把用例中 `sendStatus(0)` 的证据日志当作通过. 本轮没有失败后降低断言或跳过不适用项.

两组共同确认:

- 固定 release v1 新装及 v2 更新均成功, 查询的安装版本正确; 这两步与生产卸载均显式要求严格 `silent`, 没有静默降级为系统确认.
- testOnly 包不带标志时返回 `INSTALL_FAILED_TEST_ONLY`, 使用 `allowTestOnly` 后安装成功.
- debuggable v2 -> v1 不带标志时返回 `INSTALL_FAILED_VERSION_DOWNGRADE`, 使用 `allowDowngrade` 后实际版本为 v1.
- XAPK 容器实际安装 base 与 `feature.extra`, 包管理器返回正确 split 名和版本.
- targetSdk 22 使用 `bypassLowTargetSdk` 时安装成功, 两台都低于 API 34, 因此结果明确包含该选项被忽略的 note. 这不是 API 34+ 拦截绕过证明.
- 显式安装者 `com.android.shell` 与插件包名均成功, 查询的安装者分别与请求值一致.

## 按身份记录的平台事实

API 24 userdebug 的 Root 与 Sony API 33 user 的 Shizuku Root 身份, 都接受带双降级标志的非 debuggable v2 -> v1, 最终版本均为 v1. Sony 的结果与此前同设备 KernelSU Root 结果一致. 不将此结果推广为所有 user ROM; G8441 API 28 / Xiaomi API 35 已记录的拒绝结果仍按其设备和身份保留.

本轮两组未显式指定安装者时, 新装和更新查询的安装者都是插件包名 `io.github.supermonster003.autojs6.plugin.three.setup.installer`. Sony 的 Shizuku 运行在 Root UID 0, 不能套用 API 35 Shizuku ADB UID 2000 的默认 `com.android.shell` 结果. 默认安装者归属与系统默认 APK 打开程序是两回事; 本轮没有修改用户默认安装器.

| 设备 / 身份 | 新装 | 更新 | 严格静默卸载 |
| --- | ---: | ---: | ---: |
| API 24 / Root | 70 ms | 129 ms | 155 ms |
| API 33 / Shizuku Root | 187 ms | 637 ms | 142 ms |

以上为本轮单次操作观察, 不作为重复采样性能基准, 也不把两台设备差异归因于授权实现.

## 数据, 设置与 Release 恢复

`build/p2-authorized-matrix/` 下 `before`, `debug`, `tested`, `restored` 四阶段保留原始系统输出和逐设备 JSON 审计. 基线是在本轮维护者完成授权后重新建立, 没有拿上一轮状态覆盖新的用户选择.

安装前守卫核验所有用户及 `pm list packages -u` 的保留数据, 固定 Core/Spike 包均不存在; 每个 Core 用例再次执行相同守卫并在结束后核验清理. 本轮两台设备只有 user 0, 所有原用户包集合在测试后完全相同, 包括维护者此前安装的终端及授权工具. 只安装/卸载测试 APK 内的固定无代码夹具.

测试后和 Release 恢复后分别核对:

- Core/Spike 夹具及其保留数据均不存在, 无本轮活动安装 session, 无安装前台服务, 无 `core-matrix-*` 来源目录.
- API 24 原活动 session 为 0; Sony 原 20 个系统/OEM session 的完整 ID 集合保持, 未放弃或修改它们.
- 两台设备所有 preferred/last-chosen 的完整 XML SHA-256 与基线相同, 不仅比较当前 APK handler.
- Debug 覆盖后立即记录的历史/偏好文件集合和 SHA-256 在测试后相同, API 24 为 3 个文件, Sony 为 2 个文件. 不声称通过 Release 沙箱读取了不可访问的私有数据.
- API 24 原全局未知来源值 `1` 保持; Sony 的 package/UID 安装 app-op 均为 `default`, 前后相同. 本轮只跑特权路径, 没有修改安装许可或创建许可 journal.
- 两台插件的 Shizuku `API_V23` 原已为 `granted=true`, 最终仍为 true; Sony 原 server PID 6403 保留, API 24 前后均无 Shizuku server. API 24 原 magiskd PID 1338 保留, AVD 保持运行.

最终保留数据覆盖原 `autojs6-plugin-three-setup-installer-v1.0.0-8601e5e9.apk`, 两台均再次验证 `versionCode=48`, `debuggable=false`, 插件 UID 不变, 已安装 APK SHA-256 等于开头的正式包摘要, 上述系统状态与包集合基线仍相同. 这一步是恢复当前正式构建, 不代表新增远端发布.

另外按本轮三星专项测试协调, 仅只读导出了 Sony 已装的 Shizuku manager 唯一 base APK, 版本 `13.6.0.r1086.2650830c` / 1086, SHA-256 `6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f`, 签名校验通过, DN `CN=Rikka`. 导出未改变 Sony 的管理器, server 或授权状态; 三星上的后续操作与结果另记专项证据, 不纳入本文件两组设备通过数.

## 与原 P2.3 矩阵的对应

此前有效证据分别见 [P6 Core 矩阵](p6-matrix-evidence.md) 和 [上一轮 P2.3 补充](p2-matrix-completion-evidence.md). 这些结果与本轮新通过合并后, 原两类设备的三身份安装选项组合已有以下对应关系:

| 原矩阵设备 | none | Shizuku | Root |
| --- | --- | --- | --- |
| AVD API 24 / `emulator-5554` | 既有 5/5 | 既有 Shizuku ADB 6/6 | 本轮 libsu Root 8/8 |
| 同一 Sony XQ-DQ72 API 33 / `QV770340J7` | 既有 5/5 | 本轮 Shizuku Root 8/8 | 既有 KernelSU Root 6/6 |

五项共同核心覆盖原要求的新装/更新/降级/测试包/XAPK, 特权组额外包含非 debuggable ROM 策略, 本轮八项再增加两种显式安装者归属. none 的测试包/降级用例通过指正确拒绝及特权标志返回 `AUTHORIZER_REQUIRED`, 不代表 none 具备特权能力. 未因本轮没有改动引擎而无理由重跑已经成功的四组.

本轮消除了上一轮 API 24 应用 Root 不可用和 Sony Shizuku 未授权两个具体缺口. 原 P2.3 的 API 34+ targetSdk 22 实际拦截/绕过条件须由对应平台的独立实测补齐; 本文件两台 API 24/33 和已有 HyperOS 无拦截结果不能替代它. 不增加, 分拆或丢弃原路线图条目, 不提前推导完整发布 gate 完成.
