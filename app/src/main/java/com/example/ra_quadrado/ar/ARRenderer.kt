package com.example.ra_quadrado.ar

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView // Need this for queueEvent
import android.opengl.Matrix
import android.util.Log
import com.example.ra_quadrado.R // Import your R file to access drawables (for fallback)
import com.example.ra_quadrado.ar.rendering.BackgroundRenderer
import com.example.ra_quadrado.ar.rendering.PlaneRenderer
import com.example.ra_quadrado.ar.rendering.PointCloudRenderer
import com.example.ra_quadrado.ar.rendering.QuadRenderer
import com.example.ra_quadrado.ar.rendering.TextRenderer
import com.example.ra_quadrado.ar.rendering.Texture
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class ARRenderer(
    private val session: Session,
    private val context: android.content.Context,
    private val glSurfaceView: GLSurfaceView,
    private val onImageTracked: (isTracking: Boolean) -> Unit
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    private val quadRenderer = QuadRenderer()
    private val placeImageTexture = Texture() // Renamed for clarity, now holds the dynamically loaded image

    private var isImageCurrentlyTracking = false

    private val titleRenderer = TextRenderer()
    private val descRenderer = TextRenderer()

    private var titleText: String = "Carregando Local..."
    private var descriptionText: String = "Carregando curiosidade..."

    // Store the currently loaded image URL to avoid unnecessary reloads
    private var currentLoadedImageUrl: String? = null


    companion object {
        private const val TAG = "ARRenderer"
        private const val IMAGE_DATABASE_FILENAME = "imgs2.imgdb"
        // NUM_BRENNAND_IMAGES is no longer relevant for dynamic image loading
        // private const val NUM_BRENNAND_IMAGES = 5

        private fun checkGlError(glOperation: String) {
            var error: Int
            while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "$glOperation: glError $error")
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        checkGlError("glClearColor")

        backgroundRenderer.createOnGlThread()
        checkGlError("backgroundRenderer.createOnGlThread")

        planeRenderer.createOnGlThread()
        checkGlError("planeRenderer.createOnGlThread")

        pointCloudRenderer.createOnGlThread()
        checkGlError("pointCloudRenderer.createOnGlThread")

        quadRenderer.createOnGlThread()
        checkGlError("quadRenderer.createOnGlThread")

        titleRenderer.createOnGlThread()
        descRenderer.createOnGlThread()

        titleRenderer.setText(titleText)
        descRenderer.setText(descriptionText)

        // Load a default placeholder image initially, or keep it blank
        placeImageTexture.createOnGlThread() // Create the texture before updating it
        placeImageTexture.updateTexture(context, R.drawable.brennand1) // Assuming you have a placeholder_image.png
        currentLoadedImageUrl = "placeholder_image" // Track the default

        session.setCameraTextureName(backgroundRenderer.textureId)
        checkGlError("session.setCameraTextureName")

        val arConfig = Config(session)

        try {
            context.assets.open(IMAGE_DATABASE_FILENAME).use { inputStream ->
                val augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, inputStream)
                arConfig.augmentedImageDatabase = augmentedImageDatabase
                Log.d(TAG, "Augmented Image Database loaded successfully.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not load augmented image database: $IMAGE_DATABASE_FILENAME", e)
        }

        session.configure(arConfig)
        checkGlError("session.configure")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        session.setDisplayGeometry(0, width, height)
        GLES20.glViewport(0, 0, width, height)
        checkGlError("glViewport")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        checkGlError("glClear")

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        checkGlError("glEnable(GL_DEPTH_TEST)")

        GLES20.glDisable(GLES20.GL_BLEND)

        val frame: Frame
        try {
            frame = session.update()
        } catch (e: SessionPausedException) {
            return
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during ARCore update: ${e.message}")
            return
        }

        backgroundRenderer.draw(frame)
        checkGlError("backgroundRenderer.draw")

        val camera = frame.camera
        if (camera.trackingState == TrackingState.PAUSED) {
            updateImageRecognitionState(false)
            return
        }

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        frame.acquirePointCloud().use { pointCloud ->
            pointCloudRenderer.update(pointCloud)
            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, projmtx, 0, viewmtx, 0)
            pointCloudRenderer.draw(mvpMatrix)
            checkGlError("pointCloudRenderer.draw")
        }

        val trackedImages = session.getAllTrackables(AugmentedImage::class.java)
        var isImageFoundThisFrame = false

        val projMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)


        for (image in trackedImages) {
            if (image.trackingState == TrackingState.TRACKING &&
                image.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING) {

                isImageFoundThisFrame = true

                val imagePose = image.centerPose

                val imageWidthMeters = image.extentX
                val imageHeightMeters = image.extentZ // Note: extentZ is typically height for AugmentedImage

                // Use the aspect ratio from the currently loaded texture
                val currentTextureAspectRatio = placeImageTexture.bitmapAspectRatio

                val quadScaleX = imageWidthMeters
                // Scale Y based on the actual aspect ratio of the loaded image
                val quadScaleY = imageWidthMeters / currentTextureAspectRatio

                Log.d(TAG, "Augmented Image Tracking: ${image.name}, State: ${image.trackingState.name}")


                val modelMatrix = FloatArray(16)
                imagePose.toMatrix(modelMatrix, 0)

                Matrix.translateM(modelMatrix, 0, modelMatrix, 0, 0.0f, 0.0f, 0.005f)
                Matrix.rotateM(modelMatrix, 0, modelMatrix, 0, -90.0f, 1.0f, 0.0f, 0.0f)

                Matrix.scaleM(modelMatrix, 0, modelMatrix, 0, quadScaleX, quadScaleY, 1.0f)

                val textMaxWidth = imageWidthMeters * 10f

                val titleOffset = image.extentZ / 2 + 0.6f
                val descOffset = -image.extentZ / 2 - 0.05f

                val titleMatrix = modelMatrix.copyOf()
                Matrix.translateM(titleMatrix, 0, titleMatrix, 0, 0f, titleOffset, 0.02f)
                titleRenderer.draw(titleMatrix, viewMatrix, projMatrix, textMaxWidth)

                placeImageTexture.textureId[0].let { textureId ->
                    if (textureId != 0) {
                        GLES20.glEnable(GLES20.GL_BLEND)
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

                        quadRenderer.draw(modelMatrix, viewmtx, projmtx, textureId)
                        checkGlError("quadRenderer.draw for image overlay")

                        GLES20.glDisable(GLES20.GL_BLEND)
                        Log.d(TAG, "QuadRenderer.draw called for image overlay for tracked image.")
                    } else {
                        Log.e(TAG, "Place image texture not loaded (ID is 0).")
                    }
                }
            }
        }

        updateImageRecognitionState(isImageFoundThisFrame)
    }

    // onScreenTapped remains the same, as it's not directly responsible for image loading now.
    fun onScreenTapped() {
        Log.d(TAG, "Screen tapped in ARRenderer.")
        // You can add other tap-related logic here if needed.
    }

    private fun updateImageRecognitionState(isImageRecognized: Boolean) {
        if (isImageRecognized != isImageCurrentlyTracking) {
            onImageTracked(isImageRecognized)
            isImageCurrentlyTracking = isImageRecognized
        }
    }

    fun updateTitleText(text: String) {
        titleText = if (text.isBlank()) "Local Desconhecido" else text
        glSurfaceView.queueEvent {
            titleRenderer.setText(titleText, maxWidthPx = 512)
        }
    }

    fun updateDescriptionText(text: String) {
        descriptionText = if (text.isBlank()) "Nenhuma curiosidade encontrada." else text
        glSurfaceView.queueEvent {
            descRenderer.setText(descriptionText, maxWidthPx = 768)
        }
    }

    /**
     * Updates the augmented image displayed in AR based on a URL.
     * This method is called from ARScreen and queues the texture loading.
     *
     * @param imageUrl The URL of the image to load. If null, a placeholder will be loaded.
     */
    fun updateAugmentedImage(imageUrl: String?) {
        if (imageUrl == currentLoadedImageUrl) {
            Log.d(TAG, "Skipping image load: URL is null or already loaded ($imageUrl).")
            return
        }

        currentLoadedImageUrl = imageUrl // Update the tracker immediately

        glSurfaceView.queueEvent {
            if (!imageUrl.isNullOrBlank()) {
                placeImageTexture.updateTextureFromUrl(glSurfaceView, imageUrl) { success ->
                    if (!success) {
                        Log.e(TAG, "Failed to load image from URL: $imageUrl. Loading placeholder.")
                        placeImageTexture.updateTexture(context, R.drawable.brennand1)
                    }
                }
            } else {
                Log.d(TAG, "Image URL is null or blank. Loading placeholder image.")
                placeImageTexture.updateTexture(context, R.drawable.brennand1)
            }
        }
    }
}