******

### 发行历史

******

# v1.0.0

###### 2026/10/01

* `提示` P0 开发预览: 仓库骨架, 可被 AutoJs6 插件中心识别的插件身份, 以及特权安装 spike. Binder 契约, 安装引擎, 对话框, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `three-setup-installer` (engine `installer`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.INSTALLER` 服务骨架
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `优化` P0 已验证 Shizuku 和 Root 静默安装, 更新, 卸载及普通默认安装器设置. 宿主与脚本安装入口尚未开放, 本版本仍不支持持久默认项.
* `优化` 插件 ID, engine, 服务 action / category, Binder descriptor 与最低宿主版本改由宿主 installer-api 契约常量提供; 能力声明加入安装器契约版本 1, 最低宿主构建回填为 5299
* `优化` 可随机访问的来源避免完整缓存副本, 流来源按需暂存. 支持普通 ZIP 分包, AAB 仅供检查, 拒绝内容发生变化的来源.
* `优化` 显式选择的授权方式不回退, 区分拒绝, 超时与不兼容, 并发请求共享授权过程与特权连接.
* `优化` 安装与更新核心支持系统确认, Shizuku 和 Root, 可取消操作并返回实际确认方式与系统处理结果.
* `优化` 卸载核心支持系统确认, Shizuku 和 Root, 特权卸载可选择保留应用数据.
* `优化` 支持串行批量安装, 失败后继续或取消剩余项, 并可通过特权方式校验和选择目标用户.
* `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
* `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
* `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `package-archive-parser.aar` 与 `installer-api.aar` (AutoJs6 模块 `plugin-api/package-archive-parser` 与 `plugin-api/installer-api`, 宿主 P1 构建 6.8.0 / 5299, MPL 2.0), 与 `common-plugin-api.aar` 一同在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 升级共享安装包解析器, 支持普通 ZIP 分包容器
