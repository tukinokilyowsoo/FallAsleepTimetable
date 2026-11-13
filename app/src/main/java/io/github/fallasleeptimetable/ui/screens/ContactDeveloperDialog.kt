package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fallasleeptimetable.R

private const val BILIBILI_URL = "https://b23.tv/VZ6GPhy"
private const val XIAOHONGSHU_URL = "https://xhslink.cn/o/7qKpW27kqzt"
private const val X_URL = "https://x.com/Lyowsoo"
private const val ISSUE_URL =
    "https://github.com/tukinokilyowsoo/FallAsleepTimetable/issues/new"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactDeveloperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("联系开发者") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.profile_avatar),
                    contentDescription = "槻ノ木 響子",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "槻ノ木 響子",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Lyowsoo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = { openUrl(context, BILIBILI_URL) },
                        label = { Text("哔哩哔哩") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_bilibili),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    AssistChip(
                        onClick = { openUrl(context, XIAOHONGSHU_URL) },
                        label = { Text("小红书") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_xiaohongshu),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    AssistChip(
                        onClick = { openUrl(context, X_URL) },
                        label = { Text("X") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_x),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "报告问题请前往 GitHub 提交 Issue，或在小红书主页留言。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { openUrl(context, ISSUE_URL) }) {
                    Text("提交 Issue")
                }
                Text(
                    text = "© 2026 槻ノ木 響子 · Lyowsoo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
