package com.example.fyp.experiment

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ExperimentLogger(private val logFile: File) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val writer = FileWriter(logFile, true)

    init {
        // Write header if file is empty
        if (logFile.length() == 0L) {
            writer.append("Timestamp,Trial,PointIndex,Price,Success,FailedAttempts,TimeTaken\n")
        }
    }

    fun logTrial(
        trial: Int,
        pointIndex: Int,
        price: Float,
        success: Boolean,
        failedAttempts: Int,
        timeTaken: Long
    ) {
        val timestamp = dateFormat.format(Date())
        writer.append("$timestamp,$trial,$pointIndex,$price,$success,$failedAttempts,$timeTaken\n")
    }

    fun close() {
        writer.close()
    }
} 