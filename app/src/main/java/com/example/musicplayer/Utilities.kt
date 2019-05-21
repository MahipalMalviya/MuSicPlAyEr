package com.example.musicplayer

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.TextView

/**
 * Created by MAHIPAL-PC on 15-12-2017.
 */

object Utilities {

    fun milliSecondsToTimer(milliseconds: Long): String {
        var finalTimerString = ""
        var secondsString = ""

        // Convert total duration into time
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
        // Add hours if there
        if (hours > 0) {
            finalTimerString = "$hours:"
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0$seconds"
        } else {
            secondsString = "" + seconds
        }

        finalTimerString = "$finalTimerString$minutes:$secondsString"

        // return timer string
        return finalTimerString
    }

    fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        val percentage: Double?

        val currentSeconds = (currentDuration / 1000).toInt().toLong()
        val totalSeconds = (totalDuration / 1000).toInt().toLong()

        // calculating percentage
        percentage = currentSeconds.toDouble() / totalSeconds * 100

        // return percentage
        return percentage.toInt()
    }

    fun progressToTimer(progress: Int, totalDuration: Int): Int {
        var totalDuration = totalDuration
        var currentDuration = 0
        totalDuration = totalDuration / 1000
        currentDuration = (progress.toDouble() / 100 * totalDuration).toInt()

        // return current duration in milliseconds
        return currentDuration * 1000
    }

    fun showMessage(context: Context, message: String, maxLine: Int) {
        val snackbar = Snackbar.make((context as Activity).findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val view = snackbar.view
        val textView = view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.maxLines = maxLine
        snackbar.show()
    }

}
