package com.itosfish.colorfeatureenhance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.config.ConfigMergeManager.PatchAction

/**
 * 补丁状态相关的颜色工具类
 */
object PatchColors {
    
    /**
     * 根据补丁状态获取卡片颜色
     * @param patchAction 补丁动作类型，null表示无补丁（使用默认颜色）
     * @return CardColors
     */
    @Composable
    fun getCardColors(patchAction: PatchAction?): CardColors {
        val isDark = isSystemInDarkTheme()

        return when (patchAction) {
            PatchAction.ADD -> {
                // 绿色 - 新增特性
                CardDefaults.cardColors(
                    containerColor = if (isDark) PatchAddDark else PatchAddLight,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            PatchAction.MODIFY -> {
                // 紫色 - 修改特性
                CardDefaults.cardColors(
                    containerColor = if (isDark) PatchModifyDark else PatchModifyLight,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            PatchAction.REMOVE -> {
                // 红色 - 删除特性
                CardDefaults.cardColors(
                    containerColor = if (isDark) PatchRemoveDark else PatchRemoveLight,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            null -> {
                // 默认颜色 - 无补丁
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
    /**
     * 根据补丁状态获取边框颜色
     * @param patchAction 补丁动作类型
     * @return Color?，null表示无边框
     */
    @Composable
    fun getBorderColor(patchAction: PatchAction?): Color? {
        return when (patchAction) {
            PatchAction.ADD -> PatchAddBorder
            PatchAction.MODIFY -> PatchModifyBorder
            PatchAction.REMOVE -> PatchRemoveBorder
            null -> null
        }
    }

    /**
     * 根据补丁状态获取状态指示器颜色
     * @param patchAction 补丁动作类型
     * @return Color?，null表示无状态指示器
     */
    @Composable
    fun getIndicatorColor(patchAction: PatchAction?): Color? {
        return when (patchAction) {
            PatchAction.ADD -> PatchAddBorder
            PatchAction.MODIFY -> PatchModifyBorder
            PatchAction.REMOVE -> PatchRemoveBorder
            null -> null
        }
    }
    
    /**
     * 获取补丁状态的描述文本
     * @param patchAction 补丁动作类型
     * @return 状态描述
     */
    @Composable
    fun getPatchActionDescription(patchAction: PatchAction?): String {
        return when (patchAction) {
            PatchAction.ADD -> stringResource(R.string.patch_status_add)
            PatchAction.MODIFY -> stringResource(R.string.patch_status_modify)
            PatchAction.REMOVE -> stringResource(R.string.patch_status_remove)
            null -> ""
        }
    }
}
