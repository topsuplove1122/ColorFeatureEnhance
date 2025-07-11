package com.itosfish.colorfeatureenhance.config

import android.util.Log
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.FeatureSubNode
import com.itosfish.colorfeatureenhance.utils.ConfigUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
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
            Log.i(TAG, "开始执行配置合并")
            
            // 合并 app-features.xml
            mergeAppFeatures()
            
            // 合并 oplus-feature.xml
            mergeOplusFeatures()
            
            Log.i(TAG, "所有配置文件合并完成")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "配置合并失败", e)
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
        
        Log.i(TAG, "app-features.xml 合并完成: ${systemFeatures.size} 系统特性 + ${userPatches.size} 用户补丁 = ${mergedFeatures.size} 最终特性")
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
        
        Log.i(TAG, "oplus-feature.xml 合并完成: ${systemFeatures.size} 系统特性 + ${userPatches.size} 用户补丁 = ${mergedFeatures.size} 最终特性")
    }
    
    /**
     * 保存用户对 app-features 的修改为补丁
     */
    suspend fun saveAppFeaturePatches(originalFeatures: List<AppFeature>, modifiedFeatures: List<AppFeature>) = withContext(Dispatchers.IO) {
        val patches = generateAppFeaturePatches(originalFeatures, modifiedFeatures)
        val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
        
        patchFile.writeText(json.encodeToString(patches))
        
        Log.i(TAG, "保存 app-features 补丁: ${patches.size} 个变更")
    }
    
    /**
     * 保存用户对 oplus-features 的修改为补丁
     */
    suspend fun saveOplusFeaturePatches(originalFeatures: List<OplusFeature>, modifiedFeatures: List<OplusFeature>) = withContext(Dispatchers.IO) {
        val patches = generateOplusFeaturePatches(originalFeatures, modifiedFeatures)
        val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")
        
        patchFile.writeText(json.encodeToString(patches))
        
        Log.i(TAG, "保存 oplus-features 补丁: ${patches.size} 个变更")
    }
    
    // ========== 数据模型 ==========
    
    @Serializable
    data class AppFeaturePatch(
        val name: String,
        val action: PatchAction,
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
            Log.e(TAG, "解析 oplus-feature.xml 失败", e)
        }
        
        return features
    }
    
    // ========== 私有辅助方法（待实现） ==========
    
    private fun loadAppFeaturePatches(file: File): List<AppFeaturePatch> {
        return try {
            val content = file.readText()
            json.decodeFromString<List<AppFeaturePatch>>(content)
        } catch (e: Exception) {
            Log.e(TAG, "加载 app-features 补丁失败", e)
            emptyList()
        }
    }

    private fun loadOplusFeaturePatches(file: File): List<OplusFeaturePatch> {
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

    private fun applyOplusFeaturePatches(systemFeatures: List<OplusFeature>, patches: List<OplusFeaturePatch>): List<OplusFeature> {
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
                    args = modifiedFeature.args,
                    subNodes = modifiedFeature.subNodes
                ))
            } else if (originalFeature != modifiedFeature) {
                // 修改特性
                patches.add(AppFeaturePatch(
                    name = modifiedFeature.name,
                    action = PatchAction.MODIFY,
                    args = modifiedFeature.args,
                    subNodes = modifiedFeature.subNodes
                ))
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

        // 检查修改和新增
        modified.forEach { modifiedFeature ->
            val originalFeature = original.find { it.name == modifiedFeature.name }

            if (originalFeature == null) {
                // 新增特性
                val action = when (modifiedFeature) {
                    is OplusFeature.Standard -> OplusPatchAction.ENABLE
                    is OplusFeature.Unavailable -> OplusPatchAction.DISABLE
                }
                patches.add(OplusFeaturePatch(modifiedFeature.name, action))
            } else if (originalFeature::class != modifiedFeature::class) {
                // 类型变更
                val action = when (modifiedFeature) {
                    is OplusFeature.Standard -> OplusPatchAction.ENABLE
                    is OplusFeature.Unavailable -> OplusPatchAction.DISABLE
                }
                patches.add(OplusFeaturePatch(modifiedFeature.name, action))
            }
        }

        // 检查删除
        original.forEach { originalFeature ->
            if (modified.none { it.name == originalFeature.name }) {
                patches.add(OplusFeaturePatch(
                    name = originalFeature.name,
                    action = OplusPatchAction.REMOVE
                ))
            }
        }

        return patches
    }
}
