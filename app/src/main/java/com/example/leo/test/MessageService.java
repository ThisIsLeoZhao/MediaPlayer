package com.example.leo.test;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Random;

public class MessageService extends Service {
    private final String TAG = Utils.getTag(this);
    private final Binder mBinder = new MessageBinder();

    class MessageBinder extends Binder {
        MessageBinder() {
            Log.i(TAG, "Create MessageBinder");
        }

        MessageService getService() {
            return MessageService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate invoke");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand invoke");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind invoke");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy invoke");
        super.onDestroy();
    }

    public int getResult() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        while (cursor.moveToNext()) {
            Log.i(TAG, cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))));
            Log.i(TAG, cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.DATA))));
        }

        return new Random().nextInt(100);
    }
}
