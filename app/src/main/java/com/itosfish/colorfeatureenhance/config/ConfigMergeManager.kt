package com.itosfish.colorfeatureenhance.config

import android.util.Log
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.FeatureSubNode
import com.itosfish.colorfeatureenhance.utils.CLog
import com.itosfish.colorfeatureenhance.utils.CSU
import com.itosfish.colorfeatureenhance.utils.ConfigUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * 新架构的配置合并管理器
 * 负责系统基线配置与用户补丁的智能合并
 */
object ConfigMergeManager {
    
    private const val TAG = "ConfigMergeManager"
    
    // 获取配置路径
    private val configPaths = ConfigUtils.getConfigPaths()
    
    // JSON序列化器
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * 执行配置合并（应用启动时调用）
     */
    suspend fun performConfigMerge(): Boolean = withContext(Dispatchers.IO) {
        try {
            CLog.i(TAG, "开始执行配置合并")

            // 检查并复制缺失的系统配置
            if (!ConfigUtils.hasSystemBaseline()) {
                CLog.i(TAG, "system_baseline为空，从系统路径复制配置文件")
                val copySuccess = copySystemConfigsToBaseline()
                if (!copySuccess) {
                    CLog.w(TAG, "系统配置复制失败，继续使用空配置进行合并")
                }
            } else {
                CLog.i(TAG, "system_baseline配置文件已存在，跳过复制")
            }

            // 合并 app-features.xml
            mergeAppFeatures()

            // 合并 oplus-feature.xml
            mergeOplusFeatures()

            CLog.i(TAG, "所有配置文件合并完成")
            return@withContext true
        } catch (e: Exception) {
            CLog.e(TAG, "配置合并失败", e)
            return@withContext false
        }
    }
    
    /**
     * 合并 app-features.xml
     */
    private fun mergeAppFeatures() {
        val systemFile = File(configPaths.systemBaselineDir, configPaths.appFeaturesFile)
        val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
        val outputFile = File(configPaths.mergedOutputDir, configPaths.appFeaturesFile)
        
        // 读取系统基线配置
        val systemFeatures = if (systemFile.exists()) {
            parseAppFeaturesXml(systemFile)
        } else {
            emptyList()
        }
        
        // 读取用户补丁
        val userPatches = if (patchFile.exists()) {
            loadAppFeaturePatches(patchFile)
        } else {
            emptyList()
        }
        
        // 执行合并
        val mergedFeatures = applyAppFeaturePatches(systemFeatures, userPatches)
        
        // 写入合并结果
        writeAppFeaturesXml(outputFile, mergedFeatures)

        CLog.i(TAG, "app-features.xml 合并完成: ${systemFeatures.size} 系统特性 + ${userPatches.size} 用户补丁 = ${mergedFeatures.size} 最终特性")
    }
    
    /**
     * 合并 oplus-feature.xml
     */
    private fun mergeOplusFeatures() {
        val systemFile = File(configPaths.systemBaselineDir, configPaths.oplusFeaturesFile)
        val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")
        val outputFile = File(configPaths.mergedOutputDir, configPaths.oplusFeaturesFile)
        
        // 读取系统基线配置
        val systemFeatures = if (systemFile.exists()) {
            parseOplusFeaturesXml(systemFile)
        } else {
            emptyList()
        }
        
        // 读取用户补丁
        val userPatches = if (patchFile.exists()) {
            loadOplusFeaturePatches(patchFile)
        } else {
            emptyList()
        }
        
        // 执行合并（oplus特性合并逻辑简单）
        val mergedFeatures = applyOplusFeaturePatches(systemFeatures, userPatches)
        
        // 写入合并结果
        writeOplusFeaturesXml(outputFile, mergedFeatures)

        CLog.i(TAG, "oplus-feature.xml 合并完成: ${systemFeatures.size} 系统特性 + ${userPatches.size} 用户补丁 = ${mergedFeatures.size} 最终特性")
    }
    
    /**
     * 保存用户对 app-features 的修改为补丁
     */
    suspend fun saveAppFeaturePatches(originalFeatures: List<AppFeature>, modifiedFeatures: List<AppFeature>) = withContext(Dispatchers.IO) {
        val patches = generateAppFeaturePatches(originalFeatures, modifiedFeatures)
        val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
        
        patchFile.writeText(json.encodeToString(patches))

        CLog.i(TAG, "保存 app-features 补丁: ${patches.size} 个变更")
    }
    
