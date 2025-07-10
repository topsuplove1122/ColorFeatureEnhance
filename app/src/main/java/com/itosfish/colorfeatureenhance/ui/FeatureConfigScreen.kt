package com.itosfish.colorfeatureenhance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.AppFeatureMappings
import com.itosfish.colorfeatureenhance.data.model.FeatureGroup
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.ui.components.ColorOSTopAppBar
import kotlinx.coroutines.launch
import com.itosfish.colorfeatureenhance.utils.AddFeatureDialog
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.itosfish.colorfeatureenhance.MainActivity
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureConfigScreen(
    configPath: String,
    repository: FeatureRepository = remember { XmlFeatureRepository() }
) {
    val scope = rememberCoroutineScope()
    var features by remember { mutableStateOf<List<AppFeature>>(emptyList()) }
    val featureGroups by remember(features) {
        derivedStateOf {
            // 按描述文本分组
            features.groupBy { feature -> 
                // 使用描述作为分组键
                val description = AppFeatureMappings.getLocalizedDescription(app, feature.name)
                // 对于未知特性（没有预设映射且没有用户自定义映射），使用name作为分组键的一部分
                // 以避免不同的未知特性被错误地合并
                if (AppFeatureMappings.getInstance().getResId(feature.name) == R.string.feature_unknown && 
                    description == feature.name) {
                    "unknown_${feature.name}" // 未知特性，使用name区分
                } else {
                    "desc_${description}" // 已知特性，仅使用描述分组
                }
            }.map { (_, groupFeatures) ->
                val nameResId = AppFeatureMappings.getInstance().getResId(groupFeatures.first().name)
                FeatureGroup(nameResId, groupFeatures)
            }.sortedWith(
                compareBy<FeatureGroup> { 
                    // 首先按是否为未知特性排序
                    it.nameResId != R.string.feature_unknown 
                }.thenBy { 
                    // 然后按描述文本排序
                    AppFeatureMappings.getLocalizedDescription(app, it.features.first().name)
                }
            )
        }
    }
    
    val context = LocalContext.current

    var groupToDelete by remember { mutableStateOf<FeatureGroup?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(true) }

    // 检测滚动方向的 NestedScrollConnection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 向下滚动时隐藏FAB，向上滚动时显示FAB
                if (available.y < -10) { // 向上拖动（列表向下滚动）
                    fabVisible = false
                } else if (available.y > 10) { // 向下拖动（列表向上滚动）
                    fabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // 加载配置
    LaunchedEffect(configPath) {
        features = repository.loadFeatures(configPath)
    }

    Scaffold(
        topBar = {
            ColorOSTopAppBar(title = stringResource(id = R.string.app_title))
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.add_feature))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            items(featureGroups, key = {
                // 使用与分组相同的逻辑生成key
                val firstFeature = it.features.first()
                val description = AppFeatureMappings.getLocalizedDescription(context, firstFeature.name)
                
                if (it.nameResId == R.string.feature_unknown && 
                    description == firstFeature.name) {
                    "unknown_${firstFeature.name}"
                } else {
                    "desc_${description}"
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

        // 添加特性对话框
        if (showAddDialog) {
            AddFeatureDialog(
                onDismiss = { showAddDialog = false },
                context = context,
                onConfirm = { name, description, enabled ->
                    // 添加新特性
                    val newFeature = AppFeature(name, enabled)
                    val newList = features + newFeature
                    features = newList

                    // 保存到文件
                    scope.launch {
                        repository.saveFeatures(configPath, newList)
                    }

                    showAddDialog = false
                }
            )
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
                        
                        // 删除用户映射
                        val namesToDelete = deleting.features.map { it.name }
                        AppFeatureMappings.getInstance().removeUserMappings(context, namesToDelete)
                        
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
            // 获取特性描述（优先用户自定义描述）
            val context = LocalContext.current
            val label = AppFeatureMappings.getLocalizedDescription(context, group.features.first().name)
            
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFeatureDialog(
    onDismiss: () -> Unit,
    context: Context,
    onConfirm: (name: String, description: String, enabled: Boolean) -> Unit
) {
    var featureName by remember { mutableStateOf("") }
    var featureDescription by remember { mutableStateOf("") }
    var featureEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_feature)) },
        text = {
            Column {
                // 特性名称输入
                OutlinedTextField(
                    value = featureName,
                    onValueChange = { featureName = it },
                    label = { Text(stringResource(id = R.string.feature_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 特性描述输入
                OutlinedTextField(
                    value = featureDescription,
                    onValueChange = { featureDescription = it },
                    label = { Text(stringResource(id = R.string.feature_description)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 启用状态选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.feature_enabled))
                    Switch(
                        checked = featureEnabled,
                        onCheckedChange = { featureEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (featureName.isNotEmpty() && featureDescription.isNotEmpty()) {
                        onConfirm(featureName, featureDescription, featureEnabled)
                    }
                },
                enabled = featureName.isNotEmpty() && featureDescription.isNotEmpty()
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FeatureConfigPreview() {
    // 使用示例配置路径或空列表
    FeatureConfigScreen(configPath = "")
} 