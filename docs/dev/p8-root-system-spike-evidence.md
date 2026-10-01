# P8 Q9 Root/system 持久默认验证

日期: 2026-10-02, Asia/Shanghai. 原 Q9 要求验证 Root 经独立 system 身份是否能调用持久默认接口. 本轮在实际 UID/GID 1000 下成功添加四项 APK 持久首选, 验证系统持久记录和正常解析均为 4/4, 再仅清理本插件记录并完整恢复原基线. 这与 P0 中普通 root UID 0 被拒绝的结果不矛盾; 不能把仅 `id` 返回 1000 当作该能力已经通过.

## 设备与隔离方式

| 项目 | 实际值 |
| --- | --- |
| 设备 / serial | AVD, Android SDK built for x86 / `emulator-5554` |
| API / ABI | 24 / x86 |
| Root | Magisk `25.2:MAGISK:R` |
| SELinux | `Enforcing`, 全程保持 |
| 独立探针进程 | uid=1000(system), gid=1000(system), groups=1000(system) |
| SELinux 进程域 | `u:r:magisk:s0` |
| 当前用户 | 0, 枚举结果也只有用户 0 |
| 已安装插件 | 1.0.0 / build 51, debuggable=false |
| 已安装 APK SHA-256 | `547334493ecd76f9815167c2159e5730de5ac14cf310674d5cbd7dfa30ae29d0` |

探针为仅 Debug 源集中的 `spike/RootSystemDefaultProbe.kt`, 没有 Manifest 入口. 它只接受 `run` / `restore` / `inspect` 和 32 位十六进制 token, 固定目标为本插件 `ui.ExternalInstallActivity`, user 0, VIEW / INSTALL_PACKAGE 与 content / file 的四个 APK MIME filter. 程序开始前硬校验 UID/GID 1000 和此次限定的 API 24/x86.

没有覆盖设备上的 Release, 没有安装夹具或调用安装引擎. 已统一构建的 Debug APK 只作为独立 `su 1000 app_process` 的 classpath: 7,819,972 字节, SHA-256 `3ab8bf87c15c53ae1cde4bb71e5e0729bfbf27a7cf4bd7728b3ac0ee8f0e9f34`. 本轮没有在共享 libsu RootService 中调用 setuid, 没有改 SELinux 或 Root 管理器配置.

## 基线, 写入与恢复

原普通 preferred/last-chosen 为五项短信/Chrome 记录, 没有本插件或匹配 APK 的项. 原持久记录为空, 平台安装 session 为空, 没有 Shizuku server. 普通默认完整 XML 的 SHA-256 为 `fcef793563af6a52188a60ee15c847f904cec91fbd1b1a96bfa9429415582c8c`.

API 24 的 `preferred-xml --full` 只提供普通首选项. 探针通过系统自己的 `flushPackageRestrictionsAsUser(0)` 同步当前状态后, 只读解析 `package-restrictions.xml` 的持久首选段. 它不编辑该系统文件. 基线比较的是完整普通首选, 持久首选和四种解析; 不声称整个系统 XML 文件字节相同, 因为系统 flush 还会持久化原先仅在内存中的普通记录.

在任何持久写入前, `AtomicFile` 保存初始记录和 mutation ownership. 探针拒绝原有本插件的任意首选项或竞争 APK filter. 清理前再次要求所有非本插件记录和原普通首选保持, 本插件记录必须严格属于四个固定 filter, 无未知字段组合或重复项. 不通过清其他包, 普通 preferred 或最近使用来恢复.

本轮 token: `6f53ccc656d64334b7dad973c6c96702`.

| 阶段 | 普通首选 | 持久首选 | 四种公开解析 |
| --- | ---: | ---: | --- |
| before | 原 5 项 | 0 | 保存实际原解析 |
| locked | 原 5 项不变 | 本插件 4 项, 各有对应 action/scheme/APK MIME | 4/4 为本插件 ExternalInstallActivity |
| after clear | 原 5 项逐项相同 | 恢复 0 | 与 before 逐项相同 |

