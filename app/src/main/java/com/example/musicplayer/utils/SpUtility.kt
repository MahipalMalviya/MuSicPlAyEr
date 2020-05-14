package com.example.musicplayer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SpUtility {

    companion object {
        private var spUtility:SpUtility? = null
        private var sharedPreferences: SharedPreferences? = null

        private const val SP_NAME = "SongPreference"

        private const val SONG_INDEX = "SongIndex"
        private const val SONG_LIST = "SongList"

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

    fun storeSongs(songList:ArrayList<Song>) {
        val json = Gson().toJson(songList)
        sharedPreferences?.edit()?.putString(SONG_LIST,json)?.apply()
    }

    fun getSongs(): ArrayList<Song> {
        val json = sharedPreferences?.getString(SONG_LIST,null)

        val type = object :TypeToken<ArrayList<Song>>() {}.type

        return Gson().fromJson(json,type)
    }

    private fun setKeyVal(key: String, value: Int) {
        sharedPreferences?.edit()?.putInt(key,value)?.apply()
    }
}