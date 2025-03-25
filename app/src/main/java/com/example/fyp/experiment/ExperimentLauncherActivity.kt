package com.example.fyp.experiment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.R

class ExperimentLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_experiment_launcher)

        findViewById<Button>(R.id.tap_zone_button).setOnClickListener {
            startActivity(Intent(this, TapZoneExperimentActivity::class.java))
        }

        findViewById<Button>(R.id.two_step_button).setOnClickListener {
            startActivity(Intent(this, TwoStepSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.ad_selection_button).setOnClickListener {
            startActivity(Intent(this, AdSelectionActivity::class.java))
        }
    }
} 