package org.coderev.projectecu

import android.bluetooth.BluetoothSocket
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.control.PendingTroubleCodesCommand
import com.github.pires.obd.commands.control.TroubleCodesCommand
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OBDCommand {

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


    suspend fun getStoredDTCs(socket: BluetoothSocket?): String {
        return executeObdCommand(socket, TroubleCodesCommand())
    }

    suspend fun getPendingDTCs(socket: BluetoothSocket?): String {
        return executeObdCommand(socket, PendingTroubleCodesCommand())
    }

    suspend fun clearDTCs(socket: BluetoothSocket?) {
        executeObdCommand(socket, ResetTroubleCodesCommand())
    }
}