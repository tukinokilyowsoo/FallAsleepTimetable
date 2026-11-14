package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.data.update.UpdateCheckResult
import io.github.fallasleeptimetable.data.update.UpdateChecker
import io.github.fallasleeptimetable.data.update.UpdateSource
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    data object Checking : UpdateState
    data class Available(val result: UpdateCheckResult.Available) : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

@Composable
fun UpdateDialog(
    currentVersion: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var source by remember {
        mutableStateOf(
            UpdateSource.fromKey(prefs.getString("update_source", null)),
        )
    }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var betaPlan by remember {
        mutableStateOf(prefs.getBoolean("beta_plan", false))
    }
    val scope = rememberCoroutineScope()

    fun check(selected: UpdateSource) {
        state = UpdateState.Checking
        scope.launch {
            state = runCatching {
                UpdateChecker.check(selected, currentVersion)
            }.fold(
                onSuccess = { result ->
                    when (result) {
                        is UpdateCheckResult.Available ->
                            UpdateState.Available(result)

                        is UpdateCheckResult.UpToDate ->
                            UpdateState.UpToDate(result.currentVersion)
                    }
                },
                onFailure = {
                    UpdateState.Failed(it.message ?: "网络连接失败")
                },
            )
        }
    }

    LaunchedEffect(source) {
        check(source)
    }

    val available = (state as? UpdateState.Available)?.result?.info

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查更新") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "当前版本 $currentVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UpdateSource.entries.forEach { option ->
                        FilterChip(
                            selected = source == option,
                            onClick = {
                                if (source != option) {
                                    source = option
                                    prefs.edit()
                                        .putString("update_source", option.key)
                                        .apply()
                                }
                            },
                            label = { Text(option.label) },
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "加入 Beta 计划",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "自动提醒 Beta 版本更新",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = betaPlan,
                        onCheckedChange = { checked ->
                            betaPlan = checked
                            prefs.edit().putBoolean("beta_plan", checked).apply()
                        },
                    )
                }
                HorizontalDivider()

                when (val current = state) {
                    UpdateState.Checking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("正在检查…", style = MaterialTheme.typography.bodyMedium)
                    }

                    is UpdateState.UpToDate -> Text(
                        text = "已是最新版本（${current.version}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    is UpdateState.Failed -> Text(
                        text = "检查失败：${current.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    is UpdateState.Available -> {
                        Text(
                            text = "发现新版本 ${current.result.info.tag}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = current.result.info.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (current.result.info.notes.isNotBlank()) {
                            Text(
                                text = current.result.info.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (available != null) {
                TextButton(
                    onClick = {
                        val url = available.downloadUrl ?: available.pageUrl
                        if (url.isNotBlank()) openUrl(context, url)
                        onDismiss()
                    },
                ) { Text("下载更新") }
            } else {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
        dismissButton = {
            when (state) {
                is UpdateState.Failed -> TextButton(onClick = { check(source) }) {
                    Text("重试")
                }

                is UpdateState.Available -> TextButton(
                    onClick = {
                        available?.let { info ->
                            if (info.pageUrl.isNotBlank()) {
                                openUrl(context, info.pageUrl)
                            }
                        }
                    },
                ) { Text("Release 页面") }

                else -> Row {
                    TextButton(onClick = { check(source) }) { Text("重新检查") }
                    TextButton(onClick = onDismiss) { Text("稍后") }
                }
            }
        },
    )
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
