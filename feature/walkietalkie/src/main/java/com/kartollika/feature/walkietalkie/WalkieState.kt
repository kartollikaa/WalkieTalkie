package com.kartollika.feature.walkietalkie

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kartollika.feature.walkietalkie.Connected.WalkieMode.IDLE

sealed class WalkieTalkieState

object Idle: WalkieTalkieState() {
  var bluetoothEnabled by mutableStateOf(false)
}

class Searching : WalkieTalkieState() {
  var devices by mutableStateOf(setOf<BluetoothDevice>())
}

object Listening : WalkieTalkieState()

object Connected: WalkieTalkieState() {

  var distance by mutableStateOf(0.0)
  var walkieTalkieMode by mutableStateOf(IDLE)

  enum class WalkieMode {
    LISTENING,
    SPEAKING,
    IDLE
  }
}