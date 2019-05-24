package com.example.musicplayer.model;

import android.graphics.Bitmap;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by MAHIPAL-PC on 04-12-2017.
 */

public class Song implements Serializable{
    private long songId;
    private String songTitle;
    private String songArtist;
    private String path;
    private String minute;
    private String second;
    private String album;
    private byte[] albumArtByteArray;

    public Song(long songId, String songTitle, String songArtist, String path, String minute,String second, String album, byte[] albumArtByteArray) {
        this.songId = songId;
        this.songTitle = songTitle;
        this.songArtist = songArtist;
        this.path = path;
        this.minute = minute;
        this.second = second;
        this.album = album;
        this.albumArtByteArray = albumArtByteArray;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public void setSongArtist(String songArtist) {
        this.songArtist = songArtist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public byte[] getAlbumArtByteArray() {
        return albumArtByteArray;
    }

    public void setAlbumArtByteArray(byte[] albumArtByteArray) {
        this.albumArtByteArray = albumArtByteArray;
    }

    @Override
    public String toString() {
        return "Song{" +
                "songId=" + songId +
                ", songTitle='" + songTitle + '\'' +
                ", songArtist='" + songArtist + '\'' +
                ", path='" + path + '\'' +
                ", minute='" + minute + '\'' +
                ", second='" + second + '\'' +
                ", album='" + album + '\'' +
                ", albumArtByteArray=" + Arrays.toString(albumArtByteArray) +
                '}';
    }
}
