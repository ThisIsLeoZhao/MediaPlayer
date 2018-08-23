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

    private lateinit var mediaListViewModel: MediaListViewModel

    private val mediaPlayerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mediaPlayerService = (service as MediaPlayerService.MediaPlayerBinder).service

            localBroadcastManager.registerReceiver(mMediaReadyBroadcastReceiver, IntentFilter(MediaPlayerService.MEDIA_READY))

            val playIntent = Intent(context, MediaPlayerService::class.java)
            playIntent.putExtra(MEDIA_PATH_KEY, arguments?.getString(MediaPlayerService.MEDIA_PATH_KEY))
            context?.startService(playIntent)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mediaPlayerService = null
        }
    }

    private val mMediaReadyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setupViewStatus()

            playButton.isEnabled = true

            handler.post(mUpdateUI)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val mUpdateUI = object : Runnable {
        override fun run() {
            mediaPlayerService?.progress?.let {
                progressSeekBar.progress = it
                currentProgress.text = formatMilliSecLength(it)
                handler.postDelayed(this, 1000)
            }
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

        context?.let {
            localBroadcastManager = LocalBroadcastManager.getInstance(it)
        }

        activity?.let {
            mediaListViewModel = ViewModelProviders.of(it).get(MediaListViewModel::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        context?.applicationContext?.bindService(Intent(context, MediaPlayerService::class.java), mediaPlayerConnection, BIND_AUTO_CREATE)
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

    override fun onStop() {
        super.onStop()
        // Remove scheduled access to the service
        handler.removeCallbacks(mUpdateUI)

        localBroadcastManager.unregisterReceiver(mMediaReadyBroadcastReceiver)

        mediaPlayerService = null
        context?.applicationContext?.unbindService(mediaPlayerConnection)
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
        mediaListViewModel.moveToPreviousMedia()?.let {
            mediaPlayerService?.resetTo(it)
        }
    }

    fun nextMedia(view: View) {
        mediaListViewModel.moveToNextMedia()?.let {
            mediaPlayerService?.resetTo(it)
        }
    }

    companion object {
        fun newInstance(mediaPath: String) = MediaFragment().apply {
            val args = Bundle()
            args.putString(MediaPlayerService.MEDIA_PATH_KEY, mediaPath)
            arguments = args
        }
    }
}
