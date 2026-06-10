package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ResponsiveText

enum class ButtonSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    val height = when (size) {
        ButtonSize.SMALL -> if (isSmallScreen) 32.dp else 40.dp
        ButtonSize.MEDIUM -> if (isSmallScreen) 40.dp else 48.dp
        ButtonSize.LARGE -> if (isSmallScreen) 48.dp else 56.dp
    }

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                    fontSize = ResponsiveText.button()
                )
            ) {
                content()
            }
        }
    )
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    val height = when (size) {
        ButtonSize.SMALL -> if (isSmallScreen) 32.dp else 40.dp
        ButtonSize.MEDIUM -> if (isSmallScreen) 40.dp else 48.dp
        ButtonSize.LARGE -> if (isSmallScreen) 48.dp else 56.dp
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                    fontSize = ResponsiveText.button()
                )
            ) {
                content()
            }
        }
    )
}
