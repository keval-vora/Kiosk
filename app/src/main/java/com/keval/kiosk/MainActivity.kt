package com.keval.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.keval.kiosk.ui.theme.KioskTheme

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
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier.clickable(enabled = true, onClick = { 5/0 })
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KioskTheme {
        Greeting("Android")
    }
}