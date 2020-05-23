package com.cain.videotemp.pic.opengl

import android.view.Surface

/**
 * @author cainjiang
 * @date 2020/5/22
 */
class WindowSurface(eglCore: EglCore, var mSurface: Surface?, val mReleaseSurface: Boolean) : EglSurfaceBase(eglCore) {
    companion object {
        const val TAG = "WindowSurface"
    }

    init {
        mSurface?.let {
            createWindowSurface(it)
        }
    }

    fun release() {
        releaseEglSurface()
        if (mSurface != null) {
            if (mReleaseSurface) {
                mSurface?.release()
            }
            mSurface = null
        }
    }
}