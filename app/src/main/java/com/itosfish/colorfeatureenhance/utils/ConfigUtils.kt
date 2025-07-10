package com.itosfish.colorfeatureenhance.utils

import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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

    /**
     * 安装模块：
     * 1. 读取 assets 下的 mod.zip 并解压到缓存目录
     * 2. 将解压后的 mod 目录移动（复制）到 /data/adb/ColorOSFeaturesEnhance/
     * 3. 复制完成后返回是否成功
     */
    fun installModule(): Boolean {
        val destDirPath = "/data/adb/modules/ColorOSFeaturesEnhance"

        // 临时工作目录
        val tempDir = File(app.cacheDir, "moduleTemp").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        // 将 assets 中的 zip 写入临时文件
        val tempZip = File(tempDir, "mod.zip")
        try {
            app.assets.open("mod.zip").use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        // 解压 zip
        if (!unzip(tempZip, tempDir)) return false

        val extractedDir = File(tempDir, "mod")
        if (!extractedDir.exists()) return false

        // 复制目录到目标位置，使用 root 权限
        val shellCmd = "mkdir -p \"$destDirPath\" && cp -r \"${extractedDir.absolutePath}/.\" \"$destDirPath/\""
        CSU.runWithSu(shellCmd)

        // 判断关键文件是否复制成功
        return CSU.fileExists("$destDirPath/module.prop")
    }

    /**
     * 解压 zip 文件到指定目录
     */
    private fun unzip(zipFile: File, destDir: File): Boolean {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val newFile = File(destDir, entry!!.name)
                    if (entry!!.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}