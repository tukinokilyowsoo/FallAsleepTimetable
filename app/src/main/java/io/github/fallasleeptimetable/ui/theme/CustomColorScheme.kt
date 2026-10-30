package io.github.fallasleeptimetable.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * 从用户选定的种子色生成一套近似的 Material 3 配色。
 *
 * 没有引入 Material Color Utilities，用 HSV 推导主色/容器色/表面色，
 * 保证浅色/深色下对比度可用，风格与 M3 接近。
 */
fun colorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]
    val seedSaturation = hsv[1].coerceIn(0.20f, 1f)

    fun color(h: Float, s: Float, v: Float): Color = Color(
        AndroidColor.HSVToColor(
            floatArrayOf(
                ((h % 360f) + 360f) % 360f,
                s.coerceIn(0f, 1f),
                v.coerceIn(0f, 1f),
            ),
        ),
    )

    fun on(bg: Color): Color =
        if (bg.luminance() > 0.5f) color(hue, 0.25f, 0.15f) else Color.White

    val secondaryHue = hue + 20f
    val tertiaryHue = hue + 60f

    return if (!dark) {
        val primary = color(hue, maxOf(0.45f, seedSaturation * 0.85f), 0.55f)
        val secondary = color(secondaryHue, 0.25f, 0.45f)
        val tertiary = color(tertiaryHue, 0.30f, 0.45f)

        lightColorScheme(
            primary = primary,
            onPrimary = on(primary),
            primaryContainer = color(hue, 0.30f, 0.92f),
            onPrimaryContainer = color(hue, 0.75f, 0.18f),
            secondary = secondary,
            onSecondary = on(secondary),
            secondaryContainer = color(secondaryHue, 0.22f, 0.93f),
            onSecondaryContainer = color(secondaryHue, 0.60f, 0.20f),
            tertiary = tertiary,
            onTertiary = on(tertiary),
            tertiaryContainer = color(tertiaryHue, 0.28f, 0.92f),
            onTertiaryContainer = color(tertiaryHue, 0.60f, 0.20f),
            background = color(hue, 0.04f, 0.99f),
            onBackground = color(hue, 0.15f, 0.10f),
            surface = color(hue, 0.04f, 0.99f),
            onSurface = color(hue, 0.15f, 0.10f),
            surfaceVariant = color(hue, 0.12f, 0.90f),
            onSurfaceVariant = color(hue, 0.12f, 0.28f),
            surfaceContainerLowest = color(hue, 0.03f, 0.995f),
            surfaceContainerLow = color(hue, 0.05f, 0.975f),
            surfaceContainer = color(hue, 0.06f, 0.95f),
            surfaceContainerHigh = color(hue, 0.07f, 0.93f),
            surfaceContainerHighest = color(hue, 0.08f, 0.91f),
            surfaceDim = color(hue, 0.08f, 0.88f),
            surfaceBright = color(hue, 0.03f, 0.995f),
            outline = color(hue, 0.08f, 0.55f),
            outlineVariant = color(hue, 0.08f, 0.80f),
        )
    } else {
        val primary = color(hue, maxOf(0.35f, seedSaturation * 0.60f), 0.92f)
        val secondary = color(secondaryHue, 0.25f, 0.85f)
        val tertiary = color(tertiaryHue, 0.30f, 0.88f)

        darkColorScheme(
            primary = primary,
            onPrimary = color(hue, 0.50f, 0.15f),
            primaryContainer = color(hue, 0.45f, 0.32f),
            onPrimaryContainer = color(hue, 0.30f, 0.92f),
            secondary = secondary,
            onSecondary = color(secondaryHue, 0.45f, 0.15f),
            secondaryContainer = color(secondaryHue, 0.30f, 0.30f),
            onSecondaryContainer = color(secondaryHue, 0.25f, 0.90f),
            tertiary = tertiary,
            onTertiary = color(tertiaryHue, 0.45f, 0.15f),
            tertiaryContainer = color(tertiaryHue, 0.30f, 0.30f),
            onTertiaryContainer = color(tertiaryHue, 0.25f, 0.90f),
            background = color(hue, 0.10f, 0.06f),
            onBackground = color(hue, 0.15f, 0.92f),
            surface = color(hue, 0.10f, 0.07f),
            onSurface = color(hue, 0.15f, 0.92f),
            surfaceVariant = color(hue, 0.12f, 0.26f),
            onSurfaceVariant = color(hue, 0.10f, 0.80f),
            surfaceContainerLowest = color(hue, 0.10f, 0.04f),
            surfaceContainerLow = color(hue, 0.10f, 0.10f),
            surfaceContainer = color(hue, 0.10f, 0.14f),
            surfaceContainerHigh = color(hue, 0.10f, 0.18f),
            surfaceContainerHighest = color(hue, 0.10f, 0.22f),
            surfaceDim = color(hue, 0.10f, 0.08f),
            surfaceBright = color(hue, 0.10f, 0.30f),
            outline = color(hue, 0.12f, 0.60f),
            outlineVariant = color(hue, 0.12f, 0.35f),
        )
    }
}
