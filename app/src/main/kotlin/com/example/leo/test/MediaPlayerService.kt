package com.example.leo.test

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.util.Log
import android.widget.RemoteViews
import java.io.IOException

interface IMediaPlayerService {
    /**
     * 获取播放位置
     */
    val progress: Int
    /**
     * 获取歌曲长度
     */
    val mediaLength: Int

    val mediaList: List<Media>
    val currentMedia: Media?

    fun play()

    fun pause()

    fun previousMedia()

    fun nextMedia()

    /**
     * 播放指定位置
     */
    fun seekToPosition(msec: Int)
}

class MediaPlayerService : LifecycleService(), MediaPlayer.OnPreparedListener, IMediaPlayerService {
    private val TAG = Utils.getTag(this)

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaListViewModel: MediaListViewModel

    private val remoteViews: RemoteViews? = null

    override val mediaLength get() = mediaPlayer.duration
    override val progress get() = mediaPlayer.currentPosition
    override val mediaList = mutableListOf<Media>()
    override val currentMedia get() = mediaListViewModel.currentMedia()

    override fun onPrepared(mp: MediaPlayer) {
        sendMediaBroadcast(MEDIA_READY)
        play()
    }

    private fun sendMediaBroadcast(event: String) {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.sendBroadcast(Intent(event))
    }

    inner class MediaPlayerBinder : Binder() {
        val service: IMediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")

        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaListViewModel = MediaListViewModel()
        mediaListViewModel.getAllMedias(this).observe(this, Observer { medias ->
            medias?.let { mediaList.addAll(it) }
            sendMediaBroadcast(MEDIA_LIST_READY)
        })
        notificationManager = NotificationManagerCompat.from(this)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()

        mediaPlayer.release()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent.getStringExtra(MEDIA_PATH_KEY)?.let {
            if (it == mediaListViewModel.currentMedia()?.url) {
                sendMediaBroadcast(MEDIA_READY)
                play()

                return Service.START_NOT_STICKY
            } else {
                setMedia(it)
                startForeground(ACTION_CODE_OPEN_MEDIA, setNotification())
            }
        }

        intent.getStringExtra(ACTION_KEY)?.let {
            when (it) {
                ACTION_NEXT -> nextMedia()
                ACTION_PREVIOUS -> previousMedia()
                ACTION_PLAY_PAUSE -> {
                    if (mediaPlayer.isPlaying) {
                        startForeground(ACTION_CODE_OPEN_MEDIA, setNotification(android.R.drawable.ic_media_play))
                        pause()
                    } else {
                        startForeground(ACTION_CODE_OPEN_MEDIA, setNotification(android.R.drawable.ic_media_pause))
                        play()
                    }
                }
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        Log.i(TAG, "onBind")
        return binder
    }

    override fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun previousMedia() {
        mediaListViewModel.previousMedia()?.let {
            resetTo(it)
            sendMediaBroadcast(PREVIOUS_MEDIA)
        }
    }

    override fun nextMedia() {
        mediaListViewModel.nextMedia()?.let {
            resetTo(it)
            sendMediaBroadcast(NEXT_MEDIA)
        }
    }

    private fun resetTo(path: String) {
        setMedia(path)
        play()
    }

    override fun seekToPosition(msec: Int) {
        mediaPlayer.seekTo(msec)
    }

    private fun setMedia(path: String) = mediaPlayer.run {
        reset()
        try {
            mediaListViewModel.select(path)
            setDataSource(path)
        } catch (e: IOException) {
            // TODO: Fail to set data source
            e.printStackTrace()
        }

        prepareAsync()
        setOnPreparedListener(this@MediaPlayerService)
    }

    private fun setNotification(playPauseDrawable: Int = android.R.drawable.ic_media_pause): Notification {
        fun getIntent() = Intent(this, MediaPlayerService::class.java)

        val pendingIntent = PendingIntent.getService(this, 0,
                getIntent().apply { putExtra(MEDIA_PATH_KEY, mediaListViewModel.currentMedia()?.url) },
                PendingIntent.FLAG_UPDATE_CURRENT)

        val pausePendingIntent = PendingIntent.getService(this, ACTION_CODE_PLAY_PAUSE,
                getIntent().apply { putExtra(ACTION_KEY, ACTION_PLAY_PAUSE) },
                PendingIntent.FLAG_UPDATE_CURRENT)

        val previousPendingIntent = PendingIntent.getService(this, ACTION_CODE_PREVIOUS,
                getIntent().apply { putExtra(ACTION_KEY, ACTION_PREVIOUS) },
                PendingIntent.FLAG_UPDATE_CURRENT)

        val nextPendingIntent = PendingIntent.getService(this, ACTION_CODE_NEXT,
                getIntent().apply { putExtra(ACTION_KEY, ACTION_NEXT) },
                PendingIntent.FLAG_UPDATE_CURRENT)


        // TODO: update remote view

        return NotificationCompat.Builder(this)
                .setContentTitle("Track title")
                .setContentText("text")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous, "previous", previousPendingIntent)
                .addAction(playPauseDrawable, "play", pausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "next", nextPendingIntent)
                .setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))
                .setVisibility(VISIBILITY_PUBLIC)
                .build()
    }

    companion object {
        const val MEDIA_READY = "mediaReady"
        const val MEDIA_LIST_READY = "mediaListReady"
        const val PREVIOUS_MEDIA = "previousMedia"
        const val NEXT_MEDIA = "nextMedia"
        const val MEDIA_PATH_KEY = "path"
        const val ACTION_KEY = "notificationAction"
        const val ACTION_PLAY_PAUSE = "playPause"
        const val ACTION_PREVIOUS = "previous"
        const val ACTION_NEXT = "next"
        const val ACTION_CODE_OPEN_MEDIA = 100
        const val ACTION_CODE_PLAY_PAUSE = 101
        const val ACTION_CODE_PREVIOUS = 102
        const val ACTION_CODE_NEXT = 103
    }
}
