# P6.3 兼容矩阵进展

日期: 2026-10-01. 本文保留原路线图的设备范围, 汇总本轮安装/更新/卸载, 外部入口和默认页的真实结果. 原设备的 none 新装/更新已有证据, 两台手机的默认项保护及各 OEM 实际文件管理器路径仍有边界, P6.3 整体继续保留未完成. 补充 Root 设备和 API 37 元数据往返不替代原设备的完整验收.

## API 37 / 16 KiB 页

维护者手动启动 `AVD_API_37.1_16K`, 本轮映射为 `emulator-5558`. 只读查询确认:

| 项目 | 实际值 |
| --- | --- |
| model | `sdk_gphone16k_x86_64` |
| API | 37 |
| ABI | `x86_64` |
| `getconf PAGE_SIZE` | 16,384 |
| 安装前插件 | 不存在 |
| 安装的插件 | 1.0.0 / build 40, 签名非 Debug Release |
| APK | 1,890,319 字节, CRC32 `96bfec75` |
| SHA-256 | `50b06cec175bd0a715179a4e09707ebf205154552d3370af48a8f37375ca976d` |

安装成功后, 独立 Release runner 的两项检查均通过: APK/导出组件/签名/无原生库, 不同 UID/PID 的 INFO/INSTALLER 元数据和 Parcelable 往返以及五项非宿主拒绝. 原始日志为 `build/p6-release40-contract-emulator-5558.log`, `releaseChecks=2`, `releaseFailures=0`, 无跳过. 该 AVD 已有宿主 build 5298, 本轮没有更新它或把元数据检查当作满足最低宿主 5299 的正向安装验证.

通过启用的 `AdaptiveAutoIconAlias` 实际打开 HomeActivity, `am start -W` 为 Status=ok / LaunchState=COLD / TotalTime=594 ms. 截图 `build/p6-api37-home.png` 和层级 `build/p6-api37-home.xml` 已复核, 首页显示授权状态, 默认安装器状态, 空任务/历史与添加按钮. 没有崩溃或替换为 Debug 页面. 插件无原生库, 自身 ELF 对齐不适用; 设备真实运行页大小为 16 KiB, 并非仅查看 APK meta-data.

原 P6.3 对 API 37 特别限定为插件本身安装和运行, 本行已满足这一范围. 不从此推导该 AVD 的 Root/Shizuku, 安装选项或所有外部入口已测试. 模拟器保持运行, 没有修改 GPU/系统配置或停止维护者的 AVD.

## 本轮设备事实表

本轮 Core, 外部入口和默认页使用 Debug instrumentation. `CoreInstallMatrixDeviceTest#newInstallUpdateAndPrivilegedUninstall` 的一个用例实际安装固定无代码 v1, 再更新到 v2; 特权分支还通过生产卸载引擎完成严格 `silent` 卸载. none 分支的包移除仅为测试收尾, 不计作本轮 none 生产卸载验收. 表中的通过数均按实际选定方法计数, 不把不适用方法或保护性跳过算作通过.

