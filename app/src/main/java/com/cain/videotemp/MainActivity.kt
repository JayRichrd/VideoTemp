package com.cain.videotemp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cain.videotemp.SimpleRenderActivity.Companion.TYPE_JAVA_RENDER
import com.cain.videotemp.SimpleRenderActivity.Companion.TYPE_JNI_RENDER
import com.cain.videotemp.SimpleRenderActivity.Companion.TYPE_RENDER
import com.cain.videotemp.audio.Mp3Encoder
import com.cain.videotemp.audio.OpenSLEsDelegate
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_FROM_MAIN = 1
        const val DATA_DIR = "/videotest/"
        const val PCM_FILE = "background2.pcm"
        const val MP3_FILE = "test.mp3"
        const val ASSETS_MUSIC_FILE = "mydream.m4a"
        const val SAMPLE_RATE_HZ = 44100
        const val BYTE_RATE = 128
        const val AUDIO_CHANEL = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_OUT_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_OUT_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val INIT_OK = 1
        const val INIT_ERROR = 0

        val PERMISSIONS_STORAGE = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val PERMISSIONS_INTERNET = arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE)

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    private val mp3Encoder by lazy { Mp3Encoder() }
    private val openSLEsDelegate by lazy { OpenSLEsDelegate() }
    private val minbufferSize by lazy { AudioTrack.getMinBufferSize(SAMPLE_RATE_HZ, AUDIO_OUT_CHANNEL_CONFIG, AUDIO_OUT_FORMAT) }
    private val audioTrack by lazy {
        AudioTrack(
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                }.build(),
                AudioFormat.Builder().apply {
                    setSampleRate(SAMPLE_RATE_HZ)
                    setEncoding(AUDIO_OUT_FORMAT)
                    setChannelMask(AUDIO_OUT_CHANNEL_CONFIG)
                }.build(),
                minbufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_pcm_2_mp3.setOnClickListener(this)
        btn_ffmpeg_play.setOnClickListener(this)
        btn_audio_track_play.setOnClickListener(this)
        btn_open_sl_play.setOnClickListener(this)
        btn_java_open_gl.setOnClickListener(this)
        btn_jni_open_gl.setOnClickListener(this)
        // Android 6以上动态权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestNecessaryPermission(PERMISSIONS_STORAGE)
            requestNecessaryPermission(PERMISSIONS_INTERNET)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_pcm_2_mp3 -> {
                pcm2mp3()
            }
            R.id.btn_ffmpeg_play -> {
                ffmpegPlay()
            }
            R.id.btn_audio_track_play -> {
                audioTrackPlayOrStop()
            }
            R.id.btn_open_sl_play -> {
                openSlPlay()
            }
            R.id.btn_java_open_gl -> {
                val intent = Intent(this, SimpleRenderActivity::class.java)
                intent.putExtra(TYPE_RENDER, TYPE_JAVA_RENDER)
                startActivity(intent)
            }
            R.id.btn_jni_open_gl -> {
                val intent = Intent(this, SimpleRenderActivity::class.java)
                intent.putExtra(TYPE_RENDER, TYPE_JNI_RENDER)
                startActivity(intent)
            }
            else -> {
                Log.w(TAG, "onClick# nothing to do.")
            }
        }
    }

    private fun openSlPlay() {
        Log.i(TAG, "openSlPlay###")
        openSLEsDelegate.playByAssets(assets, ASSETS_MUSIC_FILE)
    }

    private fun audioTrackPlayOrStop() {
        if (resources.getString(R.string.audio_track_play).equals(btn_audio_track_play.text.toString(), true)) {
            btn_audio_track_play.text = resources.getString(R.string.audio_track_stop)
            Thread(Runnable {
                try {
                    val pcmPath = Environment.getExternalStorageDirectory().absolutePath + DATA_DIR + PCM_FILE
                    val pcmFile = File(pcmPath)
                    if (pcmFile.exists()) {
                        Log.i(TAG, "audioTrackPlayOrStop# prepare to play pcm.")
                        val fileIns = FileInputStream(pcmFile)
                        val tempBuffer = ByteArray(minbufferSize)
                        while (fileIns.available() > 0) {
                            val readCount = fileIns.read(tempBuffer)
                            if (readCount == AudioTrack.ERROR_BAD_VALUE ||
                                readCount == AudioTrack.ERROR_INVALID_OPERATION) {
                                continue
                            }
                            if (readCount != 0 && readCount != -1) {
                                audioTrack.write(tempBuffer, 0, readCount)
                            }
                        }
                        Log.i(TAG, "audioTrackPlayOrStop# play finished.")
                        stopAudioTrack()
                    } else {
                        Log.e(TAG, "audioTrackPlayOrStop# file don't exist!")
                    }
                } catch (e: IOException) {
                    Log.i(TAG, "audioTrackPlayOrStop# ${e.message}")
                }
            }).start()
        } else {
            Log.i(TAG, "audioTrackPlayOrStop# prepare to stop play pcm.")
            stopAudioTrack()
        }
    }

    private fun stopAudioTrack() {
        Log.i(TAG, "stopAudioTrack###")
        audioTrack.play()
        audioTrack.release()
        runOnUiThread { btn_audio_track_play.text = resources.getString(R.string.audio_track_play) }

    }

    private fun ffmpegPlay() {
        Log.i(TAG, "ffmpegPlay###")
        if (!checkNecessaryPermission(PERMISSIONS_STORAGE)) {
            Log.e(TAG, "ffmpegPlay# there is no permission.")
            return
        }
        ffplay.play()
    }

    /**
     * 用户授权结果返回
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult===requestCode: $requestCode, permissions: ${permissions.toList()}, grantResults: ${grantResults.toList()}")
        if (requestCode == PERMISSION_REQUEST_FROM_MAIN &&
            (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) || permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            Log.i(TAG, "onRequestPermissionsResult# permision is granted.")
        } else {
            Log.w(TAG, "onRequestPermissionsResult# permision is denied.")
        }
    }

    private fun pcm2mp3() {
        if (!checkNecessaryPermission(PERMISSIONS_STORAGE)) {
            Log.e(TAG, "pcm2mp3# there is no permission.")
            return
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
                Toast.makeText(this, resources.getString(R.string.transform_finish), Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "pcm2mpp3# encode error.")
            }
        }
    }

    private fun requestNecessaryPermission(permissions: Array<String>) {
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

    private fun checkNecessaryPermission(permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "pcm2mp3# there is no permission.")
                return false
            }
        }
        return true
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
}
