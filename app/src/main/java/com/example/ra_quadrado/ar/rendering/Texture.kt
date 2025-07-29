package com.example.ra_quadrado.ar.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Texture {
    val textureId = IntArray(1)
    private val httpClient = OkHttpClient() // OkHttpClient for network requests
    var bitmapAspectRatio: Float = 1.0f // Expose aspect ratio of the loaded bitmap

    companion object {
        private const val TAG = "Texture"
        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
        )
        private val QUAD_TEX_COORDS = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
        val QUAD_VERTEX_BUF: FloatBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(QUAD_COORDS)
        val QUAD_TEX_BUF: FloatBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(QUAD_TEX_COORDS)

        init {
            QUAD_VERTEX_BUF.position(0)
            QUAD_TEX_BUF.position(0)
        }
    }

    fun createOnGlThread() {
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    /**
     * Updates the texture from a drawable resource.
     * This method should be called on the GL thread.
     */
    fun updateTexture(context: Context, resId: Int) {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        if (bitmap != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            bitmap.recycle()
            Log.d(TAG, "Texture updated from drawable resource: $resId, AspectRatio: $bitmapAspectRatio")
        } else {
            Log.e(TAG, "Failed to decode bitmap from resource: $resId")
            bitmapAspectRatio = 1.0f // Reset to default aspect ratio on failure
        }
    }

    /**
     * Updates the texture from a URL.
     * This method initiates an asynchronous network request and should be called from any thread.
     * The actual OpenGL texture update will be queued to the GL thread.
     *
     * @param glSurfaceView The GLSurfaceView to queue OpenGL operations on its GL thread.
     * @param imageUrl The URL of the image to download.
     * @param onComplete Optional callback to indicate success/failure of the download and texture update.
     */
    fun updateTextureFromUrl(glSurfaceView: GLSurfaceView, imageUrl: String, onComplete: (Boolean) -> Unit = {}) {
        if (imageUrl.isBlank()) {
            Log.w(TAG, "Image URL is blank, skipping texture update.")
            onComplete(false)
            return
        }

        val request = Request.Builder().url(imageUrl).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to download image from URL: $imageUrl, Error: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download image from URL: $imageUrl, HTTP: ${response.code}")
                    onComplete(false)
                    return
                }

                response.body?.bytes()?.let { imageBytes ->
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        glSurfaceView.queueEvent {
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
                            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                            bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat() // Update aspect ratio
                            bitmap.recycle()
                            Log.d(TAG, "Texture updated from URL: $imageUrl, AspectRatio: $bitmapAspectRatio")
                            onComplete(true)
                        }
                    } else {
                        Log.e(TAG, "Failed to decode bitmap from bytes for URL: $imageUrl")
                        onComplete(false)
                    }
                } ?: run {
                    Log.e(TAG, "Empty response body for URL: $imageUrl")
                    onComplete(false)
                }
            }
        })
    }
}
