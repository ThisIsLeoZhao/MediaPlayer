package com.example.leo.test

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.provider.MediaStore
import java.util.*

class MediaListViewModel : ViewModel() {
    private val medias = MutableLiveData<List<Media>>()
    private var currentSelectedMediaIndex = 0

    fun getAllMedias(context: Context): LiveData<List<Media>> {
        fetchMedias(context)
        return medias
    }

    fun select(mediaUrl: String) = medias.value?.let {
        for (i in 0 until it.size) {
            if (mediaUrl == it[i].url) {
                currentSelectedMediaIndex = i
                break
            }
        }
    }

    fun nextMedia() = medias.value?.let {
        val nextIndex = (currentSelectedMediaIndex + 1) % it.size
        it[nextIndex].url
    }

    fun previousMedia() = medias.value?.let {
        val previousIndex = if (currentSelectedMediaIndex == 0)
            it.size - 1
        else
            currentSelectedMediaIndex - 1

        it[previousIndex].url
    }

    fun currentMedia() = medias.value?.get(currentSelectedMediaIndex)

    private fun fetchMedias(context: Context) {
        val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.IS_MUSIC + "<>0", null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            val allMedias = ArrayList<Media>(cursor.count)

            while (it.moveToNext()) {
                val media = Media().apply {
                    id = it.getInt(it.getColumnIndex(MediaStore.Audio.Media._ID))
                    title = it.getString(it.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    artist = it.getString(it.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    url = it.getString(it.getColumnIndex(MediaStore.Audio.Media.DATA))
                }

                allMedias.add(media)
            }
            medias.value = allMedias
        }
    }
}
