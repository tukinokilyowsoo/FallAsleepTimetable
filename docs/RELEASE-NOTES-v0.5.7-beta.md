# FallAsleep 课程表 v0.5.7-beta

发布日期：2026-11-02
版本：`0.5.7-beta`（versionCode 13）
包名：`io.github.tukinokilyowsoo.fallasleeptimetable`
APK：`FallAsleepTimetable_arm64_v0.5.7-beta.apk`（arm64-v8a，约 11 MB）
SHA-256：`495963b588b2611cc56d16971cbc76f8db72fe738dc3776798dcb51dc4e98e1e`

---

## 合规与隐私收尾

### 应用内法律入口

设置 → 关于新增三项，均可离线查看：

- **许可证**：PolyForm Noncommercial 1.0.0 全文与 Required Notice
- **第三方依赖**：随 APK 分发的组件、版本、许可证与完整文本（Apache-2.0 / MIT / BSD-3-Clause）
- **隐私政策**：本地数据、权限、教务导入、系统日历、导出/分享与删除方式

三个对话框使用内置轻量 Markdown 渲染：标题、粗体、列表、表格、代码块、分隔线、行内代码与链接，不再显示原始 `#` / `|` / `**`。

### 数据绝对不出设备

- `allowBackup=false`
- `fullBackupContent=false`（Android 11 及以下）
- `dataExtractionRules` 排除云备份与设备迁移的全部目录（Android 12+）
- 除用户主动发起（教务网页、导出/分享、写入系统日历）外，应用不会主动联网或上传；没有统计、广告、崩溃上报 SDK。

> 注意：系统备份与换机迁移已关闭。换设备前请在旧设备导出 JSON，再到新设备导入。

### 随 APK 打包的文本

- `assets/LICENSE.txt`
- `assets/THIRD-PARTY-NOTICES.md`
- `assets/PRIVACY.md`

---

## 同时包含

- 0.5.6：Niagara 组件改名「今日课程*针对Niagara优化」、显示课程数与空行占位、字号按当天全部课程测量。
- 0.5.5：JSON 备份可选恢复学期设置与节次时间。
- 0.5.4：Niagara 方案 A（开始时间/课程名/教室、30sp 上限、0.82 安全系数、56dp 右侧安全区）。

---

## 测试

- JUnit 22 项全部通过。
- `assembleRelease` 构建成功，使用既有 release 签名密钥。
- 已上传 GitHub pre-release。

---

## 已知限制

- 关闭系统备份后，换机不会自动迁移数据，需要手动导出/导入 JSON。
- Niagara 组件外层高度仍由启动器控制；显示课程数不足时用空行占位，但不改变外层高度。
- 教务系统若使用 HTTP，登录信息可能以未加密方式传输；建议优先 HTTPS。
