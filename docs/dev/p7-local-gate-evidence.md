# P7.3 本地构建检查与 build 48 交付

2026-10-02更新: 原P2/P6剩余矩阵已收口, 最新签名构建和当前在线设备交付见 [build51本地交付](p7-matrix-complete-release-evidence.md). 本文以下保留2026-10-01的build48历史事实.

日期: 2026-10-01. 原 P7.3 的本地构建检查完成, 不代表 1.0.0 已公开发布或 P2/P6 完整设备矩阵已经通过. 本轮沿用维护者的仅本地提交指示, 没有推送, 创建 tag/Release 或提交官方索引.

## 构建与静态校验

`build/session48-platform-gate.log` 使用仓库要求的 Temurin 参数, 只输出一组平台决策. 实际选择为 Temurin `21.0.12.1+1`, AGP `9.3.2`, Kotlin `2.3.20`, R8 `8.13.19`, Gradle `9.5.0`; Java 21, min/target/compile SDK `24/37/37`. 禁止自动增加 build number 和自动更新 BUILD_TIME 的参数均已传入.

同一命令实际执行 `assembleDebugUnitTest`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `lintRelease`, `verifyNativePageAlignment`, `appendDigestToReleasedFiles`, 1分56秒成功:

- 268项JVM, 0失败, 0错误, 0跳过.
- Debug/androidTest与R8 Release装配通过, 原生库检查通过.
- Debug lint 0错误/22警告, 普通Release lint 0错误/23警告. 已有警告不描述为全零.
- `-PandroidTestRelease` 的独立Release测试APK及对应lint另用13秒构建成功, `build/session48-release-test-build.log`; 该测试源集下Release lint为0错误/29警告.
- Markdown 10语言/36生成物, 图标15项校验通过. 最终文档标点, `git diff --check`, 提交数与干净工作树在收尾再次核验.

完整268项JVM的XML报告保存在 `build/session48-all-jvm-results/`. 最后文档守卫的首次选择器误用路径而未找到测试, 更正完整类名后 `session48-final-doc-guard-verified.log` 通过; 未把未执行用例的选择器失败记作通过.

本轮生产变更是面向用户的十语言说明/内置发行历史, 安装引擎没有改动. 没有重复已有100 MiB性能采样, 进程死亡矩阵, 所有未选中instrumentation或CI全设备组合; 原证据保持原构建范围. 新默认记录守卫与实际设备验收另见 [P6补充](p6-default-history-evidence.md).

## 签名产物

| 项目 | 值 |
| --- | --- |
| 文件 | `autojs6-plugin-three-setup-installer-v1.0.0-8601e5e9.apk` |
| 版本 | `1.0.0`, versionCode `48` |
| 大小 | `1,902,783` 字节, 约 `1.815 MiB` |
| CRC32 | `8601e5e9`, 与文件名一致 |
| SHA-256 | `6cc923343072e52ad9cf27fcc939ce35bf8488394ad6b2f4b7f4e4cc8b9f3efe` |
| 签名 | apksigner验证成功, v2签名, 1个签名者 |
| 证书SHA-256 | `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213` |
| 内容 | debuggable=false, 无原生库, 无Debug探针入口 |

`build/session48-artifact.json`, `session48-signature.txt` 和 `session48-badging.txt` 保存产物与签名事实. Release测试构建后再次比较普通Release输出的SHA-256, 与收集到 `releases/` 的APK一致. 旧build 44产物核验已记录摘要后移入本仓库忽略目录 `build/session48-prior-artifact/`, `releases/` 只有上述一个APK.

## Release往返与设备交付

同一APK在API 24 AVD `emulator-5554`, API 35 Xiaomi `968e9f18`, API 37 AVD `emulator-5558` 各通过2/2独立平台runner检查, 无失败或跳过, `releaseChecks=2`, `releaseFailures=0`. 日志为 `build/session48-release-contract-<serial>.log`. 实际已安装APK摘要与表格相同; 检查签名/组件/无原生库, 跨UID/PID的INFO/INSTALLER元数据与Parcelable往返及非宿主拒绝. API 37再次读取实际页大小为16,384字节. 不将契约往返等同于该设备全部安装选项验证.

随后交付审计记录原八台设备均保留数据覆盖为同一build 48 Release: `emulator-5554`, `emulator-5556`, `emulator-5558`, `BH900ASK9E`, `QV710AF65F`, `QV770340J7`, `968e9f18`, `bek749scrwv4wo8h`. 每台重新读取SDK/ABI, 核验versionCode/SHA-256/debuggable=false/UID. 全用户固定Core/Spike夹具及保留数据不存在, 无插件安装前台服务; 安装许可模式与交付前相同. Xiaomi原6个/Sony Root原20个系统活动session ID保持, 其余设备为空. 四台原Shizuku server PID保持3077/27810/6403/20102, 三台AVD原无server的状态保持, 用户AVD继续运行.

原始资料在 `build/session48-delivery-audit/`: `before/` 保留最初快照, `delivery-baseline/` 为交付边界, `ready/` 是每台写入前复核, `restored/` 为覆盖后和Release往返后复核. 全量preferred XML及用户包集合与交付边界一致. 本轮此前XQ-AT72解析查询观察到的stopapp记录变化另按 [原始边界](p6-default-history-evidence.md) 记录, 不把之后的交付一致推导为整轮从未有状态变化.

首次交付预检在任何安装之前发现API 24新增 `io.github.supermonster003.autojs6.plugin.three.shell.terminal.test`, 因而拒绝沿用旧包集合继续. 本轮没有安装该包, 原快照完整保留, 新增项作为外部变化单独写入交付基线, 不清理或回滚. 更新前仍重新核对其他默认项/许可/系统session/server/插件APK摘要全部保持, 再执行本插件的覆盖安装.

## 提交与剩余原条目

本轮逻辑提交为build 45 `d4c7393` (P2.3矩阵补充), build 46 `c0edd96` (P6.3默认记录保护/实际Files), build 47 `dc9fe7e` (P7.2十语言说明/发行历史), build 48 (本地构建检查与交付证据). 最终 `VERSION_BUILD=48` 与 `git rev-list --count HEAD` 一致, 工作树干净. 本轮没有修改宿主或其他插件仓库源码.

下一步按原P2.3/P6.3继续: API 24需要应用UID可用的Root环境; 若在同一台API 33+真机验收三授权, XQ-DQ72仍需给本插件Shizuku授权; 实际低targetSdk绕过需要有原生拦截的API 34+特权环境, 当前HyperOS会直接接受targetSdk 22. XQ-AT72的默认页仍保护本插件已有最近使用记录, 需要维护者安排可验收基线, 本轮未取得或扩大清除许可. Redmi实际Files的系统安装器路由限制如实保留. 这些缺口不靠修改路线图或重新定义成功来关闭.

P7.3的远端仓库/tag/Release/索引/宿主推送各项继续未勾选. 恢复远端发布需要维护者明确解除仅本地提交限制, 并完成原矩阵及发行条件; 未据本地构建成功提前进入P8/P9.
