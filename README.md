# FallAsleep 课程表

## Tomo 受够了走在路上看课表结果跳转各种APP的生活！！！

受够了开屏广告！受够了摇一摇跳转！受够了花里胡哨的界面！受够了臭不要脸的会员！！！

于是乎就有了这个完全离线、超轻量、无广告、完全免费、长期维护的 Material You 课程表！

> 当前测试版：**v1.1.0-beta1** · 稳定版：v1.0.0 · Android 8.0+ · arm64-v8a

## Tomo 推荐使用 FallAsleep 课程表！

- **无广告，完全免费**：没有广告位、订阅和账号，也没有自建后端。
- **完全离线**：课表、设置、备份全部保存在本机。
- **Material 3 设计规范**：Android 12+ 跟随壁纸取色，也支持固定主题色与深色模式。
- **手机与平板自适应**：手机底部导航，平板侧栏 + 双栏课表与今日课程。
- **原生 Kotlin**：Jetpack Compose 构建，轻量、干净。

## 功能

### 课表

- 多课表：主课表、实验课、考研课表分开管理，随时切换。
- 周次：当前周自动定位、上一周 / 下一周、点击「本周」回到当前周。
- 课程格：起止周、单双周、连节、颜色、教师、教室、备注。
- 上课时间随课表独立保存：每张课表可设置各自的节数与上下课时间，也可一键同步到全部课表。
- 今日页：当天课程时间轴，已结束课程灰显。
- 全屏课表：隐藏系统栏与界面元素，课表铺满屏幕。
- 课程详情弹层、空状态引导。

### 导入

- **教务导入**：内置 WebView 手动登录，抓取当前课表页后在本地解析。
  - 支持新版正方（jwglxt）、老正方、强智三套模板，自动识别。
  - 可选择覆盖当前课表，沿用该表已有的上课时间配置。
- **文件导入**：JSON 备份、ICS 日历、CSV 表格、旧版 `.wakeup_schedule`。
- JSON 备份包含学期设置时，可选择一并恢复开学日期、周数和上课时间。

### 分享与导出

主页右上角「分享与导出」集中提供：

- 分享课表长图：本周或整学期，支持主题、显示教师 / 教室开关。
- 导出 JSON 备份、导出 CSV 表格。
- 导入课表文件。

设置 → 数据：

- 导出日历（.ics）。
- 导入到日历：写入系统日历，由系统负责提前提醒。

### 桌面小组件

- 今日课程：按组件宽度显示今天 / 今天+明天 / 三天。
- 周课表：固定当前周，重叠课程分栏，今天列加粗。
- 每个组件可单独选择课表、显示教师 / 教室、显示后续日期。
- 下课后 1 分钟内刷新（需授予「闹钟和提醒」权限；未授权时按系统周期刷新）。
- Android 12+ 莫奈取色。

### 动效与系统

- 平板周切换：课表与今日课程分别滑动淡入淡出。
- 预测式返回：今日 / 设置滑开时露出下层课表。
- 全屏放大动画、FAB 加号旋转成叉、切周轻触感。


### 其他

- 检查更新：默认 GitHub 源，提供国内镜像（gh-proxy 加速 API 与 APK 下载）；有可用版本时在主页弹出提醒，可拒收 / 恢复更新提醒。
- 深色模式、自定义主题色、字体缩放、TalkBack 课程描述。

## 下载与安装

正式版发布于 [Releases](https://github.com/tukinokilyowsoo/FallAsleepTimetable/releases/latest)：

- 文件：`FallAsleepTimetable_arm64_v1.0.0.apk`
- 架构：arm64-v8a（绝大多数近年的手机与平板）
- 大小：约 11 MB
- SHA-256：`09a0c4a300bb8fa45166ba6a4266ec536f647dc7c97e3fdcf062164ad6b566bd`

测试版在 Releases 中标记为 Pre-release：

- 版本：`v1.1.0-beta1`
- 文件：`FallAsleepTimetable_arm64_v1.1.0-beta1.apk`
- SHA-256：`6c59d914304b7f7714ec9e6ab442cb24d7f511037484d3e6862a2a3d0ec5e417`

说明：

- 使用独立 release 签名。此前安装过 **debug** 版的用户需要先导出 JSON 再卸载重装；安装过 release 版旧测试版的用户可直接覆盖升级。
- 包名：`io.github.tukinokilyowsoo.fallasleeptimetable`。从 beta2 起更换了包名，可与更早版本并存；数据不互通，需要导出 JSON 再导入。

## 系统要求

- Android 8.0（API 26）及以上
- arm64-v8a
- 无网络也可使用课表与小组件；教务导入需要网络

## 已知问题

- 教务页面改版可能导致解析失效；可通过「导出日志」反馈页面 HTML 片段。
- 解析模板目前覆盖正方（新 / 旧）与强智，其他教务系统暂不支持。
- 小组件的下课精准刷新依赖「闹钟和提醒」权限；未授权时最长约 30 分钟刷新一次。
- 预测式返回的完整手势动画需要 Android 13+，旧系统功能一致但没有预览动画。

## 隐私

- 无账号、无后端、无统计 SDK。
- 教务登录只在应用内 WebView 会话中进行，不保存学号密码。
- 详细说明见 [PRIVACY.md](PRIVACY.md)。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- `material3-adaptive-navigation-suite`（NavigationBar / Rail / Drawer 自适应）
- Room + DataStore 本地存储
- Glance 桌面小组件
- jsoup 解析教务页面
- minSdk 26，targetSdk / compileSdk 36
- 动态取色（Android 12+）与内置浅色 / 深色配色回退

## 项目结构

```
app/src/main/java/io/github/fallasleeptimetable/
├── MainActivity.kt
├── data/            # Room、导入解析、日历、分享、更新检查
├── domain/          # 课表模型与周次/布局逻辑
├── ui/              # Compose 导航、课表 / 今日 / 设置、主题
└── widget/          # Glance 桌面小组件
```

## 开发与文档

- [M0 记录](docs/M0-记录.md) / [M1 记录](docs/M1-记录.md) / [M2 记录](docs/M2-记录.md) / [M3 记录](docs/M3-记录.md)
- [v1.0.0 正式版说明](docs/RELEASE-NOTES-v1.0.0.md)
- [第三方依赖声明](THIRD-PARTY-NOTICES.md)
- [隐私政策](PRIVACY.md)

## 许可证

本项目采用 [PolyForm Noncommercial License 1.0.0](LICENSE)：

- 允许个人学习、研究、测试、业余项目等**非商业用途**；
- 允许非商业性的修改与分发，但分发时必须附带许可证文本与 Required Notice；

> 这不是 OSI 认可的开源许可证

## 反馈

- 问题与建议：[GitHub Issues](https://github.com/tukinokilyowsoo/FallAsleepTimetable/issues)
- 也可以在小红书留言。

---

© 2026 槻ノ木 響子 · Lyowsoo
