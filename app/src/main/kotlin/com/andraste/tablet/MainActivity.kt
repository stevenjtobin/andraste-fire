package com.andraste.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.andraste.tablet.nav.NavGraph
import com.andraste.tablet.ui.theme.AndrasterTheme
import com.andraste.tablet.ui.theme.IceniDeepGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AndrasteRoot() }
    }
}

@Composable
private fun AndrasteRoot() {
    AndrasterTheme {
        Surface(
            modifier = Modifier.fillMaxSize().background(IceniDeepGreen),
            color = IceniDeepGreen
        ) {
            val nav = rememberNavController()
            NavGraph(nav)
        }
    }
}
