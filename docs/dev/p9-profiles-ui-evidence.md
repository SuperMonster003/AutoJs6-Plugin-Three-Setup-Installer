# P9 来源配置文件界面验收

日期: 2026-10-02. 本文记录原 P9 来源配置文件条目的页面和保存行为, 不替代安装入口, 匹配优先级或最终签名 Release 的运行时验收.

## 测试包与设备

- 任务自建 AVD `Three_Setup_Dhizuku_P8_API31`, 序列号 `emulator-5562`, Android 12 / API 31 / x86_64, 唯一当前用户为 0. 本轮由主线程启动并交由页面验收独占, 未操作用户物理设备.
- 保留应用数据覆盖原 1.2.0/build60 Release 后使用主线程固定的 Debug 与 androidTest. 未卸载主包, 未清数据, 未更改设备所有者或授权设置.
- 主 APK: 8,766,337 bytes, SHA-256 `ec09e1173a2ab72c20b9439eb0b10d526cf7331e6f777af4fd4c9072f09c1101`.
- 测试 APK: 1,640,940 bytes, SHA-256 `0c3f4caa116520000c9e826125dfdde3b66c89d5d758806eda69584093ce3d35`.
- 固定包来源 `build/profiles-fixed-debug-initial/`, 本轮副本, 原始日志, 设置 journal, 系统快照及截图均保存于 `build/profiles-ui-5562-20261002/`. 这些本地制品不提交.

## 实际通过范围

`InstallProfilesDeviceTest` 使用显式参数 `profileUiFixtures=true`, 限制模拟器 user 0, **4/4 通过, 13.351 s**:

1. 字段选择取消不改变草稿或持久配置; 字段确认, 完成单项编辑仍不持久化, 列表取消保持原值. 页面重建保留已确认的编辑草稿. 列表保存只产生一次偏好变更, 显式 `false`, `null`, `auto`, `current` 均保留为覆写键.
2. 新增, 启停, 上下移动及删除均先作用于列表草稿. 取消不生效; 保存后的顺序, 启用值, 来源和包名前缀符合界面操作. 空覆写配置文件保留, 不被当作无效项丢弃.
3. 页面打开后由另一保存操作改变配置时, 当前保存被 CAS 拒绝, 原编辑草稿仍显示. 取消重新载入不丢弃草稿, 明确确认后才载入另一处保存的值.
4. 无法读取的配置文件禁止新增和直接保存. 重置选择取消不生效; 确认重置后仍只是空列表草稿, 页面取消保持原损坏内容; 最后保存才恢复为有效空配置.

`StandaloneAppearanceDeviceTest` 使用 `profileAppearanceOnly=true`, **2/2 通过, 5.325 s**:

- 英文, 亮色, 默认字号, 浅色主题种子.
- 阿拉伯语, 暗色, 2 倍字号, 页面内容限制为 360 dp. 标题和首尾文本无省略, 固定底部新增/取消/保存按钮均处于系统安全区域且满足触摸尺寸.
- 只调整页面私有配置和内容宽度, 未更改系统字号, 密度, 夜间设置. 此处不是 Android 实际多窗口验收.

另通过实际界面点击 `Home -> More -> Settings -> Installation profiles -> Add`, 保存编辑条件页和滚动选项页截图, 再取消编辑与列表. 未保存新配置. 代表截图为 `profile-edit-top.png`, `profile-edit-options.png`, `appearance/ar-dark-font2-content360-InstallProfilesActivity-top.png` 及对应 bottom 图.

## 恢复与证据边界

- 原安装配置文件不存在. 每例在第一次写入前记录原始 document 与本轮拥有的 document, 恢复前逐字比较当前内容, 拒绝覆盖外部修改. 四份独立 journal 均为 `restored`, 最终 `installation_profiles.xml` 仍不存在.
- 原 `installer_persistent_default.xml` 前后 SHA-256 均为 `3325d2a819fdd8062c2cdc48a09b995c9b012915bcdf88b1cf9742a7f057c793`.
- 外观 journal 恢复原空偏好值, 所有启动器组件状态由既有外观辅助恢复. 该辅助会留下空的 `app-appearance.xml`; 原有效设置不变. 不将文件存在性变化写成字节完全一致.
- Dhizuku 设备所有者仍为 user 0 的 `com.rosan.dhizuku`, 实际页面仍报告已授权. Shizuku manager 的原许可保留, 服务器在本次 AVD 重启后未运行, 本测试未启动服务器或改动许可.
- 首次私有快照只包含 `shared_prefs` 和 `files`, 遗漏 `no_backup` 历史目录, 因而不能声称整轮历史前后字节一致. 页面用例没有发起安装调用. 最终只读记录显示 24 条已有终态历史, 结果为 completed/failed, interrupted 为 0, 最新更新时间为 `2026-10-02T00:36:05.934Z`, 早于本轮页面测试. 历史 SHA-256 为 `9d80b351856a98694f6eb18985e8d33df766881c1c439b92a6c70e10fbaf0201`; 后续手动截图操作前后此文件字节一致.
- 本轮没有真实 UI 测试失败, 无需生产界面修复. 统一编译前发现的测试字段误用已改为 `TaskSnapshot.state.terminal`, 不属于设备行为失败. 最终英文继承说明仅澄清对话框选择高于显式请求的优先关系, 不改变行为.
- 最终 PackageManager 的 Active install sessions 和 Finalized install sessions 均为空; 保留原系统历史 session 记录, 未进行清理.
- AVD 已交还主线程继续运行时和宿主验收, 按任务安排保留该固定 Debug/test 组合, 未恢复旧主包或测试包.
