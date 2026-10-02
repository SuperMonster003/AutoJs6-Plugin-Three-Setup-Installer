# P9 高级安装选项设备证据

日期: 2026-10-02, Asia/Shanghai. 本文仅记录 emulator-5554 与 QV770340J7 的 P9 第一项验收. API 35 的 Shizuku/shell, 更新所有权及宿主脚本结果由其他证据记录, 不合并为本文测试次数.

## 固定构建与夹具

本轮初始 Debug 为 1.2.0/build 57 的实施中产物, 不作为最终发行号. 两台使用同一已固定 APK, 覆盖前核对摘要, 没有卸载主包或清除数据.

| 产物 | 字节数 | SHA-256 |
| --- | ---: | --- |
| 初始 Debug main.apk | 7,740,844 | `fadffc87bc9fc3641a5c0c35a0c5c5a61d1ca8db5dea0b062c4f6808d8d1c5dd` |
| 初始 androidTest test.apk | 1,615,184 | `4b619b1f455154233d6f285fc8a2d72bc94aa1feb26ede004c572d5ffea1e4ac` |
| advanced-fixtures/v1.apk | 8,593 | `bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0` |

夹具包名为 `io.github.supermonster003.autojs6.installer.advanced.fixture`, minSdk 24, targetSdk 28, versionCode 1. 唯一 DEX 类只有整数运算, 没有 Android 组件, 权限访问, I/O 或网络代码; 声明 READ_CALENDAR 和 INTERNET 分别观察运行时权限与普通权限. 本轮没有启动该应用. 与 P2/P8 的无代码夹具不同, 它保留 DEX 供编译请求使用, 但不把编译请求被接受扩大成已经运行应用代码.

## 设备原始状态

| 项目 | emulator-5554 | QV770340J7 |
| --- | --- | --- |
| 型号 | Android SDK built for x86 | Sony XQ-DQ72 |
| API / ABI | 24 / x86 | 33 / arm64-v8a |
| SELinux | Enforcing | Enforcing |
| 原主包 | Release 1.1.0/build 57 | Release 1.0.0/build 51 |
| 原主包 SHA-256 | `d1300f2b1ac97c240bc02e58752909d1fa5b78a18397d3894ac5ce1aef8df31e` | `547334493ecd76f9815167c2159e5730de5ac14cf310674d5cbd7dfa30ae29d0` |
| 主包 / 测试包 UID | 10293 / 10294 | 10623 / 10659 |
| 原活动安装 session | 0 | 20, 全部逐 ID 保存 |
| 原持久默认项 | 0 | 0 |
| 原私有文件记录数, 主包 / 测试包 | 11 / 0 | 8 / 0 |

本轮两台原测试 APK 均从各自设备直接导出, SHA-256 都为 `8e253348315d83e0b77bd41dfe99ed4669c13ef63be01a3fa72b762602fd8c51`. 主包与测试包原始 APK, 包元数据, 授权, app-op, 用户, 完整普通默认 XML, 持久段, session, 原服务进程和私有文件副本保存在 ignored 的 `build/p9-backend-devices/<serial>/baseline.json` 及同目录. 不依赖另一台设备的原包推定本机初始摘要.

每次设备操作均指定 serial, 首次与部署前重新读取 SDK/ABI. 原默认 XML 摘要分别为 `fcef793563af6a52188a60ee15c847f904cec91fbd1b1a96bfa9429415582c8c` 和 `b54f15311e4741b35deb26c6c005556f4e7158b02e11ddf7e50187947f5239f5`. Sony 的受保护策略文件采用 ABX, 仅用系统 abx2xml 转换到标准输出后只读解析, 没有写回系统文件.

## 实际结果

每组包括 `AdvancedInstallOptionsDeviceTest` 两项与 `PrivilegedMetadataDeviceTest` 两项, 共三组 12/12 通过, 无跳过.

