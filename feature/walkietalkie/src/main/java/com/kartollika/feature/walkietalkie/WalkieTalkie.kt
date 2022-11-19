package com.kartollika.feature.walkietalkie

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.content.Intent
import android.view.MotionEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode.Reverse
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.kartollika.feature.walkietalkie.Connected.WalkieMode
import com.kartollika.feature.walkietalkie.Connected.WalkieMode.IDLE
import com.kartollika.feature.walkietalkie.Connected.WalkieMode.LISTENING
import com.kartollika.feature.walkietalkie.Connected.WalkieMode.SPEAKING
import com.kartollika.walkietalkie.audio.play.AudioPlayService

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable fun WalkieTalkieContent(
  state: WalkieTalkieState,
  modifier: Modifier = Modifier,
  onConnect: () -> Unit = {},
  onListen: () -> Unit = {},
  onDisconnect: () -> Unit = {},
  connectToDevice: (BluetoothDevice) -> Unit = {},
  startSpeaking: () -> Unit = {},
  stopSpeaking: () -> Unit = {},
  permissionState: MultiplePermissionsState,
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
        val context = LocalContext.current

        DisposableEffect(Unit) {
          context.startService(Intent(context, AudioPlayService::class.java))

          onDispose {
            context.stopService(Intent(context, AudioPlayService::class.java))
          }
        }

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
      walkieTalkieMode = state.walkieTalkieMode,
      modifier = Modifier.align(Center),
      startSpeaking = startSpeaking,
      stopSpeaking = stopSpeaking
    )

    Column(
      modifier = Modifier.align(BottomCenter),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = "Расстояние между устройствами:")
      Text(text = "≈ ${Connected.distance} m")
    }
  }
}

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun PushToTalk(
  walkieTalkieMode: WalkieMode,
  startSpeaking: () -> Unit,
  stopSpeaking: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val color by animateColorAsState(
    targetValue = when (walkieTalkieMode) {
      LISTENING -> Color.Cyan
      SPEAKING -> Color.Green
      IDLE -> Color.Yellow
    }, animationSpec = tween(500)
  )

  val listeningInfiniteTransition = rememberInfiniteTransition()
  val scale by when (walkieTalkieMode) {
    LISTENING -> listeningInfiniteTransition.animateFloat(
      initialValue = 0.8f,
      targetValue = 1.2f,
      animationSpec = infiniteRepeatable(
        animation = tween(400, easing = FastOutSlowInEasing),
        repeatMode = Reverse
      )
    )
    else -> animateFloatAsState(targetValue = if (walkieTalkieMode == SPEAKING) 1f else 0.8f)
  }

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
      }
      .graphicsLayer(scaleX = scale, scaleY = scale),
    shape = CircleShape,
    color = color
  ) {
    val icon = when (walkieTalkieMode) {
      LISTENING -> Icons.Filled.Speaker
      SPEAKING -> Icons.Filled.Wifi
      IDLE -> Icons.Filled.Mic
    }
    Icon(
      modifier = Modifier.padding(42.dp),
      imageVector = icon,
      contentDescription = "Push to speak",
      tint = Color(0xff121212)
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
        onClick = {
          if (permissionState.allPermissionsGranted) {
            onListen()
          } else {
            permissionState.launchMultiplePermissionRequest()
          }
        },
      )
    } else {
      Button(onClick = onDisconnect) {
        Text(text = "Disconnect")
      }
    }
  }
}