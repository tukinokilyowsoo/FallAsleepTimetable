# FallAsleep 课程表 · UI 设计术语表

> 版本 v1 · 2026-10-29
> 用途：描述界面细节时有一致的词，避免“那个东西”“调一下”这类模糊表达。
> 对照：Material 3（Material You）+ Jetpack Compose + 本项目的实际部件。

---

## 0. 怎么用

一句话模板：

> **「哪个页面 / 哪个部件」+「改什么属性」+「改成什么值」+「在什么状态下」**

例子：

- 「课表页的**课程块**，圆角从 10dp 改成 12dp，深色模式下也保持。」
- 「平板**侧栏**宽度从 80dp 改成 96dp，选中指示器用 `secondaryContainer`。」
- 「手机**底部导航栏**在横屏时换成**侧栏**。」
- 「**添加课程**从全屏对话框改成 medium 窗口的 **side sheet**。」

---

## 1. 设计系统与主题

| 中文 | English | 说明 |
|---|---|---|
| 设计系统 | design system | 颜色、字体、形状、组件、规范的总和 |
| 设计令牌 | design token | 可复用的变量，如 `md.sys.color.primary` |
| 主题 | theme | 一套配色 + 字体 + 形状的组合 |
| 浅色主题 / 深色主题 | light / dark theme | 跟随系统或手动切换 |
| 动态取色 | dynamic color | 从壁纸提取种子色生成整套配色（Android 12+） |
| 种子色 | seed color | 生成色调板的起点颜色 |
| 色调板 | tonal palette | 由种子色生成的一串明度阶梯 |
| 强调色 | accent color | 非 M3 正式术语，M3 用 primary/secondary/tertiary |
| 高对比度 | high contrast | 无障碍增强配色 |
| 设计规范 | spec / guideline | 组件该长什么样、怎么用 |

---

## 2. 颜色（Material 3 语义角色）

M3 的颜色不是“红/蓝”，而是**角色**。写需求时直接说角色名最准确。

| 中文 | English | 用在哪 |
|---|---|---|
| 主色 | `primary` | 主要按钮、选中态、强调 |
| 主色上的内容 | `onPrimary` | 主色背景上的文字/图标 |
| 主色容器 | `primaryContainer` | 次级强调块、选中背景 |
| 容器上的内容 | `onPrimaryContainer` | 容器色上的文字 |
| 次色 / 第三色 | `secondary` / `tertiary` | 辅助强调、区分色 |
| 背景 | `background` | 页面最底层 |
| 表面 | `surface` | 卡片、栏、面板的底色 |
| 表面容器（五级） | `surfaceContainerLowest/Low/Container/High/Highest` | 表达层级，代替阴影 |
| 表面上的内容 | `onSurface` | 正文文字 |
| 变体表面 | `surfaceVariant` / `onSurfaceVariant` | 次要文字、分隔区域 |
| 描边 | `outline` / `outlineVariant` | 边框、分隔线 |
| 错误 | `error` / `onError` / `errorContainer` | 删除、失败提示 |
| 反色表面 | `inverseSurface` / `inverseOnSurface` | Snackbar 等反色元素 |
| 遮罩 | `scrim` | 弹窗背后的压暗层 |
| 表面色调 | `surfaceTint` | 抬升时叠加的色调 |
| 状态层 | state layer | 悬停/按下时叠加的透明色 |
| 对比度 | contrast ratio | 正文至少 4.5:1 |
| 不透明度 | alpha / opacity | 0–1 或 0–100% |

### 本项目约定

- 课程块用**容器色**（`primaryContainer` 等）+ `on*Container` 文字，不用纯色块。
- 深色模式不写死颜色，全部走 `MaterialTheme.colorScheme`。
- 背景图模式要加 `scrim`，保证文字对比度。

---

## 3. 文字与排版

| 中文 | English | 说明 |
|---|---|---|
| 字阶 | type scale | M3 的 15 种字号 |
| 展示大字 | `displayLarge/Medium/Small` | 超大标题（本项目基本不用） |
| 标题 | `headlineLarge/Medium/Small` | 页面大标题 |
| 小标题 | `titleLarge/Medium/Small` | 卡片/分区标题 |
| 正文 | `bodyLarge/Medium/Small` | 段落、列表内容 |
| 标签 | `labelLarge/Medium/Small` | 按钮、课程格、辅助信息 |
| 字重 | font weight | Regular/Medium/Bold |
| 行高 | line height | 行间距 |
| 字距 | letter spacing | 字与字的间距 |
| 字号缩放 | font scale | 系统“字体大小”，要能放大到 200% |
| 单行 / 多行 | single line / max lines | 超出用省略号 |
| 省略号 | ellipsis | 文字截断的“…” |
| 自动换行 | wrap | 一行放不下时换行 |
| 设备无关像素 | dp | 布局尺寸单位 |
| 缩放像素 | sp | 字体单位，跟随系统字体大小 |

