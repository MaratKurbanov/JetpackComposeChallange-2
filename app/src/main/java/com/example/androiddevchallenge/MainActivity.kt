/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androiddevchallenge.ui.theme.MyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyTheme {
                CountdownTimer()
            }
        }
    }
}

var ringTone: Ringtone? = null

fun getRingTone(context: Context): Ringtone? {
    if (ringTone == null) {
        var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
        ringTone = RingtoneManager.getRingtone(context, alarmUri)
    }

    return ringTone
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownTimer() {
    var degree by remember { mutableStateOf(0F) }
    var useSeconds by remember { mutableStateOf(true) }
    var start by remember { mutableStateOf(false) }
    var shouldPlayRingTone by remember { mutableStateOf(true) }
    var (isRingRingTonePlaying, setIsRingTonePlaying) = remember { mutableStateOf(false) }
    var (seconds, setSeconds) = remember { mutableStateOf("") }
    var (minutes, setMinutes) = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)

    if (seconds.isNotEmpty()) {
        degree = (seconds.toInt() * 6).toFloat()
        useSeconds = true
    } else if (minutes.isNotEmpty()) {
        degree = (minutes.toInt() * 6).toFloat()
        useSeconds = false
    }

    fun startTimer() {
        if (degree <= 0) return

        start = !start
        seconds = ""
        minutes = ""
        shouldPlayRingTone = true

        coroutineScope.launch {
            while (start) {
                degree = updateDegree(
                    degree,
                    useSeconds,
                    context,
                )

                delay(1000)

                if (degree <= 0) {
                    start = false
                }

                if (degree == 0f && shouldPlayRingTone) {
                    setIsRingTonePlaying(true)
                    getRingTone(context)?.play()
                }

                Log.d("Marat", "marat: degree=$degree")
            }
        }
    }

    fun reset() {
        degree = 0f
        setSeconds("")
        setMinutes("")
        shouldPlayRingTone = false
        setIsRingTonePlaying(false)
        if (getRingTone(context)?.isPlaying!!) {
            getRingTone(context)?.stop()
        }
    }

    Surface(color = MaterialTheme.colors.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            AnimatedVisibility(visible = isRingRingTonePlaying) {
                SnoozeDialog { reset() }
            }

            AnimatedVisibility(visible = !start) {
                UserInputRow(
                    seconds = seconds,
                    setSeconds = setSeconds,
                    minutes = minutes,
                    setMinutes = setMinutes,
                    startTimer = { startTimer() },
                    reset = { reset() },
                    modifier = rowModifier
                )
            }

            AnimatedVisibility(visible = start) {
                ClockFaceRow(
                    degree = degree,
                    modifier = rowModifier
                )
            }

            ButtonRow(
                start = start,
                modifier = rowModifier,
                onClick = { startTimer() },
                onReset = { reset() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = degree > 0) {
                SecondsDisplay(
                    useSeconds = useSeconds,
                    degree = degree,
                    modifier = rowModifier
                )
            }
        }
    }
}

@Composable
fun SnoozeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        // title = { Text(text = "Snooze", fontSize = 25.sp, fontWeight = FontWeight.Bold) },
        buttons = {
            Row(modifier = Modifier) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(text = "Stop", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        onDismissRequest = onDismiss
    )
}

fun updateDegree(
    deg: Float,
    useSeconds: Boolean,
    context: Context
): Float {

    return if (useSeconds) {
        if (deg <= 6f) {
            0f
        } else {
            deg - 6F
        }
    } else {

        if (deg <= 0.1f) {
            0f
        } else {
            deg - 0.1F
        }
    }
}

fun parseNumeric(num: String): String {
    if (num.length === 0) return ""

    return when (val temp = num.toIntOrNull()) {
        null -> ""
        else -> {
            if (temp > 60) {
                "60"
            } else {
                temp.toString()
            }
        }
    }
}

