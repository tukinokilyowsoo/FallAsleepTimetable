package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.SslErrorHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.fallasleeptimetable.data.AppContainer
import io.github.fallasleeptimetable.data.EduCredentialStore
import io.github.fallasleeptimetable.data.diagnostics.ImportLog
import io.github.fallasleeptimetable.data.importer.EduParseResult
import io.github.fallasleeptimetable.data.importer.EduScheduleParser
import io.github.fallasleeptimetable.ui.dayLabel
import io.github.fallasleeptimetable.ui.periodLabel
import io.github.fallasleeptimetable.ui.theme.FallAsleepTheme
import io.github.fallasleeptimetable.ui.theme.ThemeMode
import io.github.fallasleeptimetable.ui.weekRangeLabel
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener

class EduImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val termId = intent.getLongExtra(EXTRA_TERM_ID, -1L)
        val initialUrl = intent.getStringExtra(EXTRA_URL)
        val currentTimetableId = intent.getLongExtra(EXTRA_CURRENT_TIMETABLE_ID, -1L)
        val currentTimetableName = intent.getStringExtra(EXTRA_CURRENT_TIMETABLE_NAME)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val dynamicColor = prefs.getBoolean("dynamic_color", true)
        val customColor = prefs.getLong("custom_color", 0L)
        setContent {
            FallAsleepTheme(
                darkTheme = ThemeMode.isDark(this@EduImportActivity),
                dynamicColor = dynamicColor,
                customSeed = customColor.takeIf { it != 0L }?.let { Color(it) },
            ) {
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

private const val MAX_HTML_CHARS = 2_000_000

private const val GRAB_PAGE_JS = """
(function() {
  var html = '';
  var theory = null;
  function collect(doc, depth) {
    if (!doc || !doc.documentElement || theory !== null) return;
    var page = doc.location;
    if (page && page.hostname === 'jwgl.hbvtc.edu.cn' && page.pathname === '/jsxsd/xskb/xskb_list.do') {
      var table = doc.querySelector('table#timetable');
      if (table) {
        var snapshot = table.cloneNode(true);
        snapshot.querySelectorAll('input, script, style, .kbcontent1').forEach(function(node) {
          node.remove();
        });
        theory = snapshot.outerHTML.slice(0, $MAX_HTML_CHARS + 1);
        return;
      }
    }
    var remaining = $MAX_HTML_CHARS + 1 - html.length;
    if (remaining > 0) html += doc.documentElement.outerHTML.slice(0, remaining);
    if (depth >= 3) return;
    var frames = doc.querySelectorAll('iframe, frame');
    for (var i = 0; i < frames.length && theory === null; i++) {
      try {
        collect(frames[i].contentDocument, depth + 1);
      } catch (e) {}
    }
  }
  collect(document, 0);
  return theory === null ? html : theory;
})()
"""

private const val REPAIR_HBVTC_DASHBOARD_HEIGHT_JS = """
(function() {
  var frame = document.getElementById('Frame0');
  var container = document.getElementById('iframeClass');
  var styleId = 'fallasleep-hbvtc-height';
  if (!frame || !container || !document.head || document.getElementById(styleId)) return;
  var style = document.createElement('style');
  style.id = styleId;
  document.head.appendChild(style);

  function update() {
    if (!style.textContent && frame.clientHeight > 1) return;
    var height = Math.floor(window.innerHeight || document.documentElement.clientHeight);
    var available = height - Math.ceil(container.getBoundingClientRect().top);
    if (available <= 0) return;
    var css = 'html, body, .new-main {height:' + height + 'px !important}' +
        '#iframeClass {height:' + available + 'px !important}';
    if (style.textContent !== css) style.textContent = css;
  }
  function afterLayout() { requestAnimationFrame(update); }
  window.addEventListener('resize', afterLayout);
  if (window.visualViewport) window.visualViewport.addEventListener('resize', afterLayout);
  afterLayout();
  setTimeout(afterLayout, 400);
})()
"""

/** 登录页自动填充：只在可见的密码框存在时填写，且不覆盖用户已输入的内容。 */
private fun autoFillCredentialsJs(account: String, password: String): String = """
(function() {
  var account = ${JSONObject.quote(account)};
  var password = ${JSONObject.quote(password)};
  function visible(el) {
    return !!(el && (el.offsetWidth || el.offsetHeight || el.getClientRects().length));
  }
  function writable(el) {
    return el && !el.value && !el.disabled && !el.readOnly;
  }
  var pwd = null;
  var pwds = document.querySelectorAll('input[type="password"]');
  for (var i = 0; i < pwds.length; i++) {
    if (visible(pwds[i]) && writable(pwds[i])) { pwd = pwds[i]; break; }
  }
  if (!pwd) return;
  var form = pwd.form || document;
  var user = form.querySelector('#userAccount, #username, input[name="userAccount"], input[name="username"], input[name="account"]');
  if (!writable(user)) {
    user = null;
    var inputs = form.querySelectorAll('input[type="text"], input[type="tel"], input:not([type])');
    for (var j = 0; j < inputs.length; j++) {
      if (visible(inputs[j]) && writable(inputs[j])) { user = inputs[j]; break; }
    }
  }
  function setValue(el, value) {
    var proto = Object.getPrototypeOf(el);
    var desc = Object.getOwnPropertyDescriptor(proto, 'value');
    if (desc && desc.set) desc.set.call(el, value); else el.value = value;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }
  if (user) setValue(user, account);
  setValue(pwd, password);
})()
"""

/**
 * 不少教务系统（如正方 jsxsd）是 1200px 起的桌面布局，但页面自己写了
 * width=device-width 的 viewport，手机上只能看到左侧一小条，像白屏。
 * 检测到内容明显宽于屏幕时，把布局宽度改成实际内容宽度，让系统缩放整页。
 */
private const val FIT_WIDE_PAGE_JS = """
(function() {
  function fit() {
    var doc = document.documentElement;
    if (!doc) return;
    var screenWidth = window.innerWidth || 0;
    var contentWidth = Math.max(
      doc.scrollWidth || 0,
      document.body ? document.body.scrollWidth : 0
    );
    if (screenWidth <= 0 || contentWidth < 720) return;
    if (contentWidth <= screenWidth * 1.25) return;
    var meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.setAttribute('name', 'viewport');
      if (document.head) document.head.appendChild(meta);
    }
    meta.setAttribute('content', 'width=' + Math.ceil(contentWidth));
  }
  fit();
  setTimeout(fit, 300);
})()
"""

/** 桌面 Chrome UA：部分教务系统只对电脑端提供完整课表页面。 */
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

/**
 * 普通手机 Chrome UA。
 *
 * WebView 默认 UA 带 "; wv" 和 "Version/4.0"，一些教务系统会据此
 * 返回残缺页面或直接拦下主界面；换成浏览器 UA 后与系统浏览器一致。
 */
private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

/** 只允许 http/https，避免 file:、content:、javascript: 等 scheme 进入 WebView。 */
private fun normalizeWebUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val candidate = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
    val uri = Uri.parse(candidate)
    if (uri.scheme != "http" && uri.scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    return candidate
}

internal fun hbvtcScheduleUrl(pageUrl: String?): String? {
    val page = pageUrl?.let { runCatching { URI(it) }.getOrNull() } ?: return null
    if (page.scheme !in listOf("http", "https") || page.userInfo != null ||
        !"jwgl.hbvtc.edu.cn".equals(page.host, ignoreCase = true) ||
        page.rawPath !in listOf("/jsxsd/framework/xsMainV.htmlx", "/jsxsd/framework/xsMainV_new.htmlx")
    ) {
        return null
    }
    return page.resolve("/jsxsd/xskb/xskb_list.do").toString()
}

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
    var currentPageUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var overwriteCurrent by rememberSaveable { mutableStateOf(false) }
    var pendingOverwriteConfirm by remember { mutableStateOf(false) }
    var desktopUserAgent by rememberSaveable {
        mutableStateOf(prefs.getBoolean("edu_desktop_ua", false))
    }
    var rememberPassword by rememberSaveable {
        mutableStateOf(prefs.getBoolean("edu_remember_password", false))
    }
    var showCredentialDialog by remember { mutableStateOf(false) }
    // 用户输入 http 时先尝试 https，失败再退回 http；避免 http 页面里
    // 被 CSP 升级到 https 的子资源 / iframe 变成跨源，导致 CORS 或取不到内容。
    var httpFallbackUrl by remember { mutableStateOf<String?>(null) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setAllowFileAccessFromFileURLs(false)
            settings.setAllowUniversalAccessFromFileURLs(false)
            settings.saveFormData = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // 办事大厅一类门户用 window.open / target=_blank 打开教务系统。
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    currentPageUrl = url
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    currentPageUrl = url
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    httpFallbackUrl = null
                    view.evaluateJavascript(FIT_WIDE_PAGE_JS, null)
                    if (hbvtcScheduleUrl(url) != null) {
                        view.evaluateJavascript(REPAIR_HBVTC_DASHBOARD_HEIGHT_JS, null)
                    }
                    val host = runCatching { Uri.parse(url).host }.getOrNull()
                    val remember = context
                        .getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getBoolean("edu_remember_password", false)
                    if (remember && !host.isNullOrBlank()) {
                        EduCredentialStore.load(context, host)?.let { credentials ->
                            view.evaluateJavascript(
                                autoFillCredentialsJs(
                                    credentials.account,
                                    credentials.password,
                                ),
                                null,
                            )
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    val failedUrl = request.url?.toString().orEmpty()
                    if (request.isForMainFrame && failedUrl.startsWith("https://")) {
                        httpFallbackUrl?.let { fallback ->
                            httpFallbackUrl = null
                            view.loadUrl(fallback)
                            return
                        }
                    }
                    ImportLog.append(
                        context,
                        header = buildString {
                            append("WebView 加载失败\n")
                            append("frame=")
                                .append(if (request.isForMainFrame) "main" else "sub")
                                .append('\n')
                            append("url=").append(request.url).append('\n')
                            append("code=").append(error.errorCode).append('\n')
                            append("error=").append(error.description)
                        },
                    )
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    sslError: SslError,
                ) {
                    handler.cancel()
                    val fallback = httpFallbackUrl
                    if (fallback != null) {
                        httpFallbackUrl = null
                        view.loadUrl(fallback)
                    } else {
                        ImportLog.append(
                            context,
                            header = "WebView 证书错误\nurl=${sslError.url}",
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    response: WebResourceResponse,
                ) {
                    ImportLog.append(
                        context,
                        header = buildString {
                            append("WebView 返回错误\n")
                            append("frame=")
                                .append(if (request.isForMainFrame) "main" else "sub")
                                .append('\n')
                            append("url=").append(request.url).append('\n')
                            append("status=").append(response.statusCode)
                        },
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        ImportLog.append(
                            context,
                            header = buildString {
                                append("WebView 脚本错误\n")
                                append("source=").append(message.sourceId())
                                append(':').append(message.lineNumber()).append('\n')
                                append("message=").append(message.message())
                            },
                        )
                    }
                    return true
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    val transport = resultMsg.obj as? WebView.WebViewTransport
                        ?: return false
                    val popup = WebView(view.context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                popupView: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val target = request.url?.toString() ?: return false
                                if (target.startsWith("http://") ||
                                    target.startsWith("https://")
                                ) {
                                    view.loadUrl(target)
                                    popupView.destroy()
                                    return true
                                }
                                return false
                            }
                        }
                    }
                    transport.webView = popup
                    resultMsg.sendToTarget()
                    return true
                }
            }
        }
    }

    fun loadPreferred(target: String) {
        val httpsUrl = if (target.startsWith("http://")) {
            "https://" + target.removePrefix("http://")
        } else {
            null
        }
        httpFallbackUrl = httpsUrl?.let { target }
        webView.loadUrl(httpsUrl ?: target)
    }

    LaunchedEffect(Unit) {
        webView.settings.userAgentString =
            if (desktopUserAgent) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        normalizeWebUrl(url)?.let(::loadPreferred)
    }

    DisposableEffect(Unit) {
        onDispose {
            // 教务登录只保留在当前会话里，退出导入页时清掉 Cookie。
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            webView.destroy()
        }
    }

    fun openUrl() {
        val input = url.trim()
        if (input.isEmpty()) {
            error = "请输入教务网址"
            return
        }
        val normalized = normalizeWebUrl(input)
        if (normalized == null) {
            error = "请输入有效的 http/https 教务网址"
            return
        }
        url = normalized
        prefs.edit().putString("edu_url", normalized).apply()
        loadPreferred(normalized)
    }

    fun currentHost(): String? {
        val target = webView.url ?: normalizeWebUrl(url)
        return runCatching { target?.let { Uri.parse(it).host } }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun setRememberPassword(enabled: Boolean) {
        if (!enabled) {
            rememberPassword = false
            prefs.edit().putBoolean("edu_remember_password", false).apply()
            EduCredentialStore.clear(context)
            return
        }
        val host = currentHost()
        if (host == null) {
            error = "请先打开教务网址，再保存密码"
            return
        }
        if (EduCredentialStore.load(context, host) != null) {
            rememberPassword = true
            prefs.edit().putBoolean("edu_remember_password", true).apply()
        } else {
            showCredentialDialog = true
        }
    }

    fun setDesktopUserAgent(enabled: Boolean) {
        desktopUserAgent = enabled
        prefs.edit().putBoolean("edu_desktop_ua", enabled).apply()
        webView.settings.userAgentString =
            if (enabled) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        error = null
        // 重新加载一次，让教务系统按新 UA 返回对应布局。
        if (webView.url != null) webView.reload()
    }

    fun grabPage() {
        if (loading) return
        loading = true
        val pageUrl = webView.url ?: url
        webView.evaluateJavascript(GRAB_PAGE_JS) { result ->
            val html = runCatching {
                JSONTokener(result ?: "null").nextValue() as? String
            }.getOrNull()
            if (html != null && html.length > MAX_HTML_CHARS) {
                loading = false
                error = "页面内容过大，无法解析"
                return@evaluateJavascript
            }
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

    fun startImport(result: EduParseResult) {
        if (importing) return
        importing = true
        val overwrite = overwriteCurrent && currentTimetableId > 0
        scope.launch {
            val outcome = runCatching {
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
                            sourceTimetableId = currentTimetableId.takeIf { it > 0 },
                            parsed = result.timetable,
                        )
                    }
                }
            }
            outcome.onSuccess {
                onImported(it.timetableId, it.timetableName, overwrite)
            }.onFailure {
                importing = false
                error = it.message ?: "导入失败"
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
                        TextButton(onClick = { setDesktopUserAgent(!desktopUserAgent) }) {
                            Text(if (desktopUserAgent) "手机版" else "电脑版")
                        }
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

                hbvtcScheduleUrl(currentPageUrl)?.let { scheduleUrl ->
                    TextButton(
                        onClick = {
                            httpFallbackUrl = null
                            webView.loadUrl(scheduleUrl)
                        },
                        enabled = !loading,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text("出现登陆后白屏问题？可能出现请求跨域拦截，点此打开学期理论课表")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "记住密码",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = if (rememberPassword) {
                                "已保存在本机，登录页会自动填入"
                            } else {
                                "仅保存在本机，使用系统密钥加密"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (rememberPassword) {
                        TextButton(
                            onClick = {
                                if (currentHost() == null) {
                                    error = "请先打开教务网址，再保存密码"
                                } else {
                                    showCredentialDialog = true
                                }
                            },
                        ) { Text("修改") }
                    }
                    Switch(
                        checked = rememberPassword,
                        onCheckedChange = { setRememberPassword(it) },
                    )
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
                                if (overwriteCurrent && currentTimetableId > 0) {
                                    pendingOverwriteConfirm = true
                                } else {
                                    startImport(result)
                                }
                            },
                        )
                    }
                }
            }

            if (parsed == null) {
                Text(
                    text = "手动登录后进入个人课表页面，再点右上角「抓取本页」。页面显示不全时，可切换「电脑版 / 手机版」或双指放大。",
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

    if (showCredentialDialog) {
        val host = currentHost().orEmpty()
        CredentialDialog(
            host = host,
            initial = host.takeIf { it.isNotBlank() }
                ?.let { EduCredentialStore.load(context, it) },
            onDismiss = { showCredentialDialog = false },
            onSave = { account, password ->
                val targetHost = currentHost()
                when {
                    targetHost == null -> {
                        showCredentialDialog = false
                        error = "请先打开教务网址，再保存密码"
                    }

                    EduCredentialStore.save(
                        context,
                        targetHost,
                        EduCredentialStore.Credentials(account, password),
                    ) -> {
                        rememberPassword = true
                        prefs.edit().putBoolean("edu_remember_password", true).apply()
                        showCredentialDialog = false
                        webView.evaluateJavascript(
                            autoFillCredentialsJs(account, password),
                            null,
                        )
                    }

                    else -> error = "保存密码失败，请重试"
                }
            },
        )
    }

    if (pendingOverwriteConfirm) {
        AlertDialog(
            onDismissRequest = { pendingOverwriteConfirm = false },
            title = { Text("覆盖当前课表？") },
            text = {
                Text(
                    "将清空「${currentTimetableName ?: "当前课表"}」的现有课程，" +
                        "并写入本次导入的课程。此操作无法撤销，上课时间设置会保留。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingOverwriteConfirm = false
                        parsed?.let { startImport(it) }
                    },
                ) { Text("覆盖导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwriteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CredentialDialog(
    host: String,
    initial: EduCredentialStore.Credentials?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var account by remember { mutableStateOf(initial?.account.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记住密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (host.isBlank()) {
                        "登录信息仅保存在本机，使用系统密钥加密。"
                    } else {
                        "为 $host 保存登录信息，下次打开登录页会自动填入。" +
                            "仅保存在本机，使用系统密钥加密。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(account.trim(), password) },
                enabled = account.isNotBlank() && password.isNotEmpty(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
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
