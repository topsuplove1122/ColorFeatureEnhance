package com.itosfish.colorfeatureenhance.data.model

import com.itosfish.colorfeatureenhance.R
import android.content.Context

/**
 * 应用特性映射表管理
 */
class AppFeatureMappings private constructor() {

    /**
     * Map of feature name to Chinese description.
     */
    val NAME_TO_RES_ID: Map<String, Int> = mapOf(
        // AI 聊天场景
        "com.oplus.directservice.aitoolbox_enable" to R.string.feature_ai_chat_scene,
        // 直播助手
        "com.oplus.mediaturbo.game_live" to R.string.feature_live_assistant,
        // 点击屏幕唤醒息屏
        "oplus.aod.wakebyclick.support" to R.string.feature_tap_to_wake_screen,
        // 实况照片支持设置锁屏图片
        "com.oplus.wallpapers.livephoto_wallpaper" to R.string.feature_livephoto_lock_screen,
        // 全局 AOD
        "com.oplus.systemui.panoramic_aod.enable" to R.string.feature_global_aod,
        // 分离样式通控面板图标可变形
        "com.android.systemui.qs_deform_enable" to R.string.feature_qs_icon_deformable,
        // 区域录屏
        "com.oplus.screenrecorder.area_record" to R.string.feature_area_record,
        // 游戏 AI 3.0
        "feature.support.game.AI_PLAY_version3" to R.string.feature_game_ai_v3,
        // 游戏 AI 4.0 (多个变种共用同一描述)
        "feature.support.game.AI_PLAY_version4_mlbb" to R.string.feature_game_ai_v4,
        "feature.support.game.AI_PLAY_version4" to R.string.feature_game_ai_v4,
        "feature.support.game.AI_PLAY_version4_audio" to R.string.feature_game_ai_v4,
        // 游戏助手辅助
        "feature.support.game.ASSIST_KEY" to R.string.feature_game_assist_key,
        // 游戏 4D 震动模式
        "com.coloros.gamespace_game_support_fourd_vibration" to R.string.feature_game_4d_vibration,
        // 电话触摸分享与屏幕共享
        "com.android.incallui.share_screen_and_touch_cmd_support" to R.string.feature_phone_touch_share,
        // 游戏助手旁路供电
        "com.oplus.games.show_bypass_charging_when_gameapps" to R.string.feature_game_bypass_charging,
        // 侧边栏声音分轨助手
        "com.oplus.smartmediacontroller.lss_assistant_enable" to R.string.feature_sidebar_audio_split_assistant,
        // 游戏风扇火力模式
        "feature.custom_heat_game.support" to R.string.feature_game_fan_power_mode,
        // 全场景旁路供电
        "com.oplus.plc_charge.support" to R.string.feature_all_scene_bypass_charging,
        "com.oplus.fullscene_plc_charge.support" to R.string.feature_all_scene_bypass_charging,
        // 反向充电
        "com.oplus.reversecharge" to R.string.feature_reverse_charging,
        // 微信视频通话功能增强
        "com.oplus.mediaturbo.wechat_videoCalling" to R.string.feature_wechat_video_call_enhance,
        // 键盘 NFC 分享 iOS 控制中心界面
        "oplus.ptc.nfc.ios.settings" to R.string.feature_keyboard_nfc_ios_share,
        // 以下特性暂未明确中文描述
        "com.oplus.battery.whitelist_vowifi" to R.string.feature_unknown,
        "oplus_skip_remove_recent_task" to R.string.feature_unknown,
        "oplus_disallow_run_apps" to R.string.feature_unknown,
        "com.coloros.colordirectservice.cm_enable" to R.string.feature_unknown,
        "com.oplus.mediaturbo.tencent_meeting" to R.string.feature_unknown,
        "com.android.settings.network_access_permission" to R.string.feature_unknown,
        "com.oplus.battery.autostart_limit_num" to R.string.feature_unknown,
        "com.android.launcher.recent_lock_limit_num" to R.string.feature_unknown
    )

    /**
     * 单例实例
     */
    companion object {
        @Volatile
        private var INSTANCE: AppFeatureMappings? = null
        
        /**
         * 获取映射表实例
         */
        fun getInstance(): AppFeatureMappings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppFeatureMappings().also { INSTANCE = it }
            }
        }

        /**
         * 获取特性的描述资源ID，优先从用户自定义映射中查找
         * @param context 上下文
         * @param featureName 特性名称
         * @return 描述字符串
         */
        fun getLocalizedDescription(context: Context, featureName: String): String {
            // 先查找用户自定义映射
            val userMappings = UserFeatureMappings.getInstance(context)
            val userDescription = userMappings.getDescription(featureName)
            // 如果用户描述存在且不为空，则使用用户描述
            if (userDescription != null && userDescription.isNotEmpty()) {
                return userDescription
            }
            
            // 再查找预设映射
            val resId = getInstance().getResId(featureName)
            val result = if (resId == R.string.feature_unknown) {
                featureName
            } else {
                context.getString(resId)
            }
            return result
        }
    }

    /**
     * 获取中文描述，若未收录则返回 "待确认"。
     */
    fun getResId(featureName: String): Int = NAME_TO_RES_ID[featureName] ?: R.string.feature_unknown

    /**
     * 保存用户自定义映射
     */
    fun saveUserMapping(context: Context, name: String, description: String) {
        UserFeatureMappings.getInstance(context).saveMapping(name, description)
    }

    /**
     * 删除用户自定义映射
     */
    fun removeUserMapping(context: Context, name: String) {
        UserFeatureMappings.getInstance(context).removeMapping(name)
    }

    /**
     * 批量删除用户自定义映射
     */
    fun removeUserMappings(context: Context, names: List<String>) {
        val userMappings = UserFeatureMappings.getInstance(context)
        names.forEach { userMappings.removeMapping(it) }
    }

    /**
     * 检查特性名称和描述是否与预设映射匹配
     * @return 如果匹配返回true，否则返回false
     */
    fun isMatchingPresetDescription(context: Context, name: String, description: String): Boolean {
        val resId = getResId(name)
        // 如果是未知特性，直接返回false
        if (resId == R.string.feature_unknown) {
            return false
        }
        
        // 比较预设描述和用户输入的描述
        val presetDescription = context.getString(resId)
        return presetDescription == description
    }
} 