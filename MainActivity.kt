package com.example.spaceseeker

import SpaceInvadersGame
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.spaceseeker.ui.theme.SpaceSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceSeekTheme {
                SpaceInvadersGame()
            }
        }
    }
}