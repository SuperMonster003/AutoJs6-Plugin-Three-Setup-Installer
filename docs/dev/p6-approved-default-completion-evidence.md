# P6.3 授权后的 XQ-AT72 默认页与原设备矩阵收口

日期: 2026-10-02. 维护者明确允许调整 XQ-AT72 / `QV710AF65F` 的安装包最近使用记录. 本轮采用限定设备和限定记录的保留方式完成默认页与真实 Files 验收, 实际没有永久删除原四条插件记录. 原矩阵按每台设备真实结果归档, ROM 拒绝或系统接管如实保留.

## 授权范围与测试保护

新增测试参数 `defaultUiPreserveApprovedPluginLastChosen=true` 仅适用于 `QV710AF65F`, API 31, owner user 0. 普通模式及上一轮第三方历史保留模式仍拒绝原有本插件记录. 三种历史保留参数互斥; 既有 InstallerX 单设备维护入口不由新参数触发.

本插件的原记录必须恰好为四条 `ACTION_VIEW` / `CATEGORY_DEFAULT` / no-scheme / `always=false` / `match=600000` / `set=0`, MIME 分别为 APK, APKM, XAPK 和 octet-stream, 组件必须为本插件 `ExternalInstallActivity`. public 和完整 shell XML 的 canonical 数组均精确比对, 不能以包名或 MIME 列表近似代替. 相关第三方记录仍需唯一的 shell `always=false` 证明且为零 scheme; 本机为 stopapp 的一条旧 `INSTALL_PACKAGE` 记录. 四个 APK 解析探针开始时全部为系统 Resolver.

修改前保存 package/UID/API/fingerprint/runId/authorizer/完整基线及许可范围的持久 journal. 锁定后只允许增加四种生产 APK action/scheme 组合, 原记录和插件偏好必须完全保留. 清理前再次核对原 public/shell 数组和偏好完整不变, 新项仅可为实际 APK MIME, 单 action/scheme, 每种组合最多一条且总数最多四条. `hasDataType` 的通配语义不能用于判定可清理的新项, 本轮改为精确 MIME 比较并增加回归. 最后只清理本插件新增的首选项, 对完整基线比对成功后才标记 journal restored.

这些改动均在 androidTest 中, 没有放宽生产 Binder 权限或安装行为. 子代理只读复核本地 API 31 AOSP: 非 null 包名的 `clearPackagePreferredActivities` 只移除该包 `always=true` 项, `removeExisting` 按过滤器匹配, 零 scheme 原项与一个 scheme 的生产项不相等. Sony 实际是否保留仍由下述设备前后快照证明, 不从 SDK 源码直接推导 ROM 成功.

## XQ-AT72 实际完整流程

设备重新核验为 API 31 / `arm64-v8a`, 插件 UID 15158, Shizuku ADB UID 2000, 原 server PID 27810. 原主包为签名 Release 48, 测试时保留数据覆盖 Debug 48和本轮测试APK. 原历史有8条已结束记录, 不存在待恢复条目或200条上限溢出风险; 固定夹具在全部用户中不存在.

`build/session49-default-files/default-ui.log` 完整运行1/1通过, 无跳过, 21.674秒:

1. 真实默认页选择Shizuku后先取消, 默认项/插件偏好不变.
2. 再确认锁定, VIEW/INSTALL_PACKAGE与content/file四种解析均指向插件, 原四条插件最近使用及stopapp等所有其他记录完整保留.
3. 从实际系统 `com.google.android.documentsui/com.android.documentsui.files.FilesActivity` 打开本轮目录中的 `fixture.apk`, 直接进入插件确认页, 核验固定包名. 独立驱动并未向插件构造显式安装入口.
4. 取消本次安装, 完成结果页, 回到真实默认页确认解锁. 四种解析恢复系统Resolver, public/shell/偏好全量恢复.

独立 Files 驱动 `files-driver.log` 与 `api31-QV710AF65F-guarded-external-record.json` 同时记录 `directConfirmation=true`, `cancelledWithoutInstall=true`. runId为 `a25fca94-d7ce-485e-9013-23643f72d600`, token为 `1658bf59-ed2b-48ea-acaa-feb8c8db670f`. 默认恢复后单独执行历史清理1/1, `history-cleanup.log` 核验只移除该token的一条取消项, 持久回调成功, 其他历史/默认项/偏好不变. 电脑侧再次比较原8条历史JSON对象完全相同.

