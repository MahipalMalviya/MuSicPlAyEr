package com.example.musicplayer.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.example.musicplayer.callbacks.MediaPlayerControls
import android.os.Binder
import com.example.musicplayer.model.Song
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.media.session.MediaSessionManager
import com.example.musicplayer.utils.SpUtility
import kotlin.collections.ArrayList
import android.media.MediaPlayer.MetricsConstants.PLAYING
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.RemoteException


/**
 * Created by MAHIPAL-PC on 18-12-2017.
 */

class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayerControls {

    private val LOG_TAG = MusicService::class.java.simpleName

    private var mMediaPlayer: MediaPlayer? = null

    private var songList: ArrayList<Song>? = null
    private var songIndex = -1
    private var activeSong: Song? = null

    //MediaSession
    private val mediaSessionManager: MediaSessionManager? = null
    private val mediaSession: MediaSessionCompat? = null
    private val transportControls: MediaControllerCompat.TransportControls? = null

    companion object {
        //AudioPlayer notification ID
        private const val NOTIFICATION_ID = 101

        const val MEDIA_FILE = "media"

        const val ACTION_PLAY = "com.mahipal.musicplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.mahipal.musicplayer.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.mahipal.musicplayer.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.mahipal.musicplayer.ACTION_PAUSE"
        const val ACTION_STOP = "com.mahipal.musicplayer.ACTION_STOP"
    }

    // Binder given to the clients
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        initMediaPlayer()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        try {
            songList = SpUtility.getInstance(applicationContext)?.getSongs()
            songIndex = SpUtility.getInstance(applicationContext)?.getCurrenSongIndex()?:0

            if (songIndex != -1 && songIndex < songList?.size?:0) {
                activeSong = songList?.get(songIndex)
            } else {
                stopSelf()
            }

            mMediaPlayer?.stop()
            mMediaPlayer?.reset()

            mMediaPlayer?.setDataSource(activeSong?.path)
            playMedia()

        } catch (ex:NullPointerException) {
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
//                initMediaSession()
//                initMediaPlayer()
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }

//            buildNotification(PlaybackStatus.PLAYING)
        }

        //Handle Intent action from MediaSession.TransportControls
//        handleIncomingActions(intent);

        return START_STICKY
    }

    override fun onStart(intent: Intent, startId: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()

        stopMedia()
        mMediaPlayer?.release()
    }

    inner class LocalBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.setOnCompletionListener(this)
        mMediaPlayer?.setOnPreparedListener(this)

        if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
        }
        mMediaPlayer?.reset()
    }

    override fun onCompletion(mp: MediaPlayer?) {

    }

    override fun onPrepared(mp: MediaPlayer?) {
        playMedia()
    }

    override fun playMedia() {
        if (mMediaPlayer?.isPlaying == false) {
            mMediaPlayer?.start()
        }
    }

    override fun pauseMedia() {
        if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.pause()
        }
    }

    override fun stopMedia() {
        if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
        }
    }

    override fun nextMedia() {

    }

    override fun prevMedia() {

    }

    fun buildNotification() {

    }

}
