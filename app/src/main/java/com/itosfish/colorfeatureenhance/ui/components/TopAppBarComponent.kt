package com.itosfish.colorfeatureenhance.ui.components

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.itosfish.colorfeatureenhance.FeatureMode
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.utils.showAboutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSTopAppBar(
    title: String,
    currentMode: FeatureMode,
    onModeChange: (FeatureMode) -> Unit,
    isSearchActive: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(title) },
        actions = {
            // 模式切换按钮
            TextButton(onClick = {
                val next =
                    if (currentMode == FeatureMode.APP) FeatureMode.OPLUS else FeatureMode.APP
                onModeChange(next)
            }) {
                val labelRes =
                    if (currentMode == FeatureMode.APP) R.string.mode_app else R.string.mode_oplus
                Text(text = stringResource(id = labelRes))
            }
            // 搜索按钮（可选）
            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.search_feature),
                        tint = if (isSearchActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // 刷新按钮（可选）
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.refresh)
                    )
                }
            }
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