# P6.4 Release 往返, 性能与体积证据

日期: 2026-10-01. 本文对应原 P6.4 条目, 记录同一签名混淆 Release 的设备往返, 100 MiB 单 APK 的三个授权路径, 特权服务首次连接与安装后空闲 PSS. 数值来自本轮原始日志, 不代表重复采样基准, 不替代尚未完成的完整 OEM 兼容矩阵. 历史记录的定向清理单独列于文末, 不以夹具卸载代替历史清理.

## 被测产物与 R8 边界

| 项目 | 实际值 |
| --- | --- |
| 插件 | 3-Setup Installer 1.0.0 / build 40 |
| 宿主正向调用端 | AutoJs6 6.8.0 / build 5303, 官方包名和 UID, Debug 专用受 `android.permission.DUMP` 保护的探针 |
| 最终被测 APK 副本 | `build/p6-tested-release40.apk` |
| 最终本地发行文件 | `releases/autojs6-plugin-three-setup-installer-v1.0.0-96bfec75.apk`, CRC32 `96bfec75`, 与被测副本 SHA-256 相同 |
| APK 字节数 | 1,890,319 字节, 约 1.803 MiB, 低于原预期 3 MiB |
| APK SHA-256 | `50b06cec175bd0a715179a4e09707ebf205154552d3370af48a8f37375ca976d` |
| 构建属性 | Release 签名, `debuggable=false`, R8 与资源压缩启用 |
| 原生库 | APK 中没有 `lib/*.so`, 不限制 ABI |

`-PandroidTestRelease` 选择独立 `app/src/releaseTest` 源集, 运行签名 Release, 不把 Debug APK 改名后当作 Release. 生产 `proguard-rules.pro` 没有新增测试专用 keep. 既有生产规则保留公开 `org.autojs.plugin.common.api` / `org.autojs.plugin.installer.api` 的 Parcelable 与 AIDL, Manifest 发现服务, Shizuku 服务, libsu 和 `RootService` 类名. 隐藏接口适配反射使用设备自身的 `android.content.pm.*` 接口和 Stub; 插件没有打包 `android.*` 框架存根, 不通过保留整套实现来绕过压缩. 下述 Shizuku/Root 的真正安装进一步覆盖了混淆后的私有 Binder 和隐藏接口路径.

测试 APK 的 `proguard-test-rules.pro` 只保留测试自己的类. 最终 runner 为纯 Java/Android 平台 `ReleaseContractInstrumentation`, 不依赖 AndroidX, Kotlin 或 JUnit 测试运行时. 远端探针在测试 APK 的独立 UID 和 `:release_contract` 进程运行, 从已安装 Release APK 加载公开契约, 核对契约 ClassLoader, 再真正跨 Binder 获取 `PluginInfo` 与能力 Bundle. 测试核对双端 PID/UID, Parcelable 字段, 五个非宿主操作的 `SecurityException`, 并确认生产 APK 不含 Debug 探针. 这些元数据和拒绝用例不被冒充为正向安装; 正向操作由后述官方宿主 UID 发起.

构建和运行入口如下, `deviceSerial` 必须使用实际选中的设备:

```powershell
.\gradlew.bat '-PandroidTestRelease' `
  '-Pautojs.gradle.build.number.auto.increment.enabled=false' `
  '-Pautojs.gradle.build.time.update.enabled=false' `
  :app:assembleRelease :app:assembleReleaseAndroidTest

$pluginPackage = 'io.github.supermonster003.autojs6.plugin.three.setup.installer'
adb -s $deviceSerial shell am instrument -w -r `
  -e class "$pluginPackage.release.ReleaseContractDeviceTest" `
  "${pluginPackage}.test/$pluginPackage.release.ReleaseContractInstrumentation"
