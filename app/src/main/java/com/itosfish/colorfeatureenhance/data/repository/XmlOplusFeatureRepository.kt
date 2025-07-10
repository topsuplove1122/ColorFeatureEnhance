package com.itosfish.colorfeatureenhance.data.repository

import android.util.Xml
import com.itosfish.colorfeatureenhance.data.model.AppFeature
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

        val inputStream = file.inputStream()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        val features = mutableListOf<AppFeature>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "oplus-feature") {
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
        // 同名特性去重，至少一个启用即可视为启用
        features
            .groupBy { it.name }
            .map { (name, list) ->
                AppFeature(name, list.any { it.enabled })
            }
    }

    override suspend fun saveFeatures(configPath: String, features: List<AppFeature>): Unit = withContext(Dispatchers.IO) {
        val file = File(configPath)
        file.parentFile?.mkdirs()

        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.appendLine("<oplus-config>")

            features.forEach { feature ->
                writer.appendLine("\t<oplus-feature name=\"${feature.name}\"/>")
            }
            writer.appendLine("</oplus-config>")
        }

        ConfigUtils.copyConfigToModule()
    }

    private fun parseEnabledFromArgs(args: String): Boolean {
        if (args.startsWith("boolean:")) {
            return args.substringAfter("boolean:").equals("true", ignoreCase = true)
        }
        // 没有 args 时，默认视为启用（与示例文件一致）
        return true
    }
} 