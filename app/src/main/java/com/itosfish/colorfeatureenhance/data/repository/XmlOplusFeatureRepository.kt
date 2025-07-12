package com.itosfish.colorfeatureenhance.data.repository

import android.util.Log
import android.util.Xml
import com.itosfish.colorfeatureenhance.config.ConfigMergeManager
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.FeatureSubNode
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.utils.ConfigUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * 针对 <oplus-config> 节点、<oplus-feature> 标签的解析/写入实现
 */
class XmlOplusFeatureRepository : FeatureRepository {

    override suspend fun loadFeatures(configPath: String): List<AppFeature> = withContext(Dispatchers.IO) {
        // 对于oplus特性，我们需要显示系统基线中的所有特性，并根据补丁状态设置enabled状态
        val configPaths = ConfigUtils.getConfigPaths()
        val systemBaselineFile = File(configPaths.systemBaselineDir, configPaths.oplusFeaturesFile)

        if (!systemBaselineFile.exists()) return@withContext emptyList()

        // 加载系统基线特性
        val systemFeatures = parseXmlFeatures(systemBaselineFile)

        // 加载用户补丁
        val patchFile = File(configPaths.userPatchesDir, "oplus-features.patch.json")
        val userPatches = if (patchFile.exists()) {
            ConfigMergeManager.loadOplusFeaturePatches(patchFile)
        } else {
            emptyList()
        }

        // 根据补丁状态设置特性的enabled状态
        val featuresWithPatchStatus = systemFeatures.map { feature ->
            val patch = userPatches.find { it.name == feature.name }
            when (patch?.action) {
                ConfigMergeManager.OplusPatchAction.REMOVE -> feature.copy(enabled = false)
                else -> feature.copy(enabled = true)
            }
        }

        featuresWithPatchStatus
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        // 读取系统基线配置作为原始配置
        val configPaths = ConfigUtils.getConfigPaths()
        val systemBaselineFile = File(configPaths.systemBaselineDir, configPaths.oplusFeaturesFile)
        val originalOplusFeatures = if (systemBaselineFile.exists()) {
            loadOplusFeatures(systemBaselineFile.absolutePath)
        } else {
            emptyList()
        }

        Log.i("XmlOplusFeatureRepository", "开始保存oplus特性配置，原始特性数量: ${originalOplusFeatures.size}, 修改后特性数量: ${features.size}")

        // 将AppFeature转换为OplusFeature
        // oplus开关控制删除：enabled=true保留特性，enabled=false删除特性
        val modifiedOplusFeatures = convertAppFeaturesToOplusFeatures(features.filter { it.enabled })
        Log.i("XmlOplusFeatureRepository", "转换后的oplus特性数量: ${modifiedOplusFeatures.size}")

        // 生成并保存用户补丁
        ConfigMergeManager.saveOplusFeaturePatches(originalOplusFeatures, modifiedOplusFeatures)
        Log.i("XmlOplusFeatureRepository", "用户补丁已保存")

        // 重新执行配置合并
        val mergeSuccess = ConfigMergeManager.performConfigMerge()
        Log.i("XmlOplusFeatureRepository", "配置合并结果: $mergeSuccess")

        // 如果合并成功，复制到模块目录
        if (mergeSuccess) {
            val copySuccess = ConfigUtils.copyMergedConfigToModule()
            Log.i("XmlOplusFeatureRepository", "复制到模块目录结果: $copySuccess")
        } else {
            Log.e("XmlOplusFeatureRepository", "配置合并失败，跳过复制到模块目录")
        }
    }

    /**
     * 加载OplusFeature列表（用于补丁生成）
     */
    private suspend fun loadOplusFeatures(configPath: String): List<ConfigMergeManager.OplusFeature> = withContext(Dispatchers.IO) {
        val file = File(configPath)
        if (!file.exists()) return@withContext emptyList()

        val features = mutableListOf<ConfigMergeManager.OplusFeature>()

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
                            features.add(ConfigMergeManager.OplusFeature.Standard(name))
                        }
                        "unavailable-oplus-feature" -> {
                            val name = parser.getAttributeValue(null, "name") ?: ""
                            features.add(ConfigMergeManager.OplusFeature.Unavailable(name))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("XmlOplusFeatureRepository", "解析 oplus-feature.xml 失败", e)
        }

        return@withContext features
    }

    /**
     * 将AppFeature转换为OplusFeature
     */
    private fun convertAppFeaturesToOplusFeatures(appFeatures: List<AppFeature>): List<ConfigMergeManager.OplusFeature> {
        return appFeatures.map { appFeature ->
            // 对于oplus特性，只保留启用的特性，根据原始类型决定是Standard还是Unavailable
            if (appFeature.args == "unavailable") {
                ConfigMergeManager.OplusFeature.Unavailable(appFeature.name)
            } else {
                ConfigMergeManager.OplusFeature.Standard(appFeature.name)
            }
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
                        "oplus-feature" -> {
                            // 新特性开始
                            val nameAttr = parser.getAttributeValue(null, "name") ?: ""
                            val argsAttr: String? = parser.getAttributeValue(null, "args")
                            
                            val enabled = true // Oplus 特性默认启用
                            
                            if (nameAttr.isNotEmpty()) {
                                currentFeature = AppFeature(nameAttr, enabled, argsAttr)
                                currentSubNodes.clear()
                            }
                        }
                        "unavailable-oplus-feature" -> {
                            val nameAttr = parser.getAttributeValue(null, "name") ?: ""
                            if (nameAttr.isNotEmpty()) {
                                // 解析为未启用的特性
                                features.add(AppFeature(nameAttr, enabled = false, args = "unavailable"))
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
                    if (parser.name == "oplus-feature" && currentFeature != null) {
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
        
        // 去重：同名特性仅保留一项
        return features
            .groupBy { it.name }
            .map { (_, list) ->
                val first = list.first()
                val subNodes = list.flatMap { it.subNodes }.distinctBy { it.args }
                // 只要有一项 enabled=true，则视为启用
                val enabled = list.any { it.enabled }
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
        // unavailable 特性
        if (feature.args == "unavailable") {
            writer.appendLine("\t<unavailable-oplus-feature name=\"${escapeAttr(feature.name)}\"/>")
            return
        }

        if (feature.isSimple) {
            writer.appendLine("\t<oplus-feature name=\"${feature.name}\"${attrArgs(feature.args)}/>")
        } else {
            writer.appendLine("\t<oplus-feature name=\"${feature.name}\"${attrArgs(feature.args)}>")
            feature.subNodes.forEach { subNode ->
                val nameAttr = if (subNode.name.isNullOrBlank()) "" else " name=\"${escapeAttr(subNode.name)}\""
                writer.appendLine("\t\t<${subNode.type}${nameAttr} args=\"${escapeAttr(subNode.args)}\"/>")
            }
            writer.appendLine("\t</oplus-feature>")
        }
    }
} 