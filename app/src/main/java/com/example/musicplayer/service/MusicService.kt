package com.example.musicplayer.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.musicplayer.R
import com.example.musicplayer.activity.MainActivity
import com.example.musicplayer.callbacks.MediaPlayerControls
import com.example.musicplayer.constants.PlayerConstants
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities
import java.util.*
import android.view.KeyEvent

/**
 * Created by MAHIPAL-PC on 18-12-2017.
 */

class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayerControls {

    private val LOG_TAG = MusicService::class.java.simpleName

    var mMediaPlayer: MediaPlayer? = null

    private var songList: ArrayList<Song>? = null
    private var songIndex = -1
    private var activeSong: Song? = null
    private var notificationManager: NotificationManager? = null

    //MediaSession
    private val mediaSessionManager: MediaSessionManager? = null
    private val mediaSession: MediaSessionCompat? = null
    private val transportControls: MediaControllerCompat.TransportControls? = null

    var notificationControl: ((String) -> Unit)? = null

    companion object {
        //AudioPlayer notification ID
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "MusicPlayer101"
        private const val CHANNEL_NAME = "MusicPlayer"

        const val NOTIFY_PLAY = "com.mahipal.musicplayer.ACTION_PLAY"
        const val NOTIFY_PAUSE = "com.mahipal.musicplayer.ACTION_PAUSE"
        const val NOTIFY_PREV = "com.mahipal.musicplayer.ACTION_PREVIOUS"
        const val NOTIFY_NEXT = "com.mahipal.musicplayer.ACTION_NEXT"
    }

    // Binder given to the clients
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        mMediaPlayer = MediaPlayer()

        //register player control broadcast receiver
        registerPlayerControlsBroadCastReceiver()

        //register notification control broadcast receiver
        registerNotificationBroadcastReceiver()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        try {
            songList = SpUtility.getInstance(applicationContext)?.getSongs()
            songIndex = SpUtility.getInstance(applicationContext)?.getCurrentSongIndex() ?: 0

            PlayerConstants.SONG_NUMBER = songIndex
            PlayerConstants.SONG_LIST = songList

            if (songIndex != -1 && songIndex < songList?.size ?: 0) {
                activeSong = songList?.get(songIndex)
            } else {
                stopSelf()
            }

        } catch (ex: NullPointerException) {
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
//                initMediaSession()
                initMediaPlayer()
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }

        }

        //Handle Intent action from MediaSession.TransportControls
