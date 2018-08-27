package com.example.leo.test

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_media.*

import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_media_list.*

class MediaListFragment : Fragment() {
    private lateinit var listAdapter: MediaAdapter
    private lateinit var openMediaHelper: IOpenMediaHelper

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var mediaPlayerService: MediaPlayerService? = null

    private val mediaListReadyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateToLatestMediaList()
        }
    }

    private val mediaPlayerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mediaPlayerService = (service as MediaPlayerService.MediaPlayerBinder).service
            updateToLatestMediaList()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mediaPlayerService = null
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        openMediaHelper = activity as IOpenMediaHelper
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(it,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        0)
            }

            localBroadcastManager = LocalBroadcastManager.getInstance(it)

            context?.let {context ->
                listAdapter = MediaAdapter(context, ArrayList())
                mediaList.adapter = listAdapter
            }
        }

        mediaList.setOnItemClickListener { parent, view1, position, id ->
            val path = view1.tag as String
            openMediaHelper.openMedia(path)
        }
    }

    override fun onStart() {
        super.onStart()

        context?.bindService(
                Intent(context, MediaPlayerService::class.java),
                mediaPlayerConnection, Context.BIND_AUTO_CREATE)
        localBroadcastManager.registerReceiver(mediaListReadyBroadcastReceiver,
                IntentFilter(MediaPlayerService.MEDIA_LIST_READY))

        updateToLatestMediaList()
    }

    override fun onStop() {
        super.onStop()

        mediaPlayerService = null
        context?.unbindService(mediaPlayerConnection)
        localBroadcastManager.unregisterReceiver(mediaListReadyBroadcastReceiver)
    }

    private fun updateToLatestMediaList() = listAdapter.run {
        mediaPlayerService?.mediaList?.let {
            clear()
            addAll(it)
            notifyDataSetChanged()
        }
    }

    private class MediaAdapter(context: Context, medias: ArrayList<Media>) : ArrayAdapter<Media>(context, 0, medias) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val retView = convertView ?:
            LayoutInflater.from(context).inflate(R.layout.fragment_media_listitem, parent, false)

            return retView.apply {
                getItem(position)?.let {
                    val title = findViewById<TextView>(R.id.mediaTitle)
                    val artist = findViewById<TextView>(R.id.mediaArtist)

                    title.text = it.title
                    artist.text = it.artist

                    retView.tag = it.url
                }
            }
        }
    }
}
