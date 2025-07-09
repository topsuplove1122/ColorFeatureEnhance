package com.itosfish.colorfeatureenhance.data.model

/**
 * 表示单个配置特性
 * @param name XML 中的特性 name
 * @param enabled 当前是否开启
 */
data class AppFeature(
    val name: String,
    val enabled: Boolean
) 