package com.cain.videotemp.pic.opengl.filter

import com.cain.videotemp.pic.opengl.OpenGLTools
import java.nio.FloatBuffer

/**
 * @author cainjiang
 * @date 2020/5/22
 */
open class BaseImageFilter {
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

}