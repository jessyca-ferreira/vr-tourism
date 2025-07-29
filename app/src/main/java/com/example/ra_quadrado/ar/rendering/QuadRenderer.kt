package com.example.ra_quadrado.ar.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Renders a textured quad in ARCore. */
class QuadRenderer {
    // Make these mutable (var) and initialize to default/invalid values
    private var programId: Int = 0
    private var positionHandle: Int = -1
    private var texCoordHandle: Int = -1
    private var mvpMatrixHandle: Int = -1
    private var textureHandle: Int = -1

    // These can be initialized in the constructor as they don't involve OpenGL calls
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private val vertexShaderCode = """
        uniform mat4 u_MvpMatrix;
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;

        void main() {
            gl_Position = u_MvpMatrix * a_Position;
            v_TexCoord = a_TexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D u_Texture;
        varying vec2 v_TexCoord;

        void main() {
            gl_FragColor = texture2D(u_Texture, v_TexCoord);
        }
    """.trimIndent()

    companion object {
        private const val TAG = "QuadRenderer"
    }

    // Initialize non-OpenGL buffers in the constructor
    init {
        val vertices = floatArrayOf(
            -0.5f, 0.5f, 0.0f, // Top left
            -0.5f, -0.5f, 0.0f, // Bottom left
            0.5f, 0.5f, 0.0f,  // Top right
            0.5f, -0.5f, 0.0f   // Bottom right
        )

        val texCoords = floatArrayOf(
            0.0f, 0.0f, // Top left
            0.0f, 1.0f, // Bottom left
            1.0f, 0.0f, // Top right
            1.0f, 1.0f  // Bottom right
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordBuffer.put(texCoords).position(0)
    }

    /** Creates the OpenGL program and necessary objects on the GL thread. */
    fun createOnGlThread() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Failed to compile shaders during createOnGlThread. QuadRenderer will not function.")
            return // Early exit if shaders failed
        }

        programId = GLES20.glCreateProgram()
        if (programId == 0) {
            Log.e(TAG, "Could not create OpenGL program during createOnGlThread.")
            return // Early exit if program creation failed
        }

        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val infoLog = GLES20.glGetProgramInfoLog(programId)
            Log.e(TAG, "Could not link program during createOnGlThread: $infoLog")
            GLES20.glDeleteProgram(programId)
            programId = 0
            return // Early exit if program linking failed
        }

        GLES20.glDetachShader(programId, vertexShader)
        GLES20.glDetachShader(programId, fragmentShader)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MvpMatrix")
        textureHandle = GLES20.glGetUniformLocation(programId, "u_Texture")

        if (positionHandle == -1) Log.e(TAG, "Could not get a_Position location.")
        if (texCoordHandle == -1) Log.e(TAG, "Could not get a_TexCoord location.")
        if (mvpMatrixHandle == -1) Log.e(TAG, "Could not get u_MvpMatrix location.")
        if (textureHandle == -1) Log.e(TAG, "Could not get u_Texture location.")

        Log.d(TAG, "QuadRenderer initialized on GL thread. Program ID: $programId")
        Log.d(TAG, "Handles: position=$positionHandle, texCoord=$texCoordHandle, mvpMatrix=$mvpMatrixHandle, texture=$textureHandle")
    }

    /** Deletes the OpenGL program and objects. */
    fun releaseOnGlThread() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
    }

    fun draw(modelMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray, textureId: Int) {
        if (programId == 0) {
            Log.w(TAG, "Attempted to draw with invalid programId. Is createOnGlThread called?")
            return
        }
        if (positionHandle == -1 || texCoordHandle == -1 || mvpMatrixHandle == -1 || textureHandle == -1) {
            Log.w(TAG, "Attempted to draw with invalid attribute/uniform handles. Are they initialized?")
            return
        }

        GLES20.glUseProgram(programId)

        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Could not create shader of type $type")
            return 0
        }
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Could not compile shader $type:\n$infoLog")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}