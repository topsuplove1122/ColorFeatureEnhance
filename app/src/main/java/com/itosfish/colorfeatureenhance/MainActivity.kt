package com.itosfish.colorfeatureenhance

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itosfish.colorfeatureenhance.config.ConfigMergeManager
import com.itosfish.colorfeatureenhance.data.remote.RemoteConfigManager
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.data.repository.XmlOplusFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.ui.DisclaimerScreen
import com.itosfish.colorfeatureenhance.ui.FeatureConfigScreen
import com.itosfish.colorfeatureenhance.ui.theme.ColorFeatureEnhanceTheme
import com.itosfish.colorfeatureenhance.utils.CLog
import com.itosfish.colorfeatureenhance.utils.CSU
import com.itosfish.colorfeatureenhance.utils.ConfigUtils
import com.itosfish.colorfeatureenhance.utils.DisclaimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = this

        // 检查是否需要显示免责声明
        val disclaimerManager = DisclaimerManager.getInstance(this)

        setContent {
            ColorFeatureEnhanceTheme {
                var showDisclaimer by remember { mutableStateOf(disclaimerManager.shouldShowDisclaimer()) }

                if (showDisclaimer) {
                    // 显示免责声明
                    DisclaimerScreen(
                        onAccepted = {
                            showDisclaimer = false
                            CLog.i("MainActivity", "用户已同意免责声明，继续应用启动")
                            // 用户同意后执行应用初始化
                            initializeApp()
                        },
                        onExit = {
                            CLog.i("MainActivity", "用户拒绝免责声明，退出应用")
                            finish()
                        }
                    )
                } else {
                    // 显示主界面
                    var currentMode by remember { mutableStateOf(FeatureMode.APP) }

                    // 使用新架构的配置路径（从merged_output读取）
                    val configPaths = ConfigUtils.getConfigPaths()
                    val configPath = when (currentMode) {
                        FeatureMode.APP -> "${configPaths.mergedOutputDir}/${configPaths.appFeaturesFile}"
                        FeatureMode.OPLUS -> "${configPaths.mergedOutputDir}/${configPaths.oplusFeaturesFile}"
                    }

                    // 根据模式记忆 repository，切换模式时重建
                    val repository: FeatureRepository = remember(currentMode) {
                        when (currentMode) {
                            FeatureMode.APP -> XmlFeatureRepository()
                            FeatureMode.OPLUS -> XmlOplusFeatureRepository()
                        }
                    }

                    FeatureConfigScreen(
                        configPath = configPath,
                        currentMode = currentMode,
                        onModeChange = { currentMode = it },
                        repository = repository
                    )
                }
            }
        }

        // 如果用户已经同意过免责声明，直接初始化应用
        if (!disclaimerManager.shouldShowDisclaimer()) {
            initializeApp()
        }
    }

    /**
     * 初始化应用（在用户同意免责声明后执行）
     */
    private fun initializeApp() {
        CSU.checkRoot()

        // 如果检测到 Overlayfs，则提示不支持并跳过安装
        if (CSU.isOverlayfs()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ksu_not_supported_title)
                .setMessage(R.string.ksu_not_supported_message)
                .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        // 检查并安装模块
        if (!ConfigUtils.isModuleInstalled()) {
            val installSuccess = ConfigUtils.installModule()
            if (installSuccess) {
                Toast.makeText(
                    app,
                    app.getString(R.string.module_install_success),
                    Toast.LENGTH_SHORT
                ).show()
                CLog.i("MainActivity", "模块安装成功")
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.module_install_fail_title)
                    .setMessage(R.string.module_install_fail_message)
                    .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                CLog.e("MainActivity", "模块安装失败")
            }
        }

        // 初始化新的配置管理系统
        val initSuccess = ConfigUtils.initializeConfigSystem()
        if (initSuccess) {
            CLog.i("MainActivity", "配置系统初始化成功")

            // 异步执行配置合并和云端配置更新
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // 1. 先执行配置合并
                    val mergeSuccess = ConfigMergeManager.performConfigMerge()
                    if (mergeSuccess) {
                        CLog.i("MainActivity", "配置合并完成")
                    } else {
                        CLog.w("MainActivity", "配置合并失败")
                    }

                    // 2. 异步检查云端配置更新（不阻塞主流程）
                    launch(Dispatchers.IO) {
                        try {
                            val remoteConfigManager =
                                RemoteConfigManager.getInstance(this@MainActivity)
                            val updateResult = remoteConfigManager.checkAndUpdateConfig()

                            when (updateResult) {
                                is RemoteConfigManager.UpdateResult.Success -> {
                                    CLog.i("MainActivity", "云端配置更新成功")
                                }

                                is RemoteConfigManager.UpdateResult.NoUpdate -> {
                                    CLog.d("MainActivity", "云端配置无需更新")
                                }

                                is RemoteConfigManager.UpdateResult.Error -> {
                                    CLog.w(
                                        "MainActivity",
                                        "云端配置更新失败: ${updateResult.message}"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            CLog.w("MainActivity", "云端配置更新过程中发生异常", e)
                        }
                    }

                } catch (e: Exception) {
                    CLog.e("MainActivity", "配置合并过程中发生异常", e)
                }
            }
        } else {
            CLog.e("MainActivity", "配置系统初始化失败")
        }

    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: MainActivity private set
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ColorFeatureEnhanceTheme {
        FeatureConfigScreen(
            configPath = "",
            currentMode = FeatureMode.APP,
            onModeChange = {},
            repository = XmlFeatureRepository()
        )
    }
}