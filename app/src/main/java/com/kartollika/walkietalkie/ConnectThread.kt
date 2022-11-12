package com.kartollika.walkietalkie

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.annotation.SuppressLint
import android.util.Log
import java.io.IOException
import java.util.UUID

class ConnectThread : Thread() {
  private var bDevice: BluetoothDevice? = null

  var socket: BluetoothSocket? = null
    private set

  @SuppressLint("MissingPermission")
  fun connect(device: BluetoothDevice?, UUID: UUID?): Boolean {
    try {
      socket = device!!.createRfcommSocketToServiceRecord(UUID)
    } catch (e: IOException) {
      Log.d("CONNECT", "Failed at create RFCOMM")
      return false
    }
    if (socket == null) {
      return false
    }
    try {
      socket!!.connect()
    } catch (e: IOException) {
      Log.d("CONNECT", "Failed at socket connect")
      try {
        socket!!.close()
      } catch (close: IOException) {
        Log.d("CONNECT", "Failed at socket close")
      }
      return false
    }
    return true
  }

  fun closeConnect(): Boolean {
    try {
      socket!!.close()
    } catch (e: IOException) {
      Log.d("CONNECT", "Failed at socket close")
      return false
    }
    return true
  }
}