3-Setup Installer 用于安装, 更新, 检查与卸载 Android 应用. 可以从它的首页, AutoJs6 的安装操作与脚本, 或其他应用打开和分享的安装包发起安装; 默认由 Android 系统确认, 经 Shizuku, Root 或 Dhizuku 授权后也可以静默安装. 不连接 AutoJs6 也能单独使用.

### 开始使用

1. 从启动器打开 3-Setup Installer. 需要静默安装时, 在首页 "授权方式" 中点按 Shizuku, Root 或 Dhizuku 对应的 "授权".
2. 点按 "选择安装包", 选择一个或多个 APK, APKS, XAPK 等安装包.
3. 在安装对话框中核对版本, 签名与权限, 然后点按 "安装".
4. 在 AutoJs6 "插件中心" 启用本插件后, AutoJs6 中的安装操作同样显示此对话框; 在其他应用中打开或分享安装包时也可以选择 3-Setup Installer.

### 主要功能

- **安装对话框**: 列出版本变化, 大小, SDK 与签名比对, 标明新安装, 更新, 重新安装或降级; APK 分包, 权限与 "更多选项" 可以展开调整.
- **授权方式**: 未授权时由 Android 逐次确认; 经 Shizuku, Root 或 Dhizuku 授权后可以静默安装, 允许降级或授予权限等. 设置中的 "授权方式顺序与启用" 决定自动选择的顺序.
- **批量与记录**: 一次可以安装多个应用, 首页显示进行中的任务与 "最近安装"; 记录只保存应用信息与结果, 不保存安装包.
- **已安装应用**: 从首页的 "更多" 菜单进入, 可以搜索, 排序, 查看应用信息或卸载应用.
- **默认安装器**: 把本插件设为打开 APK 文件的默认应用; 能否设置取决于授权方式与系统限制.
- **安装配置文件与黑名单**: 在设置中按安装来源与包名前缀预设安装选项, 或用 "包名黑名单" 与 "SharedUID 黑名单" 阻止指定应用的安装.
- **签名检查**: 新安装包的签名与已安装应用不一致时默认阻止, 只能在安装对话框中对当前项目放行一次.
- **设置**: 调整 "默认交互方式", 外观, 启动器图标与通知, 也可以查看 "发行历史" 或手动 "检查更新".

### 在脚本中使用

AutoJs6 脚本通过 `installer` 模块安装, 检查与卸载应用. 下例先显示安装对话框, 由用户确认后安装; 同步方法需要在脚本线程中调用, 不能在 UI 线程中调用:

```js
console.log(installer.status);
try {
    let result = installer.install('./app.apk', { interaction: 'dialog' });
    console.log(result.packageName, result.versionName);
} catch (e) {
    console.error(e.code, e.message);
}
```

- 不写 `interaction` 时为 `auto`: 已授权特权方式时静默安装, 否则由 Android 请求确认.
- 传入数组为批量安装, 返回与来源顺序对应的结果, 请逐项检查 `ok` 与 `error`; `installAsync` 返回 Promise, 可以在 UI 线程中使用.
- `installer.inspect(source)` 只读取安装包的包名, 版本等信息, 不安装.
- 完整的选项与事件见 [AutoJs6 文档 > installer](https://docs.autojs6.com/#/installer).

### 运行要求

- Android 7.0 或更高版本. 与 AutoJs6 配合使用需要 AutoJs6 6.8.0 或更高版本, 单独使用不需要 AutoJs6.
- Shizuku, Root 与 Dhizuku 均为可选, 只在静默安装或使用特权选项时需要; Dhizuku 需要 Android 8.0 或更高版本.
- 安装与卸载权限用于请求 Android 确认, 查询应用列表的权限用于已安装应用与版本, 签名比对; 通知权限用于通知栏安装与进度通知.
- 仅在手动检查插件更新时联网, 安装包不会上传.

### 常见问题

**已授权仍需确认安装的原因**

Android 或厂商系统的策略仍可能要求用户确认. 这时选择 "自动" 会改由 Android 确认, 选择静默方式则以失败结束并说明原因.

**安装后无法开启无障碍服务的原因**

Android 13 起会限制以本地文件方式安装的应用使用无障碍等受限设置; 经 Shizuku 或 Root 安装时默认不标记为本地文件. 对信任的应用, 在系统设置的应用信息页右上角菜单中允许受限制的设置, 再开启无障碍服务; 入口因系统而异.

**无法安装 .aab 文件的原因**

Android App Bundle 是发布格式, 不能直接安装. 请先用 bundletool 转换为 .apks 安装包; 插件可以显示 .aab 文件的包名与模块信息.

**允许降级后仍降级失败的原因**

"允许降级" 只是向系统提出请求, 是否接受由系统版本, 授权方式以及应用是否可调试决定. 失败时结果中会附上系统返回的信息.

授权方式, 安装选项与权限的详细说明见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer).
