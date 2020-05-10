package com.cain.videotemp.pic.opengl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author : jiangyu
 * @date   : 2020/5/11
 * @desc   : xxx
 */
class SimpleRender(private val drawers: MutableList<BitmapDrawer> = mutableListOf()) : GLSurfaceView.Renderer {

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        drawers.forEach {
            it.draw()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        //开启混合，即半透明
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        val textureIds = OpenGLTools.createTextureIds(drawers.size)
        for ((index, drawer) in drawers.withIndex()) {
            drawer.setTextureID(textureIds[index])
        }
    }

    fun addDrawer(drawer: BitmapDrawer) {
        drawers.add(drawer)
    }
}