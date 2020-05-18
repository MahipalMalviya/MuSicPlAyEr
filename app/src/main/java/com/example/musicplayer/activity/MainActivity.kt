package com.example.musicplayer.activity


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.AdapterSongs
import com.example.musicplayer.constants.PlayerConstants
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import io.fabric.sdk.android.services.concurrency.AsyncTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

//    private var mArrSongs = ArrayList<Song>()
//    private var mMediaPlayer: MediaPlayer? = null
    private var mHandler: Handler? = null
    private var musicService: MusicService? = null
    private var serviceBound = false

//    private var currentSongIndex = 0

    companion object {
        const val ACTION_PLAY = "com.mahipal.mediaplayer.action.play"
        const val ACTION_PAUSE = "com.mahipal.mediaplayer.action.pause"
        const val ACTION_PREV = "com.mahipal.mediaplayer.action.prev"
        const val ACTION_NEXT = "com.mahipal.mediaplayer.action.next"
        const val ACTION_STOP = "com.mahipal.mediaplayer.action.stop"
    }

    private val mUpdateTimeTask = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {

            musicService?.mMediaPlayer?.let { player ->

                try {
                    val totalDuration = player.duration.toLong()
                    val currentDuration = player.currentPosition.toLong()

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
                } catch (ex:IllegalStateException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private val serviceConnection = object :ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.service
            serviceBound = true

            musicService?.notificationControl = { notifyAction ->
                if (notifyAction.equals(MusicService.NOTIFY_NEXT,true) or
                        notifyAction.equals(MusicService.NOTIFY_PREV,true)) {

                    updateProgressBar()
                }
                updateUiControls()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Thread.setDefaultUncaughtExceptionHandler(DefaultExceptionHandler(this))
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        AsyncTaskThread().execute()
    }

    private fun updateUi() {
        cv_progress_bar?.visibility = View.GONE

        mHandler = Handler()
//        mMediaPlayer = MediaPlayer()

        setListener()

        sliding_layout?.addPanelSlideListener(object : PanelSlideListener {

            override fun onPanelSlide(panel: View, slideOffset: Float) {

                if (slideOffset > 0.88) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        relative?.setBackgroundColor(resources.getColor(R.color.whiteTransparent, theme))
                    }

                    imgBtn_play?.visibility = View.INVISIBLE
                    img_play_song?.scaleType = ImageView.ScaleType.CENTER_CROP
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

        recyclerViewSongList?.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false)
        val mAdapterSongs = AdapterSongs(PlayerConstants.SONG_LIST)
        recyclerViewSongList?.adapter = mAdapterSongs

//        currentSongIndex = SpUtility.getInstance(this)?.getCurrenSongIndex() ?: 0

//        Utilities.setImageByByteArray(this, mArrSongs[currentSongIndex.plus(1)].albumArtByteArray, img_playing_song)
//        txt_playing_songName?.text = mArrSongs[currentSongIndex.plus(1)].songTitle
//        imgBtn_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
//        changeMusicAlbumArt(currentSongIndex)
//        btn_song_play?.setImageResource(R.drawable.ic_play_arrow_24dp)

        mAdapterSongs.onItemClick = { adapterPosition ->
            PlayerConstants.SONG_NUMBER = adapterPosition
            PlayerConstants.SONG_PAUSED = false
            playAudio(adapterPosition)
            updateUiControls()
        }
    }

    private fun setListener() {
        try {
            seekBar_play_song?.setOnSeekBarChangeListener(this)
//            mMediaPlayer?.setOnCompletionListener(this)

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

            R.id.imgBtn_play -> {
                musicService?.playMedia()
                updateUiControls()
                musicService?.buildNotification()
            }

            R.id.btn_song_play -> {
                musicService?.playMedia()
                updateUiControls()
                musicService?.buildNotification()
            }

            R.id.btn_song_next -> {
                musicService?.nextMedia()
//                updateUiControls()
            }
            R.id.btn_song_previous -> {
                musicService?.prevMedia()
//                updateUiControls()
            }
            R.id.btn_song_shuffle_play ->

                if (PlayerConstants.isShuffle) {
                    PlayerConstants.isShuffle = false
                    Toast.makeText(applicationContext, "Shuffle OFF", Toast.LENGTH_SHORT).show()
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_off_24dp)
                } else {

                    PlayerConstants.isShuffle = true
                    SpUtility.getInstance(this)?.setSongShuffle(PlayerConstants.isShuffle)

                    Toast.makeText(applicationContext, "Shuffle ON", Toast.LENGTH_SHORT).show()

                    PlayerConstants.isRepeat = false
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_on_24dp)
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off_24dp)
                }

            R.id.btn_song_repeat_play ->

                if (PlayerConstants.isRepeat) {

                    PlayerConstants.isRepeat = false
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    Toast.makeText(applicationContext, "Repeat OFF", Toast.LENGTH_SHORT).show()
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off_24dp)
                } else {
                    // make repeat to true
                    PlayerConstants.isRepeat = true
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    Toast.makeText(applicationContext, "Repeat ON", Toast.LENGTH_SHORT).show()

                    // make shuffle to false
                    PlayerConstants.isShuffle = false
                    SpUtility.getInstance(this)?.setSongShuffle(PlayerConstants.isShuffle)

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

        musicService?.mMediaPlayer?.let { player ->
            val totalDuration = player.duration
            val currentPosition = Utilities.progressToTimer(seekBar.progress, totalDuration)

            // forward or backward to certain seconds
            player.seekTo(currentPosition)

            // update timer progress again
            updateProgressBar()
        }
    }

    override fun onResume() {
        super.onResume()

//        val isServiceRunning = Utilities.isServiceRunning(MusicService::class.java.simpleName,this)
//        if (serviceBound) {
            updateUiControls()
//        }

        updateProgressBar()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("ServiceState",serviceBound)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    fun updateUiControls() {
        try {

            changeMusicAlbumArt(PlayerConstants.SONG_NUMBER,img_playing_song)
            changeMusicAlbumArt(PlayerConstants.SONG_NUMBER,img_play_song)
            txt_playing_songName?.text = PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle

        } catch (ex:Exception) {
            ex.printStackTrace()
        }

        if (!PlayerConstants.SONG_PAUSED) {
            imgBtn_play?.setImageResource(R.drawable.ic_pause_24dp)
            btn_song_play?.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            imgBtn_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
            btn_song_play?.setImageResource(R.drawable.ic_play_arrow_24dp)
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
    }

    private fun changeMusicAlbumArt(currentSongIndex: Int,imageView: ImageView) {
        Utilities.setImageByByteArray(this, PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.albumArtByteArray,
                imageView)
    }

    override fun onDestroy() {
        if (PlayerConstants.SONG_PAUSED && musicService != null) {

            mHandler?.removeCallbacks(mUpdateTimeTask)
            musicService?.stopSelf()
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    private fun playAudio(songIndex:Int) {
        //Check is service is active
        if (!serviceBound) {

            SpUtility.getInstance(this)?.setCurrentSongIndex(songIndex)

            val playerIntent = Intent(this, MusicService::class.java)
            startService(playerIntent)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            //Store the new songIndex to SharedPreferences
            SpUtility.getInstance(this)?.setCurrentSongIndex(songIndex)

            //Service is active
            //Send a broadcast to the service -> Play new Song
            val broadcastIntent = Intent(ACTION_PLAY)
            sendBroadcast(broadcastIntent)
        }

        seekBar_play_song?.progress = 0
        seekBar_play_song?.max = 100
        updateProgressBar()
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
            val mArrSongs = Utilities.getMp3Songs(this@MainActivity)
            PlayerConstants.SONG_LIST = mArrSongs
            SpUtility.getInstance(this@MainActivity)?.storeSongs(mArrSongs)
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            updateUi()
        }
    }
}
