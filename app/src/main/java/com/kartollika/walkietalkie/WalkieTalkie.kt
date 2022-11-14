package com.kartollika.walkietalkie

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission") @Composable fun WalkieTalkie(
  state: WalkieTalkieState,
  onConnect: () -> Unit = {},
  onListen: () -> Unit = {},
  onDisconnect: () -> Unit = {},
  connectToDevice: (BluetoothDevice) -> Unit = {},
  startSpeaking: () -> Unit = {},
  stopSpeaking: () -> Unit = {},
  permissionState: MultiplePermissionsState,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
    BluetoothControllers(
      modifier = Modifier.fillMaxWidth(),
      state = state,
      onListen = onListen,
      onConnect = onConnect,
      onDisconnect = onDisconnect,
      permissionState = permissionState
    )

    when (state) {
      is Idle, is Listening -> {}
      is Connected -> {
        Connected(
          state = state,
          modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
          startSpeaking = startSpeaking,
          stopSpeaking = stopSpeaking
        )
      }

      is Searching -> {
        BluetoothDevices(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          state = state,
          connectToDevice = connectToDevice
        )
      }
    }
  }
}

@Composable
private fun Connected(
  state: Connected,
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Center) {
    PushToTalk(
      modifier = Modifier.align(Center),
      startSpeaking = startSpeaking,
      stopSpeaking = stopSpeaking
    )

    Column(modifier = Modifier.align(BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = "Расстояние между устройствами:")
      Text(text = "≈ ${state.distance} m")
    }
  }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun PushToTalk(
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .size(232.dp)
      .padding(32.dp)
      .pointerInteropFilter { event ->
        when (event.action) {
          MotionEvent.ACTION_DOWN -> {
            startSpeaking()
          }
          MotionEvent.ACTION_UP -> {
            stopSpeaking()
          }
          else -> false
        }
        true
      },
    shape = CircleShape,
    color = Color.Cyan
  ) {
    Icon(
      modifier = Modifier.padding(24.dp),
      imageVector = Icons.Default.Mic,
      contentDescription = "Push to speak"
    )
  }
}

@Composable
private fun BluetoothDevices(
  state: Searching,
  connectToDevice: (BluetoothDevice) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(modifier = modifier) {
    items(state.devices.toList()) { device: BluetoothDevice ->
      BluetoothDevice(connectToDevice, device)
    }
  }
}

@SuppressLint("MissingPermission") @Composable private fun BluetoothDevice(
  connectToDevice: (BluetoothDevice) -> Unit, device: BluetoothDevice
) {
  Row(modifier = Modifier
    .fillMaxWidth()
    .clickable {
      connectToDevice(device)
    }
    .padding(16.dp)) {
    if (device.bondState == BOND_BONDED) {
      Icon(imageVector = Icons.Default.Link, contentDescription = "Bonded")
    } else {
      Icon(imageVector = Icons.Default.QuestionMark, contentDescription = "New")
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(text = device.name ?: device.address)
  }
}

@OptIn(ExperimentalPermissionsApi::class) @Composable private fun BluetoothControllers(
  modifier: Modifier = Modifier,
  state: WalkieTalkieState,
  onListen: () -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  permissionState: MultiplePermissionsState
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
    if (state !is Connected) {
      ProgressButton(
        loading = state is Searching,
        idleContent = {
          Text(text = "Connect")
        },
        onClick = {
          if (permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
            onConnect()
          } else {
            permissionState.launchMultiplePermissionRequest()
          }
        },
      )

      ProgressButton(
        loading = state is Listening,
        idleContent = {
          Text(text = "Listen")
        },
        onClick = onListen,
      )
    } else {
      Button(onClick = onDisconnect) {
        Text(text = "Disconnect")
      }
    }
  }
}