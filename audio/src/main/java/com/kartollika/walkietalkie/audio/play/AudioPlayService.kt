package com.kartollika.walkietalkie.audio.play

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager
import android.media.AudioTrack
import android.os.IBinder
import com.kartollika.walkietalkie.bluetooth.BluetoothAction
import com.kartollika.walkietalkie.bluetooth.BluetoothAction.ReceivingVoiceStarted
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import com.kartollika.walkietalkie.bluetooth.SocketHolder
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayService : Service() {

  @Inject
  lateinit var bluetoothActionsDataSource: BluetoothActionsDataSource

  private var audioTrack: AudioTrack? = null
  private var keepPlaying = true

  override fun onBind(intent: Intent): IBinder {
    error("Not supported")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startStreaming()
    return super.onStartCommand(intent, flags, startId)
  }

  private fun startStreaming() {
    val audioStreamingRunnable = Runnable {
      var bufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, CHANNEL_OUT_MONO, ENCODING)
      if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
        bufferSize = AUDIO_SAMPLE_RATE * 2
      }

      val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setAudioFormat(
          AudioFormat.Builder()
            .setSampleRate(AUDIO_SAMPLE_RATE)
            .setChannelMask(CHANNEL_OUT_MONO)
            .setEncoding(ENCODING)
            .build()
        )
        .build()
      this.audioTrack = audioTrack

      audioTrack.play()

      val buffer = ByteArray(bufferSize)

      try {
        val socket = SocketHolder.socket!!
        val inputStream: InputStream = socket.inputStream ?: return@Runnable
        var readBytes: Int = inputStream.read(buffer, 0, bufferSize)

        while (keepPlaying && readBytes != 1) {
          bluetoothActionsDataSource.sendAction(ReceivingVoiceStarted)
          audioTrack.write(buffer, 0, buffer.size)
          readBytes = inputStream.read(buffer, 0, bufferSize)
        }
        inputStream.close()

        audioTrack.release()
      } catch (e: IOException) {
        e.printStackTrace()
        bluetoothActionsDataSource.sendAction(BluetoothAction.Error(e, "Input stream closed"))
      } catch (e: java.lang.NullPointerException) {
        bluetoothActionsDataSource.sendAction(BluetoothAction.Error(e, "Input stream closed"))
        e.printStackTrace()
      }
    }
    Thread(audioStreamingRunnable).start()
  }

  override fun onDestroy() {
    super.onDestroy()
    keepPlaying = false
    if (audioTrack != null) {
      audioTrack?.release()
    }
  }

  companion object {
    private const val AUDIO_SAMPLE_RATE = 8000
    private const val ENCODING = ENCODING_PCM_16BIT
  }
}