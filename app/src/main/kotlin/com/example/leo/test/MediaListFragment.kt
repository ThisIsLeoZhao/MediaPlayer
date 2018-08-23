package com.example.leo.test

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import java.util.ArrayList
import kotlinx.android.synthetic.main.fragment_media_list.*

class MediaListFragment : Fragment() {
    private lateinit var listAdapter: MediaAdapter
    private lateinit var openMediaHelper: IOpenMediaHelper
    private lateinit var mediaListViewModel: MediaListViewModel

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

            mediaListViewModel = ViewModelProviders.of(it).get(MediaListViewModel::class.java)
            context?.let {
                listAdapter = MediaAdapter(it, ArrayList())
                mediaList.adapter = listAdapter

                mediaListViewModel.getAllMedias(it).observe(this, Observer {
                    listAdapter.clear()
                    listAdapter.addAll(it)
                    listAdapter.notifyDataSetChanged()
                })
            }
        }



        mediaList.setOnItemClickListener { parent, view1, position, id ->
            val path = view1.tag as String
            openMediaHelper.openMedia(path)
            mediaListViewModel.select(path)
        }
    }

    private inner class MediaAdapter(context: Context, medias: List<Media>) : ArrayAdapter<Media>(context, 0, medias) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val retView = convertView ?:
            LayoutInflater.from(context).inflate(R.layout.fragment_media_listitem, parent, false)

            return retView.apply {
                getItem(position).let {
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
