package com.mahipal.musicplayer.activity


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mahipal.musicplayer.R
import com.mahipal.musicplayer.adapter.AdapterSongs
import com.mahipal.musicplayer.adapter.PlaylistQueueAdapter
import com.mahipal.musicplayer.constants.PlayerConstants
import com.mahipal.musicplayer.exception.DefaultExceptionHandler
import com.mahipal.musicplayer.listeners.OnSwipeTouchListener
import com.mahipal.musicplayer.model.Song
import com.mahipal.musicplayer.service.MusicService
import com.mahipal.musicplayer.utils.Controls
import com.mahipal.musicplayer.utils.SpUtility
import com.mahipal.musicplayer.utils.Utilities
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import kotlinx.android.synthetic.main.activity_music_player.*
import kotlinx.coroutines.*


class MusicPlayerActivity : AppCompatActivity(), View.OnClickListener, MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener {

    private var TAG = MusicPlayerActivity::class.java.simpleName

    private var musicService: MusicService? = null
    private var isPlaylistQueueVisible = false

    private var playlistQueueAdapter: PlaylistQueueAdapter? = null
    private var mAdapterSongs: AdapterSongs? = null

    companion object {
        const val ACTION_PLAY_NEW_AUDIO = "com.mahipal.mediaplayer.action.playNewAudio"
//        const val ACTION_PLAY = "com.mahipal.mediaplayer.action.play"
//        const val ACTION_PAUSE = "com.mahipal.mediaplayer.action.pause"
//        const val ACTION_NEXT = "com.mahipal.mediaplayer.action.next"
//        const val ACTION_PREV = "com.mahipal.mediaplayer.action.previous"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(DefaultExceptionHandler(this))
        setContentView(R.layout.activity_music_player)

        setSupportActionBar(toolbar)

        if (PlayerConstants.SONG_NUMBER == -1) {
            sliding_layout?.isTouchEnabled = false
        }

        GlobalScope.launch(Dispatchers.Main + handler) {
            val songList = fetchSongList()
            SpUtility.getInstance(this@MusicPlayerActivity)?.storeSongs(songList)
            PlayerConstants.SONG_LIST = songList

            if (songList?.isNotEmpty() == true) {
                updateUi()
            }
        }

        MusicService.notificationControl = {
            updateUiControls()
        }

        MusicService.updateUiOnMediaAction = {
            updateUiControls()
        }
    }

