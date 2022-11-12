package com.kartollika.walkietalkie

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kartollika.walkietalkie.Connected.WalkieMode.IDLE

sealed class WalkieTalkieState

data class Idle(
  val bluetoothEnabled: () -> Boolean
) : WalkieTalkieState() {
}

@Stable
object Searching : WalkieTalkieState() {
  var devices by mutableStateOf(setOf<BluetoothDevice>())
}

object Listening : WalkieTalkieState()

object Connected: WalkieTalkieState() {

  var walkieMode: WalkieMode = IDLE

  enum class WalkieMode {
    LISTENING,
    SPEAKING,
    IDLE
  }
}