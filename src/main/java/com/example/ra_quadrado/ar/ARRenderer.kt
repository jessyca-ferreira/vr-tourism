package com.example.ra_quadrado.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.ra_quadrado.R
import com.example.ra_quadrado.ar.rendering.BackgroundRenderer
import com.example.ra_quadrado.ar.rendering.PlaneRenderer
import com.example.ra_quadrado.ar.rendering.PointCloudRenderer
import com.example.ra_quadrado.ar.rendering.QuadRenderer
import com.example.ra_quadrado.ar.rendering.TextRenderer
import com.example.ra_quadrado.ar.rendering.Texture
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val session: Session,
    private val context: Context,
    private val glSurfaceView: GLSurfaceView,
    private val onAnchorPlaced: () -> Unit, // UPDATED: Callback for when the anchor is successfully placed
    private val onRefreshNeeded: () -> Unit
) : GLSurfaceView.Renderer {

    // Renderers
    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    private val quadRenderer = QuadRenderer()
    private val titleRenderer = TextRenderer()
    private val descRenderer = TextRenderer()

    // Texture and state for the object to be placed
    private val placeImageTexture = Texture()
    private var isReadyToPlace = false
    private var placedAnchor: Anchor? = null

    // Viewport dimensions for hit testing
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0

    // Text and image URL state
    private var titleText: String = "Carregando Local..."
    private var descriptionText: String = "Carregando curiosidade..."
    private var currentLoadedImageUrl: String? = null

    // Visibility detection variables
    private var consecutiveFramesOutOfView = 0
    private val maxFramesOutOfView = 45 // Approx 1.5 seconds at 30fps

    // Constants for placement quality checks
    private val MIN_PLANE_WIDTH = 0.8f // Minimum 80cm width
    private val MIN_PLANE_HEIGHT = 0.8f // Minimum 80cm height

    companion object {
        private const val TAG = "ARRenderer"
    }

    fun resetPlacement() {
        placedAnchor?.detach()
        placedAnchor = null
        isReadyToPlace = false
        consecutiveFramesOutOfView = 0
        Log.d(TAG, "Placement has been reset. Ready for new object.")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        backgroundRenderer.createOnGlThread()
        planeRenderer.createOnGlThread()
        pointCloudRenderer.createOnGlThread()
        quadRenderer.createOnGlThread()
        titleRenderer.createOnGlThread()
        descRenderer.createOnGlThread()

        titleRenderer.setText(titleText)
        descRenderer.setText(descriptionText)

        placeImageTexture.createOnGlThread()
        // Load a placeholder initially
        placeImageTexture.updateTexture(context, R.drawable.brennand1)
        currentLoadedImageUrl = "placeholder_image"

        session.setCameraTextureName(backgroundRenderer.textureId)
        val arConfig = Config(session)
        arConfig.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL // Detect horizontal surfaces
        session.configure(arConfig)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        session.setDisplayGeometry(0, width, height)
        viewportWidth = width
        viewportHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
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
        val camera = frame.camera

        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        val projmtx = FloatArray(16)
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)
        val viewmtx = FloatArray(16)
        camera.getViewMatrix(viewmtx, 0)

        checkObjectVisibility(camera)

        frame.acquirePointCloud().use { pointCloud ->
            pointCloudRenderer.update(pointCloud)
            pointCloudRenderer.draw(viewmtx)
        }

        // --- AUTOMATIC PLACEMENT LOGIC ---
        if (placedAnchor == null && isReadyToPlace) {
            // Hit test against the middle of the screen
            val hitResults = frame.hitTest(viewportWidth / 2f, viewportHeight / 2f)

            // Find the first hit result that meets our stricter criteria
            val hitResult = findSuitableHit(hitResults)

            if (hitResult != null) {
                // A suitable, stable plane was found. Create the anchor.
                placedAnchor = hitResult.createAnchor()
                consecutiveFramesOutOfView = 0

                // --- THIS IS THE TRIGGER ---
                // Call the lambda to signal the UI thread to start the audio
                onAnchorPlaced()

                Log.d(TAG, "Object placed on a STABLE horizontal plane. onAnchorPlaced callback invoked.")
            }
        }

        placedAnchor?.let { anchor ->
            if (anchor.trackingState == TrackingState.TRACKING) {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

                val modelMatrix = FloatArray(16)
                anchor.pose.toMatrix(modelMatrix, 0)

                // Make the image face upwards from the horizontal plane
                Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f)

                // Scale the image
                val imageScale = 0.3f
                val aspectRatio = placeImageTexture.bitmapAspectRatio
                val scaleX = imageScale
                val scaleY = if (aspectRatio > 0) imageScale / aspectRatio else imageScale
                Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1.0f)

                // Draw the image quad
                placeImageTexture.textureId[0].let { textureId ->
                    if (textureId != 0) {
                        quadRenderer.draw(modelMatrix, viewmtx, projmtx, textureId)
                    }
                }

                // --- Draw Text ---
                // Position the title above the image
                val titleMatrix = modelMatrix.copyOf()
                Matrix.translateM(titleMatrix, 0, 0f, 1.2f, 0f) // Adjust Y for vertical offset
                titleRenderer.draw(titleMatrix, viewmtx, projmtx, imageScale * 1.5f)

                GLES20.glDisable(GLES20.GL_BLEND)

            } else if (anchor.trackingState == TrackingState.STOPPED) {
                Log.d(TAG, "Anchor tracking stopped. Resetting placement.")
                resetPlacement()
                onRefreshNeeded()
            }
        }
    }

    private fun findSuitableHit(hitResults: List<HitResult>): HitResult? {
        return hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            if (trackable is Plane) {
                // Check all our conditions here
                val isHorizontal = trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                val isTracking = trackable.trackingState == TrackingState.TRACKING
                val isBigEnough = trackable.extentX >= MIN_PLANE_WIDTH && trackable.extentZ >= MIN_PLANE_HEIGHT
                val isPoseInPolygon = trackable.isPoseInPolygon(hit.hitPose)

                // The hit is suitable only if ALL conditions are true
                isHorizontal && isTracking && isBigEnough && isPoseInPolygon
            } else {
                false
            }
        }
    }


    private fun checkObjectVisibility(camera: com.google.ar.core.Camera) {
        val anchor = placedAnchor ?: return

        if (anchor.trackingState == TrackingState.TRACKING) {
            val isVisible = isAnchorInView(anchor, camera)
            if (!isVisible) {
                consecutiveFramesOutOfView++
                if (consecutiveFramesOutOfView >= maxFramesOutOfView) {
                    Log.d(TAG, "Object out of view for $maxFramesOutOfView frames. Triggering refresh.")
                    resetPlacement()
                    onRefreshNeeded()
                }
            } else {
                consecutiveFramesOutOfView = 0
            }
        }
    }

    private fun isAnchorInView(anchor: Anchor, camera: com.google.ar.core.Camera): Boolean {
        val anchorPose = anchor.pose
        val cameraPose = camera.pose

        // Simple distance check first
        val dx = anchorPose.tx() - cameraPose.tx()
        val dy = anchorPose.ty() - cameraPose.ty()
        val dz = anchorPose.tz() - cameraPose.tz()
        val distanceSquared = dx * dx + dy * dy + dz * dz
        if (distanceSquared > 100) return false // More than 10 meters away

        // Full frustum check
        val anchorPosition = floatArrayOf(anchorPose.tx(), anchorPose.ty(), anchorPose.tz(), 1.0f)
        val viewMatrix = FloatArray(16)
        val projMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f)
        val viewProjectionMatrix = FloatArray(16)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projMatrix, 0, viewMatrix, 0)
        val screenPosition = FloatArray(4)
        Matrix.multiplyMV(screenPosition, 0, viewProjectionMatrix, 0, anchorPosition, 0)

        // Check if behind the camera
        if (screenPosition[3] <= 0) return false

        // Normalize to Normalized Device Coordinates (NDC)
        val ndcX = screenPosition[0] / screenPosition[3]
        val ndcY = screenPosition[1] / screenPosition[3]

        // Check if within the screen bounds (with a small margin)
        val margin = 0.5f
        return ndcX >= -1 - margin && ndcX <= 1 + margin && ndcY >= -1 - margin && ndcY <= 1 + margin
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

    fun updateAugmentedImage(imageUrl: String?) {
        if (imageUrl == currentLoadedImageUrl) {
            return
        }
        currentLoadedImageUrl = imageUrl

        glSurfaceView.queueEvent {
            if (!imageUrl.isNullOrBlank()) {
                placeImageTexture.updateTextureFromUrl(glSurfaceView, imageUrl) { success ->
                    if (!success) {
                        // Fallback to placeholder if URL loading fails
                        placeImageTexture.updateTexture(context, R.drawable.brennand1)
                    }
                }
            } else {
                // Fallback to placeholder if URL is null or blank
                placeImageTexture.updateTexture(context, R.drawable.brennand1)
            }
        }
    }

    fun signalDataIsReady() {
        isReadyToPlace = true
        Log.d(TAG, "Renderer has been signaled that data is ready for placement.")
    }

    fun forceRefresh() {
        Log.d(TAG, "Force refresh triggered")
        resetPlacement()
        onRefreshNeeded()
    }
}