package com.meter.app.ui.meter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meter.app.ble.ConnectionState
import com.meter.app.ui.theme.MeterGreen
import com.meter.app.ui.theme.OfflineGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterDetailScreen(
    meterId: String,
    onBackClick: () -> Unit,
    viewModel: MeterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val meter by viewModel.meter.collectAsState(initial = null)
    val latestReading by viewModel.latestReading.collectAsState(initial = null)
    val connectionState by viewModel.connectionState.collectAsState()
    val receivedData by viewModel.receivedData.collectAsState()

    val isConnected = connectionState is ConnectionState.Connected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meter?.name ?: "电表详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isConnected) {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isConnected) MeterGreen else OfflineGray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "已连接"
                                is ConnectionState.Connecting -> "连接中..."
                                is ConnectionState.Disconnected -> "未连接"
                                is ConnectionState.Error -> "连接错误"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = meter?.macAddress ?: meter?.address ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!isConnected) {
                        Button(
                            onClick = { viewModel.refreshData() }
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("连接")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text("断开")
                        }
                    }
                }
            }

            // Real-time data
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("实时数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DataItem("功率", "${latestReading?.currentPower ?: 0f}", "W")
                        DataItem("电压", "${latestReading?.voltage ?: 0f}", "V")
                        DataItem("电流", "${latestReading?.current ?: 0f}", "A")
                    }
                }
            }

            // Energy stats
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("用电统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DataItem("今日", "${latestReading?.dailyEnergy ?: 0f}", "kWh")
                        DataItem("本月", "${latestReading?.monthlyEnergy ?: 0f}", "kWh")
                        DataItem("总计", "${latestReading?.totalEnergy ?: 0f}", "kWh")
                    }
                }
            }

            // Raw data for debugging
            if (receivedData != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("原始数据 (调试)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = receivedData!!.joinToString(" ") { "%02X".format(it) },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DataItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}