package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.VisualizerSource
import com.example.player.VisualizerStyle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Jetpack Compose Canvas component rendering real-time frequency spectrum,
 * floating peak holds, radial mandalas, and fluid waveforms captured via
 * the Android Visualizer API or AudioRecord PCM processor.
 */
@Composable
fun RealtimeFrequencyCanvasVisualizer(
    frequencyBands: FloatArray,
    peakBands: FloatArray,
    waveformData: FloatArray,
    style: VisualizerStyle,
    source: VisualizerSource,
    isPlaying: Boolean,
    sensitivity: Float,
    onStyleChange: (VisualizerStyle) -> Unit,
    onSourceChange: (VisualizerSource) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onRequestPermission: () -> Unit,
    hasPermission: Boolean,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(false) }

    // Smooth continuous rotation animation for Radial mode
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerRotation")
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse transition for center core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceElevated, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("realtime_visualizer_container")
    ) {
        // Visualizer Top Info Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source Badge & Permission Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .clickable { onRequestPermission() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("visualizer_source_badge")
            ) {
                val (badgeColor, badgeText) = when (source) {
                    VisualizerSource.VISUALIZER_API -> Pair(CyanPrimary, "Visualizer API (Hardware)")
                    VisualizerSource.AUDIO_RECORD -> Pair(MintSecondary, "AudioRecord Mic FFT")
                    VisualizerSource.SYNTHETIC -> Pair(CoralAccent, if (hasPermission) "Adaptive FFT" else "Tap to Enable Mic/Hardware")
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Quick Style Switchers
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onStyleChange(VisualizerStyle.SPECTRUM_BARS) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (style == VisualizerStyle.SPECTRUM_BARS) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .testTag("style_bars_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Spectrum Bars",
                        tint = if (style == VisualizerStyle.SPECTRUM_BARS) CyanPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onStyleChange(VisualizerStyle.RADIAL_MANDALA) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (style == VisualizerStyle.RADIAL_MANDALA) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .testTag("style_radial_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = "Radial Mandala",
                        tint = if (style == VisualizerStyle.RADIAL_MANDALA) CyanPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onStyleChange(VisualizerStyle.WAVEFORM_CURVE) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (style == VisualizerStyle.WAVEFORM_CURVE) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .testTag("style_wave_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = "Waveform Curve",
                        tint = if (style == VisualizerStyle.WAVEFORM_CURVE) CyanPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showControls) CyanContainer else Color.Transparent)
                        .testTag("visualizer_settings_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Sensitivity Settings",
                        tint = if (showControls) CyanPrimary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable Sensitivity & Mode Bar
        AnimatedVisibility(visible = showControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensitivity Gain: ${"%.1f".format(sensitivity)}x",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Visualizer API",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (source == VisualizerSource.VISUALIZER_API) CyanPrimary else TextMuted,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onSourceChange(VisualizerSource.VISUALIZER_API) }
                        )
                        Text(text = "•", color = TextMuted)
                        Text(
                            text = "AudioRecord",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (source == VisualizerSource.AUDIO_RECORD) CyanPrimary else TextMuted,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onSourceChange(VisualizerSource.AUDIO_RECORD) }
                        )
                        Text(text = "•", color = TextMuted)
                        Text(
                            text = "Adaptive",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (source == VisualizerSource.SYNTHETIC) CyanPrimary else TextMuted,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { onSourceChange(VisualizerSource.SYNTHETIC) }
                        )
                    }
                }
                Slider(
                    value = sensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // THE CORE CUSTOM CANVAS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBg)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("frequency_spectrum_canvas")
            ) {
                // Ensure onDraw relies solely on DrawScope's internal size dimensions
                when (style) {
                    VisualizerStyle.SPECTRUM_BARS -> {
                        drawSpectrumBars(
                            frequencyBands = frequencyBands,
                            peakBands = peakBands,
                            isPlaying = isPlaying
                        )
                    }
                    VisualizerStyle.RADIAL_MANDALA -> {
                        drawRadialMandala(
                            frequencyBands = frequencyBands,
                            rotationAngle = rotationDegrees,
                            pulse = pulseScale,
                            isPlaying = isPlaying
                        )
                    }
                    VisualizerStyle.WAVEFORM_CURVE -> {
                        drawWaveformCurve(
                            frequencyBands = frequencyBands,
                            waveformData = waveformData,
                            isPlaying = isPlaying
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws vertical frequency spectrum bars with floating peak dots and mirror reflections.
 */
private fun DrawScope.drawSpectrumBars(
    frequencyBands: FloatArray,
    peakBands: FloatArray,
    isPlaying: Boolean
) {
    val count = frequencyBands.size
    if (count == 0) return

    val totalWidth = size.width
    val totalHeight = size.height

    // 70% height for upward bars, 30% for subtle bottom reflection
    val baselineY = totalHeight * 0.72f
    val maxBarHeight = baselineY * 0.90f

    val totalSpacing = totalWidth * 0.20f
    val spacing = totalSpacing / (count + 1)
    val barWidth = (totalWidth - totalSpacing) / count

    val barGradient = Brush.verticalGradient(
        colors = listOf(CyanPrimary, CyanAccent, MintSecondary),
        startY = baselineY - maxBarHeight,
        endY = baselineY
    )

    val reflectionGradient = Brush.verticalGradient(
        colors = listOf(CyanAccent.copy(alpha = 0.35f), Color.Transparent),
        startY = baselineY,
        endY = totalHeight
    )

    for (i in 0 until count) {
        val x = spacing + i * (barWidth + spacing)
        val mag = frequencyBands[i].coerceIn(0.02f, 1.0f)
        val peak = peakBands.getOrElse(i) { mag }.coerceIn(0.02f, 1.0f)

        val barH = maxBarHeight * mag
        val topY = baselineY - barH

        // Draw main frequency bar with rounded top corners
        drawRoundRect(
            brush = barGradient,
            topLeft = Offset(x, topY),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )

        // Draw floating peak dot
        val peakY = baselineY - (maxBarHeight * peak) - 4.dp.toPx()
        val peakRadius = (barWidth * 0.45f).coerceAtLeast(1.5.dp.toPx())

        // Peak halo glow
        drawCircle(
            color = CyanPrimary.copy(alpha = 0.35f),
            radius = peakRadius * 1.8f,
            center = Offset(x + barWidth / 2, peakY)
        )
        // Peak solid core
        drawCircle(
            color = if (isPlaying) CoralAccent else CyanAccent,
            radius = peakRadius,
            center = Offset(x + barWidth / 2, peakY)
        )

        // Draw subtle mirror reflection below baseline
        val reflectionH = barH * 0.35f
        drawRoundRect(
            brush = reflectionGradient,
            topLeft = Offset(x, baselineY + 2.dp.toPx()),
            size = Size(barWidth, reflectionH),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
    }

    // Baseline accent line
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, CyanPrimary.copy(alpha = 0.4f), Color.Transparent)),
        start = Offset(0f, baselineY),
        end = Offset(totalWidth, baselineY),
        strokeWidth = 1.dp.toPx()
    )
}

/**
 * Draws a circular radial mandala frequency visualizer radiating outward from the center.
 */
private fun DrawScope.drawRadialMandala(
    frequencyBands: FloatArray,
    rotationAngle: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    val count = frequencyBands.size
    if (count == 0) return

    val centerX = size.width / 2
    val centerY = size.height / 2
    val center = Offset(centerX, centerY)

    val maxRadius = minOf(size.width, size.height) * 0.48f
    val baseInnerRadius = maxRadius * 0.38f * (if (isPlaying) pulse else 1f)
    val maxSpike = maxRadius - baseInnerRadius

    // Center pulsating orb
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanPrimary.copy(alpha = 0.4f), Color.Transparent),
            center = center,
            radius = baseInnerRadius
        ),
        radius = baseInnerRadius,
        center = center
    )

    drawCircle(
        color = CyanContainer,
        radius = baseInnerRadius * 0.65f,
        center = center
    )

    drawCircle(
        color = CyanPrimary,
        radius = 4.dp.toPx(),
        center = center
    )

    val angleStep = (2 * PI / count)
    val rotationRad = (rotationAngle * PI / 180f)

    for (i in 0 until count) {
        val angle = (i * angleStep) + rotationRad
        val mag = frequencyBands[i].coerceIn(0.04f, 1.0f)
        val spikeLen = maxSpike * mag

        val startX = centerX + (baseInnerRadius * cos(angle)).toFloat()
        val startY = centerY + (baseInnerRadius * sin(angle)).toFloat()

        val endX = centerX + ((baseInnerRadius + spikeLen) * cos(angle)).toFloat()
        val endY = centerY + ((baseInnerRadius + spikeLen) * sin(angle)).toFloat()

        // Color based on frequency range (Bass=Coral, Mid=Cyan, High=Mint)
        val barColor = when {
            i < count * 0.25f -> CoralAccent
            i < count * 0.65f -> CyanPrimary
            else -> MintSecondary
        }

        drawLine(
            color = barColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Outer tip glowing dot
        drawCircle(
            color = barColor.copy(alpha = 0.7f),
            radius = 2.dp.toPx(),
            center = Offset(endX, endY)
        )
    }
}

