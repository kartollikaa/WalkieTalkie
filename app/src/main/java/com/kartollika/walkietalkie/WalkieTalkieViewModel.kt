package com.kartollika.walkietalkie

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartollika.walkietalkie.Connected.WalkieMode
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DeviceConnected
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DeviceDisconnected
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DeviceDiscovered
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DiscoveryStarted
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DiscoveryStopped
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DistanceChanged
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.Error
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.ListenForConnections
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class WalkieTalkieViewModel @Inject constructor(
  private val bluetoothActionsDataSource: BluetoothActionsDataSource
) : ViewModel() {

  private var micRecorder: MicRecorder? = null
  private val _walkieTalkieState: MutableStateFlow<WalkieTalkieState> = MutableStateFlow(Idle)
  val walkieTalkieState = _walkieTalkieState.asStateFlow()

  init {
    bluetoothActionsDataSource.bluetoothActions
      .onEach { action ->
        when (action) {
          is DeviceConnected -> {
            onDeviceConnected()
          }
          DeviceDisconnected -> {}
          is DeviceDiscovered -> {
            addDevice(action.device)
          }
          DiscoveryStarted -> {
            onDiscoveryStarted()
          }
          DiscoveryStopped -> {
            goIdle()
          }
          is Error -> {
            onError(action)
          }
          is ListenForConnections -> {
            listenForConnections()
          }
          is DistanceChanged -> {
            changeDistance(action.distance)
          }
        }
      }
      .launchIn(viewModelScope)
  }

  private fun changeDistance(distance: Double) {
    (walkieTalkieState.value as? Connected)?.distance = distance
  }

  private fun onError(error: Error) {
    _walkieTalkieState.tryEmit(Idle)
  }

  private fun onDeviceConnected() {
    _walkieTalkieState.tryEmit(Connected)
  }

  private fun onDiscoveryStarted() {
    _walkieTalkieState.tryEmit(Searching())
  }

  private fun goIdle() {
    _walkieTalkieState.tryEmit(Idle)
  }

  private fun addDevice(bluetoothDevice: BluetoothDevice?) {
    bluetoothDevice ?: return

    val talkieState = walkieTalkieState.value
    if (talkieState is Searching) {
      val bluetoothDevices = talkieState.devices.toMutableSet()
      if (!bluetoothDevices.map { it.address }.contains(bluetoothDevice.address)) {
        bluetoothDevices.add(bluetoothDevice)
      } else {
        bluetoothDevices.remove(bluetoothDevices.first { it.address == bluetoothDevice.address })
        bluetoothDevices.add(bluetoothDevice)
      }
      talkieState.devices = bluetoothDevices
    }
  }

  private fun listenForConnections() {
    _walkieTalkieState.tryEmit(Listening)
  }

  private fun closeConnection() {
    _walkieTalkieState.tryEmit(Idle)
  }

  private fun updateWalkieMode(walkieMode: WalkieMode) {
    (walkieTalkieState.value as Connected).walkieTalkieMode = walkieMode
  }
}