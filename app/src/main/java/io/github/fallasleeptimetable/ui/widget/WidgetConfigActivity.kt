package io.github.fallasleeptimetable.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.fallasleeptimetable.data.AppDatabase
import io.github.fallasleeptimetable.data.TimetableEntity
import io.github.fallasleeptimetable.ui.theme.FallAsleepTheme
import io.github.fallasleeptimetable.widget.WidgetConfig
import io.github.fallasleeptimetable.widget.WidgetConfigStore
import io.github.fallasleeptimetable.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 桌面小组件添加/重新配置时打开的设置页。
 *
 * 配置按小组件实例保存：选择课表、是否显示教师/教室、今日组件是否显示后两天。
 */
open class WidgetConfigActivity : ComponentActivity() {
    protected open val widgetKind: String = WIDGET_KIND_TODAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        val initial = WidgetConfigStore.load(this, appWidgetId)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val dynamicColor = prefs.getBoolean("dynamic_color", true)
        val customColor = prefs.getLong("custom_color", 0L)

        setContent {
            FallAsleepTheme(
                dynamicColor = dynamicColor,
                customSeed = customColor.takeIf { it != 0L }?.let { Color(it) },
            ) {
                var timetables by remember { mutableStateOf<List<TimetableEntity>?>(null) }
                LaunchedEffect(Unit) {
                    timetables = withContext(Dispatchers.IO) {
                        AppDatabase.get(applicationContext).timetableDao().getAll()
                    }
                }

                val tables = timetables
                if (tables == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    WidgetConfigScreen(
                        kind = widgetKind,
                        timetables = tables,
                        initial = initial,
                        onCancel = { finish() },
                        onSave = { config ->
                            WidgetConfigStore.save(this, appWidgetId, config)
                            WidgetUpdater.updateAll(this)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId,
                                ),
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val WIDGET_KIND_TODAY = "today"
        const val WIDGET_KIND_WEEK = "week"
    }
}

class TodayWidgetConfigActivity : WidgetConfigActivity() {
    override val widgetKind: String = WidgetConfigActivity.WIDGET_KIND_TODAY
}

class WeekWidgetConfigActivity : WidgetConfigActivity() {
    override val widgetKind: String = WidgetConfigActivity.WIDGET_KIND_WEEK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    kind: String,
    timetables: List<TimetableEntity>,
    initial: WidgetConfig,
    onCancel: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    val isToday = kind == WidgetConfigActivity.WIDGET_KIND_TODAY
    var timetableId by remember {
        mutableStateOf(
            initial.timetableId?.takeIf { id -> timetables.any { it.id == id } },
        )
    }
    var showTeacher by remember { mutableStateOf(initial.showTeacher) }
    var showRoom by remember { mutableStateOf(initial.showRoom) }
    var showUpcomingDays by remember { mutableStateOf(initial.showUpcomingDays) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("小组件设置") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        text = "选择课表",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("跟随应用当前课表") },
                        supportingContent = { Text("在应用里切换课表时同步变化") },
                        leadingContent = {
                            RadioButton(
                                selected = timetableId == null,
                                onClick = { timetableId = null },
                            )
                        },
                        modifier = Modifier.clickable { timetableId = null },
                    )
                }
                items(timetables, key = { it.id }) { table ->
                    ListItem(
                        headlineContent = { Text(table.name) },
                        leadingContent = {
                            RadioButton(
                                selected = timetableId == table.id,
                                onClick = { timetableId = table.id },
                            )
                        },
                        modifier = Modifier.clickable { timetableId = table.id },
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item {
                    Text(
                        text = "显示内容",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                item {
                    SwitchRow(
                        label = "显示教室",
                        description = if (isToday) {
                            "课程卡片中的地点"
                        } else {
                            "周课表课程格中的地点"
                        },
                        checked = showRoom,
                        onCheckedChange = { showRoom = it },
                    )
                }
                if (isToday) {
                    item {
                        SwitchRow(
                            label = "显示教师",
                            description = "课程卡片中的教师",
                            checked = showTeacher,
                            onCheckedChange = { showTeacher = it },
                        )
                    }
                    item {
                        SwitchRow(
                            label = "显示明天/后天",
                            description = "组件够宽时并排显示后两天的课程",
                            checked = showUpcomingDays,
                            onCheckedChange = { showUpcomingDays = it },
                        )
                    }
                }
                item {
                    Text(
                        text = "每个小组件可以单独设置；删除小组件时设置会一并清除。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onSave(
                            WidgetConfig(
                                timetableId = timetableId,
                                showTeacher = showTeacher,
                                showRoom = showRoom,
                                showUpcomingDays = showUpcomingDays,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
