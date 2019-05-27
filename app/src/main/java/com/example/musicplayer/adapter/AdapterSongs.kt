package com.example.musicplayer.adapter

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import java.util.ArrayList
import kotlinx.android.synthetic.main.item_song_list.view.*

class AdapterSongs(private val mArrSong: ArrayList<Song>?) :
        RecyclerView.Adapter<AdapterSongs.SongHolder>() {

    private lateinit var txtSongName: TextView
    private lateinit var txtArtistName: TextView
    private lateinit var txtDuration: TextView
    private lateinit var imgSong: ImageView

    var onItemClick: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song_list, null)
        return SongHolder(view)
    }

    override fun onBindViewHolder(holder: SongHolder, position: Int) {
        val song = mArrSong?.get(holder.adapterPosition)

        holder.bindSongs(song)
    }

    override fun getItemCount(): Int {
        return mArrSong?.size?: 0
    }

    inner class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindSongs(song: Song?) {
            song?.songTitle?.let {
                itemView.txt_songName.text = it
            }
            song?.songArtist?.let {
                itemView.txt_songArtist.text = "$it."
            }

            val minutes = song?.minute
            val seconds = song?.second
            if (seconds?.length == 1) {
                itemView.txt_songDuration.text = "0$minutes:0$seconds"
            } else {
                itemView.txt_songDuration.text = "0$minutes:$seconds"
            }

            Glide.with(itemView.context)
                    .load(song?.albumArtByteArray)
                    .placeholder(R.drawable.music)
                    .into(itemView.img_song)

        }

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
