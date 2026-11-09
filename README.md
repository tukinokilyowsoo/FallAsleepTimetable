# FallAsleep课程表

一个 Material You（Material 3）风格的 Android 课程表应用：离线、无账号、无后端，手机与平板自适应。

> 当前状态：**v0.5.8-beta 测试版** —— M0–M3 已完成；新增周切换、预测式返回、全屏放大与 FAB 旋转动效，以及「隐藏状态栏」开关；Niagara 小组件已移除。剩余功能见 [剩余功能执行计划](docs/剩余功能执行计划.md)。

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
| M3 | 提醒、小组件、ICS/CSV、分享图 | ✅ 完成（含小组件配置、下课精准刷新、无障碍） |
| M4 | 多学期、备份、无障碍、打磨 | 计划中 |

详细环境与验证记录见 [docs/M0-记录.md](docs/M0-记录.md)、[docs/M1-记录.md](docs/M1-记录.md)、[docs/M2-记录.md](docs/M2-记录.md) 与 [docs/M3-记录.md](docs/M3-记录.md)。

- [v0.5.8-beta 更新说明](docs/RELEASE-NOTES-v0.5.8-beta.md)
- [v0.5.7-beta 更新说明](docs/RELEASE-NOTES-v0.5.7-beta.md)
- [v0.5.6-beta 更新说明](docs/RELEASE-NOTES-v0.5.6-beta.md)
- [v0.5.5-beta 更新说明](docs/RELEASE-NOTES-v0.5.5-beta.md)
- [v0.5.4-beta 更新说明](docs/RELEASE-NOTES-v0.5.4-beta.md)
- [v0.5.3-beta 更新说明](docs/RELEASE-NOTES-v0.5.3-beta.md)
- [交接文档](handoff.md)
- [第三方依赖声明](THIRD-PARTY-NOTICES.md)
- [隐私政策](PRIVACY.md)
- [v0.5.2-beta 更新说明](docs/RELEASE-NOTES-v0.5.2-beta.md)
- [v0.5.1-beta 更新说明](docs/RELEASE-NOTES-v0.5.1-beta.md)
- [v0.5.0-beta 更新说明](docs/RELEASE-NOTES-v0.5.0-beta.md)
- [v0.4.0-beta 阶段性交付报告](docs/交付报告-v0.4.0-beta.md)
- [剩余功能执行计划](docs/剩余功能执行计划.md)

## 下载 / 测试版

测试版发布在仓库的 **Releases** 页面：`v0.5.8-beta`（`FallAsleepTimetable_arm64_v0.5.8-beta.apk`，arm64-v8a，约 11 MB）。

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
| ![小组件](docs/screenshots/WIDGET.jpg) |

更多：课程详情、今日页、设置、学期设置见 `docs/screenshots/`。

## 许可证

本项目采用 [PolyForm Noncommercial License 1.0.0](LICENSE)：

- 允许个人学习、研究、测试、业余项目等**非商业用途**；
- 允许非商业性的修改与分发，但分发时必须附带许可证文本与 Required Notice；
- **不允许商业使用**；如需商业授权，请通过仓库 Issues 联系作者。

> 这不是 OSI 认可的开源许可证，GitHub 会将其标记为 "View license / Other"。
