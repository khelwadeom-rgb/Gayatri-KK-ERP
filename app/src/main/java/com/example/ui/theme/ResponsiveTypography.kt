package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Responsive Typography System for Gayatri Krushi Kendra ERP.
 * Automatically scales font sizes based on screen width.
 */
data class AppTypography(
    val displayLarge: TextUnit = 28.sp,
    val displayMedium: TextUnit = 24.sp,
    val titleLarge: TextUnit = 20.sp,
    val titleMedium: TextUnit = 18.sp,
    val bodyLarge: TextUnit = 16.sp,
    val bodyMedium: TextUnit = 14.sp,
    val caption: TextUnit = 12.sp,
    val labelSmall: TextUnit = 11.sp
)

val LocalAppTypography = compositionLocalOf { AppTypography() }

object ResponsiveTextScale {
    @Composable
    @ReadOnlyComposable
    fun getTypography(): AppTypography {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        return when {
            screenWidth < 360 -> { // Small Phones
                AppTypography(
                    displayLarge = 22.sp,
                    displayMedium = 20.sp,
                    titleLarge = 18.sp,
                    titleMedium = 16.sp,
                    bodyLarge = 14.sp,
                    bodyMedium = 12.sp,
                    caption = 10.sp,
                    labelSmall = 9.sp
                )
            }
            screenWidth < 600 -> { // Medium/Large Phones
                AppTypography(
                    displayLarge = 28.sp,
                    displayMedium = 24.sp,
                    titleLarge = 20.sp,
                    titleMedium = 18.sp,
                    bodyLarge = 16.sp,
                    bodyMedium = 14.sp,
                    caption = 12.sp,
                    labelSmall = 11.sp
                )
            }
            else -> { // Tablets
                AppTypography(
                    displayLarge = 36.sp,
                    displayMedium = 32.sp,
                    titleLarge = 26.sp,
                    titleMedium = 22.sp,
                    bodyLarge = 18.sp,
                    bodyMedium = 16.sp,
                    caption = 14.sp,
                    labelSmall = 12.sp
                )
            }
        }
    }
}

/**
 * Returns a Material3 Typography based on responsive scaling.
 */
@Composable
fun getResponsiveMaterialTypography(): Typography {
    val responsive = ResponsiveTextScale.getTypography()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = responsive.displayLarge
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = responsive.displayMedium
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = responsive.titleLarge
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = responsive.titleMedium
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = responsive.bodyLarge
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = responsive.bodyMedium
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = responsive.bodyMedium // Using bodyMedium for button labels by default
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = responsive.labelSmall
        )
    )
}
