package com.example.musicplayer.service

import android.annotation.TargetApi
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.*
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.musicplayer.R
import com.example.musicplayer.activity.MusicPlayerActivity
import com.example.musicplayer.callbacks.MediaPlayerControls
import com.example.musicplayer.constants.PlayerConstants
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities
import java.util.*


/**
 * Created by MAHIPAL-PC on 18-12-2017.
 */

class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayerControls, AudioManager.OnAudioFocusChangeListener {

    private val LOG_TAG = MusicService::class.java.simpleName

    private var timer: Timer? = null

    private var songList: ArrayList<Song>? = null
    private var songIndex = -1
    private var activeSong: Song? = null
    private var notificationManager: NotificationManager? = null

    //MediaSession
    private val mediaSessionManager: MediaSessionManager? = null
    private val mediaSession: MediaSessionCompat? = null
    private val transportControls: MediaControllerCompat.TransportControls? = null
    private var audioManager: AudioManager? = null

    //Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    companion object {
        var notificationControl: ((String) -> Unit)? = null
        var updateUiOnMediaAction: (() -> Unit)? = null

        var mMediaPlayer: MediaPlayer? = null

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
        timer = Timer()

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener()
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        reqisterNoisyReceiver()

        //register player control broadcast receiver
        registerPlayerControlsBroadCastReceiver()

