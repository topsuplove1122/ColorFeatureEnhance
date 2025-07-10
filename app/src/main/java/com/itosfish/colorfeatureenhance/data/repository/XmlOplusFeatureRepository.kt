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

/**
 * 针对 <oplus-config> 节点、<oplus-feature> 标签的解析/写入实现
 */
class XmlOplusFeatureRepository : FeatureRepository {

    override suspend fun loadFeatures(configPath: String): List<AppFeature> = withContext(Dispatchers.IO) {
        val file = File(configPath)
        if (!file.exists()) return@withContext emptyList()

        val features = parseXmlFeatures(file)
        features
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        val file = File(configPath)
        file.parentFile?.mkdirs()

        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.appendLine("<oplus-config>")

            features.forEach { feature ->
                writeFeature(writer, feature)
            }
            writer.appendLine("</oplus-config>")
        }

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
                                // 使用特殊 args 标记不可用
                                features.add(AppFeature(nameAttr, enabled = true, args = "unavailable"))
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
                AppFeature(first.name, true, first.args, subNodes)
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