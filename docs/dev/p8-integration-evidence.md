# P8 1.1.0 集成验收

日期: 2026-10-02, Asia/Shanghai. 维护者明确允许 P7 远端发布继续延迟时实施 P8. 沿用原五个条目, 不新增, 分拆或丢弃条目; 不推送, 打标签或发布任何仓库.

## 契约与授权

- 宿主 494e3e9493 / build 5306 引入 V2, bd9817980f / build 5307 增加可选 persistentConfigured 回执字段. 第一至第十个 AIDL 事务保持, 仅末尾追加 setDefaultInstallerV2. 插件继续声明最低宿主 5299, 基础能力版本 1, 最大版本 2; 新功能由同一 Binder 的实时能力协商, 不试调用未知事务. 未改变结构的结果 envelope 保持版本 1, 旧客户端仍可解码.
- `installer-api.aar` 为 bd9817980f 的 release 产物, 31,659 字节, SHA-256 `5e2cdf1a5440d9d544d1f8bb82a4460dd12055c5b948c1e3dc038f941ca94fb6`. 三份来源/锁文件同步, 共享解析 AAR 不变.
- Dhizuku API 2.6.0 为 MIT, AAR 38,692 字节, SHA-256 `1dcf1d29032a0799e8878cd1c46d987de7775bba5e7e873b60962ab9a73470e5`. 完整许可证随 THIRD_PARTY_NOTICES 打包; GPL 管理器独立安装, 不打包其代码或资源. API 26 以下不初始化 Dhizuku, 其他路径保留 API 24.
- 新配置默认 auto 顺序为 Shizuku / Root / Dhizuku / none. 旧配置升级保留相对顺序和禁用项, 仅插入未启用的 Dhizuku 选择. 显式身份不回退; Dhizuku 仅当前用户, 归属实际 owner, 不伪装 Root/shell flags 或 keepData.
- Dhizuku owner/provider 包, UID, 用户, 当前 admin 和签名身份均核对; 只包装新建框架对象, 不修改共享 PackageManager. 随机来源标记与私有 journal 只用于已知会话回收. 下一次有效 Dhizuku 安装或卸载触发恢复, 不扫描 owner 全部会话或重放安装. API 26/27 无来源标记读回, 创建回复丢失或落盘前死亡无法证明归属时保留. 专项实测和边界见 [Dhizuku 会话证据](p8-dhizuku-session-evidence.md).

## 持久默认与通知

持久默认的被动查询不从本地回执推断系统策略. persistentConfigured 仅表示对应 owner 的上次成功配置; 普通 method 仍为实际观测的 preferred / none. 完成的持久写入响应可确认 method=persistent; 清除后普通首选仍可能指向插件. 首页与设置页明确区分当前解析和历史配置.

Dhizuku 持久策略限定 API 26-33; API 34+ 的最终策略结果需要 owner 的 PolicyUpdateReceiver, 本插件无法取得该结果时在写入前拒绝, 包括清除. 升级系统后不能把旧回执当作可清除凭据; 可用的 Root/system 路径或 owner 管理工具需独立处理. 写入前保存 pending 状态, 部分失败不自动清除未知旧策略. auto 只从启用且可用的 Root/user0 与 Dhizuku/API26-33 选择持久路径, 不选择 Shizuku/none.

Root 的独立 UID/GID 1000 调用, 固定 user0 和本插件四种 APK filter, 实际持久 XML/解析验证及取消协议见 [Root/system 证据](p8-root-system-spike-evidence.md). 既有竞争策略, 未知 filter 或无法验证的平台会被明确拒绝; 不修改共享 RootService 身份或 SELinux.

notification 只用于安装, 确认/取消/进度/结果均在通知中完成, none 的系统确认由用户点击通知打开. 禁止自动弹插件安装对话框, 通知不可用不得静默等待或降级. 外部入口将临时 URI 授权交给前台服务, 来源和安装不会跨进程重启自动恢复. 实际 Shizuku/none 安装, 跨 UID 来源及通知拒绝见 [通知证据](p8-notification-evidence.md).

## 正式宿主 UID 的脚本调用

只在本轮自建 `Three_Setup_Dhizuku_P8_API31` / emulator-5562 上新增宿主, 不替换用户设备的 owner. 采用主机当时已有 Debug 5307 APK, 42,221,755 字节, SHA-256 `1dc8065d22fb5f710a8f222e578f7e9330fa369675eea14ca6afcb7d1099840e`. 宿主另一个会话同期修改并构建 Rhino/依赖, 因此该包按实际摘要记录, 不归因为本轮某个干净宿主提交; 本轮不重新构建或提交其工作区内容.

`tools/p8-release-script.js` 由官方 ShortcutActivity 的直接路径入口运行, 脚本及固定无组件 APK 位于本次新建宿主私有目录. 驱动先证明专用 AVD 身份, owner, 当前用户和夹具缺席; 每个 case 独立 token, 结果存在时禁止重放. 宿主原有路径检查要求全文件访问, 仅在该新装测试宿主执行期间开启 MANAGE_EXTERNAL_STORAGE, 完成后恢复 default. 不使用设备全局安装或扫描开关.

