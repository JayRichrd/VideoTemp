package com.cain.videotemp.pic.opengl

import android.opengl.EGL14
import android.util.Log

/**
 * @author cainjiang
 * @date 2020/5/22
 */
open class EglSurfaceBase(private val eglCore: EglCore) {
    companion object {
        const val TAG = "EglSurfaceBase"
    }

    private var eGLSurface = EGL14.EGL_NO_SURFACE
    private var width = -1
    private var height = -1

    fun createWindowSurface(surface: Any) {
        Log.i(TAG, "createWindowSurface# surface: $surface")
        check(!(eGLSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        eGLSurface = eglCore.createWindowSurface(surface)
    }

    fun createOffscreenSurface(width: Int, height: Int) {
        Log.i(TAG, "createOffscreenSurface# width: $width, height: $height")
        check(!(eGLSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        eGLSurface = eglCore.createOffscreenSurface(width, height)
        this.width = width
        this.height = height
    }

    fun getWidth(): Int = if (width < 0) {
        eglCore.querySurface(eGLSurface, EGL14.EGL_WIDTH)
    } else {
        width
    }

    /**
     * Returns the surface's height, in pixels.
     */
    fun getHeight(): Int = if (height < 0) {
        eglCore.querySurface(eGLSurface, EGL14.EGL_HEIGHT)
    } else {
        height
    }

    /**
     * Release the EGL surface.
     */
    fun releaseEglSurface() {
        Log.i(TAG, "releaseEglSurface# ")
        eglCore.releaseSurface(eGLSurface)
        eGLSurface = EGL14.EGL_NO_SURFACE
        height = -1
        width = height
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        Log.i(TAG, "makeCurrent# ")
        eglCore.makeCurrent(eGLSurface)
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    open fun swapBuffers(): Boolean {
        Log.i(TAG, "swapBuffers# ")
        var result: Boolean
        if (!eglCore.swapBuffers(eGLSurface).also { result = it }) {
            Log.w(TAG, "swapBuffers# swapBuffers failed")
        }
        return result
    }
}