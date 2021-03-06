package com.mahipal.musicplayer.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.mahipal.musicplayer.R
import com.mahipal.musicplayer.constants.PlayerConstants
import com.mahipal.musicplayer.model.Song
import com.mahipal.musicplayer.utils.Utilities
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

            val bitmap = Utilities.getAlbumart(itemView.context,song?.albumId)
            Glide.with(itemView.context)
                    .load(bitmap)
                    .placeholder(R.drawable.music)
                    .into(itemView.img_song)

            if (PlayerConstants.SONG_NUMBER != -1 && PlayerConstants.SONG_LIST?.
                            get(PlayerConstants.SONG_NUMBER)?.songTitle?.equals(song?.songTitle) == true) {
                itemView.txt_songName.typeface = Typeface.DEFAULT_BOLD
            } else {
                itemView.txt_songName.typeface = Typeface.DEFAULT
            }
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
