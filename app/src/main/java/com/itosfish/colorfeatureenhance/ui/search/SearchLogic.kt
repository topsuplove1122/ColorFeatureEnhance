package com.itosfish.colorfeatureenhance.ui.search

import android.content.Context
import com.itosfish.colorfeatureenhance.FeatureMode
import com.itosfish.colorfeatureenhance.data.model.AppFeature
import com.itosfish.colorfeatureenhance.data.model.AppFeatureMappings
import com.itosfish.colorfeatureenhance.data.model.FeatureGroup
import com.itosfish.colorfeatureenhance.data.model.OplusFeatureMappings

/**
 * 搜索功能逻辑
 */
object SearchLogic {
    /**
     * 根据搜索查询过滤特性列表
     * @param features 原始特性列表
     * @param query 搜索查询文本
     * @param context 上下文，用于获取本地化描述
     * @param currentMode 当前特性模式
     * @return 过滤后的特性列表
     */
    fun filterFeatures(
        features: List<AppFeature>,
        query: String,
        context: Context,
        currentMode: FeatureMode
    ): List<AppFeature> {
        if (query.isBlank()) return features
        
        val normalizedQuery = query.trim().lowercase()
        
        return features.filter { feature ->
            // 检查特性名称是否匹配
            val nameMatches = feature.name.lowercase().contains(normalizedQuery)
            
            // 获取描述并检查是否匹配
            val description = if (currentMode == FeatureMode.APP) {
                AppFeatureMappings.getLocalizedDescription(context, feature.name)
            } else {
                OplusFeatureMappings.getLocalizedDescription(context, feature.name)
            }
            
            val descriptionMatches = description.lowercase().contains(normalizedQuery)
            
            // 名称或描述匹配即返回
            nameMatches || descriptionMatches
        }
    }
    
    /**
     * 根据搜索查询过滤特性组
     * @param groups 原始特性组列表
     * @param query 搜索查询文本
     * @param context 上下文，用于获取本地化描述
     * @param currentMode 当前特性模式
     * @return 过滤后的特性组列表
     */
    fun filterFeatureGroups(
        groups: List<FeatureGroup>,
        query: String,
        context: Context,
        currentMode: FeatureMode
    ): List<FeatureGroup> {
        if (query.isBlank()) return groups
        
        val normalizedQuery = query.trim().lowercase()
        
        return groups.filter { group ->
            // 检查组内任一特性是否匹配
            group.features.any { feature ->
                // 检查特性名称是否匹配
                val nameMatches = feature.name.lowercase().contains(normalizedQuery)
                
                // 获取描述并检查是否匹配
                val description = if (currentMode == FeatureMode.APP) {
                    AppFeatureMappings.getLocalizedDescription(context, feature.name)
                } else {
                    OplusFeatureMappings.getLocalizedDescription(context, feature.name)
                }
                
                val descriptionMatches = description.lowercase().contains(normalizedQuery)
                
                // 名称或描述匹配即返回
                nameMatches || descriptionMatches
            }
        }
    }
} 