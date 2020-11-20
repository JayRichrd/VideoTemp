package com.cain.videotemp

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_ffmpeg_filter.*

class FfmpegFilterActivity : AppCompatActivity(), SurfaceHolder.Callback {
    companion object {
        const val FILTER_DEMO_DIR = "/filter_demo/"
        const val FILTER_DEMO_INPUT_FILE = "test.mp4"
        const val TAG = "FfmpegFilterActivity"
    }

    private val inputFilePath = Environment.getExternalStorageDirectory().path + FILTER_DEMO_DIR + FILTER_DEMO_INPUT_FILE
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg_filter)
        surfaceHolder = surface_view.holder
        surfaceHolder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.i(TAG, "surfaceCreated#")
        Thread {
            play(inputFilePath, surfaceHolder.surface)
        }.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged# format = $format, widht = $width, height = $height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    external fun play(inputFile: String, surface: Surface):Int
}