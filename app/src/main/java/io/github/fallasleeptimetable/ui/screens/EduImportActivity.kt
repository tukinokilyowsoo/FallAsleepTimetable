package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.fallasleeptimetable.data.AppContainer
import io.github.fallasleeptimetable.data.diagnostics.ImportLog
import io.github.fallasleeptimetable.data.importer.EduParseResult
import io.github.fallasleeptimetable.data.importer.EduScheduleParser
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.periodLabel
import io.github.fallasleeptimetable.ui.theme.FallAsleepTheme
import io.github.fallasleeptimetable.ui.weekRangeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONTokener

class EduImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val termId = intent.getLongExtra(EXTRA_TERM_ID, -1L)
        val initialUrl = intent.getStringExtra(EXTRA_URL)
        val currentTimetableId = intent.getLongExtra(EXTRA_CURRENT_TIMETABLE_ID, -1L)
        val currentTimetableName = intent.getStringExtra(EXTRA_CURRENT_TIMETABLE_NAME)
        setContent {
            FallAsleepTheme {
                EduImportScreen(
                    termId = termId,
                    initialUrl = initialUrl,
                    currentTimetableId = currentTimetableId,
                    currentTimetableName = currentTimetableName,
                    onImported = { timetableId, timetableName, overwritten ->
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_TIMETABLE_ID, timetableId)
                                .putExtra(EXTRA_TIMETABLE_NAME, timetableName)
                                .putExtra(EXTRA_OVERWRITTEN, overwritten),
                        )
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_TERM_ID = "extra_term_id"
        const val EXTRA_TIMETABLE_ID = "extra_timetable_id"
        const val EXTRA_TIMETABLE_NAME = "extra_timetable_name"
        const val EXTRA_CURRENT_TIMETABLE_ID = "extra_current_timetable_id"
        const val EXTRA_CURRENT_TIMETABLE_NAME = "extra_current_timetable_name"
        const val EXTRA_OVERWRITTEN = "extra_overwritten"
        const val EXTRA_URL = "extra_url"
    }
}

