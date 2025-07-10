package com.itosfish.colorfeatureenhance.utils

import android.app.Activity
import android.text.method.LinkMovementMethod
// Linkify 可能引入歧义，已不再需要
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itosfish.colorfeatureenhance.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itosfish.colorfeatureenhance.data.model.AppFeatureMappings
import android.content.Context


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

@Composable
fun AddFeatureDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, enabled: Boolean) -> Unit,
    context: Context
) {
    var featureName by remember { mutableStateOf("") }
    var featureDescription by remember { mutableStateOf("") }
    var featureEnabled by remember { mutableStateOf(true) }
    var showPresetMatchDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_feature)) },
        text = {
            Column {
                // 特性名称输入
                OutlinedTextField(
                    value = featureName,
                    onValueChange = { featureName = it },
                    label = { Text(stringResource(id = R.string.feature_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 特性描述输入
                OutlinedTextField(
                    value = featureDescription,
                    onValueChange = { featureDescription = it },
                    label = { Text(stringResource(id = R.string.feature_description)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 启用状态选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.feature_enabled))
                    Switch(
                        checked = featureEnabled,
                        onCheckedChange = { featureEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (featureName.isNotEmpty()) {
                        // 只有当描述不为空时才保存映射
                        if (featureDescription.isNotEmpty()) {
                            // 检查是否与预设映射匹配
                            val isPresetMatch = AppFeatureMappings.getInstance().isMatchingPresetDescription(
                                context, featureName, featureDescription
                            )

                            if (isPresetMatch) {
                                // 显示提示对话框
                                showPresetMatchDialog = true
                            } else {
                                // 保存用户自定义映射
                                AppFeatureMappings.getInstance().saveUserMapping(
                                    context, featureName, featureDescription
                                )
                                // 添加特性并关闭对话框
                                onConfirm(featureName, featureDescription, featureEnabled)
                                // 不要在这里关闭对话框，因为showPresetMatchDialog=true时需要继续显示
                            }
                        } else {
                            // 描述为空，直接添加特性并关闭对话框
                            onConfirm(featureName, featureDescription, featureEnabled)
                        }
                    }
                },
                enabled = featureName.isNotEmpty() // 只要名称不为空即可
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )

    // 预设映射匹配提示对话框
    if (showPresetMatchDialog) {
        AlertDialog(
            onDismissRequest = { showPresetMatchDialog = false },
            title = { Text(text = stringResource(id = R.string.preset_mapping_match_title)) },
            text = { Text(text = stringResource(id = R.string.preset_mapping_match_message)) },
            confirmButton = {
                TextButton(onClick = {
                    // 关闭提示对话框，添加特性并关闭主对话框
                    showPresetMatchDialog = false
                    onConfirm(featureName, featureDescription, featureEnabled)
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        )
    }
} 