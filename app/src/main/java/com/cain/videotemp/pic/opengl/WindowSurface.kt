package com.cain.videotemp.pic.opengl

import android.util.Log
import android.view.Surface

/**
 * @author cainjiang
 * @date 2020/5/22
 */
class WindowSurface(eglCore: EglCore, var surface: Surface, val releaseSurface: Boolean) : EglSurfaceBase(eglCore) {
    companion object {
        const val TAG = "WindowSurface"
    }

    init {
        Log.i(TAG, "init# ")
        createWindowSurface(surface)
    }

    fun release() {
        Log.i(TAG, "release# ")
        releaseEglSurface()
        if (releaseSurface) {
            surface.release()
        }
    }
}