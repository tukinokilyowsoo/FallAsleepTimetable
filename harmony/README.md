# FallAsleep 课程表 · HarmonyOS 原生版

这是 `harmonyos` 分支上的鸿蒙原生工程骨架，目标是覆盖 HarmonyOS 5 / 6 / 7（NEXT）设备。
与 Android 版共用产品设计，但**不共享 Kotlin/Compose 代码**：鸿蒙只运行 HAP，需用 ArkTS / ArkUI 重写。

## 当前状态

- 已建立 DevEco Stage 模型工程骨架：`AppScope`、`entry`、`EntryAbility`、`pages/Index`。
- `Index` 目前是迁移占位页，还没有课表、导入、设置等功能。
- 当前环境没有 DevEco Studio / HarmonyOS SDK，**无法在本机编译 HAP**；需要在安装了 DevEco Studio 的电脑上打开 `harmony/`。

## 运行方式

1. 安装 DevEco Studio（HarmonyOS 5.0 或更新版本）与对应 SDK。
2. `File → Open` 选择本目录 `harmony/`。
3. 在 `build-profile.json5` 中把 `compatibleSdkVersion` 调整为你的 DevEco 版本（当前写的是 `5.0.0(12)`）。
4. 在 `Project Structure → Signing Configs` 配置调试/发布签名（`.p12` + `.cer` + `.p7b`）。
5. 运行 `entry` 到真机或模拟器。

## 包名与上架

- bundleName：`io.github.tukinokilyowsoo.fallasleeptimetable.harmony`
- 需要在 AppGallery Connect 新建「HarmonyOS 应用」，不能复用 Android 应用的包名。
- 版本号从 `1.0.0` 开始；上架材料与 Android 版相同（隐私政策、备案、截图等）。
- AppGallery 的鸿蒙应用使用 HAP 包，签名与 Android 的 keystore 无关。

## 迁移映射

| Android 实现 | HarmonyOS 对应 |
|---|---|
| Room + SQLite | `@kit.ArkData` RelationalStore |
| SharedPreferences / DataStore | `@kit.ArkData` preferences |
| Jetpack Compose + Material 3 | ArkUI 声明式组件 + 自定义主题 |
| Navigation / 底部导航 | `Navigation` + `Tabs` |
| WebView 教务导入 | `Web` 组件 + `runJavaScript` 抓取页面 |
| Glance 桌面小组件 | Form（静态卡片） |
| AlarmManager 精确刷新 | `reminderAgentManager` / 后台任务 |
| 日历写入 | `@kit.CalendarKit` |
| 分享长图 / 文件导出 | `@kit.ShareKit` / `photoAccessHelper` |
| ICS / CSV / JSON 解析 | ArkTS 手写解析（可复用格式定义） |
| GitHub 更新检查 | **不要移植**：鸿蒙版只通过 AppGallery 更新 |

## 建议迁移顺序

1. 数据模型（学期、课表、课程、时间槽）+ 本地存储 + 单元测试。
2. 周课表网格与课程详情，先只读。
3. 课程增删改、多课表、上课时间设置。
4. 文件导入（JSON / ICS / CSV）与导出。
5. 教务 Web 导入。
6. 设置、深色模式、主题色。
7. 今日页、桌面卡片、日历、分享。
8. AppGallery 上架。

每一步都应保持可运行，不要一次性重写全部页面。
