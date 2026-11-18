# FallAsleep 课程表 v1.2.1

离线、无广告、完全免费的 Material You 课程表。1.2.1 主要修复部分教务系统在应用内打不开的问题，并加入本机记住密码。

- 版本：`1.2.1`（versionCode 28）
- 包名：`io.github.tukinokilyowsoo.fallasleeptimetable`
- APK：`FallAsleepTimetable_arm64_v1.2.1.apk`（arm64-v8a，10,974,667 字节）
- SHA-256：`dc636c6b510970d78fff39aea331a255a8ad283b36a7c33c40aa6a982f170e28`
- 最低系统：Android 8.0（API 26）
- 语言：简体中文

---

## 本次更新

### 教务导入

- 输入 http 网址时优先按 https 打开，失败再回退到 http。避免 http 页面里的字体、iframe 被升级成 https 后变成跨源，导致页面空白或抓不到课表。
- WebView 默认 UA 改为普通手机 Chrome，不再带 `wv` / `Version/4.0`，减少教务系统对 WebView 的拦截。
- 宽桌面页面自动缩放适配屏幕，可双指放大。
- 支持 `window.open` / `target="_blank"`，办事大厅类门户可以直接进入教务系统。
- 抓取页面时递归收集多层 iframe。
- 新增 WebView 诊断日志：主页面 / iframe 加载失败、HTTP 错误、JS 脚本错误都会写入日志。
- 排查期间暂时放宽了 WebView 的文件访问与混合内容限制，后续版本再收紧。

### 记住密码

- 导入页新增「记住密码」开关。
- 账号密码按站点区分，使用 Android Keystore 的 AES/GCM 密钥加密后保存在应用私有目录，应用不参与云备份。
- 下次打开同一站点登录页时自动填入空的账号 / 密码框，不自动提交。
- 关闭开关会立即清除本机保存的凭据。

## 升级说明

- 与 v1.2.0 使用相同的 release 签名，可直接覆盖安装并保留数据。
- 从 debug 版升级仍需卸载重装（签名不同），建议先导出 JSON。

## 已知问题

- 部分学校只提供 http，应用会自动回退，但页面自身的混合内容限制仍然存在。
- 教务页面改版可能导致解析失效，可通过「导出日志」反馈页面内容。
- 小组件的下课精准刷新依赖「闹钟和提醒」权限；未授权时最长约 30 分钟刷新一次。

## 反馈

- 问题与建议：<https://github.com/tukinokilyowsoo/FallAsleepTimetable/issues>

---

© 2026 槻ノ木 響子 · Lyowsoo
