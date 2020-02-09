package com.example.musicplayer.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore

import com.example.musicplayer.model.Song

import java.util.ArrayList

/**
 * Created by MAHIPAL-PC on 10-12-2017.
 */

object SongManager {

    private var albumArtByteArray: ByteArray? = null


    fun getMp3Songs(context: Context): ArrayList<Song> {

        val arrayList = ArrayList<Song>()

        val musicResolver = context.contentResolver

        val allSongsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
        val sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER

        val cursor = musicResolver.query(allSongsUri, null, selection, null, sort)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val songId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))

                    val fullPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))

                    val metadataRetriever = MediaMetadataRetriever()
                    metadataRetriever.setDataSource(fullPath)

                    try {
                        albumArtByteArray = metadataRetriever.embeddedPicture
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val songTime = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                    var minutes = ""
                    var seconds = ""
                    try {
                        val dur = java.lang.Long.parseLong(songTime)
                        minutes = (dur / 60000).toString()
                        seconds = (dur % 60000 / 1000).toString()
                    } catch (nfe: NumberFormatException) {
                        nfe.printStackTrace()
                    }

                    metadataRetriever.release()

                    val songName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val ext = songName.substring(songName.lastIndexOf(".") + 1)

                    if (ext == "mp3" || ext == "MP3") {
                        val song = Song(songId, songName, artistName, fullPath, minutes, seconds, "", albumArtByteArray)
                        arrayList.add(song)
                    }

                } while (cursor.moveToNext())
                cursor.close()
            }
        }
        return arrayList
    }
}
