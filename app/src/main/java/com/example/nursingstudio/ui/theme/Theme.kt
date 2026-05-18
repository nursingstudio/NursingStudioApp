package com.example.nursingstudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandBlueCore,
    onPrimary = Color.White,
    secondary = BrandSaffronCore,
    onSecondary = Color.White,
    tertiary = MedicalTealAccent,
    error = AlertErrorRed,
    background = NeutralBgOffWhite,
    onBackground = DarkTextPrimary,
    surface = SurfaceCardClean,
    onSurface = DarkTextPrimary,
    surfaceVariant = SurfaceVariantMuted,
    onSurfaceVariant = SlateTextSecondary
)

@Composable
fun NursingStudioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // ✅ 2026 Industry Gold Standard Fix (No Deprecation Warnings):
            // Edge-to-edge friendly WindowCompat layer implementation
            window.decorView.setBackgroundColor(NeutralBgOffWhite.toArgb())

            val insetsController = WindowCompat.getInsetsController(window, view)
            // System level safety initialization
            insetsController.isAppearanceLightStatusBars = true // Black icons over soft off-white surface
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography, // Linked flawlessly from Type.kt
        content = content
    )
}