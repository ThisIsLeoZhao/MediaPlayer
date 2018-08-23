package com.example.leo.test

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import java.io.IOException

class MediaPlayerService : Service(), MediaPlayer.OnPreparedListener {
    private val TAG = Utils.getTag(this)

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer
    private var playingMediaPath: String? = null

    private lateinit var notificationManager: NotificationManagerCompat
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
        sendMediaReadyBroadcast()
        play()
    }

    private fun sendMediaReadyBroadcast() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.sendBroadcast(Intent(MEDIA_READY))
    }

    inner class MediaPlayerBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")

        super.onCreate()
        mediaPlayer = MediaPlayer()
        notificationManager = NotificationManagerCompat.from(this)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()

        mediaPlayer.release()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val path = intent.getStringExtra(MEDIA_PATH_KEY)
        if (path == playingMediaPath) {
            sendMediaReadyBroadcast()
            return Service.START_NOT_STICKY
        }

        setMedia(path)
        startForeground(100, setNotification())
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
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

    fun resetTo(path: String) {
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
        intent1.putExtra(MEDIA_PATH_KEY, "")
        val pendingIntent = PendingIntent.getService(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        //        remoteViews.setOnClickPendingIntent(R.id.playButton, pendingIntent);


        // TODO: update remote view
        val notification = NotificationCompat.Builder(this)
                .setContentTitle("Much longer tesssssssssssssssss that cannot fit one line...")
                .setContentText("text")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Much longer thhhhhhhhhhhhhhhhhhext that cannot fit one line..."))
                .setProgress(100, 55, false)
                .build()

        //        notification.contentView = remoteViews;

        Handler(mainLooper).postDelayed({
            val notification1 = NotificationCompat.Builder(this)
                    .setContentText("Much longer ")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    //                .setContentIntent(pendingIntent)
                    .setTicker("Ticker")
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setStyle(NotificationCompat.BigTextStyle()
                            .bigText("Much lone line..."))
                    .addAction(android.R.drawable.sym_def_app_icon, "HEYYY", pendingIntent)
                    .setProgress(100, 95, false)
                    .build()
            notificationManager.notify(202, notification1)
        }, 5000)

        return notification
    }

    companion object {
        const val MEDIA_READY = "mediaReady"
        const val MEDIA_PATH_KEY = "path"
    }
}
