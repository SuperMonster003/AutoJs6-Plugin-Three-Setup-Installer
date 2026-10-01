# P3.2 文件管理器与浏览器外部入口验收

日期: 2026-10-01. 设备为维护者指定的 Sony XQ-AT72 / QV710AF65F / API 31 / arm64-v8a. 本文区分维护者的完整安装反馈和代理执行的候选/确认页复核, 不把后者计为重复安装成功.

## 维护者手动反馈 (build 33)

| 来源 | 格式 | 已报告结果 |
| --- | --- | --- |
| MT Manager, 系统建议入口 | APKS | 出现插件信息/确认页, 点击安装后返回 Installation successful. 微信 `com.tencent.mm`, versionName `8.0.72`, 启动器图标出现, 应用可打开并使用. |
| 浏览器下载列表 | APK | 候选列表包含插件, 安装后有启动器图标, 应用可正常使用. 维护者未为该 APK 单独提供包名/版本. |
| 浏览器下载列表 | XAPK | 候选列表包含插件并完成安装, 应用可正常使用. 维护者明确澄清原反馈中的 apkx 为笔误, 实际是 `.xapk`. |
| 浏览器下载列表 | APKM | Facebook, 从 APKMirror 下载, 候选列表包含插件并成功安装, 应用可正常使用. |
| Files by Google | APK | 候选列表包含插件, 能正常安装并使用. |
| Files by Google | XAPK / APKM | 候选列表缺少插件, 本轮复现并修复. |

本轮只读查询系统包信息核对到: 微信 `8.0.72` / 3085, Facebook `582.0.0.0.30` / 475404750, Instagram `449.0.0.52.84` / 385511871. 本机待复核 XAPK 为已下载的 Instagram 文件. 浏览器侧成功结果由维护者提供, 代理未自动启动浏览器重新下载或安装这些应用.

## Files by Google 问题与必要适配

Files by Google 当前安装版本为 `1.10256.867145328.0-release` / 1711587. 代理通过其实际 Downloads 列表点击维护者已下载的 Facebook APKM 和 Instagram XAPK, 在 build 33 均复现系统候选列表缺少插件.

系统记录的两条实际 Intent:

```text
ACTION_VIEW content://com.google.android.apps.nbu.files.provider/2/118732
  type=application/octet-stream flags=0x800001
ACTION_VIEW content://com.google.android.apps.nbu.files.provider/2/118733
  type=application/octet-stream flags=0x800001
```

URI 只携带提供方的数字 ID, 没有 `.apkm` / `.xapk` 文件名. 初版从宿主迁入的 ZIP/octet-stream filter 要求 URI 的 pathPattern 匹配扩展名, 因而无法匹配这类来源. 实际显示名只能在读取 ContentProvider 后获得, 不能作为系统候选列表阶段的 path 条件.

build 35 移除该通用 MIME filter 的 host/path 限制, 保留 `content` scheme 和精确的 `application/zip` / `application/octet-stream` MIME. 专用安装包 MIME, file URI 路径, 只读 PFD, 来源大小上限和按真实内容解析均保持. 没有注册 `*/*` 或 HTTP(S) 来源. 插件也可能出现在其它被提供方标为通用 ZIP/二进制的内容候选中; 是否为可安装包仍由既有解析器验证, 文件名和 MIME 不构成安装许可.

## 自动化回归

