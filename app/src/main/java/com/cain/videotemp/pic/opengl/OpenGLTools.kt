package com.cain.videotemp.pic.opengl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @author : jiangyu
 * @date   : 2020/5/11
 * @desc   : xxx
 */
object OpenGLTools {

    private const val SIZEOF_FLOAT = 4

    fun createTextureIds(count: Int): IntArray {
        val texture = IntArray(count)
        GLES30.glGenTextures(count, texture, 0) //生成纹理
        return texture
    }

    fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }
}