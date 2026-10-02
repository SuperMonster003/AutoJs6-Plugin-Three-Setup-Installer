# P7 线上索引与宿主安装向导实测

日期: 2026-10-02. 本次完成真实宿主 UI 的线上发现, APK 下载校验, Android 系统安装与向导成功回执. 安装目标为首个公开 [v1.2.0 Release](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/tag/v1.2.0), 版本 1.2.0/build 63, 来源提交 `1e63028234e9542f13a1fec8aa75eaa09cf25114`.

## 发布与环境前提

主线程确认官方索引提交 `229b1afa10287ae1e53de2792415c13af23a8029` 已推送, [Actions 37011765516](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/actions/runs/37011765516) 已完成且成功, 才开始宿主首启. 官方 main 原始 JSON 的发布记录 SHA-256 为 `856cfa5c3e81df9293e741a18a5a793b2586ae4719b66b4549c1346139d3cdbd`. 宿主本次实际读取其固定官方 URL:

`https://raw.githubusercontent.com/SuperMonster003/AutoJs6-Official-Plugins-Index/refs/heads/main/plugins.official.generated.json`

旧任务 AVD 的注册与数据目录已经不存在. 经主线程明确授权, 使用本机现成 API 31 Play Store system image 新建干净任务 AVD, 没有复用用户设备或迁移旧数据:

- 名称 `Three_Setup_P7_Wizard_API31_0aa40118`, serial `emulator-5562`, emulator 进程 PID 46028.
- API 31, `x86_64`, hardware `ranchu`, model `sdk_gphone64_x86_64`.
- Fingerprint: `google/sdk_gphone64_x86_64/emulator64_x86_64_arm64:12/SE1A.211212.001.B1/8023802:user/release-keys`.
- 创建前和启动后归属 journal 保存在 `build/p7-live-wizard-0aa40118/journal.json`. 数据目录在该忽略目录的 `avd/` 下, 新注册文件为 `E:/.android/avd/Three_Setup_P7_Wizard_API31_0aa40118.ini`.
- 启动使用 `-no-window -no-audio -no-snapshot`, Windows helper 隐藏启动. 用户 `AVD_API_24` 的进程 PID 39396 前后身份一致, 没有对其发出设备命令或改变其状态.
- 新 AVD 原本没有宿主, 插件或测试 APK, 没有用户账号, device/profile owner 或特权管理器. 本轮未配置 owner, Shizuku 或 Root, 未安装任何 instrumentation/test APK.

宿主使用已经固定的本地签名 `AutoJs6 6.8.0 / build 5312` universal APK, 42,392,808 bytes, SHA-256 `4a7bef4fdd9a21c295176eeed7bf304c61b77dd05fc4fd4e80455b51322c7ff0`. 已验证 APK v2 签名. 这是包含相关集成的本地宿主构建; 本分工没有修改或构建宿主, 没有推送宿主仓库, 不将该测试描述为任意公开旧宿主都已支持新功能.

## 真实 UI 路径

1. 新宿主第一次启动自动显示 Plugin install wizard. 当时没有预置索引缓存, UI 先显示加载, 再列出来自正式官方索引的条目. 没有注入 JSON, 替换网络响应或使用本地假索引.
2. 通过向导的 Deselect all 清除预选项, 在 Tools 分类只选择 `3-Setup Installer`. UI 显示大小 2.14 MB, 最终按钮为 `INSTALL (1)`, 没有安装其他预选插件.
3. 从宿主真实生成的 `files/plugin_center/autojs6_plugin_index_v9.json` 只读导出该条目, 核对 source `OFFICIAL`, 版本 1.2.0/63, 最低宿主 5299, 下载 URL, 大小与 SHA-256. 下载 URL 为 [公开 Release APK](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/download/v1.2.0/autojs6-plugin-three-setup-installer-v1.2.0-8b427100.apk).
4. 宿主通过自己的 `PluginInstaller.downloadToCache` 从网络下载. 该生产入口按索引提供的 SHA-256 验证网络流. 在 Android 安装确认期间, 又从宿主实际下载缓存只读取出文件做独立校验; 没有向设备推入本地插件 APK 来替代下载.
5. 系统要求新宿主获得未知来源安装权限. 通过 Android Settings 中 AutoJs6 的 Allow from this source 开关授予本次必要权限. 没有使用 shell AppOps 写入, 没有授予悬浮窗或所有文件访问权限.
6. 第一轮权限设置自动恢复安装确认时, 测试驱动多按一次 Back, 取消了尚未提交的系统确认. 随后通过向导的 Cancel remaining 结束该轮, 回执为 Installed: 0 / Cancelled: 1. 这是明确的测试操作取消, 不是 APK 安装失败. 没有在后台重放这次安装.
7. 从宿主 Plugins 页的正常 Plugin install wizard 入口重新进入, 再次只选这一项, 真实重新下载并独立核验相同摘要, 然后在 Android 系统确认页点击 Install.
8. Google Play Protect 首次遇到该公开 APK, 显示 App scan recommended. 按本轮已确认的正常安装准备范围选择 Scan app. 系统随后明确显示 `This app looks safe` 和 `You can continue to install it`, 再选择正常 Install. 从点击扫描至首次保存安全结果的 UI XML 约 20.6 秒, 该值是观察时间间隔, 不是扫描器内部计时.
9. Android 显示 `App installed.`. 返回宿主后, 向导显示 `Installed: 1 - 3-Setup Installer`. 宿主 Plugins 页随后显示 1.2.0 (63), 已启用开关, 可用的 Uninstall/Settings 操作.