真实调用为 `addPersistentPreferredActivity(filter, component, 0)` 四次, 并在读取持久 XML 后检查实际结果. 最后只调用 `clearPackagePersistentPreferredActivities(本插件包名, 0)`. 结果 `success=true`, `fourPersistentFiltersVerified=true`, `restored=true`, 退出码 0; journal 的 before 与 after 对象完全相同. 没有使用普通 `addPreferredActivity` 冒充持久策略.

## 收尾

独立输出目录为 `/data/local/tmp/three-setup-root-system-6f53ccc656d64334b7dad973c6c96702`. classpath APK 推送后先核对摘要并设为只读; audit 子目录由 system 持有. 收尾再次核对 APK 摘要, 每个审计文件的 token/格式/摘要和唯一目录清单, 逐文件移除并删除空目录. 没有递归清空 `/data/local/tmp` 或删除未知路径.

- 普通 XML SHA-256, 持久空集, 平台 session 空集, 原 app-op 模式和全局未知来源值保持.
- Magisk daemon PID 1338 和原管理器 root 进程 PID 8221 保留; 没有本轮探针进程或安装前台服务.
- 已安装主包仍为上文同一非 Debug Release 51, 实际摘要一致.
- `build/p8-root-system-spike/` 保留 `before.json`, `after.json`, `probe-journal.json`, `run-stdout.log`, `removed-files.json` 和 `state.json`; 最终驱动 phase=`cleaned`, baselineRestored=`true`.

本结论是 API 24 / Magisk 25.2 / Enforcing 的实际能力证据. 没有重启设备验证持久策略跨重启, 没有推广到所有 Root 管理器或 OEM. 其他 ROM/SELinux 若拒绝切换身份, 读取政策或框架调用, 应明确返回不可用, 不关闭安全设置来制造成功.

## 生产桥约束

生产接入使用独立 `priv/RootSystemDefaultMain` 与 `auth/RootPersistentDefaults`, 仍由原 UID 0 Root shell 只负责启动, 不更改 RootService 身份. 固定闭集为 set / clear / read, 只支持当前 user 0, 目标包/组件/四 filter 固定. 仅在四种持久匹配和正常解析均通过时返回开启成功, 清除后验证本插件持久项为空; 原普通默认仍可能指向插件, 因此清除持久策略不应要求普通解析也变成别的应用.

子进程握手包含实际 UID/GID 1000, PID 和 startTicks. 应用预先创建并持有其 UID 所有的三个普通私有文件, 原 Root shell 在启动 system 子进程前打开 control / output / guard. 子进程验证三个文件描述符的类型和属主, 在 guard 上获取 OS 文件锁; 不支持保留这些描述符的 Root 管理器会在写入前失败. prepared 回执先同步到磁盘, 应用再保存 commitSent 并发送明确 commit; 操作输出限 64 KiB, 子进程期限为 8 秒, 应用总期限为 20 秒并另有 3 秒清理宽限.

取消和超时不重放设置操作. 未明确 commit 前取消不得写入; commit 已验证完成但与取消竞态时允许返回已经生效的结果. 异常回滚只能撤销可证明本次从空集新增且每个 filter 唯一的策略, 要求原普通记录和全部其他包持久项均未变化. 它不能删除本插件原有策略或其他包的默认项. 硬进程死亡或系统 Binder 挂起不能保证补偿完成, 未确认结果保留私有审计并报错, 不能写成已恢复.

竞争 APK 策略的保护包含 provider / path 专用 filter, 以及 application/* 和 */* MIME, 不以一个恰好不匹配的探针 URI 证明不存在竞争策略. 新设置拒绝已有部分或未知的本插件策略. 原始 filter XML 必须同时与 Android 的读取/序列化往返及当前平台固定 filter 的序列化结构等价, 未知属性/子项不能因框架读取时忽略而被当成已知策略; 外层 item 的未知数据也受保护. 已有完整的四类已知策略允许重复记录, 再次设置只验证并返回四类匹配, 不追加或去重; 显式清除允许全部为已知 filter 的集合. 部分失败回滚仍拒绝重复项, 不能把并发新增的记录当作本次所有.

