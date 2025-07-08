package com.itosfish.colorfeatureenhance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.ui.components.ColorOSTopAppBar

@Composable
fun FeatureConfigScreen() {
    Scaffold(
        topBar = {
            ColorOSTopAppBar(title = stringResource(id = R.string.app_title))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(text = stringResource(id = R.string.feature_config_placeholder))
        }
    }
} 