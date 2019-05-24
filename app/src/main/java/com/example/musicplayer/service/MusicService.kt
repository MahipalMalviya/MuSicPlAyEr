package com.example.musicplayer.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

/**
 * Created by MAHIPAL-PC on 18-12-2017.
 */

class MusicService : Service() {

    private var mMediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.isLooping = true
        mMediaPlayer?.setVolume(100f, 100f)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mMediaPlayer?.start()
        return START_STICKY
    }

    override fun onStart(intent: Intent, startId: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.stop()
        mMediaPlayer?.release()
    }

}
