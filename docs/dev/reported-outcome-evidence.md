# 已装上却报 "安装已取消": 根因与验证 (2026-11-18)

## 现象

AutoJs6 插件中心第二期收口走查 (宿主仓库 `docs/plugin-center/evidence/P14-pc2_p14-20261118.md` 发现 2) 中, 3-Setup Installer 1.2.2 三次把已经成功的安装报告为取消:

| 设备 | 操作 | 插件结果页 | 实际 |
|---|---|---|---|
| Redmi 22120RN86C, Android 13, Shizuku | AutoJs6 经插件更新自身 6641 -> 6642 | 安装已取消, CANCELLED | 6642 已装上, md5 与预期一致 |
| OnePlus OPD2413, Android 15 ColorOS 15, 无特权 | 插件中心安装 Java Libraries 14, 中途先为插件授 "安装未知应用" | Installation cancelled, USER_CANCELLED, `INSTALL_FAILED_ABORTED: User rejected permissions` | 14 已装上 |
| 同一平板 | AutoJs6 经插件更新自身 | 取消 | 已装上 |

## 根因

两条独立的路径:

1. 调用方死亡被当成取消. `InstallerBinder` 监听宿主回调 Binder 的死亡, 死亡时关闭会话租约并 `cancel()`. 宿主只绑定了插件服务, 宿主一死服务也被销毁, `onDestroy` 关闭全部租约, 同样走 `cancel()`. 宿主更新自身时, 系统在安装过程中 (`installPackageLI`) 杀掉宿主; 平板 logcat: 09:55:54.702 `am_kill org.autojs.autojs6 ... due to installPackageLI`, 20 ms 后插件经 `finishActivity` 关掉了系统安装器 (`InstallStart ... request-sub`). 此时平台的成功广播还没到, 等待线程被中断, 结果记为 CANCELLED.
2. ColorOS 的系统安装器用自己的会话装包, 再拒绝插件的会话. `CONFIRM_INSTALL` 由 `com.android.packageinstaller/.InstallStart` 转给 `com.oplus.appdetail`, 后者复制 APK 并以自己的会话安装 (平板 09:49:53.9 装上 Java Libraries); 用户在完成页点完成后 `InstallStart` 才结束, 对插件的原会话 `setPermissionsResult(false)`, 约 1 s 后插件收到 `STATUS_FAILURE_ABORTED` / `INSTALL_FAILED_ABORTED: User rejected permissions`, 映射为 USER_CANCELLED. 佐证: 平板上宿主当前的 `dumpsys package` 为 `installerPackageName = initiatingPackageName = com.android.packageinstaller`, `originatingPackageName = 3-Setup Installer`, 即包不是由插件的会话装上的.

## 修复

- 引擎在平台提交前回调 `onCommitting()`. 提交之后, 调用方死亡或服务随调用方销毁不再取消该条目: `InstallSession.detachCaller()` 让它继续等待真实的平台结果, 发往已死调用方的回调失败被忽略, 结果照常进入插件的结果页, 通知与历史; 同一批的后续条目取消. 提交之前的死亡仍按取消处理 (原设备测试 `clientProcessDeathCancelsAndReclaimsItsSession` 覆盖).
- 提交之后的任何失败, 报告前先读取已安装的包核对 (`InstalledOutcome`): 包现在为目标用户安装, versionCode 等于预备包, 签名证书摘要等于源 APK, 并且相对于开始会话前 (同一包锁内采样) 是新安装或 `lastUpdateTime` 有变化. 满足时报告成功, `notes` 写明平台原来的报告 (例如 `The package installer reported USER_CANCELLED (INSTALL_FAILED_ABORTED: User rejected permissions), but the package is installed ...`); 不满足时原样报告失败. 需要后续处理的选项 (更新所有权, dexopt) 在这种结果里记为未完成.

## 验证

临时 AVD `T_ReportedOutcome_35` (API 35 google_apis x86_64, emulator-5640), 宿主 debug 6642 x86_64 (与插件同签名), 驱动脚本 `tools/run-reported-outcome-acceptance.py` (宿主 "ui" 脚本调用 `installer.install`, `authorizer: none`, `interaction: dialog`):

