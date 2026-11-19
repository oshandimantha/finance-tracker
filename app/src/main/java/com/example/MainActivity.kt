package com.example

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.OshanFinanceApp
import com.example.ui.theme.OshanFinanceTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OshanFinanceApplication
        setContent {
            OshanFinanceTheme {
                val financeViewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModel.Factory(app.repository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.appPreferences)
                )

                val appLockEnabled by settingsViewModel.appLockEnabled.collectAsStateWithLifecycle()
                val privacyScreenEnabled by settingsViewModel.privacyScreenEnabled.collectAsStateWithLifecycle()
                val appName by settingsViewModel.appName.collectAsStateWithLifecycle()

                // Hide sensitive financial numbers in Recent Apps switcher & block screenshots
                LaunchedEffect(privacyScreenEnabled) {
                    if (privacyScreenEnabled) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                var isUnlocked by rememberSaveable { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // Re-lock whenever the app returns from background while App Lock is enabled
                DisposableEffect(lifecycleOwner, appLockEnabled) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP && appLockEnabled) {
                            isUnlocked = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    OshanFinanceApp(
                        viewModel = financeViewModel,
                        settingsViewModel = settingsViewModel
                    )
                    if (appLockEnabled && !isUnlocked) {
                        AppLockOverlay(
                            appName = appName,
                            onUnlocked = { isUnlocked = true }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun AppLockOverlay(
    appName: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        } else {
            errorMessage = "Authentication cancelled. Tap Unlock to try again."
        }
    }

    fun launchDeviceUnlock() {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard == null || !keyguard.isDeviceSecure) {
            // Device has no PIN/Pattern/Biometrics configured; unlock gracefully
            onUnlocked()
            return
        }
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock $appName",
            "Verify your identity to view your financial records"
        )
        if (intent != null) {
            errorMessage = null
            authLauncher.launch(intent)
        } else {
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) {
        launchDeviceUnlock()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.modern_finance_logo_1794928820205),
                    contentDescription = appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your financial records are locked for privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { launchDeviceUnlock() },
                    modifier = Modifier.testTag("unlock_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Unlock with PIN / Biometrics")
                }
            }
        }
    }
}
