package com.kartollika.walkietalkie.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.SharedFlow

sealed class BluetoothAction {
  object DiscoveryStarted: BluetoothAction()
  object DiscoveryStopped: BluetoothAction()
  object ListenForConnections: BluetoothAction()
  data class DeviceDiscovered(val device: BluetoothDevice?): BluetoothAction()
  data class DeviceConnected(val device: BluetoothDevice?): BluetoothAction()
  object DeviceDisconnected: BluetoothAction()
  data class Error(
    val exception: Exception? = null,
    val message: String? = null
  ): BluetoothAction()
  data class DistanceChanged(val distance: Double): BluetoothAction()
  object ReceivingVoiceStarted: BluetoothAction()
}

interface BluetoothActionsDataSource {
  val bluetoothActions: SharedFlow<BluetoothAction>

  fun sendAction(action: BluetoothAction)
  fun onRssiReceived(rssi: Int)
}