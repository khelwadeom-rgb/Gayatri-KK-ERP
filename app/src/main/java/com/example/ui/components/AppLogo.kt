package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Int = 200,
    logoPath: String? = null,
    onClick: () -> Unit = {}
) {
    val darkBackground = Color(0xFF030712) // Charcoal950
    val greenBorder = Color(0xFF065F46) // DeepGreen800
    val sunColor = Color(0xFFE7D06B) // GKKAccent
    val gkkColor = Color(0xFF2E7D32) // Emerald500
    val sizeDp = size.dp

    if (logoPath != null) {
        AsyncImage(
            model = logoPath,
            contentDescription = "Shop Logo",
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .clickable { onClick() },
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(darkBackground)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size
                val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                val radius = canvasSize.minDimension / 2
                
                // Outer circular border
                drawCircle(
                    color = greenBorder,
                    radius = radius * 0.95f,
                    style = Stroke(width = (radius * 0.04f))
                )
                
                // Inner circular border
                drawCircle(
                    color = greenBorder,
                    radius = radius * 0.68f,
                    style = Stroke(width = (radius * 0.02f))
                )

                // Curved Text: GAYATRI KRUSHI KENDRA
                drawIntoCanvas { canvas ->
                    val path = android.graphics.Path()
                    val arcRadius = radius * 0.75f
                    val rect = android.graphics.RectF(
                        center.x - arcRadius,
                        center.y - arcRadius,
                        center.x + arcRadius,
                        center.y + arcRadius
                    )
                    // Draw text on the top arc
                    path.addArc(rect, 165f, 210f)
                    
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = radius * 0.15f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawTextOnPath("GAYATRI KRUSHI KENDRA", path, 0f, 0f, paint)
                }

                // Sun in the center
                val sunRadius = radius * 0.18f
                val sunCenter = center.copy(y = center.y - radius * 0.1f)
                drawCircle(
                    color = sunColor,
                    radius = sunRadius,
                    center = sunCenter
                )
                
                // Sun rays
                val rayCount = 10
                val rayLength = sunRadius * 0.4f
                val rayStrokeWidth = (radius * 0.02f)
                for (i in 0 until rayCount) {
                    val angle = (i * 360f / rayCount - 90f).toRadians()
                    val start = Offset(
                        sunCenter.x + (sunRadius + (radius * 0.04f)) * cos(angle).toFloat(),
                        sunCenter.y + (sunRadius + (radius * 0.04f)) * sin(angle).toFloat()
                    )
                    val end = Offset(
                        sunCenter.x + (sunRadius + rayLength) * cos(angle).toFloat(),
                        sunCenter.y + (sunRadius + rayLength) * sin(angle).toFloat()
                    )
                    drawLine(
                        color = sunColor,
                        start = start,
                        end = end,
                        strokeWidth = rayStrokeWidth
                    )
                }
                
                // GKK Text
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = gkkColor.toArgb()
                        textSize = radius * 0.38f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                    // Position GKK slightly below the sun
                    canvas.nativeCanvas.drawText("GKK", center.x, center.y + radius * 0.25f, paint)
                }
                
                // Leaves at the bottom of GKK
                val leafWidth = radius * 0.25f
                val leafHeight = radius * 0.12f
                
                // Left leaf
                drawLeaf(
                    color = Color(0xFF2E7D32),
                    base = Offset(center.x - radius * 0.25f, center.y + radius * 0.28f),
                    width = leafWidth,
                    height = leafHeight,
                    rotation = -150f
                )
                
                // Right leaf
                drawLeaf(
                    color = Color(0xFF40E349),
                    base = Offset(center.x + radius * 0.25f, center.y + radius * 0.28f),
                    width = leafWidth,
                    height = leafHeight,
                    rotation = -30f
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLeaf(
    color: Color,
    base: Offset,
    width: Float,
    height: Float,
    rotation: Float
) {
    val path = Path().apply {
        moveTo(0f, 0f)
        cubicTo(width * 0.3f, -height * 0.8f, width * 0.7f, -height * 0.8f, width, 0f)
        cubicTo(width * 0.7f, height * 0.8f, width * 0.3f, height * 0.8f, 0f, 0f)
        close()
    }

    withTransform({
        translate(base.x, base.y)
        rotate(rotation, Offset.Zero)
    }) {
        drawPath(path, color)
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, 0f),
            end = Offset(width, 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun Float.toRadians(): Double = Math.toRadians(this.toDouble())

@Preview(showBackground = true, backgroundColor = 0xFF030712)
@Composable
fun AppLogoPreview() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        AppLogo(size = 240)
    }
}
