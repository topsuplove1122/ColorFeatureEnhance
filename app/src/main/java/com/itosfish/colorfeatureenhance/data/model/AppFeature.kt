package com.itosfish.colorfeatureenhance.data.model

/**
 * 表示单个配置特性
 * @param name XML 中的特性 name
 * @param enabled 当前是否开启
 */
data class AppFeature(
    val name: String,
    val enabled: Boolean,
    /** 原始 args 字符串，如 "int:1", "String:xxxxx"，boolean 情况可为空 */
    val args: String? = null
) 