package com.example.nursingstudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Saffron,
    secondary = SaffronDark,
    tertiary = SaffronLight,
    background = AppBackground
)

@Composable
fun NursingStudioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 2026 Modern Way: Property use karein, setter method nahi
            @Suppress("DEPRECATION")
            window.statusBarColor = Saffron.toArgb()

            // Icons brightness control
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography, // Linked the renamed variable
        content = content
    )
}