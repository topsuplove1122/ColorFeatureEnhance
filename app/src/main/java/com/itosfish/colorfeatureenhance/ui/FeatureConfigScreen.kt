package com.itosfish.colorfeatureenhance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.itosfish.colorfeatureenhance.ui.components.ColorOSTopAppBar

@Composable
fun FeatureConfigScreen() {
    Scaffold(
        topBar = {
            ColorOSTopAppBar(title = "ColorOS特性补全")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(text = "特性补全配置页面（待实现）")
        }
    }
} 