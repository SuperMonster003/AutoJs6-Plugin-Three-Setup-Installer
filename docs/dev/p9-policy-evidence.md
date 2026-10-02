# P9 签名门禁, 黑名单与权限预览

日期: 2026-10-02. 对应原 P9 第二项, 不新增或拆分路线图条目. 本文仅记录已实施及已验证范围, 不将 Debug 白盒验证冒称为最终 R8 全矩阵.

## 实现边界

- 所有生产安装入口共用 `DescriptorInstallEnvironment` 的安全检查. 黑名单在普通确认前检查; 选项, 分包与目标确定后评估签名. 签名例外只由当前实际附着的安装对话框生成, 不属于脚本选项, 通知动作, 设置默认值或恢复数据.
- 有风险时, 同一对话框先完成原有选择, 再展示最终选择对应的签名审核页. 复选框默认未选, 仅点击安装按钮不足以放行. notification / silent / 保持静默的 auto 不提供此例外; auto 本来已经选择 dialog 时可以审核. 放行只允许继续尝试, Android 仍可能拒绝安装.
- 审核不持有包锁. 获取包锁后, 写入前重新检查选中 APK 内容摘要, 目标用户/范围, 授权方式, 已有签名/版本事实和黑名单版本. 一次性凭证绑定当前 record, prompt, item 与这些事实; 不同事实或重复消费都拒绝, 不自动重复弹出审核.
- SH/Root 使用真实特权查询和跨用户匹配, 包含保留数据及其它用户的已有包记录. none/Dhizuku 只承诺当前用户签名预检, 当前用户明确未找到可以作为当前用户的新装处理. 此负结果不是全局不存在的证明: 配置 SharedUID 黑名单时, 无法排除其它用户的已有组会拒绝安装. 查询失败也不得作为未安装.
- 包名和 SharedUID 规则精确且区分大小写; 既检查声明的 SharedUID, 也检查已有安装的组. 黑名单不能由单次签名审核覆盖. 规则读取失败时拒绝安装, 包括 Android 把损坏的 SharedPreferences XML 读成空映射的情况. 只有规则文件实际不存在才视作未配置.
- 签名预检依赖 Android 对 base APK 的验证. 普通可写来源先复制到本请求私有目录, 复制时验证原内容摘要, 平台只验证这个只读快照. 确实处于当前请求私有 staging 中且没有内部符号链接的 base 可直接验证. 证书事实按已验证 SHA-256 在本次环境缓存; 临时快照用后释放, 最终写入仍验证各个选中 APK 的摘要. 这一步可能需要额外临时空间.
- 不用签名方案存在性或历史证书集合的任意交集冒充匹配. 完整当前签名集合相等才自动通过; 合法签名轮换也可能要求一次审核. 其它 split 的完整签名一致性仍由 Android 最终验证. 最后检查后发生的外部系统竞争也由 Android 最终裁决.
- 权限预览是当前选中 APK 清单中的声明列表, 不是已授予权限或无条件可授予权限. 使用共享解析 AAR 的字段; 最终解析器对真实元素, Android namespace 和 typed attribute 进行严格校验, 对应宿主来源及单测由主线程记录.

## JVM 守卫

本插件集成 JVM 首轮 347 项通过, 第一个高级选项条目的隔离阶段 333 项通过, 数量按主线程构建记录. 本条目的新增检查包括:

- `InstallSafetyPolicyTest` 10 项: 未配置与合法空列表, 损坏存储, 精确规则和上限, incoming / installed SharedUID, 当前用户负结果的限制, 未知签名, 多签名完整集合, 所有审核事实的失效, 调用者集合防变, 16 线程竞争只允许一次消费.
- `policy.InstallSessionPolicyTest` 3 项: 四种交互统一写前拒绝, 普通确认后的最终拒绝, 审核不持包锁且最终校验必须等待包锁.
- `InstallSafetyRecoveryTest` 1 项: 即使已经勾选并创建决定, 恢复快照也不包含审核 token, 规则版本或可用凭证, 不提供重试入口.

高级选项的早期成功与 `followUpPending` 恢复检查单独放在 `InstallFollowUpRecoveryTest` 和 `InstallSessionTest`, 不混入本条目的凭证模型, 以便两个原路线图条目分别提交.

## 专属 API 31 AVD 的真实界面与平台验证

设备 `emulator-5562`, API 31, 本轮专属设备, 预先激活并授权的 Dhizuku. 没有修改用户设备. 开始时已有主线程保留的 10 条终态历史, 以本轮直接读取为基线; 不清除这些历史.

固定测试构建保存在 `build/p9-policy-emulator-5562-46ad6d8a/`:

