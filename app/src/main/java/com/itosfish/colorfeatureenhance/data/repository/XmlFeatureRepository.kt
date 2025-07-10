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
                val argsAttr = parser.getAttributeValue(null, "args") ?: ""

                val enabled = parseEnabledFromArgs(argsAttr)
                if (nameAttr.isNotEmpty()) {
                    features.add(AppFeature(nameAttr, enabled))
                }
            }
            eventType = parser.next()
        }
        inputStream.close()
        // 去重：同名特性仅保留一项，启用状态以至少一个 true 为准
        features
            .groupBy { it.name }
            .map { (name, list) ->
                AppFeature(name, list.any { it.enabled })
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
                val boolValue = if (feature.enabled) "true" else "false"
                writer.appendLine("    <app_feature name=\"${feature.name}\" args=\"boolean:$boolValue\"/>")
            }

            writer.appendLine("</extend_features>")
        }

        // 将更新后的文件复制到模块目录
        ConfigUtils.copyConfigToModule()
    }

    /**
     * 根据 args 属性解析是否启用
     * boolean:true -> true
     * boolean:false -> false
     * 其它情况，如果标签存在即认为启用
     */
    private fun parseEnabledFromArgs(args: String): Boolean {
        if (args.startsWith("boolean:")) {
            return args.substringAfter("boolean:").equals("true", ignoreCase = true)
        }
        // 没有明确 boolean 时，只要节点存在就视为已启用
        return true
    }
} 