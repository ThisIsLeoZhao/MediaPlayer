package com.example.leo.test;

import android.Manifest;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MediaListFragment extends Fragment {
    private ListView mMediaList;
    private MediaAdapter mListAdapter;
    private IOpenMediaHelper mOpenMediaHelper;
    private MediaListViewModel mMediaListViewModel;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mOpenMediaHelper = (IOpenMediaHelper) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        }

        mListAdapter = new MediaAdapter(getContext(), new ArrayList<>());
        mMediaList = view.findViewById(R.id.mediaList);
        mMediaList.setAdapter(mListAdapter);
        mMediaList.setOnItemClickListener((parent, view1, position, id) -> {
            final String path = (String) view1.getTag();
            mOpenMediaHelper.openMedia(path);
            mMediaListViewModel.select(path);
        });

        mMediaListViewModel = ViewModelProviders.of(getActivity()).get(MediaListViewModel.class);
        mMediaListViewModel.getAllMedias(getContext()).observe(this, mediaList -> {
            mListAdapter.clear();
            mListAdapter.addAll(mediaList);
            mListAdapter.notifyDataSetChanged();
        });

    }

    private class MediaAdapter extends ArrayAdapter<Media> {

        public MediaAdapter(@NonNull Context context, List<Media> medias) {
            super(context, 0, medias);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_media_listitem, parent, false);
            }

            Media media = getItem(position);
            TextView title = convertView.findViewById(R.id.mediaTitle);
            TextView artist = convertView.findViewById(R.id.mediaArtist);

            title.setText(media.title);
            artist.setText(media.artist);

            convertView.setTag(media.url);

            return convertView;
        }
    }
}
