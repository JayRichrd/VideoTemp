package com.cain.videotemp.pic.opengl

import android.opengl.*
import android.util.Log

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

    var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    var mEGLContext = EGL14.EGL_NO_CONTEXT
    var mEGLConfig: EGLConfig? = null
    var mGlVersion = -1

    init {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }

        var config: EGLConfig?
        if (flags and FLAG_TRY_GLES3 != 0) {
            config = getConfig(flags, 3)
            config?.let {
                val attrib3_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                val context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext, attrib3_list, 0)
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    mEGLConfig = it
                    mEGLContext = context
                    mGlVersion = 3
                }
            }
        }

        if (mEGLContext === EGL14.EGL_NO_CONTEXT) {  // GLES 2 only, or GLES 3 attempt failed
            // GLES 2 only, or GLES 3 attempt failed
            //Log.d(TAG, "Trying GLES 2");
            config = getConfig(flags, 2)
            if (config == null) {
                throw java.lang.RuntimeException("Unable to find a suitable EGLConfig")
            }
            val attrib2_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext, attrib2_list, 0)
            checkEglError("eglCreateContext")
            mEGLConfig = config
            mEGLContext = context
            mGlVersion = 2
        }

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0)
        Log.d(TAG, "EGLContext created, client version ${values[0]}")
    }

    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
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
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,  // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        )

        if (flags and FLAG_RECORDABLE != 0) {
            attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
            attribList[attribList.size - 2] = 1
        }

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

    fun release(): Unit {
        if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLConfig = null
    }

    fun releaseSurface(eglSurface: EGLSurface): Unit {
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface)
    }
}