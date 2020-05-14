package com.example.musicplayer.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import java.util.ArrayList

/**
 * Created by MAHIPAL-PC on 15-12-2017.
 */

object Utilities {

    fun getMp3Songs(context: Context): ArrayList<Song> {

        var albumArtByteArray: ByteArray? = null

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

    fun milliSecondsToTimer(milliseconds: Long): String {
        var finalTimerString = ""
        val secondsString: String

        // Convert total duration into time
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
        // Add hours if there
        if (hours > 0) {
            finalTimerString = "$hours:"
        }

        // Prepending 0 to seconds if it is one digit
        secondsString = if (seconds < 10) {
            "0$seconds"
        } else {
            "" + seconds
        }

        finalTimerString = "$finalTimerString$minutes:$secondsString"

        // return timer string
        return finalTimerString
    }

    fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        val percentage: Double?

        val currentSeconds = (currentDuration / 1000).toInt().toLong()
        val totalSeconds = (totalDuration / 1000).toInt().toLong()

        // calculating percentage
        percentage = currentSeconds.toDouble() / totalSeconds * 100

        // return percentage
        return percentage.toInt()
    }

    fun progressToTimer(progress: Int, totalDuration: Int): Int {
        var totalDuration = totalDuration
        var currentDuration = 0
        totalDuration /= 1000
        currentDuration = (progress.toDouble() / 100 * totalDuration).toInt()

        // return current duration in milliseconds
        return currentDuration * 1000
    }

    fun showMessage(context: Context, message: String, maxLine: Int) {
        val snackbar = Snackbar.make((context as Activity).findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val view = snackbar.view
        val textView = view.findViewById<TextView>(R.id.snackbar_text)
        textView.maxLines = maxLine
        snackbar.show()
    }

    fun getBitmapFromByteArray(byteArray:ByteArray?): Bitmap? {
        byteArray?.let {
            try {
                val options = BitmapFactory.Options()
                options.inSampleSize = 2
                return  BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun setImageByByteArray(context: Context, byteArray: ByteArray?,imageView: ImageView?) {
        byteArray?.let {
            imageView?.let {
                Glide.with(context)
                        .load(byteArray)
                        .into(imageView)
            }
        }
    }

}