| 原设备 / serial | none 新装/更新 | 本轮特权静默安装/更新/卸载 | 显式外部 Activity / 取消 | 默认安装器页 |
| --- | --- | --- | --- | --- |
| AVD API 24 / x86 / `emulator-5554` | 统一 Core 5/5 中完成 | Shizuku ADB, 统一 Core 6/6 中完成 | 1/1 | Shizuku 真页面锁定/解锁 1/1, 基线恢复 |
| Sony G8441 / API 28 / arm64-v8a / `BH900ASK9E` | 1/1, 含已授权 Play Protect 扫描及明确安全结果确认 | libsu Root, 1/1 | 1/1 | 保护性跳过, 保留已有 APK preferred/last-chosen |
| Sony XQ-AT72 / API 31 / arm64-v8a / `QV710AF65F` | 1/1 | Shizuku ADB, 1/1 | 1/1 | 保护性跳过, 保留本插件已有 preferred/last-chosen |
| Redmi 22120RN86C / API 33 / arm64-v8a / `bek749scrwv4wo8h` | 1/1 | 没有运行中的 Shizuku 或 Root 能力, 不适用 | 1/1 | 无可用特权身份, 特权锁定不适用 |
| Xiaomi 23046RP50C / API 35 / arm64-v8a / `968e9f18` | 统一 Core 5/5 中完成 | Shizuku ADB, 基础操作及两项安装者归属共 3/3 | 1/1 | Shizuku 真页面锁定/解锁 1/1, 基线恢复 |
| AVD API 37 / x86_64 / `emulator-5558` | 按原文限定, 使用上节 build 40 插件安装/运行证据 | 原限定范围未要求 | 原限定范围未要求 | 原限定范围未要求 |

Sony XQ-DQ72 / API 33 / KernelSU Root / `QV770340J7` 是额外补充设备. 本轮统一 Core 6/6 是 Root 安装选项的补充证据, 加上此前 P5 默认页和 P6 进程/性能证据, 仍不替代 Redmi 的原 P6 行. 上表的特权身份只表示实际执行过的路径; 设备已有另一个 Shizuku server 不等于本轮该身份也已执行同组测试.

## 统一 Core 与各真机基础操作

none 的五项为 `testOnlyRequiresAnExplicitPrivilegedFlag`, `debuggableDowngradeRequiresTheFlag`, `lowTargetSdkBypassAndPre34Note`, `xapkInstallsBothBaseAndFeatureSplit`, `newInstallUpdateAndPrivilegedUninstall`. 特权六项在此基础上增加 `releaseDowngradeRecordsTheActualRomPolicy`. 测试包和降级的 none 分支验证平台拒绝及特权标志不可用, 不把拒绝写成成功安装.

| 最终日志, 均在 `build/` | 设备 / 身份 | 通过 / 跳过 | 完整用例组耗时 |
| --- | --- | --- | ---: |
| `p2-p6-core-none24.log` | AVD API 24 / none | 5 / 0 | 7.624 秒 |
| `p2-p6-core-shizuku24.log` | AVD API 24 / Shizuku ADB | 6 / 0 | 7.378 秒 |
| `p2-p6-core-none35.log` | Xiaomi API 35 / none | 5 / 0 | 31.189 秒 |
| `p2-p6-core-root33.log` | 补充 Sony XQ-DQ72 API 33 / KernelSU Root | 6 / 0 | 10.836 秒 |
| `p6-oem-none28-verified.log` | G8441 API 28 / none | 1 / 0 | 14.570 秒 |
| `p6-oem-root28-verified.log` | G8441 API 28 / Root | 1 / 0 | 3.243 秒 |
| `p6-oem-none31.log` | XQ-AT72 API 31 / none | 1 / 0 | 2.254 秒 |
| `p6-oem-shizuku31.log` | XQ-AT72 API 31 / Shizuku ADB | 1 / 0 | 3.537 秒 |
| `p6-oem-shizuku35.log` | Xiaomi API 35 / Shizuku ADB, 含两项安装者归属 | 3 / 0 | 3.597 秒 |
| `p6-oem-none-redmi33.log` | 原 Redmi API 33 / none | 1 / 0 | 6.302 秒 |

这些耗时包含 instrumentation, 安装确认, 扫描或清理等待, 不作为重复采样的性能基准. G8441 none 的最终轮新装 2,854 ms, 更新 10,817 ms; 更新阶段实际接受固定夹具的 `Scan app` 和 `This app looks safe` 页面中的 `Install`, 没有绕过恶意应用或其他安全警告.

