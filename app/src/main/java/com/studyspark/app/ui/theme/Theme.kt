package com.studyspark.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SparkPurple,
    onPrimary = Color.White,
    primaryContainer = SparkPurpleLight,
    onPrimaryContainer = SparkPurpleDark,
    secondary = SparkCoral,
    onSecondary = Color.White,
    secondaryContainer = SparkCoralLight,
    onSecondaryContainer = Color(0xFF8B0000),
    tertiary = SparkMint,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant
)

private val DarkColors = darkColorScheme(
    primary = SparkPurpleLight,
    onPrimary = SparkPurpleDark,
    primaryContainer = SparkPurpleDark,
    onPrimaryContainer = SparkPurpleLight,
    secondary = SparkCoral,
    onSecondary = Color.White,
    tertiary = SparkMint,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant
)

@Composable
fun StudySparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// Helper to avoid import error inside this file
private val Color = androidx.compose.ui.graphics.Color
