package com.cain.videotemp.pic.opengl

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @author : jiangyu
 * @date   : 2020/5/10
 * @desc   : xxx
 */
class BitmapDrawer(private val mBitmap: Bitmap) {
    // 顶点坐标
    private val mVertexCoors = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

    // 纹理坐标
    private val mTextureCoors = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

    // 纹理ID
    private var mTextureId: Int = -1

    //OpenGL程序ID
    private var mProgram: Int = -1

    // 顶点坐标接收者
    private var mVertexPosHandler: Int = -1

    // 纹理坐标接收者
    private var mTexturePosHandler: Int = -1

    // 纹理接收者
    private var mTextureHandler: Int = -1
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureBuffer: FloatBuffer

    init {
        //【步骤1: 初始化顶点坐标】
        initPos()
    }

    private fun initPos() {
        val bb = ByteBuffer.allocateDirect(mVertexCoors.size * 4)
        bb.order(ByteOrder.nativeOrder())
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer.put(mVertexCoors)
        mVertexBuffer.position(0)

        val cc = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
        cc.order(ByteOrder.nativeOrder())
        mTextureBuffer = cc.asFloatBuffer()
        mTextureBuffer.put(mTextureCoors)
        mTextureBuffer.position(0)
    }

    private fun createGLPrg() {
        if (mProgram == -1) {
            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, getFragmentShader())
            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES30.glCreateProgram()
            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES30.glCreateProgram()
            //将顶点着色器加入到程序
            GLES30.glAttachShader(mProgram, vertexShader)
            //将片元着色器加入到程序中
            GLES30.glAttachShader(mProgram, fragmentShader)
            //连接到着色器程序
            GLES30.glLinkProgram(mProgram)
            mVertexPosHandler = GLES30.glGetAttribLocation(mProgram, "aPosition")
            mTexturePosHandler = GLES30.glGetAttribLocation(mProgram, "aCoordinate")
            mTextureHandler = GLES30.glGetUniformLocation(mProgram, "uTexture")
        }
        //使用OpenGL程序
        GLES30.glUseProgram(mProgram)
    }

    private fun activateTexture() {
        //激活指定纹理单元
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        //绑定纹理ID到纹理单元
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)
        //将激活的纹理单元传递到着色器里面
        GLES30.glUniform1i(mTextureHandler, 0)
        //配置边缘过渡参数
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    private fun bindBitmapToTexture() {
        if (!mBitmap.isRecycled) {
            //绑定图片到被激活的纹理单元
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mBitmap, 0)
        }
    }

    private fun doDraw() {
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(mVertexPosHandler)
        GLES30.glEnableVertexAttribArray(mTexturePosHandler)
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES30.glVertexAttribPointer(mVertexPosHandler, 2, GLES30.GL_FLOAT, false, 0, mVertexBuffer)
        GLES30.glVertexAttribPointer(mTexturePosHandler, 2, GLES30.GL_FLOAT, false, 0, mTextureBuffer)
        //开始绘制
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  gl_Position = aPosition;" +
                "  vCoordinate = aCoordinate;" +
                "}"
    }

    private fun getFragmentShader(): String {
        return "precision mediump float;" +
                "uniform sampler2D uTexture;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  gl_FragColor = color;" +
                "}"
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES30.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        return shader
    }

    fun setTextureID(id: Int) {
        mTextureId = id
    }

    fun draw() {
        if (mTextureId != -1) {
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg()
            //【步骤3: 激活并绑定纹理单元】
            activateTexture()
            //【步骤4: 绑定图片到纹理单元】
            bindBitmapToTexture()
            //【步骤5: 开始渲染绘制】
            doDraw()
        }
    }

    fun release() {
        GLES30.glDisableVertexAttribArray(mVertexPosHandler)
        GLES30.glDisableVertexAttribArray(mTexturePosHandler)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES30.glDeleteProgram(mProgram)
    }
}