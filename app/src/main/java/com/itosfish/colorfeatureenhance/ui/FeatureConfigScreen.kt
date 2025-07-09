package com.itosfish.colorfeatureenhance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.ui.components.ColorOSTopAppBar
import com.itosfish.colorfeatureenhance.data.model.FeatureGroup
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.AppFeatureMappings
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Alignment

@Composable
fun FeatureConfigScreen(
    configPath: String,
    repository: FeatureRepository = remember { XmlFeatureRepository() }
) {
    val scope = rememberCoroutineScope()
    var features by remember { mutableStateOf<List<AppFeature>>(emptyList()) }
    val featureGroups by remember(features) {
        derivedStateOf {
            // 按nameResId分组
            features.groupBy { feature ->
                val resId = AppFeatureMappings.getResId(feature.name)
                if (resId == R.string.feature_unknown) {
                    "${resId}_${feature.name}" // unique per unknown feature
                } else {
                    resId.toString()
                }
            }.map { (_, groupFeatures) ->
                val nameResId = AppFeatureMappings.getResId(groupFeatures.first().name)
                FeatureGroup(nameResId, groupFeatures)
            }.sortedWith(compareBy<FeatureGroup> { it.nameResId }.thenBy { it.features.first().name })
        }
    }

    var groupToDelete by remember { mutableStateOf<FeatureGroup?>(null) }

    // 加载配置
    LaunchedEffect(configPath) {
        features = repository.loadFeatures(configPath)
    }

    Scaffold(
        topBar = {
            ColorOSTopAppBar(title = stringResource(id = R.string.app_title))
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            items(featureGroups, key = {
                if (it.nameResId == R.string.feature_unknown) {
                    "${it.nameResId}_${it.features.first().name}"
                } else {
                    it.nameResId.toString()
                }
            }) { group ->
                FeatureGroupItem(
                    group = group,
                    onToggle = { updatedGroup ->
                        // 更新组内所有特性的状态
                        val updatedFeatures = features.map { feature ->
                            val shouldUpdate = updatedGroup.features.any { it.name == feature.name }
                            if (shouldUpdate) feature.copy(enabled = updatedGroup.isEnabled) else feature
                        }
                        features = updatedFeatures
                        
                        // 异步保存
                        scope.launch {
                            repository.saveFeatures(configPath, updatedFeatures)
                        }
                    },
                    onLongPress = {
                        groupToDelete = group
                    }
                )
            }
        }

        // 删除确认对话框
        groupToDelete?.let { deleting ->
            val label = if (deleting.nameResId == R.string.feature_unknown) {
                deleting.features.firstOrNull()?.name ?: "Unknown"
            } else stringResource(id = deleting.nameResId)

            AlertDialog(
                onDismissRequest = { groupToDelete = null },
                title = { Text(text = stringResource(id = R.string.delete_confirm_title)) },
                text = { Text(text = stringResource(id = R.string.delete_confirm_message, label)) },
                confirmButton = {
                    TextButton(onClick = {
                        // 删除并保存
                        val updatedFeatures = features.filterNot { feature ->
                            deleting.features.any { it.name == feature.name }
                        }
                        features = updatedFeatures
                        scope.launch {
                            repository.saveFeatures(configPath, updatedFeatures)
                        }
                        groupToDelete = null
                    }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { groupToDelete = null }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun FeatureGroupItem(
    group: FeatureGroup,
    onToggle: (FeatureGroup) -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = if (group.nameResId == R.string.feature_unknown) {
                // 对于未知特性，显示原始名称
                group.features.firstOrNull()?.name ?: "Unknown"
            } else {
                stringResource(id = group.nameResId)
            }
            
            // 如果组内有多个项，显示计数
            val displayText = if (group.features.size > 1) {
                "$label (${group.features.size})"
            } else {
                label
            }
            
            Text(
                text = displayText, 
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = group.isEnabled,
                onCheckedChange = {
                    onToggle(group.withEnabled(it))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureConfigPreview() {
    // 使用示例配置路径或空列表
    FeatureConfigScreen(configPath = "")
} 