package com.itosfish.colorfeatureenhance

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.data.repository.XmlOplusFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.ui.FeatureConfigScreen
import com.itosfish.colorfeatureenhance.ui.theme.ColorFeatureEnhanceTheme
import com.itosfish.colorfeatureenhance.utils.CSU
import com.itosfish.colorfeatureenhance.utils.ConfigUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = this
        setContent {
            ColorFeatureEnhanceTheme {
                var currentMode by remember { mutableStateOf(FeatureMode.APP) }

                val configPath = when (currentMode) {
                    FeatureMode.APP -> getExternalFilesDir(null)?.absolutePath + "/com.oplus.app-features.xml"
                    FeatureMode.OPLUS -> getExternalFilesDir(null)?.absolutePath + "/com.oplus.oplus-feature.xml"
                }

                // 根据模式记忆 repository，切换模式时重建
                val repository: FeatureRepository = remember(currentMode) {
                    when (currentMode) {
                        FeatureMode.APP -> XmlFeatureRepository()
                        FeatureMode.OPLUS -> XmlOplusFeatureRepository()
                    }
                }

                Toast.makeText(this, "配置文件路径: $configPath", Toast.LENGTH_SHORT).show()

                FeatureConfigScreen(
                    configPath = configPath,
                    currentMode = currentMode,
                    onModeChange = { currentMode = it },
                    repository = repository
                )
            }
        }
        CSU.checkRoot()

        // 如果检测到原版 KernelSU，则提示不支持并跳过安装
        if (CSU.isKSU()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ksu_not_supported_title)
                .setMessage(R.string.ksu_not_supported_message)
                .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        // 如果目录已存在，则视为已安装
        if (!CSU.dirExists("/data/adb/modules/ColorOSFeaturesEnhance")) {
            val a = ConfigUtils.installModule()
            if (a) {
                Toast.makeText(app, app.getString(R.string.module_install_success), Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.module_install_fail_title)
                    .setMessage(R.string.module_install_fail_message)
                    .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                Log.e("MainActivity", "Module installation failed.")
            }
        }

        ConfigUtils.copySystemConfig()

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