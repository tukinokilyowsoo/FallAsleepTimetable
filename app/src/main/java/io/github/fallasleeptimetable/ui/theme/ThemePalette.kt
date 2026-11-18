package io.github.fallasleeptimetable.ui.theme

/** 应用默认主题种子色（蓝灰），也用于未自定义时的整套 M3 配色。 */
const val DEFAULT_THEME_SEED: Long = 0xFF4A5C92L

/** 一组 Material 调色板预设：名称 + 种子色（ARGB）。 */
data class ThemePalettePreset(
    val name: String,
    val seed: Long,
)

/**
 * 取自 Android / Material Theme Builder 常用调色板的种子色。
 * 应用会用 [colorSchemeFromSeed] 从种子生成整套浅色/深色语义色。
 */
val androidThemePalettes: List<ThemePalettePreset> = listOf(
    ThemePalettePreset("紫罗兰", 0xFF6750A4),
    ThemePalettePreset("靛蓝", 0xFF3F5F90),
    ThemePalettePreset("天蓝", 0xFF0061A4),
    ThemePalettePreset("青色", 0xFF006A6A),
    ThemePalettePreset("绿色", 0xFF4E6A00),
    ThemePalettePreset("橄榄", 0xFF6A5F00),
    ThemePalettePreset("橙色", 0xFF8B5000),
    ThemePalettePreset("红色", 0xFFB3261E),
    ThemePalettePreset("粉色", 0xFF984061),
    ThemePalettePreset("玫瑰", 0xFF7D5260),
    ThemePalettePreset("棕色", 0xFF765B4E),
    ThemePalettePreset("蓝灰", 0xFF4A5C92),
)
