package com.cain.videotemp.pic.opengl.render

import android.view.Surface

/**
 * @author : jiangyu
 * @date   : 2020/5/20
 * @desc   : xxx
 */
class EGLRender {
    external fun nativeInit()
    external fun nativeRelease()
    external fun onSurfaceCreated(surface: Surface)
    external fun onSurfaceChanged(width: Int, height: Int)
    external fun onSurfaceDestroyed()
}
