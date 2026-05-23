package com.alpware.notifybridge.ui

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.alpware.notifybridge.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Displays the QR pairing scanner used to connect with the Mac application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.qr_pairing_scanner_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Shows the live camera preview and scans QR pairing codes.
            QrCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onQrScanned = onQrScanned
            )

            // Visual focus area displayed above the camera preview.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(28.dp)
                    )
            )

            Text(
                text = stringResource(R.string.qr_pairing_scanner_prompt),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}

/**
 * Hosts the CameraX preview and processes frames with ML Kit QR scanning.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun QrCameraPreview(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    // Prevents duplicate scan callbacks while the current result is being processed.
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            // Native CameraX preview container rendered inside Compose.
            val previewView = PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)

            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    // Restrict ML Kit scanning to QR codes only.
                    val scanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                    )

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(viewContext)
                            ) { imageProxy ->
                                val mediaImage = imageProxy.image

                                if (mediaImage == null || hasScanned) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                // Convert the camera frame into an ML Kit compatible image.
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val value = barcodes
                                            .firstOrNull()
                                            ?.rawValue

                                        // Deliver the first valid QR payload and stop repeated scans.
                                        if (!value.isNullOrBlank() && !hasScanned) {
                                            hasScanned = true
                                            onQrScanned(value)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                    // Rebind the camera pipeline with preview and analysis use cases.
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                },
                ContextCompat.getMainExecutor(viewContext)
            )

            previewView
        }
    )
}