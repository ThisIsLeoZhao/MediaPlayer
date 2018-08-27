package com.example.leo.test

import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.leo.test.MediaPlayerService.Companion.MEDIA_PATH_KEY
import com.example.leo.test.databinding.FragmentMediaBinding
import java.util.*
import kotlinx.android.synthetic.main.fragment_media.*

class MediaFragment : Fragment() {

    private var mediaPlayerService: MediaPlayerService? = null
    private val isPlaying = ObservableField(true)

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val mediaPlayerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mediaPlayerService = (service as MediaPlayerService.MediaPlayerBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mediaPlayerService = null
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val mediaReadyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setupViewStatus()
            playButton.isEnabled = true
        }
    }

    private val updateUI = object : Runnable {
        override fun run() {
            mediaPlayerService?.progress?.let {
                progressSeekBar.progress = it
                currentProgress.text = formatMilliSecLength(it)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        retainInstance = true

        val binding: FragmentMediaBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_media, container, false)
        setupDataBinding(binding)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setBackgroundColor(Color.WHITE)

        activity?.let {
            localBroadcastManager = LocalBroadcastManager.getInstance(it)
            localBroadcastManager.registerReceiver(mediaReadyBroadcastReceiver,
                    IntentFilter(MediaPlayerService.MEDIA_READY))
        }

        val playIntent = Intent(context, MediaPlayerService::class.java)
        playIntent.putExtra(MEDIA_PATH_KEY, arguments?.getString(MediaPlayerService.MEDIA_PATH_KEY))
        context?.startService(playIntent)
    }

    override fun onStart() {
        super.onStart()

        handler.post(updateUI)

        context?.bindService(
                Intent(context, MediaPlayerService::class.java),
                mediaPlayerConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        // Remove scheduled access to the service
        handler.removeCallbacks(updateUI)

        localBroadcastManager.unregisterReceiver(mediaReadyBroadcastReceiver)

        mediaPlayerService = null
        context?.unbindService(mediaPlayerConnection)
    }

    private fun setupViewStatus() {
        mediaPlayerService?.run {
            progressSeekBar.max = mediaLength
            progressSeekBar.progress = progress
            totalLength.text = formatMilliSecLength(mediaLength)
            currentProgress.text = formatMilliSecLength(progress)

            progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(progressSeekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        seekToPosition(this@MediaFragment.progressSeekBar.progress)
                        currentProgress.text = formatMilliSecLength(progress)
                    }
                }

                override fun onStartTrackingTouch(progressSeekBar: SeekBar) {

                }

                override fun onStopTrackingTouch(progressSeekBar: SeekBar) {

                }
            })
        }
    }

    private fun formatMilliSecLength(millis: Int): String {
        val sec = millis / 1000

        return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
    }

    private fun setupDataBinding(binding: FragmentMediaBinding) {
        binding.mainView = this
        binding.isPlaying = isPlaying
    }

    fun playOrPauseMediaPlay(view: View) {
        isPlaying.run {
            if (get() == true) {
                mediaPlayerService?.pause()
            } else {
                mediaPlayerService?.play()
            }

            set(get()?.not())
        }
    }

    fun previousMedia(view: View) {
        mediaPlayerService?.previousMedia()
    }

    fun nextMedia(view: View) {
        mediaPlayerService?.nextMedia()
    }

    companion object {
        fun newInstance(mediaPath: String) = MediaFragment().apply {
            arguments = Bundle().apply {
                putString(MediaPlayerService.MEDIA_PATH_KEY, mediaPath)
            }
        }
    }
}
