package com.cain.videotemp.pic.opengl.filter

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import android.text.TextUtils
import android.util.Log
import com.cain.videotemp.pic.opengl.OpenGLTools
import java.nio.FloatBuffer
import java.util.*

/**
 * @author cainjiang
 * @date 2020/5/22
 */
open class BaseImageFilter(val ctx: Context, vertexShader: String, fragmentShader: String) {
    companion object {
        const val TAG = "BaseImageFilter"
        const val VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;                                   \n" +
                    "uniform mat4 uTexMatrix;                                   \n" +
                    "attribute vec4 aPosition;                                  \n" +
                    "attribute vec4 aTextureCoord;                              \n" +
                    "varying vec2 textureCoordinate;                            \n" +
                    "void main() {                                              \n" +
                    "    gl_Position = uMVPMatrix * aPosition;                  \n" +
                    "    textureCoordinate =(uTexMatrix * aTextureCoord).xy;    \n" +
                    "}                                                          \n"

        const val FRAGMENT_SHADER_2D =
            "precision mediump float;                                   \n" +
                    "varying vec2 textureCoordinate;                            \n" +
                    "uniform sampler2D inputTexture;                                \n" +
                    "void main() {                                              \n" +
                    "    gl_FragColor = texture2D(inputTexture, textureCoordinate); \n" +
                    "}                                                          \n"

        const val SIZEOF_FLOAT = 4
        const val CoordsPerVertex = 3
        const val CoordsPerTexture = 2
        const val GL_NOT_INIT = -1
        val CubeVertices = floatArrayOf(
                -1.0f, -1.0f, 0.0f,  // 0 bottom left
                1.0f, -1.0f, 0.0f,  // 1 bottom right
                -1.0f, 1.0f, 0.0f,  // 2 top left
                1.0f, 1.0f, 0.0f)
        val TextureVertices = floatArrayOf(
                0.0f, 0.0f,  // 0 bottom left
                1.0f, 0.0f,  // 1 bottom right
                0.0f, 1.0f,  // 2 top left
                1.0f, 1.0f // 3 top right
        )

        val FULL_RECTANGLE_BUF: FloatBuffer = OpenGLTools.createFloatBuffer(CubeVertices)
    }

    private var mVertexBuffer: FloatBuffer = FULL_RECTANGLE_BUF
    protected var mTextureBuffer: FloatBuffer = OpenGLTools.createFloatBuffer(TextureVertices)
    private var mCoordsPerVertex: Int = CoordsPerVertex
    private var mVertexCount: Int = CubeVertices.size / mCoordsPerVertex
    protected var programHandle = 0
    private var muMVPMatrixLoc = 0
    private var maPositionLoc = 0
    private var maTextureCoordLoc = 0
    private var mInputTextureLoc = 0
    private var mTexMatrixLoc = 0

    // 渲染的Image的宽高
    private var imageWidth = 0
    private var imageHeight = 0

    // 显示输出的宽高
    private var displayWidth = 0
    private var displayHeight = 0

    // --- 视锥体属性 start ---
    // 视图矩阵
    protected var mViewMatrix = FloatArray(16)

    // 投影矩阵
    protected var mProjectionMatrix = FloatArray(16)

    // 模型矩阵
    private var mModelMatrix = FloatArray(16)

    // 变换矩阵
    private var mMVPMatrix = FloatArray(16)

    // 缩放矩阵
    private var mTexMatrix = FloatArray(16)

    // 模型矩阵欧拉角的实际角度
    protected var mYawAngle = 0.0f
    protected var mPitchAngle = 0.0f
    protected var mRollAngle = 0.0f

    // --- 视锥体属性 end ---
    // --- 视锥体属性 end ---
    private var runOnDraw: LinkedList<Runnable>

    // FBO属性
    private var framebuffers: IntArray? = null
    private var framebufferTextures: IntArray? = null
    private var mFrameWidth = -1
    private var mFrameHeight = -1

    init {
        runOnDraw = LinkedList()
        // 需要检查vertex/fragment shader是否不为空, 构建滤镜组需要用到这个
        // 需要检查vertex/fragment shader是否不为空, 构建滤镜组需要用到这个
        if (!TextUtils.isEmpty(vertexShader) && !TextUtils.isEmpty(fragmentShader)) {
            programHandle = OpenGLTools.createProgram(vertexShader, fragmentShader)
            maPositionLoc = GLES30.glGetAttribLocation(programHandle, "aPosition")
            maTextureCoordLoc = GLES30.glGetAttribLocation(programHandle, "aTextureCoord")
            muMVPMatrixLoc = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix")
            mInputTextureLoc = GLES30.glGetUniformLocation(programHandle, "inputTexture")
            mTexMatrixLoc = GLES30.glGetUniformLocation(programHandle, "uTexMatrix")
        } else {
            programHandle = -1
            maPositionLoc = -1
            maTextureCoordLoc = -1
            muMVPMatrixLoc = -1
            mInputTextureLoc = -1
            mTexMatrixLoc = -1
        }
        initIdentityMatrix()
        Log.i(TAG, "init# runOnDraw: $runOnDraw, " +
                "programHandle: $programHandle, " +
                "maPositionLoc: $maPositionLoc, " +
                "maTextureCoordLoc: $maTextureCoordLoc, " +
                "muMVPMatrixLoc: $muMVPMatrixLoc, " +
                "mInputTextureLoc: $mInputTextureLoc, " +
                "mTexMatrixLoc: $mTexMatrixLoc")
    }