扫描期间没有关闭 Play Protect, 没有选择跳过检查或 Install anyway, 没有提交设备内其他文件. 安全结果仅记录此设备对此公开 APK 的实际提示, 不延伸为其他环境或后续版本的安全保证.

## APK 与安装结果核验

| 项目 | 实际结果 |
| --- | --- |
| 文件名 | `autojs6-plugin-three-setup-installer-v1.2.0-8b427100.apk` |
| 大小 | 2,140,911 bytes |
| 两次真实宿主下载的 SHA-256 | `d8ae073dfaaa155ce31540663f55395639be13b8d5074c57f6057ef62b5b5fbe` |
| APK v2 签名证书 SHA-256 | `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213` |
| 系统安装后的 `base.apk` SHA-256 | `d8ae073dfaaa155ce31540663f55395639be13b8d5074c57f6057ef62b5b5fbe` |
| 安装后的版本与 UID | 1.2.0 / 63, UID 10148 |
| 系统安装者记录 | `com.google.android.packageinstaller` |
| 宿主向导最终成功数量 | 1 |

网络缓存副本和系统安装后的副本均保存在本地忽略证据目录, 以便独立复核. 安装后 `base.apk` 与公开发行 APK 完全相同, 没有换成后续主分支测试修正构建.

## 最终状态与边界

- 新 AVD 相比创建后的原始包列表只增加 AutoJs6 与本插件两项, 没有移除任何包, 没有安装测试 APK 或其他插件.
- 插件原本缺席, 因而没有执行卸载保留数据或覆盖旧插件; 没有原用户插件设置或历史需要恢复. 未清除任何应用数据.
- 宿主下载缓存文件由向导在结束时自行删除, 已只读确认不存在. 线上索引缓存保留为本次实际网络获取结果.
- 安装完成后, 通过同一 Android Settings UI 将 AutoJs6 的未知来源安装开关恢复为关闭. 最终 AppOps 显示 `REQUEST_INSTALL_PACKAGES: deny`; 原未配置状态与此次显式关闭的有效限制相同, 不声称审计文本字节一致.
- 宿主提出的悬浮窗和所有文件访问建议均取消, 没有开放这些能力. 前后 preferred XML 字节一致, 没有设置默认安装器或触碰 chooser 默认项.
- 最终 active install sessions 为 0, device/profile owner 仍未配置. 没有安装或启动 Root/Shizuku/Dhizuku server. 本轮只走普通 Android 安装流程.
- 精确清理本次随机命名的 UI XML 临时文件. 本地 APK 副本, 截图和 journal 为审计证据保留.
- 主线程复核安装结果与扫描截图后, 再按 journal, 唯一 AVD 名称, live SDK/ABI 和进程 PID 46028 校验归属, 仅关闭本轮 `emulator-5562`. 最终 journal 为 `verified-success-and-task-avd-closed`, 关闭时间 2026-10-02 13:41:05 UTC. 用户 AVD_API_24 的 PID 39396 仍保持运行. AVD 数据与本地证据保留, 未提交或推送本分工文件.

本次提供的是固定 Host 5312 在 API 31 上的真实公开发布安装链路证据. 不把新的首次安装实验写成已有插件升级, 既有用户数据迁移, 多 ROM 向导或远端宿主发布验证.

## 本地证据位置

统一目录为 `build/p7-live-wizard-0aa40118/`:

- `journal.json`, `boot-identity.json`, `baseline/`, `final/windows-process-identity.json`: 新 AVD 的归属与公开基线.
- `host-live-index-cache.json`, `host-selected-online-entry.json`: 宿主从线上索引实际得到的条目.
- `downloaded-through-host-wizard.apk`, `retry-downloaded-through-host-wizard.apk`, 两份 download verification JSON, `downloaded-apk-signature.txt`: 两次真实下载及签名校验.
- `wizard-installer-selected.png`: 正式条目与只选一项的状态.
- `wizard-first-attempt-cancelled.png`: 首轮确认取消的如实记录.
- `install-landed.png`, `play-protect-scan-started.png`, `play-protect-scan-outcome.png`, `play-protect-scan.json`: 首次系统安全扫描的提示与结果.
- `after-safe-install.png`, `wizard-final-summary.png`, `final-visible-host-result.png`: 系统安装成功, 向导 Installed: 1, 宿主识别已安装插件.
- `installed/installed-plugin.apk`, `installed/installed-result.json`, `final/result.json`: 设备安装结果摘要与最终状态检查.

UI 操作和对应 XML/截图均已保存. UI 驱动只使用设备界面与输入操作; 读取宿主下载缓存用于验证, 没有修改宿主数据以伪造安装结果.
