package com.example.fyp.experiment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TapZoneExperimentActivity : AppCompatActivity() {
    private lateinit var scatterPlotView: ScatterPlotView
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private lateinit var experimentLogger: ExperimentLogger
    private var isExperimentRunning = false
    private var currentTrial = 0
    private val totalTrials = 20
    private val handler = Handler(Looper.getMainLooper())
    private var trialStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tap_zone_experiment)

        // Initialize views
        scatterPlotView = findViewById(R.id.scatterPlotView)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)

        // Initialize experiment logger
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(getExternalFilesDir(null), "tap_zone_experiment_$timestamp.csv")
        experimentLogger = ExperimentLogger(logFile)

        // Set up callbacks
        scatterPlotView.setOnPointSelectedListener { point, failedAttempts, zoomLevel ->
            if (isExperimentRunning) {
                val timeTaken = System.currentTimeMillis() - trialStartTime
                experimentLogger.logTrial(
                    trial = currentTrial,
                    pointIndex = point.index,
                    price = point.price,
                    success = true,
                    failedAttempts = failedAttempts,
                    timeTaken = timeTaken
                )
                currentTrial++
                startNextTrial()
            }
        }

        scatterPlotView.setOnFailedAttemptListener { failedAttempts ->
            if (isExperimentRunning) {
                val timeTaken = System.currentTimeMillis() - trialStartTime
                experimentLogger.logTrial(
                    trial = currentTrial,
                    pointIndex = -1, // No point selected
                    price = 0f,
                    success = false,
                    failedAttempts = failedAttempts,
                    timeTaken = timeTaken
                )
            }
        }

        // Set up start button
        startButton.setOnClickListener {
            if (!isExperimentRunning) {
                startExperiment()
            }
        }
    }

    private fun startExperiment() {
        isExperimentRunning = true
        currentTrial = 0
        startButton.visibility = View.GONE
        statusText.text = "Trial ${currentTrial + 1} of $totalTrials"
        startNextTrial()
    }

    private fun startNextTrial() {
        if (currentTrial >= totalTrials) {
            endExperiment()
            return
        }

        // Reset the view for the next trial
        scatterPlotView.invalidate()
        trialStartTime = System.currentTimeMillis()
        statusText.text = "Trial ${currentTrial + 1} of $totalTrials"
    }

    private fun endExperiment() {
        isExperimentRunning = false
        startButton.visibility = View.VISIBLE
        statusText.text = "Experiment completed!"
        experimentLogger.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isExperimentRunning) {
            experimentLogger.close()
        }
    }
}

data class Point(
    val x: Float,
    val y: Float
) 