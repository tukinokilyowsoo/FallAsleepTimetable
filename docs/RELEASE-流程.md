# 发布流程（GitHub Release）

## 签名密钥（务必备份）

- Keystore：`/data/user/0/cc.codroid/files/home/.fallasleep/fallasleep-release.jks`
- 备份：`/sdcard/cccodroid/FallAsleep-release-keystore/`（含 `fallasleep-release.jks` 和 `credentials.txt`）
- Gradle 从 `~/.gradle/gradle.properties` 读取：
  - `FALLASLEEP_STORE_FILE`
  - `FALLASLEEP_STORE_PASSWORD`
  - `FALLASLEEP_KEY_ALIAS`
  - `FALLASLEEP_KEY_PASSWORD`

**注意**：

- 密钥和密码不要提交到仓库；`~/.gradle/gradle.properties` 与 `~/.fallasleep/` 都在应用私有目录。
- 一旦丢失 keystore，已安装用户将无法升级，只能卸载重装（数据丢失）。
- 共享目录里的备份含明文密码，确认自己保存好后建议删除共享目录中的副本。

## 发版步骤

1. 修改 `app/build.gradle.kts`：
   - `versionCode` 递增（当前 3）；
   - `versionName` 改为新版本号，例如 `0.3.0-beta2`。
2. 构建：
   ```sh
   $HOME/gradle-8.14.3/bin/gradle assembleRelease
   ```
   产物：`app/build/outputs/apk/release/app-release.apk`
3. 校验签名：
   ```sh
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   sha256sum app/build/outputs/apk/release/app-release.apk
   ```
4. 更新 `docs/RELEASE-NOTES-vX.Y.Z.md`（功能、已知问题、SHA-256）。
5. 提交并推送：
   ```sh
   git add -A && git commit -m "chore: 发布 vX.Y.Z"
   git push
   ```
6. 创建 GitHub pre-release（测试版）：
   ```sh
   gh release create vX.Y.Z-betaN \
     app/build/outputs/apk/release/app-release.apk \
     --title "FallAsleep 课程表 vX.Y.Z-betaN" \
     --notes-file docs/RELEASE-NOTES-vX.Y.Z.md \
     --prerelease
   ```
7. 正式版去掉 `--prerelease`，并可用 `--latest` 标记最新。

## 测试者范围

仓库目前是私有的，Release 只有仓库协作者/自己可见。
若要让外部测试者下载：

- 把仓库改为公开，或
- 把测试者加为 collaborators，或
- 只把 APK 链接私下发给对方（GitHub Release 资源对私有仓库需要登录）。

## 安装说明（发给测试者）

- 新版签名与 debug 版不同：先卸载旧 debug 包（会清空数据）。
- 建议卸载前导出 JSON 课表备份，安装后导入。
