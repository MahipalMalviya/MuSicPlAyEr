package com.mahipal.musicplayer.utils

import android.content.Context
import com.mahipal.musicplayer.R
import com.mahipal.musicplayer.constants.PlayerConstants
import com.mahipal.musicplayer.constants.PlayerConstants.SONG_CHANGE_HANDLER
import com.mahipal.musicplayer.service.MusicService


object Controls {

    fun playControl(context: Context) {
        sendMessage(context.resources.getString(R.string.play))
    }

    fun pauseControl(context: Context) {
        sendMessage(context.resources.getString(R.string.pause))
    }

    fun nextControl(context: Context) {
        val isServiceRunning = Utilities.isServiceRunning(MusicService::class.java.name, context)
        if (!isServiceRunning)
            return
        if (PlayerConstants.SONG_LIST?.size?:0 > 0) {
            if (PlayerConstants.SONG_NUMBER < PlayerConstants.SONG_LIST?.size?:0 - 1) {
                PlayerConstants.SONG_NUMBER++
                SONG_CHANGE_HANDLER?.let { handler ->
                    handler.sendMessage(handler.obtainMessage())
                }
            } else {
                PlayerConstants.SONG_NUMBER = 0
                SONG_CHANGE_HANDLER?.let { handler ->
                    handler.sendMessage(handler.obtainMessage())
                }
            }
        }
        SpUtility.getInstance(context)?.setCurrentSongIndex(PlayerConstants.SONG_NUMBER)
        PlayerConstants.SONG_PAUSED = false
    }

    fun previousControl(context: Context) {
        val isServiceRunning = Utilities.isServiceRunning(MusicService::class.java.name, context)
        if (!isServiceRunning)
            return
        if (PlayerConstants.SONG_LIST?.size?:0 > 0) {
            if (PlayerConstants.SONG_NUMBER > 0) {
                PlayerConstants.SONG_NUMBER--
                SONG_CHANGE_HANDLER?.let { handler ->
                    handler.sendMessage(handler.obtainMessage())
                }
            } else {
                PlayerConstants.SONG_NUMBER = PlayerConstants.SONG_LIST?.size?:0 - 1
                SONG_CHANGE_HANDLER?.let { handler ->
                    handler.sendMessage(handler.obtainMessage())
                }
            }
        }
        SpUtility.getInstance(context)?.setCurrentSongIndex(PlayerConstants.SONG_NUMBER)
        PlayerConstants.SONG_PAUSED = false
    }

    private fun sendMessage(message: String) {
        try {
            PlayerConstants.PLAY_PAUSE_HANDLER?.let { handler ->
                handler.sendMessage(handler.obtainMessage(0,message))
            }
        }catch (ex:Exception) {
            ex.printStackTrace()
        }
    }

}