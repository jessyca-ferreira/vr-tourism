package com.example.ra_quadrado.ar.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PlaneRenderer {
    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0
    private var verticesBuffer: FloatBuffer? = null

    fun createOnGlThread() {
        val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            attribute vec3 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);
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
    }

    fun drawPlanes(planes: Collection<Plane>, cameraPose: Pose, cameraProjection: FloatArray) {
        for (plane in planes) {
            if (plane.trackingState != com.google.ar.core.TrackingState.TRACKING || plane.subsumedBy != null) continue

            val pose = plane.centerPose
            val modelMatrix = FloatArray(16)
            pose.toMatrix(modelMatrix, 0)

            // --- THIS IS THE CORRECTED BLOCK ---
            val cameraViewMatrix = FloatArray(16)
            cameraPose.toMatrix(cameraViewMatrix, 0)

            val modelViewMatrix = FloatArray(16)
            Matrix.multiplyMM(modelViewMatrix, 0, cameraViewMatrix, 0, modelMatrix, 0)

            val modelViewProjectionMatrix = FloatArray(16)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)
            // --- END OF CORRECTION ---

            GLES20.glUseProgram(program)
            GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)
            GLES20.glUniform4f(colorUniform, 1.0f, 1.0f, 1.0f, 0.5f) // White with 50% transparency

            val planeVertices = plane.polygon
            if (verticesBuffer == null || verticesBuffer!!.capacity() < planeVertices.limit()) {
                verticesBuffer = ByteBuffer.allocateDirect(planeVertices.limit() * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
            }
            verticesBuffer!!.rewind()
            verticesBuffer!!.put(planeVertices)
            verticesBuffer!!.rewind()

            GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, verticesBuffer)
            GLES20.glEnableVertexAttribArray(positionAttrib)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBuffer!!.limit() / 3)
            GLES20.glDisableVertexAttribArray(positionAttrib)
        }
    }
}