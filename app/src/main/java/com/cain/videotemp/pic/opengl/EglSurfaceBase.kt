package com.cain.videotemp.pic.opengl

import android.opengl.EGL14
import android.util.Log

/**
 * @author cainjiang
 * @date 2020/5/22
 */
open class EglSurfaceBase(val mEglCore: EglCore) {
    companion object {
        const val TAG = "EglSurfaceBase"
    }

    private var mEGLSurface = EGL14.EGL_NO_SURFACE
    private var mWidth = -1
    private var mHeight = -1

    fun createWindowSurface(surface: Any): Unit {
        check(!(mEGLSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        mEGLSurface = mEglCore.createWindowSurface(surface)
    }

    fun createOffscreenSurface(width: Int, height: Int): Unit {
        check(!(mEGLSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        mEGLSurface = mEglCore.createOffscreenSurface(width, height)
        mWidth = width
        mHeight = height
    }

     fun getWidth(): Int {
        return if (mWidth < 0) {
            mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH)
        } else {
            mWidth
        }
    }

    /**
     * Returns the surface's height, in pixels.
     */
     fun getHeight(): Int {
        return if (mHeight < 0) {
            mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT)
        } else {
            mHeight
        }
    }

    /**
     * Release the EGL surface.
     */
     fun releaseEglSurface() {
        mEglCore.releaseSurface(mEGLSurface)
        mEGLSurface = EGL14.EGL_NO_SURFACE
        mHeight = -1
        mWidth = mHeight
    }

    /**
     * Makes our EGL context and surface current.
     */
     fun makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface)
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    open fun swapBuffers(): Boolean {
        val result = mEglCore.swapBuffers(mEGLSurface)
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed")
        }
        return result
    }


}