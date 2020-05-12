package com.cain.videotemp.pic.opengl

import android.content.res.AssetManager
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author : jiangyu
 * @date   : 2020/5/11
 * @desc   : xxx
 */
class NativeRender(val assetManager: AssetManager) : GLSurfaceView.Renderer {

    override fun onDrawFrame(gl: GL10?) {
        glDraw()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glResize(width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glInit(assetManager)
    }

    external fun glInit(assetManager: AssetManager)
    external fun glResize(width: Int, height: Int)
    external fun glDraw()
}