被动状态查询不启动 Root shell, 不把本地上次成功配置回执当作当前真实持久状态. 开启结果才使用本次 system 子进程的四种持久匹配及四种正常解析证明; 清除只确认持久策略撤销. auto 从用户允许且可用的授权顺序选择 user 0 的 Root 或受支持的 Dhizuku; none / Shizuku 不能替代持久默认接口, 显式身份失败不切换到其他身份. SELinux, ROM 策略存储格式, 隐藏 API 或 Root 管理器不支持时明确失败.

## 应用生产桥首次验收

同一 API 24/x86/Magisk/Enforcing 设备上, 保数据覆盖统一构建的中间 Debug 51, 执行 `RootPersistentDefaultsDeviceTest` 的两项 instrumentation, 结果 2/2 通过, 无跳过, 用时 5.537 秒. 实际走应用 `RootPersistentDefaults` 到独立 system main, 包括开启后持久匹配 4/4 与普通解析 4/4, 再次设置幂等, 新设置在 prepared 后/commit 前取消不写入, 已有四条策略在取消时保留, 显式清除后归零. 另一项使用实际 Android IntentFilter 证明受限 provider/path 和通配 MIME 的竞争守卫.

| 产物 | SHA-256 |
| --- | --- |
| 中间 Debug 51 | `e6aa6e5685b31a32a0cb3f76aba3be9d6a5af5cccbb5c2d106b0b5117cbf27a1` |
| 对应 androidTest | `9ffc932a7f0b2602cc90cb18fd913291cd31c9699549afd90f38ed2b4be7d8ef` |

`build/p8-root-system-production/production-instrumentation.log` 保存成功记录. 首次人为输入了错误的测试类路径, 产生 initializationError, 没有进入 Root 方法; 原记录保留为 `production-instrumentation-first.log`, 不计入成功结果.

测试后完整普通 XML 摘要仍为上文原值, 持久项与系统 session 均为空, 原 UID 10293, Magisk daemon/管理器 Root 进程, app-op 模式, 全局未知来源值, 设置与安装历史摘要保持. 唯一新增空 guard 经确认基线不存在, 子进程均已结束, 文件摘要为 SHA-256 空串摘要后移除, 再删空目录. 回盖同签名原 Release 51 后, AndroidX `profileinstaller_profileWrittenFor_lastUpdateTime.dat` 因应用更新自然刷新, 其余既有私有文件保持; 不覆盖该框架记账文件来制造全目录字节相同. 各阶段审计与精确清理依据保留在同一 ignored 证据目录.

## 重复策略兼容与最终 Debug 验收

统一构建的 `RootPersistentDefaultProtocolTest` 为 5/5 通过, 无跳过, 验证固定命令和引用, 数字/身份边界, 旧策略保护, 重复项不纳入自动回滚, 以及清除后普通默认仍可能保持的语义. Debug 1.1.0/build 52 在同一设备执行六项 instrumentation, 6/6 通过, 无跳过, 用时 4.066 秒. 操作前重新读取 API 24/x86/Enforcing 和原插件 UID 10293, 固定两份产物到证据目录后才保数据覆盖.

| 产物 | 字节数 | SHA-256 |
| --- | ---: | --- |
| Debug 1.1.0/build 52 | 7,597,581 | `f4ca0458797fd458a9c1f68b8bafb8eaf71478940c2048bd862b1b089ead9130` |
| 对应 androidTest | 1,599,663 | `1abbfcab9b3c5b318469000e9b661ca0d5b2f96ea56085b45e3b3ff4163a892c` |

三项 Root 用例覆盖实际应用生产桥, Android IntentFilter 竞争匹配, 以及原始 XML 的保守归属判定. 三项兼容/安全用例分别证明 API 24 不初始化仅支持 API 26+ 的 Dhizuku API, V1/V2 协商保留版本 1 envelope, 正式 Binder 对非宿主 UID 的全部业务操作和回调前置拒绝.

