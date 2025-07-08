package com.itosfish.colorfeatureenhance.utils

import android.app.Activity
import android.text.method.LinkMovementMethod
// Linkify 可能引入歧义，已不再需要
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itosfish.colorfeatureenhance.R

/**
 * 使用 XML 布局展示 About 弹窗，模仿 Shizuku 实现。
 */
fun showAboutDialog(activity: Activity) {
    val inflater = LayoutInflater.from(activity)
    val root = inflater.inflate(R.layout.dialog_about, null, false)

    // 图标
    val iconView = root.findViewById<ImageView>(R.id.icon)
    iconView.setImageDrawable(activity.packageManager.getApplicationIcon(activity.applicationInfo))

    // 版本号
    val versionNameView = root.findViewById<TextView>(R.id.version_name)
    val versionName = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
    versionNameView.text = "$versionName"

    // 富文本链接
    val sourceCodeView = root.findViewById<TextView>(R.id.source_code)
    sourceCodeView.movementMethod = LinkMovementMethod.getInstance()
    MaterialAlertDialogBuilder(activity)
        .setView(root)
        .show()
} 