| 场景 | 1.2.2 发布包 (`36ebec19`) | 修复后 debug 包 |
|---|---|---|
| `self-update`: 宿主经插件重装自身, 安装时宿主被杀 | FAIL, 结果页 cancelled, CANCELLED, 宿主 lastUpdateTime 已变 | PASS, 结果页 success, 宿主 lastUpdateTime 13:18:55 -> 13:20:03 |
| `oem-abort`: 插件无 "安装未知应用" 权限, 设置页打开开关后进入系统确认; 模拟 ColorOS, 由 shell 用另一个会话装上同一 APK, 再按返回键拒绝原会话 (平台消息同为 `User rejected permissions`) | FAIL, 结果页 cancelled, USER_CANCELLED, 宿主收到 USER_CANCELLED, 夹具 versionCode 2 已装上 | PASS, 结果页 success, 宿主收到 ok |

- 修复中途的一次 `self-update` 仍报 CANCELLED, 由此找到第二个取消来源 (服务随宿主销毁时关闭租约), 一并修复后通过.
- JVM: `:app:testDebugUnitTest` 409 项, 0 失败 / 错误 / 跳过; 新增 `InstalledOutcomeTest` 5 项, `InstallSessionTest` 6 项 (提交后调用方死亡, 提交前死亡, 已装上的中止, 未变化的中止, 提交前失败不核对, 提交后取消), `InstallEngineTest` 1 项 (提交通知只在提交前发出一次).
- 设备 (同一 AVD): `InstallerBinderDeviceTest`, `InstallSafetyPolicyDeviceTest`, `InstallSessionDeviceTest` 共 20 项通过.

真机复测 (2026-11-18, 提交 `116a7e4` 的 debug 包 versionCode 150 覆盖原插件, 宿主 debug 6642 的 "ui" 脚本调用 `installer.install`, `interaction: dialog`, 所有对话框由维护者点按):

| 设备 | 场景 | 结果 |
|---|---|---|
| Redmi 22120RN86C (Android 13, MIUI 14) | Shizuku, AutoJs6 重装自身 | 插件记为 completed; 13:51:10 换上新包, 13:51:11 `am_kill org.autojs.autojs6 ... installPackageLI` |
| OnePlus OPD2413 (Android 15, ColorOS 15) | 无特权, 插件先无 "安装未知应用" (由维护者在设置页打开), 装 Java Libraries 1.0.0 (vc 14) | 插件记为 completed, AutoJs6 收到 ok, notes 为 `The package installer reported USER_CANCELLED (INSTALL_FAILED_ABORTED: User rejected permissions), but the package is installed ...`; 13:56:39 由 `com.android.packageinstaller` 的会话装上, 13:56:40.958 插件会话以 `User rejected permissions` 销毁 |
| 同一平板 | 无特权, AutoJs6 重装自身 | 插件记为 completed; 13:57:17 换上新包并结束宿主, 13:57:18.992 插件会话同样被 ColorOS 中止 |

测后两台装回原插件 (Redmi build 48, 平板 1.2.2 / 149, md5 核对一致), 宿主内容未变, Java Libraries 已卸载.

1.2.3 发布前补测 (2026-11-20, release 包 `4f9789ab` versionCode 152, 即 R8 后的同一修复; 临时 AVD API 35 google_apis x86_64, emulator-5650, 宿主 debug 6642 x86_64, 驱动脚本新增 `reject-new` / `reject-same` 两个场景, 核对真正的拒绝不会被报成成功):

| 场景 | 结果 |
|---|---|
| `self-update` | PASS, 结果页 success, 宿主 lastUpdateTime 13:04:41 -> 13:05:48 (已替换) |
| `oem-abort` | PASS, 结果页 success, 宿主收到 ok, 夹具由 shell 装上 (versionCode 2, 13:06:20) |
| `reject-new`: 夹具未安装, 系统确认按返回键拒绝 | PASS, 结果页 cancelled, USER_CANCELLED, 宿主收到 USER_CANCELLED, 夹具仍未安装 |
| `reject-same`: 同一夹具 APK 已由 shell 装上, 再按返回键拒绝 | PASS, 结果页 cancelled, USER_CANCELLED, 宿主收到 USER_CANCELLED, 夹具 lastUpdateTime 13:06:58 未变 |

- 同一提交的 debug 包在 API 33 临时 AVD 上: `ReleaseHistoryPageInstrumentationTest`, `SettingsDeviceTest`, `StandaloneAppearanceDeviceTest`, `ThreeSetupInstallerPluginContractTest`, `HomeActivityDeviceTest`, `InstallerBinderDeviceTest`, `InstallSessionDeviceTest`, `InstallSafetyPolicyDeviceTest` 共 39 项, 32 项通过, 7 项为需要显式开启的夹具 (engineAuthorizer) 按设计跳过, 0 失败.
