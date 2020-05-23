package com.example.musicplayer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SpUtility {

    companion object {
        private var spUtility: SpUtility? = null
        private var sharedPreferences: SharedPreferences? = null

        private const val SP_NAME = "SongPreference"

        private const val SONG_INDEX = "SongIndex"
        private const val SONG_LIST = "SongList"
        private const val SONG_SHUFFLE = "SongShuffle"
        private const val SONG_REPEAT = "SongRepeat"
        private const val TOTAL_DURATION = "totalDuration"
        private const val CURRENT_DURATION = "currentDuration"

        fun getInstance(context: Context): SpUtility? {
            if (spUtility == null) {
                spUtility = SpUtility()
                sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            }
            return spUtility
        }
    }

    fun setCurrentSongIndex(songIndex: Int) {
        setKeyVal(SONG_INDEX, songIndex)
    }

    fun getCurrentSongIndex(): Int {
        return sharedPreferences?.getInt(SONG_INDEX, 0) ?: 0
    }

    fun storeSongs(songList: ArrayList<Song>?) {
        if (songList == null) {
            return
        }
        val json = Gson().toJson(songList)
        sharedPreferences?.edit()?.putString(SONG_LIST, json)?.apply()
    }

    fun getSongs(): ArrayList<Song> {
        val json = sharedPreferences?.getString(SONG_LIST, null)

        val type = object : TypeToken<ArrayList<Song>>() {}.type

        return Gson().fromJson(json, type)
    }

    fun setSongShuffle(isShuffleOn: Boolean) {
        setKeyVal(SONG_SHUFFLE, isShuffleOn)
    }

    fun isSongShuffle(): Boolean {
        return sharedPreferences?.getBoolean(SONG_SHUFFLE, false) ?: false
    }

    fun setSongRepeat(isRepeatOn: Boolean) {
        setKeyVal(SONG_REPEAT, isRepeatOn)
    }

    fun isSongRepeat(): Boolean {
        return sharedPreferences?.getBoolean(SONG_REPEAT, false) ?: false
    }

    fun setSongTotalDuration(totalDuration: Int) {
        setKeyVal(TOTAL_DURATION,totalDuration)
    }

    fun getSongTotalDuration(): Long? {
        return sharedPreferences?.getLong(TOTAL_DURATION,0)
    }

    fun setSongCurrentDuration(totalDuration: Int) {
        setKeyVal(CURRENT_DURATION,totalDuration)
    }

    fun getSongCurrentDuration(): Long? {
        return sharedPreferences?.getLong(CURRENT_DURATION,0)
    }

    private fun setKeyVal(key:String,value:Long) {
        sharedPreferences?.edit()?.putLong(key,value)?.apply()
    }

    private fun setKeyVal(key:String,value:Boolean) {
        sharedPreferences?.edit()?.putBoolean(key,value)?.apply()
    }

    private fun setKeyVal(key: String, value: Int) {
        sharedPreferences?.edit()?.putInt(key, value)?.apply()
    }

    fun clearSharedPreference() {
        sharedPreferences?.edit()?.clear()?.apply()
    }
}