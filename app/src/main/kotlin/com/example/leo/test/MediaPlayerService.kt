package com.example.leo.test

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import java.io.IOException

class MediaPlayerService : LifecycleService(), MediaPlayer.OnPreparedListener {
    private val TAG = Utils.getTag(this)

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer
    private var playingMediaPath: String? = null

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

        val path = intent.getStringExtra(MEDIA_PATH_KEY)
        if (path == playingMediaPath) {
            sendMediaBroadcast(MEDIA_READY)
            play()

            return Service.START_NOT_STICKY
        }

        setMedia(path)
        startForeground(100, setNotification())
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
        mediaListViewModel.moveToPreviousMedia()?.let {
            resetTo(it)
        }
    }

    fun nextMedia() {
        mediaListViewModel.moveToNextMedia()?.let {
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
        } catch (e: IOException) {
            // TODO: Fail to set data source
            e.printStackTrace()
        }

        playingMediaPath = path
        prepareAsync()
        setOnPreparedListener(this@MediaPlayerService)
    }

    private fun setNotification(): Notification {
        val intent1 = Intent(this, MediaPlayerService::class.java)
        intent1.putExtra(MEDIA_PATH_KEY, playingMediaPath)
        val pendingIntent = PendingIntent.getService(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        //        remoteViews.setOnClickPendingIntent(R.id.playButton, pendingIntent);


        // TODO: update remote view
        val notification = NotificationCompat.Builder(this)
                .setContentTitle("Track title")
                .setContentText("text")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous, "previous", null)
                .addAction(android.R.drawable.ic_media_play, "play", null)
                .addAction(android.R.drawable.ic_media_next, "next", null)
                .setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))
                .setProgress(100, 55, false)
                .setVisibility(VISIBILITY_PUBLIC)
                .build()

        //        notification.contentView = remoteViews;

        return notification
    }

    companion object {
        const val MEDIA_READY = "mediaReady"
        const val MEDIA_LIST_READY = "mediaListReady"
        const val MEDIA_PATH_KEY = "path"
    }
}
