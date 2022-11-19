package com.kartollika.feature.walkietalkie

import android.Manifest.permission
import android.os.Build

internal val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
  listOf(
    permission.BLUETOOTH,
    permission.BLUETOOTH_ADMIN,
    permission.BLUETOOTH_SCAN,
    permission.BLUETOOTH_CONNECT,
    permission.ACCESS_COARSE_LOCATION,
    permission.ACCESS_FINE_LOCATION,
    permission.RECORD_AUDIO,
    permission.BLUETOOTH_ADVERTISE
  )
} else {
  listOf(
    permission.BLUETOOTH,
    permission.BLUETOOTH_ADMIN,
    permission.ACCESS_COARSE_LOCATION,
    permission.ACCESS_FINE_LOCATION,
    permission.RECORD_AUDIO,
  )
}