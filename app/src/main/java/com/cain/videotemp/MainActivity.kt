package com.cain.videotemp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cain.videotemp.audio.Mp3Encoder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_FROM_MAIN = 1
        const val DATA_DIR = "/videotest/"
        const val PCM_FILE = "background2.pcm"
        const val MP3_FILE = "test.mp3"
        const val SAMPLE_RATE_HZ = 44100
        const val BYTE_RATE = 128
        const val AUDIO_CHANEL = AudioFormat.CHANNEL_IN_MONO
        const val INIT_OK = 1
        const val INIT_ERROR = 0
        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    val mp3Encoder by lazy { Mp3Encoder() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_pcm_2_mp3.setOnClickListener(this)
        // Android 6以上动态权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkNecessaryPermission(PERMISSIONS_STORAGE)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_pcm_2_mp3 -> {
                pcm2mp3()
            }
            else -> {
                Log.w(TAG, "onClick# nothing to do.")
            }
        }
    }

    /**
     * 用户授权结果返回
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult===requestCode: $requestCode, permissions: ${permissions.toList()}, grantResults: ${grantResults.toList()}")
        if (requestCode == PERMISSION_REQUEST_FROM_MAIN && (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) || permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            Log.i(TAG, "onRequestPermissionsResult# permision is granted.")
        } else {
            Log.w(TAG, "onRequestPermissionsResult# permision is denied.")
        }
    }

    private fun pcm2mp3() {
        PERMISSIONS_STORAGE.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "pcm2mp3# there is no permission.")
                return
            }
        }

        /**
         * 转换前，先将assets目录下的文件拷贝到sd卡中的/videotest/目录下
         */
        val pcmPath = Environment.getExternalStorageDirectory().absolutePath + DATA_DIR + PCM_FILE
        val pcmFile = File(pcmPath)
        if (!pcmFile.exists()) {
            Log.e(TAG, "pcm2mp3# pcm file does not exist.")
            return
        }

        val mp3Path = Environment.getExternalStorageDirectory().absolutePath + DATA_DIR + MP3_FILE
        val mp3File = File(mp3Path)
        if (mp3File.exists() && mp3File.delete()) {
            Log.i(TAG, "pcm2mp3# pcmPath: $pcmPath, mp3Path:$mp3Path")
            if (mp3Encoder.initEcoder(pcmPath, AUDIO_CHANEL, BYTE_RATE, SAMPLE_RATE_HZ, mp3Path) == INIT_OK) {
                Log.i(TAG, "pcm2mp3# init success and begin to encode data.")
                mp3Encoder.encode()
                mp3Encoder.destroy()
            } else {
                Log.e(TAG, "pcm2mpp3# encode error.")
            }
        }
    }

    private fun checkNecessaryPermission(permissions: Array<String>) {
        val permissionList = arrayListOf<String>()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission)
            }
        }
        if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), PERMISSION_REQUEST_FROM_MAIN)
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
}
