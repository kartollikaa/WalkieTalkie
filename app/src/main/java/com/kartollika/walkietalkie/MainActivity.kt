package com.kartollika.walkietalkie

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.RECORD_AUDIO
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kartollika.walkietalkie.bluetooth.BluetoothService
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

  private val micRecorder = MicRecorder()
  private var settingsIntentLauncher = registerForActivityResult(StartActivityForResult()) {}
  private val enableBluetoothLauncher =
    registerForActivityResult(StartActivityForResult()) { result ->

    }

  private lateinit var bluetoothService: BluetoothService

  private val bluetoothServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as BluetoothService.BluetoothBinder
      bluetoothService = binder.getService()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    bindBluetoothService()

    setContent {
      WalkieTalkieTheme {
        Surface(color = MaterialTheme.colors.background) {
          val viewModel: WalkieTalkieViewModel = hiltViewModel()
          val state by viewModel.walkieTalkieState.collectAsState()

          DisposableEffect(state) {
            if (state is Connected) {
              startService(Intent(this@MainActivity, AudioPlayService::class.java))
            }

            onDispose {
              stopService(Intent(this@MainActivity, AudioPlayService::class.java))
            }
          }

          WalkieTalkieScreen(
            state = state,
            openSettings = ::openSettings,
            shouldShowRequestPermissionRationale = { permission ->
              ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            },
            connectToDevice = { bluetoothDevice ->
              bluetoothService.connect(bluetoothDevice)
            },
            onListen = {
              bluetoothService.listenForConnection()
            },
            onConnect = {
              bluetoothService.startDiscovery()
            },
            stopSpeaking = {
              micRecorder.keepRecording = false
            },
            startSpeaking = {
              micRecorder.keepRecording = true
              Thread(micRecorder).start()
            },
            onDisconnect = {
              bluetoothService.disconnect()
            }
          )
        }
      }
    }
  }

  private fun bindBluetoothService() {
    val intent = Intent(this, BluetoothService::class.java)
    bindService(
      intent,
      bluetoothServiceConnection,
      Context.BIND_AUTO_CREATE
    )
  }

  private fun openSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    settingsIntentLauncher.launch(intent)
  }
}

@Preview(showBackground = true) @Composable fun DefaultPreview() {
  WalkieTalkieTheme {}
}