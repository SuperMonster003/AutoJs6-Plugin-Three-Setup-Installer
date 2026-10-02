# P9 来源配置运行时证据

日期: 2026-10-02. 范围: 来源配置的真实外部安装链路, 明确选项优先级, 重复来源保留, V3 请求门禁. 本文件不替代设置页 UI, 宿主脚本或最终 R8 发行验收记录.

## 设备与固定产物

- 专属 AVD: `Three_Setup_Dhizuku_P8_API31`, serial `emulator-5562`.
- API 31, 主 ABI `x86_64`, ABI 列表 `x86_64,arm64-v8a`, hardware `ranchu`, model `sdk_gphone64_x86_64`.
- Fingerprint: `google/sdk_gphone64_x86_64/emulator64_x86_64_arm64:12/SE1A.211212.001.B1/8023802:user/release-keys`.
- 本轮只有 user 0. 系统 device owner 为已有 `com.rosan.dhizuku`, UID 10147, 原进程 PID 1834. 沿用已有插件授权, 未重新申请权限或启动 Root/Shizuku server.
- 本轮固定 Debug APK 仍标识 `1.2.0 / build 60`, 是来源配置实现的中间验收包, 不是新的正式发行编号.

| 产物 | 字节数 | SHA-256 |
| --- | ---: | --- |
| 本轮主 APK | 8766353 | `110da527a842002d18badb451bec568721533e2d5512991eaeb8e57d1b7dbe3e` |
| 本轮 androidTest APK | 1661330 | `e8a8b23eb109e618c0820ce299a300a4949e129207a3c7969127230cd90a1b17` |
| 直接备份的原主 APK | 8766337 | `ec09e1173a2ab72c20b9439eb0b10d526cf7331e6f777af4fd4c9072f09c1101` |
| 直接备份并恢复的原测试 APK | 1640940 | `0c3f4caa116520000c9e826125dfdde3b66c89d5d758806eda69584093ce3d35` |

安装样本只使用 test assets 中固定的 `advanced-fixtures/v1.apk`, SHA-256 `bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0`, 包名 `io.github.supermonster003.autojs6.installer.advanced.fixture`, versionCode 1. 该样本有用于编译测试的固定代码, 没有 Activity/Service/Receiver/Provider 等自动入口; 本轮不执行其代码或使用其声明的权限.

## 执行范围与结果

统一构建先通过 391 项 JVM 测试, Debug/androidTest 装配与 lint; JVM 无失败/错误/跳过, lint 为 0 error / 51 warning. 其中本分工新增配置模型与 Core 时序 33 项, 持久化失败回滚 10 项. 这些数量为统一构建结果, 与下列设备测试分别统计.

本轮只执行 `InstallProfileRuntimeDeviceTest` 的 3 项和 `InstallerBinderDeviceTest#sourceProfileOptInRequiresVersionThreeEvenWhenFalse` 单方法, 使用 `profileRuntimeFixtures=true`. 没有运行整个 Binder 测试类. 设备结果为 **4/4 通过, 无跳过**, instrumentation 用时 17.838 秒.

| 用例 | 实际观察 | 证明范围 |
| --- | --- | --- |
| 本地默认 Root 被匹配配置覆盖 | 调用真实 `ExternalInstaller.start(..., options = null)`, 全局默认为不可用的 Root, 来源为 external. 读取固定 APK 的真实包名后命中包名前缀配置, 使用 Dhizuku 成功安装 versionCode 1. 请求明确选项集合为空, 最终交互仍为 AUTO, 没有插件确认选择 | 证明默认 Root 失败不会在读取真实包名及应用配置前阻断请求. 文件名始终为 `fixture.apk`, 不符合规则包名前缀, 匹配依据不是文件名 |
| 明确选项压过配置 | 命中配置声明 Root 与 `deleteSource=true`, 但调用方明确传入 Dhizuku 和 `deleteSource=false`. 最终实际通过 Dhizuku 安装成功, 有效选项保留 false, 结果 `sourceDeleteRequested=false`, `sourceDeleted=false` | 明确 false 与授权方式都保持优先. 固定来源摘要不变, 真实私有 provider 删除计数为 0 |
| 重复来源清理入口 | 同一批次的两个 content URI 完全相同, 或两个 file URI 规范化后为相同路径. 首项请求删除, 后项明确 false. 真实 `ExternalSources.deleteInstalled` 返回保留说明, 文件摘要不变, 第二项仍可打开, provider 删除计数为 0 | 该项直接验证生产清理入口, 没有进行两次真实安装, 没有生成批量历史. 不据此宣称完整双项安装链路已验收 |
| V3 来源配置标记门禁 | 精确执行已指定的 Binder 单方法并通过 | `applySourceProfiles` 即使明确为 false 也必须按 V3 协商. 不扩大为整类 Binder 结果 |