### 本项目约定

- 课程块文字随宽度自适应：平板 `labelMedium`，手机 `labelSmall`。
- 所有文字必须走 `MaterialTheme.typography`，不写死 `fontSize`。
- 课程名最多 2–3 行，超出省略；教室 1 行。

---

## 4. 形状、间距、层级

| 中文 | English | 说明 |
|---|---|---|
| 圆角 | corner radius | 例：12dp |
| 形状阶梯 | shape scale | extraSmall 4 / small 8 / medium 12 / large 16 / extraLarge 28 |
| 全圆角 | full / pill | 胶囊形 |
| 内边距 | padding | 内容与边框的距离 |
| 外边距 | margin | 元素与其他元素的距离 |
| 间距 | gap / spacing | 同级元素之间的距离 |
| 8dp 网格 | 8dp grid | 常用间距：4/8/12/16/24/32dp |
| 触控目标 | touch target | 最小 48×48dp |
| 高度 / 阴影 | elevation / shadow | 浮起程度 |
| 色调抬升 | tonal elevation | M3 用颜色表达层级 |
| 裁剪 | clip | 把内容裁进形状里 |
| 分隔线 | divider | 1dp 细线 |
| 密度 | density | 屏幕像素密度（dpi） |

### 常用尺寸速查（M3 默认，非硬性）

| 部件 | 尺寸 |
|---|---|
| 底部导航栏高度 | 80dp（不含系统 inset） |
| 导航侧栏宽度 | 80dp |
| FAB | 56dp；Extended FAB 高 56dp |
| 顶部应用栏 | 64dp；Medium 112dp；Large 152dp |
| 对话框圆角 | 28dp |
| 底部面板顶部圆角 | 28dp |
| 卡片圆角 | 12dp |
| 列表项高度 | 56dp（单行）/ 72dp（双行） |
| 课程块圆角 | 本项目 10–12dp |

---

## 5. 布局与自适应

| 中文 | English | 说明 |
|---|---|---|
| 视口 / 窗口 | viewport / window | 应用实际可用的区域 |
| 窗口尺寸类 | window size class | 按宽度分档，不看设备型号 |
| 紧凑 | compact | < 600dp，手机竖屏 |
| 中等 | medium | 600–839dp，小平板/折叠展开 |
| 扩展 | expanded | 840–1199dp，平板横屏 |
| 大 / 特大 | large / extra-large | ≥1200dp / ≥1600dp |
| 自适应 | adaptive | 布局随窗口改变结构（换导航形态、分栏） |
| 响应式 | responsive | 只做拉伸/换行，结构不变 |
| 单栏 / 双栏 / 三栏 | single-pane / two-pane / three-pane | 分栏布局 |
| 主从布局 | master-detail | 左列表 + 右详情 |
| 列表-详情 | list-detail | M3 canonical layout 之一 |
| 辅助面板 | supporting pane | 主内容旁放详情/工具 |
| 信息流 | feed | 单列可滚动内容流 |
| 侧栏 | navigation rail | 平板左侧竖排导航 |
| 底部导航栏 | navigation bar | 手机底部导航 |
| 抽屉导航 | navigation drawer | 大屏左侧可展开菜单 |
| 导航套件 | navigation suite | 自动在 bar/rail/drawer 间切换的组件 |
| 顶部应用栏 | top app bar | 标题、操作按钮 |
| 大标题应用栏 | large top app bar | 滚动时收起的大标题 |
| 分屏 / 多窗口 | split-screen / multi-window | 应用只占部分窗口 |
| 折叠屏 | foldable | 注意铰链与姿态 |
| 铰链 | hinge | 折叠屏中间的转轴区域 |
| 横屏 / 竖屏 | landscape / portrait | 方向 |
| 边到边 | edge-to-edge | 内容绘制到系统栏下面 |

### 本项目约定

- 断点用**窗口宽度**，不是设备类型：手机横屏也可能用侧栏。
- 平板（≥600dp）用侧栏，手机用底部导航。
- 平板宽屏课表页：左课表 + 右「本周课程」双栏。
- 未来可加：medium 窗口的 side sheet、expanded 的三栏。

