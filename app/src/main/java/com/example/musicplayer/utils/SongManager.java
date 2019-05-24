package com.example.musicplayer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.musicplayer.model.Song;

import java.util.ArrayList;

/**
 * Created by MAHIPAL-PC on 10-12-2017.
 */

public class SongManager {

    private static byte[] albumArtByteArray;


    public static ArrayList<Song> getMp3Songs(Context context) {

        ArrayList<Song> arrayList = new ArrayList<>();

        ContentResolver musicResolver = context.getContentResolver();

        Uri allSongsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        String sort = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        Cursor cursor = musicResolver.query(allSongsUri, null, selection, null, sort);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long song_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    String artist_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                    String fullPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                    metadataRetriever.setDataSource(fullPath);

                    try {
                        albumArtByteArray = metadataRetriever.getEmbeddedPicture();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String songTime = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                    String minutes="";
                    String seconds="";
                    try {
                        long dur = Long.parseLong(songTime);
                        minutes = String.valueOf(dur / 60000);
                        seconds = String.valueOf((dur % 60000) / 1000);
                    } catch (NumberFormatException nfe){
                        nfe.printStackTrace();
                    }
                    metadataRetriever.release();

                    String songName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    String ext = songName.substring(songName.lastIndexOf(".") + 1);

                    if (ext.equals("mp3") || ext.equals("MP3")) {
                        Song song = new Song(song_id, songName, artist_name, fullPath, minutes,seconds, "", albumArtByteArray);
                        arrayList.add(song);
                    }

                } while (cursor.moveToNext());
                cursor.close();
            }
        }
        return arrayList;
    }
}
