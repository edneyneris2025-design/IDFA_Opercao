package com.example.ui.screens.qr

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.BallisticBlack
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalRed
import com.example.ui.viewmodel.TacticalViewModel
import com.example.util.QrCodeHelper
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun QrScannerSheet(
    viewModel: TacticalViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scanMessage by viewModel.qrScanMessage.collectAsState()
    val qrPayload by viewModel.qrSharePayload.collectAsState()

    var manualQrInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(580.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TacticalGreen)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = TacticalGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INTEGRAÇÃO TÁTICA VIA QR CODE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TacticalCardDark,
                contentColor = TacticalGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TacticalGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("LER / ESCANEAR QR", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("GERAR / COMPARTILHAR", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Tab 0: QR Scanner & Quick Presets
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Camera live viewfinder or simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(BallisticBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraQrPreview(
                            onQrDecoded = { text ->
                                viewModel.processQrCodeResult(text)
                            }
                        )

                        // Reticle Overlay
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(2.dp, TacticalGreen, RoundedCornerShape(8.dp))
                        )
                    }

                    // Result message banner
                    val msg = scanMessage
                    if (msg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TacticalGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = TacticalGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Text(
                        text = "CARREGAMENTO RÁPIDO DE MISSÃO (SIMULAÇÃO):",
                        fontSize = 10.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val preset = """{"type":"TACOPS_MISSION","title":"Operação Falcão Negro","originLat":-23.55052,"originLng":-46.633308}"""
                                viewModel.processQrCodeResult(preset)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2B23)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Missão Satélite", fontSize = 10.sp, color = TacticalGreen)
                        }

                        Button(
                            onClick = {
                                val preset = """{"type":"TACOPS_OPERATOR","callsign":"Cobra","role":"Suporte","squad":"Bravo","radio":"CH 02"}"""
                                viewModel.processQrCodeResult(preset)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2B23)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Operador Cobra", fontSize = 10.sp, color = TacticalCyan)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualQrInput,
                            onValueChange = { manualQrInput = it },
                            placeholder = { Text("Ou cole os dados do QR code aqui...", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TacticalGreen,
                                unfocusedBorderColor = TacticalBorder
                            ),
                            modifier = Modifier.weight(1f).height(50.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (manualQrInput.isNotBlank()) {
                                    viewModel.processQrCodeResult(manualQrInput)
                                    manualQrInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("LER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Tab 1: Generate QR Code for sharing mission / loadout
                val payloadToDisplay = qrPayload ?: viewModel.generateMissionQrPayload()
                var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(payloadToDisplay) {
                    qrBitmap = QrCodeHelper.generateQrBitmap(payloadToDisplay, 512, 512)
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "QR CODE DE SINCRONIZAÇÃO DE MISSÃO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apresente este código aos membros do esquadrão para sincronizar o mapa de satélite e coordenadas.",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val bmp = qrBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Code Tático",
                            modifier = Modifier
                                .size(210.dp)
                                .border(2.dp, TacticalGreen, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.qrSharePayload.value = viewModel.generateMissionQrPayload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2B23)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ATUALIZAR PACOTE DE MISSÃO", fontSize = 11.sp, color = TacticalGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraQrPreview(
    onQrDecoded: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val qrText = decodeQrFromImageProxy(imageProxy)
                    if (qrText != null) {
                        ContextCompat.getMainExecutor(ctx).execute {
                            onQrDecoded(qrText)
                        }
                    }
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun decodeQrFromImageProxy(image: ImageProxy): String? {
    return try {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val width = image.width
        val height = image.height

        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decodeWithState(bitmap)
        result.text
    } catch (_: Exception) {
        null
    }
}
