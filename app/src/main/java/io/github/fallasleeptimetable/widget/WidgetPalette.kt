package io.github.fallasleeptimetable.widget

import android.content.Context
import android.content.res.Configuration

data class WidgetPalette(
    val background: Int,
    val surface: Int,
    val text: Int,
    val secondaryText: Int,
    val outline: Int,
    val containers: List<Int>,
    val onContainers: List<Int>,
    val mutedContainer: Int,
    val mutedText: Int,
)

fun widgetPalette(context: Context): WidgetPalette {
    val dark = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    return if (!dark) {
        WidgetPalette(
            background = 0xFFFFFBFE.toInt(),
            surface = 0xFFF3EDF7.toInt(),
            text = 0xFF1B1B21.toInt(),
            secondaryText = 0xFF45464F.toInt(),
            outline = 0xFFCAC4D0.toInt(),
            containers = listOf(
                0xFFEADDFF.toInt(),
                0xFFD7E3FF.toInt(),
                0xFFFFD8E4.toInt(),
                0xFFFFE0B2.toInt(),
                0xFFD7E8CC.toInt(),
                0xFFCFE5F5.toInt(),
            ),
            onContainers = listOf(
                0xFF21005D.toInt(),
                0xFF001B3D.toInt(),
                0xFF31111D.toInt(),
                0xFF2B1700.toInt(),
                0xFF102010.toInt(),
                0xFF001E2C.toInt(),
            ),
            mutedContainer = 0xFFE3E3E8.toInt(),
            mutedText = 0xFF6F7078.toInt(),
        )
    } else {
        WidgetPalette(
            background = 0xFF1B1B21.toInt(),
            surface = 0xFF26232B.toInt(),
            text = 0xFFE6E1E9.toInt(),
            secondaryText = 0xFFC9C5D0.toInt(),
            outline = 0xFF49454F.toInt(),
            containers = listOf(
                0xFF4F378B.toInt(),
                0xFF2F4677.toInt(),
                0xFF633B48.toInt(),
                0xFF5A3F00.toInt(),
                0xFF3A4E30.toInt(),
                0xFF27475A.toInt(),
            ),
            onContainers = listOf(
                0xFFEADDFF.toInt(),
                0xFFD7E3FF.toInt(),
                0xFFFFD8E4.toInt(),
                0xFFFFE0B2.toInt(),
                0xFFD7E8CC.toInt(),
                0xFFCFE5F5.toInt(),
            ),
            mutedContainer = 0xFF34323A.toInt(),
            mutedText = 0xFF9A98A2.toInt(),
        )
    }
}
