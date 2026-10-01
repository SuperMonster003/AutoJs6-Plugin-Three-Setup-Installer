# P3.3 未知来源授权的真实设备路径

日期: 2026-10-01. 本轮新增显式 opt-in 的 grantingUnknownSourcePermissionInSettingsContinuesTheSameNoneInstallation 用例, 参数 unknownSourceGrant=true. 保留原拒绝授权用例, 不改变生产安装路径.

测试对来源 APK 校验固定包名和无代码属性, 拒绝任何用户下预先存在的夹具或保留数据. 真实操作使用 none; shell 只建立拒绝前置条件并恢复本插件原有 package / UID 两层 AppOps, 授权成功必须通过实际 Settings 开关.

## 必须保持的断言

- Settings 的 package: URI 只指向本插件, 所在任务以本次 live UserActionActivity token 为根. 只识别已观察的标准组件和唯一可勾选开关, 不按全局屏幕坐标猜测肯定操作.
- 使用不拦截的 ActivityMonitor 和真实 PackageInstaller.SessionCallback. 平台 session 只创建一次, 授权前后 session ID 不变, 系统确认只启动一次.
- 安装成功必须收到原 session 的成功结果, 正确版本, 源文件保留且 SHA-256 未变. 没有该结果就不能算安装成功.
- finally 清理只操作已证明属于本次固定夹具的 session / task / 文件. 原始测试异常保留, 清理错误作为 suppressed; 若跳过同时有清理错误, 转为失败.
- 权限两层原值与有效 canRequestPackageInstalls 状态均恢复并复查. 日志只输出本 case 的状态, 组件和控件结构字段, 不收集任意界面正文.

## 实测结果

| 设备 | 已证明的实际行为 | 完整安装验收 |
| --- | --- | --- |
| Sony G8441 / API 28 / arm64-v8a | 打开本插件 Settings, 点击开关得到允许, 原 session 继续并显示一次系统安装确认. 用户确认后进入额外 Play Protect 扫描同意页 | 跳过成功验收; 测试拒绝上传 / 扫描, 等原 session 失败终结, 验证未安装及来源未变 |
| Xiaomi 23046RP50C / API 35 / arm64-v8a | 打开本插件 Settings, 验证 switchWidget 为唯一 android.widget.Switch, 点击后进入同任务的 SpecialPermissionInterceptActivity | 跳过; 额外 OEM 授权页未自动操作, 当前许可及安装成功未验证 |

两台设备本次的原模式均为 package=default / uid=default / allowed=false, 结束后均恢复为该状态.

Sony 最终运行: session 787941782, createdSessions=[787941782], confirmationSessions=[787941782], finishedSessions=[(787941782,false)], 插件结果 USER_CANCELLED. Play Protect 窗口必须同时匹配 com.android.vending, 固定夹具名称, App scan recommended 标题和唯一 Don't install app 否定操作才会点击. 不点击 Scan app 或绕过选项, 不改变全局扫描设置, 不上传 APK.

HyperOS 的额外组件精确为 com.miui.securitycenter/com.miui.permcenter.privacymanager.SpecialPermissionInterceptActivity. 只在本次已验证 token 的任务中识别该组件后跳过, 不把其他异常泛化为可忽略.

最终组合日志 build/p3-followup-api28-final-suite.log 和 build/p3-followup-api35-final-suite.log 在各设备均为 15 项通过 + 1 项上述授权流程有意跳过, 0 失败. 通过项为 InstallDialogDeviceTest 12 项和真实窗口 3 项. Runner 的 OK (16 tests) 包含 -4 assumption 状态, 不等于 16 项通过.

## 首轮问题与修正

1. Instrumentation.sendKeyDownUpSync 返回跨应用 Settings 时缺少 INJECT_EVENTS, 改为 UiAutomation 注入并验证前后任务 / token. 授权开关点击本身未被替代.
2. 平台已接管 commit 后, PackageInstaller 不再保证 abandon 有效. Sony 的额外扫描页因此使初次清理超时. 后续显式识别并拒绝自有夹具扫描, 等原 session 失败终结, 不反复重装或关闭系统扫描服务. 初次挂起的两条夹具 session 已清理, 未遗留夹具安装.
3. Compose 的文本查找未提供预期匹配, 改为最多 512 个节点 / 深度 32 的完整有界遍历, 仍保持精确包名 / 文本 / 可见性与唯一性, 遍历不完整时拒绝点击.
4. HyperOS 控件资源名为 com.android.settings:id/switchWidget, 已按实际节点增加该精确项. 点击后的额外权限步骤单独识别并如实跳过, 不将其伪装成授权成功.

P3.3 原测试 checkbox 继续保留未完成. 本轮证明了真实授权前段及安全退出边界, 不能替代最终成功安装的完整路径.

## 后续用户手动启动 API 31 AVD 的真实授权复测

2026-10-01, 用户自行启动 API 31 AVD 后执行同一 opt-in 用例. 原始 instrumentation 日志 build/p4-api31-p3-followup.log 记录:

