package com.example.leo.test;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class BaseActivity extends AppCompatActivity {
    private WindowManager windowManager;

    private FrameLayout contentContainer;

    private View floatView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();

        contentContainer = (FrameLayout) ((ViewGroup) decorView.getChildAt(0)).getChildAt(1);

        floatView = LayoutInflater.from(this).inflate(R.layout.float_music_control, null);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.BOTTOM;
        contentContainer.addView(floatView, layoutParams);
    }
}
