package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 应用内许可证入口。
 *
 * APK 中内置完整许可证文本，保证分发时接收者能拿到条款与 Required Notice。
 */
@Composable
fun LicenseDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    LegalTextDialog(
        context = context,
        title = "许可证",
        assetName = "LICENSE.txt",
        summary = "禁止商业使用；商业授权请通过仓库 Issues 联系作者。",
        fallback = "PolyForm Noncommercial License 1.0.0\n" +
            "https://polyformproject.org/licenses/noncommercial/1.0.0\n\n" +
            "Required Notice: Copyright (c) 2026 tukinokilyowsoo",
        onDismiss = onDismiss,
    )
}

/** 第三方依赖声明，列出随 APK 分发的组件、版本与许可证。 */
@Composable
fun ThirdPartyNoticesDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    LegalTextDialog(
        context = context,
        title = "第三方依赖",
        assetName = "THIRD-PARTY-NOTICES.md",
        summary = "随 APK 分发的开源组件、版本与许可证全文。",
        fallback = "第三方依赖声明未找到，请查看仓库中的 THIRD-PARTY-NOTICES.md。",
        onDismiss = onDismiss,
    )
}

/** 隐私政策：说明本地数据、权限与教务导入的数据流。 */
@Composable
fun PrivacyPolicyDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    LegalTextDialog(
        context = context,
        title = "隐私政策",
        assetName = "PRIVACY.md",
        summary = "离线、无账号、无自建后端；说明本地数据、权限与教务导入。",
        fallback = "隐私政策未找到，请查看仓库中的 PRIVACY.md。",
        onDismiss = onDismiss,
    )
}

@Composable
private fun LegalTextDialog(
    context: Context,
    title: String,
    assetName: String,
    summary: String,
    fallback: String,
    onDismiss: () -> Unit,
) {
    val text = remember(assetName) {
        runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.getOrElse { fallback }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    MarkdownDocument(text = text)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
