package com.example.musicplayer.constants

import android.os.Handler
import com.example.musicplayer.model.Song

object PlayerConstants {

    var SONG_LIST : ArrayList<Song>? = null

    //song number which is playing right now from SONGS_LIST
    var SONG_NUMBER = 0

    //song is playing or paused
    var SONG_PAUSED = true

    //song changed (next, previous)
    var SONG_CHANGED = false

    //handler for song changed(next, previous) defined in service(SongService)
    var SONG_CHANGE_HANDLER: Handler? = null

    //handler for song play/pause defined in service(SongService)
    var PLAY_PAUSE_HANDLER: Handler? = null

    //handler for showing song progress defined in Activities(MainActivity, AudioPlayerActivity)
    var PROGRESSBAR_HANDLER: Handler? = null

    var isShuffle = false

    var isRepeat = false
}