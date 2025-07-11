package com.itosfish.colorfeatureenhance.utils

import android.util.Log
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 新架构的配置管理工具类
 * 负责配置目录初始化、模块安装等基础功能
 */
object ConfigUtils {

    private const val TAG = "ConfigUtils"

    // 配置文件名常量
    private const val APP_FEATURES_FILE = "com.oplus.app-features.xml"
    private const val OPLUS_FEATURES_FILE = "com.oplus.oplus-feature.xml"
    private const val METADATA_FILE = "metadata.json"

    // 配置目录路径
    private val baseDir = app.getExternalFilesDir(null)?.absolutePath ?: ""
    private val configsDir = "$baseDir/configs"
    private val systemBaselineDir = "$configsDir/system_baseline"
    private val userPatchesDir = "$configsDir/user_patches"
    private val mergedOutputDir = "$configsDir/merged_output"

    @Serializable
    data class ConfigMetadata(
        val systemVersion: String,
        val lastUpdate: Long,
        val appFeaturesHash: String? = null,
        val oplusFeaturesHash: String? = null
    )

    /**
     * 初始化新架构的配置管理系统
     * 创建必要的目录结构，但不复制任何配置文件
     * 配置文件的复制由模块脚本负责
     */
    fun initializeConfigSystem(): Boolean {
        try {
            Log.i(TAG, "开始初始化配置管理系统")

            // 创建目录结构
            val success = createDirectoryStructure()
            if (!success) {
                Log.e(TAG, "创建目录结构失败")
                return false
            }

            // 初始化元数据文件（如果不存在）
            initializeMetadata()

            Log.i(TAG, "配置管理系统初始化完成")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "配置系统初始化失败", e)
            return false
        }
    }

    /**
     * 创建新架构的目录结构
     */
    private fun createDirectoryStructure(): Boolean {
        val mediaDirBase = baseDir.replace("/storage/emulated/0", "/data/media/0")
        val mediaConfigsDir = "$mediaDirBase/configs"

        val shellCmd = StringBuilder().apply {
            append("mkdir -p \"$mediaConfigsDir/system_baseline\" && ")
            append("mkdir -p \"$mediaConfigsDir/user_patches\" && ")
            append("mkdir -p \"$mediaConfigsDir/merged_output\" && ")
            append("chmod -R 777 \"$mediaDirBase\"")
        }

        CSU.runWithSu(shellCmd.toString())

        // 验证目录是否创建成功
        return CSU.dirExists("$mediaConfigsDir/system_baseline") &&
               CSU.dirExists("$mediaConfigsDir/user_patches") &&
               CSU.dirExists("$mediaConfigsDir/merged_output")
    }

    /**
     * 初始化元数据文件
     */
    private fun initializeMetadata() {
        val metadataFile = File(systemBaselineDir, METADATA_FILE)
        if (!metadataFile.exists()) {
            val metadata = ConfigMetadata(
                systemVersion = "unknown",
                lastUpdate = System.currentTimeMillis()
            )

            val json = Json { prettyPrint = true }
            metadataFile.writeText(json.encodeToString(metadata))
            Log.i(TAG, "创建初始元数据文件")
        }
    }

    /**
     * 检查系统基线配置是否存在
     */
    fun hasSystemBaseline(): Boolean {
        return File(systemBaselineDir, APP_FEATURES_FILE).exists() &&
               File(systemBaselineDir, OPLUS_FEATURES_FILE).exists()
    }

    /**
     * 检查合并输出配置是否存在
     */
    fun hasMergedOutput(): Boolean {
        return File(mergedOutputDir, APP_FEATURES_FILE).exists() &&
               File(mergedOutputDir, OPLUS_FEATURES_FILE).exists()
    }

    /**
     * 获取配置目录路径
     */
    fun getConfigPaths(): ConfigPaths {
        return ConfigPaths(
            configsDir = configsDir,
            systemBaselineDir = systemBaselineDir,
            userPatchesDir = userPatchesDir,
            mergedOutputDir = mergedOutputDir,
            appFeaturesFile = APP_FEATURES_FILE,
            oplusFeaturesFile = OPLUS_FEATURES_FILE
        )
    }

    data class ConfigPaths(
        val configsDir: String,
        val systemBaselineDir: String,
        val userPatchesDir: String,
        val mergedOutputDir: String,
        val appFeaturesFile: String,
        val oplusFeaturesFile: String
    )

    /**
     * 安装模块（简化版）
     * 从 assets 解压模块到指定目录
     */
    fun installModule(): Boolean {
        try {
            Log.i(TAG, "开始安装模块")
            val destDirPath = "/data/adb/modules/ColorOSFeaturesEnhance"

            // 临时工作目录
            val tempDir = File(app.cacheDir, "moduleTemp").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            // 解压模块文件
            val tempZip = File(tempDir, "mod.zip")
            if (!extractModuleAssets(tempZip)) {
                Log.e(TAG, "解压模块资源失败")
                return false
            }

            // 安装到目标目录
            val extractedDir = File(tempDir, "mod")
            if (!extractedDir.exists()) {
                Log.e(TAG, "解压后的模块目录不存在")
                return false
            }

            val shellCmd = StringBuilder().apply {
                append("mkdir -p \"$destDirPath\" && ")
                append("cp -r \"${extractedDir.absolutePath}/.\" \"$destDirPath/\" && ")
                append("chmod -R 755 \"$destDirPath\"")
            }

            CSU.runWithSu(shellCmd.toString())

            val success = CSU.fileExists("$destDirPath/module.prop")
            if (success) {
                Log.i(TAG, "模块安装成功")
            } else {
                Log.e(TAG, "模块安装失败")
            }

            return success

        } catch (e: Exception) {
            Log.e(TAG, "模块安装过程中发生异常", e)
            return false
        }
    }

    /**
     * 从 assets 提取模块文件
     */
    private fun extractModuleAssets(tempZip: File): Boolean {
        return try {
            app.assets.open("mod.zip").use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            }
            unzip(tempZip, tempZip.parentFile!!)
        } catch (e: Exception) {
            Log.e(TAG, "提取模块资源失败", e)
            false
        }
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

    /**
     * 检查模块是否已安装
     */
    fun isModuleInstalled(): Boolean {
        return CSU.dirExists("/data/adb/modules/ColorOSFeaturesEnhance") &&
               CSU.fileExists("/data/adb/modules/ColorOSFeaturesEnhance/module.prop")
    }

    /**
     * 获取模块版本信息
     */
    fun getModuleVersion(): String? {
        val modulePropsPath = "/data/adb/modules/ColorOSFeaturesEnhance/module.prop"
        if (!CSU.fileExists(modulePropsPath)) return null

        return try {
            val output = CSU.runWithSu("grep '^version=' \"$modulePropsPath\" | cut -d'=' -f2")
            output.trim().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "获取模块版本失败", e)
            null
        }
    }

    /**
     * 将merged_output的配置复制到模块挂载目录
     * 每次保存配置后调用此方法
     */
    fun copyMergedConfigToModule(): Boolean {
        try {
            Log.i(TAG, "开始复制合并配置到模块目录")

            val configPaths = getConfigPaths()
            Log.d(TAG, "配置路径: mergedOutputDir=${configPaths.mergedOutputDir}")

            val moduleBase = "/data/adb/modules/ColorOSFeaturesEnhance"
            val moduleDirs = listOf(
                "$moduleBase/my_product/etc/extension",
                "$moduleBase/anymount/my_product/etc/extension"
            )

            val shellCmd = StringBuilder()

            // 创建模块目录
            moduleDirs.forEach { dir ->
                shellCmd.append("mkdir -p \"$dir\" && ")
            }

            var hasFilesToCopy = false

            // 复制app-features.xml
            val appFeaturesSource = "${configPaths.mergedOutputDir}/${configPaths.appFeaturesFile}"
            Log.d(TAG, "检查源文件: $appFeaturesSource")
            if (File(appFeaturesSource).exists()) {
                Log.i(TAG, "源文件存在，准备复制 app-features.xml")
                moduleDirs.forEach { dir ->
                    shellCmd.append("cp \"$appFeaturesSource\" \"$dir/\" && ")
                }
                hasFilesToCopy = true
            } else {
                Log.w(TAG, "源文件不存在: $appFeaturesSource")
            }

            // 复制oplus-feature.xml
            val oplusFeaturesSource = "${configPaths.mergedOutputDir}/${configPaths.oplusFeaturesFile}"
            Log.d(TAG, "检查源文件: $oplusFeaturesSource")
            if (File(oplusFeaturesSource).exists()) {
                Log.i(TAG, "源文件存在，准备复制 oplus-feature.xml")
                moduleDirs.forEach { dir ->
                    shellCmd.append("cp \"$oplusFeaturesSource\" \"$dir/\" && ")
                }
                hasFilesToCopy = true
            } else {
                Log.w(TAG, "源文件不存在: $oplusFeaturesSource")
            }

            if (!hasFilesToCopy) {
                Log.e(TAG, "没有找到任何需要复制的文件")
                return false
            }

            // 移除最后的 &&
            val finalCmd = shellCmd.toString().removeSuffix(" && ")
            Log.d(TAG, "执行Shell命令: $finalCmd")

            if (finalCmd.isNotEmpty()) {
                val result = CSU.runWithSu(finalCmd)
                Log.d(TAG, "Shell命令执行结果: $result")
            } else {
                Log.w(TAG, "没有Shell命令需要执行")
            }

            // 验证复制是否成功
            var successCount = 0
            moduleDirs.forEach { dir ->
                val appExists = CSU.fileExists("$dir/${configPaths.appFeaturesFile}")
                val oplusExists = CSU.fileExists("$dir/${configPaths.oplusFeaturesFile}")
                Log.d(TAG, "目录 $dir: app-features存在=$appExists, oplus-feature存在=$oplusExists")
                if (appExists || oplusExists) {
                    successCount++
                }
            }

            val success = successCount > 0
            if (success) {
                Log.i(TAG, "配置文件复制到模块目录成功 ($successCount/${moduleDirs.size} 个目录)")
            } else {
                Log.e(TAG, "配置文件复制到模块目录失败，所有目录都没有文件")
            }

            return success

        } catch (e: Exception) {
            Log.e(TAG, "复制配置到模块目录时发生异常", e)
            return false
        }
    }
}