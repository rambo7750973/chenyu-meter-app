package com.meter.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontWeight = FontWeight.Bold
                    )
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
        ) {
            // General settings
            Text(
                text = "通用设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                icon = Icons.Default.Person,
                title = "账户管理",
                subtitle = "管理您的账户信息",
                onClick = { /* TODO: Navigate to account */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "通知设置",
                subtitle = "管理应用通知",
                onClick = { /* TODO: Navigate to notifications */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Language,
                title = "语言设置",
                subtitle = "选择应用语言",
                onClick = { /* TODO: Navigate to language */ }
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // Meter settings
            Text(
                text = "电表设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                icon = Icons.Default.Bolt,
                title = "电价设置",
                subtitle = "配置电价类型和价格",
                onClick = { /* TODO: Navigate to pricing */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Sync,
                title = "数据同步",
                subtitle = "设置数据同步频率",
                onClick = { /* TODO: Navigate to sync */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "数据管理",
                subtitle = "导出或清除数据",
                onClick = { /* TODO: Navigate to data management */ }
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // About
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "关于应用",
                subtitle = "版本 1.0.0",
                onClick = { /* TODO: Show about dialog */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Help,
                title = "帮助与反馈",
                subtitle = "获取帮助或反馈问题",
                onClick = { /* TODO: Navigate to help */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Security,
                title = "隐私政策",
                subtitle = "查看隐私政策",
                onClick = { /* TODO: Navigate to privacy */ }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Logout button
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("退出登录")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}