/**
 * Draws a smooth fluid cubic Bézier waveform / energy frequency area curve.
 */
private fun DrawScope.drawWaveformCurve(
    frequencyBands: FloatArray,
    waveformData: FloatArray,
    isPlaying: Boolean
) {
    val totalWidth = size.width
    val totalHeight = size.height
    val midY = totalHeight / 2f

    // Construct upper energy spline path
    val path = Path()
    val fillPath = Path()

    val count = frequencyBands.size
    if (count < 2) return

    val stepX = totalWidth / (count - 1)

    // Calculate control points
    val points = ArrayList<Offset>()
    for (i in 0 until count) {
        val x = i * stepX
        val mag = frequencyBands[i].coerceIn(0.02f, 1.0f)
        val y = midY - (mag * (midY * 0.85f))
        points.add(Offset(x, y))
    }

    path.moveTo(points[0].x, points[0].y)
    fillPath.moveTo(0f, midY)
    fillPath.lineTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val ctrlX = (p0.x + p1.x) / 2
        path.cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
        fillPath.cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
    }

    fillPath.lineTo(totalWidth, midY)
    fillPath.close()

    // Draw gradient area fill
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                CyanPrimary.copy(alpha = 0.35f),
                CyanAccent.copy(alpha = 0.15f),
                Color.Transparent
            ),
            startY = 0f,
            endY = midY
        )
    )

    // Draw glowing stroke line
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(
            colors = listOf(CyanAccent, CyanPrimary, MintSecondary, CoralAccent)
        ),
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Mirrored bottom curve with lower opacity
    val mirrorPath = Path()
    mirrorPath.moveTo(points[0].x, midY + (midY - points[0].y) * 0.45f)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val ctrlX = (p0.x + p1.x) / 2
        val y0 = midY + (midY - p0.y) * 0.45f
        val y1 = midY + (midY - p1.y) * 0.45f
        mirrorPath.cubicTo(ctrlX, y0, ctrlX, y1, p1.x, y1)
    }

    drawPath(
        path = mirrorPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                CyanAccent.copy(alpha = 0.4f),
                MintSecondary.copy(alpha = 0.4f)
            )
        ),
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // Baseline
    drawLine(
        color = CyanPrimary.copy(alpha = 0.2f),
        start = Offset(0f, midY),
        end = Offset(totalWidth, midY),
        strokeWidth = 1.dp.toPx()
    )
}
