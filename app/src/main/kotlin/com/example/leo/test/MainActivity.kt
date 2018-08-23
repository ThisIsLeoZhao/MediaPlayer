package com.example.leo.test

import android.os.Bundle
import android.util.Log

interface IOpenMediaHelper {
    fun openMedia(path: String)
    fun lastMedia(): String?
    fun nextMedia(): String?
}

class MainActivity : BaseActivity(), IOpenMediaHelper {
    private val TAG = Utils.getTag(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")

        setContentView(R.layout.activity_main)
    }

    override fun openMedia(path: String) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MediaFragment.newInstance(path))
                .addToBackStack(null)
                .commit()
    }

    override fun lastMedia(): String? {
        return null
    }

    override fun nextMedia(): String? {
        return null
    }
}
