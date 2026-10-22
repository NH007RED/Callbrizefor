package com.nh.callbrize

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nh.callbrize.ui.AppRoot
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.CallbrizeTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallbrizeTheme { AppRoot(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.onBackground()
    }
}
