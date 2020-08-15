package com.cain.videotemp.video

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cain.videotemp.R
import com.cain.videotemp.video.decoder.AudioDecoder
import com.cain.videotemp.video.decoder.VideoDecoder
import kotlinx.android.synthetic.main.activity_simple_video_player.*
import java.util.concurrent.Executors

class SimpleVideoPlayerActivity : AppCompatActivity() {
    val path = Environment.getExternalStorageDirectory().absolutePath + "/mvtest_2.mp4"
    var videoDecoder: VideoDecoder? = null
    var audioDecoder: AudioDecoder? = null
    companion object {
        const val PERMISSION_REQUEST_FROM_MAIN = 1
    }
    val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_video_player)
        // Android 6以上动态权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestNecessaryPermission(PERMISSIONS_STORAGE)
        }
    }

    fun clickToPlayer(view: View) {
        val threadPool = Executors.newFixedThreadPool(10)
        videoDecoder = VideoDecoder(path, sfv, null)
        threadPool.execute(videoDecoder)
        audioDecoder = AudioDecoder(path)
        threadPool.execute(audioDecoder)
        videoDecoder?.goOn()
        audioDecoder?.goOn()
    }

    override fun onDestroy() {
        videoDecoder?.stop()
        audioDecoder?.stop()
        super.onDestroy()
    }

    private fun requestNecessaryPermission(permissions: Array<String>) {
        val permissionList = arrayListOf<String>()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission)
            }
        }
        if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(),PERMISSION_REQUEST_FROM_MAIN)
        }
    }
}