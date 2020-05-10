package com.cain.videotemp.pic.opengl

import android.opengl.GLES20

/**
 * @author : jiangyu
 * @date   : 2020/5/11
 * @desc   : xxx
 */
object OpenGLTools {

    fun createTextureIds(count: Int): IntArray {
        val texture = IntArray(count)
        GLES20.glGenTextures(count, texture, 0) //生成纹理
        return texture
    }
}