ROM 策略按设备实际结果保留. 本轮 API 24 userdebug 的 Shizuku 与补充 Sony XQ-DQ72 API 33 user 的 Root 均接受带两项降级标志的非 debuggable 包降级. 这不覆盖或推翻此前 G8441 API 28 user / Xiaomi API 35 user 拒绝同类降级的记录, 也不支持把所有 user ROM 一概写成拒绝. Xiaomi API 35 的 none 路径本轮仍接受 targetSdk 22, 因此该结果不能证明绕过了原本存在的低 targetSdk 拦截. 相关历史范围见 [P2 核心证据](p2-core-evidence.md).

## HyperOS 安装者归属事实

`p6-oem-shizuku35.log` 在同一 Xiaomi 23046RP50C / API 35 / Shizuku ADB 身份记录以下结果. `default` 表示请求未显式填写安装者, 不是系统默认 APK 打开程序.

| requested | observed | 实际交互 |
| --- | --- | --- |
| `default` | `com.android.shell` | 新装和更新均 `silent` |
| `com.android.shell` | `com.android.shell` | `silent` |
| `io.github.supermonster003.autojs6.plugin.three.setup.installer` | 同一插件包名 | `silent` |

两个显式安装者请求均成功, 因而不能将本设备描述为仅接受 `com.android.shell`. none 路径记录的安装者为插件包名. 这些是本设备和已测身份的结果, 不推广为所有 HyperOS 版本或其他 OEM 的兼容保证, 也不表示改动了用户的默认安装器.

## 外部入口与历史归属

本轮选定 `ExternalInstallDeviceTest#externalViewActivityShowsTheOwnedSourceAndCancellationNeverAllocatesAnInstallSession`: 用自有 content URI 显式启动真实 `ExternalInstallActivity`, 校验固定夹具的安装确认页, 再按本次 token 取消. 来源保留, 平台 session 集合不变. 每次只移除本次新 token 的精确历史 ID, 并核对原历史对象及持久文件未变.

| 设备 | 最终日志, 均在 `build/` | 通过 / 跳过 | 耗时 |
| --- | --- | --- | ---: |
| AVD API 24 | `p6-oem-external-emulator-5554.log` | 1 / 0 | 3.110 秒 |
| G8441 API 28 | `p6-oem-external-BH900ASK9E.log` | 1 / 0 | 5.198 秒 |
| XQ-AT72 API 31 | `p6-oem-external-QV710AF65F.log` | 1 / 0 | 2.639 秒 |
| Xiaomi API 35 | `p6-oem-external-968e9f18.log` | 1 / 0 | 1.423 秒 |
| Redmi API 33 | `p6-oem-external-redmi33.log` | 1 / 0 | 1.672 秒 |

五份日志均有 `removedIds=1`, `otherHistoryUnchanged=true`, `durable=true`. 用例在开始前拒绝未终态历史和满 200 条历史, 防止测试加载触发旧任务恢复或新条目挤掉原有历史. 这组证据覆盖外部 Activity/来源/确认/取消, 没有实际操作每台 OEM 的文件管理器或浏览器, 也没有据此验证隐式 resolver 或点击 Always.

XQ-AT72 上已有的维护者 APK/XAPK/APKS 反馈以及 Files by Google 的实际 XAPK/APKM 证据仍见 [P3 外部入口证据](p3-external-entry-evidence.md). Redmi 本轮宿主文件管理器 APK/XAPK 与插件中心 URL, 在有/无插件时共六个实际 UI case, 记录于宿主 `AutoJs6/docs/dev/installer-entry-evidence.md`; 它们属于 P1.4/P2.6, 不能替代这里的外部安装器入口矩阵.

## 默认页通过与保护性跳过

本轮默认页选定 `DefaultInstallerUiDeviceTest#explicitAuthorizerLocksAndUnlocksThroughTheRealDefaultInstallerPage`, 经过真实页面的授权方式选择, 取消, 确认锁定和确认解锁. 本轮未启用 Files hold 驱动, 因而只记录页面和四种公开解析; 既有实际 DocumentsUI 证据另见 [P5 默认安装器验收](p5-default-installer-evidence.md).

