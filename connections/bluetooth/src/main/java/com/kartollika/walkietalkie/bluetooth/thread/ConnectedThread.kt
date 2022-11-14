package com.kartollika.walkietalkie.bluetooth.thread

import com.kartollika.walkietalkie.bluetooth.BluetoothAction.Error
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import java.io.IOException
import java.io.InputStream

class ConnectedThread(
  private val inputStream: InputStream,
  private val dataSource: BluetoothActionsDataSource,
) : Thread() {

  private val buffer = ByteArray(1024)

  override fun run() {
    var numBytes: Int // bytes returned from read()

    // Keep listening to the InputStream until an exception occurs.
    while (true) {
      // Read from the InputStream.
      try {
        inputStream.read(buffer)
      } catch (e: IOException) {
        dataSource.sendAction(Error(e, "Input stream was disconnected"))
        break
      }
    }
  }
}