package com.example.ra_quadrado.ar.rendering

import android.opengl.GLES20
import com.google.ar.core.PointCloud
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PointCloudRenderer {
    private var vbo = 0
    private var vboSize = 0
    private var programName = 0
    private var positionAttribute = 0
    private var mvpMatrixUniform = 0
    private var colorUniform = 0
    private var pointSizeUniform = 0
    private var numPoints = 0

    fun createOnGlThread() {
        val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            uniform float u_PointSize;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
                gl_PointSize = u_PointSize;
            }
        """.trimIndent()
        val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """.trimIndent()
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        vbo = buffers[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also { GLES20.glShaderSource(it, VERTEX_SHADER); GLES20.glCompileShader(it) }
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also { GLES20.glShaderSource(it, FRAGMENT_SHADER); GLES20.glCompileShader(it) }
        programName = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(programName, "u_Color")
        mvpMatrixUniform = GLES20.glGetUniformLocation(programName, "u_MvpMatrix")
        pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize")
    }

    fun update(pointCloud: PointCloud) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        numPoints = pointCloud.points.remaining() / 4
        if (vboSize < pointCloud.points.remaining()) {
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, pointCloud.points.remaining(), pointCloud.points, GLES20.GL_DYNAMIC_DRAW)
            vboSize = pointCloud.points.remaining()
        } else {
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointCloud.points.remaining(), pointCloud.points)
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(programName)
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f)
        GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(pointSizeUniform, 5.0f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }
}