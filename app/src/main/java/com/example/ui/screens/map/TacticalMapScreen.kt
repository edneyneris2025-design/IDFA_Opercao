package com.example.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.widget.Toast
import kotlinx.coroutines.delay
import com.example.data.local.model.OperatorEntity
import com.example.data.local.model.TacticalZoneEntity
import com.example.domain.geo.GeoPoint
import com.example.domain.geo.GeoUtils
import com.example.domain.map.SatelliteSource
import com.example.domain.map.SatelliteTileProvider
import com.example.domain.map.TileCoord
import com.example.ui.theme.BallisticBlack
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalRed
import com.example.ui.viewmodel.MapLayerMode
import com.example.ui.viewmodel.TacticalViewModel
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TacticalMapScreen(
    viewModel: TacticalViewModel,
    onNavigateToOperatorInventory: (Long) -> Unit,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val operators by viewModel.operators.collectAsState()
    val zones by viewModel.zones.collectAsState()
    val isDrawing by viewModel.isDrawingPolygon.collectAsState()
    val draftPoints by viewModel.currentDraftPoints.collectAsState()
    val isSimulating by viewModel.isPatrolSimulationRunning.collectAsState()
    val mapLayer by viewModel.mapLayerMode.collectAsState()

    val centerLat by viewModel.mapCenterLat.collectAsState()
    val centerLng by viewModel.mapCenterLng.collectAsState()
    val zoom by viewModel.mapZoom.collectAsState()

    val tileVersion by viewModel.tileProvider.tileVersion.collectAsState()
    val satelliteSource by viewModel.satelliteSource.collectAsState()
    val isSatelliteEnabled by viewModel.isSatelliteEnabled.collectAsState()
    val satelliteOpacity by viewModel.satelliteOpacity.collectAsState()

    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    var selectedOperatorForSheet by remember { mutableStateOf<OperatorEntity?>(null) }
    var showSaveZoneDialog by remember { mutableStateOf(false) }
    var showSatelliteConfigDialog by remember { mutableStateOf(false) }
    var showGpsTelemetryDialog by remember { mutableStateOf(false) }
    var showMarkWaypointDialog by remember { mutableStateOf(false) }
    var waypointNameInput by remember { mutableStateOf("Rally Point / Base") }
    var waypointTypeInput by remember { mutableStateOf("STRATEGIC_OBJECTIVE") }
    var newZoneName by remember { mutableStateOf("Zona de Exclusão") }
    var newZoneType by remember { mutableStateOf("EXCLUSION") }

    val context = LocalContext.current
    val userGps by viewModel.userGpsLocation.collectAsState()
    val gpsAccuracy by viewModel.gpsAccuracyMeters.collectAsState()
    val gpsAltitude by viewModel.gpsAltitudeMeters.collectAsState()
    val gpsHeading by viewModel.gpsHeadingDegrees.collectAsState()
    val isGpsActive by viewModel.isGpsActive.collectAsState()
    val gpsFeedbackMsg by viewModel.gpsFeedbackMessage.collectAsState()

    LaunchedEffect(gpsFeedbackMsg) {
        if (gpsFeedbackMsg != null) {
            delay(3500)
            viewModel.gpsFeedbackMessage.value = null
        }
    }

    // Pulsing animation for breached zones and alerts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Check if any zone is currently breached
    val anyBreachedZone = zones.firstOrNull { it.isBreached }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(BallisticBlack)) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Main Map Canvas with touch/pan/pinch and vertex click
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDrawing, zoom, centerLat, centerLng, panOffsetX, panOffsetY) {
                    if (isDrawing) {
                        detectTapGestures { offset ->
                            val tileZoomCurrent = zoom.roundToInt().coerceIn(1, 19)
                            val tileSizeCurrent = 256.0 * 2.0.pow((zoom - tileZoomCurrent).toDouble())
                            val cTileX = SatelliteTileProvider.lonToTileX(centerLng, tileZoomCurrent)
                            val cTileY = SatelliteTileProvider.latToTileY(centerLat, tileZoomCurrent)

                            val tapTileX = cTileX + (offset.x - (size.width / 2f) - panOffsetX) / tileSizeCurrent
                            val tapTileY = cTileY + (offset.y - (size.height / 2f) - panOffsetY) / tileSizeCurrent

                            val tappedLng = SatelliteTileProvider.tileXToLon(tapTileX, tileZoomCurrent)
                            val tappedLat = SatelliteTileProvider.tileYToLat(tapTileY, tileZoomCurrent)

                            val tappedGeoPoint = GeoPoint(tappedLat, tappedLng)
                            viewModel.addPointToDraft(tappedGeoPoint)
                        }
                    } else {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                            val newZoom = (viewModel.mapZoom.value * gestureZoom).coerceIn(12f, 20f)
                            viewModel.mapZoom.value = newZoom
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().testTag("tactical_satellite_canvas")) {
                // Dependency on tileVersion forces redraw when background satellite tiles download
                @Suppress("UNUSED_VARIABLE")
                val triggerRedraw = tileVersion

                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerCanvasX = canvasWidth / 2f + panOffsetX
                val centerCanvasY = canvasHeight / 2f + panOffsetY

                val tileZoom = zoom.roundToInt().coerceIn(1, 19)
                val tileSize = 256.0 * 2.0.pow((zoom - tileZoom).toDouble())
                val centerTileX = SatelliteTileProvider.lonToTileX(centerLng, tileZoom)
                val centerTileY = SatelliteTileProvider.latToTileY(centerLat, tileZoom)

                fun geoToScreen(point: GeoPoint): Offset {
                    val px = centerCanvasX + (SatelliteTileProvider.lonToTileX(point.lng, tileZoom) - centerTileX) * tileSize
                    val py = centerCanvasY + (SatelliteTileProvider.latToTileY(point.lat, tileZoom) - centerTileY) * tileSize
                    return Offset(px.toFloat(), py.toFloat())
                }

                // 1. Draw Real Satellite Map Background
                if (isSatelliteEnabled) {
                    drawSatelliteTiles(
                        scope = this,
                        tileProvider = viewModel.tileProvider,
                        source = satelliteSource,
                        tileZoom = tileZoom,
                        tileSize = tileSize,
                        centerTileX = centerTileX,
                        centerTileY = centerTileY,
                        centerCanvasX = centerCanvasX,
                        centerCanvasY = centerCanvasY,
                        opacity = satelliteOpacity,
                        mode = mapLayer
                    )
                } else {
                    drawSatelliteTerrain(
                        scope = this,
                        mode = mapLayer,
                        centerCanvasX = centerCanvasX,
                        centerCanvasY = centerCanvasY
                    )
                }

                // 2. Draw Military MGRS Grid lines & Crosshairs
                drawTacticalGridOverlay(
                    scope = this,
                    centerCanvasX = centerCanvasX,
                    centerCanvasY = centerCanvasY,
                    mode = mapLayer
                )

                // 3. Draw Saved Tactical Zones (Polygons) with dynamic color transitions!
                for (zone in zones) {
                    val pts = GeoUtils.parsePointsJson(zone.pointsJson)
                    if (pts.size >= 3) {
                        val screenPoints = pts.map { geoToScreen(it) }

                        // Dynamic boundary color switching based on occupancy / breach status:
                        val (strokeColor, fillColor) = when {
                            zone.isBreached && zone.zoneType == "EXCLUSION" -> {
                                // Flashing dynamic alert color when an operator breaches exclusion zone!
                                val dynamicAlertColor = Color(0xFFFF9100).copy(alpha = pulseAlpha)
                                val dynamicFill = Color(0xFFFF1744).copy(alpha = 0.35f * pulseAlpha)
                                Pair(dynamicAlertColor, dynamicFill)
                            }
                            zone.isBreached && zone.zoneType == "STRATEGIC_OBJECTIVE" -> {
                                // Objective secured / contested: changes line to vivid green
                                Pair(Color(0xFF00E676), Color(0x3300E676))
                            }
                            zone.zoneType == "EXCLUSION" -> {
                                // Unbreached exclusion zone: Solid tactical red line
                                Pair(Color(0xFFFF1744), Color(0x22FF1744))
                            }
                            zone.zoneType == "STRATEGIC_OBJECTIVE" -> {
                                // Unoccupied strategic objective: Cyan line
                                Pair(Color(0xFF00E5FF), Color(0x2200E5FF))
                            }
                            zone.zoneType == "SAFE_ZONE" -> {
                                Pair(Color(0xFFFFD600), Color(0x22FFD600))
                            }
                            else -> {
                                Pair(Color(0xFF2979FF), Color(0x222979FF))
                            }
                        }

                        // Draw polygon filled background
                        val polyPath = Path().apply {
                            moveTo(screenPoints[0].x, screenPoints[0].y)
                            for (i in 1 until screenPoints.size) {
                                lineTo(screenPoints[i].x, screenPoints[i].y)
                            }
                            close()
                        }

                        drawPath(
                            path = polyPath,
                            color = fillColor,
                            style = Fill
                        )

                        // Draw polygon border stroke (thicker if breached/alert)
                        val strokeWidth = if (zone.isBreached) 5.dp.toPx() else 3.dp.toPx()
                        drawPath(
                            path = polyPath,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = if (zone.isBreached) null else PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                            )
                        )

                        // Draw vertex dots
                        for (pt in screenPoints) {
                            drawCircle(
                                color = strokeColor,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                        }

                        // Draw zone label and status at polygon centroid
                        var centroidX = 0f
                        var centroidY = 0f
                        for (p in screenPoints) {
                            centroidX += p.x
                            centroidY += p.y
                        }
                        centroidX /= screenPoints.size
                        centroidY /= screenPoints.size

                        val statusText = if (zone.isBreached) {
                            "⚠️ VIOLADA: ${zone.breachedByCallsigns}"
                        } else {
                            zone.name.uppercase()
                        }

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (zone.isBreached) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                                textSize = 32f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                            }
                            drawText(statusText, centroidX, centroidY, paint)
                        }
                    }
                }

                // 4. Draw Current Draft Polygon (while user is drawing with side button)
                if (isDrawing && draftPoints.isNotEmpty()) {
                    val draftScreenPoints = draftPoints.map { geoToScreen(it) }

                    if (draftScreenPoints.size >= 2) {
                        val draftPath = Path().apply {
                            moveTo(draftScreenPoints[0].x, draftScreenPoints[0].y)
                            for (i in 1 until draftScreenPoints.size) {
                                lineTo(draftScreenPoints[i].x, draftScreenPoints[i].y)
                            }
                        }
                        drawPath(
                            path = draftPath,
                            color = TacticalGreen,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                    }

                    // Vertex markers with numbers
                    for ((idx, pt) in draftScreenPoints.withIndex()) {
                        drawCircle(
                            color = TacticalGreen,
                            radius = 7.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "P${idx + 1}",
                            pt.x + 15f,
                            pt.y - 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GREEN
                                textSize = 28f
                                isFakeBoldText = true
                            }
                        )
                    }
                }

                // 5. Draw Tactical Operators (Blips, Direction Chevrons, Health Rings)
                var hasDrawnUserGps = false
                for (op in operators) {
                    val opScreenPos = geoToScreen(GeoPoint(op.latitude, op.longitude))

                    if (op.isUserDevice) {
                        hasDrawnUserGps = true
                        // --- DEDICATED HIGH-VISIBILITY GPS BEACON FOR USER DEVICE ---
                        // 1. Accuracy Circle (scaled to current zoom / tileSize)
                        val accMeters = gpsAccuracy ?: 8f
                        val accRadiusPx = (accMeters * (tileSize / 75.0)).toFloat().coerceIn(24.dp.toPx(), 90.dp.toPx())
                        drawCircle(
                            color = TacticalCyan.copy(alpha = 0.12f * pulseAlpha),
                            radius = accRadiusPx,
                            center = opScreenPos
                        )
                        drawCircle(
                            color = TacticalCyan.copy(alpha = 0.45f),
                            radius = accRadiusPx,
                            center = opScreenPos,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                        )

                        // 2. Dual Concentric Sonar Pulses
                        drawCircle(
                            color = TacticalCyan.copy(alpha = 0.25f),
                            radius = 28.dp.toPx(),
                            center = opScreenPos
                        )
                        drawCircle(
                            color = TacticalGreen.copy(alpha = 0.35f),
                            radius = 20.dp.toPx(),
                            center = opScreenPos,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 3. Precision Crosshair Reticle
                        val chLen = 16.dp.toPx()
                        drawLine(
                            color = TacticalCyan,
                            start = Offset(opScreenPos.x - chLen, opScreenPos.y),
                            end = Offset(opScreenPos.x + chLen, opScreenPos.y),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = TacticalCyan,
                            start = Offset(opScreenPos.x, opScreenPos.y - chLen),
                            end = Offset(opScreenPos.x, opScreenPos.y + chLen),
                            strokeWidth = 2.dp.toPx()
                        )

                        // 4. Solid Center Core & Diamond
                        drawCircle(
                            color = TacticalCyan,
                            radius = 9.dp.toPx(),
                            center = opScreenPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = opScreenPos
                        )

                        // 5. Heading pointer (device bearing or compass)
                        val headingVal = gpsHeading ?: op.heading
                        val headingRad = Math.toRadians((headingVal - 90).toDouble())
                        val arrowLen = 28.dp.toPx()
                        val arrowEnd = Offset(
                            opScreenPos.x + (cos(headingRad) * arrowLen).toFloat(),
                            opScreenPos.y + (sin(headingRad) * arrowLen).toFloat()
                        )
                        drawLine(
                            color = TacticalCyan,
                            start = opScreenPos,
                            end = arrowEnd,
                            strokeWidth = 3.5.dp.toPx()
                        )

                        // 6. Tactical Label
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#00E5FF")
                                textSize = 30f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                            }
                            drawText("★ VOCÊ (MEU GPS)", opScreenPos.x, opScreenPos.y - 28.dp.toPx(), paint)
                        }
                    } else {
                        // Standard Squad Operator
                        val (healthColor, ringColor) = when (op.healthState) {
                            "ACTIVE" -> Pair(TacticalGreen, TacticalGreen)
                            "COMBAT" -> Pair(TacticalAmber, TacticalAmber)
                            "WOUNDED" -> Pair(Color(0xFFFF9100), Color(0xFFFF9100))
                            "KIA" -> Pair(TacticalRed, TacticalRed)
                            else -> Pair(TacticalBlue, TacticalBlue)
                        }

                        // Pulsing radar ring around operator
                        drawCircle(
                            color = ringColor.copy(alpha = 0.25f),
                            radius = 22.dp.toPx(),
                            center = opScreenPos
                        )

                        // Outer health indicator border
                        drawCircle(
                            color = healthColor,
                            radius = 12.dp.toPx(),
                            center = opScreenPos,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Inner core
                        drawCircle(
                            color = healthColor,
                            radius = 8.dp.toPx(),
                            center = opScreenPos
                        )

                        // Heading arrow / direction pointer
                        val headingRad = Math.toRadians((op.heading - 90).toDouble())
                        val arrowLen = 22.dp.toPx()
                        val arrowEnd = Offset(
                            opScreenPos.x + (cos(headingRad) * arrowLen).toFloat(),
                            opScreenPos.y + (sin(headingRad) * arrowLen).toFloat()
                        )
                        drawLine(
                            color = healthColor,
                            start = opScreenPos,
                            end = arrowEnd,
                            strokeWidth = 3.dp.toPx()
                        )

                        // Callsign and Role text
                        val displayText = "${op.callsign} [${op.hpPercent}%]"
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                            }
                            drawText(displayText, opScreenPos.x, opScreenPos.y - 22.dp.toPx(), paint)
                        }
                    }
                }

                // If user GPS exists but is not mapped to an operator yet
                if (!hasDrawnUserGps && userGps != null) {
                    val gpsScreenPos = geoToScreen(userGps!!)
                    drawCircle(
                        color = TacticalCyan.copy(alpha = 0.25f),
                        radius = 26.dp.toPx(),
                        center = gpsScreenPos
                    )
                    drawCircle(
                        color = TacticalCyan,
                        radius = 10.dp.toPx(),
                        center = gpsScreenPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = gpsScreenPos
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#00E5FF")
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        drawText("★ MEU GPS", gpsScreenPos.x, gpsScreenPos.y - 28.dp.toPx(), paint)
                    }
                }
            }
        }

        // --- TOP HUD: Tactical Coordinates & Status Bar ---
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalCardDark.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Bússola",
                    tint = TacticalGreen,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSatelliteEnabled) "🛰️ SATÉLITE REAL (ESRI)" else "RADAR TÁTICO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSatelliteEnabled) TacticalGreen else TacticalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = TacticalGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = TacticalGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = GeoUtils.formatTacticalGrid(centerLat, centerLng),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Satellite & Layers configuration dialog button
                IconButton(
                    onClick = { showSatelliteConfigDialog = true },
                    modifier = Modifier.size(36.dp).testTag("satellite_layer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = "Configurar Satélite & Camadas",
                        tint = if (isSatelliteEnabled) TacticalGreen else TacticalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- BREACH ALERT BANNER (High Visibility Alert) ---
        AnimatedVisibility(
            visible = anyBreachedZone != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
        ) {
            anyBreachedZone?.let { zone ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (zone.zoneType == "EXCLUSION") TacticalRed.copy(alpha = 0.95f) else TacticalGreen.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (zone.zoneType == "EXCLUSION") "ALERTA DE VIOLAÇÃO DE PERÍMETRO!" else "OBJETIVO EM DISPUTA / DOMINADO",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${zone.name}: Operador(es) [${zone.breachedByCallsigns}] detectado(s) dentro da área demarcada!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // --- GPS CONFIRMATION / TELEMETRY BANNER ---
        AnimatedVisibility(
            visible = gpsFeedbackMsg != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 20.dp, end = 20.dp)
        ) {
            gpsFeedbackMsg?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalCardDark.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = msg,
                            color = TacticalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- BARRA LATERAL ESQUERDA DA TELA (REQUESTED FEATURE: MAP BUTTONS ON LEFT SIDEBAR) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 68.dp, bottom = 68.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // SIDEBAR DOCK HEADER / BADGE
            Surface(
                color = TacticalCardDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TacticalGreen)
                    )
                    Text(
                        text = "PAINEL TÁTICO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 1. BOTÃO LATERAL ESQUERDO: MEU GPS
            Button(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    viewModel.centerOnMyGps(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGpsActive) TacticalCyan.copy(alpha = 0.28f) else TacticalCardDark,
                    contentColor = TacticalCyan
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isGpsActive) TacticalCyan else TacticalGreen
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("side_button_my_gps")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Acessar Meu GPS",
                        tint = if (isGpsActive) TacticalCyan else TacticalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "MEU GPS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isGpsActive) TacticalCyan else TacticalGreen
                        )
                        Text(
                            text = if (isGpsActive) "SINAL FIXO" else "LOCALIZAR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGpsActive) TacticalGreen else Color.LightGray
                        )
                    }
                }
            }

            // 2. SUB-AÇÕES GPS: DADOS TELEMETRIA & MARCAR PONTO NO MAPA
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { showGpsTelemetryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCardDark),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("side_button_gps_telemetry")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Telemetria GPS",
                        tint = TacticalCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "DADOS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showMarkWaypointDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCardDark),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("side_button_mark_waypoint")
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Marcar Ponto",
                        tint = TacticalAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "MARCAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalAmber
                    )
                }
            }

            // 3. BOTÃO LATERAL ESQUERDO: DESENHAR POLÍGONO / ZONA DE COMBATE
            Button(
                onClick = { viewModel.togglePolygonDrawingMode() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDrawing) TacticalAmber else TacticalCardDark,
                    contentColor = if (isDrawing) Color.Black else TacticalGreen
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isDrawing) TacticalAmber else TacticalGreen
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("side_button_draw_polygon")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isDrawing) Icons.Default.Close else Icons.Default.Polyline,
                        contentDescription = "Desenhar Polígono",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isDrawing) "CANCELAR" else "DESENHAR ZONA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Expanded controls when side drawing mode is ACTIVE
            AnimatedVisibility(visible = isDrawing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalCardDark.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MARQUE OS PONTOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalGreen
                        )
                        Text(
                            text = "Toque no mapa para posicionar os vértices da zona (${draftPoints.size} pts).",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            lineHeight = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.undoLastDraftPoint() },
                                enabled = draftPoints.isNotEmpty(),
                                modifier = Modifier.weight(1f).padding(end = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Desfazer", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Voltar", fontSize = 9.sp)
                            }

                            Button(
                                onClick = {
                                    if (draftPoints.size >= 3) {
                                        showSaveZoneDialog = true
                                    }
                                },
                                enabled = draftPoints.size >= 3,
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).padding(start = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Concluir", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Concluir", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. BOTÃO LATERAL ESQUERDO: SATÉLITE & CAMADAS
            Button(
                onClick = { showSatelliteConfigDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("side_button_satellite_layers")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SatelliteAlt,
                        contentDescription = "Configurar Satélite",
                        tint = TacticalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("SATÉLITE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 5. BOTÃO LATERAL ESQUERDO: LEITOR QR CODE
            Button(
                onClick = onOpenQrScanner,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCardDark),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.7f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("side_button_qr_code")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Ler QR Tático",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("QR CODE", color = TacticalCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 6. BOTÃO LATERAL ESQUERDO: SIMULAÇÃO DE PATRULHA
            Button(
                onClick = { viewModel.toggleFieldPatrolSimulation() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulating) TacticalRed.copy(alpha = 0.25f) else TacticalCardDark
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSimulating) TacticalRed else TacticalGreen
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.testTag("side_button_patrol_sim")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Simulação de Campo",
                        tint = if (isSimulating) TacticalRed else TacticalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isSimulating) "PARAR TESTE" else "SIMULAÇÃO",
                        color = if (isSimulating) TacticalRed else TacticalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7. CONTROLES DE ZOOM E CENTRO (NA BASE DA BARRA LATERAL ESQUERDA)
            Card(
                colors = CardDefaults.cardColors(containerColor = TacticalCardDark.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.mapZoom.value = (viewModel.mapZoom.value + 0.8f).coerceAtMost(21f) },
                        modifier = Modifier.size(34.dp).testTag("side_button_zoom_in")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TacticalGreen)
                    }
                    IconButton(
                        onClick = { viewModel.mapZoom.value = (viewModel.mapZoom.value - 0.8f).coerceAtLeast(12f) },
                        modifier = Modifier.size(34.dp).testTag("side_button_zoom_out")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TacticalGreen)
                    }
                    IconButton(
                        onClick = {
                            panOffsetX = 0f
                            panOffsetY = 0f
                        },
                        modifier = Modifier.size(34.dp).testTag("side_button_center_view")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centralizar", tint = TacticalCyan)
                    }
                }
            }
        }

        // --- BOTTOM FLOATING BAR: Tactical Status Strip & Quick Operations ---
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalCardDark.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TacticalGreen))
                    Text(
                        text = "${operators.size} OPERADORES",
                        fontSize = 10.sp,
                        color = TacticalGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = TacticalBorder,
                    modifier = Modifier.width(1.dp).height(14.dp)
                ) {}

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${zones.size} ÁREAS ATIVAS",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isSimulating) {
                    Surface(
                        color = TacticalRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalRed.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "SIMULAÇÃO ATIVA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialog to Save Demarcated Polygon
    if (showSaveZoneDialog) {
        AlertDialog(
            onDismissRequest = { showSaveZoneDialog = false },
            title = {
                Text(
                    text = "DELIMITAR NOVA ÁREA TÁTICA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Vértices marcados: ${draftPoints.size}. Defina o nome e a finalidade estratégica:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = newZoneName,
                        onValueChange = { newZoneName = it },
                        label = { Text("Nome da Área / Setor") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalGreen,
                            unfocusedBorderColor = TacticalBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tipo de Demarcação:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalCyan)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = newZoneType == "EXCLUSION",
                            onClick = { newZoneType = "EXCLUSION" },
                            label = { Text("Exclusão / Risco", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalRed,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = newZoneType == "STRATEGIC_OBJECTIVE",
                            onClick = { newZoneType = "STRATEGIC_OBJECTIVE" },
                            label = { Text("Objetivo HQ", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = newZoneType == "SAFE_ZONE",
                            onClick = { newZoneType = "SAFE_ZONE" },
                            label = { Text("Safe Zone", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalAmber,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishAndSavePolygon(newZoneName, newZoneType)
                        showSaveZoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
                ) {
                    Text("SALVAR NO MAPA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveZoneDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = TacticalCardDark
        )
    }

    if (showSatelliteConfigDialog) {
        SatelliteConfigDialog(
            viewModel = viewModel,
            onDismiss = { showSatelliteConfigDialog = false }
        )
    }

    if (showGpsTelemetryDialog) {
        GpsTelemetryDialog(
            viewModel = viewModel,
            onDismiss = { showGpsTelemetryDialog = false },
            onCenterGps = {
                panOffsetX = 0f
                panOffsetY = 0f
                viewModel.centerOnMyGps(context)
                showGpsTelemetryDialog = false
            },
            onOpenMarkWaypoint = {
                showGpsTelemetryDialog = false
                showMarkWaypointDialog = true
            }
        )
    }

    if (showMarkWaypointDialog) {
        MarkGpsWaypointDialog(
            viewModel = viewModel,
            onDismiss = { showMarkWaypointDialog = false }
        )
    }
}

private val TacticalSurfaceElevated = Color(0xFF192620)

/**
 * Draws real-world satellite photographic imagery tiles (Esri World Imagery, Topo, OSM)
 */
private fun drawSatelliteTiles(
    scope: DrawScope,
    tileProvider: SatelliteTileProvider,
    source: SatelliteSource,
    tileZoom: Int,
    tileSize: Double,
    centerTileX: Double,
    centerTileY: Double,
    centerCanvasX: Float,
    centerCanvasY: Float,
    opacity: Float,
    mode: MapLayerMode
) {
    val w = scope.size.width
    val h = scope.size.height

    // 1. Base dark background for unrendered tiles
    scope.drawRect(color = Color(0xFF090E0B))

    // Calculate tile coordinate range covering current canvas viewport
    val minTileX = floor(centerTileX - (centerCanvasX) / tileSize).toInt()
    val maxTileX = ceil(centerTileX + (w - centerCanvasX) / tileSize).toInt()
    val minTileY = floor(centerTileY - (centerCanvasY) / tileSize).toInt()
    val maxTileY = ceil(centerTileY + (h - centerCanvasY) / tileSize).toInt()

    val maxIndex = (1 shl tileZoom)
    val nativeCanvas = scope.drawContext.canvas.nativeCanvas

    val tilePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        alpha = (opacity.coerceIn(0.1f, 1f) * 255).roundToInt()
        if (mode == MapLayerMode.FLIR_THERMAL) {
            // Apply thermal tint to satellite photography
            colorFilter = android.graphics.PorterDuffColorFilter(
                android.graphics.Color.parseColor("#FF5252"),
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
        }
    }

    val srcRect = Rect(0, 0, 256, 256)

    for (tx in minTileX..maxTileX) {
        val normX = (tx % maxIndex + maxIndex) % maxIndex
        val left = (centerCanvasX + (tx - centerTileX) * tileSize).toFloat()
        val right = (left + tileSize).toFloat()

        for (ty in minTileY..maxTileY) {
            if (ty !in 0 until maxIndex) continue
            val top = (centerCanvasY + (ty - centerTileY) * tileSize).toFloat()
            val bottom = (top + tileSize).toFloat()

            val tileCoord = TileCoord(tileZoom, normX, ty)
            val bitmap = tileProvider.getTile(tileCoord, source)

            if (bitmap != null) {
                val dstRect = RectF(left, top, right, bottom)
                nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, tilePaint)
            } else {
                // Fallback texture while tile is downloading in background
                scope.drawRect(
                    color = Color(0xFF101C15),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size((right - left), (bottom - top))
                )
                scope.drawRect(
                    color = Color(0x1800E676),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size((right - left), (bottom - top)),
                    style = Stroke(1f)
                )
            }
        }
    }
}

/**
 * Draws fallback radar background when real satellite is turned off
 */
private fun drawSatelliteTerrain(
    scope: DrawScope,
    mode: MapLayerMode,
    centerCanvasX: Float,
    centerCanvasY: Float
) {
    val w = scope.size.width
    val h = scope.size.height

    val baseBrush = when (mode) {
        MapLayerMode.SATELLITE -> Brush.radialGradient(
            colors = listOf(Color(0xFF142018), Color(0xFF0F1812), Color(0xFF080D0A)),
            center = Offset(centerCanvasX, centerCanvasY),
            radius = (w + h) / 1.5f
        )
        MapLayerMode.FLIR_THERMAL -> Brush.radialGradient(
            colors = listOf(Color(0xFF280B1E), Color(0xFF140510), Color(0xFF070008)),
            center = Offset(centerCanvasX, centerCanvasY),
            radius = (w + h) / 1.5f
        )
        MapLayerMode.TACTICAL_GRID -> Brush.radialGradient(
            colors = listOf(Color(0xFF0C1613), Color(0xFF07100D), Color(0xFF030706)),
            center = Offset(centerCanvasX, centerCanvasY),
            radius = (w + h) / 1.5f
        )
        MapLayerMode.TOPOGRAPHIC -> Brush.radialGradient(
            colors = listOf(Color(0xFF1A1F1B), Color(0xFF121613), Color(0xFF090C0A)),
            center = Offset(centerCanvasX, centerCanvasY),
            radius = (w + h) / 1.5f
        )
    }

    scope.drawRect(brush = baseBrush)

    // Draw simulated satellite contour lines / elevation rings
    val contourColor = when (mode) {
        MapLayerMode.FLIR_THERMAL -> Color(0x22FF5252)
        MapLayerMode.TACTICAL_GRID -> Color(0x1800E5FF)
        else -> Color(0x1A00E676)
    }

    for (r in 1..8) {
        val radius = r * 90f + ((centerCanvasX + centerCanvasY) % 50f)
        scope.drawCircle(
            color = contourColor,
            radius = radius,
            center = Offset(centerCanvasX + (r * 15f), centerCanvasY - (r * 10f)),
            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
        )
    }
}

/**
 * Draws military MGRS tactical grid overlay and crosshairs
 */
private fun drawTacticalGridOverlay(
    scope: DrawScope,
    centerCanvasX: Float,
    centerCanvasY: Float,
    mode: MapLayerMode
) {
    val w = scope.size.width
    val h = scope.size.height

    val gridColor = when (mode) {
        MapLayerMode.FLIR_THERMAL -> Color(0x25FF80AB)
        MapLayerMode.TACTICAL_GRID -> Color(0x3B00E5FF)
        else -> Color(0x2800E676)
    }

    val step = 120f
    val offsetX = centerCanvasX % step
    val offsetY = centerCanvasY % step

    // Vertical lines
    var x = offsetX
    while (x < w) {
        scope.drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
        x += step
    }

    // Horizontal lines
    var y = offsetY
    while (y < h) {
        scope.drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += step
    }

    // Center Crosshair Reticle
    val crosshairLen = 24f
    val crosshairColor = if (mode == MapLayerMode.FLIR_THERMAL) Color(0xFFFF5252) else Color(0xFF00E676)
    scope.drawLine(
        color = crosshairColor,
        start = Offset(centerCanvasX - crosshairLen, centerCanvasY),
        end = Offset(centerCanvasX + crosshairLen, centerCanvasY),
        strokeWidth = 2f
    )
    scope.drawLine(
        color = crosshairColor,
        start = Offset(centerCanvasX, centerCanvasY - crosshairLen),
        end = Offset(centerCanvasX, centerCanvasY + crosshairLen),
        strokeWidth = 2f
    )
    scope.drawCircle(
        color = crosshairColor,
        radius = 16f,
        center = Offset(centerCanvasX, centerCanvasY),
        style = Stroke(width = 1.5f)
    )
}

@Composable
fun SatelliteConfigDialog(
    viewModel: TacticalViewModel,
    onDismiss: () -> Unit
) {
    val isEnabled by viewModel.isSatelliteEnabled.collectAsState()
    val source by viewModel.satelliteSource.collectAsState()
    val opacity by viewModel.satelliteOpacity.collectAsState()
    val mapLayer by viewModel.mapLayerMode.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SatelliteAlt,
                    contentDescription = null,
                    tint = TacticalGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONFIGURAÇÃO DO SATÉLITE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalGreen
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Switch: Enable Real Satellite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Imagem Real de Satélite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Exibir fotos aéreas de alta resolução sob operadores e perímetros",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.isSatelliteEnabled.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TacticalGreen,
                            checkedTrackColor = TacticalGreen.copy(alpha = 0.3f)
                        )
                    )
                }

                if (isEnabled) {
                    // Source Selector
                    Text(
                        text = "FONTE DO SATÉLITE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = source == SatelliteSource.ESRI_WORLD_IMAGERY,
                            onClick = { viewModel.satelliteSource.value = SatelliteSource.ESRI_WORLD_IMAGERY },
                            label = { Text("🛰️ Esri World Imagery (Satélite Real)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = source == SatelliteSource.ESRI_TOPO,
                            onClick = { viewModel.satelliteSource.value = SatelliteSource.ESRI_TOPO },
                            label = { Text("🗺️ Topográfico / Curvas de Nível", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = source == SatelliteSource.OPEN_STREET_MAP,
                            onClick = { viewModel.satelliteSource.value = SatelliteSource.OPEN_STREET_MAP },
                            label = { Text("🌐 Vias & Terreno Tático (OSM)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Opacity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Brilho / Opacidade do Satélite:",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${(opacity * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalGreen
                            )
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { viewModel.satelliteOpacity.value = it },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = TacticalGreen,
                                activeTrackColor = TacticalGreen
                            )
                        )
                    }

                    // Map Vision Mode
                    Text(
                        text = "MODO DE VISÃO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = mapLayer == MapLayerMode.SATELLITE,
                            onClick = { viewModel.mapLayerMode.value = MapLayerMode.SATELLITE },
                            label = { Text("Natural", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = mapLayer == MapLayerMode.FLIR_THERMAL,
                            onClick = { viewModel.mapLayerMode.value = MapLayerMode.FLIR_THERMAL },
                            label = { Text("FLIR / Térmico", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalRed,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = mapLayer == MapLayerMode.TACTICAL_GRID,
                            onClick = { viewModel.mapLayerMode.value = MapLayerMode.TACTICAL_GRID },
                            label = { Text("Grade HUD", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black)
            ) {
                Text("APLICAR", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = TacticalCardDark
    )
}

@Composable
fun GpsTelemetryDialog(
    viewModel: TacticalViewModel,
    onDismiss: () -> Unit,
    onCenterGps: () -> Unit,
    onOpenMarkWaypoint: () -> Unit
) {
    val context = LocalContext.current
    val userGps by viewModel.userGpsLocation.collectAsState()
    val gpsAccuracy by viewModel.gpsAccuracyMeters.collectAsState()
    val gpsAltitude by viewModel.gpsAltitudeMeters.collectAsState()
    val gpsHeading by viewModel.gpsHeadingDegrees.collectAsState()
    val operators by viewModel.operators.collectAsState()
    val userOp = operators.firstOrNull { it.isUserDevice } ?: operators.firstOrNull()

    val currentLat = userGps?.lat ?: userOp?.latitude ?: viewModel.mapCenterLat.value
    val currentLng = userGps?.lng ?: userOp?.longitude ?: viewModel.mapCenterLng.value
    val currentAlt = gpsAltitude ?: userOp?.altitude ?: 45.0
    val currentAccuracy = gpsAccuracy ?: 3.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = TacticalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEU PONTO GPS / TELEMETRIA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TacticalCyan
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Operator badge
                userOp?.let { op ->
                    Surface(
                        color = TacticalGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "OPERADOR:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalGreen
                            )
                            Text(
                                text = "${op.callsign} (${op.role} - Squad ${op.squad})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Grid MGRS & Lat/Lng Card
                Surface(
                    color = Color(0xFF0F1713),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "COORDENADAS GEOGRÁFICAS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan
                        )
                        Text(
                            text = GeoUtils.formatTacticalGrid(currentLat, currentLng),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "LAT: ${"%.6f".format(currentLat)} | LNG: ${"%.6f".format(currentLng)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                    }
                }

                // Precision & Telemetry metrics
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFF0F1713),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PRECISÃO GPS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "± ${"%.1f".format(currentAccuracy)}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentAccuracy < 6f) TacticalGreen else TacticalAmber
                            )
                            Text(
                                text = if (currentAccuracy < 6f) "ALTA PRECISÃO" else "SINAL NORMAL",
                                fontSize = 7.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF0F1713),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("ALTITUDE / RUMO", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${currentAlt.roundToInt()}m | ${gpsHeading?.roundToInt() ?: 0}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TacticalCyan
                            )
                            Text(
                                text = "NÍVEL DO MAR",
                                fontSize = 7.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Quick Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipText = "COORD: Lat ${"%.6f".format(currentLat)}, Lng ${"%.6f".format(currentLng)} | MGRS: ${GeoUtils.formatTacticalGrid(currentLat, currentLng)}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("GPS Coords", clipText))
                            Toast.makeText(context, "Coordenadas copiadas para o rádio!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("COPIAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenMarkWaypoint,
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CRIAR PONTO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCenterGps,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_center_gps_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("CENTRALIZAR NO MAPA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = Color.Gray)
            }
        },
        containerColor = TacticalCardDark
    )
}

@Composable
fun MarkGpsWaypointDialog(
    viewModel: TacticalViewModel,
    onDismiss: () -> Unit
) {
    var waypointName by remember { mutableStateOf("Rally Point / Ponto de Encontro") }
    var waypointType by remember { mutableStateOf("STRATEGIC_OBJECTIVE") }
    val userGps by viewModel.userGpsLocation.collectAsState()
    val centerLat by viewModel.mapCenterLat.collectAsState()
    val centerLng by viewModel.mapCenterLng.collectAsState()

    val targetLat = userGps?.lat ?: centerLat
    val targetLng = userGps?.lng ?: centerLng

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = TacticalAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MARCAR PONTO TÁTICO NO MEU GPS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalAmber
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Posição Atual do GPS: ${GeoUtils.formatTacticalGrid(targetLat, targetLng)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalCyan
                )

                OutlinedTextField(
                    value = waypointName,
                    onValueChange = { waypointName = it },
                    label = { Text("Nome do Ponto / Objetivo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalAmber,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de Marcação no Terreno:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = waypointType == "STRATEGIC_OBJECTIVE",
                        onClick = { waypointType = "STRATEGIC_OBJECTIVE" },
                        label = { Text("Rally Point / Objetivo", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = waypointType == "SAFE_ZONE",
                        onClick = { waypointType = "SAFE_ZONE" },
                        label = { Text("Safe Zone", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = waypointType == "EXCLUSION",
                        onClick = { waypointType = "EXCLUSION" },
                        label = { Text("Área de Risco / Exclusão", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.markWaypointAtCurrentGps(waypointName, waypointType)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber, contentColor = Color.Black)
            ) {
                Text("SALVAR PONTO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = TacticalCardDark
    )
}