@Composable
fun ClockFaceRow(degree: Float, modifier: Modifier) {

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .padding(10.dp),
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = size.minDimension / 4

            drawCircle(
                color = Color.Black,
                center = Offset(x = canvasWidth / 2, y = canvasHeight / 2),
                radius = radius + 4,
                alpha = 0.5f,
                style = Stroke(
                    width = 4f,
                )
            )

            drawCircle(
                color = Color.Red,
                center = Offset(x = canvasWidth / 2, y = canvasHeight / 2),
                radius = radius,
                alpha = 0.5f,
            )

            withTransform({
                // translate(left = canvasWidth / 5F)
                rotate(degrees = -90f)
            }) {

                drawArc(
                    color = Color.Gray,
                    size = size / 2f,
                    topLeft = Offset(x = canvasWidth / 4, y = canvasHeight / 4),
                    startAngle = 0f,
                    sweepAngle = degree,
                    useCenter = true
                )
            }

            withTransform({
                // translate(left = canvasWidth / 5F)
                rotate(degrees = degree)
            }) {
                inset(50F, 50F) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(x = canvasWidth / 2 - 50, y = canvasHeight / 2 - 50),
                        end = Offset(
                            x = canvasWidth / 2 - 50,
                            y = (canvasHeight / 2) - radius - 50
                        ),
                        strokeWidth = 5f,
                    )
                }
            }
        }
    }
}

@Composable
fun UserInputRow(
    seconds: String,
    setSeconds: (String) -> Unit,
    minutes: String,
    setMinutes: (String) -> Unit,
    startTimer: () -> Unit,
    reset: () -> Unit,
    modifier: Modifier = Modifier
) {

    var isSeconds by remember { mutableStateOf(true) }

    fun switchSecondsVsMinutes() {
        isSeconds = !isSeconds
        if (isSeconds) {
            setMinutes("")
        } else {
            setSeconds("")
        }
    }

    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val buttonModifier = Modifier
            .padding(5.dp)
            .width(100.dp)
            .alignByBaseline()

        Crossfade(targetState = isSeconds) { screen ->
            when (screen) {
                true -> OutlinedTextField(
                    value = seconds,
                    maxLines = 1,
                    singleLine = true,
                    onValueChange = { setSeconds(parseNumeric(it)) },
                    textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
                    label = { Text("Seconds") },
                    modifier = buttonModifier,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            startTimer()
                        }
                    )
                )
                false -> OutlinedTextField(
                    value = minutes,
                    singleLine = true,
                    onValueChange = { setMinutes(parseNumeric(it)) },
                    textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
                    label = { Text("Minutes") },
                    modifier = buttonModifier,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            startTimer()
                        }
                    )
                )
            }
        }

        ExtendedFloatingActionButton(
            text = {
                Crossfade(targetState = isSeconds) { buttonText ->
                    when (buttonText) {
                        true -> Text("Seconds")
                        false -> Text("Minutes")
                    }
                }
            },
            onClick = {
                switchSecondsVsMinutes()
                reset()
            },
            Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
fun SecondsDisplay(
    degree: Float,
    useSeconds: Boolean,
    modifier: Modifier
) {
    var time: String
    if (useSeconds) {
        time = ((degree / 6).toInt()).toString()
    } else {
        val timeLong = (degree / 0.1).toInt()
        val seconds = (timeLong % 3600) / 60
        val minutes = timeLong % 60
        time = "$seconds : $minutes"
    }

    Row(
        modifier.padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(time, fontSize = 60.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview()
@Composable
fun SecondsDispalyPreview() {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    MyTheme {
        SecondsDisplay(30f, false, rowModifier)
    }
}

@Composable
fun ButtonRow(
    start: Boolean,
    onClick: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onClick, Modifier.width(100.dp)) {

            Crossfade(targetState = start) { buttonText ->
                when (buttonText) {
                    false -> Text("Start")
                    true -> Text("Stop")
                }
            }
        }

        val alpha: Float by animateFloatAsState(if (start) 0.5f else 1f)
        Button(
            onClick = {
                onReset()
            },
            Modifier
                .width(100.dp)
                .graphicsLayer(alpha = alpha)
        ) {
            Text("Reset")
        }
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        CountdownTimer()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        CountdownTimer()
    }
}
