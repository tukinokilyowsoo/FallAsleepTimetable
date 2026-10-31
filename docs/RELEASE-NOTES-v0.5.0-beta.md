# FallAsleep 课程表 v0.5.0-beta

发布日期：2026-10-31
版本：`0.5.0-beta`（versionCode 6）
包名：`io.github.tukinokilyowsoo.fallasleeptimetable`
APK：`FallAsleepTimetable_arm64_v0.5.0-beta.apk`（arm64-v8a，约 10 MB）
SHA-256：`423c4a8e286ecfeaf9bd5b19a6e8cf05907616aee6f2d221edd2fdb2f89316b4`

---

## 新增

### ICS 日历导出 / 导入

- 设置 → 数据 → **导出 ICS 日历**：生成 `.ics`，可导入系统日历、Google 日历等。
- 每次上课安排一个事件，用 RRULE 表达连续周和单双周。
- 课程名、教师、教室、节次、起止周写入事件；中文按 RFC 5545 转义并折行。
- 本应用重新导入自己导出的 ICS 时，周次、单双周、节次、教师精确还原。
- 导入第三方 ICS：按事件日期与学期开学日期推算周次、星期，按节次时间反推节次；
  支持 `TZID`、UTC（`Z`）与浮动时间；跳过全天事件和学期外事件。

### CSV 表格导出 / 导入

- 设置 → 数据 → **导出 CSV 表格**：UTF-8 BOM + CRLF，Excel 打开不乱码。
- 列：`课程名称,教师,教室,星期,开始节次,节数,开始周,结束周,单双周`。
- 导入兼容常见列名别名、中文/数字/英文星期、`1-16` 周次范围与全周/单周/双周。
- 无效行跳过并保留其他课程；缺少必填列时给出明确提示。

### 导入自动识别

- 「导入课表」现在自动识别 JSON 备份、旧版 `.wakeup_schedule`、ICS、CSV，
  导入后生成一张新课程表并自动切换。

---

## 测试

- 新增 JUnit 测试 14 项通过：
  - `IcsTimetableTest`：导出转义/折行、自身往返、第三方 TZID + 单双周、逐周事件合并、跳过全天与学期外事件；
  - `CsvTimetableTest`：BOM/引号转义、自身往返、第三方列名与范围、坏行跳过、缺列报错。
- 构建：`assembleRelease`、`assembleDebug` 均成功；release APK 使用既有签名密钥。

---

## 已知问题

- ICS 导出使用设备本地浮动时间，跨时区查看时按查看端当地时间显示。
- ICS 导入按当前学期的节次时间表反推节次；与学期时间差异超过 45 分钟的课程会被跳过。
- 第三方 ICS 中带 `RECURRENCE-ID` 的改期例外暂不单独导入。
- 分享长图、课程搜索、无障碍完善仍待后续迭代。
