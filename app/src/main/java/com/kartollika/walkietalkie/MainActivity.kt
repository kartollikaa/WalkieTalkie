package com.kartollika.walkietalkie

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.kartollika.feature.walkietalkie.WalkieTalkieRoute
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import com.kartollika.walkietalkie.bluetooth.BluetoothService
import com.kartollika.walkietalkie.ui.theme.WalkieTalkieTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var dataSource: BluetoothActionsDataSource

  private var bluetoothService: BluetoothService? = null

  private val bluetoothServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as BluetoothService.BluetoothBinder
      bluetoothService = binder.getService()
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      bluetoothService = null
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      WalkieTalkieTheme {
        Surface(color = MaterialTheme.colors.background) {
          WalkieTalkieRoute(
            connectToDevice = { bluetoothDevice ->
              bluetoothService?.connect(bluetoothDevice)
            },
            onListen = {
              bluetoothService?.listenForConnection()
            },
            onConnect = {
              bluetoothService?.startDiscovery()
            },
            onDisconnect = {
              bluetoothService?.disconnect()
            }
          )
        }
      }
    }

    bindBluetoothService()
  }

  private fun bindBluetoothService() {
    val intent = Intent(this, BluetoothService::class.java)
    bindService(
      intent,
      bluetoothServiceConnection,
      Context.BIND_AUTO_CREATE
    )
  }
}

@Preview(showBackground = true) @Composable fun DefaultPreview() {
  WalkieTalkieTheme {}
}