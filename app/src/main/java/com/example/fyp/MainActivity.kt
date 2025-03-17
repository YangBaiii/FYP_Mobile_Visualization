package com.example.fyp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fyp.experiment.TapZoneExperimentActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the experiment activity
        val intent = Intent(this, TapZoneExperimentActivity::class.java)
        startActivity(intent)
        finish() // Close this activity
    }
}
