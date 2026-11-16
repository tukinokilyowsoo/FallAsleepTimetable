# 华为应用市场（AppGallery）分支说明

本分支 `huawei` 用于华为应用市场分发，与 `main`（GitHub Release）分开维护。

## 与 main 的差异

| 项目 | main / GitHub 版 | huawei / AppGallery 版 |
|---|---|---|
| 包名 | `io.github.tukinokilyowsoo.fallasleeptimetable` | `io.github.tukinokilyowsoo.fallasleeptimetable.huawei` |
| 更新方式 | 应用内检查 GitHub Release，跳转浏览器下载 | 不自动检查、不下载 APK；设置 → 关于 → 检查更新 打开应用市场详情页 |
| Beta 计划 | 支持 | 隐藏/不生效 |
| 签名 | 本地 release keystore | 上传 key 仍为本地 release keystore；AppGallery Connect 使用华为应用签名重新签名 |
| 数据 | 与本分支不互通 | 与本分支不互通；切换渠道需导出 JSON 再导入 |

两个包名不同，可以同时安装，但数据独立。

## 构建

```sh
cd /data/user/0/cc.codroid/files/home/workspace/FallAsleepTimetable
$HOME/gradle-8.14.3/bin/gradle assembleRelease   # 上传 key 签名的 APK，用于本地测试
$HOME/gradle-8.14.3/bin/gradle bundleRelease     # AAB，推荐上传到 AGC 并使用华为应用签名
```

产物：

- APK：`app/build/outputs/apk/release/app-release.apk`
- AAB：`app/build/outputs/bundle/release/app-release.aab`

> AppGallery Connect 开启「应用签名」后，商店分发版本由 AGC 重新签名；本地产物只用于测试。上传 key（现有 release keystore）必须备份并保持不变，否则后续无法升级。

## AppGallery Connect 配置

1. 注册华为开发者账号并完成实名认证。
2. AppGallery Connect → 我的应用 → 新建应用：
   - 平台：Android
   - 包名：`io.github.tukinokilyowsoo.fallasleeptimetable.huawei`
   - 应用名称：FallAsleep 课程表
3. 证书指纹：用 `keytool -list -v -keystore <keystore>` 取上传证书的 SHA-256 填入（若后续接入 HMS 服务必须与 AGC 应用签名证书一致）。
4. 开启「应用签名」（App Signing），按控制台指引上传 key 或让 AGC 生成签名证书；后续所有版本使用同一上传 key。
5. 上传 AAB（推荐）或 APK，填写版本说明，提交审核。

## 上架材料清单

- 应用图标：216×216 PNG。
- 截图：至少 3 张，建议 450×800 竖屏；可补充平板截图。
- 应用简介、详细描述、分类、年龄分级。
- 隐私政策 URL（仓库中的 `PRIVACY.md` 需发布为可访问网页）。
- 中国大陆分发：APP 备案号；部分类目需要软件著作权证书。
- 权限说明：日历读写、精确闹钟、振动、网络；审核时需说明用途。
- 无账号、无后端、无广告、无统计 SDK，可在审核备注中说明。

## 更新合规

华为审核明确禁止应用自行下载、安装或替换更新包。本分支已：

- 禁用启动时的 GitHub 更新检查与 Snackbar；
- 移除「拒收/接收更新提醒」与 Beta 计划入口；
- 将「检查更新」改为打开应用市场本应用详情页（`market://details?id=...`，限定 `com.huawei.appmarket`）。

应用市场未安装或尚未上架时，会提示用户到华为应用市场搜索。

## 已知边界

- **HarmonyOS NEXT / 5 / 6 / 7 不运行 APK**，本分支只覆盖 HarmonyOS 4.x 及以下、EMUI、海外华为设备。原生鸿蒙版本见 `harmonyos` 分支。
- 两个渠道包名不同，数据不互通。
- AppGallery 的更新检查需要应用已在市场正式上架后才能验证；本地上传 key 签名包与商店重签名包的 versionCode 必须递增。
