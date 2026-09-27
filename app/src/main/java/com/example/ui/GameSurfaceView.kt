package com.example.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.GameRenderer

@Composable
fun GameSurfaceView(
    renderer: GameRenderer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember(context, renderer) {
        CustomGLSurfaceView(context, renderer)
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        glView.onResume()
                    } catch (t: Throwable) {
                        Log.w("GameSurfaceView", "Error in onResume", t)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        glView.onPause()
                    } catch (t: Throwable) {
                        Log.w("GameSurfaceView", "Error in onPause", t)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                glView.onPause()
            } catch (t: Throwable) {
                Log.w("GameSurfaceView", "Error in onDispose onPause", t)
            }
        }
    }

    AndroidView(
        factory = { glView },
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
            // Use standard depth-enabled config chooser (compatible with all Android devices and emulators)
            setEGLConfigChooser(true)
        } catch (t: Throwable) {
            Log.w("CustomGLSurfaceView", "Error setting EGL configuration", t)
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
