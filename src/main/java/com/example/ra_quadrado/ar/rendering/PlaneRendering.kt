package com.example.ra_quadrado.ar.rendering

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PlaneRenderer {
    private var program = 0
    private var positionAttrib = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0
    private var verticesBuffer: FloatBuffer? = null

    // A semi-transparent blue color is often less distracting than white for visualization
    private val planeColor = floatArrayOf(0.0f, 0.0f, 1.0f, 0.3f) // R, G, B, Alpha

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

    /**
     * Renders the collection of detected planes.
     *
     * @param planes The collection of ARCore planes to render.
     * @param viewMatrix The camera's view matrix.
     * @param projectionMatrix The camera's projection matrix.
     */
    fun draw(planes: Collection<Plane>, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        for (plane in planes) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) {
                continue // Skip planes that are not actively tracked or have been merged.
            }

            // Get the plane's position and orientation in world space.
            val modelMatrix = FloatArray(16)
            plane.centerPose.toMatrix(modelMatrix, 0)

            // --- THIS IS THE CORRECT MATRIX CALCULATION ---
            val modelViewMatrix = FloatArray(16)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)

            val modelViewProjectionMatrix = FloatArray(16)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
            // --- END OF CORRECTION ---

            // Update the vertex data for the plane polygon.
            val planePolygon = plane.polygon
            if (verticesBuffer == null || verticesBuffer!!.capacity() < planePolygon.capacity()) {
                val a = planePolygon.capacity()
                verticesBuffer = ByteBuffer.allocateDirect(a * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            }
            verticesBuffer!!.rewind()
            verticesBuffer!!.put(planePolygon)
            verticesBuffer!!.rewind()

            GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, verticesBuffer)

            // Set the shader uniforms.
            GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)
            GLES20.glUniform4fv(colorUniform, 1, planeColor, 0)

            // Draw the plane.
            GLES20.glEnableVertexAttribArray(positionAttrib)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBuffer!!.limit() / 3)
            GLES20.glDisableVertexAttribArray(positionAttrib)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}