        //register notification control broadcast receiver
        registerNotificationBroadcastReceiver()
    }

    private inner class MainTask : TimerTask() {
        override fun run() {
            handler.sendEmptyMessage(0)
        }
    }

    private val handler = Handler(Handler.Callback { _ ->
        if (mMediaPlayer != null) {

            val totalDuration = mMediaPlayer?.duration?.toLong() ?: 0
            val currentDuration = mMediaPlayer?.currentPosition?.toLong() ?: 0

            val longArray = LongArray(2)
            longArray[0] = currentDuration
            longArray[1] = totalDuration

            try {
                PlayerConstants.PROGRESSBAR_HANDLER?.sendMessage(PlayerConstants.PROGRESSBAR_HANDLER!!.obtainMessage(0, longArray))
            } catch (ex: Exception) {
            }
        }
        true
    })

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

    override fun onAudioFocusChange(foucsState: Int) {
        when (foucsState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer()
                else if (mMediaPlayer?.isPlaying == false) mMediaPlayer?.start()
                mMediaPlayer?.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer?.isPlaying == true) mMediaPlayer?.stop()
                mMediaPlayer?.release()
                mMediaPlayer = null
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer?.isPlaying == true) mMediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer?.isPlaying == true) mMediaPlayer?.setVolume(0.1f, 0.1f)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager?.requestAudioFocus(createAudioRequestFocus())
        } else {
            audioManager?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true
        }
        return false
    }

    private fun removeAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                    audioManager?.abandonAudioFocusRequest(createAudioRequestFocus())
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                    audioManager?.abandonAudioFocus(this)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createAudioRequestFocus(): AudioFocusRequest {
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .build()
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
    }

    override fun onPrepared(mp: MediaPlayer?) {
        playMedia()
        timer?.scheduleAtFixedRate(MainTask(), 0, 100)
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
            songIndex = songList?.size?.minus(1) ?: 0
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
            playerAction?.equals(MusicPlayerActivity.ACTION_PLAY_NEW_AUDIO, true) == true -> {

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

            playerAction?.equals(MusicPlayerActivity.ACTION_PLAY, true) == true -> {
                playMedia()
                buildNotification()
                updateUiOnMediaAction?.invoke()
            }

            playerAction?.equals(MusicPlayerActivity.ACTION_PAUSE, true) == true -> {
                pauseMedia()
                buildNotification()
                updateUiOnMediaAction?.invoke()
            }

            playerAction?.equals(MusicPlayerActivity.ACTION_NEXT, true) == true -> {
                nextMedia()
                updateUiOnMediaAction?.invoke()
            }

            playerAction?.equals(MusicPlayerActivity.ACTION_PREV, true) == true -> {
                prevMedia()
                updateUiOnMediaAction?.invoke()
            }
        }
    }

    private fun registerPlayerControlsBroadCastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(MusicPlayerActivity.ACTION_PLAY_NEW_AUDIO)
        intentFilter.addAction(MusicPlayerActivity.ACTION_PLAY)
        intentFilter.addAction(MusicPlayerActivity.ACTION_PAUSE)
        intentFilter.addAction(MusicPlayerActivity.ACTION_NEXT)
        intentFilter.addAction(MusicPlayerActivity.ACTION_PREV)
        registerReceiver(playerControlBroadcastReceiver, intentFilter)
    }

    private val notificationBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action?.equals(Intent.ACTION_MEDIA_BUTTON) == true) {
                val keyEvent = intent.extras?.get(Intent.EXTRA_KEY_EVENT) as KeyEvent
                if (keyEvent.action != KeyEvent.ACTION_DOWN)
                    return

                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
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
                    intent?.action.equals(NOTIFY_PLAY, true) -> {
                        playMedia()
                        buildNotification()
                        notificationControl?.invoke(NOTIFY_PLAY)
                    }
                    intent?.action.equals(NOTIFY_PAUSE, true) -> {
                        pauseMedia()
                        buildNotification()
                        notificationControl?.invoke(NOTIFY_PAUSE)
                    }
                    intent?.action.equals(NOTIFY_NEXT, true) -> {
                        nextMedia()
                        notificationControl?.invoke(NOTIFY_NEXT)
                    }
                    intent?.action.equals(NOTIFY_PREV, true) -> {
                        prevMedia()
                        notificationControl?.invoke(NOTIFY_PREV)
                    }
                }
            }
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia()
            buildNotification()
        }
    }

    private fun reqisterNoisyReceiver() {
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, intentFilter)
    }

    private fun callStateListener() {
        // Get the telephony manager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        //Starting listening for PhoneState changes
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        if (mMediaPlayer != null) {
                            pauseMedia()
                            ongoingCall = true
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Phone idle. Start playing.
                        if (mMediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                playMedia()
                            }
                        }
                    }
                }
            }
        }

        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager?.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun registerNotificationBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFY_PLAY)
        intentFilter.addAction(NOTIFY_PAUSE)
        intentFilter.addAction(NOTIFY_PREV)
        intentFilter.addAction(NOTIFY_NEXT)
        registerReceiver(notificationBroadcastReceiver, intentFilter)
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

        notificationCompat.setSmallIcon(R.drawable.music)

        attachPendingIntent(smallNotificationView)
        attachPendingIntent(expandedNotificationView)

        songList?.get(songIndex)?.albumArtByteArray?.let { bytes ->
            notificationCompat.contentView.setImageViewBitmap(R.id.status_bar_album_art,
                    Utilities.getBitmapFromByteArray(bytes))

            notificationCompat.contentView.setImageViewBitmap(R.id.ll_notification,
                    Utilities.getBitmapFromByteArray(bytes))

            notificationCompat.bigContentView.setImageViewBitmap(R.id.status_bar_album_art,
                    Utilities.getBitmapFromByteArray(bytes))

            notificationCompat.bigContentView.setImageViewBitmap(R.id.ll_notification,
                    Utilities.getBitmapFromByteArray(bytes))
        }

        notificationCompat.setCustomContentView(smallNotificationView)
        notificationCompat.setCustomBigContentView(expandedNotificationView)

        if (mMediaPlayer?.isPlaying == true) {
            notificationCompat.contentView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_24dp)
            notificationCompat.bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_24dp)
        } else {
            notificationCompat.contentView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_arrow_24dp)
            notificationCompat.bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_arrow_24dp)
        }

        notificationCompat.contentView.setTextViewText(R.id.status_bar_track_name, songTitle)
        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_track_name, songTitle)

        notificationCompat.contentView.setTextViewText(R.id.status_bar_artist_name, songArtist)
        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_artist_name, songArtist)

        notificationCompat.bigContentView.setTextViewText(R.id.status_bar_album_name, albumName)

        //set pending Intent for Notification on Click event
        val launchIntent = Intent(applicationContext, MusicPlayerActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pLaunchIntent = PendingIntent.getActivity(applicationContext, 0, launchIntent, 0)
        notificationCompat.setContentIntent(pLaunchIntent)

        notificationCompat.setOngoing(true)

        val notification = notificationCompat.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        getManager()?.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(): NotificationChannel {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
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
        timer?.cancel()

        notificationControl = null
        updateUiOnMediaAction = null
        stopMedia()
        mMediaPlayer?.release()
        mMediaPlayer = null

        removeAudioFocus()
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }

        //unregister Noisy BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver)

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