private const val GRAB_PAGE_JS = """
(function() {
  var html = document.documentElement ? document.documentElement.outerHTML : '';
  var frames = document.getElementsByTagName('iframe');
  for (var i = 0; i < frames.length; i++) {
    try {
      var doc = frames[i].contentDocument;
      if (doc && doc.documentElement) {
        html += doc.documentElement.outerHTML;
      }
    } catch (e) {}
  }
  return html;
})()
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduImportScreen(
    termId: Long,
    initialUrl: String? = null,
    currentTimetableId: Long = -1L,
    currentTimetableName: String? = null,
    onImported: (Long, String, Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val repository = remember { AppContainer.repository(context) }
    val scope = rememberCoroutineScope()

    var url by rememberSaveable {
        mutableStateOf(
            initialUrl?.takeIf { it.isNotBlank() }
                ?: prefs.getString("edu_url", "")
                ?: "",
        )
    }
    var parsed by remember { mutableStateOf<EduParseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var overwriteCurrent by rememberSaveable { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
            val initial = url.trim()
            if (initial.isNotEmpty()) {
                loadUrl(if (initial.startsWith("http")) initial else "http://$initial")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    fun openUrl() {
        val input = url.trim()
        if (input.isEmpty()) {
            error = "请输入教务网址"
            return
        }
        val normalized = if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "http://$input"
        }
        url = normalized
        prefs.edit().putString("edu_url", normalized).apply()
        webView.loadUrl(normalized)
    }

    fun grabPage() {
        if (loading) return
        loading = true
        val pageUrl = webView.url ?: url
        webView.evaluateJavascript(GRAB_PAGE_JS) { result ->
            val html = runCatching {
                JSONTokener(result ?: "null").nextValue() as? String
            }.getOrNull()
            if (html.isNullOrBlank()) {
                loading = false
                error = "没有抓取到页面内容，请等页面加载完成后再试"
                ImportLog.append(
                    context,
                    header = "教务导入失败：页面内容为空\nurl=$pageUrl",
                )
                return@evaluateJavascript
            }
            scope.launch {
                val outcome = withContext(Dispatchers.Default) {
                    runCatching { EduScheduleParser.parse(html) }
                }
                loading = false
                outcome.onSuccess { result ->
                    ImportLog.append(
                        context,
                        header = buildString {
                            append("教务导入成功\n")
                            append("url=").append(pageUrl).append('\n')
                            append("template=").append(result.template.name).append('\n')
                            append("courses=").append(result.timetable.courses.size).append('\n')
                            append("htmlLength=").append(html.length)
                        },
                    )
                    parsed = result
                }.onFailure { throwable ->
                    val template = EduScheduleParser.detectTemplate(html)
                    ImportLog.append(
                        context,
                        header = buildString {
                            append("教务导入失败\n")
                            append("url=").append(pageUrl).append('\n')
                            append("template=").append(template?.name ?: "UNKNOWN").append('\n')
                            append("htmlLength=").append(html.length).append('\n')
                            append("error=")
                                .append(throwable.message ?: throwable::class.java.simpleName)
                        },
                        body = html,
                    )
                    error = throwable.message ?: "解析失败"
                }
            }
        }
    }

    BackHandler(enabled = parsed != null) { parsed = null }
    BackHandler(enabled = parsed == null) {
        if (webView.canGoBack()) webView.goBack() else onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("从教务系统导入") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                parsed != null -> parsed = null
                                webView.canGoBack() -> webView.goBack()
                                else -> onCancel()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (parsed == null) {
                        TextButton(onClick = { grabPage() }, enabled = !loading) {
                            Text(if (loading) "抓取中…" else "抓取本页")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (parsed == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("教务网址") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { openUrl() }) { Text("打开") }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                )

                parsed?.let { result ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        ParsePreview(
                            result = result,
                            importing = importing,
                            overwriteCurrent = overwriteCurrent,
                            canOverwrite = currentTimetableId > 0,
                            currentTimetableName = currentTimetableName,
                            onOverwriteChange = { overwriteCurrent = it },
                            onBack = { parsed = null },
                            onImport = {
                                if (importing) return@ParsePreview
                                importing = true
                                scope.launch {
                                    val outcome = runCatching {
                                        val overwrite = overwriteCurrent &&
                                            currentTimetableId > 0
                                        if (!overwrite && termId <= 0) {
                                            throw IllegalStateException("没有有效学期")
                                        }
                                        withContext(Dispatchers.IO) {
                                            if (overwrite) {
                                                repository.importParsedTimetableInto(
                                                    timetableId = currentTimetableId,
                                                    parsed = result.timetable,
                                                )
                                            } else {
                                                repository.importParsedTimetable(
                                                    termId = termId,
                                                    sourceTimetableId = currentTimetableId
                                                        .takeIf { it > 0 },
                                                    parsed = result.timetable,
                                                )
                                            }
                                        }
                                    }
                                    outcome.onSuccess {
                                        onImported(
                                            it.timetableId,
                                            it.timetableName,
                                            overwriteCurrent && currentTimetableId > 0,
                                        )
                                    }
                                        .onFailure {
                                            importing = false
                                            error = it.message ?: "导入失败"
                                        }
                                }
                            },
                        )
                    }
                }
            }

            if (parsed == null) {
                Text(
                    text = "手动登录后进入个人课表页面，再点右上角「抓取本页」。",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text("好") }
            },
        )
    }
}

@Composable
private fun ParsePreview(
    result: EduParseResult,
    importing: Boolean,
    overwriteCurrent: Boolean,
    canOverwrite: Boolean,
    currentTimetableName: String?,
    onOverwriteChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("解析预览", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${result.template.label} · ${result.timetable.courses.size} 门课程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(result.timetable.courses) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(course.name, style = MaterialTheme.typography.titleMedium)
                        course.teacher?.let { teacher ->
                            Text(
                                text = teacher,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        course.sessions.take(4).forEach { session ->
                            Text(
                                text = buildString {
                                    append(dayLabel(session.dayOfWeek))
                                    append(' ')
                                    append(periodLabel(session.startPeriod, session.periodCount))
                                    append(" · ")
                                    append(
                                        weekRangeLabel(
                                            session.startWeek,
                                            session.endWeek,
                                            session.parity,
                                        ),
                                    )
                                    session.room?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (course.sessions.size > 4) {
                            Text(
                                text = "…共 ${course.sessions.size} 个上课时间",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (canOverwrite) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "覆盖当前课表",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "清空「${currentTimetableName ?: "当前课表"}」的课程，" +
                            "沿用它的上课时间设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = overwriteCurrent,
                    onCheckedChange = onOverwriteChange,
                    enabled = !importing,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, enabled = !importing) { Text("返回") }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onImport, enabled = !importing) {
                Text(
                    when {
                        importing -> "导入中…"
                        overwriteCurrent && canOverwrite ->
                            "覆盖导入 ${result.timetable.courses.size} 门课程"
                        else -> "导入 ${result.timetable.courses.size} 门课程"
                    },
                )
            }
        }
    }
}