---

## 6. 系统栏、手势与安全区

| 中文 | English | 说明 |
|---|---|---|
| 状态栏 | status bar | 顶部时间/电量 |
| 系统导航栏 | system navigation bar | 底部返回/主页手势区（注意与“应用底部导航栏”区分） |
| 手势提示线 | gesture handle / gesture bar | 底部那条小横线 |
| 手势导航 | gesture navigation | 全面屏手势 |
| 三键导航 | 3-button navigation | 返回/主页/最近任务 |
| 沉浸 | immersive | 内容延伸到系统栏后面 |
| 插入区 | window insets | 系统栏、刘海、键盘占用的区域 |
| 安全区 | safe area / safeDrawing | 不被系统栏遮挡的区域 |
| 键盘插入区 | IME insets | 软键盘高度 |
| 刘海/挖孔 | display cutout | 屏幕开孔 |
| 预测式返回 | predictive back | 返回前预览目标页面 |
| 系统栏对比度 | system bar contrast | 系统给手势区自动加的底色 |
| 系统栏图标明暗 | light/dark system bar icons | 状态栏图标颜色 |

### 本项目约定

- 全局 `enableEdgeToEdge()`，内容延伸到手势区。
- 关闭 `isNavigationBarContrastEnforced`，手势提示线沉浸。
- 侧栏背景贯穿整屏高度，导航项纵向居中，避开系统栏。
- App 自己的底部导航栏 ≠ 系统导航栏，沟通时说清“应用底部导航栏”。

---

## 7. 组件

### 按钮与操作

| 中文 | English | 说明 |
|---|---|---|
| 填充按钮 | filled button | 主操作 |
| 色调按钮 | filled tonal button | 次主操作 |
| 描边按钮 | outlined button | 次要操作 |
| 文字按钮 | text button | 低优先级 |
| 图标按钮 | icon button | 只有图标 |
| 悬浮操作按钮 | FAB (floating action button) | 页面主操作 |
| 扩展 FAB | extended FAB | 图标 + 文字 |
| 分段按钮 | segmented button | 2–5 个互斥选项 |
| 开关 | switch | 开/关 |
| 复选框 | checkbox | 多选 |
| 单选按钮 | radio button | 单选 |
| 滑块 | slider | 连续值 |

### 容器与导航

| 中文 | English | 说明 |
|---|---|---|
| 卡片 | card | filled / elevated / outlined |
| 列表项 | list item | 标题 + 副标题 + 前后图标 |
| 底部面板 | bottom sheet | 从底部升起的面板 |
| 模态底部面板 | modal bottom sheet | 带遮罩、阻断操作 |
| 非模态底部面板 | standard bottom sheet | 不阻断，可同时操作 |
| 对话框 | dialog | 居中弹窗 |
| 全屏对话框 | full-screen dialog | 手机上编辑长表单 |
| 侧面板 | side sheet | 平板上的侧边编辑面板 |
| 菜单 | menu / dropdown menu | 点击后弹出的选项列表 |
| 提示条 | snackbar | 底部短暂提示，可带操作 |
| 横幅 | banner | 页面顶部的常驻提示 |
| 气泡提示 | tooltip | 长按/悬停解释 |
| 徽标 | badge | 小红点/数字 |
| 标签页 | tabs | 顶部切换 |
| 分页器 | pager | 左右滑动页面 |
| 进度指示 | progress indicator | 线性/环形，确定/不确定 |
| 空状态 | empty state | 没数据时的提示 + 主操作 |
| 骨架屏 | skeleton | 加载中的灰色占位 |
| 占位符 | placeholder | 输入框灰字提示 |

### 输入

| 中文 | English | 说明 |
|---|---|---|
| 文本框 | text field | filled / outlined |
| 辅助文字 | supporting text | 输入框下方说明 |
| 错误提示 | error text | 校验失败 |
| 前缀/后缀 | prefix / suffix | 单位、符号 |
| 下拉选择 | exposed dropdown menu | 展开式选择器 |
| 日期选择器 | date picker | 日历弹窗 |
| 时间选择器 | time picker | 表盘/数字 |
| 步进器 | stepper | −/+ 调整数值（本项目大量使用） |
| 芯片 | chip | assist / filter / input / suggestion |
| 筛选芯片 | filter chip | 多选筛选，本项目用于星期/单双周 |

### 本项目映射

