package com.cain.videotemp.pic.opengl

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface

/**
 * @author : jiangyu
 * @date   : 2020/5/21
 * @desc   : xxx
 */
class EglCore(sharedContext: EGLContext, flags: Int) {
    companion object {
        const val TAG = "EglCore"
        const val FLAG_RECORDABLE = 0x01
        const val FLAG_TRY_GLES3 = 0x02
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    private var eGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eGLContext = EGL14.EGL_NO_CONTEXT
    private var eGLConfig: EGLConfig? = null
    private var glVersion = -1

    init {
        eGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eGLDisplay, version, 0, version, 1)) {
            eGLDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }

        var config: EGLConfig?
        if (flags and FLAG_TRY_GLES3 != 0) {
            Log.i(TAG, "init# FLAG_TRY_GLES3")
            config = getConfig(flags, 3)
            config?.let {
                val attrib3Array = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                val context = EGL14.eglCreateContext(eGLDisplay, config, sharedContext, attrib3Array, 0)
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    eGLConfig = it
                    eGLContext = context
                    glVersion = 3
                }
            }
        }

        if (eGLContext === EGL14.EGL_NO_CONTEXT) {  // GLES 2 only, or GLES 3 attempt failed
            // GLES 2 only, or GLES 3 attempt failed
            Log.d(TAG, "init# Trying GLES 2")
            config = getConfig(flags, 2)
            if (config == null) {
                throw java.lang.RuntimeException("Unable to find a suitable EGLConfig")
            }
            val attrib2Array = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(eGLDisplay, config, sharedContext, attrib2Array, 0)
            checkEglError("eglCreateContext")
            eGLConfig = config
            eGLContext = context
            glVersion = 2
        }

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(eGLDisplay, eGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0)
        Log.i(TAG, "init# EGLContext created, client version ${values[0]}")
    }

    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        Log.i(TAG, "getConfig# flags: $flags, version: $version")
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,  //EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,  // placeholder for recordable [@-3]
                EGL14.EGL_NONE)

        if (flags and FLAG_RECORDABLE != 0) {
            attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
            attribList[attribList.size - 2] = 1
        }

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            Log.e(TAG, "getConfig# unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

    fun release() {
        Log.i(TAG, "release# ")
        if (eGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(eGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(eGLDisplay, eGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eGLDisplay)
        }
        eGLDisplay = EGL14.EGL_NO_DISPLAY
        eGLContext = EGL14.EGL_NO_CONTEXT
        eGLConfig = null
    }

    fun releaseSurface(eglSurface: EGLSurface) {
        Log.i(TAG, "releaseSurface# eglSurface: $eglSurface")
        EGL14.eglDestroySurface(eGLDisplay, eglSurface)
    }

    protected fun finalize() {
        if (eGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            // We're limited here -- finalizers don't run on the thread that holds
            // the EGL state, so if a surface or context is still current on another
            // thread we can't fully release it here.  Exceptions thrown from here
            // are quietly discarded.  Complain in the log file.
            Log.w(TAG, "finalize# EglCore was not explicitly released, state may be leaked")
            release()
        }
    }

    fun createWindowSurface(surface: Any): EGLSurface {
        Log.i(TAG, "createWindowSurface# surface: $surface")
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw java.lang.RuntimeException("invalid surface: $surface")
        }

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(eGLDisplay, eGLConfig, surface, surfaceAttribs, 0)
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null) {
            throw java.lang.RuntimeException("surface was null")
        }
        return eglSurface
    }

    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        Log.i(TAG, "createOffscreenSurface# width: $width, height: $height")
        val surfaceAttribs = intArrayOf(
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreatePbufferSurface(eGLDisplay, eGLConfig, surfaceAttribs, 0)
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface == null) {
            throw java.lang.RuntimeException("surface was null")
        }
        return eglSurface
    }

    fun makeCurrent(eglSurface: EGLSurface) {
        if (eGLDisplay === EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.i(TAG, "makeCurrent# w/o display")
        }
        if (!EGL14.eglMakeCurrent(eGLDisplay, eglSurface, eglSurface, eGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface) {
        if (eGLDisplay === EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "makeCurrent(p1,p2)#  w/o display")
        }
        if (!EGL14.eglMakeCurrent(eGLDisplay, drawSurface, readSurface, eGLContext)) {
            throw RuntimeException("eglMakeCurrent(draw,read) failed")
        }
    }

    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        Log.i(TAG, "swapBuffers# eglSurface: $eglSurface")
        return EGL14.eglSwapBuffers(eGLDisplay, eglSurface)
    }

    fun setPresentationTime(eglSurface: EGLSurface, nsecs: Long): Unit {
        EGLExt.eglPresentationTimeANDROID(eGLDisplay, eglSurface, nsecs)
    }

    fun isCurrent(eglSurface: EGLSurface): Boolean = eGLContext == EGL14.eglGetCurrentContext() && eglSurface == EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)

    fun querySurface(eglSurface: EGLSurface, what: Int): Int {
        Log.i(TAG, "querySurface# eglSurface: $eglSurface, what: $what")
        val value = IntArray(1)
        EGL14.eglQuerySurface(eGLDisplay, eglSurface, what, value, 0)
        return value[0]
    }

}