package com.example.leo.test;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

public class SeparateProcessService extends Service {
    private final String TAG =
            getClass().getSimpleName() + System.identityHashCode(this);

    public static final int MSG_SAY_HELLO = 1;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate invoke");
        super.onCreate();
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
//                    Toast.makeText(getApplicationContext(), "New result from client: " + msg.arg1, Toast.LENGTH_SHORT).show();
                    if (msg.replyTo != null) {
                        try {
                            msg.replyTo.send(Message.obtain(null, 0,
                                    msg.arg1 + 1, 0));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final Messenger mMessenger = new Messenger(new MyHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind invoke");
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy invoke");
        super.onDestroy();
    }
}
