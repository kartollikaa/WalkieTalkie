package com.kartollika.walkietalkie.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

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
}

interface BluetoothActionsDataSource {
  val bluetoothActions: SharedFlow<BluetoothAction>
  fun sendAction(action: BluetoothAction)
}

@Singleton
class BluetoothActionsDataSourceImpl @Inject constructor(): BluetoothActionsDataSource {

  private val _bluetoothActions = MutableSharedFlow<BluetoothAction>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  override val bluetoothActions = _bluetoothActions.asSharedFlow()

  override fun sendAction(action: BluetoothAction) {
    _bluetoothActions.tryEmit(action)
  }
}