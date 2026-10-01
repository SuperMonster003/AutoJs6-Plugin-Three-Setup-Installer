# P8 1.1.0 / build 57 本地交付

日期: 2026-10-02, Asia/Shanghai. 本文件记录原 P8 五项实现后的最终包, 构建及设备交付, 不替代各功能证据中的实际测试版本和范围. P7 远端发布继续按维护者要求延迟.

## 最终产物

| 项目 | 值 |
| --- | --- |
| 版本 | 1.1.0 / build 57 |
| 文件 | `autojs6-plugin-three-setup-installer-v1.1.0-c2238464.apk` |
| 大小 | 1,965,043 字节 |
| SHA-256 | `d1300f2b1ac97c240bc02e58752909d1fa5b78a18397d3894ac5ce1aef8df31e` |
| CRC32 | `c2238464`, 与文件名一致 |
| 签名 | apksigner 校验通过, 单一 v2 签名 |
| 证书 SHA-256 | `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213` |
| 内容 | 非 debuggable, 无 native libraries, 无 Debug 探针入口 |
| 平台 | min/target/compile SDK 24/37/37, Java21 |

`releases/` 只保留该包. 旧 Release51 及未通过 gate 的候选先核验准确摘要, 再逐文件移入忽略目录 `build/p8-release-archive/`; 不删除未知包. `BUILD_TIME` 在本次发行固定为 2026-10-02, 之后构建继续关闭自动时间和构建计数更新.

## 构建与检查

最终完整运行时 312 JVM 用例全部通过, 无失败/错误/跳过. 三笔功能提交通过独立索引候选保持逐步能力边界: 第一笔只启用 Dhizuku/V2, 第二笔启用持久默认, 第三笔才启用 notification; 未实现的模式在中间提交明确拒绝. 前两笔隔离源树分别 297/307 JVM 全通过, Kotlin 与 androidTest 编译通过. 实际功能设备测试使用综合构建, 不声称每笔中间提交分别完成实机矩阵.

Temurin `21.0.12.1+1` 参数路径输出单组平台决策. Debug, androidTest, 签名 R8 Release, 独立纯 Java/Android Release 测试 APK及无原生库检查均成功. Debug 与普通 Release lint 最终各 0 错误/40 警告, 没有通过忽略 NewApi 检查或提高 minSdk 处理错误. 十语言36份文档, 15项图标, 文档标点与 Git 空白检查通过.

保留 gate 中的真实首次失败:

- `p8-release57-gate.log`: 新 Debug 死亡控制的 RecentTaskInfo 可空性编译失败, 改为安全访问.
- `p8-release57-gate-retry.log`: lint 发现 SessionInfo.getSize 在 API27 才公开. API26 改为缺少 size 读回, 仍可持有当前已创建的 lease, 但该缺失值不能被当作重启恢复的所有权证明; API26/27 缺少 origin nonce 的保守边界保持. JVM 补缺失 size 拒绝匹配.
- `p8-release57-final-gate.log`: 签名包和 JVM 已完成后, Debug helper 的 TaskInfo.taskId 被指出需要 API29; 增加版本判断及旧平台 persistentId. 补丁漏 import 的一次编译失败保留于 `p8-release57-lint-final.log`, 随后修正.
- `p8-lint-verified.log`: Debug/androidTest 和两种 lint 最终成功, 1分10秒. 上述后两项仅 Debug helper 变化, 不改变已经冻结的最终生产 APK. `p8-release57-contract-build.log` 独立 Release 测试 APK 8秒构建成功.

生产 APK 已核对 SHA/CRC/签名, 独立 Release 测试构建未更改该包. `p8-release57-signature.log` 保存签名输出. 测试器 SHA-256 为 `16cea22b4788c89664ed59548ca253b3892458241c50750d15809313f554cf43`.

## 同一 R8 包的设备检查

| 设备 | 实际范围 |
| --- | --- |
| emulator-5554 / API24 / x86 / Magisk25.2 | R8 main 实际 system UID/GID1000 的 read/set/read/幂等set/clear/read 六次全部成功, 实际持久 XML 和解析4/4; 独立跨UID契约2/2, 六类业务操作拒绝非宿主 |
| 本轮自建 emulator-5562 / API31 / x86_64 | 正式宿主UID的Rhino脚本协商V2, Dhizuku静默实装/卸载及持久set/clear均成功; 独立跨UID契约2/2 |
| Sony QV710AF65F / API31 | 通知实装/授权释放/系统重建使用各自固定Debug包, 最后保数据覆盖同一Release57并核对设备APK摘要与非Debug; 不扩大为该Release重跑全部通知流程 |
| 新 Samsung SM-A566B / API36 / 16 KiB | 独立跨UID契约2/2, 六类业务操作拒绝非宿主, 同包运行及无native检查; 此前Debug三项兼容/接口及实际通知许可拒绝共4项通过 |

