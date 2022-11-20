package com.kartollika.walkietalkie.bluetooth

import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DistanceChanged
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class BluetoothActionsDataSourceImpl @Inject constructor(): BluetoothActionsDataSource {

  private var distanceMean = 0.0
  private var distanceCount = 0
  private var countForMean = COUNT_FOR_MEAN_DEFAULT

  private val _bluetoothActions = MutableSharedFlow<BluetoothAction>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  override val bluetoothActions = _bluetoothActions.asSharedFlow()

  override fun sendAction(action: BluetoothAction) {
    _bluetoothActions.tryEmit(action)
  }

  // dist = 10 ^ ((-69 -(<RSSI_VALUE>))/(10 * 2))
  override fun onRssiReceived(rssi: Int) {
    val powerRight = (BluetoothService.RSSI_MEASURED_POWER - rssi) / (10 * 2).toDouble()
    val distance = 10.toDouble().pow(powerRight)
    distanceMean += distance
    distanceCount++

    countForMean = if (rssi < -82) {
      COUNT_FOR_MEAN_LOW_SIGNAL
    } else {
      COUNT_FOR_MEAN_DEFAULT
    }

    if (distanceCount >= countForMean) {
      distanceMean /= distanceCount

      val decimal = BigDecimal(distanceMean).setScale(1, RoundingMode.HALF_EVEN)
      _bluetoothActions.tryEmit(DistanceChanged(decimal.toDouble()))

      distanceCount = 1
    }
  }

  companion object {
    private const val COUNT_FOR_MEAN_DEFAULT = 5
    private const val COUNT_FOR_MEAN_LOW_SIGNAL = 2
  }
}