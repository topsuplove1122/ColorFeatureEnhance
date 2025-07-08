package com.itosfish.colorfeatureenhance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.itosfish.colorfeatureenhance.ui.components.showAboutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSTopAppBar(title: String) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { showAboutDialog(context as android.app.Activity) }) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "关于"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
} 