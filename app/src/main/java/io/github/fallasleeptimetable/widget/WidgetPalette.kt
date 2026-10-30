package io.github.fallasleeptimetable.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color

data class WidgetPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val secondaryText: Color,
    val outline: Color,
    val containers: List<Color>,
    val onContainers: List<Color>,
    val mutedContainer: Color,
    val mutedText: Color,
)

fun widgetPalette(context: Context): WidgetPalette {
    val dark = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    return if (!dark) {
        WidgetPalette(
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFF3EDF7),
            text = Color(0xFF1B1B21),
            secondaryText = Color(0xFF45464F),
            outline = Color(0xFFCAC4D0),
            containers = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD7E3FF),
                Color(0xFFFFD8E4),
                Color(0xFFFFE0B2),
                Color(0xFFD7E8CC),
                Color(0xFFCFE5F5),
            ),
            onContainers = listOf(
                Color(0xFF21005D),
                Color(0xFF001B3D),
                Color(0xFF31111D),
                Color(0xFF2B1700),
                Color(0xFF102010),
                Color(0xFF001E2C),
            ),
            mutedContainer = Color(0xFFE3E3E8),
            mutedText = Color(0xFF6F7078),
        )
    } else {
        WidgetPalette(
            background = Color(0xFF1B1B21),
            surface = Color(0xFF26232B),
            text = Color(0xFFE6E1E9),
            secondaryText = Color(0xFFC9C5D0),
            outline = Color(0xFF49454F),
            containers = listOf(
                Color(0xFF4F378B),
                Color(0xFF2F4677),
                Color(0xFF633B48),
                Color(0xFF5A3F00),
                Color(0xFF3A4E30),
                Color(0xFF27475A),
            ),
            onContainers = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD7E3FF),
                Color(0xFFFFD8E4),
                Color(0xFFFFE0B2),
                Color(0xFFD7E8CC),
                Color(0xFFCFE5F5),
            ),
            mutedContainer = Color(0xFF34323A),
            mutedText = Color(0xFF9A98A2),
        )
    }
}
