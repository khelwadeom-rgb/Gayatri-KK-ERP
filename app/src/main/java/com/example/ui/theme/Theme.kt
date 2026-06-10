package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Premium Enterprise Light Color Scheme for Gayatri Krushi Kendra ERP
 */
private val LightColorScheme = lightColorScheme(
    primary = GKKPrimary,
    onPrimary = PureWhite,
    primaryContainer = GKKCream,
    onPrimaryContainer = GKKPrimary,
    
    secondary = GKKEarthBrown,
    onSecondary = PureWhite,
    secondaryContainer = Gray100,
    onSecondaryContainer = Charcoal900,
    
    tertiary = GKKAccent,
    onTertiary = Charcoal950,
    tertiaryContainer = SoftOrange100,
    onTertiaryContainer = DeepGreen900,
    
    error = ErrorRed600,
    onError = PureWhite,
    errorContainer = ErrorRed100,
    onErrorContainer = Charcoal950,
    
    background = PureWhite,
    onBackground = Charcoal900,
    
    surface = PureWhite,
    onSurface = Charcoal900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    
    outline = Gray400,
    inverseOnSurface = SoftWhite,
    inverseSurface = Charcoal900,
    inversePrimary = Emerald500
)

/**
 * Premium Enterprise Dark Color Scheme for Gayatri Krushi Kendra ERP
 */
private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Charcoal950,
    primaryContainer = DeepGreen800,
    onPrimaryContainer = Emerald100,
    
    secondary = Gray400,
    onSecondary = Charcoal950,
    secondaryContainer = DarkSurfaceLighter,
    onSecondaryContainer = SoftWhite,
    
    tertiary = SoftOrange500,
    onTertiary = Charcoal950,
    tertiaryContainer = DarkSurface,
    onTertiaryContainer = SoftOrange100,
    
    error = ErrorRed600,
    onError = PureWhite,
    errorContainer = DarkSurface,
    onErrorContainer = ErrorRed100,
    
    background = DarkBg,
    onBackground = DarkTextPrimary,
    
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceLighter,
    onSurfaceVariant = DarkTextSecondary,
    
    outline = Gray600,
    inverseOnSurface = DarkBg,
    inverseSurface = DarkTextPrimary,
    inversePrimary = DeepGreen800
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Set to false by default for brand consistency in enterprise apps
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
        typography = getResponsiveMaterialTypography(),
        content = content
    )
}
