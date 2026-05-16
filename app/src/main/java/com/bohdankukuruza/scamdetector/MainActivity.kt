package com.bohdankukuruza.scamdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bohdankukuruza.scamdetector.ui.screen.DetectorScreen
import com.bohdankukuruza.scamdetector.ui.theme.NortonScamDetectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NortonScamDetectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DetectorScreen()
                }
            }
        }
    }
}