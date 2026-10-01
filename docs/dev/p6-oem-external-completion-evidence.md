# P6.3 OEM 实际文件管理器入口补充

日期: 2026-10-01. 本文记录 G8441 和 Redmi 的真实系统 Files 操作, 补充 [P6 矩阵](p6-matrix-evidence.md) 已有的显式 `ExternalInstallActivity` 验收. 两类证据分别保留: 本轮观察到了实际系统候选和 OEM 直接路由, 没有把候选存在或显式 Activity 通过写成两台设备均已通过 Files 到插件确认页.

## 设备与固定来源

两台设备在操作前重新读取 SDK 和 ABI, 并在操作前后读取已安装主包的实际 APK 计算 SHA-256. 全程保留同一 build 44 签名 Release, 没有覆盖 Debug, 没有运行 instrumentation 或修改生产代码.

| 项目 | 值 |
| --- | --- |
| 插件版本 | 1.0.0 / versionCode 44, debuggable=false |
| 插件 APK SHA-256 | `3ab3c7691da231a723b59988d6861f0091184d88f742942bf2b7b47225b63e9a` |
| 来源 | 仓库 `app/src/androidTest/assets/fixture-v1.apk`, 设备目录内名为 `fixture.apk` |
| 来源包名 / 版本 | `io.github.supermonster003.autojs6.installer.spike.fixture`, versionName 1.0 / versionCode 1 |
| 来源大小 | 8,536 字节 |
| 来源 SHA-256 | `fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69` |
| 来源内容 | APK 内无 DEX 和 `lib/` 原生库, 固定无代码夹具 |

包名和版本由本地 APK 元数据读取, 不冒充本轮插件确认页核验. 每台来源目录均为本轮随机创建, 创建前同时检查路径和 symlink 不存在; 操作前后从设备读取来源重新核对长度与 SHA-256. 未打开用户已有的安装包.

## 真实 Files 观察

| 设备 | 实际文件管理器 | 实际点击后的结果 | 本轮范围 |
| --- | --- | --- | --- |
| Sony G8441 / API 28 / arm64-v8a / `BH900ASK9E` | `com.android.documentsui/.files.FilesActivity`, versionName 9 / versionCode 28 | 系统 Resolver 显示 `Open with Package installer`, `JUST ONCE` / `ALWAYS`; `Use a different app` 中明确出现 `3-Setup Installer` | 已证明真实 Files 的 APK 候选包含插件. 未选择候选或 `JUST ONCE`; 用 Back 退出 |
| Redmi 22120RN86C / API 33 / arm64-v8a / `bek749scrwv4wo8h` | `com.google.android.documentsui/com.android.documentsui.files.FilesActivity`, `t_frc_doc_330543000` / 330543000 | 没有显示 chooser, 直接进入 `com.android.packageinstaller` 的未知应用风险说明, 按钮为 `CANCEL` / `CONTINUE` | 保留 OEM 实际直接路由事实. 用 Back 退出, 未按 `CONTINUE`, 没有进入插件确认页 |

G8441 的最终操作为 Files 存储根 -> Download -> 本轮目录 -> 唯一 `fixture.apk` 行. Activity 记录确认 Resolver 的 `launchedFromPackage=com.android.documentsui`, 而不是通过 shell 显式启动插件 Activity:

```text
ACTION_VIEW
content://com.android.externalstorage.documents/document/primary%3ADownload%2FThreeSetupExternalProbe-de003ff55424459d9d16d4b054c646a1%2Ffixture.apk
type=application/vnd.android.package-archive
flags=0x800003
resolved component=android/com.android.internal.app.ResolverActivity
```

该机先前已有指向 `com.google.android.packageinstaller/com.android.packageinstaller.InstallStart` 的 APK VIEW, 无 scheme, `always=false` 记录. 当前系统界面也确实将 Package installer 放在最近使用的标题区域. 为保留原记录, 本轮停在候选列表, 没有把 `JUST ONCE` 视为不会影响最近使用记录的操作. chooser 中还显示旧版 AutoJs6; 只读候选查询确认该设备宿主仍注册旧 `PackageInstallerEntryActivity`, 不把这项设备现状误判为当前宿主源码重新注册了入口.

Redmi 实际 Files 从本轮目录的 `fixture.apk` 行直接交给系统安装器. 风险说明来自系统安装器, 不属于插件解析失败. 本轮没有为了继续验证而确认风险提示, 授予 Files 安装许可, 改默认项或安装夹具. 现有截图和 Activity 记录能够证明实际页面及系统组件; 初始交接 Intent 的完整日志未取得, 不推测其全部 flags 或附加字段.

## 公开解析与 OEM 差异

以下使用 APK MIME, `cmd package resolve-activity --brief`, VIEW / INSTALL_PACKAGE 和 content / file 四种组合查询. 它们属于合成隐式解析探针, 与上一节实际 UI 证据分开计数. 两台设备前后结果相同.

| 设备 | VIEW + content | VIEW + file | INSTALL_PACKAGE + content | INSTALL_PACKAGE + file |
| --- | --- | --- | --- | --- |
| G8441 | 系统 Resolver | 系统 Resolver | 系统 Resolver | 系统 Resolver |
| Redmi | `com.android.packageinstaller/.InstallStart` | 本插件 `ExternalInstallActivity` | 系统 Resolver | 本插件 `ExternalInstallActivity` |

