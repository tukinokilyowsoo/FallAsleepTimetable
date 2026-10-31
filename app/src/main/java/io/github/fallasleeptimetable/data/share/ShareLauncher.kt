package io.github.fallasleeptimetable.data.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareLauncher {
    /** 把 PNG 写入缓存目录并通过系统分享面板发送。 */
    fun sharePng(context: Context, png: ByteArray, fileName: String) {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        directory.listFiles()?.forEach { old ->
            if (old.isFile && old.name != fileName) old.delete()
        }
        val file = File(directory, fileName)
        file.writeBytes(png)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FallAsleep 课程表")
            clipData = ClipData.newRawUri("课表长图", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "分享课表")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
