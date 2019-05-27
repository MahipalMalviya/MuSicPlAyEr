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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.example.musicplayer.R
import com.example.musicplayer.utils.SongManager
import com.example.musicplayer.adapter.AdapterSongs
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities

import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import kotlinx.android.synthetic.main.activity_main.*

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Random


class MainActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private val permissionList = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)


    private var mRecyclerViewSongs: RecyclerView? = null
    private var mArrSongs: ArrayList<Song>? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mHandler: Handler? = null

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
            txt_song_TotalTime?.text = "" + Utilities.milliSecondsToTimer(totalDuration)
            // Displaying time completed playing
            txt_song_Curr_Time?.text = "" + Utilities.milliSecondsToTimer(currentDuration)

            // Updating progress bar
            val progress = Utilities.getProgressPercentage(currentDuration, totalDuration)
            //Log.d("Progress", ""+progress);
            seekBar_play_song?.progress = progress

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

        setListener()

        if (mArrSongs?.isNotEmpty() == true) {

            sliding_layout?.addPanelSlideListener(object : PanelSlideListener {

                override fun onPanelSlide(panel: View, slideOffset: Float) {

                    if (slideOffset > 0.88) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            relative?.setBackgroundColor(resources.getColor(R.color.whiteTransparent,theme))
                        }

                        imgBtn_play?.visibility = View.INVISIBLE
                        changeMusicAlbumArt(currentSongIndex)
                        img_play_song?.scaleType = ImageView.ScaleType.FIT_XY
                    }
                    if (slideOffset < 0.09) {
                        imgBtn_play?.visibility = View.VISIBLE
                        img_play_song?.setImageResource(R.drawable.music)
//                        window.statusBarColor = resources.getColor(R.color.colorPrimaryDark,theme)
                    }
                }

                override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {

                }
            })
        }
    }

    private fun loadSongs() {
        val uiRunnable = Runnable {
            progress_bar?.visibility = View.GONE

            if (mArrSongs?.isEmpty() == true) {
                Utilities.showMessage(this, "no song available.", 4)
                return@Runnable
            }

            val mAdapterSongs = AdapterSongs(mArrSongs)
            mRecyclerViewSongs?.adapter = mAdapterSongs

            currentSongIndex = SpUtility.getInstance(this)?.getCurrenSongIndex()?:0
            Utilities.setImageByByteArray(this, mArrSongs?.get(currentSongIndex.plus(1))?.albumArtByteArray, img_playing_song)
            txt_playing_songName?.text = mArrSongs?.get(currentSongIndex.plus(1))?.songTitle
            imgBtn_play?.setImageResource(R.drawable.ic_action_play)
            changeMusicAlbumArt(currentSongIndex)
            btn_song_play?.setImageResource(R.drawable.ic_action_play)

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

    private fun setListener() {
        try {
            seekBar_play_song?.setOnSeekBarChangeListener(this)
            mMediaPlayer?.setOnCompletionListener(this)

            imgBtn_play?.setOnClickListener(this)
            btn_song_play?.setOnClickListener(this)
            btn_song_next?.setOnClickListener(this)
            btn_song_previous?.setOnClickListener(this)
            btn_song_shuffle_play?.setOnClickListener(this)
            btn_song_repeat_play?.setOnClickListener(this)
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
                        imgBtn_play?.setImageResource(R.drawable.ic_action_play)
                        btn_song_play?.setImageResource(R.drawable.ic_action_play)
                    }
                } else {
                    if (mArrSongs?.isNotEmpty() == true && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        imgBtn_play?.setImageResource(R.drawable.ic_action_pause)
                        btn_song_play?.setImageResource(R.drawable.ic_action_pause)
                    }
                }
            } catch (iStateException: IllegalStateException) {
                iStateException.printStackTrace()
            }

            R.id.btn_song_play -> try {
                if (mArrSongs?.isNotEmpty() == true && mMediaPlayer?.isPlaying == true) {

                    if (mMediaPlayer != null) {
                        mMediaPlayer?.pause()
                        btn_song_play?.setImageResource(R.drawable.ic_action_play)
                        imgBtn_play?.setImageResource(R.drawable.ic_action_play)
                    }
                } else {
                    if (mArrSongs?.isNotEmpty() == true && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        btn_song_play?.setImageResource(R.drawable.ic_action_pause)
                        imgBtn_play?.setImageResource(R.drawable.ic_action_pause)
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
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_action_shuffle_off)
                } else if (mArrSongs?.isNotEmpty() == true){
                    // make repeat to true
                    isShuffle = true
                    Toast.makeText(applicationContext, "Shuffle is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isRepeat = false
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_action_shuffle_on)
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_action_repeat_off)
                }

            R.id.btn_song_repeat_play ->

                if (mArrSongs?.isNotEmpty() == true && isRepeat) {
                    isRepeat = false
                    Toast.makeText(applicationContext, "Repeat is OFF", Toast.LENGTH_SHORT).show()
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_action_repeat_off)
                } else if(mArrSongs?.isNotEmpty() == true)    {
                    // make repeat to true
                    isRepeat = true
                    Toast.makeText(applicationContext, "Repeat is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isShuffle = false
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_action_repeat_on)
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_action_shuffle_off)
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
                    Utilities.setImageByByteArray(this, mArrSongs?.get(songIndex)?.albumArtByteArray, img_playing_song)
                    changeMusicAlbumArt(songIndex)
                    txt_playing_songName?.text = mArrSongs?.get(songIndex)?.songTitle
                    imgBtn_play?.setImageResource(R.drawable.ic_action_pause)
                    btn_song_play?.setImageResource(R.drawable.ic_action_pause)
                    seekBar_play_song?.progress = 0
                    seekBar_play_song?.max = 100

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
                img_play_song)

    }

    override fun onDestroy() {
        super.onDestroy()

        if (mMediaPlayer?.isPlaying == true) {
            SpUtility.getInstance(this)?.setCurrentSongIndex(currentSongIndex)
            mHandler?.removeCallbacks(mUpdateTimeTask)
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    override fun onBackPressed() {
        if (sliding_layout != null && (sliding_layout?.panelState == PanelState.EXPANDED ||
                        sliding_layout?.panelState == PanelState.ANCHORED)) {
            sliding_layout?.panelState = PanelState.COLLAPSED
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