| 设备 | 最终日志, 均在 `build/` | 实际结果 |
| --- | --- | --- |
| AVD API 24 / Shizuku ADB | `p6-oem-default-emulator-5554.log` | 1/1 通过, 3.885 秒; `uiLock=true`, `uiUnlock=true`, `publicResolution=4/4`, `baselineRestored=true`; 全部 preferred/last-chosen, 插件偏好和公开解析均恢复 |
| Xiaomi API 35 / Shizuku ADB | `p6-oem-default-968e9f18.log` | 1/1 通过, 2.353 秒; 同上全部恢复断言成立 |
| G8441 API 28 / Root | `p6-oem-default-BH900ASK9E.log` | 0 通过 / 1 保护性跳过; 检测到 `com.google.android.packageinstaller/com.android.packageinstaller.InstallStart` 的既有 APK preferred/last-chosen, `always=null`, 保留原项 |
| XQ-AT72 API 31 / Shizuku ADB | `p6-oem-default-QV710AF65F.log` | 0 通过 / 1 保护性跳过; 检测到本插件已有 preferred/last-chosen, 保留原项 |

两份跳过日志均明确为 `AssumptionViolatedException` 和 `INSTRUMENTATION_STATUS_CODE: -4`, 即使 runner 末尾打印 `OK (1 test)`, 也不能计为锁定/解锁通过. 本轮没有清除 G8441 的 InstallerX 打开记录或替换它的现有默认记录, 没有清除 XQ-AT72 的插件首选项. `always=null` 是本次保护检查读到的事实, 不推断其为可安全清除的最近使用项.

P5 的 API 31 页面/Files 成功记录来自 `emulator-5556`, 不是这台 XQ-AT72, 不能借用来填满原手机矩阵. Redmi 无可用特权身份, 未执行特权锁定, 不把不适用记成失败或成功.

## 安装许可恢复与 Shizuku 状态

none 矩阵使用外部持久 journal 管理测试前置许可. 这是临时预授权下的安装/更新证据, 不作为 P3 首次进入 Settings 授予未知来源权限的 UI 通过. API 26+ 的包级和 UID 级原模式分别记录并恢复; 驱动仅写实际需要变化的字段, 避免 API 28 对未改变的 UID 默认模式发出不必要的 `--uid` 写入. API 24 只核验原全局未知来源值为 `1`, 没有改该全局开关. 工具会拒绝同目标已有 pending journal, 但不提供跨进程互斥锁; 同一 serial/package 的 prepare/restore 必须串行执行, 本轮遵守此约束.

`build/install-permission-journals/<id>/journal.json` 的六份记录当前全部为 `pending=false`, `phase=restored`:

| journal id | 设备 / 目标包 | 原状态 |
| --- | --- | --- |
| `475f88d3023941b49e3ebf46bae5512c` | AVD API 24 / 插件 | 全局未知来源 `1`, 仅核验 |
| `74839630d09742bdbbd9c8140d7f3aec` | G8441 API 28 / 插件 | package=`default`, uid=`default` |
| `a8f40d29637e4951be99bfc55c77c961` | XQ-AT72 API 31 / 插件 | package=`default`, uid=`default` |
| `70099bb11da14b538da4fdbd44d4d28f` | Xiaomi API 35 / 插件 | package=`default`, uid=`default` |
| `5ccf70935aa948b1b4364eb1198d4bdd` | Redmi API 33 / 插件 | package=`default`, uid=`default` |
| `4c313b23e20947be8f69e5a6ed678c15` | Redmi API 33 / 宿主 `org.autojs.autojs6`, P1 三入口共用恢复记录 | package=`default`, uid=`default` |

