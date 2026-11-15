package io.github.fallasleeptimetable.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.fallasleeptimetable.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp

private const val BILIBILI_URL = "https://b23.tv/VZ6GPhy"
private const val XIAOHONGSHU_URL = "https://xhslink.cn/o/7qKpW27kqzt"
private const val X_URL = "https://x.com/Lyowsoo"
private const val GITHUB_URL = "https://github.com/tukinokilyowsoo"

/** 长按后振幅最多到 50%（255 的一半左右）。 */
private const val MAX_VIBRATION_AMPLITUDE = 127
private const val LONG_PRESS_DELAY_MS = 500L

/** 卡片随设备倾斜的最大角度（度）与敏感度。 */
private const val MAX_CARD_TILT_DEGREES = 4.5f
private const val TILT_SENSITIVITY_RADIANS = 0.26f // 约 15°

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactDeveloperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatarScale = remember { Animatable(1f) }
    val vibrator = remember { obtainVibrator(context) }
    var vibrationJob by remember { mutableStateOf<Job?>(null) }
    var currentAmplitude by remember { mutableIntStateOf(0) }

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            vibrationJob?.cancel()
            vibrator?.cancel()
        }
    }

    RegisterTiltSensor { x, y ->
        tiltX = x
        tiltY = y
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationX = tiltX
                        rotationY = tiltY
                        cameraDistance = 12f * density
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = availableHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "联系开发者",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(R.drawable.profile_avatar),
                        contentDescription = "槻ノ木 響子",
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                scaleX = avatarScale.value
                                scaleY = avatarScale.value
                            }
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    scope.launch {
                                        // 缩小阶段：线性、慢速。
                                        avatarScale.animateTo(
                                            0.82f,
                                            tween(durationMillis = 2000, easing = LinearEasing),
                                        )
                                    }

                                    var longPressed = false
                                    val longPressJob = scope.launch {
                                        delay(LONG_PRESS_DELAY_MS)
                                        longPressed = true
                                        currentAmplitude = 0
                                        vibrationJob?.cancel()
                                        val start = SystemClock.elapsedRealtime()
                                        vibrationJob = scope.launch {
                                            while (isActive) {
                                                val seconds =
                                                    (SystemClock.elapsedRealtime() - start) / 1000f
                                                // 指数缓慢上升，封顶 50% 振幅。
                                                val amplitude = (
                                                    MAX_VIBRATION_AMPLITUDE *
                                                        (1f - exp(-seconds / 3f))
                                                    ).toInt()
                                                    .coerceIn(1, MAX_VIBRATION_AMPLITUDE)
                                                currentAmplitude = amplitude
                                                pulseVibration(vibrator, amplitude, 45L)
                                                delay(70L)
                                            }
                                        }
                                    }

                                    // 自己消费按住期间的所有事件，避免父级滚动容器
                                    // 消费移动事件、把手势判成取消，导致尺寸来回波动。
                                    var released = false
                                    var cancelled = false
                                    while (!released) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes
                                            .firstOrNull { it.id == down.id }
                                        if (change == null) {
                                            cancelled = true
                                            released = true
                                        } else if (!change.pressed) {
                                            released = true
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                    longPressJob.cancel()

                                    if (cancelled) {
                                        vibrationJob?.cancel()
                                        vibrator?.cancel()
                                        scope.launch {
                                            avatarScale.animateTo(
                                                1f,
                                                tween(
                                                    durationMillis = 120,
                                                    easing = LinearEasing,
                                                ),
                                            )
                                        }
                                        return@awaitEachGesture
                                    }

                                    if (longPressed) {
                                        vibrationJob?.cancel()
                                        Toast.makeText(
                                            context,
                                            "猫喜欢你🥺🥺🥺",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        val decayStart = currentAmplitude.coerceAtLeast(1)
                                        scope.launch {
                                            // 松手后振幅线性衰减到 0。
                                            for (step in 8 downTo 0) {
                                                pulseVibration(
                                                    vibrator,
                                                    (decayStart * step / 8).coerceAtLeast(1),
                                                    40L,
                                                )
                                                delay(40L)
                                            }
                                            vibrator?.cancel()
                                        }
                                    }

                                    // 回弹：先弹到 120%，再用更缓的弹簧收回 100%。
                                    scope.launch {
                                        avatarScale.animateTo(
                                            1.2f,
                                            spring(
                                                dampingRatio = 0.45f,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                        avatarScale.animateTo(
                                            1f,
                                            spring(
                                                dampingRatio = 0.65f,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                }
                            },
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "槻ノ木 響子",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = "Lyowsoo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp,
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "猫想要关注🥺🥺🥺",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                openUrl(context, BILIBILI_URL)
                            },
                            label = { Text("哔哩哔哩") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bilibili),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        AssistChip(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "猫想要关注🥺🥺🥺",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                openUrl(context, XIAOHONGSHU_URL)
                            },
                            label = { Text("小红书") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_xiaohongshu),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        AssistChip(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "猫想要关注🥺🥺🥺",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                openUrl(context, X_URL)
                            },
                            label = { Text("X") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_x),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "报告问题请前往 GitHub 提交 Issue，或在小红书主页留言。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { openUrl(context, GITHUB_URL) }) {
                        Text("转到 GitHub")
                    }
                    Text(
                        text = "© 2026 槻ノ木 響子 · Lyowsoo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

/**
 * 用旋转矢量传感器把设备相对打开弹窗时的姿态倾斜映射成卡片的小角度 3D 旋转。
 * 没有传感器时保持静止。
 */
@Composable
private fun RegisterTiltSensor(onTilt: (Float, Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(SensorManager::class.java)
    }
    val sensor = remember {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    DisposableEffect(sensorManager, sensor) {
        if (sensorManager == null || sensor == null) {
            onDispose { }
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            val displayRotation = windowManager.defaultDisplay.rotation
            val rotationMatrix = FloatArray(9)
            val adjustedMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            var basePitch = Float.NaN
            var baseRoll = Float.NaN
            var smoothedX = 0f
            var smoothedY = 0f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    when (displayRotation) {
                        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_Y,
                            SensorManager.AXIS_MINUS_X,
                            adjustedMatrix,
                        )
                        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_X,
                            SensorManager.AXIS_MINUS_Y,
                            adjustedMatrix,
                        )
                        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X,
                            adjustedMatrix,
                        )
                        else -> SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Y,
                            adjustedMatrix,
                        )
                    }
                    SensorManager.getOrientation(adjustedMatrix, orientation)
                    val pitch = orientation[1]
                    val roll = orientation[2]
                    if (basePitch.isNaN()) {
                        basePitch = pitch
                        baseRoll = roll
                    }
                    val targetX = (
                        (pitch - basePitch) / TILT_SENSITIVITY_RADIANS
                        ).coerceIn(-1f, 1f) * MAX_CARD_TILT_DEGREES
                    val targetY = (
                        (roll - baseRoll) / TILT_SENSITIVITY_RADIANS
                        ).coerceIn(-1f, 1f) * MAX_CARD_TILT_DEGREES
                    smoothedX += (targetX - smoothedX) * 0.12f
                    smoothedY += (targetY - smoothedY) * 0.12f
                    onTilt(smoothedX, smoothedY)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
            )
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
}

private fun obtainVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

private fun pulseVibration(vibrator: Vibrator?, amplitude: Int, durationMs: Long) {
    val device = vibrator ?: return
    if (!device.hasVibrator()) return
    if (device.hasAmplitudeControl()) {
        device.vibrate(
            VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)),
        )
    } else {
        @Suppress("DEPRECATION")
        device.vibrate(durationMs)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