| 界面叫法 | 实际组件 / 代码 |
|---|---|
| 侧栏 | 自绘 `Surface + NavigationRailItem`（`FallAsleepApp.kt`） |
| 应用底部导航栏 | `NavigationBar`（`FallAsleepApp.kt`） |
| 周次切换按钮 | `FilledTonalIconButton` + 左右箭头 |
| 课表网格 | `WeekBoard` + `DayColumn` 自绘布局 |
| 课程块 | `CourseBlock`（`DayColumn` 内） |
| 本周课程面板 | `WeekOverview` |
| 添加课程操作面板 | `ModalBottomSheet`（`ScheduleScreen.kt`） |
| 课程详情 | `CourseDetailDialog` |
| 课程编辑 | `CourseEditorDialog`（全屏 Dialog） |
| 学期设置 | `TermSettingsDialog` |
| 新建课表 | `CreateTimetableDialog` |
| 今日页 | `TodayScreen` |

---

## 8. 交互、状态与动效

| 中文 | English | 说明 |
|---|---|---|
| 状态 | state | enabled / hovered / focused / pressed / dragged / disabled / selected |
| 选中态 | selected | 当前项高亮 |
| 禁用态 | disabled | 不可点击，降低对比度 |
| 水波纹 | ripple | 点击反馈 |
| 触感反馈 | haptic feedback | 震动 |
| 轻点 / 长按 / 滑动 / 拖拽 / 双指缩放 | tap / long press / swipe / drag / pinch | 手势 |
| 轻扫 | fling | 快速滑动带惯性 |
| 下滑关闭 | swipe to dismiss | 面板/通知下滑消失 |
| 撤销 | undo | Snackbar 里的“撤销” |
| 乐观更新 | optimistic update | 先改 UI 再写库 |
| 加载中 | loading | 转圈/骨架 |
| 空状态 | empty state | 无数据 |
| 错误状态 | error state | 加载失败/校验失败 |
| 过渡 | transition | 页面/元素切换动画 |
| 共享元素 | shared element / shared bounds | 元素在两页间平滑移动 |
| 缓动曲线 | easing | 动画速度曲线 |
| 时长 | duration | 短 50–200ms，中 250–400ms，长 450–600ms |
| 减弱动态效果 | reduced motion | 无障碍设置 |

### 本项目约定

- 周切换用滑动 + 淡入，约 220ms。
- 课程详情/编辑用系统对话框过渡，不自己造花哨动画。
- 删除用对话框二次确认（未来可加 Snackbar 撤销）。

---

## 9. 无障碍

| 中文 | English | 说明 |
|---|---|---|
| 无障碍 | accessibility (a11y) | 让所有人可用 |
| 屏幕阅读器 | screen reader / TalkBack | 朗读界面 |
| 内容描述 | content description | 图标的文字说明 |
| 语义 | semantics | Compose 的可访问性信息 |
| 焦点顺序 | focus order | 键盘/TalkBack 的移动顺序 |
| 对比度 | contrast ratio | 文字 4.5:1，大字 3:1 |
| 触控目标 | touch target | ≥48dp |
| 字体缩放 | font scaling | 系统字体放大 |
| 减弱动态 | reduce motion | 关闭/缩短动画 |

### 本项目约定

- 课程块要能读出来：「高等数学，周一第 1-2 节，第 1-16 周，教三-201」。
- 图标按钮必须有 `contentDescription`。
- 不只用颜色区分课程，课程名必须显示。

---

## 10. 课程表领域术语

| 中文 | English | 说明 |
|---|---|---|
| 课表 | timetable / schedule | 一周课程网格 |
| 学期 | term / semester | 开学日期、总周数 |
| 周次 | week | 第 N 周 |
| 当前周 | current week | 按开学日期算出的本周 |
| 节次 | period | 第几节课 |
| 起始节 / 连续节数 | start period / period count | 课程跨几节 |
| 单双周 | odd/even week（parity） | 只在单周或双周上 |
| 课程 | course | 课程基本信息 |
| 上课安排 | session | 一门课的一次时间地点安排 |
| 课程块 | course block | 网格里的色块 |
| 课表格子 | grid cell | 网格单元 |
| 空白格 | empty slot | 没课的格子 |
| 重叠 | overlap | 同一格有多门课 |
| 分栏 | lanes | 重叠课程并排显示 |
| 时间列 | time column | 左侧节次/时间 |
| 表头 | header | 星期与日期 |
| 今日列 | today column | 今天所在列 |
| 非本周课程 | off-week course | 本周不上、但学期内有 |
| 多课表 | multiple timetables | 主课表/实验/考研 |
| 周视图 | week view | 一周网格 |
| 今日视图 | today view | 当天课程列表 |
| 周课表小组件 | week widget | 桌面小部件 |

