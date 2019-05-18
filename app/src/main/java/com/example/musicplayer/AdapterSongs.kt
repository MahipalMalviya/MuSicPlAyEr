package com.example.musicplayer

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.ArrayList

class AdapterSongs(private val mArrSong: ArrayList<Song>?) :
        RecyclerView.Adapter<AdapterSongs.SongHolder>() {

    private lateinit var txtSongName: TextView
    private lateinit var txtArtistName: TextView
    private lateinit var txtDuration: TextView
    private lateinit var imgSong: ImageView

    var onItemClick: ((Int) -> Unit)? = null

    interface OnSongClickListener {
        fun onSongClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song_list, null)
        return SongHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SongHolder, position: Int) {
        val song = mArrSong?.get(position)

        txtSongName.text = song?.getSongTitle()
        txtArtistName.text = song?.getSongArtist() + "."

        val minutes = song?.getMinute()
        val seconds = song?.getSecond()
        if (seconds?.length == 1) {
            txtDuration.text = "0$minutes:0$seconds"
        } else {
            txtDuration.text = "0$minutes:$seconds"
        }

        imgSong.setImageBitmap(song?.albumArt)

    }

    override fun getItemCount(): Int {
        return mArrSong?.size?: 0
    }

    inner class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {

            txtSongName = itemView.findViewById(R.id.txt_songName)
            txtArtistName = itemView.findViewById(R.id.txt_songArtist)
            txtDuration = itemView.findViewById(R.id.txt_songDuration)
            imgSong = itemView.findViewById(R.id.img_song)

            itemView.setOnClickListener {
                onItemClick?.invoke(adapterPosition)
            }
        }
    }
}
