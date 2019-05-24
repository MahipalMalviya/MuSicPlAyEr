package com.example.musicplayer.activity


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.musicplayer.R
import com.example.musicplayer.utils.SongManager
import com.example.musicplayer.adapter.AdapterSongs
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Random


class MainActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private val permissionList = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)


    private var mRecyclerViewSongs: RecyclerView? = null
    private var mArrSongs: ArrayList<Song>? = null
    private var mSlidingUpLayout: SlidingUpPanelLayout? = null
    private var mRelative: RelativeLayout? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mHandler: Handler? = null

    //On slidingLayout
    private var mImgPlayingSong: ImageView? = null
    private var mTxtPlayingSongName: TextView? = null
    private var mImgBtnPlayOnSlideLay: ImageButton? = null

    //In SlidingLayout
    private var mImgCurrentPlaySong: ImageView? = null
    private var mImgBtnShuffle: ImageButton? = null
    private var mImgBtnPlay: ImageButton? = null
    private var mImgBtnNext: ImageButton? = null
    private var mImgBtnPrevious: ImageButton? = null
    private var mImgBtnRepeat: ImageButton? = null
    private var mTxtSongPlayCurrDuration: TextView? = null
    private var mTxtSongPlayTime: TextView? = null
    private var mSeekBarPlaySong: SeekBar? = null
    private var mCardViewProgressBar: ProgressBar? = null

    private var currentSongIndex = 0
    private var isShuffle = false
    private var isRepeat = false

    companion object {
        const val REQUEST_CODE_PERMISSION = 1001
    }


    private val mUpdateTimeTask = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            val totalDuration = mMediaPlayer?.duration?.toLong()?:0
            val currentDuration = mMediaPlayer?.currentPosition?.toLong()?:0

            // Displaying Total Duration time
            mTxtSongPlayTime?.text = "" + Utilities.milliSecondsToTimer(totalDuration)
            // Displaying time completed playing
            mTxtSongPlayCurrDuration?.text = "" + Utilities.milliSecondsToTimer(currentDuration)

            // Updating progress bar
            val progress = Utilities.getProgressPercentage(currentDuration, totalDuration)
            //Log.d("Progress", ""+progress);
            mSeekBarPlaySong?.progress = progress

            // Running this thread after 100 milliseconds
            mHandler?.postDelayed(this, 100)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mArrSongs = ArrayList()
        requestAppPermissions()

        setContentView(R.layout.activity_main)

        val mToolBar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mToolBar)

        mHandler = Handler()
        mMediaPlayer = MediaPlayer()

        initRecyclerView()

        getId()
        setListener()

        if (mArrSongs?.isNotEmpty() == true) {

            mSlidingUpLayout?.addPanelSlideListener(object : PanelSlideListener {

                override fun onPanelSlide(panel: View, slideOffset: Float) {

                    if (slideOffset > 0.88) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mRelative?.setBackgroundColor(resources.getColor(R.color.whiteTransparent,theme))
                        }

                        mImgBtnPlayOnSlideLay?.visibility = View.INVISIBLE
                        changeMusicAlbumArt(currentSongIndex)
                        mImgCurrentPlaySong?.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    if (slideOffset < 0.09) {
                        mImgBtnPlayOnSlideLay?.visibility = View.VISIBLE
                        mImgCurrentPlaySong?.setImageResource(R.drawable.music)
                        window.statusBarColor = resources.getColor(R.color.colorPrimaryDark,theme)
                    }
                }

                override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {

                }
            })
        }
    }

    private fun loadSongs() {
        val uiRunnable = Runnable {
            mCardViewProgressBar?.visibility = View.GONE

            if (mArrSongs?.isEmpty() == true) {
                Utilities.showMessage(this, "no song available.", 4)
                return@Runnable
            }

            val mAdapterSongs = AdapterSongs(mArrSongs)
            mRecyclerViewSongs?.adapter = mAdapterSongs

            mAdapterSongs.onItemClick = { adapterPosition ->
                currentSongIndex = adapterPosition
                playSong(adapterPosition)
            }
        }

        val thread = Thread(Runnable {
            mArrSongs = SongManager.getMp3Songs(this@MainActivity)
            runOnUiThread(uiRunnable)
        })
        thread.start()
    }

    private fun initRecyclerView() {
        mRecyclerViewSongs = findViewById(R.id.recyclerViewSongList)
        mRecyclerViewSongs?.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false)
    }

    private fun getId() {
        try {
            mRelative = findViewById(R.id.relative)
            mSlidingUpLayout = findViewById(R.id.sliding_layout)
            mCardViewProgressBar = findViewById(R.id.progress_bar)

            //on SlideUpPanel Layout
            mImgPlayingSong = findViewById(R.id.img_playing_song)
            mTxtPlayingSongName = findViewById(R.id.txt_playing_songName)
            mImgBtnPlayOnSlideLay = findViewById(R.id.imgBtn_play)

            //In slidePanel Layout
            mImgCurrentPlaySong = findViewById(R.id.img_play_song)
            mImgBtnShuffle = findViewById(R.id.btn_song_shuffle_play)
            mImgBtnRepeat = findViewById(R.id.btn_song_repeat_play)
            mImgBtnPlay = findViewById(R.id.btn_song_play)
            mImgBtnNext = findViewById(R.id.btn_song_next)
            mImgBtnPrevious = findViewById(R.id.btn_song_previous)
            mSeekBarPlaySong = findViewById(R.id.seekBar_play_song)
            mTxtSongPlayTime = findViewById(R.id.txt_song_TotalTime)
            mTxtSongPlayCurrDuration = findViewById(R.id.txt_song_Curr_Time)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setListener() {
        try {
            mSeekBarPlaySong?.setOnSeekBarChangeListener(this)
            mMediaPlayer?.setOnCompletionListener(this)

            mImgBtnPlayOnSlideLay?.setOnClickListener(this)
            mImgBtnPlay?.setOnClickListener(this)
            mImgBtnNext?.setOnClickListener(this)
            mImgBtnPrevious?.setOnClickListener(this)
            mImgBtnShuffle?.setOnClickListener(this)
            mImgBtnRepeat?.setOnClickListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun requestAppPermissions() {
        //check which permission granted
        val listPermissionNeeded = ArrayList<String>()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }

        //ask for non-permission granted
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionNeeded.toTypedArray(), REQUEST_CODE_PERMISSION)
            return
        }

        loadSongs()
    }

    override fun onClick(v: View) {

        when (v.id) {

            R.id.imgBtn_play -> try {
                if (mArrSongs?.isNotEmpty() == true && mMediaPlayer?.isPlaying == true) {
                    if (mMediaPlayer != null) {
                        mMediaPlayer?.pause()
                        mImgBtnPlayOnSlideLay?.setImageResource(R.drawable.ic_action_play)
                        mImgBtnPlay?.setImageResource(R.drawable.ic_action_play)
                    }
                } else {
                    if (mArrSongs?.isNotEmpty() == true && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        mImgBtnPlayOnSlideLay?.setImageResource(R.drawable.ic_action_pause)
                        mImgBtnPlay?.setImageResource(R.drawable.ic_action_pause)
                    }
                }
            } catch (iStateException: IllegalStateException) {
                iStateException.printStackTrace()
            }

            R.id.btn_song_play -> try {
                if (mArrSongs?.isNotEmpty() == true && mMediaPlayer?.isPlaying == true) {

                    if (mMediaPlayer != null) {
                        mMediaPlayer?.pause()
                        mImgBtnPlay?.setImageResource(R.drawable.ic_action_play)
                        mImgBtnPlayOnSlideLay?.setImageResource(R.drawable.ic_action_play)
                    }
                } else {
                    if (mArrSongs?.isNotEmpty() == true && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        mImgBtnPlay?.setImageResource(R.drawable.ic_action_pause)
                        mImgBtnPlayOnSlideLay?.setImageResource(R.drawable.ic_action_pause)
                    }
                }
            } catch (iStateException: IllegalStateException) {
                iStateException.printStackTrace()
            }

            R.id.btn_song_next ->

                if (mArrSongs?.isNotEmpty() == true && currentSongIndex < mArrSongs?.size?:0 - 1) {
                    playSong(currentSongIndex + 1)
                    changeMusicAlbumArt(currentSongIndex + 1)
                    currentSongIndex += 1

                } else {
                    playSong(currentSongIndex)
                }

            R.id.btn_song_previous ->

                if (mArrSongs?.isNotEmpty() == true && currentSongIndex > 0) {
                    playSong(currentSongIndex - 1)
                    changeMusicAlbumArt(currentSongIndex - 1)
                    currentSongIndex -= 1

                } else if (mArrSongs?.isNotEmpty() == true){
                    // play last song
                    playSong(mArrSongs?.size?:0 - 1)
                    currentSongIndex = mArrSongs?.size?:0 - 1
                }
            R.id.btn_song_shuffle_play ->

                if (mArrSongs?.isNotEmpty() == true && isShuffle) {
                    isShuffle = false
                    Toast.makeText(applicationContext, "Shuffle is OFF", Toast.LENGTH_SHORT).show()
                    mImgBtnShuffle?.setImageResource(R.drawable.ic_action_shuffle_off)
                } else if (mArrSongs?.isNotEmpty() == true){
                    // make repeat to true
                    isShuffle = true
                    Toast.makeText(applicationContext, "Shuffle is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isRepeat = false
                    mImgBtnShuffle?.setImageResource(R.drawable.ic_action_shuffle_on)
                    mImgBtnRepeat?.setImageResource(R.drawable.ic_action_repeat_off)
                }

            R.id.btn_song_repeat_play ->

                if (mArrSongs?.isNotEmpty() == true && isRepeat) {
                    isRepeat = false
                    Toast.makeText(applicationContext, "Repeat is OFF", Toast.LENGTH_SHORT).show()
                    mImgBtnRepeat?.setImageResource(R.drawable.ic_action_repeat_off)
                } else if(mArrSongs?.isNotEmpty() == true)    {
                    // make repeat to true
                    isRepeat = true
                    Toast.makeText(applicationContext, "Repeat is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isShuffle = false
                    mImgBtnRepeat?.setImageResource(R.drawable.ic_action_repeat_on)
                    mImgBtnShuffle?.setImageResource(R.drawable.ic_action_shuffle_off)
                }
        }
    }

    /**
     * Update timer on seekbar
     */
    private fun updateProgressBar() {
        mHandler?.postDelayed(mUpdateTimeTask, 100)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mHandler?.removeCallbacks(mUpdateTimeTask)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

        mHandler?.removeCallbacks(mUpdateTimeTask)
        val totalDuration = mMediaPlayer?.duration?: 0
        val currentPosition = Utilities.progressToTimer(seekBar.progress, totalDuration)

        // forward or backward to certain seconds
        mMediaPlayer?.seekTo(currentPosition)

        // update timer progress again
        updateProgressBar()
    }

    private fun playSong(songIndex: Int) {
        try {
            if (mArrSongs?.size?:0 > 0) {
                if (mMediaPlayer?.isPlaying == true) {
                    mMediaPlayer?.stop()
                }
                mMediaPlayer?.reset()

                mMediaPlayer?.setDataSource(mArrSongs?.get(songIndex)?.path)
                //            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer?.prepare()
                mMediaPlayer?.setOnPreparedListener { mp ->
                    mp.start()

                    mMediaPlayer?.playbackParams = PlaybackParams().allowDefaults().
                            setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
                    Utilities.setImageByByteArray(this, mArrSongs?.get(songIndex)?.albumArtByteArray, mImgPlayingSong)
                    mTxtPlayingSongName?.text = mArrSongs?.get(songIndex)?.songTitle
                    mImgBtnPlayOnSlideLay?.setImageResource(R.drawable.ic_action_pause)
                    mImgBtnPlay?.setImageResource(R.drawable.ic_action_pause)

                    mSeekBarPlaySong?.progress = 0
                    mSeekBarPlaySong?.max = 100

                    updateProgressBar()
                }
                mMediaPlayer?.prepareAsync()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (iState: IllegalStateException) {
            iState.printStackTrace()
        }

    }

    override fun onCompletion(mp: MediaPlayer) {
        // check for repeat is ON or OFF
        if (isRepeat) {
            // repeat is on play same song again
            playSong(currentSongIndex)

        } else if (isShuffle) {
            // shuffle is on - play a random song
            val rand = Random()
            currentSongIndex = rand.nextInt(mArrSongs?.size?:0 - 1 + 1)
            playSong(currentSongIndex)
            //set albumArt and change statusBar color when changed song
            changeMusicAlbumArt(currentSongIndex)
        } else {
            // no repeat or shuffle ON - play next song
            if (currentSongIndex < mArrSongs?.size?:0 - 1) {
                playSong(currentSongIndex + 1)
                //set albumArt and change statusBar color when changed song
                changeMusicAlbumArt(currentSongIndex + 1)
                currentSongIndex += 1
            } else {
                // play first song
                playSong(0)
                currentSongIndex = 0
            }
        }
    }

    fun changeMusicAlbumArt(currentSongIndex: Int) {

        Utilities.setImageByByteArray(this, mArrSongs?.get(currentSongIndex)?.albumArtByteArray,
                mImgCurrentPlaySong)

        val bitmap = Utilities.getBitmapFromByteArray(mArrSongs?.get(currentSongIndex)?.albumArtByteArray)

        bitmap?.let {
            Palette.from(it).generate { palette ->
            val vibrantSwatch = palette?.vibrantSwatch
                if (vibrantSwatch != null) {
                    window.statusBarColor = vibrantSwatch.rgb
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        SpUtility.getInstance(this)?.setCurrentSongIndex(currentSongIndex)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mMediaPlayer?.isPlaying == true) {

            mHandler?.removeCallbacks(mUpdateTimeTask)
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    override fun onBackPressed() {
        if (mSlidingUpLayout != null && (mSlidingUpLayout?.panelState == PanelState.EXPANDED ||
                        mSlidingUpLayout?.panelState == PanelState.ANCHORED)) {
            mSlidingUpLayout?.panelState = PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        //Gather permission grant results
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val perms = HashMap<String, Int>()
            // Initial
            perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

            var deniedCount = 0
            for (i in permissions.indices) {
                perms[permissions[i]] = grantResults[i]
                //add only permissions which are denied

                if (perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED &&
                        perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {

                    loadSongs()
                    return
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    deniedCount++
                }
            }

            if (deniedCount > 0) {
                requestAppPermissions()
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
