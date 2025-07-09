package com.itosfish.colorfeatureenhance

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.itosfish.colorfeatureenhance.ui.theme.ColorFeatureEnhanceTheme
import com.itosfish.colorfeatureenhance.ui.FeatureConfigScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = this
        setContent {
            ColorFeatureEnhanceTheme {
                val configPath = getExternalFilesDir(null)?.absolutePath + "/com.oplus.app-features.xml"
                Toast.makeText(this, "配置文件路径: $configPath", Toast.LENGTH_LONG).show()
                FeatureConfigScreen(configPath = configPath)
            }
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
        FeatureConfigScreen(configPath = "")
    }
}