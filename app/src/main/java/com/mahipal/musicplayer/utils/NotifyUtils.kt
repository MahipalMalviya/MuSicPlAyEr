package com.mahipal.musicplayer.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.mahipal.musicplayer.R
import com.mahipal.musicplayer.activity.MusicPlayerActivity
import com.mahipal.musicplayer.service.MusicService

object NotifyUtils {

    private val TAG = NotifyUtils::class.java.simpleName

    private var notificationManager: NotificationManager? = null

    fun getNotification(context: Context,channelId:String): NotificationCompat.Builder {
        val notificationCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context.applicationContext, channelId)
        } else {
            NotificationCompat.Builder(context.applicationContext)
        }

        notificationCompat.setOnlyAlertOnce(true)

        val smallNotificationView = RemoteViews(context.packageName, R.layout.music_player_notification)
        val expandedNotificationView = RemoteViews(context.packageName, R.layout.music_player_notification_expanded)

        attachPendingIntent(context,smallNotificationView)
        attachPendingIntent(context,expandedNotificationView)

        notificationCompat.setCustomContentView(smallNotificationView)
        notificationCompat.setCustomBigContentView(expandedNotificationView)

        notificationCompat.setSmallIcon(R.drawable.music)

        //set pending Intent for Notification on Click event
        val launchIntent = Intent(context.applicationContext, MusicPlayerActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pLaunchIntent = PendingIntent.getActivity(context.applicationContext, 0, launchIntent, 0)
        notificationCompat.setContentIntent(pLaunchIntent)

        notificationCompat.setOngoing(true)

        return notificationCompat
    }

    private fun getManager(context: Context): NotificationManager? {
        if (notificationManager == null) {
            notificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG,"notification manager not null")
        }
        return notificationManager
    }

    fun notify(noticationId:Int,notification:Notification) {
        notificationManager?.notify(noticationId,notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager?.cancel(notificationId)
    }

    private fun attachPendingIntent(context: Context,remoteView: RemoteViews) {
        val playIntent = Intent(MusicService.NOTIFY_PLAY)
        val pauseIntent = Intent(MusicService.NOTIFY_PAUSE)
        val prevIntent = Intent(MusicService.NOTIFY_PREV)
        val nextIntent = Intent(MusicService.NOTIFY_NEXT)

        val pPlay = PendingIntent.getBroadcast(context.applicationContext, 0,
                playIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_play, pPlay)

        val pPause = PendingIntent.getBroadcast(context.applicationContext, 0,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_play, pPause)

        val pNext = PendingIntent.getBroadcast(context.applicationContext, 0,
                nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_next, pNext)

        val pPrevious = PendingIntent.getBroadcast(context.applicationContext, 0,
                prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_prev, pPrevious)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun addChannel(context: Context,channelId:String,channelName:String) {
        var notificationChannel = getManager(context)?.getNotificationChannel(channelId)
        if(notificationChannel == null) {
            notificationChannel = NotificationChannel(channelId,channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setSound(null,null)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getManager(context)?.createNotificationChannel(notificationChannel)
    }
}