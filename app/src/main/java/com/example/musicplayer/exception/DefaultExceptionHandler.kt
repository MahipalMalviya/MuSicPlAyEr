package com.example.musicplayer.exception

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.example.musicplayer.activity.SplashActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.system.exitProcess

class DefaultExceptionHandler(private val activity: Activity): Thread.UncaughtExceptionHandler {

    companion object {
        val LOG_TAG = DefaultExceptionHandler::class.java.simpleName
    }
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        Log.e(LOG_TAG, "Exception e :$ex")
        ex.printStackTrace()

        Crashlytics.logException(ex)
        FirebaseCrashlytics.getInstance().sendUnsentReports()

        val intent = Intent(activity, SplashActivity::class.java)

        intent.putExtra("crash", true)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
                activity.baseContext, 0, intent, intent.flags)

        //Following code will restart your application after 0.5 seconds
        val mgr = activity.baseContext
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500,
                pendingIntent)

        //This will finish your activity manually
        activity.finish()

        //This will stop your application and take out from it.
        exitProcess(2)
    }
}