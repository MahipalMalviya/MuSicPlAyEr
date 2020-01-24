package com.example.musicplayer.activity


import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.adapter.AdapterSongs
import com.example.musicplayer.exception.DefaultExceptionHandler
import com.example.musicplayer.model.Song
import com.example.musicplayer.utils.SongManager
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import io.fabric.sdk.android.services.concurrency.AsyncTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private var mRecyclerViewSongs: RecyclerView? = null
    private var mArrSongs = ArrayList<Song>()
    private var mMediaPlayer: MediaPlayer? = null
    private var mHandler: Handler? = null

    private var currentSongIndex = 0
    private var isShuffle = false
    private var isRepeat = false

    private val mUpdateTimeTask = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            val totalDuration = mMediaPlayer?.duration?.toLong() ?: 0
            val currentDuration = mMediaPlayer?.currentPosition?.toLong() ?: 0

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
        Thread.setDefaultUncaughtExceptionHandler(DefaultExceptionHandler(this))
        setContentView(R.layout.activity_main)

        AsyncTaskThread().execute()

        setSupportActionBar(toolbar)

        mHandler = Handler()
        mMediaPlayer = MediaPlayer()

        initRecyclerView()

        setListener()

        if (mArrSongs.isNotEmpty()) {

            sliding_layout?.addPanelSlideListener(object : PanelSlideListener {

                override fun onPanelSlide(panel: View, slideOffset: Float) {

                    if (slideOffset > 0.88) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            relative?.setBackgroundColor(resources.getColor(R.color.whiteTransparent, theme))
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

    private fun updateUi() {
        cv_progress_bar?.visibility = View.GONE

        if (mArrSongs.isEmpty()) {
            Utilities.showMessage(this, "no song available.", 4)
            return
        }

        val mAdapterSongs = AdapterSongs(mArrSongs)
        mRecyclerViewSongs?.adapter = mAdapterSongs

        currentSongIndex = SpUtility.getInstance(this)?.getCurrenSongIndex() ?: 0
        Utilities.setImageByByteArray(this, mArrSongs[currentSongIndex.plus(1)].albumArtByteArray, img_playing_song)
        txt_playing_songName?.text = mArrSongs[currentSongIndex.plus(1)].songTitle
        imgBtn_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
        changeMusicAlbumArt(currentSongIndex)
        btn_song_play?.setImageResource(R.drawable.ic_play_arrow_24dp)

        mAdapterSongs.onItemClick = { adapterPosition ->
            currentSongIndex = adapterPosition
            playSong(adapterPosition)
        }
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

    override fun onClick(v: View) {

        when (v.id) {

            R.id.imgBtn_play -> try {
                if (mArrSongs.isNotEmpty() && mMediaPlayer?.isPlaying == true) {
                    if (mMediaPlayer != null) {
                        mMediaPlayer?.pause()
                        imgBtn_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
                        btn_song_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
                    }
                } else {
                    if (mArrSongs.isNotEmpty() && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        imgBtn_play?.setImageResource(R.drawable.ic_pause_24dp)
                        btn_song_play?.setImageResource(R.drawable.ic_pause_24dp)
                    }
                }
            } catch (iStateException: IllegalStateException) {
                iStateException.printStackTrace()
            }

            R.id.btn_song_play -> try {
                if (mArrSongs.isNotEmpty() && mMediaPlayer?.isPlaying == true) {

                    if (mMediaPlayer != null) {
                        mMediaPlayer?.pause()
                        btn_song_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
                        imgBtn_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
                    }
                } else {
                    if (mArrSongs.isNotEmpty() && mMediaPlayer != null) {
                        mMediaPlayer?.start()
                        btn_song_play?.setImageResource(R.drawable.ic_pause_24dp)
                        imgBtn_play?.setImageResource(R.drawable.ic_pause_24dp)
                    }
                }
            } catch (iStateException: IllegalStateException) {
                iStateException.printStackTrace()
            }

            R.id.btn_song_next ->

                if (mArrSongs.isNotEmpty() && currentSongIndex < mArrSongs.size - 1) {
                    playSong(currentSongIndex + 1)
                    changeMusicAlbumArt(currentSongIndex + 1)
                    currentSongIndex += 1

                } else {
                    playSong(currentSongIndex)
                }

            R.id.btn_song_previous ->

                if (mArrSongs.isNotEmpty() && currentSongIndex > 0) {
                    playSong(currentSongIndex - 1)
                    changeMusicAlbumArt(currentSongIndex - 1)
                    currentSongIndex -= 1

                } else if (mArrSongs.isNotEmpty()) {
                    // play last song
                    playSong(mArrSongs.size - 1)
                    currentSongIndex = mArrSongs.size - 1
                }
            R.id.btn_song_shuffle_play ->

                if (mArrSongs.isNotEmpty() && isShuffle) {
                    isShuffle = false
                    Toast.makeText(applicationContext, "Shuffle is OFF", Toast.LENGTH_SHORT).show()
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_off_24dp)
                } else if (mArrSongs.isNotEmpty()) {
                    // make repeat to true
                    isShuffle = true
                    Toast.makeText(applicationContext, "Shuffle is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isRepeat = false
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_on_24dp)
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off_24dp)
                }

            R.id.btn_song_repeat_play ->

                if (mArrSongs.isNotEmpty() && isRepeat) {
                    isRepeat = false
                    Toast.makeText(applicationContext, "Repeat is OFF", Toast.LENGTH_SHORT).show()
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off_24dp)
                } else if (mArrSongs.isNotEmpty()) {
                    // make repeat to true
                    isRepeat = true
                    Toast.makeText(applicationContext, "Repeat is ON", Toast.LENGTH_SHORT).show()
                    // make shuffle to false
                    isShuffle = false
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_on_24dp)
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_off_24dp)
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
        val totalDuration = mMediaPlayer?.duration ?: 0
        val currentPosition = Utilities.progressToTimer(seekBar.progress, totalDuration)

        // forward or backward to certain seconds
        mMediaPlayer?.seekTo(currentPosition)

        // update timer progress again
        updateProgressBar()
    }

    private fun playSong(songIndex: Int) {
        try {
            if (mArrSongs.size > 0) {
                if (mMediaPlayer?.isPlaying == true) {
                    mMediaPlayer?.stop()
                }
                mMediaPlayer?.reset()

                mMediaPlayer?.setDataSource(mArrSongs[songIndex].path)
                //            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer?.prepare()
                mMediaPlayer?.setOnPreparedListener { mp ->
                    mp.start()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mMediaPlayer?.playbackParams = PlaybackParams().allowDefaults().setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
                    }
                    Utilities.setImageByByteArray(this, mArrSongs[songIndex].albumArtByteArray, img_playing_song)
                    changeMusicAlbumArt(songIndex)
                    txt_playing_songName?.text = mArrSongs[songIndex].songTitle
                    imgBtn_play?.setImageResource(R.drawable.ic_pause_24dp)
                    btn_song_play?.setImageResource(R.drawable.ic_pause_24dp)
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
            currentSongIndex = rand.nextInt(mArrSongs.size - 1 + 1)
            playSong(currentSongIndex)
            //set albumArt and change statusBar color when changed song
            changeMusicAlbumArt(currentSongIndex)
        } else {
            // no repeat or shuffle ON - play next song
            if (currentSongIndex < mArrSongs.size - 1) {
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

        Utilities.setImageByByteArray(this, mArrSongs[currentSongIndex].albumArtByteArray,
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

    inner class AsyncTaskThread : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            mArrSongs = SongManager.getMp3Songs(this@MainActivity)
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            updateUi()
        }
    }
}
