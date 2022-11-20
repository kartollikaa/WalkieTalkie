package com.kartollika.feature.walkietalkie

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun WalkieTalkieRoute(
  viewModel: WalkieTalkieViewModel = hiltViewModel(),
  connectToDevice: (BluetoothDevice) -> Unit,
  onListen: () -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
) {
  val state by viewModel.walkieTalkieState.collectAsState()

  WalkieTalkieScreen(
    state = state,
    bluetoothEnabled = viewModel::bluetoothEnabled,
    locationEnabled = viewModel::locationEnabled,
    onConnect = onConnect,
    onListen = onListen,
    onDisconnect = onDisconnect,
    connectToDevice = connectToDevice,
    startSpeaking = viewModel::startSpeaking,
    stopSpeaking = viewModel::stopSpeaking
  )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkieTalkieScreen(
  state: WalkieTalkieState,
  bluetoothEnabled: () -> Boolean = { false },
  locationEnabled: () -> Boolean = { false },
  onConnect: () -> Unit,
  onListen: () -> Unit,
  onDisconnect: () -> Unit,
  connectToDevice: (BluetoothDevice) -> Unit,
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit,
) {
  val permissionsState = rememberMultiplePermissionsState(permissionsList) {
    if (it.values.all { it }) {
      state.permissionKeepTryingAction?.invoke()
    }
  }

  val bluetoothEnableLauncher =
    rememberLauncherForActivityResult(contract = StartActivityForResult()) {
      if (bluetoothEnabled()) {
        state.permissionKeepTryingAction?.invoke()
      } else {
        state.permissionKeepTryingAction = null
      }
    }

  val locationEnableLauncher =
    rememberLauncherForActivityResult(contract = StartActivityForResult()) {
      if (locationEnabled()) {
        state.permissionKeepTryingAction?.invoke()
      } else {
        state.permissionKeepTryingAction = null
      }
    }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .navigationBarsPadding()
      .padding(16.dp)
  ) {
    WalkieTalkieContent(
      modifier = Modifier.fillMaxSize(),
      state = state,
      onConnect = {
        actionIfEnabledBluetoothAndLocation(
          state,
          permissionsState,
          bluetoothEnabled,
          locationEnabled,
          bluetoothEnableLauncher,
          locationEnableLauncher,
          onConnect,
          goingDiscoverable = false
        )
      },
      onListen = {
        actionIfEnabledBluetoothAndLocation(
          state,
          permissionsState,
          bluetoothEnabled,
          locationEnabled,
          bluetoothEnableLauncher,
          locationEnableLauncher,
          onListen,
          goingDiscoverable = true
        )
      },
      onDisconnect = onDisconnect,
      connectToDevice = connectToDevice,
      permissionState = permissionsState,
      startSpeaking = startSpeaking,
      stopSpeaking = stopSpeaking
    )
  }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun actionIfEnabledBluetoothAndLocation(
  state: WalkieTalkieState,
  permissionsState: MultiplePermissionsState,
  bluetoothEnabled: () -> Boolean,
  locationEnabled: () -> Boolean,
  bluetoothEnableLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
  locationEnableLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
  action: () -> Unit,
  goingDiscoverable: Boolean
) {
  if (!permissionsState.allPermissionsGranted) {
    permissionsState.launchMultiplePermissionRequest()
    state.permissionKeepTryingAction = {
      actionIfEnabledBluetoothAndLocation(
        state,
        permissionsState,
        bluetoothEnabled,
        locationEnabled,
        bluetoothEnableLauncher,
        locationEnableLauncher,
        action,
        goingDiscoverable
      )
    }
    return
  }

  when {
    !bluetoothEnabled() -> {
      val intent = if (goingDiscoverable) {
        Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
          putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        })
      } else {
        Intent("android.bluetooth.adapter.action.REQUEST_ENABLE")
      }

      bluetoothEnableLauncher.launch(intent)
      state.permissionKeepTryingAction = {
        actionIfEnabledBluetoothAndLocation(
          state,
          permissionsState,
          bluetoothEnabled,
          locationEnabled,
          bluetoothEnableLauncher,
          locationEnableLauncher,
          action,
          goingDiscoverable
        )
      }
    }

    !locationEnabled() -> {
      locationEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
      state.permissionKeepTryingAction = {
        actionIfEnabledBluetoothAndLocation(
          state,
          permissionsState,
          bluetoothEnabled,
          locationEnabled,
          bluetoothEnableLauncher,
          locationEnableLauncher,
          action,
          goingDiscoverable
        )
      }
    }

    else -> {
      if (goingDiscoverable) {
        bluetoothEnableLauncher.launch(
          Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
          })
        )
      }
      action()
      state.permissionKeepTryingAction = null
    }
  }
}