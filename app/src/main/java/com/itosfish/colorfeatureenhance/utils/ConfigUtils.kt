package com.itosfish.colorfeatureenhance.utils

import com.itosfish.colorfeatureenhance.MainActivity.Companion.app

object ConfigUtils {
    /**
     * 将 /my_product/etc/extension/ 下的配置文件复制到应用可读写的外部存储目录。
     *
     * 复制完成后，配置文件将位于 app.getExternalFilesDir("configs") 对应的路径中。
     *
     * @return true 表示复制成功，false 表示失败。
     */
    fun copySystemConfig(): Boolean {
        // 获取目标目录路径
        val destDir = app.getExternalFilesDir(null)?.absolutePath ?: return false

        // Shell 脚本：创建目标目录并复制全部配置文件
        val shellCmd =
            "mkdir -p \"$destDir\" && " +
            "cp /my_product/etc/extension/com.oplus.app-features.xml \"$destDir/\" && " +
            "cp /my_product/etc/extension/com.oplus.oplus-feature.xml \"$destDir/\""

        // 执行命令（需要 root）
        CSU.runWithSu(shellCmd)

        // 基于目录是否存在来粗略判断复制是否成功
        return CSU.fileExists(destDir)
    }
}