- 初始状态为 user=0, package=default, uid=default, allowed=false.
- 实际 Settings 页面只针对本插件, bridge=0f61e8fe-0dd9-4e4a-b5d0-0749925442c3. 通过 Settings 开关得到授权后, 继续原 platformSession=1238106379, 没有另建平台安装会话.
- 额外 Play Protect 扫描提示同时匹配固定夹具标签, App scan recommended 标题和唯一否定操作. 既有 guard 选择 Don't install app, 拒绝上传扫描, 未关闭全局扫描设置或操作肯定按钮.
- createdSessions=[1238106379], confirmationSessions=[1238106379], finishedSessions=[(1238106379,false)]; 插件终态为 cancelled / USER_CANCELLED. 测试走到额外扫描的 AssumptionViolatedException 分支前已检查夹具未安装.
- finally 恢复 REQUEST_INSTALL_PACKAGES 原模式时, 系统杀死了运行 instrumentation 的目标进程. Runner 最后为 `INSTRUMENTATION_RESULT: shortMsg=Process crashed.` 和 `INSTRUMENTATION_CODE: 0`, 没有正常的通过或跳过终结, 本次不能标为通过, 也不计作普通 assumption 跳过.

补充现场日志 build/p4-api31-restoration.log 记录 `10-01 14:18:57.976 ActivityManager: Killing 6030 ... REQUEST_INSTALL_PACKAGES changed.` 和随后 `REQUEST_INSTALL_PACKAGES: default`. 收尾检查的包列表按本次 fixture / spike 名称筛选为空, 没有安装自建夹具; 权限已经恢复. 该补充记录用于说明系统终止原因和清理状态, 不能替代缺失的 instrumentation 成功结束.

本次证明了实际授权, 同会话回跳及安全拒绝额外扫描, 没有完成授权后成功安装的完整验收. P3.3 对应原条目继续保留未完成.

## 维护者允许 Play Protect 扫描后的完整成功验收

2026-10-01, 维护者明确允许扫描. 在用户已启动的 API 31 / x86_64 AVD `emulator-5556` 上完成正向验收, 未更改全局 Play Protect 设置. 上述失败和跳过记录保留为对应运行的事实.

新增 `tools/run-unknown-source-acceptance.ps1` 将权限前置条件和恢复移到 instrumentation 进程之外. 驱动先校验指定设备, 当前用户, 插件 UID 不共享, 固定夹具在所有用户下均不存在, runner 和测试参数; 在任何权限更改前保存 package / UID 两层原始 AppOps 的恢复计划. 测试退出之后, 驱动的 finally 恢复两层原值并再次查询, 避免恢复权限时 Android 杀死目标进程而丢失 runner 的结论. 测试仍必须通过真实 Settings 开关授予权限, shell 不代替授权动作.

运行方式 (PowerShell 7.2+):

```powershell
./tools/run-unknown-source-acceptance.ps1 -Serial emulator-5556 -AllowPlayProtectScan
```

扫描只允许操作本次固定无代码夹具: 同时匹配 `com.android.vending`, 固定夹具名称, `App scan recommended` 与唯一 `Scan app` 控件. 扫描后的继续操作另需精确匹配 `This app looks safe`, `You can continue to install it` 与唯一 `Install` 控件, 并确认仍为本次原安装 session. 不处理未知 OEM 对话框或有害应用警告, 不点击绕过检查的操作. 不传扫描 opt-in 时仍保留原有拒绝扫描路径.

两次运行分别记录:

| 本地证据目录 (`build/unknown-source-acceptance/`) | 实际结果 |
| --- | --- |
| `20261001-152954-bee1ab99c0a240eaad6cf6631bc69ce5` | 实际允许扫描并出现安全结果页. 当时测试尚未识别安全结果页的独立 Install 按钮, 最终超时失败; 不能算自动成功. 原权限恢复, 夹具未遗留. 页面截图为 `build/p5-api31-scan.png`. |
| `20261001-153932-8ab59de9223545d5961c4fd59c12b62c` | 增加严格安全结果页识别后, 完整授权 / 原会话继续 / 系统安装成功 / 清理通过, `OK (1 test)`, 4.015 秒, 无跳过. 本次未再次出现扫描页, 使用了先前扫描的缓存结论, 不宣称同一次运行再次展示并点击全部扫描步骤. |

成功运行的可核对值:

- authorizer=`none`, platformSession=`2078773323`, created=1, systemConfirmationStarts=1, sourcePreserved=true. 授权前后复用同一个平台会话, 版本和源文件摘要断言通过.
- 插件历史 token=`261376fb-6aaa-4c1e-850e-d5f78bfca499`, 仅删除本次 token 对应的一条测试历史; 使用删除墓碑防止晚到写入复活测试记录.
- 原权限为 package=`default`, uid=`default`; `result.json` 中 `Passed`, `TestPassed`, `AppOpsRestored`, `FixtureAbsentAfter` 均为 true, `CleanupFailures=[]`.
- `CLEANUP fixtureAbsent=true ownedSessionSettled=true permissionRestore=driver`; 不删除用户安装包, 不关闭用户启动的模拟器.

驱动自测覆盖 AppOps 输出解析与异常拒绝, PowerShell AST 解析通过. 正向成功补齐原 P3.3 测试条目中的未知来源引导安装部分; 既有取消, 超时和特权卸载确认的设备证据仍见 `p3-ui-evidence.md`. P3.3 原测试条目据此勾选, 不代替 P3.2 的系统文件管理器 / 浏览器 APK / XAPK 手动矩阵.
