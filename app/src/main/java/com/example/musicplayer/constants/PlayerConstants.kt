package com.example.musicplayer.constants

import android.os.Handler
import com.example.musicplayer.model.Song

object PlayerConstants {

    var SONG_LIST : ArrayList<Song>? = null

    //song number which is playing right now from SONGS_LIST
    var SONG_NUMBER = -1

    //song is playing or paused
    var SONG_PAUSED = true

    //handler for showing song progress defined in Activities(MusicPlayerActivity, AudioPlayerActivity)
    var PROGRESSBAR_HANDLER: Handler? = null

    var isShuffle = false

    var isRepeat = false
}