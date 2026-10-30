package io.github.fallasleeptimetable.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class CourseColors(val container: Color, val content: Color)

@Composable
fun courseColors(index: Int): CourseColors {
    val scheme = MaterialTheme.colorScheme
    return when (((index % 6) + 6) % 6) {
        0 -> CourseColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        1 -> CourseColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        2 -> CourseColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        3 -> CourseColors(scheme.surfaceContainerHighest, scheme.onSurface)
        4 -> CourseColors(scheme.primary, scheme.onPrimary)
        else -> CourseColors(scheme.tertiary, scheme.onTertiary)
    }
}

@Composable
fun courseColorAt(index: Int): Color = courseColors(index).container
