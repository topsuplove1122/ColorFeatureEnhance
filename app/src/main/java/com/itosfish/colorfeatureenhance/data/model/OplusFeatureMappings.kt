package com.itosfish.colorfeatureenhance.data.model

import android.content.Context
import com.itosfish.colorfeatureenhance.R

/**
 * Oplus 系统级特性映射表管理
 */
class OplusFeatureMappings private constructor() {

    /**
     * feature name -> string resId
     */
    val NAME_TO_RES_ID: Map<String, Int> = mapOf(
        // 适配性代码
        "oplus.software.systemui.navbar_pick_color" to R.string.oplus_feature_pick_color_optimization,
        "oplus.software.string_gc_support" to R.string.oplus_feature_gcstring,
        "oplus.software.display.1.5k_resolution_switch_support" to R.string.oplus_feature_compatibility_code,

        // AI 护眼
        "com.oplus.eyeprotect.ai_intelligent_eye_protect_support" to R.string.oplus_feature_ai_eye_protect,

        // 卫星网络
        "oplus.software.radio.tt_satellite_support" to R.string.oplus_feature_satellite_network,
        "oplus.software.radio.satellite_aging" to R.string.oplus_feature_satellite_network,

        // 游戏加速
        "oplus.software.radio.smart_game_qoe_recovery" to R.string.oplus_feature_game_acceleration,

        // Hyper 模式性能引擎
        "oplus.software.gamehyper" to R.string.oplus_feature_hyper_engine,

        // 触摸优化
        "oplus.software.enable_touch_preload" to R.string.oplus_feature_touch_optimization,

        // 独立游戏助手色彩按钮
        "oplus.software.display.game_color_enhance_1.0" to R.string.oplus_feature_game_color_button,

        // 护眼提醒
        "oplus.software.display.ai_eyeprotect_v1_support" to R.string.oplus_feature_eyeprotect_reminder,

        // 无障碍屏幕白点减少
        "oplus.software.display.reduce_white_point" to R.string.oplus_feature_reduce_white_point,

        // 原神肩键
        "oplus.software.shoulderkey_support" to R.string.oplus_feature_genshin_shoulder_key,

        // 屏幕最小亮度拓展 & 低Lux去晃动拓展（使用相同描述）
        "oplus.software.display.lux_small_debounce_expand_support" to R.string.oplus_feature_min_brightness_expand,

        // 低亮度屏闪
        "oplus.hardware.display.no_bright_eyes_low_freq_strobe" to R.string.oplus_feature_low_brightness_flicker,

        // 游戏助手 ifs 画面
        "oplus.software.display.game.ifs_enable" to R.string.oplus_feature_game_ifs,

        // 视频音量增强
        "oplus.video.audio.volume.enhancement" to R.string.oplus_feature_video_volume_enhance,

        // 侧边栏视频助手
        "oplus.software.smart_sidebar_video_assistant" to R.string.oplus_feature_sidebar_video_assistant,

        // 散热风扇
        "oplus.misc.fan.support" to R.string.oplus_feature_cooling_fan,

        // 直播场景加速
        "oplus.software.radio.livebroadcast_accelerate" to R.string.oplus_feature_live_scene_acceleration,

        // 录音支持声音焦点
        "oplus.software.audio.record.sound_focus_support" to R.string.oplus_feature_sound_focus_record,

        // 前台预加载
        "oplus.softwore.top.preload" to R.string.oplus_feature_foreground_preload,

        // 其它映射未知
        "default" to R.string.feature_unknown
    )

    companion object {
        @Volatile private var INSTANCE: OplusFeatureMappings? = null

        fun getInstance(): OplusFeatureMappings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OplusFeatureMappings().also { INSTANCE = it }
            }
        }

        fun getLocalizedDescription(context: Context, featureName: String): String {
            val userMappings = UserFeatureMappings.getInstance(context)
            val userDesc = userMappings.getDescription(featureName)
            if (!userDesc.isNullOrEmpty()) return userDesc

            val resId = getInstance().getResId(featureName)
            return if (resId == R.string.feature_unknown) featureName else context.getString(resId)
        }

        /** 保存用户自定义映射 */
        fun saveUserMapping(context: Context, name: String, description: String) {
            UserFeatureMappings.getInstance(context).saveMapping(name, description)
        }

        /** 删除用户映射 */
        fun removeUserMapping(context: Context, name: String) {
            UserFeatureMappings.getInstance(context).removeMapping(name)
        }

        fun isMatchingPresetDescription(context: Context, name: String, description: String): Boolean {
            val resId = getInstance().getResId(name)
            if (resId == R.string.feature_unknown) return false
            val preset = context.getString(resId)
            return preset == description
        }
    }

    fun getResId(name: String): Int = NAME_TO_RES_ID[name] ?: R.string.feature_unknown
} 