| 设备 / 授权通道 | 实际服务 UID | 结果 | 仪器用时 |
| --- | ---: | --- | ---: |
| API 24 / Root | 0 | 4/4 | 2.501 s |
| API 33 / Root | 0 | 4/4 | 3.754 s |
| API 33 / Shizuku | 0 | 4/4 | 3.475 s |

Sony 原 Shizuku server 为 root UID 0, PID 6403, 本轮 UserService 也实际返回 UID 0. 该行证明 Shizuku 的 Root 身份通道, 不冒充 shell UID 2000 验收. 原 server 没有重启或换身份.

| 能力 | 请求与实际观察 | 证据范围 |
| --- | --- | --- |
| grantAllRequestedPermissions | 首次不带 0x100, READ_CALENDAR 为 denied, INTERNET 为 granted; 同一固定 APK 再安装并带 0x100 后 READ_CALENDAR 为 granted | 两台三组均通过真实 PackageManager.checkPermission 读取, 不是只检查入参 |
| installReason | API 33 明确传 user, 私有 createSession Bundle 的值为 4; API 24 显式 unknown 在开启 session 前拒绝 | API 33 此处核验实际发送参数, 没有宣称系统最终持久 reason 已独立读回 |
| packageSource | API 33 明确传 other, Bundle 值为 1, 安装后 InstallSourceInfo.packageSource 实际读回 1; API 24 显式 unspecified 在开启 session 前拒绝 | 参数与系统读回均记录, 未传选项的旧行为保留 |
| requestUpdateOwnership | 本文设备 API 均小于 34, 显式 true 被前置拒绝 | 不把其他设备的 owner 成功或 public/private 可见性结果算入本文 |
| dexopt | 请求 speed, 后端实际执行固定单包命令, 退出码 0 且输出 Success, 结构化结果为 accepted | 旧平台 Success 可能包含跳过, 不宣称 PERFORMED; 没有全局编译, 清 profile 或自动重试 |
| 编译超时后的安装事实 | 第三次真实替换安装成功后, 测试代理把真实后端 postInstall 的预算设为 0; 返回 timeout 且没有进程退出码, 安装结果仍 ok=true, versionCode 仍为 1 | 证明未启动编译的耗尽预算路径, 不冒充已经实测长时间编译中断 |

前置拒绝用例经过真实 `SessionInstallEngine` 的参数校验, 在受监测的 `openSession` 之前结束. API 24 共 8 个组合, API 33 共 5 个组合, 每组 platformOpens=0, authorizerRequests=0, 既有 PackageInstaller session 集合保持. 组合包括 none/Dhizuku 请求 grant 或 dexopt, 以及真实当前 SDK 不支持的显式 metadata/verify. 没有为这些拒绝用例初始化 Dhizuku 或申请 Root.

只读元数据用例比较本机已安装插件的真实原始证书, 包名/user, 版本和 lastUpdateTime 与公开 PM 结果, 并证明构造的保留数据/未知签名记录不会被编码成 found=false. 后者是编码边界用例, 不是实际跨用户安装实验. 私有查询的全局记录与 installedForUser 分开; 本文不声称已经在第二个用户完成安装验收.

## 数据归属与清理

夹具只从冻结的 androidTest asset 读取, 每次校验完整 SHA-256, 在原先不存在的随机自有缓存目录中准备. `FixturePackageOwnership` 检查每个用户的完整 `pm list packages -u` 输出及 android 哨兵, 拒绝任何原夹具或保留数据. helper 只增加两个固定 advanced 包名白名单, 未开放任意包名. `FixtureHistoryOwnership` 先拒绝未完成历史, 对直接 engine 操作不认领或删除任何历史行, 收尾要求原记录完全一致.

成功路径使用同一实际授权引擎卸载本次夹具; finally 仍按原缺席证明做精确夹具清理. 两台均核验全部用户包含保留数据的夹具缺席, 原 session 集合恢复, 无安装前台服务或遗留插件特权 helper. 随机源目录清理前核对仍位于本次应用缓存父目录, 没有清全缓存或全历史.

