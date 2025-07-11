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
        "default" to R.string.feature_unknown,
        "oplus.software.directservice.finger_flashnotes_enable" to R.string.oplus_feature_finger_flashnotes,
        "com.android.launcher.is_support_icon_blur" to R.string.oplus_feature_icon_blur,
        "oplus.software.display.game.htfi_enable" to R.string.oplus_feature_game_htfi_high_temp,
        "oplus.hardware.display.bl_pwm_dc" to R.string.oplus_feature_pwm_dimming,
        "oplus.hardware.display.full_brightness_DC" to R.string.oplus_feature_full_brightness_dc,
        "oplus.software.audio.super_volume_4x" to R.string.oplus_feature_super_volume_4x,
        "oplus.hardware.audio.voice_isolation_support" to R.string.oplus_feature_voice_isolation,
        "oplus.software.spatializer_speaker" to R.string.oplus_feature_speaker_spatializer,
        "oplus.software.radio.support_5g" to R.string.oplus_feature_5g_support,
        "oplus.software.radio.support_dual_nr" to R.string.oplus_feature_dual_nr,
        "oplus.software.radio.nr_always_sa_pre" to R.string.oplus_feature_nr_always_sa_pre,
        "oplus.software.radio.smart5g_sa_enabled" to R.string.oplus_feature_smart5g_sa,
        "oplus.software.radio.game_accelerate" to R.string.oplus_feature_game_accelerate_concert,
        "oplus.software.radio.meeting_accelerate" to R.string.oplus_feature_meeting_accelerate,
        "oplus.software.radio.data_concert_accelerate" to R.string.oplus_feature_data_concert_accelerate,
        "oplus.software.display.osie_aipq_support" to R.string.oplus_feature_aipq,
        "oplus.software.display.osie_aisdr2hdr_support" to R.string.oplus_feature_aisdr2hdr,
        "oplus.software.game.yuanshenvibration" to R.string.oplus_feature_genshin_vibration,
        "oplus.software.vibrator.screenshot_wave_support" to R.string.oplus_feature_screenshot_wave_vibration,
        "oplus.software.smart_loop_drag" to R.string.oplus_feature_smart_loop_drag,
        "oplus.software.screenon_before_special_action_open_camera" to R.string.oplus_feature_screenon_before_camera,
        "com.oplus.camera.under.water.support" to R.string.oplus_feature_underwater_camera,
        "oplus.software.fingerprint.ultrasonic_underwater_shutter" to R.string.oplus_feature_ultrasonic_underwater_shutter,
        "oplus.software.floatwindow_hide_alertwindow_notification" to R.string.oplus_feature_floatwindow_hide_alert_notification,
        "oplus.software.tvconnect.support" to R.string.oplus_feature_tv_connect,
        "oplus.software.tv_videocall.support" to R.string.oplus_feature_tv_videocall,
        "oplus.software.flexible_freeform.support" to R.string.oplus_feature_flexible_freeform,
        "oplus.software.audio.magicvoice_loopback_support" to R.string.oplus_feature_magicvoice_loopback,
        "oplus.software.display.game.support_vulkan" to R.string.oplus_feature_game_support_vulkan,
        "oplus.software.display.game_color_enhance_2.0" to R.string.oplus_feature_game_color_enhance_2,
        "oplus.software.display.game_dark_eyeprotect_support" to R.string.oplus_feature_game_dark_eyeprotect,
        "oplus.software.support.zoom.game_enter" to R.string.oplus_feature_zoom_game_enter,
        "oplus.software.support.zoom.center_exit" to R.string.oplus_feature_zoom_center_exit,
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
            val result = if (resId == R.string.feature_unknown) featureName else context.getString(resId)
            return result
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