- JVM 243 项通过, 无失败/跳过; Debug 和 androidTest 构建通过, `build/files-entry/debug-build35.log`, 11 秒.
- 新 `opaqueProviderUrisResolveGenericPackageTypesWithoutClaimingDocumentsOrNetworkUrls` 在 API 35 旧 APK 上先准确失败, `Missing opaque URI handler`, 1 项失败; 原始日志 `build/files-entry/old-build-opaque-uri-api35.log`.
- 修复后 API 24 / API 35 各 4/4 通过, 无跳过, 4.735 / 1.904 秒, `build/files-entry/fixed-api24.log` / `fixed-api35.log`. 两项入口契约与两项来源/确认用例覆盖: 八种专用 MIME 的 VIEW/INSTALL_PACKAGE + content/file, opaque ID 的 ZIP/octet-stream, 已知 PDF/PNG/plain text MIME 与网络来源不匹配, 只读打开, 取消确认不创建平台安装会话. 安装了支持插件契约的宿主时, 同时断言官方宿主不再占用安装入口.
- 签名混淆 Release 和两种 lint 通过, `build/files-entry/release-build35.log`, 44 秒. Debug 0 errors / 22 warnings, Release 0 errors / 23 warnings. 十语言 changelog 与生成文档已同步.

## build 35 的真实 Files 界面复核

更新前关闭代理自己打开的系统候选页, 核对没有活动平台安装 session 或插件安装前台服务, 再覆盖插件. 保留维护者的三个已安装应用和源文件. 部署结果在 `build/files-entry/deployment35.json`.

| 文件 | 实际系统 UI 与插件行为 | 证据 |
| --- | --- | --- |
| Instagram XAPK | 候选列表新增 3-Setup Installer; 选择插件并使用 Just once 后, 显示 `com.instagram.android`, 版本 `449.0.0.52.84` 的安装信息/确认页, 签名匹配已安装应用 | `build/files-entry/xapk-chooser-after.xml`, `xapk-confirmation-after.xml` / `.png` |
| Facebook APKM | Android 将刚使用的插件提升到 `Open with 3-Setup Installer` 标题区域; Just once 后进入 `com.facebook.katana`, 版本 `582.0.0.0.30` 的安装信息/确认页 | `build/files-entry/apkm-chooser-after.xml` / `.png`, `apkm-confirmation-after.xml` / `.png` |

第一次 APKM 复核脚本只查普通列表行, 因系统把最近使用的应用放到标题区域而产生断言失败; 检查实际标题与系统 resolve 结果后确认候选存在. 首次读取 APKM 页面时仍在 Preparing package, 等待解析完成后确认信息页. 两者是观察时机/节点范围的问题, 不计为新的产品安装失败.

两种确认页的 base.apk 均为 checked=true / enabled=false; 已查看实际 XAPK 截图, 灰色方框中的深色勾选清晰可见. build 35 包含 build 34 的禁用勾选标记修复, 因而同时完成此前尚未覆盖到 QV710AF65F 的修复交付.

代理对两次检查均只点击 Cancel 和随后 Done, 未点击 Install 或 Always, 没有重复安装或设置默认安装器. 收尾时活动平台安装 session 为 0, 插件没有安装前台服务; 微信/Facebook/Instagram 版本与复核前一致. 自己发起的两次取消作为正常取消操作进入本地历史, 不清空维护者历史. 收尾记录 `build/files-entry/manual-recheck-result.json`.

以维护者指定的 Files by Google 作为文件管理器, 结合其 APK 成功反馈, 浏览器 APK/XAPK 成功反馈, 本次实际 Files XAPK 打开/确认以及宿主入口排除测试, 原 P3.2 的外部打开验收条目据此勾选. Files by Google APKM 的修复是额外兼容证据. 未声称已覆盖其它 OEM 文件管理器或另行运行 AOSP DocumentsUI, 这些差异继续按 P6 设备矩阵验证.

## 本地交付

- 插件版本: 1.0.0 / build 35, 已覆盖 QV710AF65F.
- APK: `releases/autojs6-plugin-three-setup-installer-v1.0.0-0948c89e.apk`, 1,880,747 字节.
- CRC32: `0948c89e`; SHA-256: `9ed0d3580a86b6544a32961d60aaccf3917e9403ca5b398ca779cf43eef2f100`.
- apksigner v2 校验通过; 本地 releases 目录仅有该 APK. build 34 的旧产物归档于 `build/files-entry/build34-62466b78.apk`.
- 本轮仅修改插件仓库并作本地提交, 未推送或发布远端版本.