自有目录 `ThreeSetupDefaultProbe-aa18a37b5584` 中的固定无代码APK SHA-256为 `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69`. 清理前核对摘要, 只移除该文件和空目录, journal记录removed=true. 没有安装夹具或用户应用, 未清除任何用户历史.

原始状态在 `build/session49-default-preflight/`, 最终记录在 `build/session49-default-files/`. 首次电脑侧原始文本比较因Windows写入原始ADB换行后形成额外空行而失败; 完整XML节点/属性/子节点的canonical比较和去空白比较均相等, 与设备内严格恢复断言一致. 该文本格式差异不描述为设备状态变更, 也不声称两份落盘文本逐字节相同.

## 守卫回归

`DefaultInstallerHistoryGuardDeviceTest` 在XQ-AT72最终6/6, 无失败或跳过, 0.117秒, 日志 `build/session49-default-guards-final.log`. 原四项继续覆盖false证明/原插件保护/真实默认与同scheme拒绝; 新增两项覆盖单设备approved模式不能接受任意基线, 以及恢复拒绝通配APK MIME和重复生产filter. 这些保护测试不写系统首选项.

API 24也运行同一新版守卫6/6, 0.064秒, 日志 `build/p2-authorized-matrix/default-history-guard6-root24.log`. 在无原APK默认项的基线上, 普通Root默认页模式另1/1, 2.592秒, 日志 `default-ui-root24-normal.log`; 未使用任何保留例外或Files hold. 原记录/偏好/四种解析全部恢复, journal `a07d4efd-9601-470d-8fb3-0cf7bcf37264` 为restored. 该AVD已恢复原Release48, magiskd PID1338保留, 原0个session/无Shizuku server状态保持.

源码校验 `build/session49-source-validation.log` 为268项JVM零失败/错误/跳过, Debug/androidTest装配成功, Debug lint 0错误/22警告. 最终签名Release另按统一收尾验证, 不将Debug验收计成混淆Release操作验收.

## 与原 P6.3 设备池的对应

| 原设备范围 | none新装/更新 | 可用特权安装/卸载 | 实际外部入口与默认页 | 证据 |
| --- | --- | --- | --- | --- |
| AVD API 24 | 原Core 5/5 | 原Shizuku 6/6, 本轮Root 8/8 | 原Shizuku默认页/Files直达通过; 本轮普通Root默认页1/1, 原状态恢复 | p6-matrix-evidence.md, p5-default-installer-evidence.md, p2-authorized-matrix-evidence.md |
| Sony G8441 API 28 | 原1/1, 包含实际确认/安全扫描 | 原Root新装/更新/卸载1/1 | 上轮Root默认页及锁定期间真实Files直达1/1, 系统安装器最近使用保留 | p6-matrix-evidence.md, p6-default-history-evidence.md |
| Sony XQ-AT72 API 31 | 原1/1 | 原Shizuku新装/更新/卸载1/1 | 本轮真实默认页/Files/取消/解锁完整1/1, 四条插件最近使用及其他记录保持 | 本文; p6-matrix-evidence.md |
| Redmi API 33 | 原1/1 | 无可用特权身份, 不适用 | 显式外部Activity通过; 实际Files由系统安装器接管, 退出且原状态不变; 无特权锁定不适用 | p6-matrix-evidence.md, p6-oem-external-completion-evidence.md |
| Xiaomi API 35 | 原Core 5/5 | 完整Shizuku 8/8, 两种显式安装者归属均验证 | 原默认页/真实Files直达与恢复通过, HyperOS安装者事实有记录 | p2-matrix-completion-evidence.md, p5-default-installer-evidence.md |
| AVD API 37 / 16 KiB | 按原条目仅验证插件本身安装与运行 | 原限定范围不要求 | build40首页/运行及build48签名Release跨进程契约2/2, 实际PAGE_SIZE=16384 | p6-matrix-evidence.md, p7-local-gate-evidence.md |

本表汇总既有有效证据和本轮缺口验收, 没有把不同构建冒充同一构建全矩阵重跑. 原P6.3要求记录每台设备实际结果, 不承诺所有ROM都允许替换系统安装器. Redmi的实际路由限制作为已观察限制收口, 不计为插件确认成功; 额外Sony XQ-DQ72和本轮Samsung不替代原Redmi. ColorOS未专项实机验证的声明保持. 原完整矩阵的测试结果现在都有明确对应, 因而可勾选原条目, 路线图结构不变.

最终本轮签名Release交付与构建另记统一收尾证据. 原设备池中本轮未在线的设备不进行新包交付, 保留其既有版本/证据边界.
