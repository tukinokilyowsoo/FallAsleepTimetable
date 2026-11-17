package io.github.fallasleeptimetable

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.fallasleeptimetable.ui.FallAsleepApp
import io.github.fallasleeptimetable.ui.screens.WelcomeScreen
import io.github.fallasleeptimetable.ui.theme.FallAsleepTheme
import io.github.fallasleeptimetable.ui.theme.ThemeMode
import io.github.fallasleeptimetable.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        WidgetUpdater.updateAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 手势提示线沉浸：去掉系统在手势条后加的对比度底色。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        setContent {
            var dynamicColor by rememberSaveable {
                mutableStateOf(prefs.getBoolean("dynamic_color", true))
            }
            var customColorArgb by rememberSaveable {
                mutableStateOf(prefs.getLong("custom_color", 0L))
            }
            var showWelcome by rememberSaveable {
                mutableStateOf(!prefs.getBoolean("welcome_seen", false))
            }
            var openEduImportAfterWelcome by rememberSaveable { mutableStateOf(false) }
            var darkMode by rememberSaveable {
                mutableStateOf(ThemeMode.get(this@MainActivity))
            }
            val darkTheme = when (darkMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            FallAsleepTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                customSeed = customColorArgb.takeIf { it != 0L }?.let { Color(it) },
            ) {
                if (showWelcome) {
                    WelcomeScreen(
                        onFinish = { startAdding ->
                            prefs.edit().putBoolean("welcome_seen", true).apply()
                            showWelcome = false
                            openEduImportAfterWelcome = startAdding
                        },
                    )
                } else {
                FallAsleepApp(
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = { enabled ->
                        dynamicColor = enabled
                        prefs.edit().putBoolean("dynamic_color", enabled).apply()
                    },
                    customColorArgb = customColorArgb.takeIf { it != 0L },
                    onCustomColorPicked = { argb ->
                        customColorArgb = argb
                        dynamicColor = false
                        prefs.edit()
                            .putLong("custom_color", argb)
                            .putBoolean("dynamic_color", false)
                            .apply()
                    },
                    darkMode = darkMode,
                    onDarkModeChange = { mode ->
                        darkMode = mode
                        ThemeMode.set(this@MainActivity, mode)
                        WidgetUpdater.updateAll(this@MainActivity)
                    },
                    onShowWelcome = {
                        prefs.edit().putBoolean("welcome_seen", false).apply()
                        showWelcome = true
                    },
                    startEduImport = openEduImportAfterWelcome,
                    onStartEduImportHandled = { openEduImportAfterWelcome = false },
                )
                }
            }
        }
    }
}
