package com.kartollika.walkietalkie.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DeviceDiscovered
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DiscoveryStarted
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.DiscoveryStopped
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.Error
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.ListenForConnections
import com.kartollika.walkietalkie.bluetooth.thread.ConnectedThread
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothService : Service() {

  @Inject
  lateinit var bluetoothAdapter: BluetoothAdapter

  @Inject
  lateinit var bluetoothActionsDataSource: BluetoothActionsDataSource

  private val binder = BluetoothBinder()

  // Bluetooth connections
  private var connectThread: ConnectThread? = null
  private var connectedThread: ConnectedThread? = null
  private var acceptThread: AcceptThread? = null
  private var connectedDevice: BluetoothDevice? = null

  private val discoveryBroadcastReceiver = object : BroadcastReceiver() {

    private val DEVICE_EXTRA = "android.bluetooth.device.extra.DEVICE"

    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == BluetoothDevice.ACTION_FOUND) {
        val bluetoothDevice = getBluetoothDevice(intent) ?: return
        bluetoothActionsDataSource.sendAction(DeviceDiscovered(bluetoothDevice))
      }
    }

    private fun getBluetoothDevice(intent: Intent): BluetoothDevice? {
      return intent.getParcelableExtra(DEVICE_EXTRA) as BluetoothDevice?
    }
  }

  override fun onBind(intent: Intent?): IBinder = binder

  inner class BluetoothBinder : Binder() {

    fun getService(): BluetoothService {
      return this@BluetoothService
    }
  }

  @SuppressLint("MissingPermission")
  fun startDiscovery() {
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    registerReceiver(discoveryBroadcastReceiver, filter)
    bluetoothAdapter.cancelDiscovery()
    val discoveryStarted = bluetoothAdapter.startDiscovery()
    if (!discoveryStarted) {
      bluetoothActionsDataSource.sendAction(Error(message = "Discovery not started"))
      return
    }

    bluetoothAdapter.startDiscovery()
    bluetoothActionsDataSource.sendAction(DiscoveryStarted)

    bluetoothAdapter.bondedDevices.forEach { device ->
      bluetoothActionsDataSource.sendAction(DeviceDiscovered(device))
    }
  }

  fun connect(bluetoothDevice: BluetoothDevice) {
    connectThread = ConnectThread(bluetoothDevice)
    connectThread?.start()
  }

  fun listenForConnection() {
    bluetoothActionsDataSource.sendAction(ListenForConnections)
    acceptThread = AcceptThread(10000)
    acceptThread?.start()
  }

  @SuppressLint("MissingPermission")
  fun stopDiscovery() {
    bluetoothAdapter.cancelDiscovery()
    bluetoothActionsDataSource.sendAction(DiscoveryStopped)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  @SuppressLint("MissingPermission")
  private inner class AcceptThread(private val timeout: Int) : Thread() {

    private val serverSocket: BluetoothServerSocket? by lazy {
      bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Bluetooth Service", UUID_CHANNEL)
    }

    override fun run() {
      // Keep listening until exception occurs or a socket is returned.
      var shouldLoop = true
      while (shouldLoop) {
        val socket: BluetoothSocket? = try {
          serverSocket?.accept(timeout)
        } catch (e: IOException) {
          shouldLoop = false
          null
        }
        socket?.also {
          manageMyConnectedSocket(it)
          serverSocket?.close()
          shouldLoop = false
        }
      }
    }

    // Closes the connect socket and causes the thread to finish.
    fun cancel() {
      try {
        disconnect()
      } catch (e: IOException) {
      }
    }
  }

  private fun manageMyConnectedSocket(socket: BluetoothSocket) {
    SocketHolder.socket = socket
    connectedDevice = socket.remoteDevice

    connectedThread?.interrupt()
    connectedThread?.cancel()

//    connectedThread = ConnectedThread(
//      socket.inputStream,
//      socket.outputStream,
//      bluetoothActionsDataSource,
//      onClose = {
//        socket.close()
//      }
//    )
    connectedThread?.start()
  }

  @SuppressLint("MissingPermission")
  private inner class ConnectThread(device: BluetoothDevice) : Thread() {

    private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
      device.createRfcommSocketToServiceRecord(UUID_CHANNEL)
    }

    @SuppressLint("MissingPermission")
    override fun run() {
      // Cancel discovery because it otherwise slows down the connection.
      stopDiscovery()

      socket?.let { socket ->
        try {
          socket.connect()
        } catch (_: Exception) {
        }
        // Connect to the remote device through the socket. This call blocks
        // until it succeeds or throws an exception.

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        manageMyConnectedSocket(socket)
        socket.close()
      }
    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
      try {
        disconnect()
      } catch (_: IOException) {
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(discoveryBroadcastReceiver)
    } catch (e: Exception) {
      // already unregistered
    }
  }

  fun disconnect() {
    SocketHolder.socket?.close()
    SocketHolder.socket = null
  }

  companion object {
    val UUID_CHANNEL = UUID.nameUUIDFromBytes("com.kartollika.walkietalkie".toByteArray())
  }
}