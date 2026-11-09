package io.github.demiterhugo.weatherflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9BFFF),
    onPrimary = Color(0xFF102550),
    primaryContainer = Color(0xFF24344E),
    onPrimaryContainer = Color(0xFFECF2FC),
    background = Color(0xFF101827),
    onBackground = Color(0xFFECF2FC),
    surface = Color(0xFF1B2638),
    onSurface = Color(0xFFECF2FC),
    surfaceVariant = Color(0xFF26354B),
    onSurfaceVariant = Color(0xFFB2C0D4),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF315DE4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF0FF),
    onPrimaryContainer = Color(0xFF16263E),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF16263E),
    surface = Color.White,
    onSurface = Color(0xFF16263E),
    surfaceVariant = Color(0xFFEDF1FA),
    onSurfaceVariant = Color(0xFF4B6079),
)

@Composable
fun WeatherFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}