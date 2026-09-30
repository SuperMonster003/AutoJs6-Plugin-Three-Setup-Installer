******

### 发行历史

******

# v1.0.0

###### 2026/09/30

* `提示` P0 开发预览: 仓库骨架, 可被 AutoJs6 插件中心识别的插件身份, 以及特权安装 spike. Binder 契约, 安装引擎, 对话框, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `three-setup-installer` (engine `installer`), 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.INSTALLER` 服务骨架
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `依赖` 附加 Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) 用于 Shizuku 授权方式
* `依赖` 附加 libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) 用于 Root 授权方式
* `依赖` 附加 AndroidHiddenApiBypass 6.1 用于特权服务访问隐藏的包安装器 API
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5298, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
