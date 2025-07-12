package com.itosfish.colorfeatureenhance.data.repository

import android.util.Xml
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.FeatureSubNode
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import android.util.Log
import com.itosfish.colorfeatureenhance.config.ConfigMergeManager
import com.itosfish.colorfeatureenhance.utils.ConfigUtils

class XmlFeatureRepository : FeatureRepository {

    /**
     * 加载 app-feature 列表。
     * 新增能力：当某个特性处于 "删除" 补丁状态 (PatchAction.REMOVE) 时，依旧在列表中显示，
     *           但标记为已删除 (enabled = false)。
     */
    override suspend fun loadFeatures(configPath: String): List<AppFeature> = withContext(Dispatchers.IO) {
        val configPaths = ConfigUtils.getConfigPaths()

        // 1. 已合并后的配置（不包含被 REMOVE 的特性）
        val mergedFile = File(configPath)
        val mergedFeatures = if (mergedFile.exists()) {
            parseXmlFeatures(mergedFile)
        } else emptyList()

        // 2. 系统基线配置，用于恢复被删除特性的原始信息
        val systemBaselineFile = File(configPaths.systemBaselineDir, configPaths.appFeaturesFile)
        val systemBaselineFeatures = if (systemBaselineFile.exists()) {
            parseXmlFeatures(systemBaselineFile)
        } else emptyList()

        // 3. 用户补丁，检查 REMOVE 项
        val patchFile = File(configPaths.userPatchesDir, "app-features.patch.json")
        val removedPatches = if (patchFile.exists()) {
            ConfigMergeManager.loadAppFeaturePatches(patchFile)
                .filter { it.action == ConfigMergeManager.PatchAction.REMOVE }
        } else emptyList()

        // 4. 将被删除的特性重新加入展示列表（标记为 disabled）
        val finalFeatures = mergedFeatures.toMutableList()
        removedPatches.forEach { patch ->
            // 仅当系统基线存在该特性时，才显示删除标记
            val original = systemBaselineFeatures.find { it.name == patch.name }
            original?.let { orig ->
                // 若列表中已存在同名项（理论上不会），先去重
                finalFeatures.removeAll { it.name == orig.name }

                // 添加禁用状态的特性用于 UI 展示
                val disabledFeature = orig.copy(enabled = false)
                finalFeatures.add(disabledFeature)
            }
        }

        // 去重：同名特性仅保留一项，启用状态以至少一个 true 为准
        return@withContext finalFeatures
            .groupBy { it.name }
            .map { (_, list) ->
                val first = list.first()
                val enabled = list.any { it.enabled }
                val subNodes = list.flatMap { it.subNodes }.distinctBy { it.args }
                AppFeature(first.name, enabled, first.args, subNodes)
            }
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        // 读取系统基线配置作为原始配置
        val configPaths = ConfigUtils.getConfigPaths()
        val systemBaselineFile = File(configPaths.systemBaselineDir, configPaths.appFeaturesFile)
        val originalFeatures = if (systemBaselineFile.exists()) {
            loadFeatures(systemBaselineFile.absolutePath)
        } else {
            emptyList()
        }

        Log.i("XmlFeatureRepository", "开始保存特性配置，原始特性数量: ${originalFeatures.size}, 修改后特性数量: ${features.size}")

        // 生成并保存用户补丁
        ConfigMergeManager.saveAppFeaturePatches(originalFeatures, features)
        Log.i("XmlFeatureRepository", "用户补丁已保存")

        // 重新执行配置合并
        val mergeSuccess = ConfigMergeManager.performConfigMerge()
        Log.i("XmlFeatureRepository", "配置合并结果: $mergeSuccess")

        // 如果合并成功，复制到模块目录
        if (mergeSuccess) {
            val copySuccess = ConfigUtils.copyMergedConfigToModule()
            Log.i("XmlFeatureRepository", "复制到模块目录结果: $copySuccess")
        } else {
            Log.e("XmlFeatureRepository", "配置合并失败，跳过复制到模块目录")
        }
    }

    /**
     * 解析 XML 文件中的特性列表
     */
    private fun parseXmlFeatures(file: File): List<AppFeature> {
        val inputStream = file.inputStream()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        val features = mutableListOf<AppFeature>()
        
        var currentFeature: AppFeature? = null
        val currentSubNodes = mutableListOf<FeatureSubNode>()
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "app_feature" -> {
                            // 新特性开始
                            val nameAttr = parser.getAttributeValue(null, "name") ?: ""
                            val argsAttr: String? = parser.getAttributeValue(null, "args")
                            
                            val enabled = if (argsAttr != null && argsAttr.startsWith("boolean:")) {
                                argsAttr.substringAfter("boolean:").equals("true", true)
                            } else {
                                true // 无布尔 args 时默认视为启用
                            }
                            
                            if (nameAttr.isNotEmpty()) {
                                currentFeature = AppFeature(nameAttr, enabled, argsAttr)
                                currentSubNodes.clear()
                            }
                        }
                        else -> {
                            // 可能是子节点，如 StringList
                            if (currentFeature != null) {
                                val type = parser.name
                                val nameAttr = parser.getAttributeValue(null, "name")
                                val argsAttr = parser.getAttributeValue(null, "args")
                                
                                if (argsAttr != null) {
                                    currentSubNodes.add(FeatureSubNode(type, nameAttr, argsAttr))
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "app_feature" && currentFeature != null) {
                        // 特性结束，添加到列表
                        if (currentSubNodes.isNotEmpty()) {
                            features.add(currentFeature!!.copy(subNodes = currentSubNodes.toList()))
                        } else {
                            features.add(currentFeature!!)
                        }
                        currentFeature = null
                    }
                }
            }
            eventType = parser.next()
        }
        inputStream.close()
        
        // 去重：同名特性仅保留一项，启用状态以至少一个 true 为准
        return features
            .groupBy { it.name }
            .map { (_, list) ->
                val first = list.first()
                val enabled = list.any { it.enabled }
                val subNodes = list.flatMap { it.subNodes }.distinctBy { it.args }
                AppFeature(first.name, enabled, first.args, subNodes)
            }
    }
    
    private fun escapeAttr(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    /**
     * 将特性写入 XML
     */
    private fun writeFeature(writer: java.io.BufferedWriter, feature: AppFeature) {
        fun attrArgs(args: String?): String {
            if (args.isNullOrBlank()) return ""
            return " args=\"${escapeAttr(args)}\""
        }
        if (feature.isSimple) {
            if (feature.args.isNullOrBlank()) {
                writer.appendLine("\t<app_feature name=\"${feature.name}\"/>")
            } else if (feature.args.startsWith("boolean:")) {
                val boolValue = if (feature.enabled) "true" else "false"
                writer.appendLine("\t<app_feature name=\"${feature.name}\" args=\"boolean:$boolValue\"/>")
            } else {
                writer.appendLine("\t<app_feature name=\"${feature.name}\"${attrArgs(feature.args)}/>")
        }
        } else {
            writer.appendLine("\t<app_feature name=\"${feature.name}\"${attrArgs(feature.args)}>")
            // 写入子节点
            feature.subNodes.forEach { subNode ->
                val nameAttr = if (subNode.name.isNullOrBlank()) "" else " name=\"${escapeAttr(subNode.name)}\""
                writer.appendLine("\t\t<${subNode.type}${nameAttr} args=\"${escapeAttr(subNode.args)}\"/>")
            }
            writer.appendLine("\t</app_feature>")
        }
    }
} 