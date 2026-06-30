package com.example.digitaluniversity.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryPurple = Color(0xFF7B39FD)
val BgGray = Color(0xFFF8F9FB)
val SurfaceGray = Color(0xFFF1F4F8)
val TextGray = Color(0xFF757575)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    background = BgGray,
    surface = SurfaceGray
)

@Composable
fun DigitalUniversityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}