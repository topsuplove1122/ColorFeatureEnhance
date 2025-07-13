package com.itosfish.colorfeatureenhance.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itosfish.colorfeatureenhance.R

/**
 * 浮动操作按钮组，包含添加和搜索按钮
 * @param onAddClick 添加按钮点击回调
 * @param onSearchClick 搜索按钮点击回调
 * @param isSearchActive 搜索是否激活
 */
@Composable
fun FloatingActionButtonGroup(
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSearchActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // 搜索按钮
        FloatingActionButton(
            onClick = onSearchClick,
            containerColor = if (!isSearchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondary,
            contentColor = if (!isSearchActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(id = R.string.search_feature)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 添加按钮
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.add_feature)
            )
        }
    }
}