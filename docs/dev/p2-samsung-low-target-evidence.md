# P2.3 Samsung 低 targetSdk 实际拦截与绕过

日期: 2026-10-02, Asia/Shanghai. 维护者为原 P2.3 的低 targetSdk 缺口提供 Samsung SM-A566B. 本轮在同一台设备和同一固定 targetSdk 22 夹具上, 实际观察到未启用 `bypassLowTargetSdk` 时的系统拒绝, 随后通过生产 Shizuku 安装引擎启用该选项并安装成功. 这补齐 [先前矩阵](p2-matrix-completion-evidence.md) 的 "没有原生拦截可供绕过" 边界, 不改写原 HyperOS 对 targetSdk 22 本来就接受的结果.

## 设备与初始基线

| 项目 | 实测值 |
| --- | --- |
| 型号 / serial | `SM-A566B` / `localhost:44836` |
| Android / API / ABI | 16 / 36 / `arm64-v8a` |
| 页大小 | 16,384 字节 |
| ROM | `samsung/a56xnaeea_16kb/a56x:16/BP2A.250605.031.A3/A566BXXU6BYIF_OXM6BYIF:user/release-keys` |
| One UI 原始属性 | `ro.build.version.oneui=80000` |
| 安全补丁属性 | `2025-10-01` |
| 当前用户 / 枚举用户 | 0 / 仅用户 0 |
| 初始插件 / Shizuku 管理器 | 均未安装 |
| 初始特权环境 | 无运行中 Shizuku server, 未找到可执行 `su`, 未发现 Root 相关进程 |
| 初始固定夹具 | Core 五种包和 Spike 包, 包括保留数据, 均不存在 |
| 初始活动安装 session | 空集 |

第一轮读取后无线 ADB 暂时失联; 对维护者给定的同一地址执行一次有界重连即恢复, 没有重启全局 ADB server 或操作其他设备. 后续在任何 APK 部署前保存完整 preferred XML, 活动 session, 用户/夹具, 授权环境和相关系统设置基线. 本地目录将 serial 中的冒号替换为下划线: `build/p2-samsung-low-target/localhost_44836/`.

## 测试准备与授权

使用已构建的插件 Debug 48 和对应 Debug androidTest, 未修改生产源码或测试源码. 新装 APK 成功后先核对 Debug 主包 versionCode 48 和 SHA-256 `c9ce1439046a0f14c2216e2f9383f6450ea6738cd22a28c2c7fba882ab2726ba`.

Shizuku 管理器取自同一设备池 Sony 已安装的唯一 base APK, 本轮分别核验包名, 版本, 摘要和签名, 再在三星安装:

| 项目 | 值 |
| --- | --- |
| 包名 | `moe.shizuku.privileged.api` |
| 版本 | `13.6.0.r1086.2650830c` / 1086 |
| APK 大小 / SHA-256 | 2,571,773 字节 / `6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f` |
| apksigner | 校验通过, 证书 DN `CN=Rikka` |
| 证书 SHA-256 | `268b5590e868fb08bae7e0ac413564cd1ff88f5ccff74af9dbd0dc918e30db30` |

启动命令来自该管理器实际 `View command` 页面, 指向已安装包的 `lib/arm64/libshizuku.so`. 执行前确认当前没有 server, 并核对设备上的 starter 与上述 APK 内对应二进制的 SHA-256 相同. 通过 ADB 正常启动后实际 server 为 shell UID 2000, PID 19058; PID, UID, `/proc` startTicks 和命令行写入 `shizuku-start-journal.json` 供精确收尾使用.

插件首页实际显示 Shizuku 可用/运行中/未授权. 点击 Shizuku 行的 Authorize, 在管理器明确显示 `Allow 3-Setup Installer to access Shizuku?` 的对话框中接受 `Allow all the time`. 没有通过改管理器私有文件或注入授权状态来代替正常请求.

none 用例需要包级未知来源许可. `tools/install-permission-journal.py` 在写入前持久记录新插件 UID 10320 的原 package=`default`, uid=`default`, 临时设置测试所需模式; none 结束后立即恢复两个原模式, 再运行 Shizuku 用例. 因而 none 结果属于预授权下的引擎拒绝验收, 不作为首次 Settings 授权 UI 的证据. 恢复 journal 为 `11ea2370017e4efa985ca71c47376210`, 最终 `pending=false`, `phase=restored`.

## 固定夹具与实际结果

只使用 `app/src/androidTest/assets/core-fixtures/low-target.apk`:

| 项目 | 值 |
| --- | --- |
| 包名 | `io.github.supermonster003.autojs6.installer.core.lowtarget` |
| 版本 | versionName 1.0 / versionCode 1 |
| minSdk / targetSdk | 21 / 22 |
| 大小 | 8,536 字节 |
| SHA-256 | `a13fba3b7d5303933cda0a113fb371e4166c596df3b9092c903b9335e4adcc35` |
| 内容 | 无 DEX, 原生库, 权限, 启动器或导出组件, `android:hasCode=false` |