    /**
     * 保存用户对 oplus-features 的修改为补丁
     */
    suspend fun saveOplusFeaturePatches(originalFeatures: List<OplusFeature>, modifiedFeatures: List<OplusFeature>) = withContext(Dispatchers.IO) {
        val patches = generateOplusFeaturePatches(originalFeatures, modifiedFeatures)
        val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")

        patchFile.writeText(json.encodeToString(patches))

        CLog.i(TAG, "保存 oplus-features 补丁: ${patches.size} 个变更")
    }

    /**
     * 获取特性的补丁状态
     * @param featureName 特性名称
     * @param isAppFeature 是否为app-features模式
     * @return 补丁动作类型，如果没有补丁则返回null
     */
    suspend fun getFeaturePatchAction(featureName: String, isAppFeature: Boolean): PatchAction? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (isAppFeature) {
                val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
                if (patchFile.exists()) {
                    val patches = loadAppFeaturePatches(patchFile)
                    patches.find { it.name == featureName }?.action
                } else {
                    null
                }
            } else {
                val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")
                if (patchFile.exists()) {
                    val patches = loadOplusFeaturePatches(patchFile)
                    val patch = patches.find { it.name == featureName }
                    // 将OplusPatchAction转换为PatchAction
                    when (patch?.action) {
                        OplusPatchAction.ENABLE -> {
                            // 检查是否为新增特性：在系统基线中不存在
                            if (isFeatureInSystemBaseline(featureName, false)) {
                                PatchAction.MODIFY // 系统基线中存在，是修改
                            } else {
                                PatchAction.ADD // 系统基线中不存在，是新增
                            }
                        }
                        OplusPatchAction.DISABLE -> {
                            // DISABLE总是修改操作（将Standard改为Unavailable）
                            PatchAction.MODIFY
                        }
                        OplusPatchAction.REMOVE -> PatchAction.REMOVE // REMOVE状态显示为删除标识
                        null -> null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            CLog.e(TAG, "获取特性补丁状态失败: $featureName", e)
            null
        }
    }

    /**
     * 检查特性是否存在于系统基线配置中
     * @param featureName 特性名称
     * @param isAppFeature 是否为app-features模式
     * @return true表示存在于系统基线中
     */
    private suspend fun isFeatureInSystemBaseline(featureName: String, isAppFeature: Boolean): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (isAppFeature) {
                val systemFile = File(configPaths.systemBaselineDir, configPaths.appFeaturesFile)
                if (systemFile.exists()) {
                    val systemFeatures = parseAppFeaturesXml(systemFile)
                    systemFeatures.any { it.name == featureName }
                } else {
                    false
                }
            } else {
                val systemFile = File(configPaths.systemBaselineDir, configPaths.oplusFeaturesFile)
                if (systemFile.exists()) {
                    val systemFeatures = parseOplusFeaturesXml(systemFile)
                    systemFeatures.any { it.name == featureName }
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            CLog.e(TAG, "检查系统基线特性失败: $featureName", e)
            false
        }
    }

    /**
     * 批量获取多个特性的补丁状态
     * @param featureNames 特性名称列表
     * @param isAppFeature 是否为app-features模式
     * @return 特性名称到补丁动作的映射
     */
    suspend fun getFeaturesPatchActions(featureNames: List<String>, isAppFeature: Boolean): Map<String, PatchAction> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (isAppFeature) {
                val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
                if (patchFile.exists()) {
                    val patches = loadAppFeaturePatches(patchFile)
                    patches.mapNotNull { patch ->
                        if (patch.name in featureNames) {
                            // 对于app-features，直接使用补丁中的action
                            patch.name to patch.action
                        } else {
                            null
                        }
                    }.toMap()
                } else {
                    emptyMap()
                }
            } else {
                val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")
                if (patchFile.exists()) {
                    val patches = loadOplusFeaturePatches(patchFile)
                    patches.mapNotNull { patch ->
                        if (patch.name in featureNames) {
                            val action = when (patch.action) {
                                OplusPatchAction.ENABLE -> {
                                    // 检查是否为新增特性：在系统基线中不存在
                                    if (isFeatureInSystemBaseline(patch.name, false)) {
                                        PatchAction.MODIFY // 系统基线中存在，是修改
                                    } else {
                                        PatchAction.ADD // 系统基线中不存在，是新增
                                    }
                                }
                                OplusPatchAction.DISABLE -> {
                                    // DISABLE总是修改操作（将Standard改为Unavailable）
                                    PatchAction.MODIFY
                                }
                                OplusPatchAction.REMOVE -> PatchAction.REMOVE // REMOVE状态返回删除标识
                            }
                            patch.name to action
                        } else {
                            null
                        }
                    }.toMap()
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            CLog.e(TAG, "批量获取特性补丁状态失败", e)
            emptyMap()
        }
    }

    /**
     * 从系统路径复制配置文件到system_baseline目录
     * 用于首次启动时初始化配置
     */
    private fun copySystemConfigsToBaseline(): Boolean {
        try {
            Log.i(TAG, "开始从系统路径复制配置文件到system_baseline")

            val systemConfigDir = "/my_product/etc/extension"
            val appFeaturesFile = configPaths.appFeaturesFile
            val oplusFeaturesFile = configPaths.oplusFeaturesFile

            var successCount = 0
            var totalFiles = 0

            // 复制 app-features.xml
            val appFeaturesSystemPath = "$systemConfigDir/$appFeaturesFile"
            val appFeaturesTargetPath = "${configPaths.systemBaselineDir}/$appFeaturesFile"

            CLog.d(TAG, "检查系统文件: $appFeaturesSystemPath")
            if (CSU.fileExists(appFeaturesSystemPath)) {
                CLog.d(TAG, "系统文件存在，开始复制: $appFeaturesFile")
                val copyCmd = "cp \"$appFeaturesSystemPath\" \"$appFeaturesTargetPath\""
                val result = CSU.runWithSu(copyCmd).output

                if (CSU.fileExists(appFeaturesTargetPath)) {
                    CLog.i(TAG, "成功复制系统配置: $appFeaturesFile")
                    successCount++
                } else {
                    CLog.w(TAG, "复制失败: $appFeaturesFile (命令输出: $result)")
                }
                totalFiles++
            } else {
                CLog.w(TAG, "系统配置文件不存在: $appFeaturesSystemPath")
            }

            // 复制 oplus-feature.xml
            val oplusFeaturesSystemPath = "$systemConfigDir/$oplusFeaturesFile"
            val oplusFeaturesTargetPath = "${configPaths.systemBaselineDir}/$oplusFeaturesFile"

            CLog.d(TAG, "检查系统文件: $oplusFeaturesSystemPath")
            if (CSU.fileExists(oplusFeaturesSystemPath)) {
                CLog.d(TAG, "系统文件存在，开始复制: $oplusFeaturesFile")
                val copyCmd = "cp \"$oplusFeaturesSystemPath\" \"$oplusFeaturesTargetPath\""
                val result = CSU.runWithSu(copyCmd).output

                if (CSU.fileExists(oplusFeaturesTargetPath)) {
                    CLog.i(TAG, "成功复制系统配置: $oplusFeaturesFile")
                    successCount++
                } else {
                    CLog.w(TAG, "复制失败: $oplusFeaturesFile (命令输出: $result)")
                }
                totalFiles++
            } else {
                CLog.w(TAG, "系统配置文件不存在: $oplusFeaturesSystemPath")
            }

            // 总结复制结果
            val success = successCount > 0
            if (success) {
                CLog.i(TAG, "系统配置复制完成: $successCount/$totalFiles 个文件复制成功")
            } else {
                CLog.w(TAG, "系统配置复制失败: 没有成功复制任何文件 (总共检查了 $totalFiles 个文件)")
            }

            return success

        } catch (e: Exception) {
            CLog.e(TAG, "复制系统配置时发生异常", e)
            return false
        }
    }

    // ========== 数据模型 ==========
    
    @Serializable
    data class AppFeaturePatch(
        val name: String,
        val action: PatchAction,
        val enabled: Boolean = true,
        val args: String? = null,
        val subNodes: List<FeatureSubNode> = emptyList()
    )
    
    @Serializable
    data class OplusFeaturePatch(
        val name: String,
        val action: OplusPatchAction
    )
    
    @Serializable
    enum class PatchAction {
        ADD, MODIFY, REMOVE
    }
    
    @Serializable
    enum class OplusPatchAction {
        ENABLE,    // 转为 <oplus-feature>
        DISABLE,   // 转为 <unavailable-oplus-feature>
        REMOVE     // 完全移除
    }
    
    @Serializable
    sealed class OplusFeature {
        abstract val name: String
        
        @Serializable
        data class Standard(override val name: String) : OplusFeature()
        
        @Serializable
        data class Unavailable(override val name: String) : OplusFeature()
    }
    
    // ========== XML解析方法 ==========
    
    /**
     * 解析 app-features.xml 文件
     */
    private fun parseAppFeaturesXml(file: File): List<AppFeature> {
        val features = mutableListOf<AppFeature>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(file.readText()))
            
            var eventType = parser.eventType
            var currentFeature: AppFeature? = null
            val currentSubNodes = mutableListOf<FeatureSubNode>()
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "app_feature" -> {
                                val name = parser.getAttributeValue(null, "name") ?: ""
                                val args = parser.getAttributeValue(null, "args")
                                currentFeature = AppFeature(name = name, args = args)
                                currentSubNodes.clear()
                            }
                            "StringList" -> {
                                val subNodeName = parser.getAttributeValue(null, "name")
                                val subNodeArgs = parser.getAttributeValue(null, "args") ?: ""
                                currentSubNodes.add(FeatureSubNode("StringList", subNodeName, subNodeArgs))
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "app_feature" && currentFeature != null) {
                            features.add(currentFeature.copy(subNodes = currentSubNodes.toList()))
                            currentFeature = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析 app-features.xml 失败", e)
        }
        
        return features
    }
    
