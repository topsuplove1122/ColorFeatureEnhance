package com.itosfish.colorfeatureenhance.utils // 所属包，存放工具类

import android.graphics.RenderEffect // Android 12+ 提供的渲染特效类，用于实现模糊
import android.graphics.Shader // 渲染特效中的 TileMode 配置
import android.os.Handler // 用于切换线程执行任务
import android.os.Looper // Looper 用于判断/获取主线程
import android.util.Log // 日志输出
import android.view.View // Android 视图基础类
import androidx.activity.compose.LocalActivity // 提供当前Activity引用
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * 使用 RenderEffect 在 Android 12+ 为视图添加/移除模糊效果的工具类。
 * 对外仅暴露一个静态方法 [setBlurView] 供调用。
 */
object BlurUtils { // Kotlin 单例对象，方便直接调用

    /** 默认模糊半径，单位: 像素(px) */
    private const val DEFAULT_RADIUS = 15f

    /**
     * 为指定 [view] 设置或取消模糊效果。
     * @param view   需要模糊的目标视图
     * @param status true -> 开启模糊；false -> 取消模糊
     */
    @JvmStatic // 让 Java 调用时表现为静态方法，方便跨语言调用
    fun setBlurView(view: View?, status: Boolean) {
        if (view == null) return // 视图为空直接返回

        // 若当前视图未启用硬件加速，RenderEffect 可能无法生效，给予警告
        if (!view.isHardwareAccelerated) {
            Log.w("BlurUtils", "视图未启用硬件加速，模糊效果可能无效")
        }

        // 待在主线程执行的任务：根据 status 应用或移除模糊
        val task = Runnable {
            // 当 status==true 时传入 DEFAULT_RADIUS，否则传 0 代表取消模糊
            applyBlurEffect(view, if (status) DEFAULT_RADIUS else 0f)
        }

        // 判断当前线程：若已是主线程直接执行，否则切换到主线程
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 已处于主线程，直接运行任务
            task.run()
        } else {
            // 不在主线程，使用 Handler 切换到主线程
            Handler(Looper.getMainLooper()).post(task)
        }
    }

    /**
     * 真正执行模糊/去模糊逻辑的内部函数。
     * @param view       目标视图
     * @param blurRadius 模糊半径，<=0 表示取消模糊
     */
    private fun applyBlurEffect(view: View, blurRadius: Float) {
        if (blurRadius <= 0f) { // radius <= 0 表示取消模糊
            view.setRenderEffect(null) // 移除已有 RenderEffect
            return
        }
        try {
            // 创建高斯模糊 RenderEffect（X、Y 半径一致，TileMode 使用 CLAMP）
            val effect =
                RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            view.setRenderEffect(effect) // 应用到目标视图
        } catch (e: IllegalArgumentException) { // 某些设备或 API 可能抛出异常
            Log.e("BlurUtils", "设置模糊失败", e)
        }
    }
}

/**
 * 组合式背景模糊效果，原 `BlurBehindDialogEffect` 已合并至本文件。
 * 在需要模糊背景的对话框附近调用即可。
 */
@Composable
fun BlurBehindDialogEffect(enable: Boolean) {
    if (!enable) return // 不启用则直接返回
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        activity?.window?.decorView?.let { BlurUtils.setBlurView(it, true) }
        onDispose {
            activity?.window?.decorView?.let { BlurUtils.setBlurView(it, false) }
        }
    }
} 