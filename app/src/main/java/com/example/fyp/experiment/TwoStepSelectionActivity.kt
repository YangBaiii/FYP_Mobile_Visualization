package com.example.fyp.experiment

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TwoStepSelectionActivity : AppCompatActivity() {
    private lateinit var selectionView: TwoStepSelectionView
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private lateinit var experimentLogger: ExperimentLogger
    private var currentTrial = 0
    private val totalTrials = 20
    private var trialStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_step_selection)

        selectionView = findViewById(R.id.selectionView)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)

        // Initialize logger
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), 
            "two_step_selection_$timestamp.csv")
        experimentLogger = ExperimentLogger(logFile)

        setupExperiment()
    }

    private fun setupExperiment() {
        startButton.setOnClickListener {
            startButton.visibility = Button.GONE
            startTrial()
        }

        selectionView.setOnSelectionListener { success, targetIndex, attempts, timeTaken ->
            experimentLogger.logTrial(
                currentTrial,
                targetIndex,
                0.0f, // Changed from 0.0 to 0.0f to match Float type
                success,
                attempts,
                timeTaken
            )

            if (currentTrial < totalTrials) {
                startTrial()
            } else {
                endExperiment()
            }
        }
    }

    private fun startTrial() {
        currentTrial++
        trialStartTime = System.currentTimeMillis()
        statusText.text = "Trial $currentTrial of $totalTrials"
        selectionView.startNewTrial()
    }

    private fun endExperiment() {
        experimentLogger.close()
        statusText.text = "Experiment completed!"
        startButton.visibility = Button.VISIBLE
        startButton.text = "Restart"
        currentTrial = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        experimentLogger.close()
    }
} 