重复策略流程从没有持久记录的已保存基线开始. 应用先创建四个已知 filter 并证明持久和正常解析各 4/4, 再次设置不追加. 仅 Debug 的固定 `RootDuplicateDefaultFixture` 独立以 system UID/GID 1000 运行, 要求系统全部持久项恰好为本测试四条, 每条再添加一次, 并证明八条中每类恰好两条. 它不接受任意包, user, 组件或 filter, 不能继续复制已经重复的集合.

随后真实生产桥读取结果为 persistentMatches=4, resolvedMatches=4, ownedFilters=8. 再次开启仍返回四类成功且 ownedFilters 保持 8, prepared 后/commit 前取消也保持 8, 最后显式 clear 验证 ownedFilters=0. 新设置取消不写入的用例也通过. XML 用例对四种固定 filter 分别附加 filter 属性, action 属性和未知子项, 共 12 种原始内容全部拒绝认领; 这项只读测试没有向系统注入未知策略.

收尾比对原普通 XML SHA-256 不变, 持久项和平台 session 为空, 原 Magisk 进程/app-op/全局来源设置不变. 所有既有私有文件的路径集合及摘要逐项相同, 包括设置, 历史, 旧 P5 审计和本轮未刷新的 AndroidX profile 记账. 本轮唯一新增空 guard 在确认无 helper 进程后精确移除, 空目录也已删除. `build/p8-root-system-production/final-debug-state.json` 为 phase=`cleaned-debug-installed`, baselineRestored=`true`; 完整结果见同目录 `final-debug-instrumentation.log` 与四份 `final-debug-*.json` 审计.

## 最终签名 R8 main 验收与交付

