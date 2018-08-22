package com.example.leo.test;

import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.leo.test.databinding.FragmentMediaBinding;
import java.util.Locale;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.example.leo.test.MediaPlayerService.MEDIA_PATH_KEY;

public class MediaFragment extends Fragment {

    private MediaPlayerService mMediaPlayerService;
    private final ObservableField<Boolean> mIsPlaying = new ObservableField<>(true);

    private Button mPlayButton;
    private Button mNextButton;
    private Button mLastButton;
    private SeekBar mSeekBar;
    private TextView mCurrentProgress;
    private TextView mTotalLength;

    private LocalBroadcastManager mLocalBroadcastManager;

    private MediaListViewModel mMediaListViewModel;

    private ServiceConnection mMediaPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMediaPlayerService = ((MediaPlayerService.MediaPlayerBinder) service).getService();

            mLocalBroadcastManager.registerReceiver(mMediaReadyBroadcastReceiver, new IntentFilter(MediaPlayerService.MEDIA_READY));

            Intent playIntent = new Intent(getContext(), MediaPlayerService.class);
            playIntent.putExtra(MEDIA_PATH_KEY, getArguments().getString(MediaPlayerService.MEDIA_PATH_KEY));
            getContext().startService(playIntent);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMediaPlayerService = null;
        }
    };

    private BroadcastReceiver mMediaReadyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupViewStatus();
            mPlayButton.setEnabled(true);

            mHandler.post(mUpdateUI);
        }
    };

    private Handler mHandler = new Handler((Looper.getMainLooper()));
    private Runnable mUpdateUI = new Runnable() {
        @Override
        public void run() {
            final int progress = mMediaPlayerService.getProgress();
            mSeekBar.setProgress(progress);
            mCurrentProgress.setText(formatMilliSecLength(progress));
            mHandler.postDelayed(mUpdateUI, 1000);
        }
    };

    public static MediaFragment newInstance(String mediaPath) {
        MediaFragment fragment = new MediaFragment();

        Bundle args = new Bundle();
        args.putString(MediaPlayerService.MEDIA_PATH_KEY, mediaPath);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        FragmentMediaBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_media, container, false);
        setupDataBinding(binding);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(Color.WHITE);

        mPlayButton = view.findViewById(R.id.playButton);
        mLastButton = view.findViewById(R.id.lastMediaButton);
        mNextButton = view.findViewById(R.id.nextMediaButton);

        mSeekBar = view.findViewById(R.id.progressSeekBar);
        mCurrentProgress = view.findViewById(R.id.currentProgress);
        mTotalLength = view.findViewById(R.id.totalLength);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        mMediaListViewModel = ViewModelProviders.of(getActivity()).get(MediaListViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        getContext().getApplicationContext().bindService(new Intent(getContext(), MediaPlayerService.class), mMediaPlayerConnection, BIND_AUTO_CREATE);
    }

    private void setupViewStatus() {
        mSeekBar.setMax(mMediaPlayerService.getMediaLength());
        mSeekBar.setProgress(mMediaPlayerService.getProgress());
        mTotalLength.setText(formatMilliSecLength(mMediaPlayerService.getMediaLength()));
        mCurrentProgress.setText(formatMilliSecLength(mMediaPlayerService.getProgress()));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMediaPlayerService.seekToPosition(mSeekBar.getProgress());
                    mCurrentProgress.setText(formatMilliSecLength(mMediaPlayerService.getProgress()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private String formatMilliSecLength(int millis) {
        final int sec = millis / 1000;

        return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    private void setupDataBinding(FragmentMediaBinding binding) {
        binding.setMainView(this);
        binding.setIsPlaying(mIsPlaying);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Remove scheduled access to the service
        mHandler.removeCallbacks(mUpdateUI);

        mLocalBroadcastManager.unregisterReceiver(mMediaReadyBroadcastReceiver);

        mMediaPlayerService = null;
        getContext().getApplicationContext().unbindService(mMediaPlayerConnection);
    }

    public void playOrPauseMediaPlay(View view) {
        assert (mIsPlaying.get() != null);

        if (mIsPlaying.get()) {
            mMediaPlayerService.pause();
        } else {
            mMediaPlayerService.play();
        }

        mIsPlaying.set(!mIsPlaying.get());
    }

    public void previousMedia(View view) {
        mMediaPlayerService.resetTo(mMediaListViewModel.moveToPreviousMedia());
    }

    public void nextMedia(View view) {
        mMediaPlayerService.resetTo(mMediaListViewModel.moveToNextMedia());
    }
}
