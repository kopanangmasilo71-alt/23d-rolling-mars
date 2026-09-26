package com.example.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.GameRenderer

@Composable
fun GameSurfaceView(
    renderer: GameRenderer,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            CustomGLSurfaceView(ctx, renderer)
        },
        update = { view ->
            try {
                view.onResume()
            } catch (t: Throwable) {
                Log.w("GameSurfaceView", "Error in onResume", t)
            }
        },
        onRelease = { view ->
            try {
                view.onPause()
            } catch (t: Throwable) {
                Log.w("GameSurfaceView", "Error in onPause", t)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private class CustomGLSurfaceView(
    context: Context,
    renderer: GameRenderer
) : GLSurfaceView(context) {

    init {
        try {
            setEGLContextClientVersion(2)
            // Ensure 16-bit depth buffer is chosen for 3D depth testing
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        } catch (t: Throwable) {
            try {
                setEGLConfigChooser(true)
            } catch (_: Throwable) {}
        }

        try {
            preserveEGLContextOnPause = true
        } catch (_: Throwable) {
            // Ignore if not supported on this device
        }

        try {
            setRenderer(renderer)
            renderMode = RENDERMODE_CONTINUOUSLY
        } catch (t: Throwable) {
            Log.e("CustomGLSurfaceView", "Failed to set GLSurfaceView renderer", t)
        }
    }
}
