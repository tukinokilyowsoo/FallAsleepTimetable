package io.github.fallasleeptimetable.data.update

import io.github.fallasleeptimetable.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateSource(val key: String, val label: String) {
    GITHUB("github", "GitHub"),
    MIRROR("mirror", "国内镜像");

    companion object {
        fun fromKey(key: String?): UpdateSource =
            entries.firstOrNull { it.key == key } ?: GITHUB
    }
}

data class UpdateInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    val downloadUrl: String?,
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
}

object UpdateChecker {
    private const val REPO = "tukinokilyowsoo/FallAsleepTimetable"
    private const val API_URL =
        "https://api.github.com/repos/$REPO/releases?per_page=10"
    private const val MIRROR_PREFIX = "https://gh-proxy.com/"

    suspend fun check(
        source: UpdateSource,
        currentVersion: String = BuildConfig.VERSION_NAME,
        includePrerelease: Boolean = true,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = when (source) {
            UpdateSource.GITHUB -> API_URL
            UpdateSource.MIRROR -> MIRROR_PREFIX + API_URL
        }
        val body = httpGet(url)
        val releases = JSONArray(body)
        val release = (0 until releases.length())
            .mapNotNull { releases.optJSONObject(it) }
            .firstOrNull { release ->
                !release.optBoolean("draft", false) &&
                    (includePrerelease || !release.optBoolean("prerelease", false))
            }
            ?: throw IOException("没有找到可用的 Release")

        val tag = release.optString("tag_name").trim()
            .ifBlank { release.optString("name").trim() }
            .ifBlank { "未知版本" }
        val pageUrl = release.optString("html_url")
        val notes = release.optString("body").trim()
        val name = release.optString("name").trim().ifBlank { tag }

        val assets = release.optJSONArray("assets") ?: JSONArray()
        val apk = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { asset ->
                val fileName = asset.optString("name")
                fileName.endsWith(".apk", ignoreCase = true) &&
                    fileName.contains("arm64", ignoreCase = true)
            }
            ?: (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .firstOrNull {
                    it.optString("name").endsWith(".apk", ignoreCase = true)
                }

        val rawDownload = apk?.optString("browser_download_url")
            ?.takeIf { it.isNotBlank() }
        val downloadUrl = rawDownload?.let {
            if (source == UpdateSource.MIRROR) MIRROR_PREFIX + it else it
        }

        val info = UpdateInfo(
            tag = tag,
            name = name,
            notes = notes,
            pageUrl = pageUrl,
            downloadUrl = downloadUrl,
        )
        if (isNewer(tag, currentVersion)) {
            UpdateCheckResult.Available(info)
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = versionNumbers(remote)
        val localParts = versionNumbers(local)
        val size = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until size) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        // 主版本相同再比较预览标记：beta2 > beta1，正式版 > 同号 beta。
        val remotePre = prerelease(remote)
        val localPre = prerelease(local)
        if (remotePre == null && localPre == null) return false
        if (remotePre == null) return true
        if (localPre == null) return false
        return comparePrerelease(remotePre, localPre) > 0
    }

    private data class PreRelease(val label: String, val number: Int)

    private fun prerelease(version: String): PreRelease? {
        val normalized = version.trim().removePrefix("v").removePrefix("V")
        val suffix = normalized.substringAfter('-', "").substringBefore('+')
        if (suffix.isBlank()) return null
        val number = suffix.takeLastWhile { it.isDigit() }.toIntOrNull() ?: 0
        val label = suffix.dropLastWhile { it.isDigit() }
        return PreRelease(label, number)
    }

    private fun comparePrerelease(a: PreRelease, b: PreRelease): Int {
        val labelCompare = a.label.compareTo(b.label)
        return if (labelCompare != 0) labelCompare else a.number.compareTo(b.number)
    }

    private fun versionNumbers(version: String): List<Int> {
        val core = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        return core.split('.').mapNotNull { it.toIntOrNull() }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty(
                "User-Agent",
                "FallAsleep/${BuildConfig.VERSION_NAME}",
            )
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("服务器返回 HTTP $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