最终 APK 为 `autojs6-plugin-three-setup-installer-v1.1.0-c2238464.apk`, versionName=1.1.0, versionCode=57, 1,965,043 字节, SHA-256 `d1300f2b1ac97c240bc02e58752909d1fa5b78a18397d3894ac5ce1aef8df31e`. 独立执行 apksigner 验证通过, v2 签名为 true, 单一签名证书 SHA-256 为 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`. 先固定完整 APK 到 `build/p8-root-system-r8/plugin-release57.apk`, 再按实际摘要保数据覆盖.

重新核对 API 24/x86/Enforcing, UID 10293, 已安装摘要和 debuggable=false 后, 固定驱动以已安装 Release 的实际 `pm path` 为 classpath 运行 `priv.RootSystemDefaultMain`. 没有加载 Debug 副本, 没有给生产加入额外测试 keep. 每轮只接受固定 read/set/clear 与新的 32 位 token, 三个私有普通文件的属主均为插件 UID; 驱动在发送 commit 前把控制日志保存到电脑磁盘, 并逐轮核对实际 UID/GID 1000, `/proc` PID/startTicks 和 `u:r:magisk:s0`.

| 操作 | 本插件持久项数 | 四类持久匹配 | 四类正常解析 | 结果 |
| --- | ---: | ---: | ---: | --- |
| 初始 read | 0 | 0/4 | 0/4 | success, settled |
| set | 4 | 4/4 | 4/4 | success, settled |
| 独立 read | 4 | 4/4 | 4/4 | success, settled |
| 再次 set | 4 | 4/4 | 4/4 | success, settled, 不追加 |
| clear | 0 | 0/4 | 不查询, -1 | success, settled |
| 最终 read | 0 | 0/4 | 0/4 | success, settled |

locked 阶段还通过独立只读审计保存实际 `package-restrictions.xml`, 确认持久段有四项, 原完整普通 XML 摘要和系统 session 不变. 每次 DONE 后等待已核实的对应子进程退出, 没有重放任何策略写入. 六次控制/输出文件逐一归档并核对原始字节摘要, 只移除本轮随机目录中的确切文件及最后的自有 guard/空目录.

`build/p8-root-system-r8/driver-state.json` 最终 phase=`cleaned`, operations=6, baselineRestored=`true`. 原普通 XML SHA-256 仍为 `fcef793563af6a52188a60ee15c847f904cec91fbd1b1a96bfa9429415582c8c`, 持久项和平台 session 均为空, 原 Magisk 进程和 app-op/全局来源设置保持, 无本轮 system helper 或插件安装前台服务. R8 六次操作前后的全部既有私有文件路径与字节摘要一致. 从 Debug 覆盖到 Release 这一步仅 AndroidX profile 更新时间记账自然变化, 设置/历史/旧审计文件均保持, 没有人工还原框架记账.

最终再次只读核验 SDK/ABI/已安装包/摘要与服务状态, 设备保留同一非 Debug Release 1.1.0/build 57. 交接证据为 `build/p8-root-system-r8/handoff.json` 与 `handoff-services.txt`; 系统 before/locked/after/handoff 审计位于 `build/p8-root-system-production/r8-*.json` 和对应 XML.

证据范围: Debug 六项包含完整应用 `RootPersistentDefaults` 的授权桥, 取消, 重复策略及兼容/安全用例; 最终签名 R8 证明保留的真实 main 与隐藏系统调用可以完成以上六次往返. R8 驱动直接管理私有控制文件, 不把它扩写为最终 Release 上全部应用 UI/宿主 Binder 调用链与硬进程死亡窗口均已实测. 本轮未重启设备, 未验收其他 API/Root 管理器的同等能力, 既有平台和中断限制继续适用.

## 同一 Release 的独立跨 UID 契约补验

主包没有再次安装或修改. 在上述同一 Release 57 上切换独立测试 APK, 使用仅 Java/Android 平台类的 `release.ReleaseContractInstrumentation` 执行完整两项检查, 2/2 通过, 无跳过. 新测试 APK 的 SHA-256 为 `16cea22b4788c89664ed59548ca253b3892458241c50750d15809313f554cf43`.

| 项目 | 实际值 |
| --- | --- |
| 插件 / 探针 UID | 10293 / 10294 |
| 插件 / 探针 PID | 22423 / 22439 |
| INFO 往返 / 全部往返 | 4 ms / 13 ms, 仅本次观察 |
| 发行产物 | 同一 SHA-256, build 57, 非 Debug, 无 debug 组件或 native library |
| 公共协议 | INFO/INSTALLER Parcelable 与 V1/V2 能力从已安装 R8 APK 读取成功 |
| 拒绝边界 | authorizer, users, default, persistent, inspect, session 共六类, 含 `setDefaultInstallerV2`, 均在解码/分配前拒绝非宿主 UID |

本机 test 包预先存在, UID 为 10294, 没有将其当作本轮新包卸载. 切换前从设备导出当前 Debug52 测试 APK, 摘要为 `1abbfcab9b3c5b318469000e9b661ca0d5b2f96ea56085b45e3b3ff4163a892c`, 与先前固定副本一致. 测试后保数据覆盖恢复上一轮统一 Release 测试器, 摘要为 `8e253348315d83e0b77bd41dfe99ed4669c13ef63be01a3fa72b762602fd8c51`, UID 和此次切换前的测试包私有文件摘要保持.

恢复依据有明确边界: P8 开始时没有为 5554 单独记录原测试 APK 摘要. 使用的是 `build/p8-notification-QV710AF65F-64e739a0/original-test.apk` 的已保存受控副本, 以及 `build/session51-release-test-build.log` 和 `session51-release-contract-{emulator-5554,QV710AF65F,QV770340J7}.log` 所证明的同轮统一测试产物与三机各 2/2 记录; 其中 5554 原探针 UID 也是 10294. 这不是对本机 P8 初始 APK 字节的直接比对证明.

收尾主包版本/摘要/UID/非 Debug 状态和全部普通/持久默认, 系统 session, 原 Root 进程, app-op/来源设置, 既有私有文件摘要与本次测试前逐项一致, 没有前台服务或遗留跨 UID 探针进程. `build/p8-root-release-contract/state.json` 为 phase=`verified-restored`, mainBaselineUnchanged=`true`; 原始输出见同目录 `instrumentation.log`. 本地结果解析最初额外期待 `-w` 未输出的 `releaseFailures` 汇总键, 在实际 `OK (2 tests)` 后产生解析假失败; finally 已正常恢复测试包, 随后依据完整原日志和逐项成功状态修正解析并只读复核, 没有重跑测试或重放业务操作.
