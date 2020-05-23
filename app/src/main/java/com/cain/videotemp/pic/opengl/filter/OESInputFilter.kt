package com.cain.videotemp.pic.opengl.filter

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.cain.videotemp.pic.opengl.OpenGLTools

/**
 * @author : jiangyu
 * @date   : 2020/5/23
 * @desc   : xxx
 */
class OESInputFilter(ctx: Context, vertexShader: String, fragmentShader: String) : BaseImageFilter(ctx, vertexShader, fragmentShader) {
    companion object {
        const val TAG = "OESInputFilter"
        const val VERTEX_SHADER = "uniform mat4 uMVPMatrix;                               \n" +
                "uniform mat4 uTexMatrix;                               \n" +
                "attribute vec4 aPosition;                              \n" +
                "attribute vec4 aTextureCoord;                          \n" +
                "varying vec2 textureCoordinate;                            \n" +
                "void main() {                                          \n" +
                "    gl_Position = uMVPMatrix * aPosition;              \n" +
                "    textureCoordinate = (uTexMatrix * aTextureCoord).xy;   \n" +
                "}                                                      \n"

        // 备注： samplerExternalOES 格式是相机流的数据，跟渲染后的texture格式不一样
        // 相机流的数据经过渲染后得到的texture格式是sampler2D的
        const val FRAGMENT_SHADER_OES = "#extension GL_OES_EGL_image_external : require         \n" +
                "precision mediump float;                               \n" +
                "varying vec2 textureCoordinate;                            \n" +
                "uniform samplerExternalOES inputTexture;                   \n" +
                "void main() {                                          \n" +
                "    gl_FragColor = texture2D(inputTexture, textureCoordinate); \n" +
                "}                                                      \n"


    }

    private var muTexMatrixLoc = 0
    private lateinit var mTextureMatrix: FloatArray

    constructor(ctx: Context) : this(ctx, VERTEX_SHADER, FRAGMENT_SHADER_OES)

    init {
        muTexMatrixLoc = GLES30.glGetUniformLocation(programHandle, "uTexMatrix")
        // 视图矩阵
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onInputSizeChanged(width: Int, height: Int) {
        super.onInputSizeChanged(width, height)
        Log.i(TAG, "onInputSizeChanged# width: $width, height: $height")
        val aspect = width.toFloat() / height // 计算宽高比
        Matrix.perspectiveM(mProjectionMatrix, 0, 60f, aspect, 2f, 10f)
    }

    override fun getTextureType(): Int {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    }

    override fun onDrawArraysBegin() {
        Log.i(TAG, "onDrawArraysBegin# ")
        GLES30.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mTextureMatrix, 0)
    }

    fun updateTextureBuffer() {
        Log.i(TAG, "updateTextureBuffer# ")
        mTextureBuffer = OpenGLTools.getTextureBuffer()
    }

    /**
     * 设置SurfaceTexture的变换矩阵
     * @param texMatrix
     */
    fun setTextureTransformMatirx(texMatrix: FloatArray) {
        Log.i(TAG, "setTextureTransformMatirx# texMatrix: $texMatrix")
        mTextureMatrix = texMatrix
    }
}