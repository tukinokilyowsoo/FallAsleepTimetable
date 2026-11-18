package io.github.fallasleeptimetable.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun FallAsleepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customSeed: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // 默认色与自定义色走同一套 M3 生成逻辑，保证表面层级与主色同源。
        customSeed != null -> colorSchemeFromSeed(customSeed, darkTheme)
        else -> colorSchemeFromSeed(Color(DEFAULT_THEME_SEED.toInt()), darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
