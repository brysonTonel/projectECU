package org.coderev.projectecu

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.control.PendingTroubleCodesCommand
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppAndroidPreview()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        AppNavigator()
    }
}

@Composable
fun DashboardScreen(onNavigateToBluetooth: () -> Unit) {
    val context = LocalContext.current
    requestBluetoothPermissions(context)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dashboard", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToBluetooth) {
            Text("Go to Bluetooth Screen")
        }

    }
}

@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    when (currentScreen) {
        "dashboard" -> DashboardScreen { currentScreen = "bluetooth" }
        "bluetooth" -> BluetoothScreen { currentScreen = "scan" }
        "scan" -> ScanDevicesScreen()
    }
}



