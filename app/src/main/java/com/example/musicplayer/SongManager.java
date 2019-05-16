package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by MAHIPAL-PC on 10-12-2017.
 */

class SongManager {

    private static Bitmap bitmap;


    static ArrayList<Song> getMp3Songs(Context context) {

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
                        byte[] art = metadataRetriever.getEmbeddedPicture();
                        BitmapFactory.Options opt = new BitmapFactory.Options();
                        opt.inSampleSize = 2;
                        if (art != null) {
                            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length, opt);
                        }
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
                        Song song = new Song(song_id, songName, artist_name, fullPath, minutes,seconds, "", bitmap);
                        arrayList.add(song);
                    }

                } while (cursor.moveToNext());
                cursor.close();
            }
        }
        return arrayList;
    }
}