- main.apk: 7,780,792 bytes, SHA-256 `29e4fc15f4731bcd3b82632c4d76e68148a20d72321429e40978106a09294e6d`.
- test.apk: 1,629,215 bytes, SHA-256 `d2da856b7d2e13a629ab0dc14c7f9071c949842277a48cafd9f378a400a494db`.
- `InstallSafetyPolicyDeviceTest`, `policyFixtures=true`: 4/4 通过, 无跳过, 27.666 秒; 日志 `policy-device-first.log`.

四项内容:

1. 包名黑名单在 auto / dialog / silent / notification 全部返回 `BLOCKED_BY_POLICY`; 声明 SharedUID 的固定夹具也被拒绝. 没有改变平台活动 session 集合, 没有单次放行入口.
2. 先真实安装固定 v1, 再使用异签名 v2. 在真实插件对话框中展开权限, 读到 `android.permission.READ_CALENDAR` 与 `android.permission.INTERNET`. 普通确认之后出现独立签名审核页, 未勾选时安装按钮不可用, 即使直接调用按钮动作也不提交. 明确勾选并继续后到达 Android, 以 `SIGNATURE_MISMATCH` 拒绝; 已有应用仍为 v1.
3. 相同异签名审核期间更新黑名单版本, 再勾选继续, 得到 `BLOCKED_BY_POLICY`; 不创建新的平台 session, 不循环弹出第二次审核, 已有 v1 保持.
4. 将固定 v1 原文件在快照创建后改写成异签名 v2. 私有快照内容摘要仍精确等于 v1, 平台从该快照读取有效签名及版本 1. 这是有界的可写 inode 变化验证; 不假称对所有 ROM 的平台解析实现进行了证明.

固定夹具由主线程 `tools/build-advanced-fixtures.ps1` 生成, 没有自动运行的 Android 组件或权限访问代码. 本用例消费的摘要为:

- v1.apk: `bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0`.
- v2-other.apk: `171e52e90eac7b93d9406732fd7960ce47eea0c500a23f6089c7eb3ef8e8ad3e`.
- shared.apk: `7833a3851db3fadfd3f6b4723a5b20f05b1ae2cd903aeec97f19be89e8ef84ad`.

`FixturePackageOwnership` 只接受三个既定测试包, 仍拒绝任何用户中的既有夹具或保留数据. `FixtureHistoryOwnership` 只认领本次固定包, 外部来源, 当前 token 和创建时间, 逐项移除且核验其他记录. 黑名单设置只在原 policy 文件不存在时临时写入, 使用独占 journal, 清理前核对最后一次写入的完整状态; 发现外部变更则保留 journal 而不是覆盖.

## 收尾与证据限制

- 独立于上述四项白盒用例, 主线程使用真实宿主 build 5309 / UID `10152` 的 V3 脚本, 在第二个固定 Debug (`4fb2149a6d81feddad0e048f49cf39901412429e1e6afb901a12cac4a82a3a26`) 上完成 Dhizuku v1 安装, 同签名 v2 更新, 异签名与 unsigned 分别 `BLOCKED_BY_POLICY`, 以及真实卸载. 结果在 `build/p9-host-script/debug-b70731afee2747be8424860971a682d0/result.json`, `ok=true`, `terminalCount=1`; 该主线程实验与策略单测, 四项 UI 实机用例分别计证据, 不相互替代.
- 四项结束后原 10 条历史的字节摘要完全相同, 完整 preferred XML canonical 相同, 原偏好文件集合相同; 临时 policy XML 与 journal 均已移除. 两个本次夹具在所有用户中均不存在, 无活动安装 session 或插件前台服务. 检查结果保存在上述目录的 `state.json`, 原始前后文件一并保留.
- 此轮设备直接覆盖声明 SharedUID 的拒绝; 省略 incoming SharedUID 后仍命中既有组, 以及无法取得全局身份时的拒绝, 在纯策略测试覆盖. 未把这些纯测试写成已完成同等真实安装场景.
- 上述固定测试包消费当时解析器. 后续严格权限解析 AAR 的构建, JVM 与最终 Release 验证由主线程统一记录, 不将该新 AAR 的验证归给这次较早的固定 APK.
- 测试类最终用 `SdkSuppress(minSdkVersion = 28)` 表明专属用例的运行前提. 首次使用 `RequiresApi` 被测试专用 lint 规则拒绝, 已按要求改正; 本轮实际设备为 API 31, 标注不改变已执行分支, 不把低版本跳过计入实测通过. `emulator-5562` 已交还主线程.
- 原 P9 的按来源配置文件条目尚未实施, 本文不将它计入完成范围. P7 远端发布仍延迟.