两次只选择现有 `CoreInstallMatrixDeviceTest#lowTargetSdkBypassAndPre34Note`, 参数均显式包含 `confirmFixture=true`, `engineAuthorizer` 分别为 `none` 和 `shizuku`. 夹具经生产 `NoneInstallEngine` / `PrivilegedInstallEngine` 安装, 没有使用 `adb install --bypass-low-target-sdk-block` 代替插件能力.

| 用例内步骤 | 请求身份 / 选项 | 实际结果 |
| --- | --- | --- |
| none 普通请求 | none, bypass=false | 系统返回 `INSTALL_FAILED_DEPRECATED_SDK_VERSION: App package must target at least SDK version 24, but found 22` |
| Shizuku 普通请求 | Shizuku shell UID 2000, bypass=false | 同一系统错误, 要求至少 24 而夹具为 22 |
| Shizuku 开启选项 | 同一 Shizuku 身份, bypass=true | 安装成功; 用例调用实际已安装版本查询断言 versionCode=1, 并断言 API 36 的该选项没有被记为旧 API 上的 ignored |

Shizuku 用例只准备一次同一来源, 先验证无标志请求的真实拒绝, 再将 `bypassLowTargetSdk=true` 交给同一生产引擎. `shizuku lowtarget verified` 仅在实际安装和版本断言全部结束后输出. 后续精确夹具清理也独立通过. 本轮 Shizuku 请求使用现有测试的 `interaction=auto`, 日志没有系统确认回调; 不把它写成另一次显式 `silent` 用例.

none 日志包含生产用户确认桥的回调, 随后的终态明确为低 targetSdk 拒绝. 本轮没有调整 Auto Blocker, 安全扫描, 验证器或低 SDK 相关全局设置来制造成功, 也没有确认危险警告. 已记录的 verifier/未知来源相关全局及 secure 设置在收尾时与初始值相同.

| 最终日志, 相对于上述本地证据目录 | 通过 / 跳过 / 失败 | instrumentation 报告耗时 |
| --- | --- | ---: |
| `lowtarget-none.log` | 1 / 0 / 0 | 0.757 秒 |
| `lowtarget-shizuku.log` | 1 / 0 / 0 | 1.963 秒 |

以上耗时包括测试准备和清理, 不作为性能基准. 没有将保护性跳过或 runner 的空成功算作通过, 也没有重复采样已经通过的同一组合.

## 收尾与交接

两个用例在操作前拒绝全用户范围内已存在的夹具/保留数据或旧夹具 session. 用例结束先等待自身平台 session 结算, 再卸载仅本次拥有的固定包, 核对所有枚举用户不存在, 删除本次私有 `core-matrix-*` 来源目录, 最后释放特权服务. 独立 Debug 审计确认 `cache` 和 `no_backup` 均为空, 没有安装历史文件或新的夹具历史; 该引擎测试没有调用全局历史清理.

临时 UI 目录为 `/data/local/tmp/three-setup-samsung-317ae7300b6d44bbb875e9941bd432ad`. 清理前核对随机路径, 非 symlink, 唯一文件 `window.xml` 以及与本地最后快照相同的 SHA-256, 只删除该文件和空目录, 随后确认目录不存在.

Shizuku server 停止前再次核对本轮 PID 19058, UID 2000, 原 startTicks 和 `shizuku_server` 命令行, 只对该进程发送 SIGTERM. 最终无 server, 恢复原先没有运行服务的状态. 管理器, 本轮对插件的明确授权以及测试 APK 按测试准备授权保留, 便于后续验收. 因原先未安装插件/管理器, 这些是本轮明确新增的依赖和授权, 不描述成所有已安装包集合未变化.

交接前保留数据覆盖同一签名 Release 48 并重新读取 SDK / ABI / 包版本 / 实际 APK 摘要:

- 插件 1.0.0 / versionCode 48, debuggable=false, SHA-256 `6cc923343072e52ad9cf27fcc939ce35bf8488394ad6b2f4b7f4e4cc8b9f3efe`.
- 用户 0 中全部固定 Core/Spike 夹具和保留数据不存在, 活动安装 session 为空, 无插件安装前台服务.
- 完整 preferred XML 前后 SHA-256 均为 `1de4a4dd9674915afa5b18cf6b199b7e9554e9b6160b05d42354eb6c656e1ba4`. 没有清除或设置任何默认安装器/最近使用项.
- 包级和 UID 级安装许可恢复为 `default`, Shizuku 临时 server journal 已为 `pending=false`, `restored=true`.
- `completion.json`, `restored-release48-summary.json`, `debug-private-cleanup.json`, `artifact-digests.json` 和两个恢复 journal 保留完整审计. 后续统一版本交付由会话最终记录另行说明.

本证据覆盖三星该固件上真实低 targetSdk 拒绝以及 Shizuku 标志实际解除拒绝, 不扩展为全部 OEM 或 Root 身份的相同结论. 原 P2.3 的跨 API/授权矩阵仍与其他设备补充结果合并判断; 本文不修改原路线图结构, 不执行远端发布.
