# P2.3 来源删除与固定保留策略

日期: 2026-10-01. 维护者确认原 `keepSourceOnFailure` 待办的含义为固定策略: 失败, 取消或超时始终保留相应未成功项的来源. 不添加可关闭的删除保护开关, 不增加公开 API 参数. 此决定补充原 D25, 不新增或拆分路线图条目.

## 实现与所有权

生产 `InstallSession` 只在安装引擎确认成功返回后调用 `Environment.onInstalled`. 外部 URI 的该钩子委托 `ExternalSources.deleteInstalled`, 且只有显式 `deleteSource=true` 才尝试删除. 失败的来源读取/解析, 用户拒绝确认及超时均不走此钩子. 本轮没有改变这条生产路径.

脚本路径仍由宿主在已确认成功后按文件所有权处理, 插件不从只读 PFD 推导或删除宿主路径. 外部 content URI 仍由其提供方决定是否接受删除; 拒删保留安装成功结果并附说明. API 24 上真实成功删除和提供方拒删的既有设备证据见 `p3-ui-evidence.md`. 本轮没有重新定义 content URI 的归属或放宽 FD 权限.

固定策略按安装项判定. 批量中已经确认成功的项目仍可按请求删除自己的来源; 后续项目失败或晚到取消不会把之前的成功改成失败, 也不能恢复已经删除的成功项来源. 既有 JVM 用例覆盖准备/确认/安装失败不调用删除钩子, 以及晚到取消保留已成功项处理结果且不删除未开始来源. 会话取消/超时和已创建平台 session 的放弃另见 `p2-session-evidence.md` 与 `p6-process-death-evidence.md`.

## 设备回归

新增 `ExternalInstallDeviceTest.deleteSourceRetainsFailedCancelledAndTimedOutSources`, 使用本次随机私有目录和 Debug 只读 provider. 每个请求显式指定 `authorizer=none`, `deleteSource=true`. 全部流程都在平台安装会话创建之前结束, 不点击系统安装确认, 不需要授予未知来源权限.

| 状态 | 触发 | 实际结果 | 删除尝试 |
| --- | --- | --- | ---: |
| 独立计数控制 | 明确调用一次只读 provider delete | UnsupportedOperationException, 文件未变 | 1 |
| 坏包失败 | 私有来源为无效 APK | INVALID_PACKAGE | 0 |
| 用户取消 | 真实插件确认页按本次 token 取消 | USER_CANCELLED | 0 |
| 查询超时 | provider 查询等待取消, 5 秒 deadline | TIMEOUT | 0 |
| 打开超时 | provider open 等待取消, 5 秒 deadline | TIMEOUT | 0 |

provider 的计数按完整 URI 隔离, 仅存在于 Debug 内存中, delete 仍固定拒绝写入. 四个实际状态均核验原始 SHA-256, 长度和文件存在, 并要求删除尝试为 0; 独立控制 URI 始终为 1, 避免计数器未工作造成假通过. 每项确认平台 session 集合与基线相同且未安装夹具. `FixtureHistoryOwnership` 只删除本次 token/ID 的终态记录, 验证其他历史对象和持久文件不变, 开始前拒绝满额或非终态历史.

| 设备 | 结果 | 用时 | 本地日志 |
| --- | --- | ---: | --- |
| API 24 x86 AVD, emulator-5554 | 1/1, 四状态及控制全通过, 无跳过 | 11.804 s | `build/p2-source-preservation24.log` |
| Xiaomi 23046RP50C, API 35, 968e9f18 | 1/1, 四状态及控制全通过, 无跳过 | 11.255 s | `build/p2-source-preservation35.log` |

两次最后均记录 `SUCCESS cases=4 controlDeleteAttempts=1 actualDeleteAttempts=0`, `historyRestored=true`, `platformSessionsUnchanged=true`, `fixtureAbsent=true`. 没有用提供方拒删导致的文件仍存在来替代未发起删除的证明. 无用户 APK 或已有历史被移除.

执行命令:

```powershell
$pkg = 'io.github.supermonster003.autojs6.plugin.three.setup.installer'
adb -s $serial shell am instrument -w -r `
  -e class "$pkg.ExternalInstallDeviceTest#deleteSourceRetainsFailedCancelledAndTimedOutSources" `
  "$pkg.test/androidx.test.runner.AndroidJUnitRunner"
```
