# P6.3 兼容矩阵进展

日期: 2026-10-01. 本文保留原路线图的设备范围. 进程死亡与 Release 性能的补充设备不替代原 OEM 矩阵; 没有因为一项元数据往返成功而认定该设备的全部安装/更新/卸载/外部入口/默认项都已验收.

## API 37 / 16 KiB 页

维护者手动启动 `AVD_API_37.1_16K`, 本轮映射为 `emulator-5558`. 只读查询确认:

| 项目 | 实际值 |
| --- | --- |
| model | `sdk_gphone16k_x86_64` |
| API | 37 |
| ABI | `x86_64` |
| `getconf PAGE_SIZE` | 16,384 |
| 安装前插件 | 不存在 |
| 安装的插件 | 1.0.0 / build 40, 签名非 Debug Release |
| APK | 1,890,319 字节, CRC32 `96bfec75` |
| SHA-256 | `50b06cec175bd0a715179a4e09707ebf205154552d3370af48a8f37375ca976d` |

安装成功后, 独立 Release runner 的两项检查均通过: APK/导出组件/签名/无原生库, 不同 UID/PID 的 INFO/INSTALLER 元数据和 Parcelable 往返以及五项非宿主拒绝. 原始日志为 `build/p6-release40-contract-emulator-5558.log`, `releaseChecks=2`, `releaseFailures=0`, 无跳过. 该 AVD 已有宿主 build 5298, 本轮没有更新它或把元数据检查当作满足最低宿主 5299 的正向安装验证.

通过启用的 `AdaptiveAutoIconAlias` 实际打开 HomeActivity, `am start -W` 为 Status=ok / LaunchState=COLD / TotalTime=594 ms. 截图 `build/p6-api37-home.png` 和层级 `build/p6-api37-home.xml` 已复核, 首页显示授权状态, 默认安装器状态, 空任务/历史与添加按钮. 没有崩溃或替换为 Debug 页面. 插件无原生库, 自身 ELF 对齐不适用; 设备真实运行页大小为 16 KiB, 并非仅查看 APK meta-data.

原 P6.3 对 API 37 特别限定为插件本身安装和运行, 本行已满足这一范围. 不从此推导该 AVD 的 Root/Shizuku, 安装选项或所有外部入口已测试. 模拟器保持运行, 没有修改 GPU/系统配置或停止维护者的 AVD.

## 原设备矩阵的剩余范围

| 原设备范围 | 当前证据入口 | 本轮边界 |
| --- | --- | --- |
| API 24 AVD / none + Shizuku ADB | `p2-core-evidence.md`, `p5-default-installer-evidence.md`, `p6-process-death-evidence.md`, `p6-release-performance-evidence.md` | 本轮补充真实 Shizuku server 停止, Release 往返与 none 100 MiB; 完整统一矩阵仍待核对 |
| Sony G8441 / API 28 | P0/P2 的 Root 安装与 Binder 证据 | 本轮未改该设备的插件或默认项 |
| Sony XQ-AT72 / API 31 | `p3-external-entry-evidence.md` 的维护者 APK/XAPK/APKS/APKM 和 Files 反馈 | 本轮交付新的 Release, 不把另一台 API 31 AVD 的往返代替本机全部流程 |
| Redmi 22120RN86C / API 33 | 原路线图设备池 | none 新装/更新及原矩阵仍待完成 |
| Xiaomi 23046RP50C / API 35 | P2/P3/P5 证据及本轮 P6 两份专项记录 | 本轮补充官方宿主死亡, Shizuku 进程/Release/性能; OEM 安装者字段完整事实表仍待收口 |
| API 37 / 16 KiB AVD | 本文上一节 | 已完成原文限定的插件安装与运行 |

本轮额外提供 Sony XQ-DQ72 / API 33 / KernelSU Root 的进程死亡和 Release 性能证据. 它不是原 Redmi API 33 的替代验收. 原 P6.3 两个 checkbox 仍保留未完成, 后续按原设备和能力逐项汇总, 不增加或拆分路线图条目. 尚未取得的 ColorOS 等 OEM 行为不写成已经实测的结论.
