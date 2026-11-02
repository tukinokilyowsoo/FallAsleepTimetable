# FallAsleep 课程表 · 交接文档

> 更新日期：2026-11-02
> 当前版本：`0.5.3-beta`（versionCode 9）
> 仓库：https://github.com/tukinokilyowsoo/FallAsleepTimetable

---

## 1. 项目一句话

离线、无账号、无后端的 Material You 课程表 Android App：本地课表 + 教务/文件导入 + 系统日历 + 三套桌面小组件（含 Niagara 透明组件）。

- 包名：`io.github.tukinokilyowsoo.fallasleeptimetable`
- 源码 namespace：`io.github.fallasleeptimetable`（与包名不同，历史原因，勿随意改）
- minSdk 26（Android 8），compileSdk / targetSdk 36
- 技术栈：Kotlin + Jetpack Compose + Material 3、Room、Glance、jsoup
- 项目性质：个人项目；仓库暂无 LICENSE（保留所有权利）

---

## 2. 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| M0 | 工程骨架、M3 主题、自适应导航 | ✅ |
| M1 | Room 数据、课表网格、课程编辑、多课表、学期设置、JSON 导入导出 | ✅ |
| M2 | 教务导入：正方 / 新正方 / 强智 + WebView 手动登录 + 日志 | ✅ |
| M3 | 上课时间、系统日历、桌面小组件、莫奈取色、ICS/CSV、分享长图、搜索、小组件配置/精准刷新、无障碍 | ✅ |
| M4 | 多学期、自动备份、时间模板、迁移、性能、R8、测试、上架材料 | ⏳ 未开始 |

详细验证清单见：
- `docs/M3-记录.md`
- `docs/剩余功能执行计划.md`
- `docs/RELEASE-流程.md`

---

## 3. 已实现功能

### 课表

- 多课表：新建、切换、重命名、删除
- 课程增删改：课程名、教师、教室、颜色、单双周、起止周、连节
- 周次切换、当前周定位、非本周淡化、重叠课程分栏、今日列高亮
- 今日页：当天课程、已结束样式
- 全屏查看
- 平板/横屏双栏：课表 + 今日课程

### 学期与时间

- 开学日期、总周数、每天节数、周六/周日显示
- 两步式上课时间向导：上午/下午节数、单节时长、课间、每节起止时间
- Material 3 时钟盘，自适应横竖屏

### 导入 / 导出

- 教务导入：WebView 手动登录 → 抓当前页 → 本地解析 → 预览/导入
  - 支持老正方、新正方（jwglxt）、强智三套模板；自动识别；支持 iframe
  - 失败可导出日志（含 HTML 快照）
- FallAsleep JSON 备份（含旧版 `.wakeup_schedule` 兼容）
- ICS 导出/导入（RRULE、单双周、`X-FALLASLEEP-*` 精确往返）
- CSV 导出/导入（UTF-8 BOM、列名别名、范围写法）
- 导入时按内容自动识别格式

### 系统集成与分享

- 写入系统本地日历「FallAsleep课程表」，提前 10 分钟提醒
- 删除当前课表日程 / 全部已导入日程
- 课表分享长图：当前周/整学期、主题、显示教师/教室、PNG + 系统分享

### 搜索

- 课程页搜索图标
- 跨课表按课程名/教师/教室搜索
- 点击结果切课表并跳到当前周或第一个有效周

### 桌面小组件（Glance）

1. **今日课程**：按宽度显示今天 / 今天+明天 / 今天+后天；已结束课程移除；全部结束显示「你已免费，辛苦了喵😋」
2. **周课表**：固定当前周，跟随周六/日设置，今天列加粗
3. **今日课程（透明）**：面向 Niagara Launcher 等自由缩放启动器
   - 仅显示当天剩余课程
   - 极简待办单行：课程名大号 + 第 n 节 / 时间 / 教室小号右对齐
   - 自动字号上限 72sp，按宽度与行高取较小值
   - 行数按设置分槽；右侧 40dp 避开 Niagara 字母索引条
   - 壁纸亮度自动黑白 + 手动黑/白
   - 设置入口合并为「设置 → 小组件 → Niagara 小组件」

