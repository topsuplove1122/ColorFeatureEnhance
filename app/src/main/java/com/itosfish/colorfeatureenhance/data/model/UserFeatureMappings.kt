package com.itosfish.colorfeatureenhance.data.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 用户自定义的特性名称和描述映射
 * 使用 SharedPreferences 持久化存储
 */
class UserFeatureMappings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * 保存特性名称和描述映射
     */
    fun saveMapping(name: String, description: String) {
        prefs.edit(commit = true) {
            putString(name, description)
        }
    }
    
    /**
     * 获取特性的描述，如果不存在则返回null
     */
    fun getDescription(name: String): String? {
        val desc = prefs.getString(name, null)
        return desc
    }
    
    /**
     * 获取所有映射
     */
    fun getAllMappings(): Map<String, String> {
        return prefs.all.filterValues { it is String }.mapValues { it.value as String }
    }
    
    /**
     * 检查特性名称是否存在映射
     */
    fun hasMapping(name: String): Boolean {
        return prefs.contains(name)
    }
    
    /**
     * 删除特性映射
     */
    fun removeMapping(name: String) {
        prefs.edit(commit = true) {
            remove(name)
        }
    }
    
    /**
     * 清空所有映射
     */
    fun clear() {
        prefs.edit(commit = true) {
            clear()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "user_feature_mappings"
        
        @Volatile
        private var INSTANCE: UserFeatureMappings? = null
        
        fun getInstance(context: Context): UserFeatureMappings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserFeatureMappings(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
} 