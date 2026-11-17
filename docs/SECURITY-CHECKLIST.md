# 安全清单（FallAsleep 课程表）

范围：Android 客户端源码、WebView 教务导入、文件导入/导出、更新检查、小组件、发布签名。
不包含：Huawei AppGallery / GitHub 账号安全、开发者电脑环境。

## 威胁模型

- 数据只保存在本机；没有自建后端、账号或统计 SDK。
- 不可信输入：用户输入的教务网址、教务页面 HTML、导入的 JSON/ICS/CSV/旧格式文件、GitHub Release 元数据。
- 主要风险：WebView 加载危险 scheme、导入文件过大/字段越界、导出日志泄露教务页面里的 token、外部链接打开非预期 scheme、导出组件被其他应用调用。

## 检查清单

### 网络与 WebView

- [x] 更新检查只访问 HTTPS 的 GitHub API / gh-proxy。
- [x] 教务网址只接受 `http` / `https`，拒绝 `file:`、`content:`、`javascript:` 等 scheme。
- [x] WebView 显式关闭文件访问、内容访问、file URL 跨源访问。
- [x] 混合内容从 `ALWAYS_ALLOW` 降为 `COMPATIBILITY_MODE`。
- [x] 退出教务导入页时清除 WebView Cookie，避免会话长期驻留。
- [x] 抓取页面设置 2,000,000 字符上限，避免超大 DOM 造成内存压力。
- [x] WebView 不覆盖 SSL 错误处理，证书错误默认中止。
- [ ] 应用级 `usesCleartextTraffic=true`：为兼容仅提供 http 的学校站点保留，属于已接受风险；课表、设置、更新检查均不使用明文。

### 文件导入 / 导出

- [x] 导入文件限制 10 MB，超过直接报错。
- [x] 课程节次、周次、单双周在解析和入库时双重限制范围。
- [x] 所有导入通过 SAF 读取，不拼接外部路径。
- [x] FileProvider 仅暴露 `cache/share/`，`exported=false`。
- [x] 导出使用 SAF `CreateDocument`，不写入共享存储的固定路径。
- [x] SQL 全部参数化，无字符串拼接查询。

### 诊断日志与隐私

- [x] 导入日志写入应用私有目录，不自动上传。
- [x] 日志写入前脱敏 password / token / session / csrf / ticket 等键值，以及密码输入框的 value。
- [x] 日志正文有大小上限；设置页提示用户发送前自行检查。
- [x] 无账号、无后端、无广告/统计 SDK。
- [x] 云备份与设备迁移全部关闭（`allowBackup=false` + data extraction rules）。

### 组件与 Intent

- [x] 只有 MainActivity 导出（launcher）。
- [x] 小组件配置 Activity 校验 appWidgetId 存在且 provider 与组件类型匹配。
- [x] `WidgetRefreshReceiver` 校验 action 白名单，只响应自定义刷新与系统受保护广播。
- [x] FileProvider 不导出，只按 URI 授权。
- [x] 外部链接只允许 `http` / `https` scheme 后交给系统浏览器。
- [x] 更新只打开下载页，不自动下载/安装；安装时由 Android 校验签名。

### 权限

- [x] INTERNET、VIBRATE、READ/WRITE_CALENDAR、SCHEDULE_EXACT_ALARM、RECEIVE_BOOT_COMPLETED 均有实际功能对应。
- [x] 日历权限在运行时申请；精确闹钟通过系统专项设置申请，未授权时降级为周期刷新。
- [x] 未申请存储、通讯录、定位、相机、麦克风等无关权限。

### 本地数据与签名

- [x] 数据库与偏好保存在应用私有目录，依赖 Android 沙箱。
- [x] release keystore 不进入仓库；备份放在 `/sdcard/cccodroid/FallAsleep-release-keystore/`，并附带 SHA-256 校验。
- [x] release 与 debug 签名分离；release 构建从 `~/.gradle/gradle.properties` 读取口令。
- [ ] 数据库未使用 SQLCipher 加密：本机课表数据的敏感度有限，沙箱 + 关闭备份视为可接受风险。

## 本次已修复

1. 教务导入 URL 增加 scheme 白名单（`EduImportActivity.kt`）。
2. WebView 关闭文件/内容访问、降低混合内容等级、退出时清除 Cookie、页面大小上限。
3. 导入日志脱敏 password/token/session/csrf 等字段，设置页提示已脱敏。
4. 文件导入增加 10 MB 上限。
5. 更新弹窗与联系页的外部链接只允许 http/https。
6. 小组件配置页校验 provider，刷新广播增加 action 白名单。
7. 移除未使用的 `ic_my_location` 资源；自适应图标补 `monochrome`。
8. Welcome 页滚动偏移改为在 `graphicsLayer` 内读取，避免高频重组。

## 残余风险（已接受）

- 部分学校只提供 HTTP，应用保留全局 cleartext；用户输入的站点本身可能是钓鱼站。
- 更新下载由浏览器完成，应用不校验 APK 哈希；Android 安装器会校验包名与签名，但不能防止仓库被完全接管。
- 未固定 GitHub API 证书（系统 CA 信任链）。
- 数据库未加密；有 root 的设备可读取应用私有目录。
- 小组件配置 Activity 仍对外导出（系统要求），已通过 appWidgetId + provider 校验降低风险。

## 验证

```sh
cd /data/user/0/cc.codroid/files/home/workspace/FallAsleepTimetable
$HOME/gradle-8.14.3/bin/gradle lintDebug
```

- 目标：0 个 Error；关键 Warning（WebView JS、外部链接、导出组件）均已人工确认。
- 手工检查：输入 `javascript:alert(1)` 应提示无效网址；导入超过 10 MB 文件应报错；导出日志中不应出现明文 token/password。
