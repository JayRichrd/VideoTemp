package com.cain.videotemp

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cain.videotemp.pic.opengl.BitmapDrawer
import com.cain.videotemp.pic.opengl.EglCore
import com.cain.videotemp.pic.opengl.WindowSurface
import com.cain.videotemp.pic.opengl.render.EGLRender
import com.cain.videotemp.pic.opengl.render.NativeRender
import com.cain.videotemp.pic.opengl.render.SimpleRender

class SimpleRenderActivity : AppCompatActivity(), SurfaceHolder.Callback {
    companion object {
        const val TAG = "SimpleRenderActivity"
        const val TYPE_RENDER = "render_type"
        const val TYPE_JAVA_RENDER = 1
        const val TYPE_JNI_RENDER = 2
        const val TYPE_CUSTOM_CONTEXT = 3
    }

    lateinit var eglRender: EGLRender

    lateinit var mEglCore: EglCore
    lateinit var mDisplaySurface: WindowSurface
    val renderThread by lazy { HandlerThread("RenderThread") }
    lateinit var renderHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_simple_render)
        (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let {
            Toast.makeText(this, "OpenGL ${it.deviceConfigurationInfo.glEsVersion}", Toast.LENGTH_LONG).show()
        }

        when (intent.getIntExtra(TYPE_RENDER, TYPE_JAVA_RENDER)) {
            TYPE_JAVA_RENDER -> {
                customRenderMode(getJavaRender())
            }
            TYPE_JNI_RENDER -> {
                customRenderMode(getJniRender())
            }
            TYPE_CUSTOM_CONTEXT -> {
                customOpenGLContext()
            }
            else -> {
                customRenderMode(getJavaRender())
            }
        }
    }

    private fun customOpenGLContext() {
        eglRender = EGLRender()
        eglRender.nativeInit()
        val surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
    }

    private fun customRenderMode(render: GLSurfaceView.Renderer) {
        val glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setRenderer(render)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        setContentView(glSurfaceView)
    }

    private fun getJavaRender(): GLSurfaceView.Renderer = SimpleRender().apply {
        addDrawer(BitmapDrawer(BitmapFactory.decodeResource(resources, R.mipmap.cover)))
    }

    private fun getJniRender(): GLSurfaceView.Renderer = NativeRender(assets)

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderThread.start()
        renderHandler = Handler(renderThread.looper)
        renderHandler.post {
            onSurfaceCreated(holder)
        }
    }

    private fun onSurfaceCreated(holder: SurfaceHolder) {
        mEglCore = EglCore(EGL14.EGL_NO_CONTEXT,EglCore.FLAG_RECORDABLE)
        mDisplaySurface = WindowSurface(mEglCore, holder.getSurface(), false)
        mDisplaySurface.makeCurrent()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }
}
