package com.mahipal.musicplayer.constants

import android.os.Handler
import com.mahipal.musicplayer.model.Song

object PlayerConstants {

    var SONG_LIST : ArrayList<Song>? = null

    var SONG_NUMBER = -1

    var SONG_PAUSED = true

    var PROGRESSBAR_HANDLER: Handler? = null

    var SONG_CHANGE_HANDLER: Handler? = null

    var PLAY_PAUSE_HANDLER: Handler? = null

    var isShuffle = false

    var isRepeat = false
}