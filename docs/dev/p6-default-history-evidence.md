# P6.3 保留最近使用记录的默认页与 Files 验收

日期: 2026-10-01. 本轮补齐 Sony G8441 / API 28 / Root 的真实默认页锁定与解锁, 并在锁定期间从系统 Files 进入插件确认页后取消. 保留原系统安装器的 APK 最近使用记录, 没有清除第三方默认项或最近使用项. 原 P6.3 整体矩阵继续保留未完成.

## 验收保护

此前普通测试遇到任何 APK preferred/last-chosen 即跳过. 本轮保留这一默认行为, 增加测试专用的显式参数 `defaultUiPreserveUnrelatedLastChosen=true`. 该参数只允许以下基线:

- owner user 0, 四种 APK action/scheme 解析均为系统 Resolver.
- 原本没有任何本插件的 preferred/last-chosen.
- 相关第三方记录的完整系统 XML 必须明确为 `always=false`, 单 action/category/type, 无 scheme/authority/path, 优先级为 0. 这些 filter 不等于生产引擎设置的四个带 scheme 的 filter.
- 每条公开 preferred 查询记录必须有唯一, 同组件且完整 filter 相同的系统 XML 证明. 真实默认项, 未知 always 值, 带 scheme 的记录, 未证明或重复证明均拒绝.

在修改默认项之前, 精确的原记录及插件偏好持久保存到原有恢复计划. 选择授权后及点击确认前重新比对. 锁定时只有本插件本次新增的四项可以不同; 解锁与恢复后全量 canonical XML, 公开查询, 插件偏好及四种解析必须恢复. 新模式不调用清第三方默认项的方法, 也不进入之前单独批准的 InstallerX 维护分支.

`DefaultInstallerHistoryGuardDeviceTest` 新增四项保护测试, 验证普通模式仍拒绝原记录, 显式证明只接受原记录, 真默认/未知标志/可替换 filter 拒绝, public/shell 唯一对应, 本插件原记录保护, 通配 MIME 同样必须有 false 证明. API 24 / 28 / 31 各 4/4, 无跳过或失败. 这些测试只构造保护输入, 不写系统默认项或启动特权服务. 日志分别为 `build/p2-matrix-completion/default-history-guard-emulator-5554.log`, `build/session45-default-files/history-guard-api28.log`, `build/session45-history-guard-api31.log`. 另经子代理独立审阅, 未发现阻断问题.

## G8441 的完整最终运行

设备 `BH900ASK9E`, API 28, ABI `arm64-v8a`, user 固件. 主包采用保留数据覆盖的 build 45 Debug, 测试 APK 使用本轮新增保护. 原第三方记录为 `com.google.android.packageinstaller/com.android.packageinstaller.InstallStart`, `ACTION_VIEW`, APK MIME, 无 scheme, `always=false`.

最终一次完整运行的日志为 `build/session45-default-files/default-ui-guarded.log`, 1/1, 无跳过, 32.850 秒. 独立 Files 驱动同时成功, 日志 `files-driver-guarded.log` 与 `api28-BH900ASK9E-guarded-external-record.json` 记录:

1. 从真实默认安装器页面选择 Root, 先取消确认, 系统默认与插件偏好不变.
2. 再确认锁定, 四种公开解析全部指向插件. 原系统安装器最近使用及其他所有记录不变.
3. 通过真实 `com.android.documentsui/.files.FilesActivity`, 从存储根进入 Download 和本轮随机目录, 点击固定 `fixture.apk`.
4. 系统直接进入插件安装确认页, 可见固定包名 `io.github.supermonster003.autojs6.installer.spike.fixture`, 没有 chooser 或替代为合成显式安装 Intent.
5. 取消本次 token, 显示取消结果并完成. 不执行安装.
6. 返回同一默认页并确认解锁, 四种解析回到原系统 Resolver, 全部原记录和偏好恢复.

最终 runId 为 `c394e8f5-c51c-4a06-8471-889110f73b42`, 外部 token 为 `660e63c4-dd84-4647-a4e1-3e1c2c978bdd`. `history-cleanup.log` 的独立恢复后清理 1/1, 仅移除该 token 的一条取消历史, 持久回调成功, 其他历史/默认项/偏好不变.

自有目录 `ThreeSetupDefaultProbe-db27a77e6f11` 中的源 APK SHA-256 为 `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69`. 清理前核对摘要, 只删除本次文件和空目录. `plan.json` 已标记 removed. `before-preferred.xml` 与 `after-preferred.xml` 完全相同, 原系统安装器最近使用项仍存在. 没有卸载用户应用或清除 Download 内容.

## 初轮驱动失败

`default-ui.log` 的页面测试 1/1, 但独立 Files 导航失败: 旧 Files 复用之前已删除目录的任务, 启动 root URI 没有重置该页面. `default-ui-retry.log` 的页面仍通过, 独立驱动却在列表尚未稳定时点入相邻目录. 两次均执行恢复并保持基线; 没有产生安装 token. 不把页面成功与 Files 失败拼接成完整通过.

最终驱动用新的 Files 任务从存储根导航, 等待滚动稳定, 通过同一 instrumentation 的受限点击桥重新定位可见的 Download/本轮目录/fixture.apk. 该桥只接受固定目录模式与文件名, 在无可点击祖先时发送已刷新边界中心的真实 finger 事件; 点击派发结果和后续页面状态分别核验. 日志中的事件接受不单独作为导航成功依据. 最终完整运行与前两次独立保存.

初次本地准备脚本还曾将不存在的 `installer_settings.xml` 当作必有文件而停止, 当时只完成 Debug 覆盖, 未创建外部目录或修改偏好. 后续沿用正式恢复快照对不存在偏好文件的既有处理, 没有创建用户设置来满足测试.

## 其他设备与剩余边界

G8441 未锁定时的实际 Files chooser 和 Redmi 实际直达系统安装器的差异见 [OEM 外部入口补充](p6-oem-external-completion-evidence.md). 本文的 G8441 成功只覆盖 Root 锁定期间, 不声称原未锁定状态自动绕过 chooser.

XQ-AT72 / `QV710AF65F` 仍有本插件自己的 APK/APKM/XAPK/octet-stream 最近使用记录, 新模式明确拒绝这类基线, 没有对该机进行锁定/解锁. 本轮只读解析查询前后还观察到 `web1n.stopapp` 的 `INSTALL_PACKAGE` APK 记录从 `always=true/set=2` 变为 `always=false/set=0`, 其他 canonical 记录相同. 原始前后快照与差异在 `build/session45-preflight/`. 没有发出该包的设置或清除默认项命令. 这与 AOSP 在候选集合失效时将首选项降为最近使用记录的逻辑一致, 但本轮未跟踪框架内部调用, 不据此声称确切归因, 也不将该查询写成全默认状态绝无变化. 参考 [AOSP PackageManagerService](https://android.googlesource.com/platform/frameworks/base/+/android-9.0.0_r30/services/core/java/com/android/server/pm/PackageManagerService.java).

本轮没有把 G8441 的保留证明扩展成清除 XQ-AT72 原记录的许可. XQ-AT72 默认页仍待维护者安排可验收的基线, 原 P6.3 checkbox 保留. 测试结束后的同签名 Release 交付和最终构建在本会话本地 gate 证据中另行记录.

本轮源码校验 `build/session46-validation.log`: 268项JVM全部通过, 无失败/跳过; Debug与androidTest装配成功, Debug lint为0错误/22警告. 文档及图标生成器校验通过. 没有生产安装引擎变更, 不重复既有100 MiB性能采样或进程死亡矩阵.
