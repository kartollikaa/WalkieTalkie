package com.kartollika.walkietalkie.bluetooth.thread

import com.kartollika.walkietalkie.bluetooth.BluetoothAction.Error
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConnectedThread(
  private val inputStream: InputStream,
  private val outputStream: OutputStream,
  private val dataSource: BluetoothActionsDataSource,
  private val onClose: () -> Unit
) : Thread() {

  override fun run() {
    var numBytes: Int
    while (!interrupted()) {
      // Keep alive
    }

//      pushBroadcastMessage(
//        BluetoothUtils.ACTION_MESSAGE_RECEIVED, mmSocket.remoteDevice, message
//      )
  }

  fun read(buffer: ByteArray) {
    try {
      val numBytes = inputStream.read(buffer)
    } catch (e: IOException) {
      dataSource.sendAction(Error(e, "Input stream was disconnected"))
    }
  }

  fun write(bytes: ByteArray) {
    try {
      outputStream.write(bytes)

      // Send to broadcast the message
//      pushBroadcastMessage(
//        BluetoothUtils.ACTION_MESSAGE_SENT, mmSocket.remoteDevice, null
//      )
    } catch (e: IOException) {
      dataSource.sendAction(Error(e, "Error occurred when sending data"))
    }
  }

  fun cancel() {
    try {
      onClose()
    } catch (e: IOException) {
      dataSource.sendAction(Error(e, "\"Could not close the connect socket\""))
    }
  }
}