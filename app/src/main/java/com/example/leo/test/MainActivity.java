package com.example.leo.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends BaseActivity implements IOpenMediaHelper {
    private final String TAG =
            Utils.getTag(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_main);
    }

    @Override
    public void openMedia(String path) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, MediaFragment.newInstance(path))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public String lastMedia() {
        return null;
    }

    @Override
    public String nextMedia() {
        return null;
    }
}