---

## 11. 易混淆对照（说错会改错地方）

| 容易混 | 区别 |
|---|---|
| 系统导航栏 vs 应用底部导航栏 | 前者是手机底部返回/手势区；后者是 App 自己的三个 Tab |
| 状态栏 vs 应用栏 | 状态栏显示时间电量；应用栏是 App 的标题栏 |
| 自适应 vs 响应式 | 自适应会换结构（栏→导航栏）；响应式只拉伸换行 |
| 对话框 vs 底部面板 | 对话框居中、强打断；底部面板从底部升起、可滑动关闭 |
| 模态 vs 非模态 | 模态有遮罩、必须先处理；非模态可与主界面同时操作 |
| 菜单 vs 底部面板 | 菜单是锚定的小浮层；底部面板是大面板，适合放列表 |
| FAB vs 扩展 FAB | 只有图标 vs 图标+文字 |
| 侧栏 vs 抽屉 | 侧栏常驻窄条；抽屉可展开/收起 |
| 卡片 vs 列表项 | 卡片是容器；列表项是行式信息 |
| 表面色 vs 容器色 | surface 是页面底；*Container 是内容块底 |
| 高度 vs 阴影 | elevation 是抽象层级；shadow 是视觉投影 |
| 色调抬升 vs 阴影 | M3 优先用颜色表达层级 |
| padding vs margin vs gap | 内边距 vs 外边距 vs 同级间距 |
| dp vs sp | 布局单位 vs 字体单位 |
| 海报/占位 vs 空状态 | 占位是加载中；空状态是无数据 |
| Snackbar vs Toast | Snackbar 在 App 内、可带按钮；Toast 是系统浮层（本项目不用 Toast） |
| 省略号 vs 裁剪 | 文字截断加“…” vs 直接切掉 |
| 筛选芯片 vs 分段按钮 | 芯片可多选/可取消；分段按钮通常单选 |
| 开关 vs 复选框 | 立即生效 vs 提交时生效 |
| 主色 vs 强调色 | M3 没有 accent，用 primary/secondary/tertiary |

---

## 12. 沟通例句（可直接套）

**尺寸/形状**

- 「课程块圆角 12dp，内外边距 6dp，重叠时两块之间留 4dp。」
- 「平板侧栏 96dp，图标 24dp，标签 12sp。」
- 「FAB 距屏幕右边和底部各 24dp，横屏时用 slightly larger。」

**颜色**

- 「课程块底色用 `secondaryContainer`，文字 `onSecondaryContainer`；深色模式不要用高饱和纯色。」
- 「今天那一列加 4% 的 `primary` 蒙层。」
- 「弹窗背后的 scrim 透明度 32%。」

**布局**

- 「手机竖屏单栏；≥600dp 改侧栏；≥840dp 课表页右栏显示本周课程。」
- 「手机 7 天课表横向滚动，最小列宽 64dp；5 天时不滚动。」
- 「横屏手机把大标题栏换成普通 top app bar，省高度。」

**交互**

- 「长按空白格直接进入添加课程，并预填星期和节次。」
- 「课程详情在手机上用底部面板，平板上用右侧面板。」
- 「删除后 Snackbar 提示，带“撤销”。」
- 「切换周次时保留滚动位置，还是回到顶部？」

**无障碍**

- 「课程块的 TalkBack 朗读顺序：课程名 → 星期节次 → 周次 → 教室。」
- 「所有图标按钮都要有 contentDescription，装饰性图标设 null。」

**系统与边到边**

- 「手势提示线区域要沉浸，导航背景延伸到 navigationBars inset。」
- 「键盘弹出时，编辑框要滚到可见区域（IME inset）。」

---

## 13. 附：本项目当前的设计口径

- 主色：动态取色（Android 12+），低版本用内置蓝紫色种子。
- 圆角：卡片 16dp，课程块 10dp（手机）/12dp（平板可考虑统一 12dp）。
- 课程块：6 种容器色，文字用对应 `on*` 色。
- 字体：全部 M3 字阶，课程名最多 2–3 行。
- 间距：8dp 基准，页面左右 16dp。
- 导航：手机底部栏；≥600dp 左侧栏（80dp，纵向居中）。
- 断点：600dp / 840dp。
- 操作：右下角 FAB 统一承载「添加课程 / 切换课表 / 新建课表」。
