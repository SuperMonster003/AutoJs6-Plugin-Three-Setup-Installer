# 2026-10-02 矩阵收口后的 build 51 本地交付

原P2.3三授权安装矩阵和P6.3设备矩阵已按 [授权后Core证据](p2-authorized-matrix-evidence.md), [三星实际低targetSdk拦截](p2-samsung-low-target-evidence.md) 和 [XQ-AT72默认页/Files及原设备映射](p6-approved-default-completion-evidence.md) 完成. 本文件记录随后同一签名Release的构建和交付, 不把此前Debug操作写成新Release的全安装矩阵重跑.

## 最终构建

版本 `1.0.0` / build `51`. 本轮没有修改生产安装引擎或公共契约; 更改为测试恢复守卫, 设备证据, 路线图与十语言当前发行历史日期 `2026/10/02` 及生成物. 宿主和其他插件仓库源码未改动.

`build/session51-platform-gate-verified.log` 的Temurin平台检查1分52秒成功, 单组平台决策为Temurin `21.0.12.1+1`, AGP `9.3.2`, Kotlin `2.3.20`, R8 `8.13.19`, Gradle `9.5.0`. 编译Java21, min/target/compile SDK为24/37/37. 自动构建计数和BUILD_TIME更新均关闭.

- JVM 268项, 0失败/错误/跳过, 完整XML保留在 `build/session51-all-jvm-results/`.
- Debug/androidTest/混淆Release/独立Release测试APK装配成功, 原生库校验通过. `session51-release-test-build.log` 为11秒成功.
- Debug lint 0错误/22警告; 普通Release lint 0错误/23警告; 独立Release测试源集下0错误/29警告.
- 文档10语言36产物, 图标15项, 最终文档标点和Git空白校验通过.

首次平台命令在新ROADMAP会话记录的中文顿号处触发标点守卫, 268项中该1项失败. 修正为ASCII逗号后完整重跑通过, 保留 `session51-platform-gate.log` 的原失败, 不改写为首次即全部成功. 最终新增证据也曾因一处中文顿号触发单项标点复检失败, 更正后最终单项复检通过, 原日志保留. 本轮没有重复无生产引擎变化的100 MiB性能, 进程死亡或其他未选中的全量instrumentation; 这些已有证据保留其原版本范围.

## 签名APK

| 项目 | 值 |
| --- | --- |
| 文件 | `autojs6-plugin-three-setup-installer-v1.0.0-cf864c74.apk` |
| 大小 | `1,902,783` 字节, 约 `1.815 MiB` |
| CRC32 | `cf864c74`, 与文件名一致 |
| SHA-256 | `547334493ecd76f9815167c2159e5730de5ac14cf310674d5cbd7dfa30ae29d0` |
| 签名 | apksigner验证成功, v2, 1个签名者 |
| 证书SHA-256 | `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213` |
| 内容 | debuggable=false, 无原生库, 无Debug探针入口 |

Release测试构建前后生产APK摘要一致, 实际设备安装的APK摘要也全部匹配. 旧build48核验原摘要后移入 `build/session51-prior-artifact/`, `releases/` 只保留本次单个APK. 摘要和签名原始结果见 `session51-artifact.json`, `session51-signature.txt`.

## 当前四台在线设备

`build/session51-delivery-audit/` 的before/ready/restored和summary保存每台交付前, 每次写入前及覆盖后/最终回归后的核验. API24 AVD `emulator-5554`, API31 Sony `QV710AF65F`, API33 Sony `QV770340J7`, API36 Samsung `localhost:44836` 均保留数据覆盖同一Release51, 实际版本/摘要/UID/无Debug核验通过.

全用户Core/Spike夹具和保留数据不存在, 无插件安装前台服务, 完整默认/最近使用XML和安装许可与交付基线相同. Sony API33的原20个系统session ID保留, 其余三个设备为空. Sony的原Shizuku server PID27810/6403保持; API24仍无Shizuku server, Samsung临时server已按独立journal停止. API24用户配置的Magisk环境保持. Samsung保留本轮新装的Shizuku管理器及插件授权, 不将这些授权测试准备描述为原先已有状态.

同一Release在API24/31/33各2/2真实跨UID/PID契约往返, `releaseChecks=2`, `releaseFailures=0`, 无跳过. 日志 `session51-release-contract-<serial>.log` 验证实际签名包/无Debug/无原生库以及INFO/INSTALLER元数据, Parcelable和非宿主拒绝.

Samsung没有安装AutoJs6宿主及其权限定义, 未为运行宿主契约额外安装宿主, 因而不计该机跨UID契约通过. 另用实际启用的launcher alias打开该Release首页, `Status: ok`, WARM, TotalTime159ms, 截图确认授权卡/默认安装器卡/无进行中任务和首页操作入口. `mActivityComponent`确认为HomeActivity, PAGE_SIZE=16384. 初次临时脚本只在topResumedActivity中查找HomeActivity而未接受alias, 因此断言失败; 结合真实target和截图后正确核验, 没有修改生产界面. 完成后按Back退出, 再次审计交付基线通过. 证据在 `build/session51-samsung-home/`.

本轮其他原设备未在线, 保留它们上轮版本与验收证据, 不冒充四台以外也更新了build51. 用户启动的AVD未关闭, 系统安全开关/用户应用未为测试清理或改造.

## 本地提交与后续

本轮按原小节拆为build49 `f4f640b` (P2.3), build50 `957ef95` (P6.3), build51 (最终本地gate/交付). 最终VERSION_BUILD与可达提交数均为51, 工作树干净. 初始工作区没有用户未提交内容, 本轮没有卷入其他仓库或会话的文件.

维护者前次列出的四项设备/授权条件均已消除, 无需再次请求相同授权. P0-P6与P7.1/P7.2/P7.3本地检查已完成原范围; Redmi系统接管Files等实测限制仍保留, ColorOS等范围外覆盖未作扩大. 原P7.3远端仓库/tag/Release/官方索引/宿主推送仍未执行: AGENTS 3.3及ROADMAP D32/Q8的仅本地提交指示尚未解除. 本轮未推送, 未建tag/远端Release, 不提前进入依赖P7的P8/P9.
