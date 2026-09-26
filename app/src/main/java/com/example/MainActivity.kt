package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.inventory.OperatorInventoryScreen
import com.example.ui.screens.logs.TacticalLogsScreen
import com.example.ui.screens.map.TacticalMapScreen
import com.example.ui.screens.operators.OperatorsListScreen
import com.example.ui.screens.qr.QrScannerSheet
import com.example.ui.screens.zones.ZonesManagementScreen
import com.example.ui.theme.BallisticBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.viewmodel.TacticalViewModel

enum class MainNavigationTab {
    MAP,
    OPERATORS,
    ZONES,
    LOGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: TacticalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                TacOpsApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TacOpsApp(viewModel: TacticalViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavigationTab.MAP) }
    var activeInventoryOperatorId by remember { mutableStateOf<Long?>(null) }
    var isQrModalOpen by remember { mutableStateOf(false) }

    // Request permissions for Camera and GPS location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            setupLocationUpdates(context, viewModel)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    // Back handler for Sub-screens (Inventory)
    if (activeInventoryOperatorId != null) {
        BackHandler {
            activeInventoryOperatorId = null
        }
        OperatorInventoryScreen(
            operatorId = activeInventoryOperatorId!!,
            viewModel = viewModel,
            onBack = { activeInventoryOperatorId = null },
            onShowQrCode = { payload ->
                viewModel.qrSharePayload.value = payload
                isQrModalOpen = true
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().background(BallisticBlack),
            bottomBar = {
                NavigationBar(
                    containerColor = TacticalCardDark,
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.MAP,
                        onClick = { currentTab = MainNavigationTab.MAP },
                        icon = {
                            Icon(Icons.Default.Map, contentDescription = "Mapa Tático", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("MAPA", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = TacticalGreen,
                            indicatorColor = TacticalGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_tab_map")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.OPERATORS,
                        onClick = { currentTab = MainNavigationTab.OPERATORS },
                        icon = {
                            Icon(Icons.Default.Groups, contentDescription = "Operadores", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("OPERADORES", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = TacticalGreen,
                            indicatorColor = TacticalGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_tab_operators")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.ZONES,
                        onClick = { currentTab = MainNavigationTab.ZONES },
                        icon = {
                            Icon(Icons.Default.Polyline, contentDescription = "Zonas Táticas", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("ZONAS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = TacticalGreen,
                            indicatorColor = TacticalGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_tab_zones")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.LOGS,
                        onClick = { currentTab = MainNavigationTab.LOGS },
                        icon = {
                            Icon(Icons.Default.Assignment, contentDescription = "Logs de Combate", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = TacticalGreen,
                            indicatorColor = TacticalGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_tab_logs")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    MainNavigationTab.MAP -> {
                        TacticalMapScreen(
                            viewModel = viewModel,
                            onNavigateToOperatorInventory = { opId ->
                                activeInventoryOperatorId = opId
                            },
                            onOpenQrScanner = {
                                viewModel.qrSharePayload.value = null
                                isQrModalOpen = true
                            }
                        )
                    }
                    MainNavigationTab.OPERATORS -> {
                        OperatorsListScreen(
                            viewModel = viewModel,
                            onNavigateToInventory = { opId ->
                                activeInventoryOperatorId = opId
                            },
                            onCenterOnMap = { lat, lng ->
                                viewModel.centerMapOn(lat, lng)
                                currentTab = MainNavigationTab.MAP
                            },
                            onShowQrCode = { payload ->
                                viewModel.qrSharePayload.value = payload
                                isQrModalOpen = true
                            }
                        )
                    }
                    MainNavigationTab.ZONES -> {
                        ZonesManagementScreen(
                            viewModel = viewModel,
                            onNavigateToMap = {
                                currentTab = MainNavigationTab.MAP
                            }
                        )
                    }
                    MainNavigationTab.LOGS -> {
                        TacticalLogsScreen(viewModel = viewModel)
                    }
                }

                // QR Modal Sheet
                if (isQrModalOpen) {
                    QrScannerSheet(
                        viewModel = viewModel,
                        onDismiss = { isQrModalOpen = false }
                    )
                }
            }
        }
    }
}

/**
 * Connects device GPS updates to user's operator entity on the field
 */
private fun setupLocationUpdates(context: Context, viewModel: TacticalViewModel) {
    try {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            
            // Check last known location immediately
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val initialLoc = when {
                lastGps != null && lastNet != null -> if (lastGps.time >= lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            initialLoc?.let { loc ->
                viewModel.updateUserGpsLocation(
                    lat = loc.latitude,
                    lng = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    heading = if (loc.hasBearing()) loc.bearing else null
                )
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    viewModel.updateUserGpsLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        heading = if (location.hasBearing()) location.bearing else null
                    )
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    locationListener
                )
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    4000L,
                    2f,
                    locationListener
                )
            }
        }
    } catch (_: Exception) {}
}
