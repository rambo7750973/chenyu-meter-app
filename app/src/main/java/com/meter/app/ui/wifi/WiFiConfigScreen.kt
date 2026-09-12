package com.meter.app.ui.wifi

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.meter.app.ble.WiFiConfigState
import com.meter.app.ble.WiFiNetwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiConfigScreen(
    meterId: String,
    onBackClick: () -> Unit,
    viewModel: WiFiConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configState by viewModel.configState.collectAsState()
    val networks by viewModel.networks.collectAsState()
    val context = LocalContext.current

    var selectedNetwork by remember { mutableStateOf<WiFiNetwork?>(null) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.scanNetworks()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi配网") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (configState) {
                        is WiFiConfigState.Success -> MaterialTheme.colorScheme.primaryContainer
                        is WiFiConfigState.Error -> MaterialTheme.colorScheme.errorContainer
                        is WiFiConfigState.ConfigSent -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (configState) {
                                is WiFiConfigState.Success -> Icons.Default.CheckCircle
                                is WiFiConfigState.Error -> Icons.Default.Error
                                is WiFiConfigState.ConfigSent -> Icons.Default.Send
                                is WiFiConfigState.Configuring -> Icons.Default.Sync
                                is WiFiConfigState.Scanning -> Icons.Default.Search
                                else -> Icons.Default.Wifi
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            when (val state = configState) {
                                is WiFiConfigState.Idle -> "选择WiFi网络并输入密码"
                                is WiFiConfigState.Scanning -> "正在扫描网络..."
                                is WiFiConfigState.ScanComplete -> "扫描完成，共发现 ${networks.size} 个网络"
                                is WiFiConfigState.Configuring -> "正在发送配置..."
                                is WiFiConfigState.ConfigSent -> "配置已发送，等待设备连接"
                                is WiFiConfigState.Success -> state.message
                                is WiFiConfigState.Error -> state.message
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Scan button
            Button(
                onClick = {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE
                        )
                    }
                    val allGranted = perms.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        viewModel.scanNetworks()
                    } else {
                        permissionLauncher.launch(perms)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = configState !is WiFiConfigState.Scanning && configState !is WiFiConfigState.Configuring
            ) {
                if (configState is WiFiConfigState.Scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("扫描WiFi网络")
            }

            // Network list
            if (networks.isNotEmpty()) {
                Text("可用网络", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(networks) { network ->
                        NetworkItem(
                            network = network,
                            isSelected = selectedNetwork?.ssid == network.ssid,
                            onClick = { selectedNetwork = network }
                        )
                    }
                }
            } else if (configState !is WiFiConfigState.Scanning) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("点击上方按钮扫描WiFi网络", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Password input
            if (selectedNetwork != null) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("已选择: ${selectedNetwork!!.ssid}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("WiFi密码") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            enabled = configState !is WiFiConfigState.Configuring
                        )
                    }
                }
            }

            // Send config button
            Button(
                onClick = {
                    selectedNetwork?.let { network ->
                        viewModel.sendWiFiConfig(network.ssid, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedNetwork != null && password.isNotEmpty() &&
                        configState !is WiFiConfigState.Configuring &&
                        configState !is WiFiConfigState.ConfigSent
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("发送WiFi配置")
            }
        }
    }
}

@Composable
fun NetworkItem(
    network: WiFiNetwork,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                tint = when (network.signalStrength) {
                    4 -> MaterialTheme.colorScheme.primary
                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(network.ssid, fontWeight = FontWeight.Medium)
                Row {
                    Text(
                        if (network.security == "OPEN") "开放" else network.security,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${network.signalStrength}/4",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}