package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class AdapterSongs extends RecyclerView.Adapter<AdapterSongs.SongHolder>{

    ArrayList<Song> mArrSong;

    public AdapterSongs(ArrayList<Song> mArrSong) {
        this.mArrSong = mArrSong;
    }

    public interface OnSongClickListener{
        void onSongClick(int position);
    }

    private OnSongClickListener mOnSongClickListener;

    public void setOnSongClickListener(OnSongClickListener onSongClickListener){
        mOnSongClickListener = onSongClickListener;
    }

    @Override
    public AdapterSongs.SongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_list,null);
        return new SongHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(AdapterSongs.SongHolder holder, int position) {
        Song song = mArrSong.get(position);

        holder.txtSongName.setText(song.getSongTitle());
        holder.txtArtistName.setText(song.getSongArtist()+".");

        String minutes = song.getMinute();
        String seconds = song.getSecond();
        if (seconds.length() == 1) {
            holder.txtDuration.setText("0" + minutes + ":0" + seconds);
        }else {
            holder.txtDuration.setText("0" + minutes + ":" + seconds);
        }

        holder.imgSong.setImageBitmap(song.albumArt);
    }

    @Override
    public int getItemCount() {
        return mArrSong.size();
    }

    public class SongHolder extends RecyclerView.ViewHolder{

        TextView txtSongName,txtArtistName,txtDuration;
        ImageView imgSong;

        public SongHolder(View itemView) {
            super(itemView);

            txtSongName = itemView.findViewById(R.id.txt_songName);
            txtArtistName = itemView.findViewById(R.id.txt_songArtist);
            txtDuration = itemView.findViewById(R.id.txt_songDuration);
            imgSong = itemView.findViewById(R.id.img_song);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    mOnSongClickListener.onSongClick(position);
                }
            });
        }
    }
}