前两项各只安装一次自有固定样本, 测试后按包归属守卫卸载. 本轮没有安装失败样本或重新执行失败安装.

## 基线与恢复

设备写入前已备份原主包和测试包, 完整读取插件私有偏好/文件/恢复记录, preferred XML, device-owner 信息, 包权限与 AppOps, 全用户包列表, 系统安装会话及相关进程. 全部后续命令均明确指定 `emulator-5562`.

- 原 `installation_profiles.xml` 与 `installer_settings.xml` 均不存在. 测试通过私有独占锁及 durable journal 保存完整偏好映射, 类型与原 XML, 写入前和恢复前比较归属. 两项安装后均恢复原空映射及原文件不存在状态. 两份 journal 最终为 `restored`, 保留用于审计.
- 原安装历史有 24 条且全部终态, 没有达到会触发淘汰的上限. 使用 `FixtureHistoryOwnership(context, advancedPackage)` 仅移除本次精确 token 的单项历史: `cd235d41-1a84-4f61-a673-5f330e4b81aa` 和 `be899b02-3c33-4c1b-a2b5-21ae6cae2b4e`. 最终原 `history.json` 字节和 SHA-256 均相同, 原 24 条未删除或改写.
- 固定夹具在所有枚举用户的安装/保留数据列表中原本缺席. 清理后恢复缺席; 不卸载预先存在的应用. 随机自有源目录均已清理, 没有残留 Dhizuku 安装恢复记录.
- 原 active install sessions 为空, 最终仍为空. Dhizuku owner 身份, 已有授权和原 PID 1834 保留. Root 可用/运行/授权状态未改变. 插件最终没有前台服务.
- preferred XML 字节相同. 插件及 Dhizuku AppOps 的有效模式相同; 使用时间, 拒绝时间等审计字段自然变化, 不声称原始 AppOps 文本字节相同.
- 原测试 APK 已通过保数据覆盖安装恢复, 随后直接核对设备 `base.apk` SHA-256 与本机原始备份相同. 主包暂保留本轮固定 Debug, 等待主线程最终正式发行覆盖; 没有倒退主包或清除数据.

私有目录并非所有文件字节相同, 两项差异已保留证据:

1. `files/profileInstalled` 是框架维护的运行时 profile 标记, 覆盖安装/运行后更新.
2. 原有 14 个终态 `installation-ui/*.json` 恢复快照在本轮前已经超过到期时间. 生产 `InstallRecoveryStore.readValid/prune` 在写入新的恢复快照时删除过期快照. 原快照最大 `expiresAt=1790901965935`, 早于基线已存在的系统安装会话时间 `1790905989386`. 这是恢复窗口的过期清理, 原安装历史保持不变. 没有为追求字节一致而重新写入这些已过期状态.

## 保存失败边界

本轮另外修复 `SharedPreferences.commit()` 返回 false 但内存已经更新的保存边界. 只有文档仍精确属于本次唯一 revision 时才回滚该字段; 保留原类型/缺失和其他键. 原本不存在的空偏好文件只在证明回滚成功且完整映射为空后移除. 回滚失败或发现外部替换时, 使用稳定的不可读快照阻止应用未经确认的新默认, 并让后续明确保存或重置通过同一 CAS 身份恢复.

10 项纯 JVM 故障注入测试验证这些行为, 包括后续成功保存只提交一次并清除失败标记. 本轮 Android 3 项使用正常存储, 没有制造设备实际磁盘写入故障. 没有新增持久意向存储层; 在失败返回至回滚期间被强制终止的极短窗口, 仍依赖 Android SharedPreferences 的原子写盘/备份机制, 进程内失败标记不被描述为跨进程 journal.

## 本地原始证据

忽略目录 `build/p9-profiles-runtime-emulator-5562-e4a00827/` 包含:

- `baseline/`: SDK/ABI/AVD/owner 身份, 原 APK 及摘要, 偏好/历史/私有文件备份和系统基线.
- `fixed/`: 本轮主包及 androidTest 固定副本.
- `runtime-and-contract.log`: 完整 4 项 instrumentation 结果及每项设置, 历史, 会话恢复回执.
- `after/audit.json` 与前后 private-data tar: 内容差异与所有恢复检查.
- `expired-ui-baseline.json`: 14 个过期旧 UI 恢复快照的到期证据.
- `restore-original-test.log` 与 `final-delivery.json`: 原测试包恢复, 设备上摘要核验以及 Debug 临时交付状态.