```

每个最终日志都包含两项具名检查, `releaseChecks=2`, `releaseFailures=0`, `OK (2 tests)` 和 `INSTRUMENTATION_CODE: -1`, 无跳过. 最终五台设备读取到的 APK 字节数和 SHA-256 均与上表一致.

| 设备 | API / ABI | 最终结果 | 原始日志 |
| --- | --- | --- | --- |
| `emulator-5554`, Android SDK built for x86 | 24 / x86 | 2/2 | `build/p6-release40-contract-emulator-5554.log` |
| `emulator-5556`, sdk_gphone64_x86_64 | 31 / x86_64 | 2/2 | `build/p6-release40-contract-api31-host-present.log` |
| `QV770340J7`, Sony XQ-DQ72 | 33 / arm64-v8a | 2/2 | `build/p6-release40-contract-QV770340J7.log` |
| `968e9f18`, Xiaomi 23046RP50C | 35 / arm64-v8a | 2/2 | `build/p6-release40-contract-968e9f18.log` |
| `emulator-5558`, sdk_gphone16k_x86_64 | 37 / x86_64 | 2/2 | `build/p6-release40-contract-emulator-5558.log` |

API 31 初次 build 40 往返缺少宿主定义的 `org.autojs.permission.PLUGIN`, 无法绑定受保护组件. 安装正式同签名宿主 build 5303 并重新安装插件和测试 APK 后, 最终两项通过. 没有移除 Manifest 权限或放宽生产调用方校验. 旧失败日志 `build/p6-release40-contract-emulator-5556.log` 保留, 不与最终通过混合统计.

## 100 MiB 夹具与测量方法

`tools/build-performance-install-fixture.ps1` 生成独立测试签名的单 APK, 固定包名 `io.github.supermonster003.autojs6.installer.performance.fixture`, 标签 `3-Setup Performance Fixture`, versionCode 1, targetSdk 35. 夹具没有应用代码, dex/so, 显式权限或组件. `assets/installation-payload.bin` 使用 ZIP STORED, 实际负载为 104,857,600 字节, 不以压缩小文件或外层容器填充替代 100 MiB 安装来源.

| 夹具项目 | 实际值 |
| --- | --- |
| 单 APK 总字节数 | 104,866,213 |
| 固定负载 | 104,857,600 字节 / 100 MiB |
| APK SHA-256 | `58fc8cc7fa1f41e2b3a0e2825ade30ad6e3853f2879e1b8b4a6baa25d9425603` |
| 签名检查 | v1, v2, v3 验证通过 |
| 本地来源与元数据 | `build/performance-install-fixture-100/fixture.apk`, `fixture-metadata.json` |

`tools/run-release-performance.ps1 -Serial <serial> -Authorizer none|shizuku|root` 先验证全用户范围没有此包或保留数据, 再经官方宿主的受保护探针使用正式 `InstallerPluginHost.inspect`, `getUsers` 和 `openSession`. 宿主探针也验证固定包名/版本/体积, 无代码/权限/组件及来源摘要. JSON 证据核对 caseId, 官方宿主包名和实际宿主 UID. 不向插件注入替代 CallerGuard, 不通过 adb 安装夹具来代替插件安装.

三个阶段分别计时: 独立 inspect, 首次 getUsers 往返, 安装请求到成功结果. Root 另记录 `requestAuthorizerElapsedMs`, 不混入首次 getUsers. 特权安装使用 `silent`, none 使用实际插件确认与系统安装确认, 因而 none 安装总时间包含准备, 操作者确认等待和系统处理. 写入进度验证实际 `bytesWritten=totalBytes=104866213`; 返回结果为固定包名, versionCode 1, 对应 authorizer 和 `ok=true`, 并检查设备中的真实安装结果.

最终 driver 在电脑侧对 `adb exec-out run-as <host> cat <唯一私有来源>` 的二进制流计算 SHA-256, 使用固定 64 KiB 缓冲, 验证精确总字节数, 退出码和 stderr, 共用 60 秒期限. 不依赖旧 Android 设备是否带 `sha256sum`, 不进行文本解码或在内存中保留整份 APK. 单元自检覆盖非文本字节, 换行, 截断, 超长和取消; 本地完整 100 MiB 进程管道也验证摘要相同, 非零退出/错误输出被拒绝.

## 最终安装与服务冷启动样本

表中耗时单位均为毫秒. 这三种授权方式分别在不同设备运行, 硬件, API, ROM, I/O 和系统确认均不同, 不能根据这些单次样本将差异归因于授权方式, 也不计算跨设备平均值.

| 设备 / 授权方式 | inspect | Root 授权请求 | 首次 getUsers | 安装总时间 | 安装结果 durationMillis | 实际写入字节 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Xiaomi API 35 / Shizuku | 216 | 不适用 | 320 | 1,176 | 987 | 104,866,213 |
| Sony API 33 / KernelSU Root | 164 | 23 | 363 | 924 | 792 | 104,866,213 |
| API 24 x86 AVD / none | 551 | 不适用 | 7 | 9,694 | 9,249 | 104,866,213 |

Shizuku 和 Root 性能运行前只读进程列表确认没有本插件的 UserService/RootService; 之前契约测试留下的 `.test:release_contract` 属于测试 APK, 不是特权服务. 因此 320 / 363 毫秒是包含特权服务冷启动, 绑定, 只读握手及用户查询的首次 getUsers 往返, 不是仅进程创建本身的裸耗时. Root 的 23 毫秒授权请求独立记录, 使用已经授予的 KernelSU 权限. 随后的安装复用这次已建立的服务, 不把安装称为第二次冷启动.

最终三个 case 目录位于 `build/release-performance/`:

- `968e9f18-shizuku-performance-41f84b7e48f148cdb967fde19a1cdca8`.
- `QV770340J7-root-performance-7b4a71cf00b9484196fd525db856b685`.
- `emulator-5554-none-performance-1cfacab51e52416eaf1749cc06f43480`.

每个目录的 `probe-evidence.json` 保留原始结果和阶段时间, `device.json` 保留设备/API/ABI/UID/来源摘要, `commands.log` 保留操作和验证. 三份最终 `result.json` 均为 `passed=true`, `sourcePreserved` 为布尔 `true`, `fixtureAbsentAfter=true`, `probeSettled=true`, `enableRestored=true`, `privateRemoved=true`, `remoteRemoved=true`, `cleanupErrors=[]`.

## 空闲 PSS

以下为性能完成, 夹具与来源清理, 宿主偏好恢复后的两次空闲背景样本. API 24 样本在清理后至少等待 30 秒. 每台两次采样约相隔 5 秒, 对应 `build/p6-idle-pss/<serial>.json`, 同目录保留原始 `ps` 和 `dumpsys meminfo` 输出. 三台的 `*-services.txt` 均确认当时插件没有前台服务.

单位为 KiB. 合计只包含本插件主进程及仍缓存的本插件特权进程, 不包含宿主, 测试 APK, Shizuku server 或系统安装器. 特权进程在安装结束后仍可缓存, 空闲不表示所有相关进程已退出.

| 设备 / 路径 | 主进程样本 1 / 2 | 特权进程样本 1 / 2 | 合计样本 1 / 2 |
| --- | ---: | ---: | ---: |
| API 24 AVD / none | 23,758 / 19,770 | 无 | 23,758 / 19,770 |
| Xiaomi API 35 / Shizuku | 14,104 / 20,990 | 69,482 / 69,482 | 83,586 / 90,472 |
| Sony API 33 / Root | 11,193 / 14,093 | 44,700 / 44,700 | 55,893 / 58,793 |

Shizuku 进程名为插件前缀加 `:three-setup-installer-privileged`, Root 为插件前缀加 `:root:0`. 这些值是安装之后特定时刻的 PSS 快照, 不代表内存峰值, 稳定平均值, 长期泄漏分析或等硬件性能比较. driver 原有 `post-operation-meminfo.txt` 只是操作后即刻记录, 没有拿它替代这里单独等待并核对服务状态的空闲样本.

## 未计入最终通过的尝试

- 本轮开始的 build 38 是旧基线. 中间 build 39 和早期 instrumentation 的探查没有代替最终 build 40 同摘要验证.
- 初始测试 Manifest 使用无效进程子名 `:release-contract`, API 31 安装 test APK 时触发解析异常. 改成合法 `:release_contract` 后通过安装. 服务 `android:name` 一直是完整 Java 类名, 没有改变生产服务.
- 初始 Kotlin/AndroidX runner 因 AGP 扣除被测应用依赖, 但生产 R8 又裁掉测试才用到的 `androidx.tracing.Trace`, 在启动时出现 `NoClassDefFoundError`. 最终改用独立平台 Java runner, 没有往生产 Release 增加 AndroidX/测试 keep. 初期构建和崩溃日志保留于 `build/p6-release-contract-build*.log`, `build/p6-release-contract31-verified.log`; 最终 runner 构建见 `build/p6-release-platform-runner-build.log`.
- none case `01343d4ccefb4a5688f0a949ae42649b` 真实安装成功, 写入 104,866,213 字节, 总时间 35,871 毫秒. 末端验证发现 API 24 的 `run-as` 环境没有可执行的 `sha256sum`, driver 为 `passed=false`; 即使夹具/来源清理和偏好恢复已成功, 也不将此轮计为最终 driver 通过.
- none case `08e0503943934297b6b2a7cdf2c3221e` 也真实安装成功. 当时已启动的旧 driver 让 PowerShell `VoidTaskResult` 进入摘要输出, `sourcePreserved` 最后成了字符串数组, 其 `passed=true` 不满足严格布尔证据要求. 此轮不计最终 driver 通过. 抑制该无值任务的管道输出后, `1cfacab51e52416eaf1749cc06f43480` 重新完整运行, 来源摘要和各项恢复验证均通过.

## 16 KiB 设备与剩余兼容范围

`emulator-5558` 为 API 37 的 `sdk_gphone16k_x86_64`, 实际 `getconf PAGE_SIZE` 返回 `16384`. 同一 build 40 完成上述 Release 两项往返, 并实际打开独立首页, 图像和 UI 层级分别保存为 `build/p6-api37-home.png` 与 `build/p6-api37-home.xml`. 因插件不含原生库, 不存在需要修复的自身 ELF 对齐项; 这里记录的是在该 16 KiB 运行环境上的启动与公开契约行为. 它没有覆盖所有授权方式, 安装矩阵或所有 OEM, 原有未验收范围继续保留.

## 偏好恢复与历史清理边界

性能前置探针只暂时启用宿主中固定 Three 插件的 enable key. 原始存在性和值先落盘到 `enable-restore.json`, 校验生产信任策略和签名 fingerprints 后才准备测试; 不修改 trust, priority 或其它插件设置. finally 恢复独立于会话清理, 严格核对 caseId, 官方宿主 UID, 固定插件包名, 签名以及原 key 的存在性和值. 最终 Shizuku/Root 两台均从显式 `enabled=false` 恢复到同值; API 24 从原本缺少该 key 的默认启用状态恢复到缺少 key, 没有写入一个替代的显式 true. 三份恢复 journal 都为 `phase=restored`, `pending=false`.

成功后的 driver 仅卸载本轮归属明确的固定夹具, 删除唯一 case 目录下白名单文件和对应 `/data/local/tmp` 来源. 失败或无法证明结束时保留私有来源与 journal. 插件的安装历史不由这个 driver 删除: 历史使用随机 UI token 和 `token:0` ID, 没有保存宿主 requestId. 不能按 `p6-performance-<caseId>` 猜 token, 不能按包名清空所有历史, 也不能整份回滚旧历史覆盖期间其它新增记录. 定向清理需要依据 Debug 历史基线, 新增 ID, 固定夹具元数据及与成功结果相同的 `durationMillis` 确认归属, 再只删除明确的测试记录.

历史定向清理已完成. 三台设备先保存 Debug 私有历史基线, 最终成功后暂时覆盖同签名 Debug, 确认没有安装前台服务并停止本插件, 在无历史 writer 时处理私有文件. API 35 首次检查发现替换包后的进程已重新启动, 因而拒绝写入;再次停止并核验进程不存在后才继续. 每条待删记录必须是基线之外的新 ID, 固定夹具包/版本/itemIndex, origin=host, 对应 authorizer, completed 终态, 且 durationMillis 与宿主实际成功结果精确相等. 首轮两次 none 的安装本身也成功, 所以它们留下的历史同样按各自成功结果单独确认归属, 不因为外层 driver 问题而遗漏.

| 设备 | 只移除的历史 ID | 与实际结果匹配的 durationMillis |
| --- | --- | ---: |
| API 24 | `be6894b7-ee50-4ca1-813d-2cf33af526f4:0` | 35,430 |
| API 24 | `52aeabac-9b5e-4e1a-b5ad-26ee051c3826:0` | 39,729 |
| API 24 | `05a39f4c-bf48-49ad-ad45-033bd504207e:0` | 9,249 |
| API 35 | `38b47352-49db-479e-917a-c7a4cd4a387b:0` | 987 |
| API 33 | `02da96ee-9648-4e9c-81f7-0a224210b010:0` | 792 |

仅从当前历史移除以上五条. API 24/35/33 的原基线条数分别为 2/2/0, 清理后仍为 2/2/0, 每条保留对象均与原基线相等. 先保存 before/after 和摘要, 检查没有 AtomicFile 的未完成写入, 同目录临时文件读回一致, 替换前再次核对原文件摘要, 再原子替换并复读. 未清空整个历史目录或覆盖其它用户新增项. `build/p6-performance-history-cleanup/<serial>/plan.json` 均为 `applied=true`, `nonOwnedEntriesUnchanged=true`; API 24 移除 3 条, API 35/33 各 1 条. 夹具包/保留数据, 私有性能来源, 临时传输文件和 enable journal 均已按归属清理.

## 最终验证与设备状态

build 40 的 268 项 JVM 全量通过, 0 失败/错误/跳过; 最后一次包含本文和路线图的重新执行见 `build/p6-final40-docs-jvm.log`. Debug/androidTest 和签名 Release/独立 Release androidTest 构建通过. Debug lint 为 0 错误/22 警告, `-PandroidTestRelease` 的 Release lint 为 0 错误/29 警告; 后者包含 12 项 UnusedResources. 十语言 Markdown 36 个生成物, 15 项图标资源和两个设备 driver 的自检通过. 本地产物经过 v2 签名验证, CRC32 文件名及 SHA-256 与五台设备上被测 APK 相同, releases 目录只有这一份 APK. 摘要记录为 `build/p6-final40-report.json`.

清理后已覆盖回同一 build 40 Release, 包括 QV710AF65F 和 QV770340J7, 不保留生产包中的 Debug 探针. `build/p6-final-device-state` 再次核验 API 24/35/33: 本轮两个固定夹具在所有用户及保留数据中均不存在, 无安装 FGS, 宿主 case 目录为空. API 35 原 6 条会话和 Root API 33 原 20 条系统会话的 id 集合与基线完全一致; 两台原 Shizuku server PID 20102/6403 保持, API 24 临时 server 已恢复停止. API 24/31/37 模拟器保持运行, 未变更系统图形配置. 此处的本地发行包和提交均未上传 GitHub.