Redmi 的 VIEW content 候选查询列出系统安装器, APK Inspector 和本插件, 但最终解析到系统安装器. 全量 preferred XML 没有 APK 专用记录. 因而该机不能仅凭 "插件在候选查询中" 或 "没有 APK preferred" 推导真实 Files 一定会显示 chooser. file URI 的结果也不能替代 content URI 的验收. 本文只记录本机观察, 未进一步归因于某个 MIUI 内部实现或推广到其他 Redmi/HyperOS 版本.

## 驱动边界与失败记录

G8441 的 DocumentsUI 9 不接受直接以 `document/primary:Download/...` 目录 URI 启动. 首次 `am start -W` 超过 45 秒; crash buffer 显示 `DocumentsContract.getRootId` 的 `Invalid URI`, 栈来自 DocumentsUI 的 `Metrics.sanitizeRoot`. 该尝试不计为成功. 后续独立尝试改为实际存储根 `content://com.android.externalstorage.documents/root/primary`, MIME `vnd.android.document/root`, 通过界面逐层导航完成上节候选观察.

该设备的系统 `uiautomator dump` 返回 `null root node returned by UiTestAutomationBridge`, 虽然截图和活动窗口均显示 Files 已打开. 本轮逐帧核对屏幕, 只滑动目录列表以及点击已确认的 Download, 本轮目录和夹具行. 没有根据不可见节点猜测点击位置. Redmi 的窗口树正常, 点击前核对实际 Files 包, 唯一本轮目录和唯一 `android:id/title` 的 `fixture.apk` 节点.

本轮本地观察驱动在忽略目录 `build/p6-oem-external-observe.py`, 原始记录分别位于 `build/p6-oem-external-completion/<serial>/`. G8441 采用截图导航的步骤不能当作此临时驱动已经实现通用自动化. 后续重复验收仍须重新检查当前目录排序和窗口, 不直接重放本轮坐标.

## 收尾与保留范围

| 设备 | 自有来源目录末段 | 完整 preferred XML 的前后 SHA-256 | 最终状态 |
| --- | --- | --- | --- |
| G8441 | `ThreeSetupExternalProbe-de003ff55424459d9d16d4b054c646a1` | `0958c79aadc2a8bcc8f340703cf21815675e045457966f269465909789337e54` | 前后相同; 用户 0 中无夹具或保留数据; 活动安装 session 0 |
| Redmi | `ThreeSetupExternalProbe-4d063ec3cb92453fadda04b7912bc9c7` | `f64e6bd7d10cc02ada840ff0cf707c0a6056569f4bcd62b2a2efedcff6ec55cf` | 前后相同; 用户 0 / 999 中无夹具或保留数据; 活动安装 session 0 |

两台均无插件安装前台服务, 原安装许可模式保持 `default`. Redmi 的包级和 UID 级模式分别核验; appops 输出的相对访问时间只作日志, 不当作模式变化. G8441 原插件 PID 22614 保持, Redmi 前后无插件主进程. 本轮没有进入插件外部 Activity, 没有调用任何历史删除接口. Release 私有历史未直接读取, 因而不将未执行的持久历史比对记为通过.

清理前重新核对本轮固定源的摘要和长度, 并要求来源目录只含 `fixture.apk`. 逐个移除该文件和空目录, 不递归清空 Download. 临时窗口树也先核对本轮保存的摘要再移除; G8441 的窗口树未产生, 只移除本轮空的临时目录. 两台来源和 `/data/local/tmp` 随机目录均复核不存在. 两台最终主包摘要仍为上文同一 Release, 没有需要通过卸载插件或清数据恢复的状态.

主要证据文件:

- G8441: `chooser-screenshot.png`, `chooser-activities.txt`, `fixture-files-screenshot.png`, `after-back-screen.png`, `before-preferred.xml`, `after-preferred.xml`, `state.json`.
- Redmi: `files-before-tap.xml`, `after-fixture-tap.xml` / `.png`, `after-fixture-tap-activities.txt`, `before-back.xml`, `after-back.xml`, `before-preferred.xml`, `after-preferred.xml`, `state.json`.

本轮没有为完成矩阵清除最近使用项或更换默认处理者. G8441 的未锁定 Files 到插件确认仍缺少保持原选择记录的完整证据; Redmi 当前普通 content 打开路径受系统实际路由限制, 不能计为插件确认成功. [P5 默认页验收](p5-default-installer-evidence.md) 的锁定期间 Files 直达和 [P3 外部入口](p3-external-entry-evidence.md) 的其他设备证据保持各自原范围, 不用于填充这两项缺口.

本次观察完成并交接后, 主代理另在 G8441 上完成保留上述最近使用记录的 Root 默认页锁定/解锁, 以及锁定期间实际 Files 直达插件确认后取消, 详见 [默认项历史保护验收](p6-default-history-evidence.md). 该结果有独立的运行与恢复证据, 不改写本文的原未锁定配置观察.
