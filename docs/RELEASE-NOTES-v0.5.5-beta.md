# FallAsleep 课程表 v0.5.5-beta

发布日期：2026-11-02
版本：`0.5.5-beta`（versionCode 11）
包名：`io.github.tukinokilyowsoo.fallasleeptimetable`
APK：`FallAsleepTimetable_arm64_v0.5.5-beta.apk`（arm64-v8a，约 11 MB）
SHA-256：`25748dce437db5c9fb946db84882d4331b89aae4eee6fe9ac92ce65b593b25e0`

---

## 本版新增

### JSON 备份可恢复学期设置与节次时间

导出的 JSON 一直包含 `term` 和 `timeSlots`，但之前导入时被忽略。本版补齐：

- 解析 `term`：开学日期、总周数、每天节数、周六/周日显示。
- 解析 `timeSlots`：每节课的开始/结束分钟数，并过滤无效条目。
- 导入前新增确认弹窗：
  - **一并恢复**：把学期设置与节次时间写回当前学期，再导入课程；
  - **仅导入课程**：保持旧行为，只导入课程到当前学期；
  - **取消**：不导入。
- 弹窗会列出文件中的开学日期、周数、每天节数、节次条数，并明确提示会覆盖该学期下所有课表共用的设置。
- 恢复时对开学日期、周数、每天节数做范围校验，避免损坏文件写入异常数据。

### 基于 0.5.4 的 Niagara 组件方案 A

同时包含 0.5.4 的 Niagara 透明组件改动：只显示开始时间/课程名/教室、字号上限 30sp、0.82 安全系数、56dp 右侧安全区、按剩余课程数分行。

---

## 测试

- JUnit 22 项全部通过（新增 `TimetableImportSettingsTest` 4 项：term/timeSlots 解析、导入预览、无设置 JSON、无效节次过滤）。
- 单元测试新增 `org.json:json` 依赖，避免 Android `org.json` 在本地 JVM 未实现。
- `assembleRelease` 构建成功，使用既有 release 签名密钥。
- 本轮未推送 GitHub、未安装到设备。

---

## 已知限制

- 「一并恢复」会影响当前学期下所有课表，弹窗已明确提示。
- JSON 仍只导入**一张课表**，多课表恢复仍需后续迭代逐个导出/导入。
- Niagara 组件外层高度仍由启动器持有，App 只能通过最小高度允许用户拖到一行。