    private fun initIdentityMatrix() {
        Matrix.setIdentityM(mViewMatrix, 0)
        Matrix.setIdentityM(mProjectionMatrix, 0)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.setIdentityM(mTexMatrix, 0)
    }

    /**
     * Surface发生变化时调用
     * @param width
     * @param height
     */
    open fun onInputSizeChanged(width: Int, height: Int) {
        Log.d(TAG, "onInputSizeChanged# width = $width, height = $height")
        imageWidth = width
        imageHeight = height
    }

    /**
     * 显示视图发生变化时调用
     * @param width
     * @param height
     */
    open fun onDisplayChanged(width: Int, height: Int) {
        Log.d(TAG, "onDisplayChanged# width = $width, height = $height")
        displayWidth = width
        displayHeight = height
    }

    /**
     * 绘制Frame
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    open fun drawFrame(textureId: Int, vertexBuffer: FloatBuffer, textureBuffer: FloatBuffer): Boolean {
        Log.i(TAG, "drawFrame# textureId: $textureId")
        if (textureId == GL_NOT_INIT) {
            return false
        }
        GLES30.glUseProgram(programHandle)
        runPendingOnDrawTasks()
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(maPositionLoc, mCoordsPerVertex, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glEnableVertexAttribArray(maPositionLoc)
        textureBuffer.position(0)
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2, GLES30.GL_FLOAT, false, 0, textureBuffer)
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc)
        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMVPMatrix, 0)
        GLES30.glUniformMatrix4fv(mTexMatrixLoc, 1, false, mTexMatrix, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(getTextureType(), textureId)
        GLES30.glUniform1i(mInputTextureLoc, 0)
        onDrawArraysBegin()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mVertexCount)
        onDrawArraysAfter()
        GLES30.glDisableVertexAttribArray(maPositionLoc)
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc)
        GLES30.glBindTexture(getTextureType(), 0)
        GLES30.glUseProgram(0)
        return true
    }

    /**
     * 绘制Frame
     * @param textureId
     */
    open fun drawFrame(textureId: Int): Boolean {
        Log.i(TAG, "drawFrame# textureId: $textureId")
        return drawFrame(textureId, mVertexBuffer, mTextureBuffer)
    }

    /**
     * 绘制到FBO
     * @param textureId
     * @return FBO绑定的Texture
     */
    open fun drawFrameBuffer(textureId: Int): Int {
        return drawFrameBuffer(textureId, mVertexBuffer, mTextureBuffer)
    }

    /**
     * 绘制到FBO
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     * @return FBO绑定的Texture
     */
    open fun drawFrameBuffer(textureId: Int, vertexBuffer: FloatBuffer, textureBuffer: FloatBuffer): Int {
        if (framebuffers == null) {
            return GL_NOT_INIT
        }
        runPendingOnDrawTasks()
        GLES30.glViewport(0, 0, mFrameWidth, mFrameHeight)
        framebuffers?.let {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, it[0])
        }
        GLES30.glUseProgram(programHandle)
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(maPositionLoc, mCoordsPerVertex, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glEnableVertexAttribArray(maPositionLoc)
        textureBuffer.position(0)
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2, GLES30.GL_FLOAT, false, 0, textureBuffer)
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc)
        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMVPMatrix, 0)
        GLES30.glUniformMatrix4fv(mTexMatrixLoc, 1, false, mTexMatrix, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(getTextureType(), textureId)
        GLES30.glUniform1i(mInputTextureLoc, 0)
        onDrawArraysBegin()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mVertexCount)
        onDrawArraysAfter()
        GLES30.glDisableVertexAttribArray(maPositionLoc)
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc)
        GLES30.glBindTexture(getTextureType(), 0)
        GLES30.glUseProgram(0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, displayWidth, displayHeight)
        Log.i(TAG, "drawFrameBuffer# textureId: $textureId")
        return framebufferTextures?.get(0) ?: GL_NOT_INIT
    }

    protected open fun runPendingOnDrawTasks() {
        Log.i(TAG, "runPendingOnDrawTasks# ")
        while (!runOnDraw.isEmpty()) {
            runOnDraw.removeFirst().run()
        }
    }

    /**
     * 获取Texture类型
     * GLES30.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    open fun getTextureType(): Int {
        Log.i(TAG, "getTextureType# ")
        return GLES30.GL_TEXTURE_2D
    }

    /**
     * 调用drawArrays之前，方便添加其他属性
     */
    open fun onDrawArraysBegin() {}

    /**
     * drawArrays调用之后，方便销毁其他属性
     */
    open fun onDrawArraysAfter() {}

    /**
     * 释放资源
     */
    open fun release() {
        Log.i(TAG, "release# ")
        GLES30.glDeleteProgram(programHandle)
        programHandle = -1
        destroyFramebuffer()
    }

    open fun destroyFramebuffer() {
        Log.i(TAG, "destroyFramebuffer# ")
        if (framebufferTextures != null) {
            GLES30.glDeleteTextures(1, framebufferTextures, 0)
            framebufferTextures = null
        }
        if (framebuffers != null) {
            GLES30.glDeleteFramebuffers(1, framebuffers, 0)
            framebuffers = null
        }
        imageWidth = -1
        imageHeight = -1
    }

    protected open fun runOnDraw(runnable: Runnable?) {
        synchronized(runOnDraw) { runOnDraw.addLast(runnable) }
    }
}