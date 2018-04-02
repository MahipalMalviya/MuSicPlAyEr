package com.example.musicplayer;

import android.graphics.Bitmap;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by MAHIPAL-PC on 04-12-2017.
 */

public class Song implements Serializable{
    public long songId;
    public String songTitle;
    public String songArtist;
    public String path;
    public String minute;
    public String second;
    public String album;
    public Bitmap albumArt;

    public Song() {

    }

    public Song(long songId, String songTitle, String songArtist, String path, String minute,String second, String album, Bitmap albumArt) {
        this.songId = songId;
        this.songTitle = songTitle;
        this.songArtist = songArtist;
        this.path = path;
        this.minute = minute;
        this.second = second;
        this.album = album;
        this.albumArt = albumArt;
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

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    public String toString() {
        return String.format("songId: %d, Title: %s, Artist: %s, Path: %s, Genere: %d, minute: %s, second: %s ",
                songId, songTitle, songArtist, path,  minute, second);
    }
}
