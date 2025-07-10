package com.itosfish.colorfeatureenhance.data.repository

import android.util.Xml
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.FeatureSubNode
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import com.itosfish.colorfeatureenhance.utils.ConfigUtils

class XmlFeatureRepository : FeatureRepository {

    override suspend fun loadFeatures(configPath: String): List<AppFeature> = withContext(Dispatchers.IO) {
        val file = File(configPath)
        if (!file.exists()) return@withContext emptyList()

        val features = parseXmlFeatures(file)
        // 去重：同名特性仅保留一项，启用状态以至少一个 true 为准
        features
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        val file = File(configPath)
        // 确保父目录存在
        file.parentFile?.mkdirs()

        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.appendLine("<extend_features>")

            features.forEach { feature -> 
                writeFeature(writer, feature)
            }

            writer.appendLine("</extend_features>")
        }

        // 将更新后的文件复制到模块目录
        ConfigUtils.copyConfigToModule()
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
    
    /**
     * 将特性写入 XML
     */
    private fun writeFeature(writer: java.io.BufferedWriter, feature: AppFeature) {
        if (feature.isSimple) {
            // 简单特性
            val argsString = feature.args
            if (argsString.isNullOrBlank()) {
                writer.appendLine("    <app_feature name=\"${feature.name}\"/>")
            } else if (argsString.startsWith("boolean:")) {
                val boolValue = if (feature.enabled) "true" else "false"
                writer.appendLine("    <app_feature name=\"${feature.name}\" args=\"boolean:$boolValue\"/>")
            } else {
                writer.appendLine("    <app_feature name=\"${feature.name}\" args=\"${argsString}\"/>")
            }
        } else {
            // 复杂特性
            val argsString = if (feature.args.isNullOrBlank()) "" else " args=\"${feature.args}\""
            writer.appendLine("    <app_feature name=\"${feature.name}\"${argsString}>")
            
            // 写入子节点
            feature.subNodes.forEach { subNode ->
                val nameAttr = if (subNode.name.isNullOrBlank()) "" else " name=\"${subNode.name}\""
                writer.appendLine("        <${subNode.type}${nameAttr} args=\"${subNode.args}\"/>")
            }
            
            writer.appendLine("    </app_feature>")
        }
    }
} 