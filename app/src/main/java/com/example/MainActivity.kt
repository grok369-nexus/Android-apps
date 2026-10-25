package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.service.NotificationHelper
import com.example.ui.AppScreen
import com.example.ui.DwellViewModel
import com.example.ui.screens.BlueprintScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PoiExplorerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DwellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra(NotificationHelper.EXTRA_ACTION)
        if (action == NotificationHelper.ACTION_VIEW_POIS) {
            viewModel.selectScreen(AppScreen.POIS)
        }
    }
}

@Composable
fun MainAppContent(viewModel: DwellViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Runtime Permission Launcher for Location & Notification
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            viewModel.refreshLiveGps(context)
            scope.launch {
                snackbarHostState.showSnackbar("Live GPS enabled! Tracking your live location.")
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        } else {
            // Already granted: query live GPS immediately
            viewModel.refreshLiveGps(context)
        }
    }

    // Handle back button on sub-screens to return to Radar
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            viewModel.selectScreen(AppScreen.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp
            ) {
                AppScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    val icon = when (screen) {
                        AppScreen.DASHBOARD -> Icons.Default.Radar
                        AppScreen.POIS -> Icons.Default.Explore
                        AppScreen.HISTORY -> Icons.Default.History
                        AppScreen.SETTINGS -> Icons.Default.Tune
                        AppScreen.BLUEPRINT -> Icons.Default.Architecture
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectScreen(screen) },
                        icon = { Icon(imageVector = icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.POIS -> PoiExplorerScreen(viewModel = viewModel)
                AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                AppScreen.BLUEPRINT -> BlueprintScreen()
            }
        }
    }
}
