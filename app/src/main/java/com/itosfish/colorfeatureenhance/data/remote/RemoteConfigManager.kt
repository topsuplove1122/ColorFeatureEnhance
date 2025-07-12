package com.itosfish.colorfeatureenhance.data.remote

import android.content.Context
import com.itosfish.colorfeatureenhance.data.model.CloudFeatureMappings
import com.itosfish.colorfeatureenhance.utils.CLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 远程配置管理器
 * 负责从云端下载、校验和解析特性描述映射配置
 */
class RemoteConfigManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "RemoteConfigManager"
        
        // 云端配置文件URL（可配置）
        private const val DEFAULT_CONFIG_URL = "https://gitee.com/su-su2239/oplus-features/raw/master/Features/mappings.json"
        
        // 网络请求超时时间
        private const val CONNECT_TIMEOUT = 10L
        private const val READ_TIMEOUT = 30L
        
        @Volatile
        private var INSTANCE: RemoteConfigManager? = null
        
        fun getInstance(context: Context): RemoteConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val cloudMappings = CloudFeatureMappings.getInstance(context)
    
    /**
     * 远程配置数据结构
     * 直接映射JSON中的特性名称到多语言描述
     */
    @Serializable
    data class RemoteConfigData(
        val mappings: Map<String, CloudFeatureMappings.MultiLanguageDescription>
    )
    
    /**
     * 配置更新结果
     */
    sealed class UpdateResult {
        object Success : UpdateResult()
        object NoUpdate : UpdateResult()
        data class Error(val message: String, val exception: Throwable? = null) : UpdateResult()
    }
    
    /**
     * 检查并更新云端配置
     * @param configUrl 配置文件URL，如果为null则使用默认URL
     * @return 更新结果
     */
    suspend fun checkAndUpdateConfig(configUrl: String? = null): UpdateResult = withContext(Dispatchers.IO) {
        try {
            CLog.i(TAG, "开始检查云端配置更新")
            
            val url = configUrl ?: DEFAULT_CONFIG_URL
            val configData = downloadConfig(url)
            
            if (configData == null) {
                CLog.w(TAG, "下载配置失败或配置为空")
                return@withContext UpdateResult.Error("下载配置失败")
            }
            
            // 解析并保存配置
            val parseResult = parseAndSaveConfig(configData)
            if (parseResult) {
                CLog.i(TAG, "云端配置更新成功")
                UpdateResult.Success
            } else {
                CLog.w(TAG, "解析配置失败")
                UpdateResult.Error("解析配置失败")
            }
            
        } catch (e: Exception) {
            CLog.e(TAG, "检查云端配置更新时发生异常", e)
            UpdateResult.Error("更新异常: ${e.message}", e)
        }
    }
    
    /**
     * 下载远程配置文件
     * @param url 配置文件URL
     * @return 配置文件内容，失败时返回null
     */
    private suspend fun downloadConfig(url: String): String? = withContext(Dispatchers.IO) {
        try {
            CLog.d(TAG, "开始下载配置: $url")
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ColorFeatureEnhance/1.0")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                CLog.w(TAG, "下载配置失败: HTTP ${response.code}")
                return@withContext null
            }
            
            val content = response.body?.string()
            if (content.isNullOrEmpty()) {
                CLog.w(TAG, "配置内容为空")
                return@withContext null
            }
            
            CLog.d(TAG, "配置下载成功，大小: ${content.length} 字符")
            return@withContext content
            
        } catch (e: Exception) {
            CLog.e(TAG, "下载配置时发生异常", e)
            return@withContext null
        }
    }
    
    /**
     * 解析并保存配置数据
     * @param configContent 配置文件内容
     * @return 是否成功
     */
    private suspend fun parseAndSaveConfig(configContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            CLog.d(TAG, "开始解析配置数据")
            
            // 尝试直接解析为映射格式
            val mappings = try {
                json.decodeFromString<Map<String, CloudFeatureMappings.MultiLanguageDescription>>(configContent)
            } catch (e: Exception) {
                CLog.d(TAG, "直接解析失败，尝试包装格式解析")
                // 尝试解析包装格式
                val configData = json.decodeFromString<RemoteConfigData>(configContent)
                configData.mappings
            }
            
            if (mappings.isEmpty()) {
                CLog.w(TAG, "配置映射为空")
                return@withContext false
            }
            
            // 验证配置数据格式
            val validMappings = validateMappings(mappings)
            if (validMappings.isEmpty()) {
                CLog.w(TAG, "没有有效的配置映射")
                return@withContext false
            }
            
            // 保存到本地存储
            cloudMappings.saveMappings(validMappings)
            
            // 保存更新时间作为版本标识
            val currentTime = System.currentTimeMillis().toString()
            cloudMappings.saveRemoteVersion(currentTime)
            
            CLog.i(TAG, "配置解析并保存成功: ${validMappings.size} 个特性映射")
            return@withContext true
            
        } catch (e: Exception) {
            CLog.e(TAG, "解析配置数据时发生异常", e)
            return@withContext false
        }
    }
    
    /**
     * 验证配置映射数据
     * @param mappings 原始映射数据
     * @return 验证后的有效映射数据
     */
    private fun validateMappings(
        mappings: Map<String, CloudFeatureMappings.MultiLanguageDescription>
    ): Map<String, CloudFeatureMappings.MultiLanguageDescription> {
        val validMappings = mutableMapOf<String, CloudFeatureMappings.MultiLanguageDescription>()
        
        mappings.forEach { (featureName, description) ->
            // 验证特性名称
            if (featureName.isBlank()) {
                CLog.w(TAG, "跳过空特性名称")
                return@forEach
            }
            
            // 验证描述内容（至少要有一种语言的描述）
            if ((description.zh.isNullOrBlank()) && (description.en.isNullOrBlank())) {
                CLog.w(TAG, "跳过无效描述的特性: $featureName")
                return@forEach
            }
            
            validMappings[featureName] = description
        }
        
        CLog.d(TAG, "配置验证完成: ${mappings.size} -> ${validMappings.size}")
        return validMappings
    }
    
    /**
     * 获取当前设备的语言代码
     * @return 语言代码（如 "zh", "en"）
     */
    fun getCurrentLanguage(): String {
        val locale = Locale.getDefault()
        return when (locale.language.lowercase()) {
            "zh" -> "zh"
            "en" -> "en"
            else -> "en" // 默认英文
        }
    }
    
    /**
     * 获取云端配置统计信息
     * @return 统计信息字符串
     */
    fun getConfigStats(): String {
        val mappingCount = cloudMappings.getMappingCount()
        val remoteVersion = cloudMappings.getRemoteVersion()
        val versionInfo = if (remoteVersion != null) {
            try {
                val timestamp = remoteVersion.toLong()
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(java.util.Date(timestamp))
                "更新时间: $date"
            } catch (e: Exception) {
                "版本: $remoteVersion"
            }
        } else {
            "未更新"
        }
        
        return "云端配置: $mappingCount 个特性, $versionInfo"
    }
    
    /**
     * 清除云端配置缓存
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cloudMappings.clearAllMappings()
            CLog.i(TAG, "云端配置缓存已清除")
            true
        } catch (e: Exception) {
            CLog.e(TAG, "清除云端配置缓存失败", e)
            false
        }
    }
}
