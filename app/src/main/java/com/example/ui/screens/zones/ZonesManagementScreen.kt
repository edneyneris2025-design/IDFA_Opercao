package com.example.ui.screens.zones

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.TacticalZoneEntity
import com.example.domain.geo.GeoUtils
import com.example.ui.theme.BallisticBlack
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalRed
import com.example.ui.viewmodel.TacticalViewModel
import kotlin.math.roundToInt

@Composable
fun ZonesManagementScreen(
    viewModel: TacticalViewModel,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zones by viewModel.zones.collectAsState()
    var zoneToDelete by remember { mutableStateOf<TacticalZoneEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().background(BallisticBlack)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ÁREAS & ZONAS DEMARCADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O aplicativo monitora continuamente a posição de cada operador. Se um membro entrar em uma zona demarcada, o sistema notifica em tempo real e a linha do polígono muda de cor imediatamente no satélite.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 15.sp
                )
            }

            if (zones.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Polyline, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhuma zona delimitada no momento.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use o botão lateral 'DESENHAR POLÍGONO' na tela do mapa para delimitar zonas de exclusão ou áreas de objetivo estratégico.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(zones, key = { it.id }) { zone ->
                    TacticalZoneCard(
                        zone = zone,
                        onCenterOnMap = {
                            val pts = GeoUtils.parsePointsJson(zone.pointsJson)
                            if (pts.isNotEmpty()) {
                                viewModel.centerMapOn(pts[0].lat, pts[0].lng)
                                onNavigateToMap()
                            }
                        },
                        onDelete = { zoneToDelete = zone }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    zoneToDelete?.let { zone ->
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            title = {
                Text(
                    text = "REMOVER ÁREA TÁTICA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalRed
                )
            },
            text = {
                Text("Deseja realmente apagar a zona '${zone.name}' do mapa tático?", color = Color.White)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteZone(zone)
                        zoneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalRed, contentColor = Color.White)
                ) {
                    Text("REMOVER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = TacticalCardDark
        )
    }
}

@Composable
fun TacticalZoneCard(
    zone: TacticalZoneEntity,
    onCenterOnMap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = GeoUtils.parsePointsJson(zone.pointsJson)
    val areaM2 = GeoUtils.calculatePolygonAreaMeters(points).roundToInt()

    val (typeLabel, baseColor) = when (zone.zoneType) {
        "EXCLUSION" -> Pair("ZONA DE EXCLUSÃO (PERIGO)", TacticalRed)
        "STRATEGIC_OBJECTIVE" -> Pair("OBJETIVO ESTRATÉGICO", TacticalGreen)
        "SAFE_ZONE" -> Pair("SAFE ZONE (NEUTRA)", TacticalAmber)
        else -> Pair("PONTO DE EXTRAÇÃO", TacticalBlue)
    }

    val isAlert = zone.isBreached

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_zone_${zone.name}"),
        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isAlert) TacticalRed else baseColor.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(if (isAlert) TacticalRed else baseColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = zone.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAlert) TacticalRed.copy(alpha = 0.2f) else baseColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAlert) TacticalRed else baseColor)
                ) {
                    Text(
                        text = if (isAlert) "OCUPADA / INVASÃO" else "DESIMPEDIDA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlert) TacticalRed else baseColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = typeLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = baseColor,
                fontFamily = FontFamily.Monospace
            )

            if (isAlert && zone.breachedByCallsigns.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = TacticalRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = TacticalRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Operadores dentro da área: ${zone.breachedByCallsigns} (Cor da linha alterada no mapa)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${points.size} Vértices • Área: ~${areaM2} m²",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )

                Row {
                    IconButton(onClick = onCenterOnMap, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Ver no mapa", tint = TacticalGreen)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TacticalRed.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