API 24 原本没有运行 Shizuku server. 本轮临时启动 PID `19678`, UID `2000`, startTicks `2571889`; 停止前按这些值和进程名重新核对, 仅对本轮进程发送 SIGTERM, 恢复原停止状态. `build/p6-oem-temporary-shizuku24.json` 已为 `restored=true`. G8441, XQ-AT72, Xiaomi 和补充 Root Sony 原有 server PID `3077`, `27810`, `20102`, `6403` 保留. 用户启动的模拟器继续运行, 未修改系统图形配置.

## 初轮失败与测试驱动修订

`build/p6-oem-none28.log` 的 G8441 初轮已经识别固定 Core 标签和 Play Protect `Scan app`, 随后失败. 当时驱动错误地要求确认 Activity 在后续扫描时仍 attached; 旧系统安装器返回后 Activity 可以结束, 原平台 session 仍等待扫描. 清理的 10 秒 session 等待异常又覆盖了原始异常, 因此旧日志无法完整还原原 cause, 不能将初轮写成通过.

测试改为在已经接受本次系统确认后, 继续核验原 session ID, 固定夹具包名和插件安装者, 不再要求 Activity 存活. 安全扫描标题和明确安全结果的限制保持. 驱动增加原始因果链和确认状态日志, 清理失败作为 suppressed 异常保留, 未结束 session 的 ID/包名/安装者/大小及自有来源目录另行记录. 旧 session `819268506` 随后由主驱动按维护者授权接受扫描及明确安全结果, 核验实际 v1/插件安装者后移除固定夹具; 该收尾不计入完整新装/更新通过. `p6-oem-none28-verified.log` 的新一轮才是表中 1/1, 并实际自动完成更新阶段的扫描与安全结果确认.

`build/p6-oem-root28.log` 的生产静默新装/更新/卸载均已成功, 但测试 finally 重复卸载已不存在的包. API 28 将错误写到 stderr, stdout 为空, 收尾因此失败. 驱动现在先严格枚举所有用户及保留数据, 全部不存在时不再调用 `pm uninstall`; 仍存在才请求卸载, 且必须收到 `Success` 并复核全用户不存在, 不把空响应当成功. `p6-oem-root28-verified.log` 的新一轮 1/1 才是最终完整结果. 两类问题均为测试驱动修订, 没有借此更改生产安装语义.

## 仍保留的边界

- G8441 和 XQ-AT72 的默认锁定因原有记录受保护而未执行. 不通过清用户默认项或最近使用记录来完成矩阵.
- 五台原设备的显式外部 Activity 用例已经通过, 各 OEM 实际文件管理器/浏览器和隐式解析路径没有因此全部完成. 既有 P3/P5 的具体设备证据按其原范围使用.
- 补充 XQ-DQ72 的 Root 结果不替代 Redmi, API 31 AVD 不替代 XQ-AT72, API 37 保留原文限定的 build 40 插件安装/运行证据. 未取得的 ColorOS 等 OEM 行为继续按未实测处理.
- 本文不以统一 Core 的通过数推导每台设备每种身份的全部 P2 组合均完成. 缺少身份的设备不做虚构测试, 低 targetSdk 已被 ROM 接受也不作为绕过拦截成功.
- 本轮已通过用例的自身夹具/历史清理与上述许可恢复均有对应断言或 journal. 最终 Debug 收尾另以 `build/p6-final-debug-audit/summary.json` 复核 8 台设备: 固定 Core/Spike 夹具及保留数据在所有枚举用户中均不存在, 没有本轮夹具平台 session 或安装前台服务. 原 Xiaomi 6 个/Sony Root 20 个系统活动 session ID 集合不变, 其余设备活动 session 为空; 四台原 server PID 保留, API 24 临时 server 已停止, 六份许可 journal 的设备当前值与原始值一致. 新的签名 Release 交付另由统一收尾记录, 不提前借用上节 build 40 的 Release 结论.

本次只补事实与证据边界, 不据此将原 P6.3 整体勾选完成.
