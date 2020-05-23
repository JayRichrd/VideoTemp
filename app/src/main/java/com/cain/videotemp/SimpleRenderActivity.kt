package com.cain.videotemp

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.EGL14
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cain.videotemp.pic.opengl.BitmapDrawer
import com.cain.videotemp.pic.opengl.EglCore
import com.cain.videotemp.pic.opengl.OpenGLTools
import com.cain.videotemp.pic.opengl.WindowSurface
import com.cain.videotemp.pic.opengl.filter.OESInputFilter
import com.cain.videotemp.pic.opengl.render.EGLRender
import com.cain.videotemp.pic.opengl.render.NativeRender
import com.cain.videotemp.pic.opengl.render.SimpleRender

class SimpleRenderActivity : AppCompatActivity(), SurfaceHolder.Callback, OnFrameAvailableListener {
    companion object {
        const val TAG = "SimpleRenderActivity"
        const val TYPE_RENDER = "render_type"
        const val TYPE_JAVA_RENDER = 1
        const val TYPE_JNI_RENDER = 2
        const val TYPE_CUSTOM_CONTEXT = 3
    }

    lateinit var eglRender: EGLRender
    private val matrix = FloatArray(16)
    lateinit var eglCore: EglCore
    lateinit var windowSurface: WindowSurface
    lateinit var oESInputFilter: OESInputFilter
    private var inputTextureId = 0
    lateinit var surfaceTexture: SurfaceTexture
    lateinit var surface: Surface
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

    override fun onDestroy() {
        Log.i(TAG, "onDestroy#")
        super.onDestroy()
        eglRender.nativeRelease()
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
        Log.i(TAG, "surfaceChanged# format: $format, width: $width, height: $height")
        internalSurfaceChanged(width, height)
    }

    private fun internalSurfaceChanged(width: Int, height: Int) {
        renderHandler.post({ onSurfaceChanged(width, height) })
    }

    private fun onSurfaceChanged(width: Int, height: Int) {
        oESInputFilter.updateTextureBuffer()
        eglRender.onSurfaceChanged(width, height)
    }

    private fun onSurfaceCreated(holder: SurfaceHolder) {
        eglCore = EglCore(EGL14.EGL_NO_CONTEXT, EglCore.FLAG_RECORDABLE)
        windowSurface = WindowSurface(eglCore, holder.getSurface(), false)
        windowSurface.makeCurrent()
        oESInputFilter = OESInputFilter(this)
        oESInputFilter.onInputSizeChanged(1080, 1920)
        oESInputFilter.onDisplayChanged(1080, 1920)
        inputTextureId = OpenGLTools.createTextureOES()
        surfaceTexture = SurfaceTexture(inputTextureId)
        surfaceTexture.setDefaultBufferSize(1090, 1920)
        surfaceTexture.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)
        eglRender.onSurfaceCreated(surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed# holder: $holder")
        internalSurfaceDestroyed()
    }

    private fun internalSurfaceDestroyed() {
        renderHandler.post { onSurfaceDestroyed() }
    }

    private fun onSurfaceDestroyed() {
        if (oESInputFilter != null) {
            oESInputFilter.release()
//            mOESInputFilter = null
        }
        if (surfaceTexture != null) {
            surfaceTexture.release()
//            mSurfaceTexture = null
        }

        if (windowSurface != null) {
            windowSurface.release()
//            mDisplaySurface = null
        }

        if (eglCore != null) {
            eglCore.release()
//            mEglCore = null
        }

        eglRender.onSurfaceDestroyed()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated# holder: $holder")
        renderThread.start()
        renderHandler = Handler(renderThread.looper)
        renderHandler.post {
            onSurfaceCreated(holder)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "onFrameAvailable# surfaceTexture: $surfaceTexture, timestamp: ${surfaceTexture.timestamp}")
        requestRender()
    }

    private fun requestRender() {
        renderHandler.post { onDrawFrame() }
    }

    private fun onDrawFrame() {
        if (surfaceTexture == null) {
            return
        }
        surfaceTexture.updateTexImage()
        windowSurface.makeCurrent()
        surfaceTexture.getTransformMatrix(matrix)
        if (oESInputFilter != null) {
            oESInputFilter.setTextureTransformMatirx(matrix)
            // 绘制渲染
            GLES30.glViewport(0, 0, 1080, 1920)
            oESInputFilter.drawFrame(inputTextureId)
        }
        windowSurface.swapBuffers()
    }
}
