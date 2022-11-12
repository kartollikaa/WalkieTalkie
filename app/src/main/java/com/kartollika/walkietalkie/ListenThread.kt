package com.kartollika.walkietalkie

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.UUID

class ListenThread {
  var socket: BluetoothSocket? = null
    private set

  @SuppressLint("MissingPermission")
  fun acceptConnect(adapter: BluetoothAdapter, mUUID: UUID?, timeout: Int): Boolean {
    var temp: BluetoothServerSocket? = null
    try {
      temp = adapter.listenUsingRfcommWithServiceRecord("BTService", mUUID)
    } catch (e: IOException) {
      Log.d("LISTEN", "Error at listen using RFCOMM")
    }
    try {
      socket = temp!!.accept(timeout)
    } catch (e: IOException) {
      Log.d("LISTEN", "Error at accept connection")
    }
    if (socket != null) {
      try {
        temp!!.close()
      } catch (e: IOException) {
        Log.d("LISTEN", "Error at socket close")
      }
      return true
    }
    return false
  }

  fun closeConnect(): Boolean {
    try {
      socket!!.close()
    } catch (e: IOException) {
      Log.d("LISTEN", "Failed at socket close")
      return false
    }
    return true
  }
}