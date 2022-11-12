package com.kartollika.walkietalkie

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartollika.walkietalkie.Connected.WalkieMode
import com.kartollika.walkietalkie.Connected.WalkieMode.IDLE
import com.kartollika.walkietalkie.Connected.WalkieMode.LISTENING
import com.kartollika.walkietalkie.Connected.WalkieMode.SPEAKING
import com.kartollika.walkietalkie.uuid.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WalkieTalkieViewModel @Inject constructor(
  private val bluetoothAdapter: BluetoothAdapter
) : ViewModel() {

  private val connectThread = ConnectThread()
  private val listenThread = ListenThread()
  private var micRecorder: MicRecorder? = null

  private val bluetoothEnabled = { bluetoothAdapter.isEnabled }

  @SuppressLint("MissingPermission")
  fun startDiscovery() {
    if (!bluetoothAdapter.isEnabled) return
    viewModelScope.launch {
      val state = Searching
      _walkieTalkieState.tryEmit(state)
      withContext(Dispatchers.Default) {
        bluetoothAdapter.cancelDiscovery()
        state.devices = bluetoothAdapter.bondedDevices
        bluetoothAdapter.startDiscovery()
      }
    }
  }

  fun addDevice(bluetoothDevice: BluetoothDevice) {
    val talkieState = walkieTalkieState.value
    if (talkieState is Searching) {
      talkieState.devices = talkieState.devices.toMutableSet().apply {
        add(bluetoothDevice)
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun listenForConnections() {
    viewModelScope.launch {
      _walkieTalkieState.tryEmit(Listening)
      withContext(Dispatchers.Default) {
        bluetoothAdapter.cancelDiscovery()
        val connectionSuccessful =
          listenThread.acceptConnect(bluetoothAdapter, UUID.UUID_CHANNEL, LISTEN_TIMEOUT)

        if (connectionSuccessful) {
          _walkieTalkieState.tryEmit(Connected)
          Log.d("Bluetooth", "connection successful")
          initConnection(listenThread.socket!!)
        } else {
          Log.d("Bluetooth", "connection unsuccessful")
          _walkieTalkieState.tryEmit(Idle(bluetoothEnabled))
        }
      }
    }
  }

  fun closeConnection() {
    _walkieTalkieState.tryEmit(Idle(bluetoothEnabled))
  }

  @SuppressLint("MissingPermission") fun connectToDevice(bluetoothDevice: BluetoothDevice) {
    viewModelScope.launch {
      withContext(Dispatchers.Default) {
        bluetoothAdapter.cancelDiscovery()
        val connectionSuccessful = connectThread.connect(bluetoothDevice, UUID.UUID_CHANNEL)
        if (connectionSuccessful) {
          _walkieTalkieState.tryEmit(Connected)
          initConnection(connectThread.socket!!)
        } else {
          Log.d("Bluetooth", "connection unsuccessful")
        }
      }
    }
  }

  private fun initConnection(socket: BluetoothSocket) {
    SocketHolder.socket = socket
  }

  fun startSpeaking() {
    updateWalkieMode(SPEAKING)
    micRecorder = MicRecorder()
    micRecorder?.keepRecording = true
    Thread(micRecorder).start()
  }

  fun idleWalkie() {
    updateWalkieMode(IDLE)
    micRecorder?.keepRecording = false
  }

  fun startListening() {
    updateWalkieMode(LISTENING)
    micRecorder?.keepRecording = false
  }

  private fun updateWalkieMode(walkieMode: WalkieMode) {
    (walkieTalkieState.value as Connected).walkieMode = walkieMode
  }

  private val _walkieTalkieState: MutableStateFlow<WalkieTalkieState> = MutableStateFlow(Idle(bluetoothEnabled))
  val walkieTalkieState = _walkieTalkieState.asStateFlow()

  companion object {
    private const val LISTEN_TIMEOUT = 10_000
  }
}