小组件公共能力：
- 按实例配置课表、显示教师/教室、今日组件显示后两天
- 下课精确刷新（AlarmManager；无权限时降级 + 30 分钟兜底）
- 跨天、改时间/时区、开机、壁纸变化、精确闹钟权限变化后刷新
- 刷新请求合并（120ms 防抖）+ 单协程串行
- 没有小组件时取消闹钟；只有周课表时只排零点

---

## 4. 数据模型（Room，5 张表）

`app/src/main/java/io/github/fallasleeptimetable/data/Entities.kt`

- `TermEntity`：学期（开学日期、总周数、每天节数、周六/日）
- `TimetableEntity`：课表（属于学期）
- `CourseEntity`：课程基本信息（名称、教师、颜色）
- `CourseSessionEntity`：一次上课安排（星期、开始节、连节、起止周、单双周、教室）
- `TimeSlotEntity`：节次时间（按学期）

外键级联：Term → Timetable → Course → CourseSession；Term → TimeSlot。

关键仓库：`data/ScheduleRepository.kt`（事务写入、导入、搜索、时间表替换）。

---

## 5. 代码结构（单模块，按 feature 分包）

```
app/src/main/java/io/github/fallasleeptimetable/
├── MainActivity.kt
├── data/
│   ├── AppContainer.kt / AppDatabase.kt / Daos.kt / Entities.kt
│   ├── ScheduleRepository.kt
│   ├── calendar/CalendarScheduleExporter.kt
│   ├── diagnostics/ImportLog.kt
│   ├── importer/            # 正方/新正方/强智、JSON、ICS、CSV、格式识别
│   └── share/               # 分享长图、分享启动、色板
├── domain/ScheduleModels.kt # 周次、可见课程、重叠分栏
├── ui/
│   ├── FallAsleepApp.kt / ScheduleViewModel.kt / Format.kt / ScheduleFab.kt
│   ├── screens/             # 课表、今日、设置、课程编辑、导入、分享、Niagara 设置
│   ├── theme/               # M3 主题、种子色生成
│   └── widget/              # 小组件配置 Activity
└── widget/                  # Glance 小组件、刷新、配置、壁纸取色
```

### 关键实现位置

- 教务解析：`data/importer/EduScheduleParser.kt`
- ICS/CSV：`data/importer/IcsTimetable.kt`、`CsvTimetable.kt`、`TimetableFileImporter.kt`
- 分享长图：`data/share/ScheduleImageExporter.kt`
- 系统日历：`data/calendar/CalendarScheduleExporter.kt`
- 刷新调度：`widget/WidgetUpdater.kt`、`WidgetRefreshScheduler.kt`、`WidgetRefreshReceiver.kt`
- Niagara 组件：`widget/NiagaraTodayWidget.kt`、`WallpaperTextColors.kt`
- 小组件配置：`widget/WidgetConfig.kt`、`ui/widget/WidgetConfigActivity.kt`

---

## 6. 构建与发布

### 环境

- JDK 17+（当前 Java 21）
- Android SDK：`~/.android-sdk` 或 `local.properties` 的 `sdk.dir`（本机为 `/data/user/0/cc.c0droid/files/home/android-sdk`）
- Gradle：`~/gradle-8.14.3/bin/gradle`
- `gradle.properties` 里的 `android.aapt2FromMavenOverride` 指向本机 aapt2（按需保留）

### 常用命令

```sh
~/gradle-8.14.3/bin/gradle testDebugUnitTest
~/gradle-8.14.3/bin/gradle assembleDebug
~/gradle-8.14.3/bin/gradle assembleRelease
```

