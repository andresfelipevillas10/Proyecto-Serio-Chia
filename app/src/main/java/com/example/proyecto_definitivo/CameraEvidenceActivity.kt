package com.example.proyecto_definitivo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class CameraEvidenceActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_evidence)

        startCamera()

        findViewById<FloatingActionButton>(R.id.btnCapture).setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            // Configuración de compresión nativa de CameraX (Calidad mínima aceptable para evidencia)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(60) // Comprime agresivamente
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Falla al inicializar cámara", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Cámara no lista", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Capturando...", Toast.LENGTH_SHORT).show()

        // Archivo temporal en caché local
        val photoFile = File(cacheDir, "evidencia_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error al capturar: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Falla al capturar: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Foto guardada: ${photoFile.absolutePath}")
                    // Devolvemos la ruta del archivo a la actividad principal
                    val resultIntent = Intent().apply {
                        putExtra("PHOTO_PATH", photoFile.absolutePath)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish() // Cerramos el backstage
                }
            }
        )
    }
}