    /**
     * 解析 oplus-feature.xml 文件
     */
    private fun parseOplusFeaturesXml(file: File): List<OplusFeature> {
        val features = mutableListOf<OplusFeature>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(file.readText()))
            
            var eventType = parser.eventType
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "oplus-feature" -> {
                            val name = parser.getAttributeValue(null, "name") ?: ""
                            features.add(OplusFeature.Standard(name))
                        }
                        "unavailable-oplus-feature" -> {
                            val name = parser.getAttributeValue(null, "name") ?: ""
                            features.add(OplusFeature.Unavailable(name))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            CLog.e(TAG, "解析 oplus-feature.xml 失败", e)
        }
        
        return features
    }
    
    // ========== 私有辅助方法（待实现） ==========
    
    fun loadAppFeaturePatches(file: File): List<AppFeaturePatch> {
        return try {
            val content = file.readText()
            json.decodeFromString<List<AppFeaturePatch>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "加载 app-features 补丁失败", e)
            emptyList()
        }
    }

    fun loadOplusFeaturePatches(file: File): List<OplusFeaturePatch> {
        return try {
            val content = file.readText()
            json.decodeFromString<List<OplusFeaturePatch>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "加载 oplus-features 补丁失败", e)
            emptyList()
        }
    }
    
    private fun applyAppFeaturePatches(systemFeatures: List<AppFeature>, patches: List<AppFeaturePatch>): List<AppFeature> {
        val result = systemFeatures.toMutableList()

        patches.forEach { patch ->
            when (patch.action) {
                PatchAction.ADD -> {
                    // 添加新特性（如果不存在）
                    if (result.none { it.name == patch.name }) {
                        result.add(AppFeature(
                            name = patch.name,
                            enabled = patch.enabled,
                            args = patch.args,
                            subNodes = patch.subNodes
                        ))
                    }
                }
                PatchAction.MODIFY -> {
                    // 修改现有特性
                    val index = result.indexOfFirst { it.name == patch.name }
                    if (index >= 0) {
                        result[index] = AppFeature(
                            name = patch.name,
                            enabled = patch.enabled,
                            args = patch.args,
                            subNodes = patch.subNodes
                        )
                    }
                }
                PatchAction.REMOVE -> {
                    // 移除特性
                    result.removeAll { it.name == patch.name }
                }
            }
        }

        return result
    }

    fun applyOplusFeaturePatches(systemFeatures: List<OplusFeature>, patches: List<OplusFeaturePatch>): List<OplusFeature> {
        val result = systemFeatures.toMutableList()

        patches.forEach { patch ->
            when (patch.action) {
                OplusPatchAction.ENABLE -> {
                    // 移除同名的所有特性，添加启用特性
                    result.removeAll { it.name == patch.name }
                    result.add(OplusFeature.Standard(patch.name))
                }
                OplusPatchAction.DISABLE -> {
                    // 移除同名的所有特性，添加禁用特性
                    result.removeAll { it.name == patch.name }
                    result.add(OplusFeature.Unavailable(patch.name))
                }
                OplusPatchAction.REMOVE -> {
                    // 完全移除该特性
                    result.removeAll { it.name == patch.name }
                }
            }
        }

        return result
    }
    
    private fun writeAppFeaturesXml(file: File, features: List<AppFeature>) {
        try {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                writer.appendLine("<extend_features>")

                features.forEach { feature ->
                    if (feature.subNodes.isEmpty()) {
                        // 简单特性
                        if (feature.args.isNullOrEmpty()) {
                            writer.appendLine("\t<app_feature name=\"${escapeXml(feature.name)}\"/>")
                        } else {
                            writer.appendLine("\t<app_feature name=\"${escapeXml(feature.name)}\" args=\"${escapeXml(feature.args)}\"/>")
                        }
                    } else {
                        // 复杂特性
                        if (feature.args.isNullOrEmpty()) {
                            writer.appendLine("\t<app_feature name=\"${escapeXml(feature.name)}\">")
                        } else {
                            writer.appendLine("\t<app_feature name=\"${escapeXml(feature.name)}\" args=\"${escapeXml(feature.args)}\">")
                        }

                        feature.subNodes.forEach { subNode ->
                            if (subNode.name.isNullOrEmpty()) {
                                writer.appendLine("\t\t<${subNode.type} args=\"${escapeXml(subNode.args)}\"/>")
                            } else {
                                writer.appendLine("\t\t<${subNode.type} name=\"${escapeXml(subNode.name)}\" args=\"${escapeXml(subNode.args)}\"/>")
                            }
                        }

                        writer.appendLine("\t</app_feature>")
                    }
                }

                writer.appendLine("</extend_features>")
                writer.appendLine("<!-- Generated by ConfigMergeManager at ${System.currentTimeMillis()} -->")
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入 app-features.xml 失败", e)
        }
    }

    private fun writeOplusFeaturesXml(file: File, features: List<OplusFeature>) {
        try {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                writer.appendLine("<oplus-config>")

                features.forEach { feature ->
                    when (feature) {
                        is OplusFeature.Standard -> {
                            writer.appendLine("\t<oplus-feature name=\"${escapeXml(feature.name)}\"/>")
                        }
                        is OplusFeature.Unavailable -> {
                            writer.appendLine("\t<unavailable-oplus-feature name=\"${escapeXml(feature.name)}\"/>")
                        }
                    }
                }

                writer.appendLine("</oplus-config>")
                writer.appendLine("<!-- Generated by ConfigMergeManager at ${System.currentTimeMillis()} -->")
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入 oplus-feature.xml 失败", e)
        }
    }

    /**
     * XML转义
     */
    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
    
    private fun generateAppFeaturePatches(original: List<AppFeature>, modified: List<AppFeature>): List<AppFeaturePatch> {
        val patches = mutableListOf<AppFeaturePatch>()

        // 检查修改和新增
        modified.forEach { modifiedFeature ->
            val originalFeature = original.find { it.name == modifiedFeature.name }

            if (originalFeature == null) {
                // 新增特性
                patches.add(AppFeaturePatch(
                    name = modifiedFeature.name,
                    action = PatchAction.ADD,
                    enabled = modifiedFeature.enabled,
                    args = modifiedFeature.args,
                    subNodes = modifiedFeature.subNodes
                ))
            } else {
                // 检查是否真的有变化 - 特别处理boolean参数
                val hasChanged = if (modifiedFeature.args?.startsWith("boolean:") == true &&
                                    originalFeature.args?.startsWith("boolean:") == true) {
                    // 对于boolean参数，比较enabled状态而不是args字符串
                    originalFeature.enabled != modifiedFeature.enabled ||
                    originalFeature.name != modifiedFeature.name ||
                    originalFeature.subNodes != modifiedFeature.subNodes
                } else {
                    // 对于非boolean参数，使用标准比较
                    originalFeature != modifiedFeature
                }

                if (hasChanged) {
                    // 修改特性 - 关键修复：正确处理布尔值逻辑
                    val finalArgs = if (modifiedFeature.args?.startsWith("boolean:") == true) {
                        // 对于布尔类型，根据enabled状态生成正确的args
                        "boolean:${modifiedFeature.enabled}"
                    } else {
                        // 对于非布尔类型，直接使用args
                        modifiedFeature.args
                    }

                    patches.add(AppFeaturePatch(
                        name = modifiedFeature.name,
                        action = PatchAction.MODIFY,
                        enabled = modifiedFeature.enabled,
                        args = finalArgs,
                        subNodes = modifiedFeature.subNodes
                    ))
                }
            }
        }

        // 检查删除
        original.forEach { originalFeature ->
            if (modified.none { it.name == originalFeature.name }) {
                patches.add(AppFeaturePatch(
                    name = originalFeature.name,
                    action = PatchAction.REMOVE
                ))
            }
        }

        return patches
    }

    private fun generateOplusFeaturePatches(original: List<OplusFeature>, modified: List<OplusFeature>): List<OplusFeaturePatch> {
        val patches = mutableListOf<OplusFeaturePatch>()

        // 检测可能的name修改：如果数量相同且只有一个name变化，视为修改而非删除+新增
        val nameChanges = detectOplusFeatureNameChanges(original, modified)

        // 检查修改和新增
        modified.forEach { modifiedFeature ->
            val originalFeature = original.find { it.name == modifiedFeature.name }

            // 检查是否是name修改的目标特性
            val isNameChangeTarget = nameChanges.values.contains(modifiedFeature.name)

            if (originalFeature == null && !isNameChangeTarget) {
                // 真正的新增特性（不是name修改的结果）
                val action = when (modifiedFeature) {
                    is OplusFeature.Standard -> OplusPatchAction.ENABLE
                    is OplusFeature.Unavailable -> OplusPatchAction.DISABLE
                }
                patches.add(OplusFeaturePatch(modifiedFeature.name, action))
            } else if (originalFeature != null && originalFeature::class != modifiedFeature::class) {
                // 类型变更（Standard ↔ Unavailable）
                val action = when (modifiedFeature) {
                    is OplusFeature.Standard -> OplusPatchAction.ENABLE
                    is OplusFeature.Unavailable -> OplusPatchAction.DISABLE
                }
                patches.add(OplusFeaturePatch(modifiedFeature.name, action))
            }
        }

        // 处理name修改：为新name生成MODIFY补丁
        nameChanges.forEach { (_, newName) ->
            val modifiedFeature = modified.find { it.name == newName }
            if (modifiedFeature != null) {
                val action = when (modifiedFeature) {
                    is OplusFeature.Standard -> OplusPatchAction.ENABLE
                    is OplusFeature.Unavailable -> OplusPatchAction.DISABLE
                }
                patches.add(OplusFeaturePatch(newName, action))
            }
        }

        // 检查删除（排除name修改的情况）
        original.forEach { originalFeature ->
            val isNameChangeSource = nameChanges.containsKey(originalFeature.name)
            if (!isNameChangeSource && modified.none { it.name == originalFeature.name }) {
                patches.add(OplusFeaturePatch(
                    name = originalFeature.name,
                    action = OplusPatchAction.REMOVE
                ))
            }
        }

        return patches
    }

    /**
     * 检测oplus特性的name修改
     * 如果数量相同且只有一个name变化，视为修改而非删除+新增
     */
    private fun detectOplusFeatureNameChanges(original: List<OplusFeature>, modified: List<OplusFeature>): Map<String, String> {
        // 只有在数量相同时才考虑name修改
        if (original.size != modified.size) return emptyMap()

        val originalNames = original.map { it.name }.toSet()
        val modifiedNames = modified.map { it.name }.toSet()

        val removedNames = originalNames - modifiedNames
        val addedNames = modifiedNames - originalNames

        // 只有当恰好有一个name被移除和一个name被添加时，才视为name修改
        if (removedNames.size == 1 && addedNames.size == 1) {
            val oldName = removedNames.first()
            val newName = addedNames.first()

            // 确保类型匹配（Standard对Standard，Unavailable对Unavailable）
            val originalFeature = original.find { it.name == oldName }
            val modifiedFeature = modified.find { it.name == newName }

            if (originalFeature != null && modifiedFeature != null &&
                originalFeature::class == modifiedFeature::class) {
                return mapOf(oldName to newName)
            }
        }

        return emptyMap()
    }
}
