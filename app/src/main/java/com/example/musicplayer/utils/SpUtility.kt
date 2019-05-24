package com.example.musicplayer.utils

import android.content.Context
import android.content.SharedPreferences

class SpUtility {

    companion object {
        private var spUtility:SpUtility? = null
        private var sharedPreferences: SharedPreferences? = null

        private const val SP_NAME = "SongPreference"

        private const val SONG_INDEX = "SongIndex"

        fun getInstance(context: Context): SpUtility? {
            if (spUtility == null) {
                spUtility = SpUtility()
                sharedPreferences = context.getSharedPreferences(SP_NAME,Context.MODE_PRIVATE)
            }
            return spUtility
        }
    }

    fun setCurrentSongIndex(songIndex: Int) {
        setKeyVal(SONG_INDEX,songIndex)
    }

    fun getCurrenSongIndex(): Int {
        return sharedPreferences?.getInt(SONG_INDEX,0)?: 0
    }

    private fun setKeyVal(key: String, value: Int) {
        sharedPreferences?.edit()?.putInt(key,value)?.apply()
    }
}