package com.meter.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.meter.app.ble.BleDevice
import com.meter.app.ble.ScanState
import com.meter.app.domain.model.Meter
import com.meter.app.ui.theme.MeterGreen
import com.meter.app.ui.theme.OfflineGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMeterClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val meters by viewModel.meters.collectAsState(initial = emptyList())
    val scanState by viewModel.scanState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val context = LocalContext.current

    var showScanDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
            showScanDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("辰域电表", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                        val allGranted = perms.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (allGranted) {
                            viewModel.startScan()
                            showScanDialog = true
                        } else {
                            permissionLauncher.launch(perms)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加电表")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("用电概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("今日用电", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${String.format("%.2f", uiState.todayEnergy)} kWh",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("本月用电", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${String.format("%.2f", uiState.monthEnergy)} kWh",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Meter list header
            Text(
                "我的电表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (meters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无电表", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击右上角 + 按钮扫描添加", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meters) { meter ->
                        MeterCard(meter = meter, onClick = { onMeterClick(meter.id) })
                    }
                }
            }
        }
    }

    // Scan dialog
    if (showScanDialog) {
        ScanDeviceDialog(
            scanState = scanState,
            devices = discoveredDevices,
            onDismiss = {
                showScanDialog = false
                viewModel.stopScan()
            },
            onConnect = { device ->
                viewModel.connectDevice(device)
                showScanDialog = false
            },
            onRefresh = { viewModel.startScan() }
        )
    }
}

@Composable
fun ScanDeviceDialog(
    scanState: ScanState,
    devices: List<BleDevice>,
    onDismiss: () -> Unit,
    onConnect: (BleDevice) -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("扫描设备") },
        text = {
            Column {
                when (scanState) {
                    is ScanState.Scanning -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在扫描附近设备...")
                    }
                    is ScanState.Error -> {
                        Text(scanState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRefresh) { Text("重试") }
                    }
                    else -> {
                        if (devices.isEmpty()) {
                            Text("未发现设备，请确保电表已开启")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text("重新扫描") }
                        }
                    }
                }

                if (devices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    devices.forEach { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text(device.address) },
                            leadingContent = {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                TextButton(onClick = { onConnect(device) }) {
                                    Text("连接")
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun MeterCard(meter: Meter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (meter.isOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (meter.isOnline) MeterGreen else OfflineGray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(meter.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(meter.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (meter.isOnline) "在线" else "离线",
                color = if (meter.isOnline) MeterGreen else OfflineGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}