//        handleIncomingActions(intent);

        return START_STICKY
    }

    inner class LocalBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    private fun initMediaPlayer() {
        mMediaPlayer?.setOnCompletionListener(this)
        mMediaPlayer?.setOnPreparedListener(this)

        if (mMediaPlayer?.isPlaying == true) {
            PlayerConstants.SONG_PAUSED = true
            mMediaPlayer?.stop()
        }
        mMediaPlayer?.reset()

        mMediaPlayer?.setDataSource(activeSong?.path)

        mMediaPlayer?.prepare()
    }

    override fun onCompletion(mp: MediaPlayer?) {

        val spUtility = SpUtility.getInstance(applicationContext)
        // check for repeat is ON or OFF
        when {
            spUtility?.isSongRepeat() == true ->
                // repeat is on play same song again
                repeatMedia()

            spUtility?.isSongShuffle() == true ->
                // shuffle is on - play a random song
                shuffleMedia()

            else ->
                // no repeat or shuffle ON - play next song
                nextMedia()
        }

//        buildNotification()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        playMedia()
        buildNotification()
        notificationControl?.invoke(NOTIFY_PLAY)
    }

    override fun playMedia() {

        try {
            if (mMediaPlayer?.isPlaying == false) {
                mMediaPlayer?.start()
                PlayerConstants.SONG_PAUSED = false
            } else {
                mMediaPlayer?.pause()
                PlayerConstants.SONG_PAUSED = true
            }
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
        }
    }

    override fun pauseMedia() {
        playMedia()
    }

    override fun stopMedia() {
        if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
        }
    }

    override fun nextMedia() {
        if (songIndex == songList?.size?.minus(1)) {
            //if last in playlist
            songIndex = 0
            activeSong = songList?.get(songIndex)
        } else {
            //get next song
            activeSong = songList?.get(++songIndex)
        }

        SpUtility.getInstance(applicationContext)?.setCurrentSongIndex(songIndex)
        PlayerConstants.SONG_NUMBER = songIndex

        initMediaPlayer()
    }

    override fun prevMedia() {
        if (songIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            songIndex = songList?.size ?: 0 - 1
            activeSong = songList?.get(songIndex)
        } else {
            //get previous in playlist
            activeSong = songList?.get(--songIndex)
        }

        SpUtility.getInstance(applicationContext)?.setCurrentSongIndex(songIndex)
        PlayerConstants.SONG_NUMBER = songIndex

        initMediaPlayer()
    }

    override fun shuffleMedia() {
        // get random song position
        val rand = Random()
        songIndex = rand.nextInt(songList?.size ?: 0 - 1 + 1)

        activeSong = songList?.get(songIndex)

        SpUtility.getInstance(applicationContext)?.setCurrentSongIndex(songIndex)
        PlayerConstants.SONG_NUMBER = songIndex

        initMediaPlayer()
    }

    override fun repeatMedia() {
        // repeat same song again

        activeSong = songList?.get(songIndex)
        SpUtility.getInstance(applicationContext)?.setCurrentSongIndex(songIndex)
        PlayerConstants.SONG_NUMBER = songIndex

        initMediaPlayer()
    }

    private val playerControlBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            val playerAction = intent?.action
            controlSongByAction(playerAction)
        }
    }

    private fun controlSongByAction(playerAction: String?) {
        when {
            playerAction?.equals(MainActivity.ACTION_PLAY, true) == true -> {

                songIndex = SpUtility.getInstance(applicationContext)?.getCurrentSongIndex() ?: 0
                PlayerConstants.SONG_NUMBER = songIndex

                if (songIndex != -1 && songIndex < songList?.size ?: 0) {
                    //index is in a valid range
                    activeSong = songList?.get(songIndex)
                } else {
                    stopSelf()
                }

                initMediaPlayer()

            }
        }
    }

    private fun registerPlayerControlsBroadCastReceiver() {
        val intentFilter = IntentFilter(MainActivity.ACTION_PLAY)
        registerReceiver(playerControlBroadcastReceiver, intentFilter)
    }

    private val notificationBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action?.equals(Intent.ACTION_MEDIA_BUTTON) == true) {
                val keyEvent = intent.extras?.get(Intent.EXTRA_KEY_EVENT) as KeyEvent
                if (keyEvent.action != KeyEvent.ACTION_DOWN)
                    return

                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->{
                        if (!PlayerConstants.SONG_PAUSED) {
                            pauseMedia()
                        } else {
                            playMedia()
                        }
                        buildNotification()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        Log.d("TAG", "TAG: KEYCODE_MEDIA_NEXT")
                        nextMedia()
                        buildNotification()
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        Log.d("TAG", "TAG: KEYCODE_MEDIA_PREVIOUS")
                        nextMedia()
                        buildNotification()
                    }
                }
            } else {
                when {
                    intent?.action.equals(NOTIFY_PLAY,true) -> {
                        playMedia()
                        buildNotification()
                        notificationControl?.invoke(NOTIFY_PLAY)
                    }
                    intent?.action.equals(NOTIFY_PAUSE,true) -> {
                        pauseMedia()
                        buildNotification()
                        notificationControl?.invoke(NOTIFY_PAUSE)
                    }
                    intent?.action.equals(NOTIFY_NEXT,true) -> {
                        nextMedia()
                        notificationControl?.invoke(NOTIFY_NEXT)
                    }
                    intent?.action.equals(NOTIFY_PREV,true) -> {
                        prevMedia()
                        notificationControl?.invoke(NOTIFY_PREV)
                    }
                }
            }
        }
    }

    private fun registerNotificationBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFY_PLAY)
        intentFilter.addAction(NOTIFY_PAUSE)
        intentFilter.addAction(NOTIFY_PREV)
        intentFilter.addAction(NOTIFY_NEXT)
        registerReceiver(notificationBroadcastReceiver,intentFilter)
    }

    fun buildNotification() {
        val songTitle = songList?.get(songIndex)?.songTitle
        val songArtist = songList?.get(songIndex)?.songArtist
        val albumName = songList?.get(songIndex)?.album

        val smallNotificationView = RemoteViews(applicationContext.packageName, R.layout.music_player_notification)
        val expandedNotificationView = RemoteViews(applicationContext.packageName, R.layout.music_player_notification_expanded)

        val notificationCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        } else {
            NotificationCompat.Builder(applicationContext)
        }

        notificationCompat.setSmallIcon(R.drawable.ic_launcher)

        attachPendingIntent(smallNotificationView)
        attachPendingIntent(expandedNotificationView)

        songList?.get(songIndex)?.albumArtByteArray?.let { bytes ->
            notificationCompat.contentView.setImageViewBitmap(R.id.status_bar_album_art,
                    Utilities.getBitmapFromByteArray(bytes))
            notificationCompat.bigContentView.setImageViewBitmap(R.id.status_bar_album_art,
                    Utilities.getBitmapFromByteArray(bytes))
        }

        notificationCompat.setCustomContentView(smallNotificationView)
        notificationCompat.setCustomBigContentView(expandedNotificationView)

        if (mMediaPlayer?.isPlaying == true) {
            notificationCompat.contentView.setImageViewResource(R.id.status_bar_play,R.drawable.ic_pause_24dp)
            notificationCompat.bigContentView.setImageViewResource(R.id.status_bar_play,R.drawable.ic_pause_24dp)
        } else {
            notificationCompat.contentView.setImageViewResource(R.id.status_bar_play,R.drawable.ic_play_arrow_24dp)
            notificationCompat.bigContentView.setImageViewResource(R.id.status_bar_play,R.drawable.ic_play_arrow_24dp)
        }

        notificationCompat.contentView.setTextViewText(R.id.status_bar_track_name,songTitle)
        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_track_name,songTitle)

        notificationCompat.contentView.setTextViewText(R.id.status_bar_artist_name,songArtist)
        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_artist_name,songArtist)

        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_album_name,albumName)

        //set pending Intent for Notification on Click event
        val launchIntent = Intent(applicationContext,MainActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pLaunchIntent = PendingIntent.getActivity(applicationContext,0,launchIntent,0)
        notificationCompat.setContentIntent(pLaunchIntent)

        notificationCompat.setOngoing(true)

        val notification = notificationCompat.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel()
        }

        getManager()?.notify(NOTIFICATION_ID,notification)
    }

    private fun createChannel() : NotificationChannel {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME,NotificationManager.IMPORTANCE_DEFAULT)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        return channel
    }

    private fun getManager(): NotificationManager? {
        if (notificationManager == null) {
            notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager
    }

    private fun attachPendingIntent(remoteView: RemoteViews) {
        val playIntent = Intent(NOTIFY_PLAY)
        val pauseIntent = Intent(NOTIFY_PAUSE)
        val prevIntent = Intent(NOTIFY_PREV)
        val nextIntent = Intent(NOTIFY_NEXT)

        val pPlay = PendingIntent.getBroadcast(applicationContext, 0,
                playIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_play, pPlay)

        val pPause = PendingIntent.getBroadcast(applicationContext, 0,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_play, pPause)

        val pNext = PendingIntent.getBroadcast(applicationContext, 0,
                nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_next, pNext)

        val pPrevious = PendingIntent.getBroadcast(applicationContext, 0,
                prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.status_bar_prev, pPrevious)

    }

    override fun onDestroy() {
        stopMedia()
        mMediaPlayer?.release()

        //unregister player control broadcast receiver
        unregisterReceiver(playerControlBroadcastReceiver)

        //unregister notification control broadcast receiver
        unregisterReceiver(notificationBroadcastReceiver)

        getManager()?.cancel(NOTIFICATION_ID)
        SpUtility.getInstance(applicationContext)?.clearSharedPreference()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        stopSelf()
    }
}
