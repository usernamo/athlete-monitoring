package com.athlete.monitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.athlete.monitoring.data.ApiClient
import com.athlete.monitoring.ui.AppViewModel
import com.athlete.monitoring.ui.AuthScreen
import com.athlete.monitoring.ui.CoachShell
import com.athlete.monitoring.ui.MainShell
import com.athlete.monitoring.ui.theme.AthleteTheme
import com.athlete.monitoring.ui.theme.SportColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AthleteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SportColors.Background
                ) {
                    val vm: AppViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    val user = state.user
                    when {
                        user == null -> AuthScreen(vm)
                        user.role == "coach" -> CoachShell(vm)
                        else -> MainShell(vm)
                    }
                }
            }
        }
    }
}
