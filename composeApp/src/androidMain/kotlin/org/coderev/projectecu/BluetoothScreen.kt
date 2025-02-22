package org.coderev.projectecu

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.control.PendingTroubleCodesCommand
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(onNavigateToScan: () -> Unit) {
    val context = LocalContext.current
    val pairedDevices = remember { getPairedDevices(context) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Paired Devices:", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(pairedDevices) { device ->
                Text(device.name ?: "Unknown Device")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToScan) {
            Text("Scan Nearby Devices")
        }
    }
}

@Suppress("UNREACHABLE_CODE")
@SuppressLint("MissingPermission")
@Composable
fun ScanDevicesScreen() {
    val context = LocalContext.current
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val bluetoothReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (discoveredDevices.none { it.address == device.address }) {
                                discoveredDevices.add(device)
                            }
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Toast.makeText(context, "Scan completed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
        onDispose { context.unregisterReceiver(bluetoothReceiver) }
    }

    Row {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Available Devices:", fontWeight = FontWeight.Bold)
            LazyColumn {
                items(discoveredDevices) { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${device.name} \n ${device.address}")
                        Button(onClick = { connectToDevice(device, context) }) {
                            Text("Connect")
                        }
                    }


                }
            }
            Spacer(modifier = Modifier.height(16.dp))

        }
        Button(onClick = {
            startScan(context, bluetoothAdapter, discoveredDevices)
        }) {
            Text("Start Scanning")
        }
    }
}

fun connectToDevice(device: BluetoothDevice, context: Context) {
    val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP
    val socket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(uuid)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            socket?.let {
                if (!it.isConnected) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@launch
                    }
                    it.connect() // Try to connect
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                Log.e("RPM CHECK", getRPM(it))
                Log.e("DTC CHECK", getStoredDTCs(it))

            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
            try {
                socket?.close() // Close socket if connection fails
            } catch (closeException: IOException) {
                closeException.printStackTrace()
            }
        }
    }
}

fun requestBluetoothPermissions(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(
                context,
                "android.permission.BLUETOOTH_SCAN"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT"
                ),
                1
            )
            return
        }
    }
}

fun startScan(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    discoveredDevices: MutableList<BluetoothDevice>
) {

    if (bluetoothAdapter?.isEnabled == false) {
        Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
        return
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            "android.permission.BLUETOOTH_SCAN"
        ) != PackageManager.PERMISSION_GRANTED

        val hasConnectPermission = ContextCompat.checkSelfPermission(
            context,
            "android.permission.BLUETOOTH_CONNECT"
        ) != PackageManager.PERMISSION_GRANTED

        if (hasScanPermission || hasConnectPermission) {
            Toast.makeText(context, "No permission", Toast.LENGTH_SHORT).show()
            return
        }
    }

    if (bluetoothAdapter?.isDiscovering == true) {
        bluetoothAdapter.cancelDiscovery()
    }
    discoveredDevices.clear()

    bluetoothAdapter?.startDiscovery()
    Toast.makeText(context, "Scanning for Devices...", Toast.LENGTH_SHORT).show()
}

@SuppressLint("MissingPermission")
fun getPairedDevices(context: Context): List<BluetoothDevice> {

    // Check permissions for Android 12+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            "android.permission.BLUETOOTH_SCAN"
        ) == PackageManager.PERMISSION_GRANTED

        val hasConnectPermission = ContextCompat.checkSelfPermission(
            context,
            "android.permission.BLUETOOTH_CONNECT"
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasScanPermission || !hasConnectPermission) {
            // Request permissions in your Android UI before calling startScan
            return emptyList()
        }
    }

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
}

private suspend fun executeObdCommand(socket: BluetoothSocket?, command: ObdCommand): String {
    return withContext(Dispatchers.IO) {
        try {
            socket?.outputStream?.let { command.run(socket.inputStream, it) }
            command.formattedResult
        } catch (e: Exception) {
            e.printStackTrace()
            "Error executing command"
        }
    }
}

suspend fun getRPM(socket: BluetoothSocket?): String {
    return executeObdCommand(socket, RPMCommand())
}

suspend fun getStoredDTCs(socket: BluetoothSocket?): String {
    return executeObdCommand(socket, TroubleCodesCommand())
}

suspend fun getPendingDTCs(socket: BluetoothSocket?): String {
    return executeObdCommand(socket, PendingTroubleCodesCommand())
}

suspend fun clearDTCs(socket: BluetoothSocket?) {
    executeObdCommand(socket, ResetTroubleCodesCommand())
}
