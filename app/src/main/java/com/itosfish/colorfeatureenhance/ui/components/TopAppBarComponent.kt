package com.itosfish.colorfeatureenhance.ui.components

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.utils.showAboutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSTopAppBar(title: String) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = {
                showAboutDialog(context as Activity)
            }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(id = R.string.about)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
} 