    private suspend fun fetchSongList(): ArrayList<Song>? {
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
                    iv_playlist?.visibility = View.VISIBLE
                    imgBtn_play?.visibility = View.GONE
                } else if (slideOffset < 0.09) {
                    iv_playlist?.visibility = View.GONE
                    imgBtn_play?.visibility = View.VISIBLE
                }
            }

            override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {

            }
        })

        recyclerViewSongList?.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false)
        mAdapterSongs = AdapterSongs(SpUtility.getInstance(this)?.getSongs())
        recyclerViewSongList?.adapter = mAdapterSongs

        mAdapterSongs?.onItemClick = { adapterPosition ->
            PlayerConstants.SONG_NUMBER = adapterPosition
            PlayerConstants.SONG_PAUSED = false

            if (PlayerConstants.SONG_NUMBER != -1) {
                sliding_layout?.isTouchEnabled = true
            }

            playAudio(adapterPosition)
            updateUiControls()
        }
    }

    private fun setListener() {
        try {
            seekBar_play_song?.setOnSeekBarChangeListener(this)

            img_play_song?.setOnTouchListener(object :OnSwipeTouchListener(this@MusicPlayerActivity){
                override fun onSwipeLeft() {
                    Controls.nextControl(this@MusicPlayerActivity)
                }

                override fun onSwipeRight() {
                    Controls.previousControl(this@MusicPlayerActivity)
                }
            })

            dragView?.setOnTouchListener(object : OnSwipeTouchListener(this@MusicPlayerActivity) {
                override fun onSwipeLeft() {
                    Controls.nextControl(this@MusicPlayerActivity)
                }

                override fun onSwipeRight() {
                    Controls.previousControl(this@MusicPlayerActivity)
                }
            })

            iv_playlist?.setOnClickListener(this)
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
                Controls.playControl(this)
            }

            R.id.btn_song_play -> {
                Controls.playControl(this)
            }

            R.id.btn_song_next -> {
                Controls.nextControl(this)
            }
            R.id.btn_song_previous -> {
                Controls.previousControl(this)
            }
            R.id.iv_playlist -> {
                if (!isPlaylistQueueVisible) {

                    isPlaylistQueueVisible = true
                    loadPlaylistQueue()
                } else {

                    isPlaylistQueueVisible = false
                    ll_playlist_queue.visibility = View.GONE
                    rl_media_shuffle?.visibility = View.VISIBLE
                }
            }
            R.id.btn_song_shuffle_play ->

                if (PlayerConstants.isShuffle) {
                    PlayerConstants.isShuffle = false
                    Toast.makeText(applicationContext, "Shuffle OFF", Toast.LENGTH_SHORT).show()
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_off)
                } else {

                    PlayerConstants.isShuffle = true
                    SpUtility.getInstance(this)?.setSongShuffle(PlayerConstants.isShuffle)

                    Toast.makeText(applicationContext, "Shuffle ON", Toast.LENGTH_SHORT).show()

                    PlayerConstants.isRepeat = false
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_on)
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off)
                }

            R.id.btn_song_repeat_play ->

                if (PlayerConstants.isRepeat) {

                    PlayerConstants.isRepeat = false
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    Toast.makeText(applicationContext, "Repeat OFF", Toast.LENGTH_SHORT).show()
                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_off)
                } else {
                    // make repeat to true
                    PlayerConstants.isRepeat = true
                    SpUtility.getInstance(this)?.setSongRepeat(PlayerConstants.isRepeat)

                    Toast.makeText(applicationContext, "Repeat ON", Toast.LENGTH_SHORT).show()

                    // make shuffle to false
                    PlayerConstants.isShuffle = false
                    SpUtility.getInstance(this)?.setSongShuffle(PlayerConstants.isShuffle)

                    btn_song_repeat_play?.setImageResource(R.drawable.ic_repeat_one_on)
                    btn_song_shuffle_play?.setImageResource(R.drawable.ic_shuffle_off)
                }
        }
    }

    private fun loadPlaylistQueue() {
        rl_media_shuffle?.visibility = View.INVISIBLE
        ll_playlist_queue.visibility = View.VISIBLE
        rv_playlist_queue.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        playlistQueueAdapter = PlaylistQueueAdapter(PlayerConstants.SONG_LIST)
        rv_playlist_queue.adapter = playlistQueueAdapter

        itemTouchHelper.attachToRecyclerView(rv_playlist_queue)

        playlistQueueAdapter?.onTouchStartDragging = { viewHolder ->
            sliding_layout?.isTouchEnabled = false
            itemTouchHelper.startDrag(viewHolder)
        }
    }

    private val itemTouchHelper by lazy {
        val itemTouchCallback = object :ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or
                                                                                ItemTouchHelper.DOWN,0) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter as PlaylistQueueAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                adapter.moveItem(from,to)

                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                sliding_layout?.isTouchEnabled = true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

        }
        ItemTouchHelper(itemTouchCallback)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {


//        MusicService.mMediaPlayer?.let { player ->
//            val totalDuration = player.duration
//            val currentPosition = Utilities.progressToTimer(seekBar.progress, totalDuration)
//
//            // forward or backward to certain seconds
//            player.seekTo(currentPosition)
//        }
    }

    private fun updateProgressBar() {
        try {
            PlayerConstants.PROGRESSBAR_HANDLER = Handler(Handler.Callback { message ->
                runOnUiThread {
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
                }
                return@Callback false
            })
        } catch (ex:Exception) {
            ex.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        updateUiControls()
        updateProgressBar()
    }

    private fun updateUiControls() {
        try {

            if (ll_playlist_queue.visibility == View.VISIBLE) {
                playlistQueueAdapter?.notifyDataSetChanged()
            }
            mAdapterSongs?.notifyDataSetChanged()

            changeMusicAlbumArt(img_playing_song)
            changeMusicAlbumArt(img_play_song)
            txt_playing_songName?.text = PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.songTitle

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        updateButtons()
    }

    private fun updateButtons() {
        if (!PlayerConstants.SONG_PAUSED) {
            imgBtn_play?.setImageResource(R.drawable.ic_pause)
            btn_song_play?.setImageResource(R.drawable.ic_pause)
        } else {
            imgBtn_play?.setImageResource(R.drawable.ic_play)
            btn_song_play?.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
    }

    private fun changeMusicAlbumArt(imageView: ImageView) {
        if (PlayerConstants.SONG_NUMBER != -1)
            return
        Utilities.setImageByByteArray(this, PlayerConstants.SONG_LIST?.get(PlayerConstants.SONG_NUMBER)?.albumId,
                imageView)
    }

    override fun onDestroy() {
        if (PlayerConstants.SONG_PAUSED) {

            stopService(Intent(this, MusicService::class.java))
        }
        super.onDestroy()
    }

    private fun playAudio(songIndex: Int) {
        //Check is service is active
        val isServiceRunning = Utilities.isServiceRunning(MusicService::class.java.simpleName,this)

        if (!isServiceRunning) {

            SpUtility.getInstance(this)?.setCurrentSongIndex(songIndex)

            val playerIntent = Intent(this, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(playerIntent)
            } else {
                startService(playerIntent)
            }
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