API24 的契约 pluginUid/probeUid 为10293/10294, 往返总13ms; API31 为10148/10149, 总40ms; API36 为10320/10321, 总40ms. 三组均实际跨进程和UID, 读取安装包中的公共 Parcelable/AIDL 类, `OK (2 tests)`, 不能用 IPC 时间比较三台性能. 记录位于 `build/p8-root-release-contract/instrumentation.log`, `build/p8-release57-contract-api31.log`, `build/p8-samsung-f8cb29acf84f/release-contract.log`.

最终 R8 的正式宿主脚本 case `70dc213086b947108f89a047d60b686d` 返回 pluginVersion=1.1.0/contractVersion=2, 实际安装版本1且安装者com.rosan.dhizuku, 2725ms, 然后卸载成功. 持久默认set/clear均为true, 来源摘要与原普通默认保持. [集成证据](p8-integration-evidence.md) 明确宿主固定APK摘要及与其他会话的构建来源边界.

## 清理与交付

- API24 的用户默认/持久策略/session/原Magisk进程/权限/设置和历史保持; R8六操作前后已记录的私有文件完全相同. 原测试包未卸载, 依据上一轮统一受控产物保数据恢复测试器; 本机最初测试APK摘要未单独保存的边界及恢复依据见 [Root证据](p8-root-system-spike-evidence.md).
- Sony 的原8条历史字节, 空偏好和32条preferred保持; 两用户夹具/保留数据清理, 原Shizuku PID27810保持, 无源grant/FGS/session. 原测试APK精确恢复. 私有历史在覆盖Release前核验, 不声称非Debug包上再次直接读取过私有数据. [通知证据](p8-notification-evidence.md) 保留真实授权缺陷的定位, 并发撤权和系统重建结果.
- 新三星按新的设备身份与初始状态验收, 没有复用旧设备基线. 原owner和全部默认保持, 测试APK原本不存在, 核对摘要后已移除. 最终仅新增宿主和插件, 未移除原有应用; PAGE_SIZE=16384, 无夹具或FGS. 最终审计 `build/p8-samsung-f8cb29acf84f/final.json`.
- 自建API31 AVD在所有实装夹具清理, 无active session/插件服务后, 按准确AVD名关闭. 配置与证据留在忽略的构建目录, 用户原emulator-5554继续运行. 其他未参与本轮插件验证的设备保留各自版本, 不宣称全部在线设备均覆盖新包.

没有性能回归数字或完整新系统/ROM全矩阵声明; Root当前真实范围为上述API24/Magisk, Dhizuku真实安装为专用API31 owner, API26安装只具备实现/编译/API守卫证据. API34+ Dhizuku持久策略写前拒绝, 用户设备owner不被更换, 不关闭SELinux或系统安全开关. 未验证的commit后Dhizuku进程死亡窗口继续按不重放/不猜测原则处理.

## 本地提交

| Build | 本地提交 | 意图 |
| --- | --- | --- |
| 52 | 4fe4329 | P8先行授权与Inspector共享解析迁移记录 |
| 53 | 82587f9 | Dhizuku/V2及会话恢复 |
| 54 | 90ac063 | Dhizuku与Root持久默认 |
| 55 | 703092e | 通知交互/来源授权/进程生命周期 |
| 56 | 003ea16 | 十语言完整说明与跨仓库证据 |
| 57 | 本文件对应提交 | 最终同包gate与交付记录 |

所有相关仓库仅本地提交. 主插件最终 VERSION_BUILD=57 与HEAD可达提交数一致; 文档/d.ts/Inspector/Offline/Ace的提交和保留的既有工作见集成证据. 宿主并行会话的其他改动由其独立提交, 本任务没有代为归档或提交这些内容; 最后只读核验时宿主HEAD为77b5a3b0c5且工作区干净. Inspector/Docs/d.ts/Offline工作区亦干净, Ace只保留原有未跟踪releases/. P7远端发布不执行, P9保持原四条待实施.
