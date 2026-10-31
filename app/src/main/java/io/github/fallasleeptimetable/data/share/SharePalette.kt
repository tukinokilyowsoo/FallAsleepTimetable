package io.github.fallasleeptimetable.data.share

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

/** 分享长图用的一组 ARGB 颜色，取色规则与课表网格一致。 */
data class SharePalette(
    val background: Int,
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val outlineVariant: Int,
    val primary: Int,
    val courseContainers: List<Int>,
    val courseContents: List<Int>,
) {
    fun container(index: Int): Int =
        courseContainers[((index % 6) + 6) % 6]

    fun content(index: Int): Int =
        courseContents[((index % 6) + 6) % 6]

    companion object {
        fun from(scheme: ColorScheme): SharePalette = SharePalette(
            background = scheme.background.toArgb(),
            surface = scheme.surfaceContainerLow.toArgb(),
            onSurface = scheme.onSurface.toArgb(),
            onSurfaceVariant = scheme.onSurfaceVariant.toArgb(),
            outlineVariant = scheme.outlineVariant.toArgb(),
            primary = scheme.primary.toArgb(),
            courseContainers = listOf(
                scheme.primaryContainer,
                scheme.secondaryContainer,
                scheme.tertiaryContainer,
                scheme.surfaceContainerHighest,
                scheme.primary,
                scheme.tertiary,
            ).map { it.toArgb() },
            courseContents = listOf(
                scheme.onPrimaryContainer,
                scheme.onSecondaryContainer,
                scheme.onTertiaryContainer,
                scheme.onSurface,
                scheme.onPrimary,
                scheme.onTertiary,
            ).map { it.toArgb() },
        )
    }
}
