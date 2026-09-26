package com.example.ui.screens.operators

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.OperatorEntity
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

@Composable
fun OperatorsListScreen(
    viewModel: TacticalViewModel,
    onNavigateToInventory: (Long) -> Unit,
    onCenterOnMap: (Double, Double) -> Unit,
    onShowQrCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val operators by viewModel.operators.collectAsState()
    var selectedSquadFilter by remember { mutableStateOf("TODOS") }
    var showAddOperatorDialog by remember { mutableStateOf(false) }

    // Statistics
    val activeCount = operators.count { it.healthState == "ACTIVE" }
    val combatCount = operators.count { it.healthState == "COMBAT" || it.healthState == "WOUNDED" }
    val kiaCount = operators.count { it.healthState == "KIA" }

    val filteredOperators = if (selectedSquadFilter == "TODOS") {
        operators
    } else {
        operators.filter { it.squad.equals(selectedSquadFilter, ignoreCase = true) }
    }

    Box(modifier = modifier.fillMaxSize().background(BallisticBlack)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Squad Health & Telemetry Stats
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ESTADO DO EFETIVO EM CAMPO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusSummaryCard(
                        title = "ATIVOS",
                        count = activeCount,
                        color = TacticalGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryCard(
                        title = "COMBATE/FERIDOS",
                        count = combatCount,
                        color = TacticalAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryCard(
                        title = "K.I.A. / MORTO",
                        count = kiaCount,
                        color = TacticalRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Squad Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val squads = listOf("TODOS", "Alpha", "Bravo", "Delta")
                    for (sq in squads) {
                        FilterChip(
                            selected = selectedSquadFilter == sq,
                            onClick = { selectedSquadFilter = sq },
                            label = { Text(sq, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Operators List
            if (filteredOperators.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "NENHUM OPERADOR CADASTRADO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Não há membros no esquadrão '$selectedSquadFilter'. Toque no botão '+' abaixo para adicionar novos operadores.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredOperators, key = { it.id }) { operator ->
                    OperatorCard(
                        operator = operator,
                        onReportHit = { viewModel.reportHit(operator.id) },
                        onRevive = { viewModel.reviveOperator(operator.id) },
                        onOpenInventory = { onNavigateToInventory(operator.id) },
                        onCenterMap = { onCenterOnMap(operator.latitude, operator.longitude) },
                        onShareQr = {
                            val payload = viewModel.generateOperatorQrPayload(operator)
                            onShowQrCode(payload)
                        },
                        onDelete = { viewModel.deleteOperator(operator) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to add Operator
        FloatingActionButton(
            onClick = { showAddOperatorDialog = true },
            containerColor = TacticalGreen,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_operator_fab")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Adicionar Operador")
        }
    }

    if (showAddOperatorDialog) {
        AddOperatorDialog(
            onDismiss = { showAddOperatorDialog = false },
            onConfirm = { callsign, name, role, squad, radio ->
                viewModel.addOperator(callsign, name, role, squad, radio)
                showAddOperatorDialog = false
            }
        )
    }
}

@Composable
private fun StatusSummaryCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun OperatorCard(
    operator: OperatorEntity,
    onReportHit: () -> Unit,
    onRevive: () -> Unit,
    onOpenInventory: () -> Unit,
    onCenterMap: () -> Unit,
    onShareQr: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val (statusText, statusColor) = when (operator.healthState) {
        "ACTIVE" -> Pair("100% OPERACIONAL", TacticalGreen)
        "COMBAT" -> Pair("EM COMBATE", TacticalAmber)
        "WOUNDED" -> Pair("FERIDO / SANGRANDO", Color(0xFFFF9100))
        "KIA" -> Pair("ELIMINADO (K.I.A.)", TacticalRed)
        else -> Pair("RESPAWN", TacticalBlue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("operator_card_${operator.callsign}"),
        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (operator.healthState == "KIA") TacticalRed else TacticalBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Callsign, Role & Squad, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(1.5.dp, statusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (operator.healthState == "KIA") Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = operator.callsign,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (operator.isUserDevice) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = TacticalCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "MEU DISPOSITIVO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TacticalCyan,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${operator.role} • Esquadrão ${operator.squad}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Health Status Pill & Quick Delete Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp).testTag("quick_delete_operator_${operator.callsign}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover Operador",
                            tint = TacticalRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HP Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "INTEGRIDADE FÍSICA: ${operator.hpPercent}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "HITS SOFRIDOS: ${operator.hitsTaken}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { operator.hpPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color(0xFF1E2822)
            )

            // Respawn countdown if eliminated
            if (operator.healthState == "KIA" && operator.respawnTimerSeconds > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = TacticalRed.copy(alpha = 0.2f),
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
                            text = "RESPAWN EM: ${operator.respawnTimerSeconds}s (Aguarde no ponto de renascimento)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactical Telemetry & Comms Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(operator.radioFrequency, fontSize = 10.sp, color = TacticalCyan, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${operator.batteryPercent}%", fontSize = 10.sp, color = TacticalGreen, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = GeoUtils.formatTacticalGrid(operator.latitude, operator.longitude).take(15) + "...",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hit Button (Tiro recebido)
                Button(
                    onClick = onReportHit,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalRed.copy(alpha = 0.2f), contentColor = TacticalRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Text("HIT!", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Medic / Heal Button
                OutlinedButton(
                    onClick = onRevive,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = "Curar", tint = TacticalGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("MÉDICO", fontSize = 10.sp, color = TacticalGreen, fontWeight = FontWeight.Bold)
                }

                // Inventory Button
                Button(
                    onClick = onOpenInventory,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCardDark, contentColor = TacticalCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Text("INVENTÁRIO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Center on Map
                IconButton(
                    onClick = onCenterMap,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Ver no mapa", tint = TacticalGreen)
                }

                // QR Code
                IconButton(
                    onClick = onShareQr,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = TacticalCyan)
                }

                // Remove Operator Button (Requested Feature)
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp).testTag("delete_operator_${operator.callsign}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover Operador",
                        tint = TacticalRed.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = TacticalRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "REMOVER OPERADOR?",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = TacticalRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Confirma a exclusão de ${operator.callsign} (${operator.realName}) da operação?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Esquadrão: ${operator.squad}\n• Função: ${operator.role}\n• Rádio: ${operator.radioFrequency}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Esta ação removerá o operador do mapa de satélite e excluirá seu inventário de armas e munições associadas.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    if (operator.isUserDevice) {
                        Surface(
                            color = TacticalAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⚠️ Atenção: Este é o seu dispositivo principal de comandante. Se removido, outro operador assumirá a liderança em campo.",
                                color = TacticalAmber,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_operator_button")
                ) {
                    Text("REMOVER DA OPERAÇÃO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = TacticalCardDark
        )
    }
}

@Composable
fun AddOperatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (callsign: String, name: String, role: String, squad: String, radio: String) -> Unit
) {
    var callsign by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Assalto") }
    var squad by remember { mutableStateOf("Alpha") }
    var radio by remember { mutableStateOf("CH 01 - 462.562 MHz") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "INCLUIR NOVO OPERADOR",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it },
                    label = { Text("Callsign / Codinome (ex: Falcon)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalGreen,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_callsign")
                )

                OutlinedTextField(
                    value = realName,
                    onValueChange = { realName = it },
                    label = { Text("Nome Real do Operador") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalGreen,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Função Operacional:", fontSize = 11.sp, color = TacticalCyan, fontWeight = FontWeight.Bold)
                val roles = listOf("Assalto", "Sniper / DMR", "Suporte", "Batedor", "Médico")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in roles.take(3)) {
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in roles.drop(3)) {
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Text("Esquadrão:", fontSize = 11.sp, color = TacticalCyan, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val squads = listOf("Alpha", "Bravo", "Delta")
                    for (sq in squads) {
                        FilterChip(
                            selected = squad == sq,
                            onClick = { squad = sq },
                            label = { Text(sq, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = radio,
                    onValueChange = { radio = it },
                    label = { Text("Canal / Frequência de Rádio") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalGreen,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (callsign.isNotBlank()) {
                        onConfirm(callsign, realName, role, squad, radio)
                    }
                },
                enabled = callsign.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_add_operator_button")
            ) {
                Text("CADASTRAR", fontWeight = FontWeight.Bold)
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