产物：
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`（已签名）

### 签名

- Keystore：`/data/user/0/cc.c0droid/files/home/.fallasleep/fallasleep-release.jks`
- 备份：`/sdcard/cccodroid/FallAsleep-release-keystore/`
- Gradle 从 `~/.gradle/gradle.properties` 读取：
  - `FALLASLEEP_STORE_FILE`
  - `FALLASLEEP_STORE_PASSWORD`
  - `FALLASLEEP_KEY_ALIAS`
  - `FALLASLEEP_KEY_PASSWORD`

**注意**：
- 密钥和密码不要提交进仓库。
- debug 与 release 签名不同，不能互相覆盖安装；切换前先导出 JSON 备份。
- 丢失 keystore 后已安装用户无法升级，只能卸载重装。

### 发布

1. 修改 `app/build.gradle.kts` 的 `versionCode` / `versionName`
2. 更新 `docs/RELEASE-NOTES-vX.Y.Z.md`，填入 SHA-256
3. 构建并校验：
   ```sh
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   sha256sum app/build/outputs/apk/release/app-release.apk
   ```
4. 提交并推送
5. 创建 GitHub pre-release：
   ```sh
   gh release create vX.Y.Z \
     /sdcard/cccodroid/FallAsleepTimetable_arm64_vX.Y.Z.apk \
     --title "FallAsleep 课程表 vX.Y.Z" \
     --notes-file docs/RELEASE-NOTES-vX.Y.Z.md \
     --prerelease
   ```

---

## 7. 测试

- JUnit 18 项：
  - `IcsTimetableTest`（5）：导出转义/折行、往返、第三方 TZID、逐周事件合并、跳过全天/学期外
  - `CsvTimetableTest`（5）：BOM/引号转义、往返、第三方列名与范围、坏行跳过、缺列报错
  - `EduScheduleParserTest`（4）：三套教务模板 + 旧格式
  - `WidgetRefreshScheduleTest`（4）：下课/零点刷新时刻计算
- 解析器测试位于 `app/src/test/java/.../data/importer/`

未覆盖：Room DAO、Compose UI、截图测试、真机自动化。

---

## 8. 已知问题与限制

1. 教务页面改版会导致解析失效，需要日志/HTML 样本维护。
2. Android 14+ 的「闹钟和提醒」权限默认不授予；未授权时小组件刷新依赖系统调度，最长 30 分钟兜底。
3. Niagara 组件非常窄时右侧说明会被压缩/隐藏；这是可读性取舍。
4. 自动黑白取色基于整张壁纸亮度，壁纸明暗反差大时可能与组件所在区域不一致，可手动指定。
5. 小组件内容不可滚动；课程多时需要拉高。
6. 尚未实现 App 内上课提醒（目前走系统日历提醒）。
7. 尚未实现多学期、自动备份、时间模板、英文界面。
8. 自定义主题色为 HSV 近似 M3，不是完整 HCT 色调板。
9. 仓库暂无 LICENSE；上架前需补许可证、隐私政策与商店材料。
10. `namespace` 与 `applicationId` 不同，改包名/签名需同时评估数据迁移。

---

## 9. 下一步建议（对应剩余功能计划）

1. **多学期**：Term 增加归档标记；所有查询按当前 Term 过滤；统一切换状态。
2. **自动备份**：定时导出 JSON 到 SAF 目录 + 恢复入口。
3. **节次时间模板**：多套时间表（夏令时/不同教学楼），切换后网格更新。
4. **数据迁移与校验**：Room migration、导入去重、异常数据修复。
5. **性能与包体**：冷启动、列表重组、R8、baseline profile。
6. **测试覆盖**：解析器 fixtures、DAO、Compose 截图测试。
7. **可选提醒**：App 内通知、免打扰时段。
8. **无障碍/多语言/上架材料**：根据发布目标排期。

---

## 10. 交接注意事项

- 工作区：`/data/user/0/cc.c0droid/files/home/workspace/FallAsleepTimetable`
- 共享 APK 目录：`/sdcard/cccodroid/`
- 本机安装走共享目录中转（`priv` 读不到应用私有目录）：
  ```sh
  cp app/build/outputs/apk/debug/app-debug.apk /sdcard/cccodroid/FallAsleep-debug.apk
  priv 'cp /sdcard/cccodroid/FallAsleep-debug.apk /data/local/tmp/fa.apk && pm install -r /data/local/tmp/fa.apk'
  ```
- 真机验证优先用 debug 包；发布前再出 release。
- 小组件验证要点：Niagara 横竖屏、自由缩放、右侧字母条、壁纸明暗、下课刷新。
- 任何导入格式改动必须补对应 JUnit 样例。
- 提交信息不要带 Claude/生成工具署名（用户要求）。
