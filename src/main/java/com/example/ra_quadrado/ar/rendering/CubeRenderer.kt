package com.example.ra_quadrado.ar.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class CubeRenderer {
    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0
    private var vertexBuffer: FloatBuffer? = null
    private var drawListBuffer: ShortBuffer? = null

    private val CUBE_COORDS = floatArrayOf(
        -0.05f, -0.05f, -0.05f,
        -0.05f, -0.05f, 0.05f,
        -0.05f, 0.05f, -0.05f,
        -0.05f, 0.05f, 0.05f,
        0.05f, -0.05f, -0.05f,
        0.05f, -0.05f, 0.05f,
        0.05f, 0.05f, -0.05f,
        0.05f, 0.05f, 0.05f,
    )

    private val DRAW_ORDER = shortArrayOf(
        0, 1, 2, 2, 1, 3,
        4, 5, 6, 6, 5, 7,
        0, 1, 4, 4, 1, 5,
        2, 3, 6, 6, 3, 7,
        0, 2, 4, 4, 2, 6,
        1, 3, 5, 5, 3, 7
    )

    fun createOnGlThread() {
        val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * a_Position;
            }
        """.trimIndent()
        val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """.trimIndent()

        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also { GLES20.glShaderSource(it, VERTEX_SHADER); GLES20.glCompileShader(it) }
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also { GLES20.glShaderSource(it, FRAGMENT_SHADER); GLES20.glCompileShader(it) }
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
        vertexBuffer = ByteBuffer.allocateDirect(CUBE_COORDS.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(CUBE_COORDS).also { it.position(0) }
        drawListBuffer = ByteBuffer.allocateDirect(DRAW_ORDER.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(DRAW_ORDER).also { it.position(0) }
    }

    fun draw(modelMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val modelViewMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        GLES20.glUseProgram(program)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glUniform4f(colorUniform, 0.0f, 1.0f, 0.0f, 1.0f) // Green color
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, mvpMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, DRAW_ORDER.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        GLES20.glDisableVertexAttribArray(positionAttrib)
    }
}