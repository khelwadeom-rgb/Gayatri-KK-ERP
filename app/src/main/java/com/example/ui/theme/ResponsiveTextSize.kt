package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit

/**
 * Standardized typography for Gayatri Krushi Kendra ERP.
 * Redirects to ResponsiveTextScale for dynamic sizing.
 */
object ResponsiveText {
    @Composable
    fun heading(): TextUnit = ResponsiveTextScale.getTypography().titleLarge

    @Composable
    fun title(): TextUnit = ResponsiveTextScale.getTypography().titleMedium

    @Composable
    fun body(): TextUnit = ResponsiveTextScale.getTypography().bodyLarge

    @Composable
    fun label(): TextUnit = ResponsiveTextScale.getTypography().caption

    @Composable
    fun button(): TextUnit = ResponsiveTextScale.getTypography().bodyLarge

    @Composable
    fun display(): TextUnit = ResponsiveTextScale.getTypography().displayLarge
}
