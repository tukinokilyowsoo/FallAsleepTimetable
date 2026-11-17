package io.github.fallasleeptimetable.data.diagnostics

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 教务导入诊断日志。
 *
 * 解析失败时保存页面 HTML，用户可在 设置 → 数据 → 导出日志 里导出后反馈。
 * 日志只写应用私有目录，不会自动上传；写入前会脱敏密码、token、session 等字段。
 */
object ImportLog {
    private const val FILE_NAME = "edu-import.log"
    private const val MAX_FILE_BYTES = 4 * 1024 * 1024L
    private const val MAX_BODY_CHARS = 1_500_000
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // 教务页面可能带 CSRF token、会话 ID 或隐藏的密码字段，导出前先脱敏。
    private val keyValueSecret = Regex(
        "(?i)(password|passwd|pwd|token|access_token|refresh_token|csrf|" +
            "csrf_token|session|sessionid|jsessionid|ticket|auth)" +
            "[\"']?\\s*[:=]\\s*[\"']?[^\\s\"'&<>]+",
    )
    private val passwordInputValue = Regex(
        "(?is)(<input[^>]*type\\s*=\\s*[\"']?password[\"']?[^>]*" +
            "value\\s*=\\s*[\"'])[^\"']*",
    )

    private fun redact(text: String): String =
        text.replace(keyValueSecret, "\$1=[REDACTED]")
            .replace(passwordInputValue, "\$1[REDACTED]")

    fun append(context: Context, header: String, body: String? = null) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > MAX_FILE_BYTES) {
                val tail = file.readText().takeLast((MAX_FILE_BYTES / 2).toInt())
                file.writeText(tail)
            }
            file.appendText(
                buildString {
                    append("=== ")
                    append(LocalDateTime.now().format(formatter))
                    append(" ===\n")
                    append(redact(header.trimEnd()))
                    append('\n')
                    if (body != null) {
                        append("--- HTML BEGIN ---\n")
                        append(redact(body.take(MAX_BODY_CHARS)))
                        append("\n--- HTML END ---\n")
                    }
                    append('\n')
                },
            )
        }
    }

    fun read(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) {
            runCatching { file.readText() }.getOrDefault("（日志读取失败）")
        } else {
            "（还没有日志。请先尝试一次教务导入，失败后再导出。）"
        }
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