Root 和 Shizuku 测试后, 原普通 XML, 持久空集, 用户列表, 0/20 个系统 session, 主包权限/app-op/全局来源设置及记录的全部主包私有文件摘要逐项保持. 原 Magisk 进程 PID 1338/8221 与 Sony Shizuku PID 6403 均保持. 进程比较按 UID/PID/PPID/名称核对, 不把正常 RSS 变化当成进程重启.

Debug 测试 APK 的临时 Manifest 权限与原独立 Release 测试器不同. 测试结束已经用各自精确备份保数据覆盖恢复测试 APK, 再核验实际摘要, UID, 权限与私有文件. 两台 `after-test-restore.json` 均无基线差异, `test-restoration.json` 记录恢复依据; 没有卸载预先存在的测试包或手动重置权限来制造一致.

完整日志位于 `build/p9-backend-devices/<serial>/initial-root/instrumentation.log`, Sony 的另一组为 `initial-shizuku/instrumentation.log`. 此阶段结束时主包暂留上述验证用 Debug, 原测试 APK 已恢复; 最终发行状态见下节.

## 最终 Release 60 交付

两台均保数据覆盖为同一非 Debug 1.2.0/build 60: `autojs6-plugin-three-setup-installer-v1.2.0-ce29bb60.apk`, 2,074,763 字节, SHA-256 `00adf451b1875b5b652975a740657e3079e0954e786056bec3a05ca705822b33`. 部署前再次核对 SDK/ABI, 原测试包和完整基线; 部署后直接读取实际已安装 APK 核对摘要及版本. 没有重跑上述高级选项矩阵或把其中间 Debug 结果改写为最终 R8 全矩阵.

使用独立 Java/平台发行测试器补验同一最终 Release, 测试器为 17,892 字节, SHA-256 `dbffb97dbfaa08fa73542b0de18b86e664ed5019cfca0ab4dca666274be65fce`. 两台各 2/2 通过, 无跳过, 证明发行产物非 Debug/无调试组件/无 native, 从实际 R8 APK 加载公共协议完成 INFO/INSTALLER 往返, 并拒绝非宿主 UID 的六类业务操作.

| 设备 | 插件 / 探针 UID | 插件 / 探针 PID | INFO / 全部往返 | 结果 |
| --- | --- | --- | --- | --- |
| emulator-5554 / API 24 | 10293 / 10294 | 28238 / 28256 | 15 ms / 70 ms | 2/2 |
| QV770340J7 / API 33 | 10623 / 10659 | 10006 / 10025 | 13 ms / 51 ms | 2/2 |

测试后分别覆盖恢复各自原测试 APK, 实际摘要仍为 `8e253348315d83e0b77bd41dfe99ed4669c13ef63be01a3fa72b762602fd8c51`, 测试包 UID/权限/记录的私有文件相同. 最后审计确认主包 UID, 原授权/app-op/来源设置, 普通默认完整摘要, 持久空集, 0/20 个原系统 session, 偏好/历史和原服务进程身份保持. 夹具含保留数据在所有用户中均缺席, 无安装前台服务或本轮插件 helper.

应用更新后, 两台 AndroidX `files/profileinstaller_profileWrittenFor_lastUpdateTime.dat` 自然刷新, Sony 另有 `files/profileInstalled` 更新. 这些框架性能记账变化单独保留, 不手工回写来声称所有目录字节相同; 其余记录的主包私有文件均与原基线相同. `build/p9-backend-devices/<serial>/final-delivery.json` 和汇总 `build/p9-backend-devices/final-delivery.json` 均为 phase=`delivered`, 包含实际 APK/test 摘要, 两项结果, 完整原 session IDs 和记账前后摘要. 原始发行测试输出在同目录 `release60-contract.log`, 最终核对为 `final-audit.json`.