中间 Debug 51 的实际宿主 UID=10152 / PID=13408, V2 协商成功. case `db49505893b24fa2b23afd728aeaf944` 完成 Dhizuku 持久 set/clear, 静默安装 v1, 核验系统实际版本与安装者 com.rosan.dhizuku, 再静默卸载. 实装耗时 2058 ms; 原首选 XML, 固定 APK 摘要保持, 自有来源和脚本逐文件核对后清理. 这是正式宿主 UID -> Rhino -> 生产客户端 -> 插件 Binder -> owner 的完整通路, 与插件 UID 的 Debug 路由测试区分.

最终 R8 Release 57 通过同一正式入口复验. case `70dc213086b947108f89a047d60b686d`, 宿主 UID=10152 / PID=14747, status 返回 pluginVersion=1.1.0 / contractVersion=2; persistent set/clear 均为 true, 真实静默安装 v1 耗时 2725 ms, 实际安装者仍为 com.rosan.dhizuku, 随后成功卸载. 来源摘要与普通默认 XML 保持, 自有三个文件及空目录按内容核对后移除. 日志 `build/p8-release57-host-acceptance.log`, 并非以 Debug 测试代替最终 R8 的 Dhizuku 路径.

初次驱动遇到宿主产物被另一构建替换, 未操作设备; 第二次被宿主存储路径检查拒绝; 第三次 Windows 的普通 adb shell stdin 在 APK 内控制字符处截断至 124 字节, 脚本摘要守卫在安装前拒绝. 改用二进制 exec-in 并在启动前回读全部来源核对后上述完整复验通过. 两份失败 case 的已知文件经摘要/内容核对清理; 不把预检失败写为安装失败或成功. 原日志保存在 `build/p8-release-host/` 和同前缀日志.

## 新 Samsung 的平台边界

维护者重新提供同端口的全新 SM-A566B, API 36. 先断开旧 transport 再连接 localhost:44836, 重新读取设备身份/启动标识/owner/用户/包集合/首选项, 不沿用旧设备数据. 初始没有宿主, 插件或测试 APK. `build/p8-samsung-f8cb29acf84f/before.json` 保存基线.

Debug 52 上三项只读兼容/接口检查在 0.4 秒内 3/3 通过, 无跳过: API34+ Dhizuku 持久 set/clear 均在 owner 查询和任何偏好写入前返回 INVALID_ARGUMENT; 生产服务九个受保护操作拒绝非宿主 UID; 独立进程路由的 V1/V2 envelope/新模式拒绝检查通过.

新设备 POST_NOTIFICATIONS 默认未授权, 保持 granted=false. 使用真实 ExternalInstaller 请求的通知不可用专项在 0.166 秒内 1/1 通过, 返回 NOTIFICATION_UNAVAILABLE, 没有打开来源, 创建系统 session, 写入历史或安装夹具. 这项为实际总通知不可用, 不与另一个设备的隔离 channel 检查混淆. 日志及前后权限状态保存在同一目录.

最终覆盖同一 Release 57 并核验设备 APK 摘要/版本/非 Debug, PAGE_SIZE=16384. 为完成真实跨 UID 的签名权限与接口验证, 在该全新设备安装上述已固定的宿主 5307, 独立 Release 契约 2/2 通过, guardedOperations=6. 首次 runner 类名参数误用路径分隔符导致未启动, 修正后通过, 原日志保留. 测试 APK 原本不存在, 核对本次摘要后已移除; 最终新增包集合严格为宿主与插件, 无原包删除, 原设备身份/owner/全部 preferred 保持, 无夹具或安装前台服务. 最终审计为 `final.json`.

## 关联仓库

| 项目 | 本轮本地提交 / 最终版本 | 验证 |
| --- | --- | --- |
| APK Inspector | 17fe20d / 1.2.2 / build42 | 共享解析迁移, 231 JVM, Debug3/3, 独立Release2/2, lint各0错误/37警告 |
| AutoJs6 | 494e3e9493, bd9817980f, 8c3045d24e, bb49c86ab129 | API/AIDL8项, installer相关91 JVM, 后续codec15项; build5306 App/androidTest/Inrt编译, build5307 AAR及字段验证 |
| Documentation | 2ada5142efb29bfb03760d4323147e15dd053504 / 6.8.0 / code86 | 144模块, 规范化, 生成新鲜度与搜索语法 |
| TypeScript Declarations | d789d42df006b4892ef7b4a0e9e0aa95e0796478 / 4.26.1 | 正向/负向类型检查 |
| Ace Editor | 9f66fd62425635742d83dfe3f09dbe2623ca7961 / 1.18.1 / build120 | LSP生成/运行时验证, 25文档产物 |
| Offline Docs | 4d8115c5c91956faa8651896971029cea30a8197 / 6.8.5 / build67 | 2 JVM及Debug/Release内容门禁, 200文件逐一匹配 |

离线内容 11,726,643 字节, SHA-256 `ca114b049bcdba1c2c9c0710d4e829865be00ce7317bf75f54e176b8e9eae800`, provenance 锁定 Documentation 2ada5142. JSDoc 收尾没有新增脚本签名, 复用此前已经完成的完整声明导出, 手工 installer 声明在 d.ts 与 Ace 同步; 为避免纳入其他会话改动, 不再次触发会构建当前宿主的全局导出. 宿主的其他会话内容与 Ace 原有未跟踪 releases/ 均保留.
