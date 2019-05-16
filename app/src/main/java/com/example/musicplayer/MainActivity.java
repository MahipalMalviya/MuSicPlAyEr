package com.example.musicplayer;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    public static final int REQUEST_CODE_PERMISSION = 1001;

    private String[] permissionList = { Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE };

    private RecyclerView mRecyclerViewSongs;
    private ArrayList<Song> mArrSongs;
    private SlidingUpPanelLayout mSlidingUpLayout;
    private RelativeLayout mRelative;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler;

    //On slidingLayout
    private ImageView mImgPlayingSong;
    private TextView mTxtPlayingSongName;
    private ImageButton mImgBtnPlayOnSlideLay;

    //In SlidingLayout
    private ImageView mImgCurrentPlaySong;
    private ImageButton mImgBtnShuffle, mImgBtnPlay, mImgBtnNext, mImgBtnPrevious, mImgBtnRepeat;
    private TextView mTxtSongPlayCurrDuration, mTxtSongPlayTime;
    private SeekBar mSeekBarPlaySong;

    private int currentSongIndex = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();

        setContentView(R.layout.activity_main);

        if (!checkPermissionGrantedOrNot()){
            requestAppPermissions();
        }

//        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Toolbar mToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        initRecyclerView();

        getId();
        setListener();

        mHandler = new Handler();
        mMediaPlayer = new MediaPlayer();

        mSeekBarPlaySong.setOnSeekBarChangeListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        mArrSongs = new ArrayList<>();

        final Runnable uiRunnable = new Runnable() {
            @Override
            public void run() {
                AdapterSongs mAdapterSongs = new AdapterSongs(mArrSongs, new AdapterSongs.OnSongClickListener() {
                    @Override
                    public void onSongClick(int position) {
                        currentSongIndex = position;
                        playSong(position);
                    }
                });
                mRecyclerViewSongs.setAdapter(mAdapterSongs);
            }
        };

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mArrSongs = SongManager.getMp3Songs(MainActivity.this);
                runOnUiThread(uiRunnable);
            }
        });
        thread.start();

        if (!mArrSongs.isEmpty()) {

            mSlidingUpLayout.addPanelSlideListener(new PanelSlideListener() {

                @Override
                public void onPanelSlide(View panel, float slideOffset) {

                    if (slideOffset > 0.88) {
                        mRelative.setBackgroundColor(getResources().getColor(R.color.whiteTransparent));

                        mImgBtnPlayOnSlideLay.setVisibility(View.INVISIBLE);
                        changeMusicAlbumArt(currentSongIndex);
                        mImgCurrentPlaySong.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                    if (slideOffset < 0.09) {
                        mImgBtnPlayOnSlideLay.setVisibility(View.VISIBLE);
                        mImgCurrentPlaySong.setImageResource(R.drawable.music);
                        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
                    }
                }

                @Override
                public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {

                }
            });
        }
    }

    private boolean checkPermissionGrantedOrNot() {
        for (String s : permissionList) {
            int res = checkCallingOrSelfPermission(s);
            return (res != PackageManager.PERMISSION_GRANTED);
        }
        return false;
    }

    private void requestAppPermissions() {
        //check which permission granted
        List<String> listPermissionNeeded = new ArrayList<>();
        for (String permission: permissionList){
            if (ContextCompat.checkSelfPermission(MainActivity.this,permission) != PackageManager.PERMISSION_GRANTED){
                listPermissionNeeded.add(permission);
            }
        }

        //ask for non-permission granted
        if (!listPermissionNeeded.isEmpty()){
            ActivityCompat.requestPermissions(MainActivity.this,
                    listPermissionNeeded.toArray(new String[listPermissionNeeded.size()]),REQUEST_CODE_PERMISSION);
            return;
        }
    }

    private void initRecyclerView() {
        mRecyclerViewSongs = findViewById(R.id.recyclerViewSongList);
        mRecyclerViewSongs.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false));
    }

    private void getId() {
        try {
            mRelative = findViewById(R.id.relative);
            mSlidingUpLayout = findViewById(R.id.sliding_layout);

            //on SlideUpPanel Layout
            mImgPlayingSong = findViewById(R.id.img_playing_song);
            mTxtPlayingSongName = findViewById(R.id.txt_playing_songName);
            mImgBtnPlayOnSlideLay = findViewById(R.id.imgBtn_play);

            //In slidePanel Layout
            mImgCurrentPlaySong = findViewById(R.id.img_play_song);
            mImgBtnShuffle = findViewById(R.id.btn_song_shuffle_play);
            mImgBtnRepeat = findViewById(R.id.btn_song_repeat_play);
            mImgBtnPlay = findViewById(R.id.btn_song_play);
            mImgBtnNext = findViewById(R.id.btn_song_next);
            mImgBtnPrevious = findViewById(R.id.btn_song_previous);
            mSeekBarPlaySong = findViewById(R.id.seekBar_play_song);
            mTxtSongPlayTime = findViewById(R.id.txt_song_TotalTime);
            mTxtSongPlayCurrDuration = findViewById(R.id.txt_song_Curr_Time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setListener() {
        try {
            mImgBtnPlayOnSlideLay.setOnClickListener(this);
            mImgBtnPlay.setOnClickListener(this);
            mImgBtnNext.setOnClickListener(this);
            mImgBtnPrevious.setOnClickListener(this);
            mImgBtnShuffle.setOnClickListener(this);
            mImgBtnRepeat.setOnClickListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.imgBtn_play:
                try {
                    if (mMediaPlayer.isPlaying()) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.pause();
                            mImgBtnPlayOnSlideLay.setImageResource(R.drawable.ic_action_play);
                            mImgBtnPlay.setImageResource(R.drawable.ic_action_play);
                        }
                    } else {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.start();
                            mImgBtnPlayOnSlideLay.setImageResource(R.drawable.ic_action_pause);
                            mImgBtnPlay.setImageResource(R.drawable.ic_action_pause);
                        }
                    }
                } catch (IllegalStateException iStateException) {
                    iStateException.printStackTrace();
                }
                break;

            case R.id.btn_song_play:
                try {
                    if (mMediaPlayer.isPlaying()) {

                        if (mMediaPlayer != null) {
                            mMediaPlayer.pause();
                            mImgBtnPlay.setImageResource(R.drawable.ic_action_play);
                            mImgBtnPlayOnSlideLay.setImageResource(R.drawable.ic_action_play);
                        }
                    } else {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.start();
                            mImgBtnPlay.setImageResource(R.drawable.ic_action_pause);
                            mImgBtnPlayOnSlideLay.setImageResource(R.drawable.ic_action_pause);
                        }
                    }
                } catch (IllegalStateException iStateException) {
                    iStateException.printStackTrace();
                }
                break;

            case R.id.btn_song_next:

                if (currentSongIndex < (mArrSongs.size() - 1)) {
                    playSong(currentSongIndex + 1);
                    changeMusicAlbumArt(currentSongIndex + 1);
                    currentSongIndex = currentSongIndex + 1;

                } else {
                    playSong(currentSongIndex);
                }
                break;

            case R.id.btn_song_previous:

                if (currentSongIndex > 0) {
                    playSong(currentSongIndex - 1);
                    changeMusicAlbumArt(currentSongIndex - 1);
                    currentSongIndex = currentSongIndex - 1;

                } else {
                    // play last song
                    playSong(mArrSongs.size() - 1);
                    currentSongIndex = mArrSongs.size() - 1;
                }
                break;
            case R.id.btn_song_shuffle_play:

                if (isShuffle) {
                    isShuffle = false;
                    Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                    mImgBtnShuffle.setImageResource(R.drawable.ic_action_shuffle_off);
                } else {
                    // make repeat to true
                    isShuffle = true;
                    Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isRepeat = false;
                    mImgBtnShuffle.setImageResource(R.drawable.ic_action_shuffle_on);
                    mImgBtnRepeat.setImageResource(R.drawable.ic_action_repeat_off);
                }
                break;

            case R.id.btn_song_repeat_play:

                if (isRepeat) {
                    isRepeat = false;
                    Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                    mImgBtnRepeat.setImageResource(R.drawable.ic_action_repeat_off);
                } else {
                    // make repeat to true
                    isRepeat = true;
                    Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isShuffle = false;
                    mImgBtnRepeat.setImageResource(R.drawable.ic_action_repeat_on);
                    mImgBtnShuffle.setImageResource(R.drawable.ic_action_shuffle_off);
                }
                break;
        }
    }

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            long totalDuration = mMediaPlayer.getDuration();
            long currentDuration = mMediaPlayer.getCurrentPosition();

            // Displaying Total Duration time
            mTxtSongPlayTime.setText("" + Utilities.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            mTxtSongPlayCurrDuration.setText("" + Utilities.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (Utilities.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            mSeekBarPlaySong.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mMediaPlayer.getDuration();
        int currentPosition = Utilities.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mMediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    public void playSong(final int songIndex) {
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();

            mMediaPlayer.setDataSource(mArrSongs.get(songIndex).getPath());
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();

                    mMediaPlayer.setPlaybackParams(new PlaybackParams().allowDefaults().setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT));
                    mImgPlayingSong.setImageBitmap(mArrSongs.get(songIndex).getAlbumArt());
                    mTxtPlayingSongName.setText(mArrSongs.get(songIndex).getSongTitle());
                    mImgBtnPlayOnSlideLay.setImageResource(R.drawable.ic_action_pause);
                    mImgBtnPlay.setImageResource(R.drawable.ic_action_pause);

                    mSeekBarPlaySong.setProgress(0);
                    mSeekBarPlaySong.setMax(100);

                    updateProgressBar();
                }
            });
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException iState) {
            iState.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // check for repeat is ON or OFF
        if (isRepeat) {
            // repeat is on play same song again
            playSong(currentSongIndex);

        } else if (isShuffle) {
            // shuffle is on - play a random song
            Random rand = new Random();
            currentSongIndex = rand.nextInt((mArrSongs.size() - 1) + 1);
            playSong(currentSongIndex);
            //set albumArt and change statusBar color when changed song
            changeMusicAlbumArt(currentSongIndex);
        } else {
            // no repeat or shuffle ON - play next song
            if (currentSongIndex < (mArrSongs.size() - 1)) {
                playSong(currentSongIndex + 1);
                //set albumArt and change statusBar color when changed song
                changeMusicAlbumArt(currentSongIndex + 1);
                currentSongIndex = currentSongIndex + 1;
            } else {
                // play first song
                playSong(0);
                currentSongIndex = 0;
            }
        }
    }

    public void changeMusicAlbumArt(int currentSongIndex) {

        mImgCurrentPlaySong.setImageBitmap(mArrSongs.get(currentSongIndex).getAlbumArt());

        Palette.from(mArrSongs.get(currentSongIndex).getAlbumArt()).generate(new Palette.PaletteAsyncListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                if (vibrantSwatch != null) {
                    getWindow().setStatusBarColor(vibrantSwatch.getRgb());
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mSlidingUpLayout != null &&
                (mSlidingUpLayout.getPanelState() == PanelState.EXPANDED || mSlidingUpLayout.getPanelState() == PanelState.ANCHORED)) {
            mSlidingUpLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Gather permission grant results
        if (requestCode == REQUEST_CODE_PERMISSION) {
            int deniedCount = 0;
            for (int granted : grantResults) {
                //add only permissions which are denied
                if (granted == PackageManager.PERMISSION_DENIED) {
                    deniedCount++;
                }
            }
            if (deniedCount > 0) {
                requestAppPermissions();
            }
        }
    }
}
