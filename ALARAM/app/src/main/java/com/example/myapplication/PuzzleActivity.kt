package com.example.alarmpuzzle

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.alarmpuzzle.ui.theme.AlarmPuzzleTheme
import kotlin.math.sqrt

class PuzzleActivity : ComponentActivity() {
    fun unregisterShakeListener(listener: SensorEventListener) {
        sensorManager?.unregisterListener(listener)
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeListener: SensorEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarm = AlarmRepository.loadAlarms(this).find { it.id == alarmId }

        if (alarm == null) {
            finish()
            return
        }

        // --- PREVENT BACK BUTTON DISMISSAL ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@PuzzleActivity, "You must solve the puzzle to dismiss the alarm.", Toast.LENGTH_SHORT).show()
            }
        })

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            AlarmPuzzleTheme {
                PuzzleScreen(alarm = alarm) {
                    AlarmSoundManager.stopSound()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeListener?.let {
            sensorManager?.registerListener(it, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeListener)
    }

    fun setShakeListener(listener: SensorEventListener) {
        this.shakeListener = listener
        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }
}

@Composable
fun PuzzleScreen(alarm: Alarm, onSolve: () -> Unit) {
    when (alarm.puzzleType) {
        PuzzleType.MATH -> MathPuzzleScreen(onSolve)
        PuzzleType.RETYPE -> RetypeTextScreen(onSolve)
        PuzzleType.SHAKE -> ShakePuzzleComposable(onSolve)
        PuzzleType.MEMORY -> MemoryPairsScreen(onSolve)
        PuzzleType.COLOR_MATCH -> ColorMatchScreen(onSolve)
    }
}


@Composable
fun ShakePuzzleComposable(onSolve: () -> Unit) {
    val activity = LocalContext.current as PuzzleActivity
    var shakeCount by remember { mutableStateOf(0) }

    val targetShakes = 12          // realistic
    val shakeThreshold = 2.0f      // realistic phone shake
    var lastUpdate = remember { 0L }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gForce = sqrt(x*x + y*y + z*z) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()

                if (gForce > shakeThreshold && now - lastUpdate > 150) {
                    lastUpdate = now
                    shakeCount++
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register listener
        activity.setShakeListener(listener)

        // Cleanup — THIS WAS MISSING
        onDispose {
            activity.unregisterShakeListener(listener)
        }
    }

    LaunchedEffect(shakeCount) {
        if (shakeCount >= targetShakes) onSolve()
    }


    ShakePuzzleScreen(shakes = shakeCount, targetShakes = targetShakes)
}


