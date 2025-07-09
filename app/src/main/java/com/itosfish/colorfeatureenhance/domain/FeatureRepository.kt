package com.itosfish.colorfeatureenhance.domain

import com.itosfish.colorfeatureenhance.data.model.AppFeature

interface FeatureRepository {
    suspend fun loadFeatures(configPath: String): List<AppFeature>

    /**
     * 将特性列表写回配置文件
     */
    suspend fun saveFeatures(configPath: String, features: List<AppFeature>)
} 