package com.itosfish.colorfeatureenhance.ui

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itosfish.colorfeatureenhance.FeatureMode
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.config.ConfigMergeManager
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.AppFeatureMappings
import com.itosfish.colorfeatureenhance.data.model.FeatureGroup
import com.itosfish.colorfeatureenhance.data.model.OplusFeatureMappings
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.ui.components.ColorOSTopAppBar
import com.itosfish.colorfeatureenhance.ui.components.HighlightedText
import com.itosfish.colorfeatureenhance.ui.components.SearchBar
import com.itosfish.colorfeatureenhance.ui.search.SearchLogic
import com.itosfish.colorfeatureenhance.ui.theme.PatchColors
import com.itosfish.colorfeatureenhance.utils.AddFeatureDialog
import com.itosfish.colorfeatureenhance.utils.EditFeatureDialog
import kotlinx.coroutines.launch

@Composable
fun FeatureConfigScreen(
    configPath: String,
    currentMode: FeatureMode,
    onModeChange: (FeatureMode) -> Unit,
    repository: FeatureRepository
) {
    val scope = rememberCoroutineScope()
    var features by remember { mutableStateOf<List<AppFeature>>(emptyList(), neverEqualPolicy()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var patchActions by remember { mutableStateOf<Map<String, ConfigMergeManager.PatchAction>>(emptyMap()) }
    val featureGroups by remember(features, currentMode, refreshTrigger) {
        derivedStateOf {
            // 按描述文本分组
            features.groupBy { feature -> 
                // 使用描述作为分组键
                val description = if (currentMode == FeatureMode.APP) {
                    AppFeatureMappings.getLocalizedDescription(app, feature.name)
                } else {
                    OplusFeatureMappings.getLocalizedDescription(app, feature.name)
                }
                // 对于未知特性（没有预设映射且没有用户自定义映射），使用name作为分组键的一部分
                // 以避免不同的未知特性被错误地合并
                val resUnknown = if (currentMode == FeatureMode.APP) {
                    AppFeatureMappings.getInstance().getResId(feature.name)
                } else {
                    OplusFeatureMappings.getInstance().getResId(feature.name)
                }
                if (resUnknown == R.string.feature_unknown && 
                    description == feature.name) {
                    "unknown_${feature.name}" // 未知特性，使用name区分
                } else {
                    "desc_${description}" // 已知特性，仅使用描述分组
                }
            }.map { (_, groupFeatures) ->
                val nameResId = if (currentMode == FeatureMode.APP) {
                    AppFeatureMappings.getInstance().getResId(groupFeatures.first().name)
                } else {
                    OplusFeatureMappings.getInstance().getResId(groupFeatures.first().name)
                }
                FeatureGroup(nameResId, groupFeatures)
            }.sortedWith(
                compareBy<FeatureGroup> { 
                    // 首先按是否为未知特性排序
                    it.nameResId != R.string.feature_unknown 
                }.thenBy { 
                    // 然后按描述文本排序
                    if (currentMode == FeatureMode.APP) {
                        AppFeatureMappings.getLocalizedDescription(app, it.features.first().name)
                    } else {
                        OplusFeatureMappings.getLocalizedDescription(app, it.features.first().name)
                    }
                }
            )
        }
    }
    
    val context = LocalContext.current

    var groupToDelete by remember { mutableStateOf<FeatureGroup?>(null) }
    var featureToEdit by remember { mutableStateOf<Pair<AppFeature, String>?>(null) } // feature and description
    var chooseFromGroup by remember { mutableStateOf<FeatureGroup?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(true) }
    
    // 复杂特性对话框状态
    var showComplexFeatureDialog by remember { mutableStateOf(false) }
    var complexFeatureConfigPath by remember { mutableStateOf("") }
    
    // 搜索相关状态
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 当搜索栏展开时拦截系统返回键，先关闭搜索栏而非退出界面
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    // 根据搜索查询过滤特性组
    val displayedFeatureGroups = remember(featureGroups, searchQuery, currentMode) {
        if (searchQuery.isBlank()) {
            featureGroups
        } else {
            SearchLogic.filterFeatureGroups(featureGroups, searchQuery, context, currentMode)
        }
    }

    // 检测滚动方向的 NestedScrollConnection - 隐藏/显示浮动按钮
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

    // 首次进入或配置路径变化时，自动执行一次合并再加载，避免出现空列表
    LaunchedEffect(configPath) {
        // 先确保系统配置已复制并完成合并
        ConfigMergeManager.performConfigMerge()
        // 再加载特性
        features = repository.loadFeatures(configPath)

        // 加载补丁状态
        if (features.isNotEmpty()) {
            val featureNames = features.map { it.name }
            patchActions = ConfigMergeManager.getFeaturesPatchActions(
                featureNames,
                currentMode == FeatureMode.APP
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                ColorOSTopAppBar(
                    title = stringResource(id = R.string.app_title),
                    currentMode = currentMode,
                    onModeChange = onModeChange,
                    isSearchActive = isSearchActive,
                    onSearchClick = {
                        if (isSearchActive) {
                            searchQuery = ""
                        }
                        isSearchActive = !isSearchActive
                    },
                    onRefresh = {
                        scope.launch {
                            // 重新合并配置以确保最新复制的系统配置被处理
                            ConfigMergeManager.performConfigMerge()
                            // 重新加载特性列表
                            val updated = repository.loadFeatures(configPath)
                            // 强制触发重组，更新UI
                            features = emptyList()
                            features = updated
                            refreshTrigger++
                        }
                    }
                )
                
                // 搜索栏
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClearQuery = { searchQuery = "" },
                    isVisible = isSearchActive
                )
            }
        },
        // 仅添加按钮浮动
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add_feature)
                    )
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
            items(displayedFeatureGroups, key = {
                // 使用与分组相同的逻辑生成key
                val firstFeature = it.features.first()
                val description = if (currentMode == FeatureMode.APP) {
                    AppFeatureMappings.getLocalizedDescription(context, firstFeature.name)
                } else {
                    OplusFeatureMappings.getLocalizedDescription(context, firstFeature.name)
                }
                
                if (it.nameResId == R.string.feature_unknown && 
                    description == firstFeature.name) {
                    "unknown_${firstFeature.name}_$refreshTrigger"
                } else {
                    "desc_${description}_$refreshTrigger"
                }
            }) { group ->
                FeatureGroupItem(
                    currentMode = currentMode,
                    group = group,
                    searchQuery = searchQuery,
                    patchAction = patchActions[group.features.first().name],
                    onToggle = { updatedGroup ->
                        // 更新组内所有特性的状态
                        val updatedFeatures = features.map { feature ->
                            val shouldUpdate = updatedGroup.features.any { it.name == feature.name }
                            if (shouldUpdate) feature.copy(enabled = updatedGroup.isEnabled) else feature
                        }
                        // 强制触发重组，确保UI立即更新
                        features = emptyList()
                        features = updatedFeatures

                        // 异步保存并重新加载
                        scope.launch {
                            repository.saveFeatures(configPath, updatedFeatures)

                            // 重新加载配置文件以获取最新的合并结果
                            val reloadedFeatures = repository.loadFeatures(configPath)
                            features = reloadedFeatures

                            // 更新补丁状态
                            if (reloadedFeatures.isNotEmpty()) {
                                val featureNames = reloadedFeatures.map { it.name }
                                patchActions = ConfigMergeManager.getFeaturesPatchActions(
                                    featureNames,
                                    currentMode == FeatureMode.APP
                                )
                            }

                            // 触发UI刷新
                            refreshTrigger++
                        }
                    },
                    onLongPress = {
                        // 仅允许删除用户新增的特性
                        val firstName = group.features.first().name
                        val action = patchActions[firstName]
                        Log.d("FeatureDelete", "LongPress: name=$firstName, mode=$currentMode, action=$action")

                        if (currentMode == FeatureMode.OPLUS) {
                            if (action == ConfigMergeManager.PatchAction.ADD) {
                                Log.d("FeatureDelete", "User added feature, show delete dialog")
                                groupToDelete = group
                            } else {
                                Log.d("FeatureDelete", "System feature, skip delete dialog")
                                // 系统已有特性：不允许删除且不做提示
                            }
                        } else {
                            Log.d("FeatureDelete", "APP mode, allow delete dialog")
                            groupToDelete = group
                        }
                    },
                    onClick = {
                        if (group.features.size == 1) {
                            // 检查是否为复杂特性
                            val feature = group.features.first()
                            featureToEdit = feature to if (currentMode == FeatureMode.APP) {
                                AppFeatureMappings.getLocalizedDescription(context, feature.name)
                            } else {
                                OplusFeatureMappings.getLocalizedDescription(context, feature.name)
                            }
                        } else {
                            chooseFromGroup = group
                        }
                    }
                )
            }
            // 显示空结果提示
            if (displayedFeatureGroups.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) 
                            stringResource(R.string.search_no_results) 
                        else 
                            "暂无特性配置",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // 删除重复的空列表提示
        }

        // 添加特性对话框
        if (showAddDialog) {
            AddFeatureDialog(
                onDismiss = { showAddDialog = false },
                context = context,
                currentMode = currentMode,
                onConfirm = { name, description, enabled, args ->
                    // 添加新特性
                    val newFeature = AppFeature(name, enabled, args)
                    val newList = features + newFeature
                    // 强制触发重组，确保UI立即更新
                    features = emptyList()
                    features = newList

                    // 保存到文件并重新加载
                    scope.launch {
                        repository.saveFeatures(configPath, newList)

                        // 重新加载配置文件以获取最新的合并结果
                        val reloadedFeatures = repository.loadFeatures(configPath)
                        features = reloadedFeatures

                        // 更新补丁状态
                        if (reloadedFeatures.isNotEmpty()) {
                            val featureNames = reloadedFeatures.map { it.name }
                            patchActions = ConfigMergeManager.getFeaturesPatchActions(
                                featureNames,
                                currentMode == FeatureMode.APP
                            )
                        }

                        // 触发UI刷新
                        refreshTrigger++
                    }

                    showAddDialog = false
                }
            )
        }

        // 删除确认对话框
        groupToDelete?.let { deleting ->
            Log.d("FeatureDelete", "Show dialog for group: ${deleting.features.map { it.name }}")
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
                        // 强制触发重组，确保UI立即更新
                        features = emptyList()
                        features = updatedFeatures
                        
                        // 删除用户映射
                        val namesToDelete = deleting.features.map { it.name }
                        AppFeatureMappings.getInstance().removeUserMappings(context, namesToDelete)
                        
                        scope.launch {
                            repository.saveFeatures(configPath, updatedFeatures)

                            // 重新加载配置文件以获取最新的合并结果
                            val reloadedFeatures = repository.loadFeatures(configPath)
                            features = reloadedFeatures

                            // 更新补丁状态
                            if (reloadedFeatures.isNotEmpty()) {
                                val featureNames = reloadedFeatures.map { it.name }
                                patchActions = ConfigMergeManager.getFeaturesPatchActions(
                                    featureNames,
                                    currentMode == FeatureMode.APP
                                )
                            }

                            // 触发UI刷新
                            refreshTrigger++
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

        // 当组内有多特性需要选择编辑对象
        chooseFromGroup?.let { grp ->
            AlertDialog(
                onDismissRequest = { chooseFromGroup = null },
                title = { Text(text = stringResource(id = R.string.select_feature_to_edit)) },
                text = {
                    Column {
                        grp.features.forEach { ft ->
                            TextButton(onClick = {
                                featureToEdit = ft to AppFeatureMappings.getLocalizedDescription(context, ft.name)
                                chooseFromGroup = null
                            }) {
                                Text(text = ft.name)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { chooseFromGroup = null }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }

        // 编辑特性对话框
        featureToEdit?.let { (ft, desc) ->
            EditFeatureDialog(
                onDismiss = { featureToEdit = null },
                context = context,
                originalName = ft.name,
                originalDescription = if (desc == ft.name) "" else desc,
                originalEnabled = ft.enabled,
                originalArgs = ft.args,
                originalSubNodes = ft.subNodes,
                currentMode = currentMode,
                onConfirm = { newName, newDesc, newEnabled, newArgs ->
                    // 更新feature list
                    val updatedFeatures = features.map {
                        if (it.name == ft.name) {
                            // 如果是复杂特性，保留原有子节点和参数
                            if (it.isComplex) {
                                it.copy(name = newName, enabled = newEnabled)
                            } else {
                                it.copy(name = newName, enabled = newEnabled, args = newArgs)
                            }
                        } else it
                    }
                    features = updatedFeatures.toList()

                    scope.launch {
                        repository.saveFeatures(configPath, updatedFeatures)

                        // 重新加载配置文件以获取最新的合并结果
                        val reloadedFeatures = repository.loadFeatures(configPath)
                        features = reloadedFeatures

                        // 更新补丁状态
                        if (reloadedFeatures.isNotEmpty()) {
                            val featureNames = reloadedFeatures.map { it.name }
                            patchActions = ConfigMergeManager.getFeaturesPatchActions(
                                featureNames,
                                currentMode == FeatureMode.APP
                            )
                        }

                        // 触发UI刷新
                        refreshTrigger++
                    }

                    featureToEdit = null
                }
            )
        }

        // 复杂特性提示对话框
        if (showComplexFeatureDialog) {
            AlertDialog(
                onDismissRequest = { showComplexFeatureDialog = false },
                title = { Text(text = stringResource(id = R.string.complex_feature_title)) },
                text = { 
                    Text(text = stringResource(id = R.string.complex_feature_message, complexFeatureConfigPath))
                },
                confirmButton = {
                    TextButton(onClick = { showComplexFeatureDialog = false }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun FeatureGroupItem(
    currentMode: FeatureMode,
    group: FeatureGroup,
    onToggle: (FeatureGroup) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    searchQuery: String = "",
    patchAction: ConfigMergeManager.PatchAction? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = PatchColors.getCardColors(patchAction),
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
            // 获取特性描述
            val description = if (currentMode == FeatureMode.APP) {
                AppFeatureMappings.getLocalizedDescription(app, group.features.first().name)
            } else {
                OplusFeatureMappings.getLocalizedDescription(app, group.features.first().name)
            }
            
            // 如果组内有多个项，显示计数
            val displayText = if (group.features.size > 1) {
                "$description (${group.features.size})"
            } else {
                description
            }
            
            // 特性名称
            val featureName = group.features.first().name

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 描述使用正常字体
                HighlightedText(
                    text = displayText,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodyLarge,
                )
                
                // 名称使用淡色小字体，仅当不是分组卡片时显示
                if (group.features.size == 1) {
                    Text(
                        text = featureName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // 补丁状态指示器
            patchAction?.let { action ->
                Text(
                    text = PatchColors.getPatchActionDescription(action),
                    style = MaterialTheme.typography.labelSmall,
                    color = PatchColors.getIndicatorColor(action) ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            val feature = group.features.first()
            feature.args?.startsWith("boolean:") == true
            val isComplex = feature.isComplex
            val isUnavailable = currentMode == FeatureMode.OPLUS && feature.args == "unavailable"
            val trailingModifier = Modifier.padding(start = 8.dp)
            // 仅当 OPLUS 模式且该特性不是用户新增（PATCH_ACTION != ADD）时显示开关
            if (currentMode == FeatureMode.OPLUS && patchAction != ConfigMergeManager.PatchAction.ADD) {
                Switch(
                    checked = group.isEnabled,
                    onCheckedChange = {
                        onToggle(group.withEnabled(it))
                    },
                    modifier = trailingModifier
                )
            } else if (isComplex) {
                Text(
                    text = stringResource(id = R.string.complex_feature_indicator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = trailingModifier
                )
            } else if (isUnavailable) {
                Text(
                    text = stringResource(id = R.string.unavailable_feature_indicator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = trailingModifier
                )
            }
            // 移除了 args 为 boolean 的配置在列表中显示开关的逻辑
            // args 为 boolean 的配置只在编辑界面显示开关
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureConfigPreview() {
    // 使用示例配置路径或空列表
    FeatureConfigScreen(
        configPath = "",
        currentMode = FeatureMode.APP,
        onModeChange = {},
        repository = XmlFeatureRepository()
    )
} 