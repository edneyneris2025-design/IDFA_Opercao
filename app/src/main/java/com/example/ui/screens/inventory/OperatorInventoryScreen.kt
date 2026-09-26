package com.example.ui.screens.inventory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.InventoryItemEntity
import com.example.data.local.model.OperatorEntity
import com.example.ui.theme.BallisticBlack
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalRed
import com.example.ui.viewmodel.TacticalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorInventoryScreen(
    operatorId: Long,
    viewModel: TacticalViewModel,
    onBack: () -> Unit,
    onShowQrCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val operators by viewModel.operators.collectAsState()
    val operator = operators.firstOrNull { it.id == operatorId }
    val inventory by viewModel.selectedOperatorInventory.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDeleteOperatorConfirm by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }

    val categories = listOf("TODOS", "PRIMARY", "SECONDARY", "AMMO", "GEAR", "TACTICAL", "MEDICAL")

    val filteredItems = if (selectedCategoryFilter == "TODOS") {
        inventory
    } else {
        inventory.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "INVENTÁRIO / LOADOUT TÁTICO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${operator?.callsign ?: "Operador"} • ${operator?.role ?: ""}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TacticalGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            operator?.let {
                                val payload = viewModel.generateOperatorQrPayload(it)
                                onShowQrCode(payload)
                            }
                        }
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Gerar QR Loadout", tint = TacticalCyan)
                    }
                    IconButton(
                        onClick = { showDeleteOperatorConfirm = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover Operador", tint = TacticalRed.copy(alpha = 0.85f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TacticalCardDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = TacticalGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_inventory_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Item")
            }
        },
        containerColor = BallisticBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Operator Header Card with Tactical Specs
            item {
                Spacer(modifier = Modifier.height(6.dp))
                operator?.let { op ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "OPERADOR: ${op.callsign.uppercase()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Nome: ${op.realName} | Esquadrão: ${op.squad}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Frequência Rádio: ${op.radioFrequency}",
                                    fontSize = 11.sp,
                                    color = TacticalCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (op.healthState == "ACTIVE") TacticalGreen.copy(alpha = 0.2f) else TacticalAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (op.healthState == "ACTIVE") TacticalGreen else TacticalAmber)
                            ) {
                                Text(
                                    text = "${op.hpPercent}% HP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (op.healthState == "ACTIVE") TacticalGreen else TacticalAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Categories Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cat in categories.take(4)) {
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(translateCategory(cat), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cat in categories.drop(4)) {
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(translateCategory(cat), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Inventory Items
            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum equipamento cadastrado nesta categoria.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        onIncrement = { viewModel.updateItemQuantity(item, 1) },
                        onDecrement = { viewModel.updateItemQuantity(item, -1) },
                        onToggleEquip = { viewModel.toggleEquipped(item) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddItemDialog) {
        AddInventoryItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { cat, name, qty, details ->
                viewModel.addInventoryItem(operatorId, cat, name, qty, details)
                showAddItemDialog = false
            }
        )
    }

    if (showDeleteOperatorConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteOperatorConfirm = false },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = TacticalRed, modifier = Modifier.size(28.dp))
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
                Text(
                    text = "Deseja realmente remover ${operator?.callsign ?: "este operador"} (${operator?.realName ?: ""}) da operação? Todos os itens de seu inventário serão apagados.",
                    color = Color.White,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteOperatorConfirm = false
                        operator?.let { op ->
                            viewModel.deleteOperator(op)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalRed, contentColor = Color.White)
                ) {
                    Text("REMOVER DA OPERAÇÃO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteOperatorConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = TacticalCardDark
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItemEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleEquip: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isEquipped) TacticalGreen.copy(alpha = 0.5f) else TacticalBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onToggleEquip, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (item.isEquipped) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Equipado",
                    tint = if (item.isEquipped) TacticalGreen else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF192620),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = translateCategory(item.category),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                if (item.details.isNotBlank()) {
                    Text(
                        text = item.details,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Quantity stepper & delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${item.quantity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = TacticalGreen, modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = TacticalRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, name: String, quantity: Int, details: String) -> Unit
) {
    var category by remember { mutableStateOf("PRIMARY") }
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "NOVO ITEM DE COMBATE",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Categoria:", fontSize = 11.sp, color = TacticalCyan, fontWeight = FontWeight.Bold)
                val cats = listOf("PRIMARY", "SECONDARY", "AMMO", "GEAR", "TACTICAL", "MEDICAL")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in cats.take(3)) {
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(translateCategory(c), fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in cats.drop(3)) {
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(translateCategory(c), fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Item / Arma (ex: M4 Carbine)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalGreen,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name")
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantidade (Mags, Unidades)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalGreen,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Especificações (FPS, Joule, Gramatura BB, etc.)") },
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
                    if (name.isNotBlank()) {
                        val qty = quantityText.toIntOrNull() ?: 1
                        onConfirm(category, name, qty, details)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_add_item_button")
            ) {
                Text("EQUIPAR", fontWeight = FontWeight.Bold)
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

fun translateCategory(cat: String): String = when (cat) {
    "TODOS" -> "TODOS"
    "PRIMARY" -> "PRIMÁRIA"
    "SECONDARY" -> "SECUNDÁRIA"
    "AMMO" -> "MUNIÇÃO / MAGS"
    "GEAR" -> "COLETE / PROTEÇÃO"
    "TACTICAL" -> "TÁTICO / RÁDIO"
    "MEDICAL" -> "MÉDICO / PYRO"
    else -> cat
}
