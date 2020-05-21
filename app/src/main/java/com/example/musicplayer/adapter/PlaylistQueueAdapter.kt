package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.Utilities
import kotlinx.android.synthetic.main.current_play_list.view.*
import java.util.*
import kotlin.collections.ArrayList

class PlaylistQueueAdapter(private val list:ArrayList<Song>?): RecyclerView.Adapter<PlaylistQueueAdapter.PlayListHolder>() {

    var onTouchStartDragging: ((RecyclerView.ViewHolder) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListHolder {
        return PlayListHolder(LayoutInflater.from(parent.context).inflate(R.layout.current_play_list,parent,false))
    }

    override fun getItemCount(): Int {
        return list?.size?:0
    }

    override fun onBindViewHolder(holder: PlayListHolder, position: Int) {

        holder.bindItem(list?.get(position))

        holder.itemView.iv_drag_drop_song?.setOnTouchListener { view, motionEvent ->
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                onTouchStartDragging?.invoke(holder)
            }
            return@setOnTouchListener true
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(list as MutableList<*>,fromPosition,toPosition)
    }

    inner class PlayListHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindItem(song: Song?) {
            song?.albumArtByteArray?.let {
                itemView.iv_playlist_album_art.setImageBitmap(Utilities.getBitmapFromByteArray(it))
            }?: itemView.iv_playlist_album_art.setImageResource(R.drawable.music)

            song?.songTitle?.let {
                itemView.tv_playlist_song_name.text = it
            }?: itemView.tv_playlist_song_name.setText("")

            song?.songArtist?.let {
                itemView.tv_playlist_artist_name.text = it
            }?: itemView.tv_playlist_artist_name.setText("")

            val minutes = song?.minute?:""
            val seconds = song?.second?:""
            if (seconds.length == 1) {
                itemView.tv_playlist_song_duration.text = "0$minutes:0$seconds"
            } else {
                itemView.tv_playlist_song_duration.text = "0$minutes:$seconds"
            }
        }
    }
}