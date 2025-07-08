package com.itosfish.colorfeatureenhance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import com.itosfish.colorfeatureenhance.BuildConfig
import com.itosfish.colorfeatureenhance.utils.BlurBehindDialogEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSTopAppBar(title: String) {
    val showAboutDialog = remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { showAboutDialog.value = true }) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "关于"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
    
    if (showAboutDialog.value) {
        AboutDialog(onDismiss = { showAboutDialog.value = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    BlurBehindDialogEffect(enable = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = { 
            Text("ColorOS特性补全可视化编辑器\n\n版本: ${BuildConfig.VERSION_CODE}\n\n©2025 ItosEO & 海里无鱼")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )

} 