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

    lateinit var mRender: EGLRender
    private val mMatrix = FloatArray(16)
    lateinit var mEglCore: EglCore
    lateinit var mDisplaySurface: WindowSurface
    lateinit var mOESInputFilter: OESInputFilter
    private var mInputTextureId = 0
    lateinit var mSurfaceTexture: SurfaceTexture
    lateinit var mSurface: Surface
    val renderThread by lazy { HandlerThread("RenderThread") }
    lateinit var mRenderHandler: Handler

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
        super.onDestroy()
        mRender.nativeRelease()
    }

    private fun customOpenGLContext() {
        mRender = EGLRender()
        mRender.nativeInit()
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
        internalSurfaceChanged(width, height)
    }

    private fun internalSurfaceChanged(width: Int, height: Int) {
        mRenderHandler.post({ onSurfaceChanged(width, height) })
    }

    private fun onSurfaceChanged(width: Int, height: Int) {
        mOESInputFilter.updateTextureBuffer()
        mRender.onSurfaceChanged(width, height)
    }

    private fun onSurfaceCreated(holder: SurfaceHolder) {
        mEglCore = EglCore(EGL14.EGL_NO_CONTEXT, EglCore.FLAG_RECORDABLE)
        mDisplaySurface = WindowSurface(mEglCore, holder.getSurface(), false)
        mDisplaySurface.makeCurrent()
        mOESInputFilter = OESInputFilter(this)
        mOESInputFilter.onInputSizeChanged(1080, 1920)
        mOESInputFilter.onDisplayChanged(1080, 1920)
        mInputTextureId = OpenGLTools.createTextureOES()
        mSurfaceTexture = SurfaceTexture(mInputTextureId)
        mSurfaceTexture.setDefaultBufferSize(1090, 1920)
        mSurfaceTexture.setOnFrameAvailableListener(this)
        mSurface = Surface(mSurfaceTexture)
        mRender.onSurfaceCreated(mSurface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        internalSurfaceDestroyed()
    }

    private fun internalSurfaceDestroyed() {
        mRenderHandler.post { onSurfaceDestroyed() }
    }

    private fun onSurfaceDestroyed() {
        if (mOESInputFilter != null) {
            mOESInputFilter.release()
//            mOESInputFilter = null
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release()
//            mSurfaceTexture = null
        }

        if (mDisplaySurface != null) {
            mDisplaySurface.release()
//            mDisplaySurface = null
        }

        if (mEglCore != null) {
            mEglCore.release()
//            mEglCore = null
        }

        mRender.onSurfaceDestroyed()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderThread.start()
        mRenderHandler = Handler(renderThread.looper)
        mRenderHandler.post {
            onSurfaceCreated(holder)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        requestRender()
    }

    private fun requestRender() {
        Log.d(TAG, "requestRender: ")
        mRenderHandler.post { onDrawFrame() }
    }

    private fun onDrawFrame() {
        if (mSurfaceTexture == null) {
            return
        }
        mSurfaceTexture.updateTexImage()
        mDisplaySurface.makeCurrent()
        mSurfaceTexture.getTransformMatrix(mMatrix)
        if (mOESInputFilter != null) {
            mOESInputFilter.setTextureTransformMatirx(mMatrix)
            // 绘制渲染
            GLES30.glViewport(0, 0, 1080, 1920)
            mOESInputFilter.drawFrame(mInputTextureId)
        }
        mDisplaySurface.swapBuffers()
    }
}
