# P9 来源配置最终 Release 设备契约验收

日期: 2026-10-02. 正式候选为签名 Release `1.2.0 / build 63`. 本次只在 API 24 与 API 35 各执行平台专用 `ReleaseContractInstrumentation` 的 2 项检查, 合计 **4/4 通过, 无失败, 无跳过**. 未运行 Debug Binder 整类测试, 未安装行为夹具, 未操作本阶段由主线程占用的 API 31 设备.

## 固定 APK

| 产物 | 字节数 | SHA-256 |
| --- | ---: | --- |
| 最终主 APK | 2140911 | `d8ae073dfaaa155ce31540663f55395639be13b8d5074c57f6057ef62b5b5fbe` |
| 本次平台专用测试 APK | 17908 | `0b687359d18974903c8461d1823f9f7425a283ecbab9a8e0553211fda50ba154` |

主线程固定的原始目录为 `build/profiles-fixed-release63/`. 本分工先检查大小与摘要, 再复制到独立证据目录. 两台设备均保数据覆盖主包和测试包, 测试后恢复各自直接备份的原测试 APK. 最终两台主包均保留上表正式 Release, 并再次从设备读取 APK 验证摘要.

## 设备与实测结果

| 设备 | 平台 | 主包 UID / 测试进程 UID | 主进程 PID / 探测进程 PID | INFO / 完整往返耗时 | 结果 |
| --- | --- | --- | --- | --- | --- |
| `emulator-5554`, Android SDK built for x86 | API 24, `x86` | 10293 / 10294 | 31331 / 31363 | 45 ms / 200 ms | 2/2 |
| `968e9f18`, Xiaomi 23046RP50C | API 35, `arm64-v8a` | 10290 / 10292 | 21399 / 21479 | 22 ms / 65 ms | 2/2 |

两台均重新读取 SDK/ABI, 只有 user 0. API 24 fingerprint 为 `google/sdk_google_phone_x86/generic_x86:7.0/NYC/6696031:userdebug/dev-keys`. API 35 fingerprint 为 `Xiaomi/liuqin/liuqin:15/AQ3A.241006.001/OS2.0.9.0.VMYCNXM:user/release-keys`.

实际 runner 为 `io.github.supermonster003.autojs6.plugin.three.setup.installer.test/io.github.supermonster003.autojs6.plugin.three.setup.installer.release.ReleaseContractInstrumentation`, 使用 `am instrument -w`, 不依赖 AndroidX 或测试用 production keep 规则. 运行且通过的两项为:

1. `productionArtifactIsSignedNonDebuggableAndHasNoDebugEntryPointsOrNativeLibraries`: 确认设备主包为签名生产包, 非 debuggable, 无 debug 入口, native library 数量为 0. 设备回执包含版本 1.2.0/build 63, APK 大小及完整摘要.
2. `realCrossUidInfoAndInstallerMetadataRoundTripPreservesCallerChecks`: 真实跨 UID/PID 读取 INFO 与 INSTALLER 元数据, 两台均报告 `guardedOperations=6`, `nativeLibraries=0`. 这证明六项受保护操作拒绝了不具备调用资格的独立探测身份; 不等于执行六种安装行为.

这些检查验证正式 R8 产物与 Binder 契约. 来源配置实际应用和清理行为的证据见 [运行时记录](p9-profiles-runtime-evidence.md); 最终 API 31 宿主脚本往返见 [正式 Release 总记录](p9-profiles-release-evidence.md).

## 原测试包恢复

设备写入前直接备份旧 Release 1.2.0/build 60 主包以及各自测试包. 部署前再次读取设备摘要与备份比较, 确认没有拿旧缓存替代当前原包. 两台旧主包摘要均为 `00adf451b1875b5b652975a740657e3079e0954e786056bec3a05ca705822b33`.

| 设备 | 恢复的原测试包 SHA-256 | 字节数 | 恢复核验 |
| --- | --- | ---: | --- |
| API 24 | `8e253348315d83e0b77bd41dfe99ed4669c13ef63be01a3fa72b762602fd8c51` | 17708 | 保数据覆盖成功, 设备 APK 摘要与直接备份一致 |
| API 35 | `ea6edc22684f016f5b52d9f0a9e6cabd0e4c96097438b017250af42ac42bc5e0` | 17708 | 保数据覆盖成功, 设备 APK 摘要与直接备份一致 |

两台原测试包并非同一摘要, 没有相互混用, 没有卸载原测试包. API 24 的首次 `adb pull` 传输报路径不存在, 但只读 shell stat 确认文件存在; 随后使用 `exec-out cat` 读取同一精确包路径, 核对 ZIP 头及已知摘要后保存直接备份. 该传输差异不被表述为设备丢失了安装包.

## 前后状态比较

已保存两台完整公开基线和最终读取结果. 比较结果如下:

- 主包与测试包 UID 均不变. user 列表及各 user 的完整包列表相同.
- 主包和恢复后的测试包权限 `granted` 值及 flags 相同. AppOps 有效模式列表相同, 无新增或移除模式. 运行时间和访问审计等字段自然变化, 不要求它们字节一致.
- preferred XML 前后字节完全相同. 没有解析 chooser 或修改默认安装器.
- API 24 原 active install sessions 为空, 最终仍为空.
- API 35 原有 7 个 `com.miui.analytics` active sessions: `470261979`, `711650958`, `2044551330`, `539699548`, `881851089`, `512545872`, `707787350`. 前后整个 active section 相同, 全部原样保留. 初版摘要解析只识别 `Session` 而漏掉该 ROM 的 `Active Session` 标题, 后已按原始 dump 修正, 没有清理或修改这些会话.
- 系统历史安装账目自然增加主包覆盖/测试包覆盖/原测试包恢复记录, 不声称整个系统安装日志相同. 两台最终均没有插件服务或插件 FGS.
- API 35 原 Shizuku server 保持 shell 身份和 PID 20102, 原管理器 PID 20226 保留. 相关服务进程集合相同.
- API 24 原 Magisk daemon 保持 root 身份和 PID 1338. 验收期间另出现 Magisk 管理器 PID 31314, 其 busybox PID 31356 及管理器 `:root` 进程 PID 31376. 未停止这些第三方管理器进程, 未变更 Root 授权或 daemon. 此设备只声明原 server 身份保留, 不声明全部进程集合相同.

全程没有清除插件或测试包数据, 没有为读取私有文件而覆盖 Debug. 本轮没有读取 Release 私有安装历史或偏好文件, 因此不声称它们的字节完全相同. 两项契约测试没有安装来源或行为夹具, 也没有请求特权授权.

## 日志判读与证据路径

忽略目录 `build/profiles-release-device-baseline-6dd3d57e/` 保存两台实时基线, 固定 Release 副本和最终结果. 各设备子目录包括 `baseline.json`, 原始 package/AppOps/preferred/session/process dump, `release63-contract.log`, 三次保数据覆盖日志, `final/` 与 `final-delivery.json`. 原 APK 直接备份位于 `build/profiles-release-preflight-baca5a37/{serial}/`.

本地初版采集器误要求仅 raw 输出模式才显示的 `releaseFailures` 字段, 而实际按约定使用 `-w`. 两台原始日志均明确包含两项具名开始/成功状态及 `OK (2 tests)`. 已据原日志修正汇总, 保留 `release63-initial-collector.json` 和说明, 没有重新执行测试或安装. 该采集器断言不是设备测试失败.

最终汇总为 `build/profiles-release-device-baseline-6dd3d57e/final-delivery.json` 与 `release63-runs.json`. 本分工未运行 Gradle, 未修改版本或路线图, 未提交或推送仓库.
