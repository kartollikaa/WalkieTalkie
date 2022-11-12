package com.kartollika.walkietalkie

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kartollika.walkietalkie.ui.theme.WalkieTalkieTheme
import dagger.hilt.android.AndroidEntryPoint

val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
  listOf(
    BLUETOOTH,
    BLUETOOTH_ADMIN,
    BLUETOOTH_SCAN,
    BLUETOOTH_CONNECT,
    ACCESS_COARSE_LOCATION,
    ACCESS_FINE_LOCATION,
    RECORD_AUDIO
  )
} else {
  listOf(
    BLUETOOTH,
    BLUETOOTH_ADMIN,
    ACCESS_COARSE_LOCATION,
    RECORD_AUDIO
  )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private var bluetoothDeviceFoundReceiver: BroadcastReceiver? = null
  private var settingsIntentLauncher: ActivityResultLauncher<Intent>? = null

  val enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) {}

  @OptIn(ExperimentalPermissionsApi::class) override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      val viewModel: WalkieTalkieViewModel = hiltViewModel()
      val state by viewModel.walkieTalkieState.collectAsState()

      val connectPermissionsState = rememberMultiplePermissionsState(permissionsList) { result ->
        if (result.values.all { granted -> granted }) return@rememberMultiplePermissionsState

        if (result.filter { it.key != ACCESS_FINE_LOCATION }
            .map { ActivityCompat.shouldShowRequestPermissionRationale(this, it.key) }
            .all { shouldShowRationale -> !shouldShowRationale }) {
          openSettings()
        }
      }

      val listenPermissionState = rememberMultiplePermissionsState(permissionsList) { result ->
        if (result.values.all { granted -> granted }) return@rememberMultiplePermissionsState

        if (result.filter { it.key != ACCESS_FINE_LOCATION }
            .map { ActivityCompat.shouldShowRequestPermissionRationale(this, it.key) }
            .all { shouldShowRationale -> !shouldShowRationale }) {
          openSettings()
        }
      }

      val onConnect = {
        if (connectPermissionsState.allPermissionsGranted) {
          registerBroadcast() { device ->
            viewModel.addDevice(device)
          }
          enableBluetooth(state) {
            viewModel.startDiscovery()
          }
        } else {
          connectPermissionsState.launchMultiplePermissionRequest()
        }
      }

      val onListen = {
        if (listenPermissionState.allPermissionsGranted) {
          enableBluetooth(state) {
            viewModel.listenForConnections()
          }
        } else {
          listenPermissionState.launchMultiplePermissionRequest()
        }
      }

      LaunchedEffect(state) {
        if (state is Connected) {
          this@MainActivity.startService(Intent(this@MainActivity, AudioPlayService::class.java))
        }
      }

      WalkieTalkieTheme {
        Surface(color = MaterialTheme.colors.background) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .systemBarsPadding()
              .navigationBarsPadding()
              .padding(16.dp)
          ) {

            WalkieTalkie(
              modifier = Modifier.fillMaxSize(), state = state,
              onConnect = {
                onConnect()
              },
              onListen = {
                onListen()
              },
              onDisconnect = {
                viewModel.closeConnection()
              },
              connectToDevice = { bluetoothDevice ->
                viewModel.connectToDevice(bluetoothDevice)
              },
              startSpeaking = {
                viewModel.startSpeaking()
              },
              stopSpeaking = {
                viewModel.idleWalkie()
              }
            )
          }
        }
      }
    }
  }

  private fun registerBroadcast(addDevice: (BluetoothDevice) -> Unit) {
    val intentFilter = IntentFilter()
    intentFilter.addAction("android.bluetooth.device.action.FOUND")

    bluetoothDeviceFoundReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if ("android.bluetooth.device.action.FOUND" == intent.action) {
          val bluetoothDevice =
            intent.getParcelableExtra<Parcelable>("android.bluetooth.device.extra.DEVICE") as BluetoothDevice?
              ?: return
          addDevice(bluetoothDevice)
        }
      }
    }
    registerReceiver(bluetoothDeviceFoundReceiver, intentFilter)
  }

  private fun enableBluetooth(state: WalkieTalkieState, onResult: () -> Unit) {
    if (state is Idle && !state.bluetoothEnabled()) {
      val intent = Intent("android.bluetooth.adapter.action.REQUEST_ENABLE")
      enableBluetoothLauncher.launch(intent)
    }
    onResult()
  }

  private fun openSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    settingsIntentLauncher?.launch(intent)
  }

  override fun onStart() {
    super.onStart()
    settingsIntentLauncher = registerForActivityResult(StartActivityForResult()) {}
  }

  override fun onStop() {
    super.onStop()

    bluetoothDeviceFoundReceiver?.let {
      unregisterReceiver(it)
      bluetoothDeviceFoundReceiver = null
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    bluetoothDeviceFoundReceiver?.let {
      unregisterReceiver(it)
      bluetoothDeviceFoundReceiver = null
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("MissingPermission")
@Composable fun WalkieTalkie(
  modifier: Modifier = Modifier,
  state: WalkieTalkieState,
  onConnect: () -> Unit,
  onListen: () -> Unit,
  onDisconnect: () -> Unit,
  connectToDevice: (BluetoothDevice) -> Unit,
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
    BluetoothControllers(
      modifier = Modifier.fillMaxWidth(),
      state = state,
      onListen = onListen,
      onConnect = onConnect,
      onDisconnect = onDisconnect
    )
    when (state) {
      is Idle, is Listening -> {}
      is Connected -> {
        Surface(
          modifier = Modifier
            .size(64.dp)
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
          Icon(imageVector = Icons.Default.Mic, contentDescription = "Push to speak")
        }
      }

      is Searching -> {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          items(state.devices.toList()) { device: BluetoothDevice ->
            BluetoothDevice(connectToDevice, device)
          }
        }
      }
    }
  }
}

@SuppressLint("MissingPermission") @Composable private fun BluetoothDevice(
  connectToDevice: (BluetoothDevice) -> Unit, device: BluetoothDevice
) {
  Row(modifier = Companion
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

@Composable private fun BluetoothControllers(
  modifier: Modifier = Modifier,
  state: WalkieTalkieState,
  onListen: () -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
    if (state !is Connected) {
      ProgressButton(
        loading = state is Searching,
        idleContent = {
          Text(text = "Connect")
        },
        onClick = onConnect,
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

@Composable fun ProgressButton(
  modifier: Modifier = Modifier,
  loading: Boolean,
  idleContent: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Button(
    modifier = modifier
      .animateContentSize()
      .width(128.dp),
    onClick = onClick
  ) {
    AnimatedVisibility(visible = loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = MaterialTheme.colors.onPrimary,
        strokeWidth = 2.dp
      )
    }
    AnimatedVisibility(visible = !loading) {
      idleContent()
    }
  }
}

@Preview(showBackground = true) @Composable fun DefaultPreview() {
  WalkieTalkieTheme {}
}