package com.itosfish.colorfeatureenhance.data.repository

import android.util.Xml
import com.itosfish.colorfeatureenhance.data.model.AppFeature
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

        val inputStream = file.inputStream()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        val features = mutableListOf<AppFeature>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "app_feature") {
                val nameAttr = parser.getAttributeValue(null, "name") ?: ""
                val argsAttr: String? = parser.getAttributeValue(null, "args")

                val enabled = if (argsAttr != null && argsAttr.startsWith("boolean:")) {
                    argsAttr.substringAfter("boolean:").equals("true", true)
                } else {
                    true // 无布尔 args 时默认视为启用
                }
                if (nameAttr.isNotEmpty()) {
                    features.add(AppFeature(nameAttr, enabled, argsAttr))
                }
            }
            eventType = parser.next()
        }
        inputStream.close()
        // 去重：同名特性仅保留一项，启用状态以至少一个 true 为准
        features
            .groupBy { it.name }
            .map { (name, list) ->
                val mergedArgs = list.first().args
                AppFeature(name, list.any { it.enabled }, mergedArgs)
            }
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        val file = File(configPath)
        // 确保父目录存在
        file.parentFile?.mkdirs()

        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.appendLine("<extend_features>")

            features.forEach { feature ->
                val argsString = feature.args
                if (argsString.isNullOrBlank()) {
                    writer.appendLine("    <app_feature name=\"${feature.name}\"/>")
                } else if (argsString.startsWith("boolean:")) {
                    val boolValue = if (feature.enabled) "true" else "false"
                    writer.appendLine("    <app_feature name=\"${feature.name}\" args=\"boolean:$boolValue\"/>")
                } else {
                    writer.appendLine("    <app_feature name=\"${feature.name}\" args=\"${argsString}\"/>")
                }
            }

            writer.appendLine("</extend_features>")
        }

        // 将更新后的文件复制到模块目录
        ConfigUtils.copyConfigToModule()
    }

    // 已合并到上方逻辑
} 