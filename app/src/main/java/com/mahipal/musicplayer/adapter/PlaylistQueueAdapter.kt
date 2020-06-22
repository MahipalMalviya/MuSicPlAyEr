package com.mahipal.musicplayer.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mahipal.musicplayer.R
import com.mahipal.musicplayer.constants.PlayerConstants
import com.mahipal.musicplayer.model.Song
import com.mahipal.musicplayer.utils.Utilities
import kotlinx.android.synthetic.main.current_play_list.view.*
import java.util.*
import kotlin.collections.ArrayList

class PlaylistQueueAdapter(private var list:ArrayList<Song>?): RecyclerView.Adapter<PlaylistQueueAdapter.PlayListHolder>() {

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
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN || motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                onTouchStartDragging?.invoke(holder)
            }
            return@setOnTouchListener true
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle?.equals(
                        PlayerConstants.SONG_LIST?.get(fromPosition)?.songTitle) == true) {

            PlayerConstants.SONG_NUMBER = toPosition

        } else if (PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle?.equals(
                        PlayerConstants.SONG_LIST?.get(toPosition)?.songTitle) == true) {

            PlayerConstants.SONG_NUMBER = fromPosition
        }
        Collections.swap(list as MutableList<*>,fromPosition,toPosition)
        notifyItemMoved(fromPosition,toPosition)
        PlayerConstants.SONG_LIST = list
    }

    inner class PlayListHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindItem(song: Song?) {
            song?.albumId?.let {
                Glide.with(itemView.context)
                        .load(Utilities.getAlbumart(itemView.context,it))
                        .into(itemView.iv_playlist_album_art)
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

            if (PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle?.equals(song?.songTitle) == true) {
                itemView.tv_playlist_song_name.typeface = Typeface.DEFAULT_BOLD
            } else {
                itemView.tv_playlist_song_name.typeface = Typeface.DEFAULT
            }
        }
    }
}