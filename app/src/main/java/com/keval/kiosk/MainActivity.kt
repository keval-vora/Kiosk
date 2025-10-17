package com.keval.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import com.keval.kiosk.ui.theme.KioskTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    private val packages = listOf(
        "com.keval.kiosk",
        "com.google.android.apps.messaging",
        "com.google.android.dialer"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = KioskDeviceAdminReceiver.getComponentName(this)

        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            devicePolicyManager.setLockTaskPackages(adminComponentName, packages.toTypedArray())
            devicePolicyManager.setPackagesSuspended(
                adminComponentName,
                arrayOf("com.android.launcher3"),
                true
            )

            val intentFilter = IntentFilter(android.content.Intent.ACTION_MAIN)
            intentFilter.addCategory(android.content.Intent.CATEGORY_HOME)
            intentFilter.addCategory(android.content.Intent.CATEGORY_DEFAULT)

            devicePolicyManager.addPersistentPreferredActivity(
                adminComponentName, intentFilter, ComponentName(this, ".Launcher")
            )

            startLockTask()
        }

        setContent {
            KioskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Greeting("Android")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::devicePolicyManager.isInitialized && devicePolicyManager.isDeviceOwnerApp(packageName)) {
            // Ensure we re-enter lock task after returning from installer UI
            startLockTask()
        }
    }

    fun updateApp() {
        val file = File(this.filesDir, "app-debug.apk")
        if (!file.exists()) {
            Toast.makeText(this, "APK not found in filesDir", Toast.LENGTH_SHORT).show()
            return
        }

        val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        if (!canInstall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Leave lock task to allow user to grant permission and install
            if (this::devicePolicyManager.isInitialized && devicePolicyManager.isDeviceOwnerApp(packageName)) {
                try { stopLockTask() } catch (_: Exception) {}
            }
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(settingsIntent)
            Toast.makeText(this, "Please grant permission to install updates", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.provider",
            file
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Pause lock task to allow Package Installer to appear
        if (this::devicePolicyManager.isInitialized && devicePolicyManager.isDeviceOwnerApp(packageName)) {
            try { stopLockTask() } catch (_: Exception) {}
        }

        try {
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to start installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column {
        Button(onClick = { Toast.makeText(context, "Crashing $name", Toast.LENGTH_SHORT).show()
        5/0}) {
            Text(
                text = "No crash $name! v${BuildConfig.VERSION_NAME}"
            )
        }
        Button(onClick = {
            val activity = (context as? MainActivity)
            if (activity != null) {
                activity.updateApp()
            } else {
                Toast.makeText(context, "Unable to access activity", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text(
                text = "Update app"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KioskTheme {
        Greeting("Android")
    }
}