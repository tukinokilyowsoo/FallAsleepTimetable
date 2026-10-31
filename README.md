# FallAsleep课程表

一个 Material You（Material 3）风格的 Android 课程表应用：离线、无账号、无后端，手机与平板自适应。

> 当前状态：**v0.4.0-beta 测试版** —— M0–M2 已验证；M3 已完成修改上课时间、系统日历、桌面小组件与莫奈取色。剩余功能见 [剩余功能执行计划](docs/剩余功能执行计划.md)。

## 技术栈

- Kotlin 2.3 + Jetpack Compose + Material 3
- `material3-adaptive-navigation-suite`（NavigationBar / Rail / Drawer 自适应）
- minSdk 26，targetSdk / compileSdk 36
- 动态取色（Android 12+）与内置浅色/深色配色回退

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| M0 | 工程骨架、M3 主题、自适应导航、真机截图 | ✅ |
| M1 | Room 数据模型、课表网格、课程增删改、多课表、学期设置、导入导出 | ✅ |
| M2 | 教务导入（正方 / 新正方 / 强智 + WebView、重命名、日志） | ✅ 已完成并验证 |
| M3 | 提醒、小组件、ICS/CSV、分享图 | 进行中（已完成：修改上课时间、系统日历、桌面小组件） |
| M4 | 多学期、备份、无障碍、打磨 | 计划中 |

详细环境与验证记录见 [docs/M0-记录.md](docs/M0-记录.md)、[docs/M1-记录.md](docs/M1-记录.md)、[docs/M2-记录.md](docs/M2-记录.md) 与 [docs/M3-记录.md](docs/M3-记录.md)。

- [v0.4.0-beta 阶段性交付报告](docs/交付报告-v0.4.0-beta.md)
- [剩余功能执行计划](docs/剩余功能执行计划.md)

## 下载 / 测试版

测试版发布在仓库的 **Releases** 页面：`v0.4.0-beta`（`FallAsleepTimetable_arm64_v0.4.0-beta.apk`，arm64-v8a，约 10 MB）。

> 包名：`io.github.tukinokilyowsoo.fallasleeptimetable`。从 beta2 起更换了包名，可与更早版本并存；数据不互通，需要导出 JSON 再导入。

发布流程与签名备份说明见 [docs/RELEASE-流程.md](docs/RELEASE-流程.md)。

## 目录结构

```
app/src/main/java/io/github/fallasleeptimetable/
├── MainActivity.kt
└── ui/
    ├── FallAsleepApp.kt        # 自适应导航壳
    ├── screens/                # 课表 / 今日 / 设置
    └── theme/                  # M3 主题与配色
```

## 截图

| 平板（expanded） | 手机（compact） |
|---|---|
| ![主页](docs/screenshots/HOME.jpg) | ![](docs/screenshots/schedule-phone.png) |
| ![小组件](docs/screenshots/WIDGET.jpg) |---|

更多：课程详情、今日页、设置、学期设置见 `docs/screenshots/`。

## 说明

- 仓库目前**没有 LICENSE**，意为保留所有权利；确定许可证后会补充。
