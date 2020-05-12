package com.cain.videotemp

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cain.videotemp.pic.opengl.BitmapDrawer
import com.cain.videotemp.pic.opengl.NativeRender
import com.cain.videotemp.pic.opengl.SimpleRender
import kotlinx.android.synthetic.main.activity_simple_render.*

class SimpleRenderActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SimpleRenderActivity"
        const val TYPE_RENDER = "render_type"
        const val TYPE_JAVA_RENDER = 1
        const val TYPE_JNI_RENDER = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val glSurfaceView = GLSurfaceView(this)
        //setContentView(R.layout.activity_simple_render)
        (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let {
            Toast.makeText(this, "OpenGL ${it.deviceConfigurationInfo.glEsVersion}", Toast.LENGTH_LONG).show()
        }
        val render: GLSurfaceView.Renderer = when (intent.getIntExtra(TYPE_RENDER, TYPE_JAVA_RENDER)) {
            TYPE_JAVA_RENDER -> {
                getJavaRender()
            }
            TYPE_JNI_RENDER -> {
                getJniRender()
            }
            else -> {
                Log.w(TAG, "onCreate# nothing to matched!")
                getJavaRender()
            }
        }
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setRenderer(render)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        setContentView(glSurfaceView)
    }

    private fun getJavaRender(): GLSurfaceView.Renderer = SimpleRender().apply {
        addDrawer(BitmapDrawer(BitmapFactory.decodeResource(resources, R.mipmap.cover)))
    }

    private fun getJniRender(): GLSurfaceView.Renderer = NativeRender(assets)
}
