package com.example.ra_quadrado.ar.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.ceil

class TextRenderer {

    private var programId: Int = 0
    private var positionHandle: Int = -1
    private var texCoordHandle: Int = -1
    private var mvpMatrixHandle: Int = -1
    private var textureHandle: Int = -1

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private var textTextureId: Int = 0
    private var textBitmapWidth = 1f // Actual width of the generated bitmap
    private var textBitmapHeight = 1f // Actual height of the generated bitmap
    private var bitmapAspectRatio = 1f


    companion object {
        private const val TAG = "TextRenderer"
    }

    private val vertexShaderCode = """
        uniform mat4 u_MvpMatrix;
        attribute vec4 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        void main() {
            gl_Position = u_MvpMatrix * a_Position;
            v_TexCoord = a_TexCoord;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D u_Texture;
        varying vec2 v_TexCoord;
        void main() {
            gl_FragColor = texture2D(u_Texture, v_TexCoord);
        }
    """

    init {
        val vertices = floatArrayOf(
            -0.5f,  0.5f, 0f,
            -0.5f, -0.5f, 0f,
            0.5f,  0.5f, 0f,
            0.5f, -0.5f, 0f
        )

        val texCoords = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordBuffer.put(texCoords).position(0)
    }

    fun createOnGlThread() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MvpMatrix")
        textureHandle = GLES20.glGetUniformLocation(programId, "u_Texture")
    }

    /**
     * Sets the text to be rendered. This method creates a bitmap from the text,
     * handling text wrapping if maxWidthPx is provided.
     *
     * @param text The text string to render.
     * @param textSizePx The size of the text in pixels.
     * @param textColor The color of the text.
     * @param bgColor The background color of the text bitmap (usually transparent).
     * @param maxWidthPx The maximum width in pixels for the text to wrap. If 0 or less, text will not wrap.
     */
    fun setText(text: String, textSizePx: Float = 64f, textColor: Int = Color.WHITE, bgColor: Int = Color.TRANSPARENT, maxWidthPx: Int = 0) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textSize = textSizePx
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isAntiAlias = true
        textPaint.style = Paint.Style.FILL

        val textToRender = if (text.isBlank()) " " else text // Ensure not empty for layout calculation

        val staticLayout: StaticLayout
        val desiredWidth: Int
        val desiredHeight: Int

        if (maxWidthPx > 0) {
            // Create StaticLayout for text wrapping
            staticLayout = StaticLayout.Builder.obtain(
                textToRender,
                0,
                textToRender.length,
                textPaint,
                maxWidthPx // Max width for wrapping
            )
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(true)
                .build()

            desiredWidth = staticLayout.width // This will be maxWidthPx or less if text is shorter
            desiredHeight = staticLayout.height // Total height of wrapped text
        } else {
            // No wrapping, calculate single line dimensions
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(textToRender, 0, textToRender.length, textBounds)
            desiredWidth = textBounds.width()
            desiredHeight = textBounds.height()
            staticLayout = StaticLayout.Builder.obtain(
                textToRender,
                0,
                textToRender.length,
                textPaint,
                desiredWidth.coerceAtLeast(1)
            ) // Use actual text width
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(true)
                .build()
        }

        // Add padding to the bitmap for better rendering
        val padding = 10 // pixels
        val bmpWidth = (desiredWidth + 2 * padding).coerceAtLeast(1)
        val bmpHeight = (desiredHeight + 2 * padding).coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        // Draw the StaticLayout onto the canvas
        canvas.save()
        canvas.translate(padding.toFloat(), padding.toFloat()) // Apply padding
        staticLayout.draw(canvas)
        canvas.restore()

        textBitmapWidth = bmpWidth.toFloat()
        textBitmapHeight = bmpHeight.toFloat()
        bitmapAspectRatio = textBitmapWidth / textBitmapHeight

        if (textTextureId != 0) {
            val textures = IntArray(1)
            textures[0] = textTextureId
            GLES20.glDeleteTextures(1, textures, 0)
        }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        Log.d(TAG, "Text bitmap created: Width=${textBitmapWidth}, Height=${textBitmapHeight}, AspectRatio=${bitmapAspectRatio}")
    }

    /**
     * Draws the text quad in AR space.
     *
     * @param modelMatrix The model matrix for the text quad's position and orientation.
     * @param viewMatrix The camera's view matrix.
     * @param projectionMatrix The camera's projection matrix.
     * @param targetWidthMeters The desired width of the text quad in AR world units (meters).
     */
    fun draw(modelMatrix: FloatArray, viewMatrix: FloatArray, projectionMatrix: FloatArray, targetWidthMeters: Float) {
        if (textTextureId == 0) return

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(programId)

        // Calculate the height based on the targetWidthMeters and the bitmap's aspect ratio
        val scaleX = targetWidthMeters
        val scaleY = targetWidthMeters / bitmapAspectRatio

        val scaleMatrix = FloatArray(16)
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.scaleM(scaleMatrix, 0, scaleX, scaleY, 1f)

        // Apply scale before model matrix
        val scaledModelMatrix = FloatArray(16)
        Matrix.multiplyMM(scaledModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0)

        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, scaledModelMatrix, 0)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTextureId)
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
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
        }
        return shader
    }
}
