package com.itosfish.colorfeatureenhance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.itosfish.colorfeatureenhance.ui.theme.ColorFeatureEnhanceTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itosfish.colorfeatureenhance.navigation.BottomNavigationBar
import com.itosfish.colorfeatureenhance.navigation.BottomNavItem
import com.itosfish.colorfeatureenhance.ui.FeatureConfigScreen
import com.itosfish.colorfeatureenhance.ui.AboutScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorFeatureEnhanceTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.FeatureConfig.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(BottomNavItem.FeatureConfig.route) {
                            FeatureConfigScreen()
                        }
                        composable(BottomNavItem.About.route) {
                            AboutScreen()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ColorFeatureEnhanceTheme {
        FeatureConfigScreen()
    }
}