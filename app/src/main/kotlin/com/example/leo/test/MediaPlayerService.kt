package com.example.leo.test

import android.R
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

class MediaPlayerService : LifecycleService(), MediaPlayer.OnPreparedListener {
    private val TAG = Utils.getTag(this)

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaListViewModel: MediaListViewModel
    var mediaList: List<Media>? = null

    private val remoteViews: RemoteViews? = null

    /**
     * 获取歌曲长度
     */
    val mediaLength get() = mediaPlayer.duration

    /**
     * 获取播放位置
     */
    val progress get() = mediaPlayer.currentPosition

    override fun onPrepared(mp: MediaPlayer) {
        sendMediaBroadcast(MEDIA_READY)
        play()
    }

    private fun sendMediaBroadcast(event: String) {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.sendBroadcast(Intent(event))
    }

    inner class MediaPlayerBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")

        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaListViewModel = MediaListViewModel()
        mediaListViewModel.getAllMedias(this).observe(this, Observer { medias ->
            mediaList = medias?.toList()
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
            if (it == mediaListViewModel.currentMedia()) {
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

    fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }

    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun previousMedia() {
        mediaListViewModel.previousMedia()?.let {
            resetTo(it)
        }
    }

    fun nextMedia() {
        mediaListViewModel.nextMedia()?.let {
            resetTo(it)
        }
    }

    private fun resetTo(path: String) {
        setMedia(path)
        play()
    }

    /**
     * 播放指定位置
     */
    fun seekToPosition(msec: Int) {
        mediaPlayer.seekTo(msec)
    }

    private fun setMedia(path: String) = mediaPlayer.run {
        reset()
        try {
            setDataSource(path)
            mediaListViewModel.select(path)
        } catch (e: IOException) {
            // TODO: Fail to set data source
            e.printStackTrace()
        }

        prepareAsync()
        setOnPreparedListener(this@MediaPlayerService)
    }

    private fun setNotification(playPauseDrawable: Int = R.drawable.ic_media_pause): Notification {
        fun getIntent() = Intent(this, MediaPlayerService::class.java)

        val pendingIntent = PendingIntent.getService(this, 0,
                getIntent().apply { putExtra(MEDIA_PATH_KEY, mediaListViewModel.currentMedia()) },
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
        val notification = NotificationCompat.Builder(this)
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

        return notification
    }

    companion object {
        const val MEDIA_READY = "mediaReady"
        const val MEDIA_LIST_READY = "mediaListReady"
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
