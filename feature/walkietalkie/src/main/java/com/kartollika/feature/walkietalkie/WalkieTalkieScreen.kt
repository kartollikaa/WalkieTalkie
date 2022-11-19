package com.kartollika.feature.walkietalkie

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun WalkieTalkieRoute(
  viewModel: WalkieTalkieViewModel = hiltViewModel(),
  connectToDevice: (BluetoothDevice) -> Unit,
  onListen: () -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  shouldShowRequestPermissionRationale: (String) -> Boolean,
) {
  val state by viewModel.walkieTalkieState.collectAsState()

  WalkieTalkieScreen(
    state = state,
    shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
    connectToDevice = connectToDevice,
    onConnect = onConnect,
    onDisconnect = onDisconnect,
    onListen = onListen,
    startSpeaking = viewModel::startSpeaking,
    stopSpeaking = viewModel::stopSpeaking
  )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkieTalkieScreen(
  state: WalkieTalkieState,
  shouldShowRequestPermissionRationale: (String) -> Boolean,
  onConnect: () -> Unit,
  onListen: () -> Unit,
  onDisconnect: () -> Unit,
  connectToDevice: (BluetoothDevice) -> Unit,
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit,
) {
  val intent = Intent("android.bluetooth.adapter.action.REQUEST_ENABLE")

  val permissionsState = rememberMultiplePermissionsState(permissionsList) { result ->
    if (result.values.all { granted -> granted }) return@rememberMultiplePermissionsState

    if (result.filter { it.key != permission.ACCESS_FINE_LOCATION }
        .map { shouldShowRequestPermissionRationale(it.key) }
        .all { shouldShowRationale -> !shouldShowRationale }) {
    }
  }

  val bluetoothEnableLauncher = rememberLauncherForActivityResult(
    contract = StartActivityForResult()
  ) {
    permissionsState.launchMultiplePermissionRequest()
    if (it.resultCode == Activity.RESULT_OK) {
      onConnect()
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
      onConnect = onConnect,
      onListen = onListen,
      onDisconnect = onDisconnect,
      connectToDevice = connectToDevice,
      permissionState = permissionsState,
      startSpeaking = startSpeaking,
      stopSpeaking = stopSpeaking
    )
  }
}