package com.example.musicplayer.activity


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.*
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.adapter.AdapterSongs
import com.example.musicplayer.constants.PlayerConstants
import com.example.musicplayer.exception.DefaultExceptionHandler
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.utils.SpUtility
import com.example.musicplayer.utils.Utilities
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import kotlinx.android.synthetic.main.activity_music_player.*
import kotlinx.coroutines.*


class MusicPlayerActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private var serviceBound = false

    private var musicService: MusicService? = null

    companion object {
        const val ACTION_PLAY_NEW_AUDIO = "com.mahipal.mediaplayer.action.playNewAudio"
        const val ACTION_PLAY = "com.mahipal.mediaplayer.action.play"
        const val ACTION_PAUSE = "com.mahipal.mediaplayer.action.pause"
        const val ACTION_NEXT = "com.mahipal.mediaplayer.action.next"
        const val ACTION_PREV = "com.mahipal.mediaplayer.action.previous"
    }

    private val serviceConnection = object :ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.service
            serviceBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(DefaultExceptionHandler(this))
        setContentView(R.layout.activity_music_player)

        setSupportActionBar(toolbar)

        GlobalScope.launch(Dispatchers.Main + handler) {
            val songList = fetchSongList()

            PlayerConstants.SONG_LIST = songList
            SpUtility.getInstance(this@MusicPlayerActivity)?.storeSongs(songList)

            updateUi()
        }

        MusicService.notificationControl = {
            updateUiControls()
        }

        MusicService.updateUiOnMediaAction = {
            updateUiControls()
        }
    }

    private suspend fun fetchSongList(): ArrayList<Song> {
        return withContext(Dispatchers.IO + handler) {
            Utilities.getMp3Songs(this@MusicPlayerActivity)
        }
    }

    private val handler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private fun updateUi() {
        cv_progress_bar?.visibility = View.GONE

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
                val intent = Intent(ACTION_PLAY)
                sendBroadcast(intent)
            }

            R.id.btn_song_play -> {
                val intent = Intent(ACTION_PAUSE)
                sendBroadcast(intent)
            }

            R.id.btn_song_next -> {
                val intent = Intent(ACTION_NEXT)
                sendBroadcast(intent)

//                updateUiControls()
            }
            R.id.btn_song_previous -> {
                val intent = Intent(ACTION_PREV)
                sendBroadcast(intent)

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

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

        MusicService.mMediaPlayer?.let { player ->
            val totalDuration = player.duration
            val currentPosition = Utilities.progressToTimer(seekBar.progress, totalDuration)

            // forward or backward to certain seconds
            player.seekTo(currentPosition)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        try {
            updateUiControls()
            PlayerConstants.PROGRESSBAR_HANDLER = Handler(Handler.Callback { message ->
                val longArray = message.obj as LongArray

                val currentDuration = longArray[0]
                val totalDuration = longArray[1]
                // Displaying Total Duration time
                txt_song_TotalTime?.text = "" + Utilities.milliSecondsToTimer(totalDuration)
                // Displaying time completed playing
                txt_song_Curr_Time?.text = "" + Utilities.milliSecondsToTimer(currentDuration)

                // Updating progress bar
                val progress = Utilities.getProgressPercentage(currentDuration, totalDuration)

                seekBar_play_song?.progress = progress
                return@Callback false
            })

        }catch (ex:Exception) {}

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("ServiceState",serviceBound)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    private fun updateUiControls() {
        try {

            changeMusicAlbumArt(img_playing_song)
            changeMusicAlbumArt(img_play_song)
            txt_playing_songName?.text = PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle

        } catch (ex:Exception) {
            ex.printStackTrace()
        }

        updateButtons()
    }

    private fun updateButtons() {
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

    private fun changeMusicAlbumArt(imageView: ImageView) {
        Utilities.setImageByByteArray(this, PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.albumArtByteArray,
                imageView)
    }

    override fun onDestroy() {
        if (PlayerConstants.SONG_PAUSED) {

            stopService(Intent(this,MusicService::class.java))
        } else {
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
            val broadcastIntent = Intent(ACTION_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }

        seekBar_play_song?.progress = 0
        seekBar_play_song?.max = 100
    }

    override fun onBackPressed() {
        if (sliding_layout != null && (sliding_layout?.panelState == PanelState.EXPANDED ||
                        sliding_layout?.panelState == PanelState.ANCHORED)) {
            sliding_layout?.panelState = PanelState.COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
}
