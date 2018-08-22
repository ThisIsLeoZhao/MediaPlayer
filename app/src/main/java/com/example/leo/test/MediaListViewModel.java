package com.example.leo.test;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MediaListViewModel extends ViewModel {
    private MutableLiveData<List<Media>> mMedias = new MutableLiveData<>();
    private int mCurrentSelectedMediaIndex;

    public LiveData<List<Media>> getAllMedias(Context context) {
        fetchMedias(context);
        return mMedias;
    }

    public void select(String mediaUrl) {
        for (int i = 0; i < mMedias.getValue().size(); i++) {
            if (mediaUrl.equals(mMedias.getValue().get(i).url)) {
                mCurrentSelectedMediaIndex = i;
                return;
            }
        }
    }

    public String moveToNextMedia() {
        mCurrentSelectedMediaIndex = (mCurrentSelectedMediaIndex + 1) % mMedias.getValue().size();
        return getCurrentMedia();
    }

    public String moveToPreviousMedia() {
        mCurrentSelectedMediaIndex = mCurrentSelectedMediaIndex == 0 ? mMedias.getValue().size() - 1
                : mCurrentSelectedMediaIndex - 1;

        return getCurrentMedia();
    }

    public String getCurrentMedia() {
        return mMedias.getValue().get(mCurrentSelectedMediaIndex).url;
    }

    private void fetchMedias(Context context) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "<>0", null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );

        List<Media> allMedias = new ArrayList<>(cursor.getCount());

        try {
            while (cursor.moveToNext()) {
                Media media = new Media();
                media.id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                media.title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                media.artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                media.url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                allMedias.add(media);
            }
        } finally {
            cursor.close();
        }

        mMedias.setValue(allMedias);
    }
}
