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
import com.itosfish.colorfeatureenhance.utils.CSU
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import com.itosfish.colorfeatureenhance.data.repository.XmlFeatureRepository
import com.itosfish.colorfeatureenhance.data.repository.XmlOplusFeatureRepository
import com.itosfish.colorfeatureenhance.domain.FeatureRepository
import com.itosfish.colorfeatureenhance.ui.FeatureConfigScreen
import com.itosfish.colorfeatureenhance.ui.theme.ColorFeatureEnhanceTheme

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