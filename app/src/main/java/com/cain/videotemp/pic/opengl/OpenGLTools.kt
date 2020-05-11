package com.cain.videotemp.pic.opengl

import android.opengl.GLES30

/**
 * @author : jiangyu
 * @date   : 2020/5/11
 * @desc   : xxx
 */
object OpenGLTools {

    fun createTextureIds(count: Int): IntArray {
        val texture = IntArray(count)
        GLES30.glGenTextures(count, texture, 0) //生成纹理
        return texture
    }
}