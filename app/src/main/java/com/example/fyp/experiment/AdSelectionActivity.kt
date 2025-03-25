package com.example.fyp.experiment

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.R

class AdSelectionActivity : AppCompatActivity() {
    private lateinit var adSelectionView: AdSelectionView
    private lateinit var nextTrialButton: Button
    private lateinit var trialCountText: TextView
    private var currentTrial = 0
    private val totalTrials = 3  // One trial for each solution type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_selection)

        adSelectionView = findViewById(R.id.adSelectionView)
        nextTrialButton = findViewById(R.id.nextTrialButton)
        trialCountText = findViewById(R.id.trialCountText)

        // Set up selection listener
        adSelectionView.setOnSelectionListener { success, trial, attempts, time ->
            // Handle selection result
            if (success) {
                nextTrialButton.isEnabled = true
            }
        }

        // Set up next trial button
        nextTrialButton.setOnClickListener {
            if (currentTrial < totalTrials - 1) {
                currentTrial++
                updateTrialCount()
                adSelectionView.setCurrentTrial(currentTrial)
                adSelectionView.startNewTrial()
                nextTrialButton.isEnabled = false
            } else {
                // Experiment complete
                finish()
            }
        }

        // Start first trial
        updateTrialCount()
        adSelectionView.setCurrentTrial(currentTrial)
        adSelectionView.startNewTrial()
    }

    private fun updateTrialCount() {
        trialCountText.text = "Trial ${currentTrial + 1}/$totalTrials"
    }
} 